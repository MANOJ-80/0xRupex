const express = require('express');
const router = express.Router();
const accountController = require('../controllers/account.controller');
const { authenticate } = require('../middleware/auth.middleware');
const { validators } = require('../middleware/validate.middleware');

// All routes require authentication
router.use(authenticate);

router.get('/', accountController.getAccounts);
router.get('/:id', validators.uuidParam, accountController.getAccount);
router.post('/', validators.createAccount, accountController.createAccount);
router.put('/:id', validators.uuidParam, accountController.updateAccount);
router.delete('/:id', validators.uuidParam, accountController.deleteAccount);

module.exports = router;
