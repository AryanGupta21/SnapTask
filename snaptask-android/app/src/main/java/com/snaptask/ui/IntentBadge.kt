package com.snaptask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IntentBadge(intent: String) {
    val (label, bg, fg) = when (intent.lowercase()) {
        "event",   "mixed" -> Triple("EVENT",   Color(0xFF1D4ED8), Color(0xFF93C5FD))
        "contact"          -> Triple("CONTACT", Color(0xFF166534), Color(0xFF86EFAC))
        "note"             -> Triple("NOTE",    Color(0xFF9A3412), Color(0xFFFDBA74))
        "expense"          -> Triple("EXPENSE", Color(0xFF6B21A8), Color(0xFFD8B4FE))
        else               -> Triple(intent.uppercase(), Color(0xFF1F2937), Color(0xFF9CA3AF))
    }
    Text(
        text = label,
        color = fg,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
