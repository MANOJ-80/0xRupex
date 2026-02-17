const success = (res, data, message = null, statusCode = 200) => {
  const response = {
    success: true,
  };

  if (message) response.message = message;

  if (data !== null && data !== undefined) {
    if (typeof data === 'object' && !Array.isArray(data)) {
      Object.assign(response, data);
    } else {
      response.data = data;
    }
  }

  return res.status(statusCode).json(response);
};

const error = (res, message, statusCode = 400, errors = null) => {
  const response = {
    success: false,
    error: message,
  };

  if (errors) response.errors = errors;

  return res.status(statusCode).json(response);
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
