// Returns structured PlannedAction data for the Android app to execute
// via Samsung Notes SDK. Does NOT call Samsung APIs directly.
module.exports = {
  execute: async (params) => {
    const { title, body, checklist } = params;

    if (!title) {
      throw new Error('samsung-notes requires a title');
    }

    return {
      action: 'create_note',
      params: {
        title,
        body: body ?? '',
        checklist: checklist ?? null,
      },
    };
  },
};
