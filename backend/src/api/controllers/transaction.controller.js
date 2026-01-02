const transactionService = require('../../services/transaction.service');
const response = require('../../utils/response');

/**
 * Get transactions
 */
const getTransactions = async (req, res, next) => {
  try {
    const result = await transactionService.getTransactions(req.user.id, req.query);
    return response.paginated(res, result.transactions, result.pagination);
  } catch (error) {
    next(error);
  }
};

/**
 * Get transaction by ID
 */
const getTransaction = async (req, res, next) => {
  try {
    const transaction = await transactionService.getTransactionById(
      req.params.id,
      req.user.id
    );
    return response.success(res, { transaction });
  } catch (error) {
    next(error);
  }
};

/**
 * Create transaction
 */
const createTransaction = async (req, res, next) => {
  try {
    const transaction = await transactionService.createTransaction(req.user.id, req.body);
    return response.success(res, { transaction }, 'Transaction created', 201);
  } catch (error) {
    next(error);
  }
};

/**
 * Sync transactions from Android app
 */
const syncTransactions = async (req, res, next) => {
  try {
    const { transactions } = req.body;
    const result = await transactionService.syncTransactions(req.user.id, transactions);
    return response.success(res, result, 'Sync completed');
  } catch (error) {
    next(error);
  }
};

/**
 * Update transaction
 */
const updateTransaction = async (req, res, next) => {
  try {
    const transaction = await transactionService.updateTransaction(
      req.params.id,
      req.user.id,
      req.body
    );
    return response.success(res, { transaction }, 'Transaction updated');
  } catch (error) {
    next(error);
  }
};

/**
 * Delete transaction
 */
const deleteTransaction = async (req, res, next) => {
  try {
    await transactionService.deleteTransaction(req.params.id, req.user.id);
    return response.success(res, null, 'Transaction deleted');
  } catch (error) {
    next(error);
  }
};

/**
 * Get monthly summary
 */
const getMonthlySummary = async (req, res, next) => {
  try {
    const now = new Date();
    const year = parseInt(req.query.year) || now.getFullYear();
    const month = parseInt(req.query.month) || now.getMonth() + 1;

    const summary = await transactionService.getMonthlySummary(req.user.id, year, month);
    return response.success(res, { summary });
  } catch (error) {
    next(error);
  }
};

/**
 * Get analytics
 */
const getAnalytics = async (req, res, next) => {
  try {
    const { start_date, end_date } = req.query;
    const endDate = end_date ? new Date(end_date) : new Date();
    const startDate = start_date
      ? new Date(start_date)
      : new Date(endDate.getFullYear(), endDate.getMonth(), 1);

    const analytics = await transactionService.getAnalytics(req.user.id, startDate, endDate);
    return response.success(res, { analytics, start_date: startDate, end_date: endDate });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getTransactions,
  getTransaction,
  createTransaction,
  syncTransactions,
  updateTransaction,
  deleteTransaction,
  getMonthlySummary,
  getAnalytics,
};
