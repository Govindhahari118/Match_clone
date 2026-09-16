const preferenceService = require('../services/preference.service');

const savePreferences = async (req, res) => {
    try {
        const result = await preferenceService.upsertPreferences(req.user.sub, req.body || {});
        return res.status(200).json(result);
    } catch (error) {
        console.error('Save preferences error:', error);
        const status = Number(error.statusCode) || 500;
        return res.status(status).json({ error: status >= 500 ? 'Failed to save preferences' : error.message });
    }
};

const getPreferences = async (req, res) => {
    try {
        return res.json(await preferenceService.getPreferences(req.user.sub));
    } catch (error) {
        return res.status(500).json({ error: 'Failed to load preferences' });
    }
};

module.exports = { savePreferences, getPreferences };
