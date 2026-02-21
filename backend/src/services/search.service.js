const prisma = require('../config/prisma');
const matchingService = require('./matching.service');

function normalizeMode(mode) {
    const value = String(mode || 'regular').trim().toLowerCase();
    if (['regular', 'advanced', 'keyword', 'profile_id'].includes(value)) {
        return value;
    }
    return 'regular';
}

function profileToSearchItem(userRow, viewerId) {
    const profile = userRow?.profile;
    if (!profile) return null;
    const dob = profile.dateOfBirth ? new Date(profile.dateOfBirth) : null;
    const now = new Date();
    let age = null;
    if (dob) {
        age = now.getFullYear() - dob.getFullYear();
        const monthDelta = now.getMonth() - dob.getMonth();
        if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < dob.getDate())) age -= 1;
    }

    return {
        id: userRow.id,
        userId: userRow.id,
        firstName: profile.firstName,
        lastName: profile.lastName,
        age,
        city: profile.city,
        state: profile.state,
        profession: profile.profession,
        photo: userRow.photos?.[0]?.thumbnailUrl || userRow.photos?.[0]?.photoUrl || 'https://via.placeholder.com/150',
        isVerified: userRow.isVerified,
        isPremium: (userRow.subscriptions || []).length > 0,
        match: 90,
        religion: profile.religion,
        caste: profile.caste,
        motherTongue: profile.motherTongue,
        maritalStatus: profile.maritalStatus,
        education: profile.educationLevel,
        income: profile.incomeBand,
        heightCm: profile.heightCm,
        lastActiveAt: userRow.lastLogin,
        reasons: ['Profile ID exact match'],
    };
}

const searchService = {
    async executeSearch(userId, query = {}) {
        const mode = normalizeMode(query.mode);
        const page = Number.parseInt(query.page, 10) > 0 ? Number.parseInt(query.page, 10) : 1;
        const limit = Number.parseInt(query.limit, 10) > 0 ? Math.min(Number.parseInt(query.limit, 10), 50) : 20;
        const sort = String(query.sort || 'relevance').toLowerCase();

        if (mode === 'profile_id') {
            const profileId = String(query.profileId || query.query || '').trim();
            if (!profileId) {
                return {
                    mode,
                    items: [],
                    page,
                    limit,
                    total: 0,
                    hasNextPage: false,
                    sort,
                    message: 'profileId is required for profile_id mode',
                };
            }

            const userRow = await prisma.user.findFirst({
                where: {
                    id: profileId,
                    isActive: true,
                    isBanned: false,
                },
                include: {
                    profile: true,
                    photos: { where: { isPrimary: true }, take: 1 },
                    subscriptions: {
                        where: {
                            status: 'active',
                            OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }],
                        },
                        take: 1,
                    },
                },
            });

            const item = profileToSearchItem(userRow, userId);
            return {
                mode,
                items: item ? [item] : [],
                page: 1,
                limit: 1,
                total: item ? 1 : 0,
                hasNextPage: false,
                sort,
            };
        }

        const filters = { ...query };
        if (mode === 'keyword') {
            filters.keyword = query.query || query.keyword || '';
        }

        const result = await matchingService.getMatches(userId, filters, {
            returnMeta: true,
            page,
            limit,
            sort,
        });

        return {
            mode,
            ...result,
        };
    },

    async createSavedSearch(userId, payload = {}) {
        const name = String(payload.name || '').trim() || 'Untitled Search';
        const filters = payload.filters && typeof payload.filters === 'object' ? payload.filters : {};

        return prisma.savedSearch.create({
            data: {
                userId,
                name,
                filters,
            },
        });
    },

    async listSavedSearches(userId) {
        return prisma.savedSearch.findMany({
            where: { userId },
            orderBy: { updatedAt: 'desc' },
        });
    },

    async updateSavedSearch(userId, id, payload = {}) {
        const existing = await prisma.savedSearch.findFirst({
            where: { id, userId },
        });
        if (!existing) {
            return null;
        }

        const updateData = {};
        if (payload.name !== undefined) {
            updateData.name = String(payload.name || '').trim() || existing.name;
        }
        if (payload.filters && typeof payload.filters === 'object') {
            updateData.filters = payload.filters;
        }

        return prisma.savedSearch.update({
            where: { id },
            data: updateData,
        });
    },

    async deleteSavedSearch(userId, id) {
        const existing = await prisma.savedSearch.findFirst({
            where: { id, userId },
        });
        if (!existing) return false;

        await prisma.savedSearch.delete({ where: { id } });
        return true;
    },
};

module.exports = searchService;
