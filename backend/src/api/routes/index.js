const express = require('express');
const router = express.Router();

const authRoutes = require('./auth.routes');
const accountRoutes = require('./account.routes');
const categoryRoutes = require('./category.routes');
const transactionRoutes = require('./transaction.routes');

// Health check
router.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    timestamp: new Date().toISOString(),
    version: '1.0.0',
  });
});

// API routes
router.use('/auth', authRoutes);
router.use('/accounts', accountRoutes);
router.use('/categories', categoryRoutes);
router.use('/transactions', transactionRoutes);

module.exports = router;
