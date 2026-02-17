const { User, Category } = require('../models');
const config = require('../config');
const logger = require('../utils/logger');

const seedDatabase = async () => {
  try {
    const existingUsers = await User.countDocuments();
    if (existingUsers > 0) {
      logger.info('Database already seeded, skipping...');
      return;
    }

    logger.info('Seeding database...');

    const testUser = await User.create({
      email: config.testUser.email,
      passwordHash: await require('bcryptjs').hash(config.testUser.password, 12),
      name: config.testUser.name,
    });

    logger.info(`Created test user: ${testUser.email}`);

    const categories = [];

    for (const cat of config.DEFAULT_CATEGORIES.expense) {
      categories.push({
        user: testUser._id,
        name: cat.name,
        type: 'expense',
        icon: cat.icon,
        color: cat.color,
        isSystem: true,
      });
    }

    for (const cat of config.DEFAULT_CATEGORIES.income) {
      categories.push({
        user: testUser._id,
        name: cat.name,
        type: 'income',
        icon: cat.icon,
        color: cat.color,
        isSystem: true,
      });
    }

    await Category.insertMany(categories);
    logger.info(`Created ${categories.length} default categories`);

    logger.info('Database seeding completed!');
  } catch (error) {
    logger.error('Error seeding database:', error);
    throw error;
  }
};

module.exports = seedDatabase;

if (require.main === module) {
  const { connectDB, disconnectDB } = require('../config/database');
  connectDB()
    .then(() => seedDatabase())
    .then(() => disconnectDB())
    .catch((err) => {
      console.error(err);
      process.exit(1);
    });
}
