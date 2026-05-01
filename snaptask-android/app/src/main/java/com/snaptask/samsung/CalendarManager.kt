package com.snaptask.samsung

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class CalendarManager(private val context: Context) {

    fun create(params: Map<String, Any>) {
        val title = params["title"] as? String ?: return
        val dateTime = params["dateTime"] as? String ?: return
        val location = params["location"] as? String
        val reminderMinutes = (params["reminderMinutes"] as? Double)?.toInt() ?: 60

        val startMs = runCatching { Instant.parse(dateTime).toEpochMilli() }
            .getOrElse { LocalDateTime.parse(dateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        val endMs = (params["endDateTime"] as? String)
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: (startMs + 60 * 60 * 1000)

        val calendarId = getDefaultCalendarId() ?: return

        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endMs)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            location?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
        }

        val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: return

        val eventId = eventUri.lastPathSegment?.toLongOrNull() ?: return
        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, reminderMinutes)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
    }

    private fun getDefaultCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)

        // Prefer the primary calendar (IS_PRIMARY = 1)
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.IS_PRIMARY} = 1",
            null,
            null
        )?.use { if (it.moveToFirst()) return it.getLong(0) }

        // Fall back to any visible calendar
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            null
        )?.use { if (it.moveToFirst()) return it.getLong(0) }

        return null
    }
}
