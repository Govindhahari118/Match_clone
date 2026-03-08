const analyticsService = require('../services/analytics.service');

const analyticsController = {
    getTaxonomy(req, res) {
        try {
            return res.status(200).json({
                taxonomy: analyticsService.getTaxonomy(),
                schemaVersion: 'events-v1',
            });
        } catch (error) {
            return res.status(500).json({ error: error.message });
        }
    },

    getFlags(req, res) {
        try {
            const userId = req.user.sub;
            const flags = analyticsService.getFlagsForUser(userId);
            return res.status(200).json({ flags });
        } catch (error) {
            return res.status(500).json({ error: error.message });
        }
    },

    async trackEvent(req, res) {
        try {
            const userId = req.user.sub;
            const result = await analyticsService.trackEvent(userId, req.body || {}, {
                ipAddress: req.ip,
                userAgent: req.get('user-agent'),
            });
            if (result.error) {
                return res.status(result.statusCode || 400).json({ error: result.error });
            }
            return res.status(201).json(result);
        } catch (error) {
            return res.status(500).json({ error: error.message });
        }
    },

    async logDecision(req, res) {
        try {
            const actorId = req.user.sub;
            const result = await analyticsService.logDecision(actorId, req.body || {});
            if (result.error) {
                return res.status(result.statusCode || 400).json({ error: result.error });
            }
            return res.status(201).json(result);
        } catch (error) {
            return res.status(500).json({ error: error.message });
        }
    },

    async logFlagExposure(req, res) {
        try {
            const actorId = req.user.sub;
            const result = await analyticsService.logFlagExposure(actorId, req.body || {});
            if (result.error) {
                return res.status(result.statusCode || 400).json({ error: result.error });
            }
            return res.status(201).json(result);
        } catch (error) {
            return res.status(500).json({ error: error.message });
        }
    },
};

module.exports = analyticsController;
