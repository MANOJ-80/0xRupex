const categoryService = require('../../services/category.service');
const response = require('../../utils/response');

const getCategories = async (req, res, next) => {
  try {
    const { type } = req.query;
    const categories = await categoryService.getCategories(req.user.id, type);
    const categoriesJson = categories.map((c) => c.toSafeObject());
    return response.success(res, { categories: categoriesJson });
  } catch (error) {
    next(error);
  }
};

const getCategory = async (req, res, next) => {
  try {
    const category = await categoryService.getCategoryById(req.params.id, req.user.id);
    return response.success(res, { category: category.toSafeObject() });
  } catch (error) {
    next(error);
  }
};

const createCategory = async (req, res, next) => {
  try {
    const category = await categoryService.createCategory(req.user.id, req.body);
    return response.success(res, { category: category.toSafeObject() }, 'Category created', 201);
  } catch (error) {
    next(error);
  }
};

const updateCategory = async (req, res, next) => {
  try {
    const category = await categoryService.updateCategory(req.params.id, req.user.id, req.body);
    return response.success(res, { category: category.toSafeObject() }, 'Category updated');
  } catch (error) {
    next(error);
  }
};

const deleteCategory = async (req, res, next) => {
  try {
    await categoryService.deleteCategory(req.params.id, req.user.id);
    return response.success(res, null, 'Category deleted');
  } catch (error) {
    next(error);
  }
};

const getCategoryStats = async (req, res, next) => {
  try {
    const { startDate, endDate } = req.query;
    const end = endDate ? new Date(endDate) : new Date();
    const start = startDate ? new Date(startDate) : new Date(end.getFullYear(), end.getMonth(), 1);

    const stats = await categoryService.getCategoryStats(req.user.id, start, end);
    return response.success(res, { stats, startDate: start, endDate: end });
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
