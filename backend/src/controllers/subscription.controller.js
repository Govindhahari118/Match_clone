const subscriptionService = require('../services/subscription.service');

const subscriptionController = {
    async getEntitlements(req, res) {
        try {
            const result = await subscriptionService.getEntitlements(req.user.sub);
            res.status(200).json(result);
        } catch (error) {
            console.error('Get entitlements error:', error);
            res.status(500).json({ error: 'Failed to fetch entitlements' });
        }
    },
};

module.exports = subscriptionController;
