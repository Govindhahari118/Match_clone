const notificationService = require('../services/notification.service');

function sendError(res, error, fallback) {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? fallback : error.message });
}

exports.list = async (req, res) => {
    try {
        const items = await notificationService.listNotifications(req.user.sub, { limit: req.query.limit });
        return res.json(items);
    } catch (error) {
        return sendError(res, error, 'Unable to load notifications');
    }
};

exports.markRead = async (req, res) => {
    try {
        return res.json(await notificationService.markRead(req.user.sub, req.params.notificationId));
    } catch (error) {
        return sendError(res, error, 'Unable to update notification');
    }
};

exports.markAllRead = async (req, res) => {
    try {
        return res.json(await notificationService.markAllRead(req.user.sub));
    } catch (error) {
        return sendError(res, error, 'Unable to update notifications');
    }
};
