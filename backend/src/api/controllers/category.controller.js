const categoryService = require('../../services/category.service');
const response = require('../../utils/response');

/**
 * Get all categories
 */
const getCategories = async (req, res, next) => {
  try {
    const { type } = req.query;
    const categories = await categoryService.getCategories(req.user.id, type);
    return response.success(res, { categories });
  } catch (error) {
    next(error);
  }
};

/**
 * Get category by ID
 */
const getCategory = async (req, res, next) => {
  try {
    const category = await categoryService.getCategoryById(req.params.id, req.user.id);
    return response.success(res, { category });
  } catch (error) {
    next(error);
  }
};

/**
 * Create category
 */
const createCategory = async (req, res, next) => {
  try {
    const category = await categoryService.createCategory(req.user.id, req.body);
    return response.success(res, { category }, 'Category created', 201);
  } catch (error) {
    next(error);
  }
};

/**
 * Update category
 */
const updateCategory = async (req, res, next) => {
  try {
    const category = await categoryService.updateCategory(
      req.params.id,
      req.user.id,
      req.body
    );
    return response.success(res, { category }, 'Category updated');
  } catch (error) {
    next(error);
  }
};

/**
 * Delete category
 */
const deleteCategory = async (req, res, next) => {
  try {
    await categoryService.deleteCategory(req.params.id, req.user.id);
    return response.success(res, null, 'Category deleted');
  } catch (error) {
    next(error);
  }
};

/**
 * Get category statistics
 */
const getCategoryStats = async (req, res, next) => {
  try {
    const { start_date, end_date } = req.query;
    const endDate = end_date ? new Date(end_date) : new Date();
    const startDate = start_date 
      ? new Date(start_date)
      : new Date(endDate.getFullYear(), endDate.getMonth(), 1);

    const stats = await categoryService.getCategoryStats(req.user.id, startDate, endDate);
    return response.success(res, { stats, start_date: startDate, end_date: endDate });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getCategories,
  getCategory,
  createCategory,
  updateCategory,
  deleteCategory,
  getCategoryStats,
};
