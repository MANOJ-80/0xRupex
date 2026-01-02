module.exports = {
  TRANSACTION_TYPES: ['income', 'expense', 'transfer'],
  TRANSACTION_SOURCES: ['manual', 'sms', 'api', 'recurring'],
  ACCOUNT_TYPES: ['bank', 'wallet', 'cash', 'credit_card'],
  CATEGORY_TYPES: ['income', 'expense'],
  
  DEFAULT_CURRENCY: 'INR',
  DEFAULT_TIMEZONE: 'Asia/Kolkata',

  // Default categories
  DEFAULT_EXPENSE_CATEGORIES: [
    { name: 'Food & Dining', icon: 'restaurant', color: '#EF4444' },
    { name: 'Transport', icon: 'directions_car', color: '#F59E0B' },
    { name: 'Shopping', icon: 'shopping_bag', color: '#8B5CF6' },
    { name: 'Bills & Utilities', icon: 'receipt', color: '#3B82F6' },
    { name: 'Entertainment', icon: 'movie', color: '#EC4899' },
    { name: 'Health', icon: 'local_hospital', color: '#10B981' },
    { name: 'Education', icon: 'school', color: '#6366F1' },
    { name: 'Personal Care', icon: 'spa', color: '#F472B6' },
    { name: 'Travel', icon: 'flight', color: '#14B8A6' },
    { name: 'Groceries', icon: 'local_grocery_store', color: '#84CC16' },
    { name: 'Subscriptions', icon: 'subscriptions', color: '#A855F7' },
    { name: 'Other', icon: 'more_horiz', color: '#6B7280' },
  ],

  DEFAULT_INCOME_CATEGORIES: [
    { name: 'Salary', icon: 'work', color: '#10B981' },
    { name: 'Freelance', icon: 'laptop', color: '#3B82F6' },
    { name: 'Investment', icon: 'trending_up', color: '#8B5CF6' },
    { name: 'Refund', icon: 'replay', color: '#F59E0B' },
    { name: 'Gift', icon: 'card_giftcard', color: '#EC4899' },
    { name: 'Other Income', icon: 'attach_money', color: '#6B7280' },
  ],
};
