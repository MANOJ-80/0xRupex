const bcrypt = require('bcryptjs');
const config = require('../../config');
const constants = require('../../config/constants');

/**
 * Seed test user with accounts, categories, and sample transactions
 */
exports.seed = async function(knex) {
  // Clear existing data (in reverse order of dependencies)
  await knex('monthly_summaries').del();
  await knex('transactions').del();
  await knex('budgets').del();
  await knex('categories').del();
  await knex('accounts').del();
  await knex('refresh_tokens').del();
  await knex('users').del();

  // Create test user
  const passwordHash = await bcrypt.hash(config.testUser.password, 12);
  
  const [user] = await knex('users').insert({
    email: config.testUser.email,
    password_hash: passwordHash,
    name: config.testUser.name,
    phone: '+919876543210',
    currency: 'INR',
    timezone: 'Asia/Kolkata',
  }).returning('*');

  console.log(`✅ Created test user: ${user.email}`);

  // Create accounts
  const accounts = await knex('accounts').insert([
    {
      user_id: user.id,
      name: 'HDFC Savings',
      type: 'bank',
      balance: 52340.50,
      institution: 'HDFC Bank',
      last_4_digits: '4532',
      color: '#EF4444',
      icon: 'account_balance',
    },
    {
      user_id: user.id,
      name: 'SBI Salary',
      type: 'bank',
      balance: 125000.00,
      institution: 'State Bank of India',
      last_4_digits: '7891',
      color: '#3B82F6',
      icon: 'account_balance',
    },
    {
      user_id: user.id,
      name: 'Google Pay',
      type: 'wallet',
      balance: 2500.00,
      institution: 'Google Pay',
      color: '#10B981',
      icon: 'account_balance_wallet',
    },
    {
      user_id: user.id,
      name: 'Cash',
      type: 'cash',
      balance: 5000.00,
      color: '#F59E0B',
      icon: 'payments',
    },
  ]).returning('*');

  console.log(`✅ Created ${accounts.length} accounts`);

  // Create expense categories
  const expenseCategories = await knex('categories').insert(
    constants.DEFAULT_EXPENSE_CATEGORIES.map(cat => ({
      user_id: user.id,
      name: cat.name,
      type: 'expense',
      icon: cat.icon,
      color: cat.color,
      is_system: true,
    }))
  ).returning('*');

  // Create income categories
  const incomeCategories = await knex('categories').insert(
    constants.DEFAULT_INCOME_CATEGORIES.map(cat => ({
      user_id: user.id,
      name: cat.name,
      type: 'income',
      icon: cat.icon,
      color: cat.color,
      is_system: true,
    }))
  ).returning('*');

  const allCategories = [...expenseCategories, ...incomeCategories];
  console.log(`✅ Created ${allCategories.length} categories`);

  // Helper to find category by name
  const findCategory = (name) => allCategories.find(c => c.name === name);
  const findAccount = (name) => accounts.find(a => a.name === name);

  // Create sample transactions for last 30 days
  const now = new Date();
  const transactions = [];

  // Salary (beginning of month)
  transactions.push({
    user_id: user.id,
    account_id: findAccount('SBI Salary').id,
    category_id: findCategory('Salary').id,
    type: 'income',
    amount: 75000.00,
    description: 'Monthly Salary - December',
    merchant: 'Acme Corp',
    source: 'manual',
    transaction_at: new Date(now.getFullYear(), now.getMonth(), 1, 10, 0, 0),
  });

  // Freelance income
  transactions.push({
    user_id: user.id,
    account_id: findAccount('HDFC Savings').id,
    category_id: findCategory('Freelance').id,
    type: 'income',
    amount: 15000.00,
    description: 'Web Development Project',
    merchant: 'Client ABC',
    source: 'manual',
    transaction_at: new Date(now.getFullYear(), now.getMonth(), 5, 14, 30, 0),
  });

  // Various expenses
  const sampleExpenses = [
    { category: 'Food & Dining', amount: 499, merchant: 'Zomato', account: 'Google Pay', daysAgo: 1 },
    { category: 'Food & Dining', amount: 350, merchant: 'Swiggy', account: 'Google Pay', daysAgo: 2 },
    { category: 'Food & Dining', amount: 1200, merchant: 'Restaurant XYZ', account: 'HDFC Savings', daysAgo: 3 },
    { category: 'Transport', amount: 250, merchant: 'Uber', account: 'Google Pay', daysAgo: 1 },
    { category: 'Transport', amount: 180, merchant: 'Ola', account: 'Google Pay', daysAgo: 4 },
    { category: 'Transport', amount: 2500, merchant: 'Petrol Pump', account: 'HDFC Savings', daysAgo: 7 },
    { category: 'Shopping', amount: 2999, merchant: 'Amazon', account: 'HDFC Savings', daysAgo: 5 },
    { category: 'Shopping', amount: 1499, merchant: 'Flipkart', account: 'HDFC Savings', daysAgo: 10 },
    { category: 'Bills & Utilities', amount: 1500, merchant: 'Electricity Bill', account: 'HDFC Savings', daysAgo: 8 },
    { category: 'Bills & Utilities', amount: 999, merchant: 'Jio Fiber', account: 'HDFC Savings', daysAgo: 12 },
    { category: 'Entertainment', amount: 199, merchant: 'Netflix', account: 'HDFC Savings', daysAgo: 15 },
    { category: 'Entertainment', amount: 119, merchant: 'Spotify', account: 'HDFC Savings', daysAgo: 15 },
    { category: 'Entertainment', amount: 450, merchant: 'BookMyShow', account: 'Google Pay', daysAgo: 6 },
    { category: 'Groceries', amount: 3500, merchant: 'BigBasket', account: 'HDFC Savings', daysAgo: 9 },
    { category: 'Groceries', amount: 850, merchant: 'Local Store', account: 'Cash', daysAgo: 2 },
    { category: 'Health', amount: 500, merchant: 'Apollo Pharmacy', account: 'HDFC Savings', daysAgo: 11 },
    { category: 'Personal Care', amount: 750, merchant: 'Salon', account: 'Cash', daysAgo: 14 },
    { category: 'Subscriptions', amount: 299, merchant: 'YouTube Premium', account: 'HDFC Savings', daysAgo: 20 },
  ];

  for (const exp of sampleExpenses) {
    const txnDate = new Date(now);
    txnDate.setDate(txnDate.getDate() - exp.daysAgo);
    txnDate.setHours(Math.floor(Math.random() * 12) + 8, Math.floor(Math.random() * 60));

    transactions.push({
      user_id: user.id,
      account_id: findAccount(exp.account).id,
      category_id: findCategory(exp.category).id,
      type: 'expense',
      amount: exp.amount,
      description: `Payment to ${exp.merchant}`,
      merchant: exp.merchant,
      source: exp.account === 'Cash' ? 'manual' : 'sms',
      transaction_at: txnDate,
      sms_hash: exp.account !== 'Cash' ? `hash_${Date.now()}_${Math.random().toString(36).substr(2, 9)}` : null,
    });
  }

  await knex('transactions').insert(transactions);
  console.log(`✅ Created ${transactions.length} sample transactions`);

  // Create a budget for Food & Dining
  await knex('budgets').insert({
    user_id: user.id,
    category_id: findCategory('Food & Dining').id,
    amount: 8000.00,
    period: 'monthly',
    start_date: new Date(now.getFullYear(), now.getMonth(), 1),
    alert_threshold: 0.75,
  });

  console.log(`✅ Created sample budget`);

  console.log('\n🎉 Seed completed successfully!');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log(`📧 Test User Email:    ${config.testUser.email}`);
  console.log(`🔑 Test User Password: ${config.testUser.password}`);
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
};
