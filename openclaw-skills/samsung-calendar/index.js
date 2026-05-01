// Returns structured PlannedAction data for the Android app to execute
// via CalendarContract. Does NOT call Samsung APIs directly.
module.exports = {
  execute: async (params) => {
    const { title, date, time, location, reminder_minutes } = params;

    if (!title || !date || !time) {
      throw new Error('samsung-calendar requires title, date, and time');
    }

    return {
      type: 'create_calendar_event',
      params: {
        title,
        dateTime: `${date}T${time}:00`,
        location: location ?? null,
        reminderMinutes: reminder_minutes ?? 60,
      },
    };
  },
};
