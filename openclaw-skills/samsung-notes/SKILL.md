name: samsung-notes
description: Creates a new note in Samsung Notes on a Samsung Galaxy device
trigger: when the captured text looks like a to-do list, whiteboard notes, lecture notes, meeting minutes, or any unstructured text that should be saved as a note
parameters:
  - title: string (note title, inferred from first line or topic)
  - body: string (full note body text)
  - checklist: array of strings (optional, use when text contains bullet points or to-do items)
examples:
  - input: "Shopping list:\n- Milk\n- Eggs\n- Bread"
    output: { title: "Shopping List", checklist: ["Milk", "Eggs", "Bread"] }
  - input: "Meeting notes Q2 planning\nRevamp onboarding\nHire 2 engineers\nLaunch beta by June"
    output: { title: "Meeting Notes: Q2 Planning", body: "Revamp onboarding\nHire 2 engineers\nLaunch beta by June" }
