const express = require('express');
const router = express.Router();
const categoryController = require('../controllers/category.controller');
const { authenticate } = require('../middleware/auth.middleware');
const { validators } = require('../middleware/validate.middleware');

// All routes require authentication
router.use(authenticate);

router.get('/', categoryController.getCategories);
router.get('/stats', validators.dateRange, categoryController.getCategoryStats);
router.get('/:id', validators.uuidParam, categoryController.getCategory);
router.post('/', validators.createCategory, categoryController.createCategory);
router.put('/:id', validators.uuidParam, categoryController.updateCategory);
router.delete('/:id', validators.uuidParam, categoryController.deleteCategory);

module.exports = router;
