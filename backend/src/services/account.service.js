const { Account, Transaction } = require('../models');
const { NotFoundError, ValidationError } = require('../utils/errors');

class AccountService {
  async getAccounts(userId) {
    return Account.find({ user: userId, isActive: true }).sort({ name: 1 });
  }

  async getAccountById(accountId, userId) {
    const account = await Account.findOne({ _id: accountId, user: userId });
    if (!account) {
      throw new NotFoundError('Account');
    }
    return account;
  }

  async createAccount(userId, data) {
    const account = await Account.create({
      user: userId,
      name: data.name,
      type: data.type,
      institution: data.institution || data.bankName,
      accountNumber: data.accountNumber,
      last4Digits: data.last4Digits || data.last_4_digits,
      balance: data.balance || data.currentBalance || 0,
      icon: data.icon || 'wallet',
      color: data.color || '#6366f1',
    });
    return account;
  }

  async updateAccount(accountId, userId, data) {
    const account = await this.getAccountById(accountId, userId);

    if (data.name !== undefined) account.name = data.name;
    if (data.type !== undefined) account.type = data.type;
    if (data.institution !== undefined) account.institution = data.institution;
    if (data.bankName !== undefined) account.institution = data.bankName;
    if (data.accountNumber !== undefined) account.accountNumber = data.accountNumber;
    if (data.last4Digits !== undefined) account.last4Digits = data.last4Digits;
    if (data.last_4_digits !== undefined) account.last4Digits = data.last_4_digits;
    if (data.balance !== undefined) account.balance = data.balance;
    if (data.currentBalance !== undefined) account.balance = data.currentBalance;
    if (data.icon !== undefined) account.icon = data.icon;
    if (data.color !== undefined) account.color = data.color;
    if (data.isActive !== undefined) account.isActive = data.isActive;

    await account.save();
    return account;
  }

  async deleteAccount(accountId, userId) {
    const account = await this.getAccountById(accountId, userId);

    const txCount = await Transaction.countDocuments({ account: accountId });

    if (txCount > 0) {
      account.isActive = false;
      await account.save();
    } else {
      await Account.deleteOne({ _id: accountId });
    }
  }

  async updateBalance(accountId, userId, amount, type) {
    const account = await this.getAccountById(accountId, userId);

    let newBalance = account.balance;
    if (type === 'expense') {
      newBalance -= parseFloat(amount);
    } else if (type === 'income') {
      newBalance += parseFloat(amount);
    } else if (type === 'transfer') {
      newBalance -= parseFloat(amount);
    }

    account.balance = newBalance;
    await account.save();

    return newBalance;
  }

  async getTotalBalance(userId) {
    const result = await Account.aggregate([
      { $match: { user: userId, isActive: true } },
      { $group: { _id: null, total: { $sum: '$balance' } } },
    ]);

    return result.length > 0 ? result[0].total : 0;
  }

  async getByLast4Digits(userId, last4Digits) {
    return Account.findOne({ user: userId, last4Digits, isActive: true });
  }
}

module.exports = new AccountService();
