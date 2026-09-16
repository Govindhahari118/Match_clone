const prisma = require('../config/prisma');

const HARD_FIELD_ALLOWLIST = new Set(['age', 'location', 'religion', 'caste', 'food_habit']);

function toArray(value) {
    if (Array.isArray(value)) return value.map((item) => String(item).trim()).filter(Boolean);
    if (typeof value === 'string') return value.split(',').map((item) => item.trim()).filter(Boolean);
    return [];
}

function normalizeHardFields(value) {
    return Array.from(new Set(toArray(value)
        .map((item) => item.toLowerCase().replace(/[\s-]+/g, '_'))
        .filter((item) => HARD_FIELD_ALLOWLIST.has(item))));
}

const preferenceService = {
    HARD_FIELD_ALLOWLIST,

    async upsertPreferences(userId, data) {
        const minAge = Number.parseInt(data.minAge ?? data.min_age, 10) || 18;
        const maxAge = Number.parseInt(data.maxAge ?? data.max_age, 10) || 60;
        if (minAge < 18 || maxAge > 80 || minAge > maxAge) {
            const error = new Error('Invalid age preference range');
            error.statusCode = 400;
            throw error;
        }

        const religions = toArray(data.preferredReligions ?? data.religions ?? data.religion);
        const castes = toArray(data.preferredCastes ?? data.castes ?? data.caste);
        const foodHabits = toArray(data.foodHabitPreferences ?? data.food_habits);
        const explicitLocations = toArray(data.preferredLocations ?? data.preferred_locations);
        const preferredLocations = explicitLocations.length ? explicitLocations : toArray(data.city);
        const income = data.minIncomeBand ?? data.income ?? data.min_income_band ?? null;
        const hardFields = normalizeHardFields(data.hardFields ?? data.hard_fields);
        const religionOpen = data.religionOpen !== undefined
            ? Boolean(data.religionOpen)
            : religions.length === 0 || religions.some((item) => item.toLowerCase() === 'any');
        const casteOpen = data.casteOpen !== undefined
            ? Boolean(data.casteOpen)
            : castes.length === 0 || castes.some((item) => item.toLowerCase() === 'any');

        const updateData = {
            minAge,
            maxAge,
            religionOpen,
            preferredReligions: religions.filter((item) => item.toLowerCase() !== 'any'),
            casteOpen,
            preferredCastes: castes.filter((item) => item.toLowerCase() !== 'any'),
            preferredLocations,
            minEducation: data.minEducation ?? data.education ?? null,
            minIncomeBand: income && String(income).toLowerCase() !== 'any' ? income : null,
            foodHabitPreferences: foodHabits.filter((item) => item.toLowerCase() !== 'any'),
            hardFields,
            updatedAt: new Date(),
        };

        const preference = await prisma.partnerPreference.upsert({
            where: { userId },
            update: updateData,
            create: { userId, ...updateData, createdAt: new Date() },
        });

        await prisma.auditLog.create({
            data: {
                userId,
                action: 'partner_preferences_updated',
                resourceType: 'partner_preference',
                resourceId: preference.id,
                changes: { hardFields, minAge, maxAge },
            },
        });
        return preference;
    },

    async getPreferences(userId) {
        return prisma.partnerPreference.findUnique({ where: { userId } });
    },
};

module.exports = preferenceService;
