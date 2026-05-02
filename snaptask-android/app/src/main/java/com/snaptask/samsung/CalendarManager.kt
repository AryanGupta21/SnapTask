package com.snaptask.samsung

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private const val TAG = "CalendarManager"

class CalendarManager(private val context: Context) {

    fun create(params: Map<String, Any>) {
        Log.d(TAG, "create() called with params: $params")

        val title = params["title"] as? String
        val dateTime = params["dateTime"] as? String

        if (title == null) { Log.e(TAG, "Missing title"); return }
        if (dateTime == null) { Log.e(TAG, "Missing dateTime"); return }

        val startMs = runCatching { Instant.parse(dateTime).toEpochMilli() }
            .getOrElse {
                runCatching {
                    LocalDateTime.parse(dateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrElse {
                    Log.e(TAG, "Failed to parse dateTime: $dateTime", it)
                    return
                }
            }

        val endMs = (params["endDateTime"] as? String)
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: (startMs + 60 * 60 * 1000)

        val location = params["location"] as? String
        val reminderMinutes = when (val r = params["reminderMinutes"]) {
            is Double -> r.toInt()
            is Int -> r
            is Long -> r.toInt()
            else -> 60
        }

        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        location?.let { intent.putExtra(CalendarContract.Events.EVENT_LOCATION, it) }

        Log.d(TAG, "Launching Calendar intent for: $title at $dateTime")
        context.startActivity(intent)
    }
}
