const express = require('express');
const cors = require('cors');
const http = require('http');
const jwt = require('jsonwebtoken');
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
const trustRoutes = require('./routes/trust.routes');
const supportRoutes = require('./routes/support.routes');
const messageService = require('./services/message.service');

const app = express();
const server = http.createServer(app);

const configuredOrigins = String(process.env.ALLOWED_ORIGINS || '')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean);
const corsOrigin = configuredOrigins.length ? configuredOrigins : (process.env.NODE_ENV === 'production' ? false : true);

const io = new Server(server, {
  cors: { origin: corsOrigin, methods: ['GET', 'POST'] },
});
app.set('io', io);

const prisma = new PrismaClient();
const PORT = process.env.PORT || 5000;

app.use(cors({ origin: corsOrigin }));
app.use(express.json({ limit: '1mb' }));

const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: process.env.NODE_ENV === 'development' ? 1000 : 100,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests, please try again later.' },
});
app.use(limiter);

app.use('/api/auth', authRoutes);
app.use('/api/meta', metaRoutes);
app.use('/api/search', searchRoutes);
app.use('/api/subscription', subscriptionRoutes);
app.use('/api/call', callRoutes);
app.use('/api/users', userRoutes);
app.use('/api/trust', trustRoutes);
app.use('/api/support', supportRoutes);

const profileRoutes = require('./routes/profile.routes');
app.use('/api/profiles', profileRoutes);
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
app.use('/api/media', mediaRoutes);
const shortlistRoutes = require('./routes/shortlist.routes');
app.use('/api/shortlist', shortlistRoutes);
const reviewRoutes = require('./routes/review.routes');
app.use('/api/reviews', reviewRoutes);
const verificationRoutes = require('./routes/verification.routes');
app.use('/api/verification', verificationRoutes);

io.use((socket, next) => {
  try {
    const authToken = socket.handshake.auth?.token;
    const headerToken = String(socket.handshake.headers?.authorization || '').replace(/^Bearer\s+/i, '');
    const token = authToken || headerToken;
    if (!token) return next(new Error('Authentication required'));
    socket.user = jwt.verify(token, process.env.JWT_SECRET);
    return next();
  } catch {
    return next(new Error('Invalid authentication token'));
  }
});

io.on('connection', (socket) => {
  const authenticatedUserId = socket.user.sub;
  socket.join(authenticatedUserId);

  // Backward-compatible event: ignore any client-supplied user id.
  socket.on('join_room', () => {
    socket.join(authenticatedUserId);
  });

  socket.on('send_message', async (data = {}, acknowledge) => {
    try {
      const { receiverId, content, clientMessageId } = data;
      const message = await messageService.saveMessage(authenticatedUserId, receiverId, content, clientMessageId);
      io.to(receiverId).emit('receive_message', message);
      socket.emit('message_sent', message);
      if (typeof acknowledge === 'function') acknowledge({ ok: true, message });
    } catch (error) {
      const payload = { ok: false, error: error.statusCode && error.statusCode < 500 ? error.message : 'Unable to send message' };
      if (typeof acknowledge === 'function') acknowledge(payload);
      else socket.emit('message_failed', payload);
    }
  });

  socket.on('message_delivered', async (data = {}, acknowledge) => {
    try {
      const receipt = await messageService.markDelivered(data.messageId, authenticatedUserId);
      io.to(receipt.senderId).emit('message_status', receipt);
      if (typeof acknowledge === 'function') acknowledge({ ok: true, receipt });
    } catch (error) {
      const payload = { ok: false, error: error.statusCode && error.statusCode < 500 ? error.message : 'Unable to acknowledge message' };
      if (typeof acknowledge === 'function') acknowledge(payload);
    }
  });
});

app.get('/health', async (req, res) => {
  try {
    await prisma.$queryRaw`SELECT 1`;
    return res.status(200).json({ status: 'ok', db: 'connected', socket: 'active', timestamp: new Date() });
  } catch (error) {
    console.error('Health check failed', error);
    return res.status(500).json({ status: 'error', db: 'disconnected' });
  }
});

server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
