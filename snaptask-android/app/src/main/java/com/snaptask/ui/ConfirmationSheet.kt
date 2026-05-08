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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.network.models.PlannedAction
import com.snaptask.network.models.SnapTaskResponse
import com.snaptask.ui.theme.*

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
        state = try { SheetState.Ready(onLoad()) } catch (e: Exception) { errorStateFrom(e) }
    }

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor    = ColorSurface,
        contentColor      = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 5 })
                    .togetherWith(fadeOut(tween(150)))
            },
            label = "sheetState"
        ) { s ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
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
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(color = ColorAccentLight, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
        Text("Analysing your image…", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = "OCR  ›  AI classification  ›  action",
            color = ColorMuted,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
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
    val lowConfidence = response.confidence < 0.6f
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

    // Header pill row
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        IntentBadge(intent = response.intent)
        Spacer(Modifier.weight(1f))
        val pct = (response.confidence * 100).toInt()
        val confColor = if (lowConfidence) Color(0xFFF59E0B) else Color(0xFF22C55E)
        Box(
            modifier = Modifier
                .background(confColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("$pct% confident", color = confColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    Text(
        text = response.summary,
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp
    )

    if (lowConfidence) ConfidenceWarning(response.confidence)

    if (response.actions.isNotEmpty()) {
        Divider(color = ColorBorder)

        if (response.actions.size == 1) {
            AnimatedContent(
                targetState = isEditing,
                transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
                label = "editToggle"
            ) { editing ->
                if (editing) EditableActionFields(response.actions[0], editedParams[0])
                else SingleActionCard(response.actions[0])
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                response.actions.forEachIndexed { i, action ->
                    AnimatedVisibility(isEditing, enter = expandVertically(), exit = shrinkVertically()) {
                        EditableActionFields(action, editedParams[i])
                    }
                    AnimatedVisibility(!isEditing) { MultiActionRow(action) }
                }
            }
        }

        TextButton(
            onClick = { isEditing = !isEditing },
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End)
        ) {
            Text(
                text = if (isEditing) "✓  Done editing" else "✏  Edit details",
                color = ColorAccentLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorMuted),
            border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorder)
        ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }

        Button(
            onClick = {
                val updated = if (isEditing) response.copy(
                    actions = response.actions.mapIndexed { i, a -> applyEdits(a, editedParams[i]) }
                ) else response
                onExecute(updated)
            },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = if (lowConfidence) ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                     else ButtonDefaults.buttonColors(containerColor = ColorAccent)
        ) {
            Text(
                if (lowConfidence) "Execute anyway" else "Execute",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ── Single action card ─────────────────────────────────────────────────────

@Composable
private fun SingleActionCard(action: PlannedAction) {
    val (color, icon) = actionMeta(action.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 24.sp) }
        Text(
            text = actionLabel(action),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 21.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Multi-action compact row ───────────────────────────────────────────────

@Composable
private fun MultiActionRow(action: PlannedAction) {
    val (color, icon) = actionMeta(action.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurfaceHigh, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 16.sp) }
        Text(actionLabel(action), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

// ── Confidence warning ─────────────────────────────────────────────────────

@Composable
private fun ConfidenceWarning(confidence: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF451A03).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠️", fontSize = 18.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Low confidence — ${(confidence * 100).toInt()}%",
                color = Color(0xFFFBBF24),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Review the details carefully before saving.", color = Color(0xFFFCD34D), fontSize = 12.sp)
        }
    }
}

// ── Editable fields ────────────────────────────────────────────────────────

@Composable
private fun EditableActionFields(action: PlannedAction, edits: MutableMap<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        editableFields(action.type).forEach { key ->
            OutlinedTextField(
                value = edits[key] ?: "",
                onValueChange = { edits[key] = it },
                label = { Text(fieldLabel(key), color = ColorMuted) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = key != "body",
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ColorAccentLight,
                    unfocusedBorderColor = ColorBorder,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = ColorAccentLight
                ),
                shape = RoundedCornerShape(12.dp),
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
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("😕", fontSize = 48.sp)
        Text(error.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            error.hint,
            color = ColorMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorSurfaceHigh)
        ) { Text("Dismiss", fontWeight = FontWeight.SemiBold) }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun actionMeta(type: String): Pair<Color, String> = when (type) {
    "create_calendar_event" -> Color(0xFF3B82F6) to "📅"
    "create_contact"        -> Color(0xFF22C55E) to "👤"
    "create_note"           -> Color(0xFFF97316) to "📝"
    "log_expense"           -> Color(0xFFA855F7) to "💳"
    else                    -> Color(0xFF6B7280)  to "•"
}

private fun editableFields(t: String) = when (t) {
    "create_calendar_event" -> listOf("title", "dateTime", "location")
    "create_contact"        -> listOf("name", "phone", "email", "company")
    "create_note"           -> listOf("title", "body")
    "log_expense"           -> listOf("merchant", "amount", "currency", "date")
    else -> emptyList()
}

private fun fieldLabel(key: String) = when (key) {
    "dateTime" -> "Date & Time (ISO)"; "company" -> "Company"
    else -> key.replaceFirstChar { it.uppercase() }
}

private fun applyEdits(action: PlannedAction, edits: Map<String, String>): PlannedAction {
    val updated = action.params.toMutableMap()
    edits.forEach { (k, v) ->
        if (v.isNotBlank()) updated[k] = when (action.params[k]) {
            is Double -> v.toDoubleOrNull() ?: v
            is Float  -> v.toFloatOrNull()  ?: v
            is Int    -> v.toIntOrNull()    ?: v
            else      -> v
        }
    }
    return action.copy(params = updated)
}

private fun actionLabel(action: PlannedAction) = when (action.type) {
    "create_calendar_event" -> buildString {
        append(action.params["title"] as? String ?: "Event")
        (action.params["dateTime"] as? String)?.let { append(" — $it") }
        (action.params["location"] as? String)?.let { append(", $it") }
    }
    "create_contact" -> buildString {
        append(action.params["name"] as? String ?: "Contact")
        val d = listOfNotNull(action.params["phone"] as? String, action.params["email"] as? String)
        if (d.isNotEmpty()) append(" (${d.joinToString(", ")})")
    }
    "create_note"    -> "Note: ${action.params["title"] as? String ?: "Untitled"}"
    "log_expense"    -> buildString {
        append(action.params["merchant"] as? String ?: "Expense")
        val amt = action.params["amount"]?.toString() ?: ""
        val cur = action.params["currency"] as? String ?: "USD"
        if (amt.isNotBlank()) append(" — $cur $amt")
    }
    else -> action.type.replace('_', ' ')
}
