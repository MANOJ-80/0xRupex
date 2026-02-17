const { AppError } = require('../../utils/errors');
const response = require('../../utils/response');
const logger = require('../../utils/logger');
const config = require('../../config');
const mongoose = require('mongoose');

const SENSITIVE_FIELDS = ['password', 'passwordHash', 'currentPassword', 'newPassword', 'refreshToken'];

const sanitizeBody = (body) => {
  if (!body || typeof body !== 'object') return body;

  const sanitized = { ...body };
  for (const field of SENSITIVE_FIELDS) {
    if (sanitized[field]) {
      sanitized[field] = '[REDACTED]';
    }
  }
  return sanitized;
};

const errorHandler = (err, req, res, next) => {
  logger.error({
    message: err.message,
    stack: err.stack,
    url: req.url,
    method: req.method,
    body: sanitizeBody(req.body),
    user: req.user?.id,
  });

  if (err instanceof AppError) {
    return response.error(res, err.message, err.statusCode, err.errors);
  }

  if (err instanceof mongoose.Error.ValidationError) {
    const errors = Object.values(err.errors).map((e) => ({
      field: e.path,
      message: e.message,
    }));
    return response.error(res, 'Validation failed', 400, errors);
  }

  if (err instanceof mongoose.Error.CastError) {
    return response.error(res, `Invalid ${err.path}: ${err.value}`, 400);
  }

  if (err.code === 11000) {
    const field = Object.keys(err.keyPattern || {})[0] || 'field';
    return response.error(res, `${field} already exists`, 409);
  }

  if (err.name === 'JsonWebTokenError') {
    return response.error(res, 'Invalid token', 401);
  }
  if (err.name === 'TokenExpiredError') {
    return response.error(res, 'Token expired', 401);
  }

  if (err.errors && Array.isArray(err.errors)) {
    return response.error(res, 'Validation failed', 400, err.errors);
  }

  const message = config.env === 'production' ? 'Internal server error' : err.message;

  return response.error(res, message, 500);
};

const notFoundHandler = (req, res) => {
  return response.error(res, 'Route not found', 404);
};

module.exports = {
  errorHandler,
  notFoundHandler,
};
