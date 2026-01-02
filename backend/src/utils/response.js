/**
 * Standard API response format
 */

const success = (res, data, message = null, statusCode = 200) => {
  return res.status(statusCode).json({
    success: true,
    message,
    data,
  });
};

const error = (res, message, statusCode = 400, errors = null) => {
  return res.status(statusCode).json({
    success: false,
    error: message,
    errors,
  });
};

const paginated = (res, data, pagination) => {
  return res.status(200).json({
    success: true,
    data,
    pagination,
  });
};

module.exports = {
  success,
  error,
  paginated,
};
