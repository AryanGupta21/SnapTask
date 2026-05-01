# Privacy Flow — Where Your Data Goes

SnapTask's key differentiator is that sensitive image data never leaves the device and no inference happens in the cloud. This document traces every data hop.

## Step-by-Step Data Flow

```
Step 1: Photo taken by Samsung Camera
        → stored locally on device as JPEG
        → image URI created (content://media/...)

Step 2: Share → SnapTask intercepts the URI
        → image is NOT copied
        → only the URI reference is passed via Intent extras

Step 3: ML Kit reads pixels from URI (on-device)
        → TextRecognizer processes image in-memory
        → returns String (text only)
        → image data is NEVER serialized or copied out

Step 4: ML Kit Entity Extraction (on-device)
        → takes the text string as input
        → returns typed entity list (dates, phones, emails, addresses)
        → no network call, no data leaves device

Step 5: Android app POSTs to OpenClaw (local Wi-Fi only)
        Payload: { "rawText": "...", "entities": [...] }
        → NO image bytes
        → NO file path
        → NO EXIF metadata
        → destination: 192.168.x.x:3000 (LAN only, not internet)

Step 6: OpenClaw sends text to Ollama
        → Ollama runs on the SAME MacBook as OpenClaw
        → call is localhost:11434 — never leaves the machine
        → no cloud API, no OpenAI, no Anthropic

Step 7: Ollama returns intent classification JSON
        → stays within MacBook

Step 8: OpenClaw returns PlannedAction JSON to Android app
        → structured data only, no raw text stored

Step 9: Android app shows confirmation card
        → user reviews before any write happens

Step 10: User taps Execute
         → Android app calls Samsung Calendar / Contacts / Notes
         → all ContentProvider / SDK calls are local and on-device
         → data written to Samsung's local databases
```

## What Each System Sees

| System | Sees | Does NOT see |
|---|---|---|
| ML Kit (on-device) | Raw image pixels (in-memory) | Nothing persisted |
| OpenClaw (MacBook LAN) | `rawText` string + entity list | Image, EXIF, file path |
| Ollama (localhost) | `rawText` string | Entities, device info |
| Samsung Calendar/Contacts/Notes | Structured event/contact/note data | Original text or image |
| Internet | Nothing | — |

## Network Boundary

```
Device  ──[Wi-Fi LAN]──  MacBook
                          ├── OpenClaw :3000
                          └── Ollama   :11434 (localhost only)

Internet: NO TRAFFIC FROM SNAPTASK
```

The `INTERNET` permission in `AndroidManifest.xml` is required for the LAN HTTP call to OpenClaw, but no traffic is routed to a public IP.
