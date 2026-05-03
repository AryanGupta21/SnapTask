package com.snaptask.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.network.models.PlannedAction
import com.snaptask.network.models.SnapTaskResponse

// ── State ──────────────────────────────────────────────────────────────────

sealed interface SheetState {
    data object Loading : SheetState
    data class Ready(val response: SnapTaskResponse) : SheetState
    data class Error(val title: String, val hint: String) : SheetState
}

fun errorStateFrom(e: Exception): SheetState.Error = when {
    e.message?.contains("NO_TEXT") == true -> SheetState.Error(
        title = "No readable text found",
        hint = "Try better lighting or hold the camera closer."
    )
    e is java.net.ConnectException || e is java.net.SocketTimeoutException -> SheetState.Error(
        title = "Can't reach the server",
        hint = "Make sure OpenClaw is running on your Mac and both devices are on the same Wi-Fi."
    )
    e is retrofit2.HttpException -> SheetState.Error(
        title = "Server error (${e.code()})",
        hint = e.message ?: "Something went wrong on the server."
    )
    else -> SheetState.Error(
        title = "Something went wrong",
        hint = e.message ?: "Unknown error"
    )
}

// ── Sheet ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationSheet(
    onLoad: suspend () -> SnapTaskResponse,
    onExecute: (SnapTaskResponse) -> Unit,
    onDismiss: () -> Unit,
) {
    var state: SheetState by remember { mutableStateOf(SheetState.Loading) }

    LaunchedEffect(Unit) {
        state = try {
            SheetState.Ready(onLoad())
        } catch (e: Exception) {
            errorStateFrom(e)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val s = state) {
                is SheetState.Loading -> LoadingContent()
                is SheetState.Ready -> ReadyContent(s.response, onExecute, onDismiss)
                is SheetState.Error -> ErrorContent(s, onDismiss)
            }
        }
    }
}

// ── Loading ────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text("Reading your image…", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "OCR → AI classification → action",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Ready ──────────────────────────────────────────────────────────────────

@Composable
private fun ReadyContent(
    response: SnapTaskResponse,
    onExecute: (SnapTaskResponse) -> Unit,
    onDismiss: () -> Unit,
) {
    val lowConfidence = response.confidence in 0.0f..0.59f
    var isEditing by remember { mutableStateOf(false) }

    // Mutable edited params per action (index → fieldKey → value)
    val editedParams = remember(response) {
        response.actions.map { action ->
            mutableStateMapOf<String, String>().also { map ->
                editableFields(action.type).forEach { key ->
                    map[key] = action.params[key]?.toString() ?: ""
                }
            }
        }
    }

    // Intent badge + summary
    IntentBadge(intent = response.intent)
    Text(response.summary, style = MaterialTheme.typography.titleMedium)

    // Confidence warning banner
    if (lowConfidence) {
        ConfidenceWarning(confidence = response.confidence)
    }

    // Actions
    if (response.actions.isNotEmpty()) {
        Divider()
        response.actions.forEachIndexed { i, action ->
            AnimatedVisibility(
                visible = isEditing,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                EditableActionFields(action, editedParams[i])
            }
            AnimatedVisibility(visible = !isEditing) {
                ActionRow(action)
            }
        }
        TextButton(
            onClick = { isEditing = !isEditing },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = if (isEditing) "Done editing" else "Edit details",
                fontSize = 13.sp
            )
        }
    }

    // Buttons
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Cancel")
        }
        Button(
            onClick = {
                val updatedResponse = if (isEditing) {
                    response.copy(actions = response.actions.mapIndexed { i, action ->
                        applyEdits(action, editedParams[i])
                    })
                } else response
                onExecute(updatedResponse)
            },
            modifier = Modifier.weight(1f),
            colors = if (lowConfidence) ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD97706)
            ) else ButtonDefaults.buttonColors()
        ) {
            Text(if (lowConfidence) "Execute anyway" else "Execute")
        }
    }
}

// ── Confidence warning ─────────────────────────────────────────────────────

@Composable
private fun ConfidenceWarning(confidence: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠️", fontSize = 16.sp)
        Column {
            Text(
                text = "Not fully confident (${(confidence * 100).toInt()}%)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFF92400E)
            )
            Text(
                text = "Review the details before saving.",
                fontSize = 12.sp,
                color = Color(0xFFB45309)
            )
        }
    }
}

// ── Editable fields ────────────────────────────────────────────────────────

@Composable
private fun EditableActionFields(
    action: PlannedAction,
    edits: MutableMap<String, String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        editableFields(action.type).forEach { key ->
            OutlinedTextField(
                value = edits[key] ?: "",
                onValueChange = { edits[key] = it },
                label = { Text(fieldLabel(key)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = key != "body",
                keyboardOptions = when (key) {
                    "amount" -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    "phone" -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                    else -> KeyboardOptions.Default
                }
            )
        }
    }
}

// ── Error ──────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(error: SheetState.Error, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(error.title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = error.hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Dismiss")
        }
    }
}

// ── Action row (read-only) ─────────────────────────────────────────────────

@Composable
private fun ActionRow(action: PlannedAction) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("•", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary)
        Text(actionLabel(action), style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun editableFields(actionType: String): List<String> = when (actionType) {
    "create_calendar_event" -> listOf("title", "dateTime", "location")
    "create_contact" -> listOf("name", "phone", "email", "company")
    "create_note" -> listOf("title", "body")
    "log_expense" -> listOf("merchant", "amount", "currency", "date")
    else -> emptyList()
}

private fun fieldLabel(key: String): String = when (key) {
    "dateTime" -> "Date & Time (ISO)"
    "company" -> "Company"
    else -> key.replaceFirstChar { it.uppercase() }
}

private fun applyEdits(action: PlannedAction, edits: Map<String, String>): PlannedAction {
    val updated = action.params.toMutableMap()
    edits.forEach { (key, value) ->
        if (value.isNotBlank()) {
            updated[key] = when (val orig = action.params[key]) {
                is Double -> value.toDoubleOrNull() ?: value
                is Float -> value.toFloatOrNull() ?: value
                is Int -> value.toIntOrNull() ?: value
                else -> value
            }
        }
    }
    return action.copy(params = updated)
}

private fun actionLabel(action: PlannedAction): String = when (action.type) {
    "create_calendar_event" -> buildString {
        append(action.params["title"] as? String ?: "Event")
        (action.params["dateTime"] as? String)?.let { append(" — $it") }
        (action.params["location"] as? String)?.let { append(", $it") }
    }
    "create_contact" -> buildString {
        append(action.params["name"] as? String ?: "Contact")
        val details = listOfNotNull(
            action.params["phone"] as? String,
            action.params["email"] as? String,
            action.params["company"] as? String
        )
        if (details.isNotEmpty()) append(" (${details.joinToString(", ")})")
    }
    "create_note" -> "Note: ${action.params["title"] as? String ?: "Untitled"}"
    "log_expense" -> buildString {
        val merchant = action.params["merchant"] as? String ?: "Expense"
        val currency = action.params["currency"] as? String ?: "USD"
        val amount = action.params["amount"]?.toString() ?: ""
        append("$merchant — $currency $amount")
    }
    else -> action.type.replace('_', ' ')
}
