const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const morgan = require('morgan');
const mongoose = require('mongoose');

const config = require('./config');
const routes = require('./api/routes');
const { errorHandler, notFoundHandler } = require('./api/middleware/error.middleware');
const { connectDB } = require('./config/database');
const logger = require('./utils/logger');

const createApp = () => {
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

const app = createApp();

module.exports = { app, connectDB };
