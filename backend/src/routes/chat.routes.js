const messageService = require('../services/message.service');
const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth.middleware');

router.use(authMiddleware);

function sendError(res, error) {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? 'Chat request failed' : error.message });
}

router.get('/conversations', async (req, res) => {
    try {
        return res.json(await messageService.getConnectedUsers(req.user.sub));
    } catch (error) {
        return sendError(res, error);
    }
});

router.post('/send', async (req, res) => {
    try {
        const { receiverId, content, clientMessageId } = req.body || {};
        const message = await messageService.saveMessage(req.user.sub, receiverId, content, clientMessageId);
        const io = req.app.get('io');
        io.to(receiverId).emit('receive_message', message);
        await messageService.markDelivered(message.id);
        return res.status(201).json({ ...message, status: 'delivered' });
    } catch (error) {
        return sendError(res, error);
    }
});

router.post('/:userId/read', async (req, res) => {
    try {
        return res.json(await messageService.markRead(req.user.sub, req.params.userId));
    } catch (error) {
        return sendError(res, error);
    }
});

router.get('/:userId', async (req, res) => {
    try {
        const messages = await messageService.getMessages(req.user.sub, req.params.userId);
        return res.json(messages);
    } catch (error) {
        return sendError(res, error);
    }
});

module.exports = router;
