const app = require('./app');
const config = require('./config');
const logger = require('./utils/logger');
const db = require('./config/database');

const startServer = async () => {
  try {
    // Test database connection
    await db.raw('SELECT 1');
    logger.info('✅ Database connection established');

    // Start server
    const server = app.listen(config.port, () => {
      logger.info(`🚀 Server running on port ${config.port}`);
      logger.info(`📍 Environment: ${config.env}`);
      logger.info(`🔗 API URL: http://localhost:${config.port}/api/v1`);
    });

    // Graceful shutdown
    const shutdown = async (signal) => {
      logger.info(`${signal} received. Shutting down gracefully...`);
      
      server.close(async () => {
        logger.info('HTTP server closed');
        
        try {
          await db.destroy();
          logger.info('Database connection closed');
          process.exit(0);
        } catch (error) {
          logger.error('Error during shutdown:', error);
          process.exit(1);
        }
      });

      // Force shutdown after 30s
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

startServer();
