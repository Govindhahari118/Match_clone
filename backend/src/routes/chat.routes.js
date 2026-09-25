const messageService = require('../services/message.service');

// This should be in message.controller.js if doing REST for initial load
// But for now keeping it related to routes or socket
const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth.middleware');

router.use(authMiddleware);

function sendChatError(res, error) {
    const status = error.code === 'MESSAGE_NOT_FOUND' ? 404 :
        ['BLOCKED', 'NOT_CONNECTED', 'INVALID_RECEIVER', 'EMPTY_MESSAGE'].includes(error.code) ? 403 : 500;
    return res.status(status).json({ error: error.message, code: error.code || 'CHAT_ERROR' });
}


// Get conversations list (Matched users)
router.get('/conversations', async (req, res) => {
    try {
        const userId = req.user.sub;
        const conversations = await messageService.getConnectedUsers(userId);
        res.json(conversations);
    } catch (error) {
        return sendChatError(res, error);
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
        return sendChatError(res, error);
    }
});

// Send message (REST fallback)
router.post('/send', async (req, res) => {
    try {
        const myId = req.user.sub;
        const { receiverId, content, clientMessageId } = req.body;
        const message = await messageService.saveMessage(myId, receiverId, content, { clientMessageId });
        // Ideally emit socket event here too via io instance
        res.json(message);
    } catch (error) {
        return sendChatError(res, error);
    }
});

module.exports = router;
