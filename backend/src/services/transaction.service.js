const db = require('../config/database');
const { NotFoundError, ValidationError } = require('../utils/errors');
const accountService = require('./account.service');
const logger = require('../utils/logger');

/**
 * Transaction Service
 */
class TransactionService {
  /**
   * Get transactions with pagination and filters
   */
  async getTransactions(userId, options = {}) {
    const {
      page = 1,
      limit = 20,
      type,
      category_id,
      account_id,
      start_date,
      end_date,
      search,
      sort_by = 'transaction_at',
      sort_order = 'desc',
    } = options;

    let query = db('transactions')
      .select(
        'transactions.*',
        'categories.name as category_name',
        'categories.icon as category_icon',
        'categories.color as category_color',
        'accounts.name as account_name'
      )
      .leftJoin('categories', 'transactions.category_id', 'categories.id')
      .leftJoin('accounts', 'transactions.account_id', 'accounts.id')
      .where('transactions.user_id', userId);

    // Apply filters
    if (type) query = query.where('transactions.type', type);
    if (category_id) query = query.where('transactions.category_id', category_id);
    if (account_id) query = query.where('transactions.account_id', account_id);
    if (start_date) query = query.where('transactions.transaction_at', '>=', start_date);
    if (end_date) query = query.where('transactions.transaction_at', '<=', end_date);
    if (search) {
      query = query.where((builder) => {
        builder
          .where('transactions.description', 'ilike', `%${search}%`)
          .orWhere('transactions.merchant', 'ilike', `%${search}%`)
          .orWhere('transactions.notes', 'ilike', `%${search}%`);
      });
    }

    // Get total count - build a separate count query
    let countQuery = db('transactions')
      .where('transactions.user_id', userId);
    if (type) countQuery = countQuery.where('transactions.type', type);
    if (category_id) countQuery = countQuery.where('transactions.category_id', category_id);
    if (account_id) countQuery = countQuery.where('transactions.account_id', account_id);
    if (start_date) countQuery = countQuery.where('transactions.transaction_at', '>=', start_date);
    if (end_date) countQuery = countQuery.where('transactions.transaction_at', '<=', end_date);
    if (search) {
      countQuery = countQuery.where((builder) => {
        builder
          .where('transactions.description', 'ilike', `%${search}%`)
          .orWhere('transactions.merchant', 'ilike', `%${search}%`)
          .orWhere('transactions.notes', 'ilike', `%${search}%`);
      });
    }
    const [{ count }] = await countQuery.count('* as count');
    const total = parseInt(count);

    // Apply pagination and sorting
    const offset = (page - 1) * limit;
    const transactions = await query
      .orderBy(`transactions.${sort_by}`, sort_order)
      .limit(limit)
      .offset(offset);

    return {
      transactions,
      pagination: {
        page,
        limit,
        total,
        total_pages: Math.ceil(total / limit),
      },
    };
  }

  /**
   * Get transaction by ID
   */
  async getTransactionById(transactionId, userId) {
    const transaction = await db('transactions')
      .select(
        'transactions.*',
        'categories.name as category_name',
        'categories.icon as category_icon',
        'accounts.name as account_name'
      )
      .leftJoin('categories', 'transactions.category_id', 'categories.id')
      .leftJoin('accounts', 'transactions.account_id', 'accounts.id')
      .where('transactions.id', transactionId)
      .where('transactions.user_id', userId)
      .first();

    if (!transaction) {
      throw new NotFoundError('Transaction');
    }

    return transaction;
  }

  /**
   * Create transaction
   */
  async createTransaction(userId, data) {
    // Auto-resolve category from categoryName if not provided
    let categoryId = data.category_id || data.categoryId;
    if (!categoryId && data.categoryName) {
      const category = await db('categories')
        .where({ user_id: userId, name: data.categoryName })
        .first();
      if (category) {
        categoryId = category.id;
      }
    }
    
    // Auto-resolve account from last4Digits if not provided
    let accountId = data.account_id || data.accountId;
    if (!accountId && data.last4Digits) {
      const account = await db('accounts')
        .where({ user_id: userId, last_4_digits: data.last4Digits })
        .first();
      if (account) {
        accountId = account.id;
      }
    }
    
    const [transaction] = await db('transactions')
      .insert({
        user_id: userId,
        account_id: accountId,
        category_id: categoryId,
        type: data.type,
        amount: data.amount,
        description: data.description,
        merchant: data.merchant,
        reference_id: data.reference_id || data.referenceId,
        transaction_at: data.transaction_at || data.transactionAt || data.transaction_date || new Date(),
        location: data.location,
        notes: data.notes,
        tags: data.tags || [],
        is_recurring: data.is_recurring || false,
        source: data.source || 'manual',
        sms_hash: data.sms_hash || data.smsHash,
      })
      .returning('*');

    // Update account balance
    if (accountId) {
      await accountService.updateBalance(
        accountId,
        userId,
        data.amount,
        data.type
      );
    }

    logger.info(`Transaction created: ${transaction.id} - ${data.type} ${data.amount} | Merchant: ${data.merchant} | Category: ${data.categoryName || 'N/A'}`);

    return transaction;
  }

