# SnapTask — Samsung Prism Hackathon

> Point your camera at anything with text. SnapTask reads it, understands it, and acts on it — creating calendar events, saving contacts, logging expenses, and writing notes automatically.

---

## Problem

Every time you photograph a flyer, business card, receipt, or whiteboard, your phone stores a JPEG and does nothing. The action implied by that image — *add this event*, *save this contact*, *log this expense* — is left entirely to you.

Manual re-entry is slow, error-prone, and completely unnecessary when the data is already in the photo.

---

## Solution

SnapTask closes that gap with a three-step pipeline:

```
[Camera / Gallery]
        │
        ▼
[ML Kit OCR]  ──── rawText ────▶  [OpenClaw Server]
  (on-device)                      Llama 3.3 70B (Groq)
                                   classifies intent +
                                   extracts parameters
        │
        ▼
[Confirmation Sheet]
  Review & edit before executing
        │
        ▼
[Samsung APIs]
  Calendar · Contacts · Notes
```

One photo. Zero re-typing. A single confirmation tap.

**Privacy:** The raw image never leaves the device. Only extracted text is sent — over local Wi-Fi to your own server, not to any third-party cloud.

---

## Features

| Feature | Status |
|---|---|
| Camera + gallery launcher | ✅ |
| On-device OCR (ML Kit Text Recognition) | ✅ |
| Llama 3.3 70B intent classification (via Groq) | ✅ |
| **Multi-action from one image** (e.g. business card → contact + calendar event) | ✅ |
| Swipeable 3-page onboarding (shown every launch) | ✅ |
| Confirmation sheet with preview before executing | ✅ |
| Confidence warning banner (< 60% confidence) | ✅ |
| Editable action fields before confirming | ✅ |
| Friendly typed error states (no text, network, server) | ✅ |
| Create calendar event (Samsung / Google Calendar) | ✅ |
| Save contact (ContactsContract) | ✅ |
| Create note (Samsung Notes via share intent) | ✅ |
| Log expense (Samsung Notes) | ✅ |
| Action history screen with date grouping | ✅ |
| Adaptive app icon | ✅ |

---

## Tech Stack

### Android App
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| OCR | Google ML Kit Text Recognition v2 |
| Networking | Retrofit 2 + OkHttp + Gson |
| Concurrency | Kotlin Coroutines |
| Samsung APIs | CalendarContract, ContactsContract, Samsung Notes SDK |
| Min SDK | API 26 (Android 8.0) |

### OpenClaw Server
| Layer | Technology |
|---|---|
| Runtime | Node.js 18+ |
| LLM | Llama 3.3 70B via Groq (`groq-sdk`) — free, no billing |
| Skill system | SKILL.md + `index.js` per integration |
| Transport | HTTP/JSON on port 3000 |

---

## Repository Structure

```
SnapTask/
├── snaptask-android/               # Kotlin Android app
│   └── app/src/main/java/com/snaptask/
│       ├── MainActivity.kt             # Home screen + camera/gallery launcher
│       ├── ShareReceiverActivity.kt    # Share-sheet entry point + action executor
│       ├── ActionHistory.kt            # SharedPrefs-based action log
│       ├── network/
│       │   ├── OpenClawClient.kt
│       │   └── models/                 # SnapTaskRequest/Response, PlannedAction
│       ├── ocr/
│       │   └── MLKitOCRProcessor.kt
│       ├── samsung/
│       │   ├── CalendarManager.kt
│       │   ├── ContactsManager.kt
│       │   └── NotesManager.kt
│       └── ui/
│           ├── OnboardingScreen.kt     # 3-page swipeable onboarding
│           ├── ConfirmationSheet.kt    # Bottom sheet: loading / ready / error
│           ├── HistoryScreen.kt        # Date-grouped action history
│           └── IntentBadge.kt
│
├── openclaw-server/                # Node.js gateway (run on your Mac)
│   ├── server.js
│   ├── package.json
│   └── .env.example
│
└── openclaw-skills/                # One folder per skill
    ├── samsung-calendar/
    ├── samsung-contacts/
    ├── samsung-notes/
    └── expense-logger/
```

---

## Setup

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog or newer |
| Node.js | 18+ |
| A Groq API key | [Get one free at console.groq.com](https://console.groq.com) — no billing required |

---

### Step 1 — Start the OpenClaw server (Mac)

```bash
cd openclaw-server
npm install

# Create your .env from the example
cp .env.example .env
# Edit .env and paste your Groq API key:
# GROQ_API_KEY=gsk_...

node server.js
# → OpenClaw Gateway listening on http://localhost:3000
```

---

### Step 2 — Find your Mac's local IP

```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
# note the 192.168.x.x address
```

---

### Step 3 — Configure the Android app

Open `snaptask-android/app/src/main/java/com/snaptask/network/OpenClawClient.kt` and set:

```kotlin
private const val BASE_URL = "http://192.168.x.x:3000/"  // ← your IP
```

Make sure your phone and Mac are on the **same Wi-Fi network**.

---

### Step 4 — Build and install

1. Place `samsung-notes-sdk.aar` (from the Samsung Developers Portal) into `snaptask-android/app/libs/`.
2. Connect your Samsung Galaxy device in Developer Mode.
3. Open `snaptask-android/` in Android Studio and run the `app` configuration.

---

## Usage

### Snapping from the app
1. Open **SnapTask** — swipe through the onboarding and tap **Get Started**.
2. Tap the **📸 SNAP** button to open the camera, or **📷 Gallery** to pick an image.
3. Capture or select an image with readable text.
4. The confirmation sheet slides up — review the detected action(s).
5. Optionally tap **Edit details** to adjust any fields.
6. Tap **Execute** to save, or **Cancel** to dismiss.

### Snapping from any app
1. Take a photo in Samsung Camera (or any app).
2. Tap **Share** → select **SnapTask**.
3. Same confirmation flow as above.

### Viewing history
- Tap **🕐 History** on the home screen to open the history screen.
- Actions are grouped by Today / Yesterday / This Week / Earlier.
- Tap **Clear** to wipe the history.

---

## Supported Actions

| Image content | Actions triggered |
|---|---|
| Business card | Save contact + optionally create a follow-up calendar event |
| Event flyer / poster | Create calendar event (title, date, time, location) |
| Receipt | Log expense (merchant, amount, currency, date) |
| Whiteboard / notes | Create note with title and body |
| Mixed content | Multiple actions from a single image |

---

## How Multi-Action Works

The Llama prompt explicitly instructs the model to return **all applicable actions** for the image, not just one. A single business card can produce:

```json
{
  "actions": [
    { "skill": "samsung-contacts", "params": { "name": "Jane Smith", "phone": "+1 415 000 0000" } },
    { "skill": "samsung-calendar", "params": { "title": "Call with Jane", "date": "2026-05-15", "time": "14:00" } }
  ]
}
```

Both actions appear in the confirmation sheet and execute together on a single tap.

---

## Privacy

1. Photo stays on device as a local file.
2. ML Kit reads pixels from the URI — returns text only, the image is never forwarded.
3. Only `rawText` is sent over local Wi-Fi to your OpenClaw server — never to any third-party.
4. Llama 3.3 on Groq processes the text and returns structured JSON — no image data involved.
5. The Android app writes directly to Calendar / Contacts / Notes — fully local.

**The raw image never leaves your device.**
