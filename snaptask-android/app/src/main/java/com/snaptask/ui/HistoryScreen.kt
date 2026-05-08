package com.snaptask.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import com.snaptask.ui.theme.*
import java.util.Calendar

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
            .background(ColorBg)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("← Back", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Text(
                text = "History",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (blips.isNotEmpty()) {
                TextButton(
                    onClick = {
                        ActionHistory.clear(context)
                        blips = emptyList()
                        onClear()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                Spacer(Modifier.width(64.dp))
            }
        }

        Divider(color = ColorBorder, thickness = 1.dp)

        if (blips.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            ) {
                sections.forEach { section ->
                    item {
                        Text(
                            text = section.header,
                            color = ColorMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp, start = 4.dp)
                        )
                    }
                    items(section.blips) { blip ->
                        HistoryRow(blip)
                        Spacer(Modifier.height(8.dp))
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text("📋", fontSize = 52.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Nothing here yet",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Actions you execute will appear here — calendar events, contacts, notes, and expenses.",
                color = ColorMuted,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistoryRow(blip: ActionHistory.Blip) {
    val (accentColor, typeLabel, icon) = when (blip.type) {
        "create_calendar_event" -> Triple(Color(0xFF3B82F6), "Calendar Event", "📅")
        "create_contact"        -> Triple(Color(0xFF22C55E), "Contact",        "👤")
        "create_note"           -> Triple(Color(0xFFF97316), "Note",           "📝")
        "log_expense"           -> Triple(Color(0xFFA855F7), "Expense",        "💳")
        else                    -> Triple(Color(0xFF6B7280), blip.type.replace('_', ' '), "•")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .heightIn(min = 64.dp)
                .fillMaxHeight()
                .background(
                    accentColor,
                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
        )

        // Icon
        Box(
            modifier = Modifier
                .padding(start = 14.dp)
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = blip.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 19.sp
            )
            Text(
                text = typeLabel,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Column(
            modifier = Modifier.padding(end = 14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = relativeTime(blip.timestamp),
                color = ColorMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun groupByDate(blips: List<ActionHistory.Blip>): List<Section> {
    if (blips.isEmpty()) return emptyList()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val todayStart     = cal.timeInMillis
    val yesterdayStart = todayStart - 86_400_000L
    val weekStart      = todayStart - 6 * 86_400_000L

    return buildList {
        blips.filter { it.timestamp >= todayStart }.takeIf { it.isNotEmpty() }
            ?.let { add(Section("TODAY", it)) }
        blips.filter { it.timestamp in yesterdayStart until todayStart }.takeIf { it.isNotEmpty() }
            ?.let { add(Section("YESTERDAY", it)) }
        blips.filter { it.timestamp in weekStart until yesterdayStart }.takeIf { it.isNotEmpty() }
            ?.let { add(Section("THIS WEEK", it)) }
        blips.filter { it.timestamp < weekStart }.takeIf { it.isNotEmpty() }
            ?.let { add(Section("EARLIER", it)) }
    }
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L      -> "just now"
        diff < 3_600_000L   -> "${diff / 60_000}m ago"
        diff < 86_400_000L  -> "${diff / 3_600_000}h ago"
        diff < 604_800_000L -> "${diff / 86_400_000}d ago"
        else                -> "${diff / 604_800_000}w ago"
    }
}
