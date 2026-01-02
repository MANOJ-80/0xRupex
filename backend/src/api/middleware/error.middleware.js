const { AppError } = require('../../utils/errors');
const response = require('../../utils/response');
const logger = require('../../utils/logger');
const config = require('../../config');

/**
 * Global error handler
 */
const errorHandler = (err, req, res, next) => {
  // Log error
  logger.error({
    message: err.message,
    stack: err.stack,
    url: req.url,
    method: req.method,
    body: req.body,
    user: req.user?.id,
  });

  // Operational errors (expected)
  if (err instanceof AppError) {
    return response.error(res, err.message, err.statusCode, err.errors);
  }

  // Knex/Database errors
  if (err.code) {
    switch (err.code) {
      case '23505': // Unique violation
        return response.error(res, 'Resource already exists', 409);
      case '23503': // Foreign key violation
        return response.error(res, 'Referenced resource not found', 400);
      case '22P02': // Invalid UUID
        return response.error(res, 'Invalid ID format', 400);
      default:
        break;
    }
  }

  // JWT errors
  if (err.name === 'JsonWebTokenError') {
    return response.error(res, 'Invalid token', 401);
  }
  if (err.name === 'TokenExpiredError') {
    return response.error(res, 'Token expired', 401);
  }

  // Validation errors from express-validator
  if (err.errors && Array.isArray(err.errors)) {
    return response.error(res, 'Validation failed', 400, err.errors);
  }

  // Unknown errors - don't leak details in production
  const message = config.env === 'production' 
    ? 'Internal server error' 
    : err.message;

  return response.error(res, message, 500);
};

/**
 * 404 handler
 */
const notFoundHandler = (req, res) => {
  return response.error(res, 'Route not found', 404);
};

module.exports = {
  errorHandler,
  notFoundHandler,
};
