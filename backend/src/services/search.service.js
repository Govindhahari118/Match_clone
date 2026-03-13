const prisma = require('../config/prisma');
const matchingService = require('./matching.service');
const privacyService = require('./privacy.service');
const { parsePagination } = require('../utils/pagination');

function normalizeMode(mode) {
    const value = String(mode || 'regular').trim().toLowerCase();
    if (['regular', 'advanced', 'keyword', 'profile_id'].includes(value)) {
        return value;
    }
    return 'regular';
}

function normalizeArrayInput(value) {
    if (Array.isArray(value)) {
        return value.map((item) => String(item || '').trim()).filter(Boolean);
    }
    const asString = String(value || '').trim();
    if (!asString) return [];
    return asString.split(',').map((item) => item.trim()).filter(Boolean);
}

function dedupe(items = [], keyFn = (item) => item) {
    const seen = new Set();
    const out = [];
    for (const item of items) {
        const key = keyFn(item);
        if (!key || seen.has(key)) continue;
        seen.add(key);
        out.push(item);
    }
    return out;
}

function profileToSearchItem(userRow) {
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
        subCaste: profile.subCaste,
        motherTongue: profile.motherTongue,
        maritalStatus: profile.maritalStatus,
        education: profile.educationLevel,
        income: profile.incomeBand,
        heightCm: profile.heightCm,
        gothra: profile.gothra,
        nakshatra: profile.nakshatra,
        zodiacSign: profile.zodiacSign,
        dosha: profile.dosha,
        foodHabit: profile.foodHabit,
        lastActiveAt: userRow.lastLogin,
        reasons: ['Profile ID exact match'],
    };
}

function buildRecoveryAttempts(filters = {}) {
    const normalized = { ...filters };
    delete normalized.page;
    delete normalized.limit;
    delete normalized.mode;
    delete normalized.sort;

    const attempts = [];

    const attemptA = { ...normalized };
    delete attemptA.caste;
    delete attemptA.motherTongue;
    delete attemptA.district;
    attempts.push({
        label: 'Relaxed community-only filters',
        filters: attemptA,
    });

    const attemptB = { ...attemptA };
    delete attemptB.profession;
    delete attemptB.education;
    delete attemptB.income;
    attempts.push({
        label: 'Relaxed profession and education constraints',
        filters: attemptB,
    });

    const attemptC = { ...attemptB };
    if (attemptC.minAge) {
        const current = Number.parseInt(attemptC.minAge, 10) || 18;
        attemptC.minAge = Math.max(18, current - 2);
    }
    if (attemptC.maxAge) {
        const current = Number.parseInt(attemptC.maxAge, 10) || 60;
        attemptC.maxAge = Math.min(80, current + 2);
    }
    attempts.push({
        label: 'Expanded age range by +/-2 years',
        filters: attemptC,
    });

    const attemptD = { ...attemptC };
    delete attemptD.city;
    delete attemptD.state;
    delete attemptD.religion;
    attempts.push({
        label: 'Relaxed location and religion constraints',
        filters: attemptD,
    });

    return attempts;
}

