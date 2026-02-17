const jwtUtils = require('../../utils/jwt');
const { AuthenticationError } = require('../../utils/errors');
const { User } = require('../../models');

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

    const user = await User.findById(decoded.userId).select('id email name');

    if (!user) {
      throw new AuthenticationError('User not found');
    }

    req.user = {
      id: user._id.toString(),
      email: user.email,
      name: user.name,
    };
    next();
  } catch (error) {
    next(error);
  }
};

const optionalAuth = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      const decoded = jwtUtils.verifyToken(token);

      if (decoded && decoded.type === 'access') {
        const user = await User.findById(decoded.userId).select('id email name');

        if (user) {
          req.user = {
            id: user._id.toString(),
            email: user.email,
            name: user.name,
          };
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
