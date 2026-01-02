const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const config = require('../config');

/**
 * Generate access token (short-lived)
 */
const generateAccessToken = (userId) => {
  return jwt.sign(
    { userId, type: 'access' },
    config.jwt.secret,
    { expiresIn: config.jwt.accessExpiry }
  );
};

/**
 * Generate refresh token (long-lived)
 */
const generateRefreshToken = (userId) => {
  return jwt.sign(
    { userId, type: 'refresh', jti: crypto.randomUUID() },
    config.jwt.secret,
    { expiresIn: config.jwt.refreshExpiry }
  );
};

/**
 * Verify token
 */
const verifyToken = (token) => {
  try {
    return jwt.verify(token, config.jwt.secret);
  } catch (error) {
    return null;
  }
};

/**
 * Hash token for storage
 */
const hashToken = (token) => {
  return crypto.createHash('sha256').update(token).digest('hex');
};

/**
 * Parse expiry string to milliseconds
 */
const parseExpiry = (expiry) => {
  const match = expiry.match(/^(\d+)([smhd])$/);
  if (!match) return 0;
  
  const value = parseInt(match[1]);
  const unit = match[2];
  
  const multipliers = {
    s: 1000,
    m: 60 * 1000,
    h: 60 * 60 * 1000,
    d: 24 * 60 * 60 * 1000,
  };
  
  return value * multipliers[unit];
};

/**
 * Get refresh token expiry date
 */
const getRefreshTokenExpiry = () => {
  return new Date(Date.now() + parseExpiry(config.jwt.refreshExpiry));
};

module.exports = {
  generateAccessToken,
  generateRefreshToken,
  verifyToken,
  hashToken,
  getRefreshTokenExpiry,
};
