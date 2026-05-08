package com.snaptask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
        hint  = "Try better lighting or hold the camera closer."
    )
    e is java.net.ConnectException || e is java.net.SocketTimeoutException -> SheetState.Error(
        title = "Can't reach the server",
        hint  = "Make sure OpenClaw is running on your Mac and both devices are on the same Wi-Fi."
    )
    e is retrofit2.HttpException -> SheetState.Error(
        title = "Server error (${e.code()})",
        hint  = e.message ?: "Something went wrong on the server."
    )
    else -> SheetState.Error(
        title = "Something went wrong",
        hint  = e.message ?: "Unknown error"
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
        // Animated transition between Loading → Ready/Error
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 })
                    .togetherWith(fadeOut(tween(150)))
            },
            label = "sheetState"
        ) { s ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (s) {
                    is SheetState.Loading -> LoadingContent()
                    is SheetState.Ready   -> ReadyContent(s.response, onExecute, onDismiss)
                    is SheetState.Error   -> ErrorContent(s, onDismiss)
                }
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
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp)
        Text(
            text = "Reading your image…",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "OCR  →  AI classification  →  action",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.3.sp
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

    val editedParams = remember(response) {
        response.actions.map { action ->
            androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>().also { map ->
                editableFields(action.type).forEach { key ->
                    map[key] = action.params[key]?.toString() ?: ""
                }
            }
        }
    }

    // Header
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IntentBadge(intent = response.intent)
        Text(
            text = "${(response.confidence * 100).toInt()}%",
            fontSize = 12.sp,
            color = if (lowConfidence) Color(0xFFD97706) else Color(0xFF22C55E),
            fontWeight = FontWeight.SemiBold
        )
    }

    Text(
        text = response.summary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    )

    if (lowConfidence) {
        ConfidenceWarning(confidence = response.confidence)
    }

    // Actions
    if (response.actions.isNotEmpty()) {
        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // Single-action gets a larger card; multiple get compact rows
        if (response.actions.size == 1) {
            val action = response.actions[0]
            AnimatedContent(
                targetState = isEditing,
                transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
                label = "singleAction"
            ) { editing ->
                if (editing) {
                    EditableActionFields(action, editedParams[0])
                } else {
                    SingleActionCard(action)
                }
            }
        } else {
            response.actions.forEachIndexed { i, action ->
                AnimatedVisibility(
                    visible = isEditing,
                    enter = expandVertically(),
                    exit  = shrinkVertically()
                ) { EditableActionFields(action, editedParams[i]) }
                AnimatedVisibility(visible = !isEditing) {
                    ActionRow(action)
                }
            }
        }

        TextButton(
            onClick = { isEditing = !isEditing },
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End)
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
                val updated = if (isEditing) {
                    response.copy(actions = response.actions.mapIndexed { i, action ->
                        applyEdits(action, editedParams[i])
                    })
                } else response
                onExecute(updated)
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

// ── Single-action card (larger display for solo results) ───────────────────

@Composable
private fun SingleActionCard(action: PlannedAction) {
    val (accentColor, icon) = when (action.type) {
        "create_calendar_event" -> Color(0xFF3B82F6) to "📅"
        "create_contact"        -> Color(0xFF22C55E) to "👤"
        "create_note"           -> Color(0xFFF97316) to "📝"
        "log_expense"           -> Color(0xFFA855F7) to "💳"
        else                    -> MaterialTheme.colorScheme.primary to "•"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Text(
            text = actionLabel(action),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
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
private fun EditableActionFields(action: PlannedAction, edits: MutableMap<String, String>) {
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
                    "phone"  -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                    else     -> KeyboardOptions.Default
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
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("😕", fontSize = 40.sp)
        Text(error.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = error.hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Dismiss")
        }
    }
}

// ── Action row (multi-action compact) ─────────────────────────────────────

@Composable
private fun ActionRow(action: PlannedAction) {
    val icon = when (action.type) {
        "create_calendar_event" -> "📅"
        "create_contact"        -> "👤"
        "create_note"           -> "📝"
        "log_expense"           -> "💳"
        else                    -> "•"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(icon, fontSize = 16.sp)
        Text(actionLabel(action), style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun editableFields(actionType: String): List<String> = when (actionType) {
    "create_calendar_event" -> listOf("title", "dateTime", "location")
    "create_contact"        -> listOf("name", "phone", "email", "company")
    "create_note"           -> listOf("title", "body")
    "log_expense"           -> listOf("merchant", "amount", "currency", "date")
    else -> emptyList()
}

private fun fieldLabel(key: String): String = when (key) {
    "dateTime" -> "Date & Time (ISO)"
    "company"  -> "Company"
    else       -> key.replaceFirstChar { it.uppercase() }
}

private fun applyEdits(action: PlannedAction, edits: Map<String, String>): PlannedAction {
    val updated = action.params.toMutableMap()
    edits.forEach { (key, value) ->
        if (value.isNotBlank()) {
            updated[key] = when (action.params[key]) {
                is Double -> value.toDoubleOrNull() ?: value
                is Float  -> value.toFloatOrNull()  ?: value
                is Int    -> value.toIntOrNull()    ?: value
                else      -> value
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
    "create_note"    -> "Note: ${action.params["title"] as? String ?: "Untitled"}"
    "log_expense"    -> buildString {
        val merchant = action.params["merchant"] as? String ?: "Expense"
        val currency = action.params["currency"] as? String ?: "USD"
        val amount   = action.params["amount"]?.toString() ?: ""
        append("$merchant — $currency $amount")
    }
    else -> action.type.replace('_', ' ')
}
