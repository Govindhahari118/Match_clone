const prisma = require('../config/prisma');

const preferenceService = {
    async upsertPreferences(userId, data) {
        // Accept both snake_case (legacy) and camelCase (settings page)
        const minAge = parseInt(data.minAge || data.min_age) || 18;
        const maxAge = parseInt(data.maxAge || data.max_age) || 60;
        const religion = data.religion || 'Any';
        const income = data.income || data.min_income_band || null;
        const city = data.city || '';
        const rawLocs = data.preferred_locations || (city ? [city] : []);
        const preferredLocations = Array.isArray(rawLocs) ? rawLocs : rawLocs.split(',').map(l => l.trim()).filter(Boolean);

        const updateData = {
            minAge,
            maxAge,
            religionOpen: !religion || religion === 'Any',
            preferredReligions: (religion && religion !== 'Any') ? [religion] : [],
            casteOpen: true,
            preferredLocations,
            minIncomeBand: (income && income !== 'Any') ? income : null,
            updatedAt: new Date(),
        };

        return await prisma.partnerPreference.upsert({
            where: { userId },
            update: updateData,
            create: { userId, ...updateData, createdAt: new Date() }
        });
    },

    async getPreferences(userId) {
        return await prisma.partnerPreference.findUnique({ where: { userId } });
    }
};

module.exports = preferenceService;
