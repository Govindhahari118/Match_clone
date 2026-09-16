const trustService = require('../services/trust.service');
const exposureService = require('../services/exposure.service');

function sendError(res, error) {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? 'Request failed' : error.message });
}

const trustController = {
    async getMine(req, res) {
        try {
            const summary = await trustService.getTrustSummary(req.user.sub);
            if (!summary) return res.status(404).json({ error: 'User not found' });
            return res.json(summary);
        } catch (error) {
            console.error('Get trust summary error:', error);
            return sendError(res, error);
        }
    },

    async getForUser(req, res) {
        try {
            const summary = await trustService.getTrustSummary(req.params.userId);
            if (!summary || !summary.discoverable) return res.status(404).json({ error: 'Profile not found' });
            return res.json(summary);
        } catch (error) {
            console.error('Get public trust summary error:', error);
            return sendError(res, error);
        }
    },

    async recordActivity(req, res) {
        try {
            const at = await trustService.recordActivity(req.user.sub);
            return res.json({ success: true, lastActiveAt: at });
        } catch (error) {
            console.error('Record activity error:', error);
            return sendError(res, error);
        }
    },

    async reconfirm(req, res) {
        try {
            const result = await trustService.reconfirmSearchStatus(req.user.sub, req.body?.status);
            return res.json({ success: true, ...result });
        } catch (error) {
            console.error('Reconfirm search status error:', error);
            return sendError(res, error);
        }
    },

    async hideProfile(req, res) {
        try {
            await exposureService.setInteractionState(req.user.sub, req.params.userId, 'hidden');
            return res.json({ success: true });
        } catch (error) {
            return sendError(res, error);
        }
    },

    async skipProfile(req, res) {
        try {
            await exposureService.setInteractionState(req.user.sub, req.params.userId, 'skipped');
            return res.json({ success: true });
        } catch (error) {
            return sendError(res, error);
        }
    },

    async restoreProfile(req, res) {
        try {
            await exposureService.restoreProfile(req.user.sub, req.params.userId);
            return res.json({ success: true });
        } catch (error) {
            return sendError(res, error);
        }
    },
};

module.exports = trustController;
