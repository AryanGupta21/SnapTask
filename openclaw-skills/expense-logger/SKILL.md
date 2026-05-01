name: expense-logger
description: Logs an expense entry to Samsung Notes from a receipt or invoice photo
trigger: when the captured text looks like a receipt, invoice, or bill — typically contains a merchant name, a monetary amount, and a date
parameters:
  - merchant: string (store or vendor name)
  - amount: number (total amount as a float)
  - currency: string (3-letter ISO code, default "USD")
  - date: string (ISO 8601 date of the transaction)
  - category: string (optional, e.g. "Food", "Travel", "Office")
examples:
  - input: "Starbucks Coffee\nDate: 20 Apr 2026\nTotal: $6.75"
    output: { merchant: "Starbucks Coffee", amount: 6.75, currency: "USD", date: "2026-04-20", category: "Food" }
  - input: "UBER TRIP\n₹320.00\n19-Apr-2026"
    output: { merchant: "Uber", amount: 320.00, currency: "INR", date: "2026-04-19", category: "Travel" }
