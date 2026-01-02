const bcrypt = require('bcryptjs');
const db = require('../config/database');
const jwtUtils = require('../utils/jwt');
const { AuthenticationError, ConflictError, NotFoundError } = require('../utils/errors');
const logger = require('../utils/logger');

/**
 * Authentication Service
 */
class AuthService {
  /**
   * Register new user
   */
  async register(email, password, name) {
    // Check if user exists
    const existingUser = await db('users').where({ email }).first();
    if (existingUser) {
      throw new ConflictError('Email already registered');
    }

    // Hash password
    const passwordHash = await bcrypt.hash(password, 12);

    // Create user
    const [user] = await db('users')
      .insert({
        email,
        password_hash: passwordHash,
        name,
      })
      .returning(['id', 'email', 'name', 'created_at']);

    logger.info(`New user registered: ${email}`);

    // Generate tokens
    const tokens = await this.generateTokens(user.id);

    return {
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
      },
      ...tokens,
    };
  }

  /**
   * Login user
   */
  async login(email, password) {
    // Find user
    const user = await db('users').where({ email }).first();
    if (!user) {
      throw new AuthenticationError('Invalid email or password');
    }

    // Verify password
    const isValid = await bcrypt.compare(password, user.password_hash);
    if (!isValid) {
      throw new AuthenticationError('Invalid email or password');
    }

    // Update last login
    await db('users').where({ id: user.id }).update({ last_login: new Date() });

    logger.info(`User logged in: ${email}`);

    // Generate tokens
    const tokens = await this.generateTokens(user.id);

    return {
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
      },
      ...tokens,
    };
  }

  /**
   * Refresh tokens
   */
  async refreshTokens(refreshToken) {
    // Verify token
    const decoded = jwtUtils.verifyToken(refreshToken);
    if (!decoded || decoded.type !== 'refresh') {
      throw new AuthenticationError('Invalid refresh token');
    }

    // Check if token exists and is valid
    const tokenHash = jwtUtils.hashToken(refreshToken);
    const storedToken = await db('refresh_tokens')
      .where({ token_hash: tokenHash, revoked: false })
      .where('expires_at', '>', new Date())
      .first();

    if (!storedToken) {
      throw new AuthenticationError('Refresh token expired or revoked');
    }

    // Revoke old token
    await db('refresh_tokens')
      .where({ id: storedToken.id })
      .update({ revoked: true });

    // Generate new tokens
    return this.generateTokens(decoded.userId);
  }

  /**
   * Logout (revoke refresh token)
   */
  async logout(refreshToken) {
    const tokenHash = jwtUtils.hashToken(refreshToken);
    await db('refresh_tokens')
      .where({ token_hash: tokenHash })
      .update({ revoked: true });
  }

  /**
   * Logout all sessions
   */
  async logoutAll(userId) {
    await db('refresh_tokens')
      .where({ user_id: userId })
      .update({ revoked: true });
  }

  /**
   * Get user by ID
   */
  async getUserById(userId) {
    const user = await db('users')
      .where({ id: userId })
      .select('id', 'email', 'name', 'created_at', 'last_login')
      .first();

    if (!user) {
      throw new NotFoundError('User');
    }

    return user;
  }

  /**
   * Generate access and refresh tokens
   */
  async generateTokens(userId) {
    const accessToken = jwtUtils.generateAccessToken(userId);
    const refreshToken = jwtUtils.generateRefreshToken(userId);

    // Store refresh token hash
    await db('refresh_tokens').insert({
      user_id: userId,
      token_hash: jwtUtils.hashToken(refreshToken),
      expires_at: jwtUtils.getRefreshTokenExpiry(),
    });

    return { accessToken, refreshToken };
  }

  /**
   * Change password
   */
  async changePassword(userId, currentPassword, newPassword) {
    const user = await db('users').where({ id: userId }).first();
    if (!user) {
      throw new NotFoundError('User');
    }

    // Verify current password
    const isValid = await bcrypt.compare(currentPassword, user.password_hash);
    if (!isValid) {
      throw new AuthenticationError('Current password is incorrect');
    }

    // Hash new password
    const passwordHash = await bcrypt.hash(newPassword, 12);

    // Update password
    await db('users').where({ id: userId }).update({ 
      password_hash: passwordHash,
      updated_at: new Date(),
    });

    // Revoke all refresh tokens
    await this.logoutAll(userId);

    logger.info(`Password changed for user: ${userId}`);
  }
}

module.exports = new AuthService();
