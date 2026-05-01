package com.snaptask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun IntentBadge(intent: String) {
    val (label, color) = when (intent.lowercase()) {
        "event"   -> Pair("EVENT",   Color(0xFF1565C0))
        "contact" -> Pair("CONTACT", Color(0xFF2E7D32))
        "note"    -> Pair("NOTE",    Color(0xFFE65100))
        "expense" -> Pair("EXPENSE", Color(0xFF6A1B9A))
        else      -> Pair(intent.uppercase(), Color(0xFF424242))
    }

    Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
