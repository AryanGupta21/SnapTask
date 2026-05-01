// Returns structured PlannedAction data for the Android app to execute
// via ContactsContract. Does NOT call Samsung APIs directly.
module.exports = {
  execute: async (params) => {
    const { name, phone, email, company } = params;

    if (!name) {
      throw new Error('samsung-contacts requires at least a name');
    }

    return {
      action: 'create_contact',
      params: {
        name,
        phone: phone ?? null,
        email: email ?? null,
        company: company ?? null,
      },
    };
  },
};
