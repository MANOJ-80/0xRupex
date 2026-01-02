const accountService = require('../../services/account.service');
const response = require('../../utils/response');

/**
 * Get all accounts
 */
const getAccounts = async (req, res, next) => {
  try {
    const accounts = await accountService.getAccounts(req.user.id);
    const totalBalance = await accountService.getTotalBalance(req.user.id);
    return response.success(res, { accounts, total_balance: totalBalance });
  } catch (error) {
    next(error);
  }
};

/**
 * Get account by ID
 */
const getAccount = async (req, res, next) => {
  try {
    const account = await accountService.getAccountById(req.params.id, req.user.id);
    return response.success(res, { account });
  } catch (error) {
    next(error);
  }
};

/**
 * Create account
 */
const createAccount = async (req, res, next) => {
  try {
    const account = await accountService.createAccount(req.user.id, req.body);
    return response.success(res, { account }, 'Account created', 201);
  } catch (error) {
    next(error);
  }
};

/**
 * Update account
 */
const updateAccount = async (req, res, next) => {
  try {
    const account = await accountService.updateAccount(
      req.params.id,
      req.user.id,
      req.body
    );
    return response.success(res, { account }, 'Account updated');
  } catch (error) {
    next(error);
  }
};

/**
 * Delete account
 */
const deleteAccount = async (req, res, next) => {
  try {
    await accountService.deleteAccount(req.params.id, req.user.id);
    return response.success(res, null, 'Account deleted');
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getAccounts,
  getAccount,
  createAccount,
  updateAccount,
  deleteAccount,
};
