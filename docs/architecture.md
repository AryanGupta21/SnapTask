# SnapTask — System Architecture

## Three-Layer Separation

```
┌─────────────────────────────────────────────────────────────┐
│  ANDROID DEVICE (Samsung Galaxy)                            │
│                                                             │
│  ┌──────────────┐    ┌─────────────────┐    ┌───────────┐  │
│  │ Samsung      │    │  SnapTask App   │    │ Samsung   │  │
│  │ Camera       │───▶│                 │───▶│ APIs      │  │
│  │              │    │  ML Kit OCR     │    │           │  │
│  │ (any app)    │    │  Share Sheet    │    │ Calendar  │  │
│  └──────────────┘    │  Compose UI     │    │ Contacts  │  │
│                      └────────┬────────┘    │ Notes     │  │
│                               │ rawText     └───────────┘  │
│                               │ + entities                  │
└───────────────────────────────┼─────────────────────────────┘
                    Local Wi-Fi │ (never internet)
┌───────────────────────────────┼─────────────────────────────┐
│  MACBOOK (same network)       │                             │
│                               ▼                             │
│  ┌─────────────────────────────────────────┐               │
│  │  OpenClaw Gateway (Node.js :3000)       │               │
│  │                                         │               │
│  │  Skills: samsung-calendar               │               │
│  │          samsung-contacts               │               │
│  │          samsung-notes                  │               │
│  │          expense-logger                 │               │
│  └──────────────────┬──────────────────────┘               │
│                     │ prompt                                │
│                     ▼                                       │
│  ┌──────────────────────────────┐                          │
│  │  Ollama (localhost:11434)    │                          │
│  │  Model: llama3.2             │                          │
│  └──────────────────────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

### Android App
| Component | Responsibility |
|---|---|
| `ShareReceiverActivity` | Receives image URI from share sheet intent; orchestrates the pipeline |
| `MLKitOCRProcessor` | Runs on-device text recognition; returns `String` |
| `EntityExtractor` | Runs on-device entity extraction; returns typed entity list |
| `OpenClawClient` | Retrofit HTTP client; POSTs text to gateway, receives `SnapTaskResponse` |
| `ConfirmationSheet` | Compose bottom sheet; shows intent + planned actions; awaits user approval |
| `CalendarManager` | Writes events via `CalendarContract.Events` |
| `ContactsManager` | Writes contacts via `ContactsContract` batch operations |
| `NotesManager` | Creates notes via Samsung Notes SDK (`SNoteController`) |

### OpenClaw Gateway
| Component | Responsibility |
|---|---|
| HTTP server | Receives `POST /process` with `{ rawText, entities }` |
| Prompt builder | Constructs LLM prompt with available skill descriptions from SKILL.md files |
| Ollama client | Sends prompt to local Ollama; receives structured JSON response |
| Skill dispatcher | Invokes matched skill `execute()` with extracted parameters |

### OpenClaw Skills
Each skill is a self-contained folder:
- `SKILL.md` — natural language description + parameter schema; read by the LLM
- `index.js` — `execute(params)` function that returns a `PlannedAction` JSON object

Skills **do not** call Samsung APIs. They translate LLM output into structured data the Android app can execute.

## Data Flow (step by step)

1. User shares image from Samsung Camera → `ShareReceiverActivity` receives URI
2. `MLKitOCRProcessor.process(uri)` → `rawText: String`
3. `EntityExtractor.annotate(rawText)` → `entities: List<ExtractedEntity>`
4. `OpenClawClient.process(rawText, entities)` → HTTP POST to MacBook
5. OpenClaw builds prompt with skill list; sends to Ollama
6. Ollama returns `{ intent, skill, params }` JSON
7. OpenClaw invokes `skill.execute(params)` → `PlannedAction`
8. OpenClaw returns `SnapTaskResponse` to Android app
9. `ConfirmationSheet` renders summary; user taps Execute
10. `CalendarManager` / `ContactsManager` / `NotesManager` writes data

## Key Architectural Constraints

1. **LLM reasoning stays on MacBook.** The Android app is a dumb executor — it does OCR and API calls. Intent classification lives in OpenClaw.
2. **Images stay on device.** The HTTP payload contains only text. No base64 images, no file paths, no EXIF.
3. **Samsung API calls happen on-device.** OpenClaw skills return data structures, not API results. The boundary is the HTTP response.
