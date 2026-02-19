const express = require('express');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');
const { PrismaClient } = require('@prisma/client');
require('dotenv').config();

const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const postRoutes = require('./routes/post.routes');

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

// Socket.io for Real-time
// We need to import message service to save messages
const messageService = require('./services/message.service');

io.on('connection', (socket) => {
  console.log('User connected', socket.id);

  socket.on('join_room', (userId) => {
    // Joining a room based on USER ID is simplest for "dm" style
    // Or we join a room based on matchId
    socket.join(userId); // Join my own room to receive messages
    console.log(`User ${socket.id} joined room ${userId}`);
  });

  socket.on('send_message', async (data) => {
    // data: { senderId, receiverId, content }
    console.log('Message received:', data);

    // Save to DB
    try {
      await messageService.saveMessage(data.senderId, data.receiverId, data.content);

      // Emit to receiver's room
      io.to(data.receiverId).emit('receive_message', data);
      // Also emit back to sender? Or let frontend handle optimist UI
    } catch (err) {
      console.error('Error saving message', err);
    }
  });

  socket.on('disconnect', () => {
    console.log('User disconnected', socket.id);
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
