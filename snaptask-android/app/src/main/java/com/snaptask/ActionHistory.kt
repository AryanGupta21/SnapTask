package com.snaptask

import android.content.Context
import kotlin.math.cos
import kotlin.math.sin

object ActionHistory {
    private const val PREFS = "snaptask_history"
    private const val KEY = "blips"
    private const val MAX = 20

    data class Blip(
        val type: String,
        val x: Float,  // normalized -0.8..0.8 within radar circle
        val y: Float,
        val timestamp: Long,
    )

    fun record(context: Context, actionType: String) {
        val existing = load(context).toMutableList()
        val angle = Math.random() * 2 * Math.PI
        val radius = 0.2f + Math.random().toFloat() * 0.6f
        existing.add(
            0, Blip(
                type = actionType,
                x = (cos(angle) * radius).toFloat(),
                y = (sin(angle) * radius).toFloat(),
                timestamp = System.currentTimeMillis()
            )
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, existing.take(MAX).joinToString("|") {
                "${it.type},${it.x},${it.y},${it.timestamp}"
            }).apply()
    }

    fun load(context: Context): List<Blip> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { item ->
            val p = item.split(",")
            if (p.size == 4) runCatching {
                Blip(p[0], p[1].toFloat(), p[2].toFloat(), p[3].toLong())
            }.getOrNull() else null
        }
    }
}
