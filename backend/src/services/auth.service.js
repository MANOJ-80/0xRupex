const bcrypt = require('bcryptjs');
const { User, RefreshToken } = require('../models');
const jwtUtils = require('../utils/jwt');
const { AuthenticationError, ConflictError, NotFoundError } = require('../utils/errors');
const logger = require('../utils/logger');

class AuthService {
  async register(email, password, name) {
    const existingUser = await User.findOne({ email });
    if (existingUser) {
      throw new ConflictError('Email already registered');
    }

    const passwordHash = await bcrypt.hash(password, 12);

    const user = await User.create({
      email,
      passwordHash,
      name,
    });

    logger.info(`New user registered: ${email}`);

    const tokens = await this.generateTokens(user._id);

    return {
      user: user.toSafeObject(),
      ...tokens,
    };
  }

  async login(email, password) {
    const user = await User.findOne({ email, isActive: true });
    if (!user) {
      throw new AuthenticationError('Invalid email or password');
    }

    const isValid = await bcrypt.compare(password, user.passwordHash);
    if (!isValid) {
      throw new AuthenticationError('Invalid email or password');
    }

    user.lastLogin = new Date();
    await user.save();

    logger.info(`User logged in: ${email}`);

    const tokens = await this.generateTokens(user._id);

    return {
      user: user.toSafeObject(),
      ...tokens,
    };
  }

  async refreshTokens(refreshToken) {
    const decoded = jwtUtils.verifyToken(refreshToken);
    if (!decoded || decoded.type !== 'refresh') {
      throw new AuthenticationError('Invalid refresh token');
    }

    const tokenHash = jwtUtils.hashToken(refreshToken);
    const storedToken = await RefreshToken.findOne({
      tokenHash,
      revoked: false,
      expiresAt: { $gt: new Date() },
    });

    if (!storedToken) {
      throw new AuthenticationError('Refresh token expired or revoked');
    }

    storedToken.revoked = true;
    await storedToken.save();

    return this.generateTokens(decoded.userId);
  }

  async logout(refreshToken) {
    const tokenHash = jwtUtils.hashToken(refreshToken);
    await RefreshToken.updateOne(
      { tokenHash },
      { revoked: true }
    );
  }

  async logoutAll(userId) {
    await RefreshToken.updateMany(
      { user: userId },
      { revoked: true }
    );
  }

  async getUserById(userId) {
    const user = await User.findById(userId);
    if (!user) {
      throw new NotFoundError('User');
    }
    return user.toSafeObject();
  }

  async generateTokens(userId) {
    const accessToken = jwtUtils.generateAccessToken(userId);
    const refreshToken = jwtUtils.generateRefreshToken(userId);

    await RefreshToken.create({
      user: userId,
      tokenHash: jwtUtils.hashToken(refreshToken),
      expiresAt: jwtUtils.getRefreshTokenExpiry(),
    });

    return { accessToken, refreshToken };
  }

  async changePassword(userId, currentPassword, newPassword) {
    const user = await User.findById(userId);
    if (!user) {
      throw new NotFoundError('User');
    }

    const isValid = await bcrypt.compare(currentPassword, user.passwordHash);
    if (!isValid) {
      throw new AuthenticationError('Current password is incorrect');
    }

    user.passwordHash = await bcrypt.hash(newPassword, 12);
    await user.save();

    await this.logoutAll(userId);

    logger.info(`Password changed for user: ${userId}`);
  }
}

module.exports = new AuthService();
