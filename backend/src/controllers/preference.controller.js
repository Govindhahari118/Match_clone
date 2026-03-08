const preferenceService = require('../services/preference.service');
const onboardingService = require('../services/onboarding.service');

const savePreferences = async (req, res) => {
    try {
        const userId = req.user.sub;
        const {
            min_age, max_age, marital_status, religion, preferred_locations, min_income_band
        } = req.body;

        const result = await preferenceService.upsertPreferences(userId, {
            min_age, max_age, marital_status, religion, preferred_locations, min_income_band
        });
        const completeness = await onboardingService.refreshCompleteness(userId);

        res.status(200).json({ ...result, onboardingCompleteness: completeness });
    } catch (error) {
        console.error('Save preferences error:', error);
        res.status(500).json({ error: error.message });
    }
};

module.exports = { savePreferences };
