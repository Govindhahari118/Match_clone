const messageService = require('../services/message.service');

// This should be in message.controller.js if doing REST for initial load
// But for now keeping it related to routes or socket
const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth.middleware');

router.use(authMiddleware);

// Get conversations list (Matched users)
router.get('/conversations', async (req, res) => {
    try {
        const userId = req.user.sub;
        const conversations = await messageService.getConnectedUsers(userId);
        res.json(conversations);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Get messages for a specific user
router.get('/:userId', async (req, res) => {
    try {
        const myId = req.user.sub;
        const otherId = req.params.userId;
        const messages = await messageService.getMessages(myId, otherId);
        res.json(messages);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Send message (REST fallback)
router.post('/send', async (req, res) => {
    try {
        const myId = req.user.sub;
        const { receiverId, content } = req.body;
        const message = await messageService.saveMessage(myId, receiverId, content);
        // Ideally emit socket event here too via io instance
        res.json(message);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
