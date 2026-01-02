const jwtUtils = require('../../utils/jwt');
const { AuthenticationError } = require('../../utils/errors');
const db = require('../../config/database');

/**
 * Authenticate JWT token
 */
const authenticate = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new AuthenticationError('No token provided');
    }

    const token = authHeader.substring(7);
    const decoded = jwtUtils.verifyToken(token);

    if (!decoded || decoded.type !== 'access') {
      throw new AuthenticationError('Invalid token');
    }

    // Check if user exists
    const user = await db('users')
      .where({ id: decoded.userId })
      .select('id', 'email', 'name')
      .first();

    if (!user) {
      throw new AuthenticationError('User not found');
    }

    req.user = user;
    next();
  } catch (error) {
    next(error);
  }
};

/**
 * Optional authentication - doesn't fail if no token
 */
const optionalAuth = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      const decoded = jwtUtils.verifyToken(token);

      if (decoded && decoded.type === 'access') {
        const user = await db('users')
          .where({ id: decoded.userId })
          .select('id', 'email', 'name')
          .first();

        if (user) {
          req.user = user;
        }
      }
    }
    next();
  } catch (error) {
    next();
  }
};

module.exports = {
  authenticate,
  optionalAuth,
};