async function findRecoveryResult(userId, filters, options = {}) {
    const attempts = buildRecoveryAttempts(filters);
    for (const attempt of attempts) {
        const result = await matchingService.getMatches(userId, attempt.filters, {
            returnMeta: true,
            page: 1,
            limit: Math.min(options.limit || 8, 20),
            sort: options.sort || 'relevance',
        });
        if (result.total > 0) {
            return {
                suggestedFilters: attempt.filters,
                message: attempt.label,
                sampleItems: result.items.slice(0, 8),
                totalRecovered: result.total,
            };
        }
    }
    return null;
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

            const item = profileToSearchItem(userRow);
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

        let recovery = null;
        if (result.total === 0) {
            recovery = await findRecoveryResult(userId, filters, { limit: 8, sort });
        }

        return {
            mode,
            ...result,
            recovery,
        };
    },

    async getDiscoveryRails(userId, query = {}) {
        const limit = Math.min(Math.max(Number.parseInt(query.limit || '8', 10) || 8, 3), 20);
        const baseFilters = { ...query };
        delete baseFilters.limit;
        delete baseFilters.mode;
        delete baseFilters.sort;

        const rails = [
            {
                key: 'bestFit',
                title: 'Best Fit',
                filters: { ...baseFilters },
                options: { sort: 'relevance' },
            },
            {
                key: 'recentlyActive',
                title: 'Recently Active',
                filters: { ...baseFilters, lastActiveDays: 3 },
                options: { sort: 'activity' },
            },
            {
                key: 'highlyCompatible',
                title: 'Highly Compatible',
                filters: { ...baseFilters, withHoroscopeOnly: baseFilters.withHoroscopeOnly ?? false },
                options: { sort: 'compatibility' },
            },
            {
                key: 'newThisWeek',
                title: 'New This Week',
                filters: { ...baseFilters },
                options: { sort: 'newest' },
            },
        ];

        const results = await Promise.all(
            rails.map((rail) => matchingService.getMatches(userId, rail.filters, {
                returnMeta: false,
                limit,
                sort: rail.options.sort,
            })),
        );

        return {
            rails: rails.map((rail, index) => ({
                key: rail.key,
                title: rail.title,
                items: (results[index] || []).slice(0, limit),
            })),
        };
    },

    async getSuggestions(_userId, query = {}) {
        const q = String(query.q || query.query || '').trim();
        const limit = Math.min(Math.max(Number.parseInt(query.limit || '10', 10) || 10, 3), 30);
        if (!q || q.length < 2) {
            return {
                q,
                suggestions: [],
                grouped: {
                    professions: [],
                    locations: [],
                    communities: [],
                },
            };
        }

        const profiles = await prisma.profile.findMany({
            where: {
                OR: [
                    { profession: { contains: q, mode: 'insensitive' } },
                    { city: { contains: q, mode: 'insensitive' } },
                    { state: { contains: q, mode: 'insensitive' } },
                    { religion: { contains: q, mode: 'insensitive' } },
                    { caste: { contains: q, mode: 'insensitive' } },
                    { motherTongue: { contains: q, mode: 'insensitive' } },
                ],
            },
            select: {
                profession: true,
                city: true,
                state: true,
                religion: true,
                caste: true,
                motherTongue: true,
            },
            take: 200,
        });

        const professions = dedupe(
            profiles
                .map((item) => item.profession)
                .filter((value) => value && String(value).toLowerCase().includes(q.toLowerCase()))
                .map((value) => ({ type: 'profession', value })),
            (item) => `${item.type}:${String(item.value).toLowerCase()}`,
        ).slice(0, limit);

        const locations = dedupe(
            profiles
                .flatMap((item) => [item.city, item.state])
                .filter((value) => value && String(value).toLowerCase().includes(q.toLowerCase()))
                .map((value) => ({ type: 'location', value })),
            (item) => `${item.type}:${String(item.value).toLowerCase()}`,
        ).slice(0, limit);

        const communities = dedupe(
            profiles
                .flatMap((item) => [item.religion, item.caste, item.motherTongue])
                .filter((value) => value && String(value).toLowerCase().includes(q.toLowerCase()))
                .map((value) => ({ type: 'community', value })),
            (item) => `${item.type}:${String(item.value).toLowerCase()}`,
        ).slice(0, limit);

        const suggestions = dedupe(
            [...professions, ...locations, ...communities],
            (item) => `${item.type}:${String(item.value).toLowerCase()}`,
        ).slice(0, limit * 2);

        return {
            q,
            suggestions,
            grouped: {
                professions,
                locations,
                communities,
            },
        };
    },

    async compareProfiles(userId, query = {}) {
        const ids = normalizeArrayInput(query.ids || query.userIds).slice(0, 6);
        if (ids.length < 2) {
            return { error: 'At least two profile IDs are required for compare mode', statusCode: 400 };
        }

        const viewer = await prisma.user.findUnique({
            where: { id: userId },
            select: {
                id: true,
                isVerified: true,
                subscriptions: {
                    where: {
                        status: 'active',
                        OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }],
                    },
                    select: { id: true },
                    take: 1,
                },
            },
        });

        const users = await prisma.user.findMany({
            where: {
                id: { in: ids },
                isActive: true,
                isBanned: false,
            },
            include: {
                profile: true,
                photos: { where: { isPrimary: true }, take: 1 },
            },
        });

        const viewerIsPremium = Boolean(viewer?.subscriptions?.length);
        const viewerIsVerified = Boolean(viewer?.isVerified);

        const projections = [];
        for (const userRow of users) {
            if (!userRow.profile) continue;
            const { settings } = await privacyService.getUserPrivacySettings(userRow.id);
            const canViewProfile = privacyService.canViewProfile({
                settings,
                isOwner: userRow.id === userId,
                viewerIsPremium,
                viewerIsVerified,
            });
            if (!canViewProfile) continue;
            const canViewPhotos = privacyService.canViewPhotos({
                settings,
                isOwner: userRow.id === userId,
                isMutualMatch: false,
                viewerIsPremium,
            });

            projections.push({
                userId: userRow.id,
                firstName: userRow.profile.firstName,
                lastName: userRow.profile.lastName,
                age: userRow.profile.dateOfBirth
                    ? Math.max(0, new Date().getFullYear() - new Date(userRow.profile.dateOfBirth).getFullYear())
                    : null,
                city: userRow.profile.city,
                state: userRow.profile.state,
                religion: userRow.profile.religion,
                caste: userRow.profile.caste,
                profession: userRow.profile.profession,
                education: userRow.profile.educationLevel,
                income: userRow.profile.incomeBand,
                heightCm: userRow.profile.heightCm,
                isVerified: userRow.isVerified,
                photo: canViewPhotos
                    ? (userRow.photos?.[0]?.thumbnailUrl || userRow.photos?.[0]?.photoUrl || null)
                    : null,
            });
        }

        return {
            comparedCount: projections.length,
            profiles: projections,
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

    async listSavedSearches(userId, query = {}) {
        const { limit, skip } = parsePagination(query, { defaultLimit: 20, maxLimit: 100 });
        return prisma.savedSearch.findMany({
            where: { userId },
            orderBy: { updatedAt: 'desc' },
            skip,
            take: limit,
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
