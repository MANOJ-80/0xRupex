const { validationResult, body, param, query } = require('express-validator');
const { ValidationError } = require('../../utils/errors');
const mongoose = require('mongoose');

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

const isValidObjectId = (value) => {
  return mongoose.Types.ObjectId.isValid(value);
};

const validators = {
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

  createAccount: [
    body('name').trim().notEmpty().withMessage('Account name required'),
    body('type')
      .isIn(['bank', 'wallet', 'cash', 'credit_card'])
      .withMessage('Invalid account type'),
    body('bankName').optional().trim(),
    body('accountNumber').optional().trim(),
    body('currentBalance').optional().isNumeric().withMessage('Balance must be numeric'),
    validate,
  ],

  createCategory: [
    body('name').trim().notEmpty().withMessage('Category name required'),
    body('type').isIn(['expense', 'income']).withMessage('Type must be expense or income'),
    body('icon').optional().trim(),
    body('color').optional().isHexColor().withMessage('Invalid color format'),
    validate,
  ],

  createTransaction: [
    body('type').isIn(['expense', 'income', 'transfer']).withMessage('Invalid transaction type'),
    body('amount').isFloat({ min: 0.01 }).withMessage('Amount must be positive'),
    body('description').optional().trim(),
    body('merchant').optional().trim(),
    body('accountId')
      .optional()
      .custom((value) => isValidObjectId(value))
      .withMessage('Invalid account ID'),
    body('categoryId')
      .optional()
      .custom((value) => isValidObjectId(value))
      .withMessage('Invalid category ID'),
    body('transactionDate').optional().isISO8601().withMessage('Invalid date format'),
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

  objectIdParam: [
    param('id')
      .custom((value) => isValidObjectId(value))
      .withMessage('Invalid ID format'),
    validate,
  ],

  pagination: [
    query('page').optional().isInt({ min: 1 }).withMessage('Page must be positive integer'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('Limit must be 1-100'),
    validate,
  ],

  dateRange: [
    query('startDate').optional().isISO8601().withMessage('Invalid start date'),
    query('endDate').optional().isISO8601().withMessage('Invalid end date'),
    validate,
  ],
};

module.exports = {
  validate,
  validators,
  isValidObjectId,
};
