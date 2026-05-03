package com.snaptask

import android.content.Context

object ActionHistory {
    private const val PREFS = "snaptask_history"
    private const val KEY = "blips_v2"
    private const val MAX = 50
    private const val SEP = "|||"

    data class Blip(
        val type: String,
        val label: String,
        val timestamp: Long,
    )

    fun record(context: Context, actionType: String, label: String) {
        val existing = load(context).toMutableList()
        existing.add(0, Blip(type = actionType, label = label, timestamp = System.currentTimeMillis()))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, existing.take(MAX).joinToString("\n") {
                "${it.type}$SEP${it.label}$SEP${it.timestamp}"
            }).apply()
    }

    fun load(context: Context): List<Blip> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split(SEP)
            if (parts.size == 3) runCatching {
                Blip(parts[0], parts[1], parts[2].toLong())
            }.getOrNull() else null
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY).apply()
    }
}
