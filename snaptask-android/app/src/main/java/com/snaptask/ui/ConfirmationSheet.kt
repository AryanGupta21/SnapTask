package com.snaptask.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snaptask.network.models.PlannedAction
import com.snaptask.network.models.SnapTaskResponse

sealed interface SheetState {
    data object Loading : SheetState
    data class Ready(val response: SnapTaskResponse) : SheetState
    data class Error(val message: String) : SheetState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationSheet(
    onLoad: suspend () -> SnapTaskResponse,
    onExecute: (SnapTaskResponse) -> Unit,
    onDismiss: () -> Unit
) {
    var state: SheetState by remember { mutableStateOf(SheetState.Loading) }

    LaunchedEffect(Unit) {
        state = try {
            SheetState.Ready(onLoad())
        } catch (e: Exception) {
            SheetState.Error(e.message ?: "Unknown error")
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
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val s = state) {
                is SheetState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text(
                        text = "Reading image…",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                is SheetState.Ready -> {
                    IntentBadge(intent = s.response.intent)
                    Text(
                        text = s.response.summary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (s.response.actions.isNotEmpty()) {
                        HorizontalDivider()
                        s.response.actions.forEach { action ->
                            ActionRow(action)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { onExecute(s.response) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Execute")
                        }
                    }
                }

                is SheetState.Error -> {
                    Text(
                        text = "Could not process image",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = s.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(action: PlannedAction) {
    val label = actionLabel(action)
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
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
