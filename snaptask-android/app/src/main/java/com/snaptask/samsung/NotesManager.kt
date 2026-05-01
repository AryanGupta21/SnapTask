package com.snaptask.samsung

import android.content.Context

class NotesManager(private val context: Context) {

    fun create(params: Map<String, Any>) {
        val title = params["title"] as? String ?: "SnapTask Import"
        val body = params["body"] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val checklist = params["checklist"] as? List<String>

        // Samsung Notes SDK integration:
        // val note = SNote().apply {
        //     this.title = title
        //     if (checklist != null) {
        //         val page = SNotePage()
        //         checklist.forEach { item -> page.addChecklistItem(item) }
        //         addPage(page)
        //     } else {
        //         this.body = body
        //     }
        // }
        // SNoteController(context).createNote(note)

        // TODO: Replace stub above with Samsung Notes SDK calls once .aar is available.
        // Download the SDK from the Samsung Developers Portal and place it in app/libs/.
        android.util.Log.d("NotesManager", "Would create note: $title\n$body")
    }

    fun logExpense(params: Map<String, Any>) {
        val merchant = params["merchant"] as? String ?: "Unknown"
        val amount = params["amount"]?.toString() ?: "0"
        val currency = params["currency"] as? String ?: "USD"
        val date = params["date"] as? String ?: ""
        val category = params["category"] as? String ?: "Expense"

        val body = """
            Merchant: $merchant
            Amount: $currency $amount
            Date: $date
            Category: $category
        """.trimIndent()

        create(mapOf("title" to "Expense: $merchant", "body" to body))
    }
}
