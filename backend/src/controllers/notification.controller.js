const notificationService = require('../services/notification.service');

const notificationController = {
    async list(req, res) {
        try {
            const userId = req.user.sub;
            const result = await notificationService.listNotifications(userId, req.query || {});
            res.status(200).json(result);
        } catch (error) {
            console.error('List notifications error:', error);
            res.status(500).json({ error: 'Failed to fetch notifications' });
        }
    },

    async markRead(req, res) {
        try {
            const userId = req.user.sub;
            const result = await notificationService.markAllRead(userId);
            res.status(200).json({ success: true, ...result });
        } catch (error) {
            console.error('Mark notifications read error:', error);
            res.status(500).json({ error: 'Failed to update notifications' });
        }
    },
};

module.exports = notificationController;
