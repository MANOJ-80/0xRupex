const { validationResult, body, param, query } = require('express-validator');
const { ValidationError } = require('../../utils/errors');

/**
 * Validate request and throw if errors
 */
const validate = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const messages = errors.array().map((err) => ({
      field: err.path,
      message: err.msg,
    }));
    throw new ValidationError('Validation failed', messages);
  }
  next();
};

/**
 * Common validators
 */
const validators = {
  // Auth
  register: [
    body('email').isEmail().normalizeEmail().withMessage('Valid email required'),
    body('password')
      .isLength({ min: 8 })
      .withMessage('Password must be at least 8 characters')
      .matches(/[A-Z]/)
      .withMessage('Password must contain uppercase letter')
      .matches(/[0-9]/)
      .withMessage('Password must contain number'),
    body('name').trim().isLength({ min: 2, max: 100 }).withMessage('Name required (2-100 chars)'),
    validate,
  ],

  login: [
    body('email').isEmail().normalizeEmail().withMessage('Valid email required'),
    body('password').notEmpty().withMessage('Password required'),
    validate,
  ],

  refreshToken: [
    body('refreshToken').notEmpty().withMessage('Refresh token required'),
    validate,
  ],

  // Accounts
  createAccount: [
    body('name').trim().notEmpty().withMessage('Account name required'),
    body('type')
      .isIn(['savings', 'current', 'credit_card', 'wallet', 'cash', 'investment'])
      .withMessage('Invalid account type'),
    body('bank_name').optional().trim(),
    body('account_number').optional().trim(),
    body('current_balance').optional().isNumeric().withMessage('Balance must be numeric'),
    validate,
  ],

  // Categories
  createCategory: [
    body('name').trim().notEmpty().withMessage('Category name required'),
    body('type').isIn(['expense', 'income']).withMessage('Type must be expense or income'),
    body('icon').optional().trim(),
    body('color').optional().isHexColor().withMessage('Invalid color format'),
    validate,
  ],

  // Transactions
  createTransaction: [
    body('type').isIn(['expense', 'income', 'transfer']).withMessage('Invalid transaction type'),
    body('amount').isFloat({ min: 0.01 }).withMessage('Amount must be positive'),
    body('description').optional().trim(),
    body('merchant').optional().trim(),
    body('account_id').optional().isUUID().withMessage('Invalid account ID'),
    body('category_id').optional().isUUID().withMessage('Invalid category ID'),
    body('transaction_date').optional().isISO8601().withMessage('Invalid date format'),
    validate,
  ],

  syncTransactions: [
    body('transactions').isArray().withMessage('Transactions must be an array'),
    body('transactions.*.type')
      .isIn(['expense', 'income', 'transfer'])
      .withMessage('Invalid transaction type'),
    body('transactions.*.amount').isFloat({ min: 0.01 }).withMessage('Amount must be positive'),
    validate,
  ],

  // Common
  uuidParam: [
    param('id').isUUID().withMessage('Invalid ID format'),
    validate,
  ],

  pagination: [
    query('page').optional().isInt({ min: 1 }).withMessage('Page must be positive integer'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('Limit must be 1-100'),
    validate,
  ],

  dateRange: [
    query('start_date').optional().isISO8601().withMessage('Invalid start date'),
    query('end_date').optional().isISO8601().withMessage('Invalid end date'),
    validate,
  ],
};

module.exports = {
  validate,
  validators,
};
