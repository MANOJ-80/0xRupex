const { Account, Transaction } = require('../models');
const { NotFoundError, ValidationError } = require('../utils/errors');
const mongoose = require('mongoose');

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
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount < 0) {
      throw new ValidationError('Invalid amount');
    }

    let updateOp = {};
    if (type === 'expense' || type === 'transfer') {
      updateOp = { $inc: { balance: -numAmount } };
    } else if (type === 'income') {
      updateOp = { $inc: { balance: numAmount } };
    } else {
      return null;
    }

    const account = await Account.findOneAndUpdate(
      { _id: accountId, user: userId },
      updateOp,
      { new: true }
    );

    if (!account) {
      throw new NotFoundError('Account');
    }

    return account.balance;
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
