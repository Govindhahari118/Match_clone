const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const http = require('http');
const rateLimit = require('express-rate-limit');
const { Server } = require('socket.io');
const { PrismaClient } = require('@prisma/client');
require('dotenv').config({ quiet: true });

const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const postRoutes = require('./routes/post.routes');
const metaRoutes = require('./routes/meta.routes');
const searchRoutes = require('./routes/search.routes');
const subscriptionRoutes = require('./routes/subscription.routes');
const callRoutes = require('./routes/call.routes');
const profileRoutes = require('./routes/profile.routes');
const photoRoutes = require('./routes/photo.routes');
const matchesRoutes = require('./routes/matches.routes');
const interactionRoutes = require('./routes/interaction.routes');
const chatRoutes = require('./routes/chat.routes');
const paymentRoutes = require('./routes/payment.routes');
const adminRoutes = require('./routes/admin.routes');
const mediaRoutes = require('./routes/media.routes');
const shortlistRoutes = require('./routes/shortlist.routes');
const reviewRoutes = require('./routes/review.routes');
const verificationRoutes = require('./routes/verification.routes');
const analyticsRoutes = require('./routes/analytics.routes');
const messageService = require('./services/message.service');
const { requestContextMiddleware, requestLoggingMiddleware } = require('./middleware/request-context.middleware');

const app = express();
const server = http.createServer(app);
const prisma = new PrismaClient();

const PORT = Number(process.env.PORT || 4000);
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
const connectedUsers = new Map();

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

app.use('/api/auth', authRoutes);
app.use('/api/meta', metaRoutes);
app.use('/api/search', searchLimiter, searchRoutes);
app.use('/api/subscription', subscriptionRoutes);
app.use('/api/call', callLimiter, callRoutes);
app.use('/api/users', userRoutes);
app.use('/api/profiles', profileLimiter, profileRoutes);
app.use('/api/photos', photoRoutes);
app.use('/api/matches', matchesRoutes);
app.use('/api/interactions', interactionRoutes);
app.use('/api/chat', chatLimiter, chatRoutes);
app.use('/api/payment', paymentRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/posts', postRoutes);
app.use('/api/media', mediaRoutes);
app.use('/api/shortlist', shortlistRoutes);
app.use('/api/reviews', reviewRoutes);
app.use('/api/verification', verificationRoutes);
app.use('/api/analytics', analyticsRoutes);

io.on('connection', (socket) => {
  console.log('User connected', socket.id);

  socket.on('join_room', (userId) => {
    if (!userId) {
      return;
    }

    const normalizedUserId = String(userId);
    socket.data.userId = normalizedUserId;
    socket.join(normalizedUserId);
    const activeSockets = connectedUsers.get(normalizedUserId) || new Set();
    activeSockets.add(socket.id);
    connectedUsers.set(normalizedUserId, activeSockets);
    io.to(normalizedUserId).emit('presence_update', { userId: normalizedUserId, isOnline: true });
    console.log(`User ${socket.id} joined room ${normalizedUserId}`);
  });

  socket.on('send_message', async (data = {}) => {
    const { senderId, receiverId, content, clientMessageId } = data;
    if (!senderId || !receiverId || !content) {
      return;
    }

    try {
      const saved = await messageService.saveMessage(senderId, receiverId, content, { clientMessageId });
      const payload = {
        ...saved,
        senderId,
        receiverId,
      };

      io.to(String(receiverId)).emit('receive_message', payload);
      io.to(String(senderId)).emit('message_ack', {
        clientMessageId: clientMessageId || null,
        messageId: saved.id,
        status: saved.status || 'sent',
      });

      const receiverSockets = connectedUsers.get(String(receiverId));
      if (receiverSockets && receiverSockets.size > 0) {
        await messageService.updateMessageStatus(receiverId, saved.id, 'delivered', {
          source: 'socket_realtime_delivery',
        });
        io.to(String(senderId)).emit('message_status', {
          messageId: saved.id,
          status: 'delivered',
          receiverId,
        });
      }
    } catch (error) {
      console.error('Error saving message', error);
      io.to(String(senderId)).emit('message_ack', {
        clientMessageId: clientMessageId || null,
        status: 'failed',
        error: error.message,
      });
    }
  });

  socket.on('typing', (data = {}) => {
    const { senderId, receiverId, isTyping = true } = data;
    if (!senderId || !receiverId) return;
    io.to(String(receiverId)).emit('typing', {
      senderId,
      receiverId,
      isTyping: Boolean(isTyping),
      ts: new Date().toISOString(),
    });
  });

  socket.on('conversation_seen', async (data = {}) => {
    const { viewerId, peerId } = data;
    if (!viewerId || !peerId) return;
    try {
      const seenUpdate = await messageService.markConversationSeen(String(viewerId), String(peerId));
      if (seenUpdate.updated > 0) {
        io.to(String(peerId)).emit('conversation_seen', {
          viewerId: String(viewerId),
          updated: seenUpdate.updated,
        });
      }
    } catch (error) {
      console.error('conversation_seen handler error', error);
    }
  });

  socket.on('disconnect', () => {
    const userId = socket.data.userId;
    if (userId) {
      const activeSockets = connectedUsers.get(userId);
      if (activeSockets) {
        activeSockets.delete(socket.id);
        if (activeSockets.size === 0) {
          connectedUsers.delete(userId);
          io.emit('presence_update', { userId, isOnline: false });
        } else {
          connectedUsers.set(userId, activeSockets);
        }
      }
    }
    console.log('User disconnected', socket.id);
  });
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
    res.status(500).json({
      status: 'error',
      db: 'disconnected',
      error: error.message,
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
