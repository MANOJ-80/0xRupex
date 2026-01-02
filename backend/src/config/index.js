require('dotenv').config();

module.exports = {
  env: process.env.NODE_ENV || 'development',
  port: parseInt(process.env.PORT, 10) || 3000,

  db: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT, 10) || 5432,
    name: process.env.DB_NAME || 'rupex',
    user: process.env.DB_USER || 'rupex',
    password: process.env.DB_PASSWORD || 'rupex123',
  },

  jwt: {
    secret: process.env.JWT_SECRET || 'dev-secret-key',
    accessExpiry: process.env.JWT_ACCESS_EXPIRY || '15m',
    refreshExpiry: process.env.JWT_REFRESH_EXPIRY || '7d',
  },

  testUser: {
    email: process.env.TEST_USER_EMAIL || 'test@rupex.dev',
    password: process.env.TEST_USER_PASSWORD || 'Test@123',
    name: process.env.TEST_USER_NAME || 'Test User',
  },

  rateLimit: {
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // limit each IP to 100 requests per windowMs
  },
};
