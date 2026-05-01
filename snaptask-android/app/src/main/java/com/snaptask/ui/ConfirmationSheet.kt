package com.snaptask.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    BottomSheetScaffold(
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (val s = state) {
                    is SheetState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Reading image...", modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    is SheetState.Ready -> {
                        IntentBadge(intent = s.response.intent)
                        Text(
                            text = s.response.summary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        s.response.actions.forEach { action ->
                            Text(
                                text = "• ${action.type.replace('_', ' ')}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }
                            Button(onClick = { onExecute(s.response) }, modifier = Modifier.weight(1f)) {
                                Text("Execute")
                            }
                        }
                    }

                    is SheetState.Error -> {
                        Text("Could not process image", style = MaterialTheme.typography.titleMedium)
                        Text(s.message, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        },
        scaffoldState = rememberBottomSheetScaffoldState()
    ) {}
}
