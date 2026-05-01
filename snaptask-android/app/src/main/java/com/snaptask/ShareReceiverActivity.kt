package com.snaptask

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.snaptask.network.OpenClawClient
import com.snaptask.network.models.SnapTaskResponse
import com.snaptask.ocr.EntityExtractor
import com.snaptask.ocr.MLKitOCRProcessor
import com.snaptask.samsung.CalendarManager
import com.snaptask.samsung.ContactsManager
import com.snaptask.samsung.NotesManager
import com.snaptask.ui.ConfirmationSheet
import com.snaptask.ui.theme.SnapTaskTheme
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    private val ocrProcessor = MLKitOCRProcessor()
    private val entityExtractor = EntityExtractor()
    private val openClawClient = OpenClawClient.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUri: Uri? = intent
            ?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getParcelableExtra(Intent.EXTRA_STREAM)

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

    private suspend fun processImage(uri: Uri): SnapTaskResponse {
        val rawText = ocrProcessor.process(this, uri)
        val entities = entityExtractor.annotate(rawText)
        return openClawClient.process(rawText, entities)
    }

    private fun executeActions(response: SnapTaskResponse) {
        lifecycleScope.launch {
            response.actions.forEach { action ->
                when (action.type) {
                    "create_calendar_event" -> CalendarManager(this@ShareReceiverActivity).create(action.params)
                    "create_contact" -> ContactsManager(this@ShareReceiverActivity).create(action.params)
                    "create_note" -> NotesManager(this@ShareReceiverActivity).create(action.params)
                    "log_expense" -> NotesManager(this@ShareReceiverActivity).logExpense(action.params)
                }
            }
            finish()
        }
    }
}
