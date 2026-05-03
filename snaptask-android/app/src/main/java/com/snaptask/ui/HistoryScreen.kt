package com.snaptask.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.ActionHistory

private val BG = Color(0xFF0D0D14)
private val MUTED = Color(0xFF6B7280)
private val SURFACE = Color(0xFF16161F)

@Composable
fun HistoryScreen(
    context: Context,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    var blips by remember { mutableStateOf(ActionHistory.load(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = MUTED, fontSize = 14.sp)
            }
            Text(
                text = "History",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            if (blips.isNotEmpty()) {
                TextButton(onClick = {
                    ActionHistory.clear(context)
                    blips = emptyList()
                    onClear()
                }) {
                    Text("Clear", color = MUTED, fontSize = 14.sp)
                }
            } else {
                Spacer(Modifier.width(64.dp))
            }
        }

        if (blips.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No actions yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("Snap something to get started.", color = MUTED, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(blips) { blip ->
                    HistoryRow(blip)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(blip: ActionHistory.Blip) {
    val (badgeLabel, badgeColor) = when (blip.type) {
        "create_calendar_event" -> "EVENT"   to Color(0xFF1565C0)
        "create_contact"        -> "CONTACT" to Color(0xFF2E7D32)
        "create_note"           -> "NOTE"    to Color(0xFFE65100)
        "log_expense"           -> "EXPENSE" to Color(0xFF6A1B9A)
        else                    -> blip.type.uppercase() to Color(0xFF424242)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SURFACE, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = badgeLabel,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(badgeColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = blip.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Text(
                text = relativeTime(blip.timestamp),
                color = MUTED,
                fontSize = 12.sp
            )
        }
    }
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L        -> "just now"
        diff < 3_600_000L     -> "${diff / 60_000}m ago"
        diff < 86_400_000L    -> "${diff / 3_600_000}h ago"
        diff < 604_800_000L   -> "${diff / 86_400_000}d ago"
        else                  -> "${diff / 604_800_000}w ago"
    }
}
