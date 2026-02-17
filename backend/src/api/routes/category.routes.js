const express = require('express');
const router = express.Router();
const categoryController = require('../controllers/category.controller');
const { authenticate } = require('../middleware/auth.middleware');
const { validators } = require('../middleware/validate.middleware');

router.use(authenticate);

router.get('/', categoryController.getCategories);
router.get('/stats', validators.dateRange, categoryController.getCategoryStats);
router.get('/:id', validators.objectIdParam, categoryController.getCategory);
router.post('/', validators.createCategory, categoryController.createCategory);
router.put('/:id', validators.objectIdParam, categoryController.updateCategory);
router.delete('/:id', validators.objectIdParam, categoryController.deleteCategory);

module.exports = router;
