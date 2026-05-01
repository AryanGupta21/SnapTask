# SnapTask — Samsung Prism Hackathon

> Turn your camera into an action engine. Snap a photo. Watch it become a calendar event, contact, or note — entirely on-device.

---

## Problem Statement

Every time you photograph a flyer, business card, whiteboard, or poster, your phone stores a JPEG and does nothing else. The action implied by that image — _add this event_, _save this contact_, _log this receipt_ — is left entirely to you.

SnapTask closes that gap. It intercepts the photo through Android's share sheet, reads the text on-device with ML Kit, sends only that text to a local AI gateway (OpenClaw) for intent classification, and executes the right Samsung API — all without the image ever leaving your device.

---

## How It Works

```
[Samsung Camera] ──share──▶ [SnapTask App]
                                   │
                            ML Kit OCR (on-device)
                                   │ rawText + entities
                                   ▼
                          [OpenClaw Gateway]  ◀── Ollama / llama3.2
                          (Node.js, MacBook)        (local LLM)
                                   │ PlannedAction JSON
                                   ▼
                          [SnapTask App] ──confirms──▶ [Samsung APIs]
                                                  Calendar / Contacts / Notes
```

**Privacy guarantee:** The raw image never leaves the device. Only extracted text is sent — over the local Wi-Fi network, never to the internet.

---

## Features

| Feature | Description |
|---|---|
| Share Sheet Integration | Registers as a share target for `image/*` — works with Samsung Camera and any other app |
| On-Device OCR | ML Kit Text Recognition v2 — no API key, no internet required |
| Entity Extraction | ML Kit Entity Extraction identifies dates, phone numbers, emails, and addresses before sending to the LLM |
| Local LLM Gateway | OpenClaw + Ollama (`llama3.2`) runs on MacBook Air M4 — no cloud inference |
| Intent Classification | LLM chooses the right Samsung API action: calendar event, contact, Samsung Note, or expense log |
| Confirmation Card | Jetpack Compose bottom sheet previews exactly what will be written before execution |
| Samsung API Execution | Writes to Samsung Calendar, Contacts (ContentProvider), and Samsung Notes (Samsung Notes SDK) |
| Privacy-First Architecture | Image data discarded after OCR; only `rawText` and `entities` cross the network boundary |

---

## Tech Stack

### Android App
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 (One UI tokens) |
| OCR | Google ML Kit Text Recognition v2 |
| Entity Extraction | Google ML Kit Entity Extraction |
| Networking | Retrofit 2 + OkHttp + Gson |
| Concurrency | Kotlin Coroutines |
| Samsung APIs | CalendarContract, ContactsContract, Samsung Notes SDK (`.aar`) |
| Min SDK | API 26 (Android 8.0) |

### OpenClaw Gateway (MacBook)
| Layer | Technology |
|---|---|
| Runtime | Node.js |
| LLM Backend | Ollama (`llama3.2`, ~2GB) |
| Skill System | SKILL.md + `index.js` per integration |
| Transport | HTTP/JSON on port 3000 |

---

## Repository Structure

```
SnapTask/
├── README.md
├── LICENSE
├── .gitignore
├── docs/
│   ├── architecture.md        # System design & data flow diagrams
│   ├── privacy-flow.md        # Step-by-step privacy guarantee
│   └── api-contracts.md       # Request / response JSON schemas
│
├── snaptask-android/          # Kotlin Android application
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradle.properties
│   └── app/
│       ├── build.gradle
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── java/com/snaptask/
│           │   ├── ShareReceiverActivity.kt   ← entry point (share sheet)
│           │   ├── ocr/
│           │   │   ├── MLKitOCRProcessor.kt   ← ML Kit Text Recognition
│           │   │   └── EntityExtractor.kt     ← ML Kit Entity Extraction
│           │   ├── network/
│           │   │   ├── OpenClawClient.kt      ← Retrofit client
│           │   │   └── models/
│           │   │       ├── SnapTaskRequest.kt
│           │   │       ├── SnapTaskResponse.kt
│           │   │       └── PlannedAction.kt
│           │   ├── samsung/
│           │   │   ├── CalendarManager.kt     ← CalendarContract
│           │   │   ├── ContactsManager.kt     ← ContactsContract
│           │   │   └── NotesManager.kt        ← Samsung Notes SDK
│           │   └── ui/
│           │       ├── ConfirmationSheet.kt   ← Compose bottom sheet
│           │       ├── IntentBadge.kt         ← colored pill chip
│           │       └── theme/
│           │           └── Theme.kt
│           └── res/
│               ├── values/strings.xml
│               └── drawable/
│
└── openclaw-skills/           # OpenClaw skill definitions
    ├── samsung-calendar/      # Deploy to ~/.openclaw/workspace/skills/
    │   ├── SKILL.md
    │   └── index.js
    ├── samsung-notes/
    │   ├── SKILL.md
    │   └── index.js
    ├── samsung-contacts/
    │   ├── SKILL.md
    │   └── index.js
    └── expense-logger/
        ├── SKILL.md
        └── index.js
```

---

## Setup & Installation

### Prerequisites

