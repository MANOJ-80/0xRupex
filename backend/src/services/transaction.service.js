const { Transaction, Account, Category } = require('../models');
const { NotFoundError, ValidationError } = require('../utils/errors');
const accountService = require('./account.service');
const logger = require('../utils/logger');

const escapeRegex = (str) => {
  if (!str) return '';
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};

class TransactionService {
  async getTransactions(userId, options = {}) {
    const {
      page = 1,
      limit = 20,
      type,
      categoryId,
      accountId,
      startDate,
      endDate,
      search,
      sortBy = 'transactionAt',
      sortOrder = 'desc',
    } = options;

    const query = { user: userId };

    if (type) query.type = type;
    if (categoryId) query.category = categoryId;
    if (accountId) query.account = accountId;
    if (startDate || endDate) {
      query.transactionAt = {};
      if (startDate) query.transactionAt.$gte = new Date(startDate);
      if (endDate) query.transactionAt.$lte = new Date(endDate);
    }
    if (search) {
      const escapedSearch = escapeRegex(search);
      query.$or = [
        { description: { $regex: escapedSearch, $options: 'i' } },
        { merchant: { $regex: escapedSearch, $options: 'i' } },
        { notes: { $regex: escapedSearch, $options: 'i' } },
      ];
    }

    const total = await Transaction.countDocuments(query);
    const totalPages = Math.ceil(total / limit);
    const skip = (page - 1) * limit;

    const sort = {};
    sort[sortBy] = sortOrder === 'desc' ? -1 : 1;

    const transactions = await Transaction.find(query)
      .populate('category', 'name icon color')
      .populate('account', 'name')
      .sort(sort)
      .skip(skip)
      .limit(limit);

    const formattedTransactions = transactions.map((t) => {
      const obj = t.toSafeObject();
      if (t.category) {
        obj.categoryName = t.category.name;
        obj.categoryIcon = t.category.icon;
        obj.categoryColor = t.category.color;
      }
      if (t.account) {
        obj.accountName = t.account.name;
      }
      return obj;
    });

    return {
      transactions: formattedTransactions,
      pagination: {
        page,
        limit,
        total,
        totalPages,
      },
    };
  }

  async getTransactionById(transactionId, userId) {
    const transaction = await Transaction.findOne({ _id: transactionId, user: userId })
      .populate('category', 'name icon color')
      .populate('account', 'name');

    if (!transaction) {
      throw new NotFoundError('Transaction');
    }

    const obj = transaction.toSafeObject();
    if (transaction.category) {
      obj.categoryName = transaction.category.name;
      obj.categoryIcon = transaction.category.icon;
      obj.categoryColor = transaction.category.color;
    }
    if (transaction.account) {
      obj.accountName = transaction.account.name;
    }

    return obj;
  }

  async createTransaction(userId, data) {
    let categoryId = data.categoryId || data.category_id;
    if (!categoryId && data.categoryName) {
      const escapedName = escapeRegex(data.categoryName);
      const category = await Category.findOne({
        user: userId,
        name: { $regex: new RegExp(`^${escapedName}$`, 'i') },
      });
      if (category) {
        categoryId = category._id;
      }
    }

    let accountId = data.accountId || data.account_id;
    if (!accountId && data.last4Digits) {
      const account = await Account.findOne({
        user: userId,
        last4Digits: data.last4Digits,
        isActive: true,
      });
      if (account) {
        accountId = account._id;
      }
    }

    const transaction = await Transaction.create({
      user: userId,
      account: accountId || null,
      category: categoryId || null,
      type: data.type,
      amount: data.amount,
      description: data.description || data.merchant,
      merchant: data.merchant,
      referenceId: data.referenceId || data.reference_id,
      transactionAt: data.transactionAt || data.transaction_at || data.transactionDate || new Date(),
      location: data.location,
      notes: data.notes,
      tags: data.tags || [],
      isRecurring: data.isRecurring || false,
      source: data.source || 'manual',
      smsHash: data.smsHash || data.sms_hash,
    });

    if (accountId) {
      await accountService.updateBalance(accountId, userId, data.amount, data.type);
    }

    logger.info(
      `Transaction created: ${transaction._id} - ${data.type} ${data.amount} | Merchant: ${data.merchant || 'N/A'} | Category: ${data.categoryName || 'N/A'}`
    );

    return this.getTransactionById(transaction._id, userId);
  }

