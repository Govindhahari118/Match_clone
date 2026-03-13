const preferenceService = require('../services/preference.service');
const onboardingService = require('../services/onboarding.service');
const { validatePreferenceUpdate } = require('../validation/preference.validation');

const savePreferences = async (req, res) => {
    try {
        const userId = req.user.sub;
        const validation = validatePreferenceUpdate(req.body);
        if (!validation.success) {
            return res.status(400).json({
                error: 'Invalid preference payload',
                details: validation.errors,
            });
        }

        const {
            min_age, max_age, minAge, maxAge, marital_status, religion, preferred_locations, min_income_band, income, city
        } = validation.data;

        const normalizedMinAge = minAge ?? min_age;
        const normalizedMaxAge = maxAge ?? max_age;
        if (normalizedMinAge && normalizedMaxAge && normalizedMinAge > normalizedMaxAge) {
            return res.status(400).json({ error: 'min_age must be less than or equal to max_age' });
        }

        const result = await preferenceService.upsertPreferences(userId, {
            min_age: normalizedMinAge,
            max_age: normalizedMaxAge,
            marital_status,
            religion,
            preferred_locations,
            min_income_band,
            income,
            city,
        });
        const completeness = await onboardingService.refreshCompleteness(userId);

        res.status(200).json({ ...result, onboardingCompleteness: completeness });
    } catch (error) {
        console.error('Save preferences error:', error);
        res.status(500).json({ error: error.message });
    }
};

module.exports = { savePreferences };
