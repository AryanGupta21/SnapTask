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
import java.util.Calendar

private val BG      = Color(0xFF0D0D14)
private val SURFACE = Color(0xFF13131C)
private val MUTED   = Color(0xFF6B7280)
private val DIVIDER = Color(0xFF1E1E2A)

private data class Section(val header: String, val blips: List<ActionHistory.Blip>)

@Composable
fun HistoryScreen(
    context: Context,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    var blips by remember { mutableStateOf(ActionHistory.load(context)) }
    val sections = remember(blips) { groupByDate(blips) }

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
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                Spacer(Modifier.width(72.dp))
            }
        }

        Divider(color = DIVIDER, thickness = 1.dp)

        if (blips.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sections.forEach { section ->
                    item {
                        Text(
                            text = section.header,
                            color = MUTED,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(section.blips) { blip ->
                        HistoryRow(blip)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text("📋", fontSize = 48.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Nothing here yet",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Actions you execute will appear here — calendar events, contacts, notes, and expenses.",
                color = MUTED,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistoryRow(blip: ActionHistory.Blip) {
    val accentColor = when (blip.type) {
        "create_calendar_event" -> Color(0xFF3B82F6)
        "create_contact"        -> Color(0xFF22C55E)
        "create_note"           -> Color(0xFFF97316)
        "log_expense"           -> Color(0xFFA855F7)
        else                    -> Color(0xFF6B7280)
    }
    val typeLabel = when (blip.type) {
        "create_calendar_event" -> "Event"
        "create_contact"        -> "Contact"
        "create_note"           -> "Note"
        "log_expense"           -> "Expense"
        else                    -> blip.type.replace('_', ' ')
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SURFACE, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(56.dp)
                .background(
                    accentColor,
                    RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = blip.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Text(
                text = typeLabel,
                color = accentColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = relativeTime(blip.timestamp),
            color = MUTED,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 14.dp)
        )
    }
}

private fun groupByDate(blips: List<ActionHistory.Blip>): List<Section> {
    if (blips.isEmpty()) return emptyList()

    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    val yesterdayStart = todayStart - 86_400_000L
    val weekStart = todayStart - 6 * 86_400_000L

    val today = blips.filter { it.timestamp >= todayStart }
    val yesterday = blips.filter { it.timestamp in yesterdayStart until todayStart }
    val thisWeek = blips.filter { it.timestamp in weekStart until yesterdayStart }
    val older = blips.filter { it.timestamp < weekStart }

    return buildList {
        if (today.isNotEmpty()) add(Section("TODAY", today))
        if (yesterday.isNotEmpty()) add(Section("YESTERDAY", yesterday))
        if (thisWeek.isNotEmpty()) add(Section("THIS WEEK", thisWeek))
        if (older.isNotEmpty()) add(Section("EARLIER", older))
    }
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L      -> "now"
        diff < 3_600_000L   -> "${diff / 60_000}m"
        diff < 86_400_000L  -> "${diff / 3_600_000}h"
        diff < 604_800_000L -> "${diff / 86_400_000}d"
        else                -> "${diff / 604_800_000}w"
    }
}