  /**
   * Sync transactions from Android app
   */
  async syncTransactions(userId, transactions) {
    const results = {
      created: 0,
      skipped: 0,
      errors: [],
    };

    for (const tx of transactions) {
      try {
        // Check for duplicate by SMS hash
        if (tx.sms_hash) {
          const existing = await db('transactions')
            .where({ user_id: userId, sms_hash: tx.sms_hash })
            .first();

          if (existing) {
            results.skipped++;
            continue;
          }
        }

        await this.createTransaction(userId, {
          ...tx,
          source: 'sms',
        });
        results.created++;
      } catch (error) {
        results.errors.push({
          transaction: tx,
          error: error.message,
        });
      }
    }

    logger.info(`Sync completed: ${results.created} created, ${results.skipped} skipped`);
    return results;
  }

  /**
   * Update transaction
   */
  async updateTransaction(transactionId, userId, data) {
    const transaction = await this.getTransactionById(transactionId, userId);

    // If categoryName is provided but no category_id, try to find category
    if (data.categoryName && !data.category_id) {
      const category = await db('categories')
        .where('name', 'ilike', data.categoryName)
        .andWhere(function() {
          this.where('user_id', userId).orWhereNull('user_id');
        })
        .first();
      if (category) {
        data.category_id = category.id;
      }
    }

    // If amount or type changed, need to adjust account balance
    const amountChanged = data.amount && data.amount !== parseFloat(transaction.amount);
    const typeChanged = data.type && data.type !== transaction.type;
    const accountChanged = data.account_id && data.account_id !== transaction.account_id;

    if ((amountChanged || typeChanged || accountChanged) && transaction.account_id) {
      // Reverse original transaction
      const reverseType = transaction.type === 'expense' ? 'income' : 'expense';
      await accountService.updateBalance(
        transaction.account_id,
        userId,
        transaction.amount,
        reverseType
      );

      // Apply new transaction
      const newAccountId = data.account_id || transaction.account_id;
      const newType = data.type || transaction.type;
      const newAmount = data.amount || transaction.amount;
      await accountService.updateBalance(newAccountId, userId, newAmount, newType);
    }

    const [updated] = await db('transactions')
      .where({ id: transactionId, user_id: userId })
      .update({
        account_id: data.account_id ?? transaction.account_id,
        category_id: data.category_id ?? transaction.category_id,
        type: data.type ?? transaction.type,
        amount: data.amount ?? transaction.amount,
        description: data.description ?? transaction.description,
        merchant: data.merchant ?? transaction.merchant,
        transaction_at: data.transaction_at ?? data.transaction_date ?? transaction.transaction_at,
        notes: data.notes ?? transaction.notes,
        tags: data.tags ?? transaction.tags,
        is_recurring: data.is_recurring ?? transaction.is_recurring,
        updated_at: new Date(),
      })
      .returning('*');

    return updated;
  }

  /**
   * Delete transaction
   */
  async deleteTransaction(transactionId, userId) {
    const transaction = await this.getTransactionById(transactionId, userId);

    // Reverse account balance
    if (transaction.account_id) {
      const reverseType = transaction.type === 'expense' ? 'income' : 'expense';
      await accountService.updateBalance(
        transaction.account_id,
        userId,
        transaction.amount,
        reverseType
      );
    }

    await db('transactions').where({ id: transactionId }).del();
  }

  /**
   * Get monthly summary
   */
  async getMonthlySummary(userId, year, month) {
    const startDate = new Date(year, month - 1, 1);
    const endDate = new Date(year, month, 0);

    const [summary] = await db('transactions')
      .select(db.raw(`
        COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) as total_income,
        COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) as total_expense,
        COUNT(*) as transaction_count
      `))
      .where('user_id', userId)
      .whereBetween('transaction_at', [startDate, endDate]);

    return {
      year,
      month,
      total_income: parseFloat(summary.total_income),
      total_expense: parseFloat(summary.total_expense),
      net_savings: parseFloat(summary.total_income) - parseFloat(summary.total_expense),
      transaction_count: parseInt(summary.transaction_count),
    };
  }

  /**
   * Get analytics data
   */
  async getAnalytics(userId, startDate, endDate) {
    // Daily spending trend
    const dailyTrend = await db('transactions')
      .select(db.raw(`
        DATE(transaction_at) as date,
        SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END) as expense,
        SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END) as income
      `))
      .where('user_id', userId)
      .whereBetween('transaction_at', [startDate, endDate])
      .groupBy(db.raw('DATE(transaction_at)'))
      .orderBy('date');

    // Top merchants
    const topMerchants = await db('transactions')
      .select('merchant')
      .sum('amount as total')
      .count('id as count')
      .where('user_id', userId)
      .where('type', 'expense')
      .whereNotNull('merchant')
      .whereBetween('transaction_at', [startDate, endDate])
      .groupBy('merchant')
      .orderBy('total', 'desc')
      .limit(10);

    // Spending by day of week
    const byDayOfWeek = await db('transactions')
      .select(db.raw(`
        EXTRACT(DOW FROM transaction_at) as day_of_week,
        SUM(amount) as total
      `))
      .where('user_id', userId)
      .where('type', 'expense')
      .whereBetween('transaction_at', [startDate, endDate])
      .groupBy(db.raw('EXTRACT(DOW FROM transaction_at)'))
      .orderBy('day_of_week');

    return {
      daily_trend: dailyTrend,
      top_merchants: topMerchants,
      by_day_of_week: byDayOfWeek,
    };
  }
}

module.exports = new TransactionService();
