const db = require('../config/database');
const { NotFoundError, ValidationError } = require('../utils/errors');
const { DEFAULT_CATEGORIES } = require('../config/constants');

/**
 * Category Service
 */
class CategoryService {
  /**
   * Get all categories for user
   */
  async getCategories(userId, type = null) {
    let query = db('categories')
      .where({ user_id: userId })
      .orderBy('type')
      .orderBy('name');

    if (type) {
      query = query.where({ type });
    }

    return query;
  }

  /**
   * Get category by ID
   */
  async getCategoryById(categoryId, userId) {
    const category = await db('categories')
      .where({ id: categoryId, user_id: userId })
      .first();

    if (!category) {
      throw new NotFoundError('Category');
    }

    return category;
  }

  /**
   * Create category
   */
  async createCategory(userId, data) {
    // Check for duplicate name
    const existing = await db('categories')
      .where({ user_id: userId, name: data.name, type: data.type })
      .first();

    if (existing) {
      throw new ValidationError('Category with this name already exists');
    }

    const [category] = await db('categories')
      .insert({
        user_id: userId,
        name: data.name,
        type: data.type,
        icon: data.icon,
        color: data.color,
        is_system: false,
      })
      .returning('*');

    return category;
  }

  /**
   * Update category
   */
  async updateCategory(categoryId, userId, data) {
    const category = await this.getCategoryById(categoryId, userId);

    if (category.is_system && data.name !== category.name) {
      throw new ValidationError('Cannot rename system categories');
    }

    const [updated] = await db('categories')
      .where({ id: categoryId, user_id: userId })
      .update({
        name: data.name ?? category.name,
        icon: data.icon ?? category.icon,
        color: data.color ?? category.color,
      })
      .returning('*');

    return updated;
  }

  /**
   * Delete category
   */
  async deleteCategory(categoryId, userId) {
    const category = await this.getCategoryById(categoryId, userId);

    if (category.is_system) {
      throw new ValidationError('Cannot delete system categories');
    }

    // Check if category has transactions
    const txCount = await db('transactions')
      .where({ category_id: categoryId })
      .count('id as count')
      .first();

    if (parseInt(txCount.count) > 0) {
      throw new ValidationError('Cannot delete category with transactions');
    }

    await db('categories').where({ id: categoryId }).del();
  }

  /**
   * Initialize default categories for user
   */
  async initializeDefaults(userId) {
    const existing = await db('categories')
      .where({ user_id: userId })
      .count('id as count')
      .first();

    if (parseInt(existing.count) > 0) {
      return; // Already initialized
    }

    const categories = DEFAULT_CATEGORIES.map((cat) => ({
      ...cat,
      user_id: userId,
      is_system: true,
    }));

    await db('categories').insert(categories);
  }

  /**
   * Get category statistics
   */
  async getCategoryStats(userId, startDate, endDate) {
    return db('transactions')
      .select('category_id')
      .select('categories.name as category_name')
      .select('categories.icon')
      .select('categories.color')
      .sum('amount as total')
      .count('transactions.id as count')
      .join('categories', 'transactions.category_id', 'categories.id')
      .where('transactions.user_id', userId)
      .where('transactions.type', 'expense')
      .whereBetween('transactions.transaction_at', [startDate, endDate])
      .groupBy('category_id', 'categories.name', 'categories.icon', 'categories.color')
      .orderBy('total', 'desc');
  }
}

module.exports = new CategoryService();
