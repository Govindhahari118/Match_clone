const callService = require('../services/call.service');

const callController = {
    async createSession(req, res) {
        try {
            const callerId = req.user.sub;
            const result = await callService.createSession(callerId, req.body || {});

            if (result?.error) {
                return res.status(result.statusCode || 400).json(result);
            }

            return res.status(200).json({
                success: true,
                session: result,
            });
        } catch (error) {
            console.error('Create call session error:', error);
            return res.status(500).json({ error: 'Failed to create call session' });
        }
    },
};

module.exports = callController;
