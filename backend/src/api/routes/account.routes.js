const express = require('express');
const router = express.Router();
const accountController = require('../controllers/account.controller');
const { authenticate } = require('../middleware/auth.middleware');
const { validators } = require('../middleware/validate.middleware');

router.use(authenticate);

router.get('/', accountController.getAccounts);
router.get('/:id', validators.objectIdParam, accountController.getAccount);
router.post('/', validators.createAccount, accountController.createAccount);
router.put('/:id', validators.objectIdParam, accountController.updateAccount);
router.delete('/:id', validators.objectIdParam, accountController.deleteAccount);

module.exports = router;
