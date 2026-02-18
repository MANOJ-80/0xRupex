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

  changePassword: [
    body('currentPassword').notEmpty().withMessage('Current password required'),
    body('newPassword')
      .isLength({ min: 8 })
      .withMessage('Password must be at least 8 characters')
      .matches(/[A-Z]/)
      .withMessage('Password must contain uppercase letter')
      .matches(/[0-9]/)
      .withMessage('Password must contain number'),
    validate,
  ],

  createAccount: [
    body('name').trim().notEmpty().withMessage('Account name required'),
    body('type')
      .isIn(['bank', 'wallet', 'cash', 'credit_card'])
      .withMessage('Invalid account type'),
    body('bankName').optional().trim(),
    body('institution').optional().trim(),
    body('accountNumber').optional().trim(),
    body('balance').optional().isFloat({ min: 0 }).withMessage('Balance must be non-negative'),
    validate,
  ],

  updateAccount: [
    body('name').optional().trim().notEmpty().withMessage('Account name cannot be empty'),
    body('type').optional().isIn(['bank', 'wallet', 'cash', 'credit_card']).withMessage('Invalid account type'),
    body('institution').optional().trim(),
    body('accountNumber').optional().trim(),
    body('last4Digits').optional().trim().isLength({ max: 4 }).withMessage('Last 4 digits max 4 chars'),
    body('balance').optional().isFloat({ min: 0 }).withMessage('Balance must be non-negative'),
    body('color').optional().isHexColor().withMessage('Invalid color format'),
    validate,
  ],

  createCategory: [
    body('name').trim().notEmpty().withMessage('Category name required'),
    body('type').isIn(['expense', 'income']).withMessage('Type must be expense or income'),
    body('icon').optional().trim(),
    body('color').optional().isHexColor().withMessage('Invalid color format'),
    validate,
  ],

  updateCategory: [
    body('name').optional().trim().notEmpty().withMessage('Category name cannot be empty'),
    body('icon').optional().trim(),
    body('color').optional().isHexColor().withMessage('Invalid color format'),
    validate,
  ],

  createTransaction: [
    body('type').isIn(['expense', 'income', 'transfer']).withMessage('Invalid transaction type'),
    body('amount').isFloat({ min: 0.01 }).withMessage('Amount must be positive'),
    body('description').optional().trim(),
    body('merchant').optional().trim(),
    body('accountId').optional().custom(isValidObjectId).withMessage('Invalid account ID'),
    body('categoryId').optional().custom(isValidObjectId).withMessage('Invalid category ID'),
    body('categoryName').optional().trim(),
    body('transactionDate').optional().isISO8601().withMessage('Invalid date format'),
    validate,
  ],

  updateTransaction: [
    body('type').optional().isIn(['expense', 'income', 'transfer']).withMessage('Invalid transaction type'),
    body('amount').optional().isFloat({ min: 0.01 }).withMessage('Amount must be positive'),
    body('description').optional().trim(),
    body('merchant').optional().trim(),
    body('accountId').optional().custom(isValidObjectId).withMessage('Invalid account ID'),
    body('categoryId').optional().custom(isValidObjectId).withMessage('Invalid category ID'),
    body('categoryName').optional().trim(),
    body('notes').optional().trim(),
    body('transactionDate').optional().isISO8601().withMessage('Invalid date format'),
    validate,
  ],

  syncTransactions: [
    body('transactions').isArray({ min: 1 }).withMessage('Transactions must be a non-empty array'),
    body('transactions.*.type').isIn(['expense', 'income', 'transfer']).withMessage('Invalid transaction type'),
    body('transactions.*.amount').isFloat({ min: 0.01 }).withMessage('Amount must be positive'),
    validate,
  ],

  objectIdParam: [
    param('id').custom(isValidObjectId).withMessage('Invalid ID format'),
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
