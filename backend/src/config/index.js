require('dotenv').config();

module.exports = {
  env: process.env.NODE_ENV || 'development',
  port: parseInt(process.env.PORT, 10) || 3000,

  mongodb: {
    uri: process.env.MONGODB_URI || process.env.DATABASE_URL || 'mongodb://localhost:27017',
    dbName: process.env.MONGODB_DB_NAME || 'rupex',
  },

  jwt: {
    secret: process.env.JWT_SECRET || 'dev-secret-key-change-in-production',
    accessExpiry: process.env.JWT_ACCESS_EXPIRY || '15m',
    refreshExpiry: process.env.JWT_REFRESH_EXPIRY || '7d',
  },

  testUser: {
    email: process.env.TEST_USER_EMAIL || 'test@rupex.dev',
    password: process.env.TEST_USER_PASSWORD || 'Test@123',
    name: process.env.TEST_USER_NAME || 'Test User',
  },

  rateLimit: {
    windowMs: 15 * 60 * 1000,
    max: 100,
  },

  corsOrigins: process.env.CORS_ORIGINS ? process.env.CORS_ORIGINS.split(',') : '*',

  TRANSACTION_TYPES: ['income', 'expense', 'transfer'],
  TRANSACTION_SOURCES: ['manual', 'sms', 'api', 'recurring'],
  ACCOUNT_TYPES: ['bank', 'wallet', 'cash', 'credit_card'],
  CATEGORY_TYPES: ['income', 'expense'],
  
  DEFAULT_CURRENCY: 'INR',
  DEFAULT_TIMEZONE: 'Asia/Kolkata',

  DEFAULT_CATEGORIES: {
    expense: [
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
    income: [
      { name: 'Salary', icon: 'work', color: '#10B981' },
      { name: 'Freelance', icon: 'laptop', color: '#3B82F6' },
      { name: 'Investment', icon: 'trending_up', color: '#8B5CF6' },
      { name: 'Refund', icon: 'replay', color: '#F59E0B' },
      { name: 'Gift', icon: 'card_giftcard', color: '#EC4899' },
      { name: 'Other Income', icon: 'attach_money', color: '#6B7280' },
    ],
  },
};
