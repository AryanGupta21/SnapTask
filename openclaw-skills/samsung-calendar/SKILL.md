name: samsung-calendar
description: Creates a calendar event on a Samsung Galaxy device
trigger: when the captured text describes an event, meeting, show, competition, conference, concert, or any time-bounded activity that has a date and time
parameters:
  - title: string (name of the event)
  - date: string (ISO 8601 date, e.g. 2026-04-20)
  - time: string (HH:MM in 24-hour format)
  - location: string (optional venue or address)
  - reminder_minutes: number (default 60, minutes before event to send reminder)
examples:
  - input: "Clash of Claws 2026 — 20 April, 4:40 PM, Samsung Arena"
    output: { title: "Clash of Claws 2026", date: "2026-04-20", time: "16:40", location: "Samsung Arena" }
  - input: "Team standup every Monday 9:00 AM, Room 3B"
    output: { title: "Team Standup", date: "<next Monday>", time: "09:00", location: "Room 3B" }
