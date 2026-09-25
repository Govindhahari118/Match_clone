const express = require('express');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');
const { PrismaClient } = require('@prisma/client');
const jwt = require('jsonwebtoken');
require('dotenv').config({ quiet: true });

const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const postRoutes = require('./routes/post.routes');
const metaRoutes = require('./routes/meta.routes');
const searchRoutes = require('./routes/search.routes');
const subscriptionRoutes = require('./routes/subscription.routes');
const callRoutes = require('./routes/call.routes');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});
app.set('io', io);

const prisma = new PrismaClient();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

// Rate Limiting (Security Hardening)
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: process.env.NODE_ENV === 'development' ? 1000 : 100, // Higher limit for dev
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests, please try again later.' }
});
app.use(limiter);

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/meta', metaRoutes);
app.use('/api/search', searchRoutes);
app.use('/api/subscription', subscriptionRoutes);
app.use('/api/call', callRoutes);
app.use('/api/users', userRoutes);
const profileRoutes = require('./routes/profile.routes');
app.use('/api/profiles', profileRoutes); // For viewing others
const photoRoutes = require('./routes/photo.routes');
app.use('/api/photos', photoRoutes);
const matchesRoutes = require('./routes/matches.routes');
app.use('/api/matches', matchesRoutes);
const interactionRoutes = require('./routes/interaction.routes');
app.use('/api/interactions', interactionRoutes);
const chatRoutes = require('./routes/chat.routes');
app.use('/api/chat', chatRoutes);
const paymentRoutes = require('./routes/payment.routes');
app.use('/api/payment', paymentRoutes);
const adminRoutes = require('./routes/admin.routes');
app.use('/api/admin', adminRoutes);
app.use('/api/posts', postRoutes);
const mediaRoutes = require('./routes/media.routes');
app.use('/api/media', mediaRoutes); // New Media Route for S3 Uploads
const shortlistRoutes = require('./routes/shortlist.routes');
app.use('/api/shortlist', shortlistRoutes);
const reviewRoutes = require('./routes/review.routes');
app.use('/api/reviews', reviewRoutes);
const verificationRoutes = require('./routes/verification.routes');
app.use('/api/verification', verificationRoutes);

// Socket.io for real-time chat. The authenticated token is the only sender identity.
const messageService = require('./services/message.service');

io.use((socket, next) => {
  try {
    const bearer = socket.handshake.auth?.token || socket.handshake.headers?.authorization;
    const token = String(bearer || '').replace(/^Bearer\s+/i, '');
    if (!token) return next(new Error('AUTH_REQUIRED'));
    const claims = jwt.verify(token, process.env.JWT_SECRET);
    socket.user = claims;
    return next();
  } catch (error) {
    return next(new Error('AUTH_INVALID'));
  }
});

io.on('connection', (socket) => {
  const authenticatedUserId = socket.user.sub;
  socket.join(authenticatedUserId);
  console.log('Authenticated user connected', authenticatedUserId, socket.id);

  // Legacy clients may still emit join_room; they can only join their own authenticated room.
  socket.on('join_room', () => {
    socket.join(authenticatedUserId);
  });

  socket.on('send_message', async (data = {}, ack = () => {}) => {
    try {
      const receiverId = data.receiverId;
      const content = data.content;
      const clientMessageId = data.clientMessageId || null;
      if (!receiverId || !content) {
        return ack({ ok: false, code: 'INVALID_MESSAGE', error: 'receiverId and content are required' });
      }

      const message = await messageService.saveMessage(authenticatedUserId, receiverId, content, { clientMessageId });
      const event = {
        id: message.id,
        matchId: message.matchId,
        senderId: authenticatedUserId,
        receiverId,
        content: message.content,
        status: message.status,
        createdAt: message.createdAt,
        clientMessageId: message.clientMessageId
      };

      io.to(receiverId).emit('receive_message', event);
      return ack({ ok: true, message: event });
    } catch (err) {
      console.error('Error saving message', err);
      return ack({ ok: false, code: err.code || 'MESSAGE_SEND_FAILED', error: err.message });
    }
  });

  socket.on('message_delivered', async ({ messageId } = {}, ack = () => {}) => {
    try {
      const message = await messageService.markDelivered(authenticatedUserId, messageId);
      io.to(message.senderId).emit('message_status', { messageId: message.id, status: message.status, deliveredAt: message.deliveredAt });
      return ack({ ok: true });
    } catch (err) {
      return ack({ ok: false, code: err.code || 'DELIVERY_ACK_FAILED', error: err.message });
    }
  });

  socket.on('message_read', async ({ messageId } = {}, ack = () => {}) => {
    try {
      const message = await messageService.markRead(authenticatedUserId, messageId);
      io.to(message.senderId).emit('message_status', { messageId: message.id, status: message.status, readAt: message.readAt });
      return ack({ ok: true });
    } catch (err) {
      return ack({ ok: false, code: err.code || 'READ_ACK_FAILED', error: err.message });
    }
  });

  socket.on('disconnect', () => {
    console.log('User disconnected', authenticatedUserId, socket.id);
  });
});

// Health Check
app.get('/health', async (req, res) => {
  try {
    await prisma.$queryRaw`SELECT 1`;
    res.status(200).json({ status: 'ok', db: 'connected', socket: 'active', timestamp: new Date() });
  } catch (error) {
    console.error('Health check failed', error);
    res.status(500).json({ status: 'error', db: 'disconnected', error: error.message });
  }
});

server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
