package com.snaptask.samsung

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

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
        Log.d(TAG, "startMs=$startMs")

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

        val calendarId = getDefaultCalendarId()
        if (calendarId == null) {
            Log.e(TAG, "No calendar account found — add a Google account on the device")
            return
        }
        Log.d(TAG, "Using calendarId=$calendarId")

        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endMs)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            location?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
        }

        val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        if (eventUri == null) {
            Log.e(TAG, "contentResolver.insert returned null — insert failed")
            return
        }
        Log.d(TAG, "Event created: $eventUri")

        val eventId = eventUri.lastPathSegment?.toLongOrNull()
        if (eventId == null) {
            Log.w(TAG, "Could not parse eventId from URI: $eventUri")
            return
        }

        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, reminderMinutes)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        Log.d(TAG, "Reminder set: ${reminderMinutes}min before")
    }

    private fun getDefaultCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.ACCOUNT_NAME)

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection,
            "${CalendarContract.Calendars.IS_PRIMARY} = 1", null, null
        )?.use {
            Log.d(TAG, "Primary calendar query: ${it.count} rows")
            if (it.moveToFirst()) return it.getLong(0)
        }

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection,
            "${CalendarContract.Calendars.VISIBLE} = 1", null, null
        )?.use {
            Log.d(TAG, "Visible calendar query: ${it.count} rows")
            if (it.moveToFirst()) return it.getLong(0)
        }

        return null
    }
}
