const { Category, Transaction } = require('../models');
const { NotFoundError, ValidationError } = require('../utils/errors');
const config = require('../config');

class CategoryService {
  async getCategories(userId, type = null) {
    const query = { user: userId };
    if (type) {
      query.type = type;
    }
    return Category.find(query).sort({ type: 1, name: 1 });
  }

  async getCategoryById(categoryId, userId) {
    const category = await Category.findOne({ _id: categoryId, user: userId });
    if (!category) {
      throw new NotFoundError('Category');
    }
    return category;
  }

  async createCategory(userId, data) {
    const existing = await Category.findOne({
      user: userId,
      name: data.name,
      type: data.type,
    });

    if (existing) {
      throw new ValidationError('Category with this name already exists');
    }

    const category = await Category.create({
      user: userId,
      name: data.name,
      type: data.type,
      icon: data.icon,
      color: data.color,
      isSystem: false,
    });

    return category;
  }

  async updateCategory(categoryId, userId, data) {
    const category = await this.getCategoryById(categoryId, userId);

    if (category.isSystem && data.name && data.name !== category.name) {
      throw new ValidationError('Cannot rename system categories');
    }

    if (data.name !== undefined) category.name = data.name;
    if (data.icon !== undefined) category.icon = data.icon;
    if (data.color !== undefined) category.color = data.color;

    await category.save();
    return category;
  }

  async deleteCategory(categoryId, userId) {
    const category = await this.getCategoryById(categoryId, userId);

    if (category.isSystem) {
      throw new ValidationError('Cannot delete system categories');
    }

    const txCount = await Transaction.countDocuments({ category: categoryId });

    if (txCount > 0) {
      throw new ValidationError('Cannot delete category with transactions');
    }

    await Category.deleteOne({ _id: categoryId });
  }

  async initializeDefaults(userId) {
    const existing = await Category.countDocuments({ user: userId });

    if (existing > 0) {
      return;
    }

    const categories = [];

    for (const cat of config.DEFAULT_CATEGORIES.expense) {
      categories.push({
        user: userId,
        name: cat.name,
        type: 'expense',
        icon: cat.icon,
        color: cat.color,
        isSystem: true,
      });
    }

    for (const cat of config.DEFAULT_CATEGORIES.income) {
      categories.push({
        user: userId,
        name: cat.name,
        type: 'income',
        icon: cat.icon,
        color: cat.color,
        isSystem: true,
      });
    }

    await Category.insertMany(categories);
  }

  async getCategoryStats(userId, startDate, endDate) {
    const stats = await Transaction.aggregate([
      {
        $match: {
          user: userId,
          type: 'expense',
          transactionAt: { $gte: startDate, $lte: endDate },
          category: { $ne: null },
        },
      },
      {
        $lookup: {
          from: 'categories',
          localField: 'category',
          foreignField: '_id',
          as: 'categoryInfo',
        },
      },
      {
        $unwind: '$categoryInfo',
      },
      {
        $group: {
          _id: '$category',
          categoryId: { $first: '$_id' },
          categoryName: { $first: '$categoryInfo.name' },
          icon: { $first: '$categoryInfo.icon' },
          color: { $first: '$categoryInfo.color' },
          total: { $sum: '$amount' },
          count: { $sum: 1 },
        },
      },
      {
        $sort: { total: -1 },
      },
    ]);

    return stats.map((s) => ({
      categoryId: s._id,
      categoryName: s.categoryName,
      icon: s.icon,
      color: s.color,
      total: s.total,
      count: s.count,
    }));
  }

  async findByName(userId, name, type = null) {
    const query = {
      user: userId,
      name: { $regex: new RegExp(`^${name}$`, 'i') },
    };
    if (type) query.type = type;
    return Category.findOne(query);
  }
}

module.exports = new CategoryService();
