// Returns structured PlannedAction data for the Android app to execute
// as a Samsung Note. Does NOT call Samsung APIs directly.
module.exports = {
  execute: async (params) => {
    const { merchant, amount, currency, date, category } = params;

    if (!merchant || amount == null) {
      throw new Error('expense-logger requires merchant and amount');
    }

    return {
      type: 'log_expense',
      params: {
        merchant,
        amount: parseFloat(amount),
        currency: currency ?? 'USD',
        date: date ?? new Date().toISOString().split('T')[0],
        category: category ?? 'General',
      },
    };
  },
};
