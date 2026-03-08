const express = require('express');
const messageService = require('../services/message.service');
const authMiddleware = require('../middleware/auth.middleware');
const { parsePagination } = require('../utils/pagination');

const router = express.Router();
router.use(authMiddleware);

router.get('/conversations', async (req, res) => {
    try {
        const userId = req.user.sub;
        const { limit, skip } = parsePagination(req.query, { defaultLimit: 20, maxLimit: 50 });
        const conversations = await messageService.getConnectedUsers(userId, { limit, skip });
        return res.json(conversations);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
});

router.get('/:userId', async (req, res) => {
    try {
        const myId = req.user.sub;
        const otherId = req.params.userId;
        const { limit, skip } = parsePagination(req.query, { defaultLimit: 50, maxLimit: 200 });
        const messages = await messageService.getMessages(myId, otherId, { limit, skip });
        await messageService.markConversationDelivered(myId, otherId);
        return res.json(messages);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
});

router.post('/send', async (req, res) => {
    try {
        const myId = req.user.sub;
        const { receiverId, content, clientMessageId } = req.body || {};
        const message = await messageService.saveMessage(myId, receiverId, content, { clientMessageId });
        return res.json(message);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
});

router.post('/status', async (req, res) => {
    try {
        const myId = req.user.sub;
        const { messageId, status } = req.body || {};
        const result = await messageService.updateMessageStatus(myId, messageId, status, {
            source: 'rest_status_endpoint',
        });
        if (result.error) {
            return res.status(result.statusCode || 400).json({ error: result.error });
        }
        return res.json(result);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
});

router.post('/:userId/read', async (req, res) => {
    try {
        const myId = req.user.sub;
        const otherId = req.params.userId;
        const result = await messageService.markConversationSeen(myId, otherId);
        return res.json(result);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
});

module.exports = router;
