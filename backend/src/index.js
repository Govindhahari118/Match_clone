const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const http = require('http');
const rateLimit = require('express-rate-limit');
const { Server } = require('socket.io');
const { PrismaClient } = require('@prisma/client');
require('dotenv').config({ quiet: true });

const { buildRouteRegistry } = require('./routes/routes.registry');
const { requestContextMiddleware, requestLoggingMiddleware } = require('./middleware/request-context.middleware');
const { initRealtime, setupRedisAdapter } = require('./realtime/socket.server');

const app = express();
const server = http.createServer(app);
const prisma = new PrismaClient();

const PORT = Number(process.env.PORT || 5000);
const BODY_LIMIT = process.env.BODY_LIMIT || '1mb';
const ORIGIN_FALLBACK = process.env.FRONTEND_URL || 'http://localhost:8000';
const ALLOWED_ORIGINS = (process.env.CORS_ORIGINS || ORIGIN_FALLBACK)
  .split(',')
  .map((origin) => origin.trim())
  .filter(Boolean);

function isAllowedOrigin(origin) {
  if (!origin) {
    return true;
  }
  return ALLOWED_ORIGINS.includes(origin);
}

const corsOptions = {
  origin(origin, callback) {
    if (isAllowedOrigin(origin)) {
      callback(null, true);
      return;
    }
    callback(new Error('Not allowed by CORS'));
  },
  credentials: true,
};

const io = new Server(server, {
  cors: {
    origin: ALLOWED_ORIGINS,
    methods: ['GET', 'POST'],
    credentials: true,
  },
});
setupRedisAdapter(io).catch((error) => {
  console.error('Redis adapter setup failed:', error);
});
initRealtime(io);

app.set('io', io);
app.disable('x-powered-by');
app.use(
  helmet({
    crossOriginResourcePolicy: false,
  })
);
app.use(cors(corsOptions));
app.use(express.json({ limit: BODY_LIMIT }));
app.use(requestContextMiddleware);
app.use(requestLoggingMiddleware);

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: process.env.NODE_ENV === 'development' ? 1000 : 100,
  standardHeaders: true,
  legacyHeaders: false,
  skip: (req) => req.path === '/health',
  message: { error: 'Too many requests, please try again later.' },
});
app.use(limiter);

const searchLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: process.env.NODE_ENV === 'development' ? 240 : 80,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Search rate limit exceeded' },
});

const profileLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: process.env.NODE_ENV === 'development' ? 180 : 60,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Profile request rate limit exceeded' },
});

const chatLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: process.env.NODE_ENV === 'development' ? 300 : 100,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Chat rate limit exceeded' },
});

const callLimiter = rateLimit({
  windowMs: 5 * 60 * 1000,
  max: process.env.NODE_ENV === 'development' ? 30 : 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Call setup rate limit exceeded' },
});

const routeRegistry = buildRouteRegistry({
  searchLimiter,
  profileLimiter,
  chatLimiter,
  callLimiter,
});

routeRegistry.forEach(({ base, router, middleware = [] }) => {
  if (Array.isArray(middleware) && middleware.length > 0) {
    app.use(base, ...middleware, router);
  } else {
    app.use(base, router);
  }
});

app.get('/health', async (_req, res) => {
  try {
    await prisma.$queryRaw`SELECT 1`;
    res.status(200).json({
      status: 'ok',
      db: 'connected',
      socket: 'active',
      uptimeSeconds: Math.round(process.uptime()),
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    console.error('Health check failed', error);
    res.status(200).json({
      status: 'degraded',
      db: 'disconnected',
      error: error.message,
      socket: 'active',
      uptimeSeconds: Math.round(process.uptime()),
      timestamp: new Date().toISOString(),
    });
  }
});

app.use((_req, res) => {
  res.status(404).json({ error: 'Route not found' });
});

app.use((error, _req, res, _next) => {
  if (error.message === 'Not allowed by CORS') {
    return res.status(403).json({ error: 'CORS origin blocked' });
  }

  console.error('Unhandled application error:', error);
  const statusCode = Number(error.status || error.statusCode || 500);
  const message =
    process.env.NODE_ENV === 'production' && statusCode === 500
      ? 'Internal server error'
      : error.message || 'Internal server error';

  return res.status(statusCode).json({ error: message });
});

const httpServer = server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});

let isShuttingDown = false;
async function shutdown(signal) {
  if (isShuttingDown) {
    return;
  }
  isShuttingDown = true;

  console.log(`${signal} received, shutting down server...`);
  const forceExitTimer = setTimeout(() => {
    console.error('Forced shutdown triggered after timeout.');
    process.exit(1);
  }, 10000);
  forceExitTimer.unref();

  try {
    io.close();
    await new Promise((resolve) => {
      httpServer.close(() => resolve());
    });
    await prisma.$disconnect();
    clearTimeout(forceExitTimer);
    process.exit(0);
  } catch (error) {
    clearTimeout(forceExitTimer);
    console.error('Graceful shutdown failed:', error);
    process.exit(1);
  }
}

process.on('SIGINT', () => {
  void shutdown('SIGINT');
});

process.on('SIGTERM', () => {
  void shutdown('SIGTERM');
});
