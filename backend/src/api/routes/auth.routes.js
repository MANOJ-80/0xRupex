const express = require('express');
const router = express.Router();
const authController = require('../controllers/auth.controller');
const { authenticate } = require('../middleware/auth.middleware');
const { validators } = require('../middleware/validate.middleware');

router.post('/register', validators.register, authController.register);
router.post('/login', validators.login, authController.login);
router.post('/refresh', validators.refreshToken, authController.refresh);

router.post('/logout', authenticate, authController.logout);
router.post('/logout-all', authenticate, authController.logoutAll);
router.get('/me', authenticate, authController.me);
router.post('/change-password', authenticate, validators.changePassword, authController.changePassword);

module.exports = router;
