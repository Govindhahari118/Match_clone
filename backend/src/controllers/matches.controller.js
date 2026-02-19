const matchingService = require('../services/matching.service');

const getMatches = async (req, res) => {
    try {
        const userId = req.user.sub;
        const filters = req.query; // Capture query params
        const matches = await matchingService.getMatches(userId, filters);
        res.status(200).json(matches);
    } catch (error) {
        console.error('Get matches error:', error);
        res.status(500).json({ error: error.message });
    }
};

module.exports = { getMatches };