  async syncTransactions(userId, transactions) {
    if (!transactions || !Array.isArray(transactions) || transactions.length === 0) {
      return { created: 0, skipped: 0, errors: [] };
    }

    const results = {
      created: 0,
      skipped: 0,
      errors: [],
    };

    for (const tx of transactions) {
      try {
        if (tx.smsHash || tx.sms_hash) {
          const existing = await Transaction.findOne({
            user: userId,
            smsHash: tx.smsHash || tx.sms_hash,
          });

          if (existing) {
            results.skipped++;
            continue;
          }
        }

        await this.createTransaction(userId, {
          ...tx,
          smsHash: tx.smsHash || tx.sms_hash,
          source: tx.source || 'sms',
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

  async updateTransaction(transactionId, userId, data) {
    const transaction = await Transaction.findOne({ _id: transactionId, user: userId });

    if (!transaction) {
      throw new NotFoundError('Transaction');
    }

    if (data.categoryName && !data.categoryId && !data.category_id) {
      const escapedName = escapeRegex(data.categoryName);
      const category = await Category.findOne({
        user: userId,
        name: { $regex: new RegExp(`^${escapedName}$`, 'i') },
      });
      if (category) {
        data.categoryId = category._id;
      }
    }

    const amountChanged = data.amount !== undefined && data.amount !== transaction.amount;
    const typeChanged = data.type !== undefined && data.type !== transaction.type;
    const accountChanged =
      (data.accountId || data.account_id) &&
      (data.accountId || data.account_id) !== transaction.account?.toString();

    if ((amountChanged || typeChanged || accountChanged) && transaction.account) {
      const reverseType = transaction.type === 'expense' ? 'income' : 'expense';
      await accountService.updateBalance(
        transaction.account,
        userId,
        transaction.amount,
        reverseType
      );

      const newAccountId = data.accountId || data.account_id || transaction.account;
      const newType = data.type || transaction.type;
      const newAmount = data.amount !== undefined ? data.amount : transaction.amount;
      await accountService.updateBalance(newAccountId, userId, newAmount, newType);
    }

    if (data.accountId !== undefined) transaction.account = data.accountId;
    if (data.account_id !== undefined) transaction.account = data.account_id;
    if (data.categoryId !== undefined) transaction.category = data.categoryId;
    if (data.category_id !== undefined) transaction.category = data.category_id;
    if (data.type !== undefined) transaction.type = data.type;
    if (data.amount !== undefined) transaction.amount = data.amount;
    if (data.description !== undefined) transaction.description = data.description;
    if (data.merchant !== undefined) transaction.merchant = data.merchant;
    if (data.transactionAt !== undefined) transaction.transactionAt = data.transactionAt;
    if (data.transaction_at !== undefined) transaction.transactionAt = data.transaction_at;
    if (data.transactionDate !== undefined) transaction.transactionAt = data.transactionDate;
    if (data.notes !== undefined) transaction.notes = data.notes;
    if (data.tags !== undefined) transaction.tags = data.tags;

    await transaction.save();

    return this.getTransactionById(transactionId, userId);
  }

  async deleteTransaction(transactionId, userId) {
    const transaction = await Transaction.findOne({ _id: transactionId, user: userId });

    if (!transaction) {
      throw new NotFoundError('Transaction');
    }

    if (transaction.account) {
      const reverseType = transaction.type === 'expense' ? 'income' : 'expense';
      await accountService.updateBalance(
        transaction.account,
        userId,
        transaction.amount,
        reverseType
      );
    }

    await Transaction.deleteOne({ _id: transactionId });
  }

  async getMonthlySummary(userId, year, month) {
    const startDate = new Date(year, month - 1, 1);
    const endDate = new Date(year, month, 0, 23, 59, 59, 999);

    const result = await Transaction.aggregate([
      {
        $match: {
          user: userId,
          transactionAt: { $gte: startDate, $lte: endDate },
        },
      },
      {
        $group: {
          _id: null,
          totalIncome: {
            $sum: { $cond: [{ $eq: ['$type', 'income'] }, '$amount', 0] },
          },
          totalExpense: {
            $sum: { $cond: [{ $eq: ['$type', 'expense'] }, '$amount', 0] },
          },
          transactionCount: { $sum: 1 },
        },
      },
    ]);

    const summary = result[0] || { totalIncome: 0, totalExpense: 0, transactionCount: 0 };

    return {
      year,
      month,
      totalIncome: summary.totalIncome || 0,
      totalExpense: summary.totalExpense || 0,
      netSavings: (summary.totalIncome || 0) - (summary.totalExpense || 0),
      transactionCount: summary.transactionCount || 0,
    };
  }

  async getAnalytics(userId, startDate, endDate) {
    const dailyTrend = await Transaction.aggregate([
      {
        $match: {
          user: userId,
          transactionAt: { $gte: startDate, $lte: endDate },
        },
      },
      {
        $group: {
          _id: {
            $dateToString: { format: '%Y-%m-%d', date: '$transactionAt' },
          },
          expense: {
            $sum: { $cond: [{ $eq: ['$type', 'expense'] }, '$amount', 0] },
          },
          income: {
            $sum: { $cond: [{ $eq: ['$type', 'income'] }, '$amount', 0] },
          },
        },
      },
      { $sort: { _id: 1 } },
      { $project: { date: '$_id', expense: 1, income: 1, _id: 0 } },
    ]);

    const topMerchants = await Transaction.aggregate([
      {
        $match: {
          user: userId,
          type: 'expense',
          merchant: { $ne: null },
          transactionAt: { $gte: startDate, $lte: endDate },
        },
      },
      {
        $group: {
          _id: '$merchant',
          total: { $sum: '$amount' },
          count: { $sum: 1 },
        },
      },
      { $sort: { total: -1 } },
      { $limit: 10 },
      { $project: { merchant: '$_id', total: 1, count: 1, _id: 0 } },
    ]);

    const byDayOfWeek = await Transaction.aggregate([
      {
        $match: {
          user: userId,
          type: 'expense',
          transactionAt: { $gte: startDate, $lte: endDate },
        },
      },
      {
        $group: {
          _id: { $dayOfWeek: '$transactionAt' },
          total: { $sum: '$amount' },
        },
      },
      { $sort: { _id: 1 } },
      { $project: { dayOfWeek: { $subtract: ['$_id', 1] }, total: 1, _id: 0 } },
    ]);

    return {
      dailyTrend,
      topMerchants,
      byDayOfWeek,
    };
  }
}

module.exports = new TransactionService();