| Tool | Version | Install |
|---|---|---|
| Android Studio | Hedgehog+ | [Download](https://developer.android.com/studio) |
| Kotlin | 1.9+ | Bundled with Android Studio |
| Node.js | 18+ | `brew install node` |
| Ollama | Latest | `brew install ollama` |
| Samsung Notes SDK | Latest | Samsung Developers Portal |

---

### Step 1 — MacBook: Set up Ollama

```bash
brew install ollama
ollama pull llama3.2        # ~2GB download, runs fast on M4
ollama serve                # starts at localhost:11434
```

---

### Step 2 — MacBook: Set up OpenClaw

```bash
git clone https://github.com/openclaw/openclaw
cd openclaw
npm install
npm run dev                 # gateway starts on port 3000
```

Configure `~/.openclaw/config.json`:

```json
{
  "agent": {
    "model": "ollama/llama3.2",
    "baseURL": "http://localhost:11434"
  },
  "server": {
    "port": 3000
  }
}
```

Deploy the skills from this repo:

```bash
cp -r openclaw-skills/* ~/.openclaw/workspace/skills/
```

---

### Step 3 — Find your MacBook's local IP

```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
# note the 192.168.x.x address
```

---

### Step 4 — Android App

1. Open `snaptask-android/` in Android Studio.
2. In `OpenClawClient.kt`, set `BASE_URL` to `http://192.168.x.x:3000/` (your IP from Step 3).
3. Place `samsung-notes-sdk.aar` from the Samsung Developers Portal into `app/libs/`.
4. Connect a Samsung Galaxy device in Developer Mode.
5. Run the `app` configuration in Android Studio.

---

## Usage

1. Open **Samsung Camera** and photograph any document, business card, flyer, or whiteboard.
2. Tap **Share** in the camera preview or Gallery.
3. Select **SnapTask** from the share sheet.
4. SnapTask reads the text on-device and asks OpenClaw what to do.
5. A **confirmation card** slides up showing the detected intent and planned action.
6. Tap **Execute** to write the data, or **Cancel** to dismiss.

### Supported Intents

| Photo Content | Action |
|---|---|
| Event poster / flyer | Creates Samsung Calendar event with date, time, location |
| Business card | Saves contact to Samsung Contacts with name, phone, email, company |
| Whiteboard / task list | Creates Samsung Note with formatted checklist |
| Receipt | Logs expense with amount, merchant, date to Samsung Notes |

---

## Architecture

See [`docs/architecture.md`](docs/architecture.md) for the full system design.

**Core architectural rules (do not violate):**

- The **Android app never makes LLM decisions** — it does OCR and executes Samsung APIs only.
- **OpenClaw skills never call Samsung APIs directly** — they return structured `PlannedAction` JSON.
- **Images never reach OpenClaw** — only `rawText` and `entities` cross the network boundary.

---

## Implementation Phases

### Phase 1 — Core Setup & Architecture
- Ollama + llama3.2 running locally on MacBook
- OpenClaw gateway cloned, configured, running on port 3000
- `samsung-calendar` skill deployed and returning valid `PlannedAction` JSON
- Android project scaffolded with share-sheet `intent-filter` in `AndroidManifest.xml`
- `MLKitOCRProcessor` returning extracted text from a static test image

### Phase 2 — Feature Development
- `EntityExtractor` enriching OCR output with ML Kit structured entities
- `OpenClawClient` (Retrofit) wired and receiving `SnapTaskResponse`
- `ConfirmationSheet` Compose UI rendering intent badge + planned actions
- `CalendarManager` writing events via `CalendarContract`
- Full end-to-end: share → OCR → OpenClaw → confirmation card → calendar write

### Phase 3 — Integrations & Optimization
- `ContactsManager` via `ContactsContract`
- `NotesManager` via Samsung Notes SDK
- `expense-logger` skill + execution handler
- Multi-skill routing: OpenClaw classifies between event / contact / note / expense
- Optional: follow-up text field in confirmation sheet for corrections

### Phase 4 — Testing & Deployment
- Graceful error states: OpenClaw unreachable, OCR failure, permission denied
- Loading state UI ("Reading image...")
- Runtime permission requests for Calendar, Contacts
- Demo rehearsal: photograph PRISM poster 10× — must succeed every time

---

## Future Improvements

- Multi-action recognition (one photo → calendar event + contact save)
- Offline intent classifier fallback when Ollama is unavailable
- Samsung DeX support with expanded confirmation UI
- Bixby Routine trigger on camera roll additions
- Additional skills: Wi-Fi QR codes, medication schedules, shipping labels

---

## Privacy

1. Photo stays on device as a local JPEG.
2. ML Kit reads pixels from the URI — returns text only, image data is discarded.
3. Only `rawText` + ML Kit `entities` are sent over local Wi-Fi to OpenClaw — never to the internet.
4. Ollama processes text on your MacBook — no cloud inference.
5. OpenClaw returns a `PlannedAction` JSON object — no data is persisted.
6. Android app writes directly to Samsung Calendar / Contacts / Notes — fully local.

**Nothing leaves the local network. The raw image never touches a server.**

---

## License

See [LICENSE](LICENSE).
