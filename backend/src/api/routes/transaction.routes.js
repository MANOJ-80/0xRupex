const express = require('express');
const router = express.Router();
const transactionController = require('../controllers/transaction.controller');
const { authenticate } = require('../middleware/auth.middleware');
const { validators } = require('../middleware/validate.middleware');

// All routes require authentication
router.use(authenticate);

router.get('/', validators.pagination, transactionController.getTransactions);
router.get('/summary', transactionController.getMonthlySummary);
router.get('/analytics', validators.dateRange, transactionController.getAnalytics);
router.get('/:id', validators.uuidParam, transactionController.getTransaction);
router.post('/', validators.createTransaction, transactionController.createTransaction);
router.post('/sync', validators.syncTransactions, transactionController.syncTransactions);
router.put('/:id', validators.uuidParam, transactionController.updateTransaction);
router.delete('/:id', validators.uuidParam, transactionController.deleteTransaction);

module.exports = router;
