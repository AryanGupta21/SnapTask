package com.snaptask.samsung

import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "NotesManager"
private const val SAMSUNG_NOTES_PKG = "com.samsung.android.app.notes"
private const val SAMSUNG_NOTES_ACTIVITY = "com.samsung.android.app.notes.ui.QuickNoteActivity"

class NotesManager(private val context: Context) {

    fun create(params: Map<String, Any>) {
        val title = params["title"] as? String ?: "SnapTask Import"
        val body = params["body"] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val checklist = params["checklist"] as? List<String>

        val content = if (checklist != null) {
            checklist.joinToString("\n") { "☐ $it" }
        } else {
            if (body.isNotBlank()) "$title\n\n$body" else title
        }

        Log.d(TAG, "Creating note: $title")

        // Try Samsung Notes first, fall back to any notes/send handler
        if (isSamsungNotesInstalled()) {
            launchSamsungNotes(title, content)
        } else {
            launchGenericNote(title, content)
        }
    }

    fun logExpense(params: Map<String, Any>) {
        val merchant = params["merchant"] as? String ?: "Unknown"
        val amount = params["amount"]?.toString() ?: "0"
        val currency = params["currency"] as? String ?: "USD"
        val date = params["date"] as? String ?: ""
        val category = params["category"] as? String ?: "General"

        val body = "Merchant: $merchant\nAmount: $currency $amount\nDate: $date\nCategory: $category"
        create(mapOf("title" to "Expense: $merchant", "body" to body))
    }

    private fun isSamsungNotesInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SAMSUNG_NOTES_PKG, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchSamsungNotes(title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(SAMSUNG_NOTES_PKG)
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        Log.d(TAG, "Launching Samsung Notes")
        context.startActivity(intent)
    }

    private fun launchGenericNote(title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        Log.d(TAG, "Samsung Notes not found, launching share chooser")
        context.startActivity(Intent.createChooser(intent, "Save note to…").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
