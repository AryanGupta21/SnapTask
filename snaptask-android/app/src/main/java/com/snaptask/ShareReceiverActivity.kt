package com.snaptask

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.snaptask.network.OpenClawClient
import com.snaptask.network.models.PlannedAction
import com.snaptask.network.models.SnapTaskResponse
import com.snaptask.ocr.EntityExtractor
import com.snaptask.ocr.MLKitOCRProcessor
import com.snaptask.samsung.CalendarManager
import com.snaptask.samsung.ContactsManager
import com.snaptask.samsung.NotesManager
import com.snaptask.ui.ConfirmationSheet
import com.snaptask.ui.theme.SnapTaskTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    private val ocrProcessor = MLKitOCRProcessor()
    private val entityExtractor = EntityExtractor()
    private val openClawClient = OpenClawClient.create()

    private var permissionDeferred: CompletableDeferred<Boolean>? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionDeferred?.complete(results.values.all { it })
        permissionDeferred = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUri: Uri? = extractSharedImageUri()
        if (imageUri == null) {
            finish()
            return
        }

        setContent {
            SnapTaskTheme {
                ConfirmationSheet(
                    onLoad = { processImage(imageUri) },
                    onExecute = { response -> executeActions(response) },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun extractSharedImageUri(): Uri? {
        val i = intent?.takeIf { it.action == Intent.ACTION_SEND } ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            i.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private suspend fun ensurePermissions(): Boolean {
        val needed = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) return true
        val deferred = CompletableDeferred<Boolean>()
        permissionDeferred = deferred
        permissionLauncher.launch(needed.toTypedArray())
        return deferred.await()
    }

    private suspend fun processImage(uri: Uri): SnapTaskResponse {
        val rawText = ocrProcessor.process(this, uri)
        if (rawText.trim().length < 10)
            throw IllegalStateException("NO_TEXT: Not enough readable text in this image")
        val entities = entityExtractor.annotate(rawText)
        return openClawClient.process(rawText, entities)
    }

    private fun executeActions(response: SnapTaskResponse) {
        lifecycleScope.launch {
            if (!ensurePermissions()) return@launch
            var launchedShareIntent = false
            response.actions.forEach { action ->
                val label = actionLabel(action)
                when (action.type) {
                    "create_calendar_event" -> { CalendarManager(this@ShareReceiverActivity).create(action.params); ActionHistory.record(this@ShareReceiverActivity, action.type, label) }
                    "create_contact"        -> { ContactsManager(this@ShareReceiverActivity).create(action.params); ActionHistory.record(this@ShareReceiverActivity, action.type, label) }
                    "create_note"           -> { NotesManager(this@ShareReceiverActivity).create(action.params); ActionHistory.record(this@ShareReceiverActivity, action.type, label); launchedShareIntent = true }
                    "log_expense"           -> { NotesManager(this@ShareReceiverActivity).logExpense(action.params); ActionHistory.record(this@ShareReceiverActivity, action.type, label); launchedShareIntent = true }
                }
            }
            // For share-based intents (notes), delay finish so the chooser/app has time to appear
            if (launchedShareIntent) {
                kotlinx.coroutines.delay(500)
            }
            finish()
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
        )

        fun actionLabel(action: PlannedAction): String = when (action.type) {
            "create_calendar_event" -> buildString {
                append(action.params["title"] as? String ?: "Event")
                (action.params["dateTime"] as? String)?.let { append(" — $it") }
                (action.params["location"] as? String)?.let { append(", $it") }
            }
            "create_contact" -> buildString {
                append(action.params["name"] as? String ?: "Contact")
                val details = listOfNotNull(
                    action.params["phone"] as? String,
                    action.params["email"] as? String
                )
                if (details.isNotEmpty()) append(" (${details.joinToString(", ")})")
            }
            "create_note" -> action.params["title"] as? String ?: "Note"
            "log_expense" -> buildString {
                append(action.params["merchant"] as? String ?: "Expense")
                val currency = action.params["currency"] as? String ?: "USD"
                val amount = action.params["amount"]?.toString() ?: ""
                if (amount.isNotBlank()) append(" — $currency $amount")
            }
            else -> action.type.replace('_', ' ')
        }
    }
}
