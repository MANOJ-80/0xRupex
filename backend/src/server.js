const config = require('./config');
const { connectDB, disconnectDB } = require('./config/database');
const logger = require('./utils/logger');
const mongoose = require('mongoose');

const createApp = async () => {
  const express = require('express');
  const cors = require('cors');
  const helmet = require('helmet');
  const compression = require('compression');
  const morgan = require('morgan');

  const routes = require('./api/routes');
  const { errorHandler, notFoundHandler } = require('./api/middleware/error.middleware');

  const app = express();

  app.use(helmet());

  app.use(
    cors({
      origin: config.corsOrigins,
      credentials: true,
      methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
      allowedHeaders: ['Content-Type', 'Authorization'],
    })
  );

  app.use(compression());

  if (config.env !== 'test') {
    app.use(
      morgan('combined', {
        stream: { write: (message) => logger.info(message.trim()) },
      })
    );
  }

  app.use(express.json({ limit: '10mb' }));
  app.use(express.urlencoded({ extended: true, limit: '10mb' }));

  app.set('trust proxy', 1);

  app.use('/api/v1', routes);

  app.get('/', (req, res) => {
    res.json({
      name: '0xRupex API',
      version: '2.0.0',
      description: 'Personal Finance Manager API - MongoDB Edition',
      docs: '/api/v1/health',
    });
  });

  app.get('/health', (req, res) => {
    const dbStatus = mongoose.connection.readyState === 1 ? 'connected' : 'disconnected';
    res.json({
      status: 'ok',
      timestamp: new Date().toISOString(),
      database: dbStatus,
      version: '2.0.0',
    });
  });

  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
};

const bootstrap = async () => {
  try {
    await connectDB();

    const app = await createApp();

    const server = app.listen(config.port, () => {
      logger.info(`Server running on port ${config.port}`);
      logger.info(`Environment: ${config.env}`);
      logger.info(`API URL: http://localhost:${config.port}/api/v1`);
    });

    const shutdown = async (signal) => {
      logger.info(`${signal} received. Shutting down gracefully...`);

      server.close(async () => {
        logger.info('HTTP server closed');
        try {
          await disconnectDB();
          logger.info('Database connection closed');
          process.exit(0);
        } catch (error) {
          logger.error('Error during shutdown:', error);
          process.exit(1);
        }
      });

      setTimeout(() => {
        logger.error('Forced shutdown after timeout');
        process.exit(1);
      }, 30000);
    };

    process.on('SIGTERM', () => shutdown('SIGTERM'));
    process.on('SIGINT', () => shutdown('SIGINT'));
  } catch (error) {
    logger.error('Failed to start server:', error);
    process.exit(1);
  }
};

bootstrap();

process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

process.on('uncaughtException', (error) => {
  logger.error('Uncaught Exception:', error);
  process.exit(1);
});
