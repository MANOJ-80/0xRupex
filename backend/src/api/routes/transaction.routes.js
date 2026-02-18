const express = require('express');
const router = express.Router();
const transactionController = require('../controllers/transaction.controller');
const { authenticate } = require('../middleware/auth.middleware');
const { validators } = require('../middleware/validate.middleware');

router.use(authenticate);

router.get('/', validators.pagination, transactionController.getTransactions);
router.get('/summary', transactionController.getMonthlySummary);
router.get('/analytics', validators.dateRange, transactionController.getAnalytics);
router.get('/:id', validators.objectIdParam, transactionController.getTransaction);
router.post('/', validators.createTransaction, transactionController.createTransaction);
router.post('/sync', validators.syncTransactions, transactionController.syncTransactions);
router.put('/:id', validators.objectIdParam, validators.updateTransaction, transactionController.updateTransaction);
router.delete('/:id', validators.objectIdParam, transactionController.deleteTransaction);

module.exports = router;
