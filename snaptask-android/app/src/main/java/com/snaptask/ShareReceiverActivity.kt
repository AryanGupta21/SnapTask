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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.snaptask.network.OpenClawClient
import com.snaptask.network.models.PlannedAction
import com.snaptask.network.models.SnapTaskResponse
import com.snaptask.ocr.MLKitOCRProcessor
import com.snaptask.samsung.CalendarManager
import com.snaptask.samsung.ContactsManager
import com.snaptask.ui.theme.SnapTaskTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    private val ocrProcessor = MLKitOCRProcessor()
    private val openClawClient = OpenClawClient.create()
    private val contactsManager by lazy { ContactsManager(this) }
    private val calendarManager by lazy { CalendarManager(this) }
    private var contactPermissionRequest: CompletableDeferred<Boolean>? = null
    private var calendarPermissionRequest: CompletableDeferred<Boolean>? = null

    private val contactPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactPermissionRequest?.complete(granted)
        contactPermissionRequest = null
    }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        calendarPermissionRequest?.complete(results.values.all { it })
        calendarPermissionRequest = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUri = extractSharedImageUri(intent)
        val taskState = mutableStateOf<TaskState>(
            if (imageUri == null) TaskState.Error("No image received") else TaskState.ReadingImage
        )

        setContent {
            SnapTaskTheme {
                val state by taskState
                TaskResultScreen(
                    state = state,
                    onCancel = { finish() },
                    onExecute = { response ->
                        lifecycleScope.launch {
                            taskState.value = executePlannedActions(response)
                        }
                    }
                )
            }
        }

        if (imageUri != null) {
            lifecycleScope.launch {
                val rawText = runCatching {
                    ocrProcessor.process(this@ShareReceiverActivity, imageUri).trim()
                }.getOrElse {
                    taskState.value = TaskState.Error("Could not read text from image")
                    return@launch
                }

                if (rawText.isBlank()) {
                    taskState.value = TaskState.Error("No text found")
                    return@launch
                }

                taskState.value = TaskState.Classifying(rawText)
                taskState.value = runCatching {
                    openClawClient.process(rawText, emptyList())
                }.fold(
                    onSuccess = { response -> TaskState.Planned(rawText, response) },
                    onFailure = { TaskState.Error("Gateway unreachable. Is it running on your computer?") }
                )
            }
        }
    }

    internal fun extractSharedImageUri(intent: Intent?): Uri? {
        val i = intent?.takeIf { it.action == Intent.ACTION_SEND } ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            i.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private suspend fun executePlannedActions(response: SnapTaskResponse): TaskState {
        val action = response.actions.firstOrNull()
            ?: return TaskState.Error("No action to execute")

        return when (action.type) {
            "create_contact" -> executeCreateContact(action)
            "create_calendar_event" -> executeCreateCalendarEvent(action)
            else -> TaskState.Error("Execution is not implemented yet for ${action.type}")
        }
    }

    private suspend fun executeCreateContact(action: PlannedAction): TaskState {
        if (!ensureContactPermission()) {
            return TaskState.Error("Contacts permission is required to create a contact")
        }

        return runCatching {
            contactsManager.create(action.params)
        }.fold(
            onSuccess = { TaskState.Executed("Contact created") },
            onFailure = { TaskState.Error("Could not create contact") }
        )
    }

    private suspend fun executeCreateCalendarEvent(action: PlannedAction): TaskState {
        if (!ensureCalendarPermission()) {
            return TaskState.Error("Calendar permission is required to create an event")
        }

        return runCatching {
            calendarManager.create(action.params)
        }.fold(
            onSuccess = { TaskState.Executed("Calendar event created") },
            onFailure = { openCalendarEditor(action) }
        )
    }

    private fun openCalendarEditor(action: PlannedAction): TaskState {
        val intent = calendarManager.buildInsertIntent(action.params)
            ?: return TaskState.Error("Could not prepare calendar event")

        return runCatching {
            startActivity(intent)
        }.fold(
            onSuccess = { TaskState.Executed("Calendar editor opened. Tap Save to create the event.") },
            onFailure = { TaskState.Error("Could not create calendar event") }
        )
    }

    private suspend fun ensureContactPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        val request = CompletableDeferred<Boolean>()
        contactPermissionRequest = request
        contactPermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
        return request.await()
    }

    private suspend fun ensureCalendarPermission(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            return true
        }

        val request = CompletableDeferred<Boolean>()
        calendarPermissionRequest = request
        calendarPermissionLauncher.launch(permissions)
        return request.await()
    }
}

@Composable
private fun TaskResultScreen(
    state: TaskState,
    onCancel: () -> Unit,
    onExecute: (SnapTaskResponse) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when (state) {
            TaskState.ReadingImage -> CenteredMessage(text = "Reading image...")

            is TaskState.Classifying -> TextDetails(
                title = "Extracted text",
                body = state.rawText,
                footer = "Classifying intent..."
            )

            is TaskState.Error -> CenteredMessage(text = state.message)

            is TaskState.Planned -> PlanDetails(
                rawText = state.rawText,
                response = state.response,
                onCancel = onCancel,
                onExecute = onExecute
            )

            is TaskState.Executed -> CenteredMessage(text = state.message)
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text)
    }
}

@Composable
private fun TextDetails(title: String, body: String, footer: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = title)
        Text(text = body)
        if (footer != null) {
            Text(text = footer)
        }
    }
}

@Composable
private fun PlanDetails(
    rawText: String,
    response: SnapTaskResponse,
    onCancel: () -> Unit,
    onExecute: (SnapTaskResponse) -> Unit
) {
    val firstAction = response.actions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Planned action")
        Text(text = response.summary)
        Text(text = "Intent: ${response.intent}")
        Text(text = "Confidence: ${response.confidence}")
        Text(text = "Action: ${firstAction?.type ?: "none"}")
        Text(text = "Params: ${firstAction?.params ?: emptyMap<String, Any>()}")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel) {
                Text(text = "Cancel")
            }
            Button(
                onClick = { onExecute(response) },
                enabled = response.actions.isNotEmpty()
            ) {
                Text(text = "Execute")
            }
        }
        Text(text = "Extracted text")
        Text(text = rawText)
    }
}

private sealed interface TaskState {
    data object ReadingImage : TaskState
    data class Classifying(val rawText: String) : TaskState
    data class Planned(val rawText: String, val response: SnapTaskResponse) : TaskState
    data class Executed(val message: String) : TaskState
    data class Error(val message: String) : TaskState
}
