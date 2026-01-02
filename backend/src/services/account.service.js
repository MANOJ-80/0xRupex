const db = require('../config/database');
const { NotFoundError, ValidationError } = require('../utils/errors');

/**
 * Account Service
 */
class AccountService {
  /**
   * Get all accounts for user
   */
  async getAccounts(userId) {
    return db('accounts')
      .where({ user_id: userId, is_active: true })
      .orderBy('name');
  }

  /**
   * Get account by ID
   */
  async getAccountById(accountId, userId) {
    const account = await db('accounts')
      .where({ id: accountId, user_id: userId })
      .first();

    if (!account) {
      throw new NotFoundError('Account');
    }

    return account;
  }

  /**
   * Create new account
   */
  async createAccount(userId, data) {
    const [account] = await db('accounts')
      .insert({
        user_id: userId,
        name: data.name,
        type: data.type,
        institution: data.institution || data.bank_name,
        account_number: data.account_number,
        last_4_digits: data.last_4_digits,
        balance: data.balance || data.current_balance || 0,
        icon: data.icon || 'wallet',
        color: data.color || '#6366f1',
      })
      .returning('*');

    return account;
  }

  /**
   * Update account
   */
  async updateAccount(accountId, userId, data) {
    const account = await this.getAccountById(accountId, userId);

    const [updated] = await db('accounts')
      .where({ id: accountId, user_id: userId })
      .update({
        name: data.name ?? account.name,
        type: data.type ?? account.type,
        institution: data.institution ?? data.bank_name ?? account.institution,
        account_number: data.account_number ?? account.account_number,
        last_4_digits: data.last_4_digits ?? account.last_4_digits,
        balance: data.balance ?? data.current_balance ?? account.balance,
        icon: data.icon ?? account.icon,
        color: data.color ?? account.color,
        is_active: data.is_active ?? account.is_active,
        updated_at: new Date(),
      })
      .returning('*');

    return updated;
  }

  /**
   * Delete account (soft delete)
   */
  async deleteAccount(accountId, userId) {
    const account = await this.getAccountById(accountId, userId);

    // Check if account has transactions
    const txCount = await db('transactions')
      .where({ account_id: accountId })
      .count('id as count')
      .first();

    if (parseInt(txCount.count) > 0) {
      // Soft delete
      await db('accounts')
        .where({ id: accountId })
        .update({ is_active: false });
    } else {
      // Hard delete if no transactions
      await db('accounts').where({ id: accountId }).del();
    }
  }

  /**
   * Update account balance
   */
  async updateBalance(accountId, userId, amount, type) {
    const account = await this.getAccountById(accountId, userId);
    
    let newBalance = parseFloat(account.balance);
    if (type === 'expense') {
      newBalance -= parseFloat(amount);
    } else if (type === 'income') {
      newBalance += parseFloat(amount);
    }

    await db('accounts')
      .where({ id: accountId })
      .update({ 
        balance: newBalance,
        updated_at: new Date(),
      });

    return newBalance;
  }

  /**
   * Get total balance across all accounts
   */
  async getTotalBalance(userId) {
    const result = await db('accounts')
      .where({ user_id: userId, is_active: true })
      .sum('balance as total')
      .first();

    return parseFloat(result.total || 0);
  }
}

module.exports = new AccountService();
