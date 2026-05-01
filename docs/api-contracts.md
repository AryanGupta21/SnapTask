# API Contracts — Android ↔ OpenClaw

## POST /process

### Request

```json
{
  "rawText": "Clash of Claws 2026\n20 April, 4:40 PM\nSamsung Arena, Seoul",
  "entities": [
    { "type": "DATE_TIME", "text": "20 April, 4:40 PM", "value": "2026-04-20T16:40:00" },
    { "type": "ADDRESS",   "text": "Samsung Arena, Seoul" }
  ],
  "deviceInfo": "Samsung Galaxy"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `rawText` | string | yes | Full OCR output from ML Kit, newlines preserved |
| `entities` | array | yes | Structured entities from ML Kit Entity Extraction |
| `deviceInfo` | string | no | Device model for logging |

### Response

```json
{
  "intent": "event",
  "confidence": 0.94,
  "summary": "Create calendar event: Clash of Claws 2026",
  "actions": [
    {
      "type": "create_calendar_event",
      "params": {
        "title": "Clash of Claws 2026",
        "dateTime": "2026-04-20T16:40:00",
        "location": "Samsung Arena, Seoul",
        "reminderMinutes": 60
      }
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `intent` | string | `"event"`, `"contact"`, `"note"`, `"expense"` |
| `confidence` | float | 0.0–1.0; show warning UI below 0.6 |
| `summary` | string | Human-readable description for the confirmation card |
| `actions` | array | One or more `PlannedAction` objects |

## PlannedAction Types

### create_calendar_event
```json
{
  "type": "create_calendar_event",
  "params": {
    "title": "string",
    "dateTime": "ISO 8601 string",
    "endDateTime": "ISO 8601 string (optional)",
    "location": "string (optional)",
    "reminderMinutes": 60
  }
}
```

### create_contact
```json
{
  "type": "create_contact",
  "params": {
    "name": "string",
    "phone": "string (optional)",
    "email": "string (optional)",
    "company": "string (optional)"
  }
}
```

### create_note
```json
{
  "type": "create_note",
  "params": {
    "title": "string",
    "body": "string",
    "checklist": ["item 1", "item 2"]
  }
}
```

### log_expense
```json
{
  "type": "log_expense",
  "params": {
    "merchant": "string",
    "amount": 12.50,
    "currency": "USD",
    "date": "ISO 8601 date string",
    "category": "string (optional)"
  }
}
```

## Error Response

```json
{
  "error": "low_confidence",
  "message": "Could not determine intent from extracted text",
  "confidence": 0.31
}
```

| Error code | Meaning | Android handling |
|---|---|---|
| `low_confidence` | LLM uncertain | Show disambiguation UI |
| `ocr_too_short` | Text under 10 chars | Show "Could not read image" |
| `skill_not_found` | No matching skill | Show raw text to user |
