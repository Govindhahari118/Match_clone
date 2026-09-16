const prisma = require('../config/prisma');
const matchingService = require('./matching.service');
const privacyService = require('./privacy.service');
const safetyService = require('./safety.service');
const trustService = require('./trust.service');

function normalizeMode(mode) {
    const value = String(mode || 'regular').trim().toLowerCase();
    if (['regular', 'advanced', 'keyword', 'profile_id'].includes(value)) return value;
    return 'regular';
}

function ageFromDob(dateOfBirth) {
    if (!dateOfBirth) return null;
    const dob = new Date(dateOfBirth);
    if (Number.isNaN(dob.getTime())) return null;
    const now = new Date();
    let age = now.getFullYear() - dob.getFullYear();
    const monthDelta = now.getMonth() - dob.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < dob.getDate())) age -= 1;
    return age;
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
                return { mode, items: [], page: 1, limit: 1, total: 0, hasNextPage: false, sort, message: 'profileId is required for profile_id mode' };
            }
            if (profileId === userId || await safetyService.isBlocked(userId, profileId)) {
                return { mode, items: [], page: 1, limit: 1, total: 0, hasNextPage: false, sort };
            }

            const [userRow, viewer] = await Promise.all([
                prisma.user.findFirst({
                    where: {
                        id: profileId,
                        isActive: true,
                        isBanned: false,
                        deletedAt: null,
                        searchStatus: { in: ['active', 'low_activity'] },
                    },
                    include: {
                        profile: true,
                        photos: { where: { isPrimary: true, moderationStatus: 'approved' }, take: 1 },
                        verifications: true,
                        subscriptions: {
                            where: { status: { in: ['active', 'grace'] }, OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }] },
                            take: 1,
                        },
                    },
                }),
                prisma.user.findUnique({
                    where: { id: userId },
                    select: {
                        isVerified: true,
                        subscriptions: {
                            where: { status: { in: ['active', 'grace'] }, OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }] },
                            take: 1,
                            select: { id: true },
                        },
                    },
                }),
            ]);

            if (!userRow?.profile || trustService.isStale(userRow)) {
                return { mode, items: [], page: 1, limit: 1, total: 0, hasNextPage: false, sort };
            }

            const [{ settings }, photoAccess] = await Promise.all([
                privacyService.getUserPrivacySettings(profileId),
                safetyService.hasPhotoAccess(userId, profileId),
            ]);
            const viewerIsPremium = Boolean(viewer?.subscriptions?.length);
            const viewerIsVerified = Boolean(viewer?.isVerified);
            if (!privacyService.canViewProfile({ settings, isOwner: false, viewerIsPremium, viewerIsVerified })) {
                return { mode, items: [], page: 1, limit: 1, total: 0, hasNextPage: false, sort };
            }

            const mutual = await safetyService.areMatched(userId, profileId);
            const canViewPhotos = privacyService.canViewPhotos({
                settings,
                isOwner: false,
                isMutualMatch: Boolean(mutual),
                hasExplicitPhotoAccess: photoAccess,
            });
            const verification = trustService.verificationSummary(userRow);
            const activity = trustService.activityBucket(userRow);
            const trust = await trustService.getTrustSummary(profileId);
            const profile = userRow.profile;
            const photo = canViewPhotos ? (userRow.photos?.[0]?.thumbnailUrl || userRow.photos?.[0]?.photoUrl || null) : null;
            const item = {
                id: userRow.id,
                userId: userRow.id,
                firstName: profile.firstName,
                lastName: profile.lastName,
                age: ageFromDob(profile.dateOfBirth),
                city: profile.city,
                state: profile.state,
                profession: profile.profession,
                photo,
                photoLocked: !canViewPhotos,
                isVerified: userRow.isVerified,
                isPremium: userRow.subscriptions.length > 0,
                religion: profile.religion,
                caste: profile.caste,
                motherTongue: profile.motherTongue,
                maritalStatus: profile.maritalStatus,
                education: profile.educationLevel,
                income: profile.incomeBand,
                heightCm: profile.heightCm,
                verification,
                activity,
                managedBy: profile.role || 'self',
                profileCompleteness: trust?.profileCompleteness ?? profile.completionPercentage,
                whyRecommended: ['Exact profile ID search'],
                reasons: ['Exact profile ID search'],
            };
            return { mode, items: [item], page: 1, limit: 1, total: 1, hasNextPage: false, sort };
        }

        const filters = { ...query };
        if (mode === 'keyword') filters.keyword = query.query || query.keyword || '';

        const result = await matchingService.getMatches(userId, filters, { returnMeta: true, page, limit, sort });
        return { mode, ...result };
    },

    async createSavedSearch(userId, payload = {}) {
        const name = String(payload.name || '').trim() || 'Untitled Search';
        const filters = payload.filters && typeof payload.filters === 'object' ? payload.filters : {};
        return prisma.savedSearch.create({ data: { userId, name, filters } });
    },

    async listSavedSearches(userId) {
        return prisma.savedSearch.findMany({ where: { userId }, orderBy: { updatedAt: 'desc' } });
    },

    async updateSavedSearch(userId, id, payload = {}) {
        const existing = await prisma.savedSearch.findFirst({ where: { id, userId } });
        if (!existing) return null;
        const updateData = {};
        if (payload.name !== undefined) updateData.name = String(payload.name || '').trim() || existing.name;
        if (payload.filters && typeof payload.filters === 'object') updateData.filters = payload.filters;
        return prisma.savedSearch.update({ where: { id }, data: updateData });
    },

    async deleteSavedSearch(userId, id) {
        const existing = await prisma.savedSearch.findFirst({ where: { id, userId } });
        if (!existing) return false;
        await prisma.savedSearch.delete({ where: { id } });
        return true;
    },
};

module.exports = searchService;
