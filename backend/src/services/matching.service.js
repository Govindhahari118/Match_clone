const prisma = require('../config/prisma');
const privacyService = require('./privacy.service');
const profileExtensionService = require('./profile-extension.service');

const INCOME_ALIASES = {
    below_5L: ['below_5L', 'below 5l', 'below_3l', 'below 3l', '3-5l', '3-5L', 'Below 3L', 'Below 5L'],
    '5-10L': ['5-10L', '5-10l'],
    '10-25L': ['10-25L', '10-25l'],
    '25-50L': ['25-50L', '25-50l'],
    '50L+': ['50L+', '50l+', 'above 50l', 'Above 50L'],
};

const MARITAL_STATUS_ALIASES = {
    never_married: ['never_married', 'never married', 'Never Married'],
    divorced: ['divorced', 'Divorced'],
    widowed: ['widowed', 'Widowed'],
    annulled: ['annulled', 'Annulled'],
    separated: ['separated', 'Separated'],
    awaiting_divorce: ['awaiting_divorce', 'awaiting divorce', 'Awaiting Divorce'],
};

const EDUCATION_ALIASES = {
    high_school: ['high_school', 'high school', '10th', '12th', 'Diploma'],
    bachelors: ['bachelors', 'bachelor', "bachelor's", 'Graduate', 'MBBS', 'B.Tech', 'B.Com', 'B.Sc', 'LLB', 'B.Arch', 'B.Des'],
    masters: ['masters', 'master', "master's", 'Post Graduate', 'M.Tech', 'MBA', 'M.Com', 'M.Sc', 'MD', 'CA'],
    phd: ['phd', 'doctorate', 'Doctorate', 'PhD'],
};

function normalize(value) {
    return String(value || '')
        .trim()
        .toLowerCase()
        .replace(/[_\s]+/g, '_');
}

function expandAliasValue(value, aliasMap) {
    const normalizedInput = normalize(value);
    const matched = Object.entries(aliasMap).find(([canonical, aliases]) => {
        const canonicalMatches = normalize(canonical) === normalizedInput;
        const aliasMatches = aliases.some((alias) => normalize(alias) === normalizedInput);
        return canonicalMatches || aliasMatches;
    });

    if (!matched) {
        return [value];
    }

    const [canonical, aliases] = matched;
    return Array.from(new Set([canonical, ...aliases]));
}

function isAny(value) {
    return value === undefined || value === null || String(value).trim() === '' || String(value).trim().toLowerCase() === 'any';
}

function parseInteger(value) {
    const number = Number.parseInt(value, 10);
    return Number.isNaN(number) ? null : number;
}

function boolValue(value) {
    if (typeof value === 'boolean') return value;
    if (value === undefined || value === null) return false;
    const lowered = String(value).trim().toLowerCase();
    return lowered === 'true' || lowered === '1' || lowered === 'yes';
}

function normalizePhotoVisibilityValue(value) {
    const normalized = normalize(value);
    if (!normalized || normalized === 'any') return null;
    if (['request_access', 'request', 'requestaccess'].includes(normalized)) return 'request_access';
    if (['public', 'protected'].includes(normalized)) return normalized;
    return null;
}

function normalizeProfileVisibilityValue(value) {
    const normalized = normalize(value);
    if (!normalized || normalized === 'any') return null;
    if (['premium', 'premium_only'].includes(normalized)) return 'premium_only';
    if (['verified', 'verified_only'].includes(normalized)) return 'verified_only';
    if (['public', 'hidden'].includes(normalized)) return normalized;
    return null;
}

function normalizeVerificationLevelValue(value) {
    const normalized = normalize(value);
    if (!normalized || normalized === 'any') return null;
    if (['blue_tick', 'blue', 'tier_2', 'id_verified'].includes(normalized)) return 'id_verified';
    if (['basic', 'phone_email', 'contact_verified'].includes(normalized)) return 'basic';
    return null;
}

function passesVerificationLevel(user, level) {
    if (!level) return true;
    if (level === 'basic') {
        return Boolean(user?.isVerified);
    }
    if (level === 'id_verified') {
        return Boolean(user?.isVerified && user?.identityStatus === 'verified');
    }
    return true;
}

function getAge(dateOfBirth) {
    if (!dateOfBirth) return null;
    const now = new Date();
    const dob = new Date(dateOfBirth);
    let age = now.getFullYear() - dob.getFullYear();
    const monthDelta = now.getMonth() - dob.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < dob.getDate())) {
        age -= 1;
    }
    return age;
}

function deterministicScore(viewerId, user, profile) {
    const seed = `${viewerId}|${user.id}|${profile.religion || ''}|${profile.city || ''}|${profile.educationLevel || ''}|${profile.profession || ''}`;
    const hash = seed.split('').reduce((acc, char) => ((acc * 31) + char.charCodeAt(0)) % 997, 7);
    const qualityBoost =
        (user.isVerified ? 8 : 0) +
        (profile.educationLevel ? 5 : 0) +
        (profile.profession ? 5 : 0) +
        (profile.city ? 3 : 0) +
        (profile.motherTongue ? 3 : 0);
    return Math.min(99, Math.max(60, 60 + (hash % 21) + qualityBoost));
}

function parsePositiveInteger(value, fallback) {
    const parsed = parseInteger(value);
    if (!parsed || parsed < 1) {
        return fallback;
    }
    return parsed;
}

function activeSubscriptionFilter() {
    return {
        some: {
            status: 'active',
            OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }],
        },
    };
}

function hasHoroscopeDetails(profile) {
    return Boolean(profile?.gothra || profile?.zodiacSign || profile?.nakshatra || profile?.birthTime || profile?.birthPlace);
}

function buildMatchReasons({ candidateUser, candidateProfile, filters, viewerProfile, isPremium }) {
    const reasons = [];

    if (candidateUser.isVerified) reasons.push('Verified profile');
    if (isPremium) reasons.push('Premium member');

    if (!isAny(filters.religion) && String(candidateProfile.religion || '').toLowerCase() === String(filters.religion).toLowerCase()) {
        reasons.push('Matches selected religion');
    }
    if (!isAny(filters.city) && String(candidateProfile.city || '').toLowerCase().includes(String(filters.city).toLowerCase())) {
        reasons.push('Matches preferred city');
    }
    if (!isAny(filters.education) && String(candidateProfile.educationLevel || '').toLowerCase().includes(String(filters.education).toLowerCase())) {
        reasons.push('Education preference aligned');
    }
    if (!isAny(filters.profession) && String(candidateProfile.profession || '').toLowerCase().includes(String(filters.profession).toLowerCase())) {
        reasons.push('Profession preference aligned');
    }
    if (viewerProfile?.city && candidateProfile.city && String(viewerProfile.city).toLowerCase() === String(candidateProfile.city).toLowerCase()) {
        reasons.push('Same city');
    }
    if (candidateUser.lastLogin && new Date(candidateUser.lastLogin).getTime() > Date.now() - 24 * 60 * 60 * 1000) {
        reasons.push('Recently active');
    }
    if (hasHoroscopeDetails(candidateProfile)) {
        reasons.push('Horoscope details available');
    }

    return Array.from(new Set(reasons)).slice(0, 3);
}

const matchingService = {
    async getMatches(userId, filters = {}, options = {}) {
        const page = parsePositiveInteger(options.page ?? filters.page, 1);
        const limit = Math.min(parsePositiveInteger(options.limit ?? filters.limit, 20), 50);
        const sort = String(options.sort ?? filters.sort ?? 'relevance').toLowerCase();
        const includeMeta = Boolean(options.returnMeta);

        // 1. Get viewer context to exclude already-interacted profiles.
        const [viewer, viewerProfile] = await Promise.all([
            prisma.user.findUnique({
                where: { id: userId },
                include: {
                    likesSent: { select: { receiverId: true } },
                    matches: { select: { userBId: true } },
                    matchesAsUserB: { select: { userAId: true } },
                    subscriptions: {
                        where: {
                            status: 'active',
                            OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }],
                        },
                        select: { id: true },
                        take: 1,
                    },
                },
            }),
            prisma.profile.findUnique({ where: { userId } }),
        ]);

        if (!viewer) {
            return includeMeta
                ? { items: [], page, limit, total: 0, hasNextPage: false, sort }
                : [];
        }

        const viewerIsPremium = Boolean(viewer.subscriptions?.length);
        const viewerIsVerified = Boolean(viewer.isVerified);

        const excludeIds = [
            userId,
            ...viewer.likesSent.map((item) => item.receiverId),
            ...viewer.matches.map((item) => item.userBId),
            ...viewer.matchesAsUserB.map((item) => item.userAId),
        ];

        // 2. Build query
        const where = {
            id: { notIn: excludeIds },
            isBanned: false,
            isActive: true,
            profile: {
                is: {},
            },
        };

        if (!isAny(filters.gender)) {
            where.profile.is.gender = filters.gender;
        }

        const minAge = parseInteger(filters.minAge);
        const maxAge = parseInteger(filters.maxAge);
        if (minAge) {
            const maxBirthDate = new Date();
            maxBirthDate.setFullYear(maxBirthDate.getFullYear() - minAge);
            where.profile.is.dateOfBirth = { ...where.profile.is.dateOfBirth, lte: maxBirthDate };
        }
        if (maxAge) {
            const minBirthDate = new Date();
            minBirthDate.setFullYear(minBirthDate.getFullYear() - maxAge - 1);
            where.profile.is.dateOfBirth = { ...where.profile.is.dateOfBirth, gte: minBirthDate };
        }

        if (!isAny(filters.religion)) where.profile.is.religion = filters.religion;
        if (!isAny(filters.caste)) where.profile.is.caste = filters.caste;

        if (!isAny(filters.maritalStatus)) {
            const maritalValues = expandAliasValue(filters.maritalStatus, MARITAL_STATUS_ALIASES);
            where.profile.is.maritalStatus = maritalValues.length === 1 ? maritalValues[0] : { in: maritalValues };
        }

        if (!isAny(filters.city)) where.profile.is.city = { contains: filters.city, mode: 'insensitive' };
        if (!isAny(filters.state)) where.profile.is.state = { contains: filters.state, mode: 'insensitive' };
        if (!isAny(filters.country)) where.profile.is.country = { contains: filters.country, mode: 'insensitive' };
        if (!isAny(filters.motherTongue)) where.profile.is.motherTongue = { contains: filters.motherTongue, mode: 'insensitive' };

        const minHeight = parseInteger(filters.minHeight);
        const maxHeight = parseInteger(filters.maxHeight);
        if (minHeight || maxHeight) {
            where.profile.is.heightCm = {
                ...(minHeight ? { gte: minHeight } : {}),
                ...(maxHeight ? { lte: maxHeight } : {}),
            };
        }

        if (!isAny(filters.education)) {
            const educationValues = expandAliasValue(filters.education, EDUCATION_ALIASES);
            where.profile.is.educationLevel = educationValues.length === 1 ? educationValues[0] : { in: educationValues };
        }

        if (!isAny(filters.profession)) {
            where.profile.is.profession = { contains: filters.profession, mode: 'insensitive' };
        }

        if (!isAny(filters.income)) {
            const incomeValues = expandAliasValue(filters.income, INCOME_ALIASES);
            where.profile.is.incomeBand = incomeValues.length === 1 ? incomeValues[0] : { in: incomeValues };
        }

        if (boolValue(filters.verifiedOnly)) {
            where.isVerified = true;
        }

        if (boolValue(filters.withPhotoOnly)) {
            where.photos = { some: {} };
        }

        if (boolValue(filters.premiumOnly)) {
            where.subscriptions = activeSubscriptionFilter();
        }

        if (boolValue(filters.withHoroscopeOnly)) {
            where.profile.is.OR = [
                { gothra: { not: null } },
                { zodiacSign: { not: null } },
                { nakshatra: { not: null } },
            ];
        }

        if (boolValue(filters.onlineNow)) {
            const onlineThreshold = new Date(Date.now() - (15 * 60 * 1000));
            where.lastLogin = { gte: onlineThreshold };
        }

        const lastActiveDays = parseInteger(filters.lastActiveDays);
        if (lastActiveDays && lastActiveDays > 0) {
            const activityThreshold = new Date(Date.now() - (lastActiveDays * 24 * 60 * 60 * 1000));
            where.lastLogin = { ...(where.lastLogin || {}), gte: activityThreshold };
        }

        const keyword = String(filters.keyword || filters.query || '').trim();
        if (keyword) {
            where.OR = [
                { profile: { is: { firstName: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { lastName: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { city: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { state: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { profession: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { religion: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { caste: { contains: keyword, mode: 'insensitive' } } } },
            ];
        }

        const users = await prisma.user.findMany({
            where,
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
            take: includeMeta ? Math.min(Math.max(limit * 5, 100), 500) : 80,
        });

        const candidateIds = users.map((userRow) => userRow.id);
        const [privacyMap, extensionMap] = await Promise.all([
            privacyService.getUsersPrivacySettings(candidateIds),
            profileExtensionService.getUsersProfileExtensions(candidateIds),
        ]);

        const requestedPhotoVisibility = normalizePhotoVisibilityValue(filters.photoVisibility);
        const requestedProfileVisibility = normalizeProfileVisibilityValue(filters.profileVisibility);
        const requestedVerificationLevel = normalizeVerificationLevelValue(filters.verificationLevel);
        const requestedHasChildren = profileExtensionService.normalizeHasChildren(filters.hasChildren);
        const requestedResidentialStatus = profileExtensionService.normalizeResidentialStatus(filters.residentialStatus);
        const requestedDistrict = String(filters.district || '').trim().toLowerCase();

        const mapped = users
            .filter((userRow) => userRow.profile)
            .map((userRow) => {
                const profile = userRow.profile;
                const isPremium = userRow.subscriptions.length > 0;
                const privacySettings = privacyMap.get(userRow.id) || privacyService.DEFAULT_PRIVACY_SETTINGS;
                const extension = extensionMap.get(userRow.id) || profileExtensionService.DEFAULT_PROFILE_EXTENSION;

                const canViewProfile = privacyService.canViewProfile({
                    settings: privacySettings,
                    isOwner: false,
                    viewerIsPremium,
                    viewerIsVerified,
                });
                if (!canViewProfile) return null;

                if (requestedPhotoVisibility && privacySettings.photoVisibility !== requestedPhotoVisibility) return null;
                if (requestedProfileVisibility && privacySettings.profileVisibility !== requestedProfileVisibility) return null;
                if (!passesVerificationLevel(userRow, requestedVerificationLevel)) return null;

                if (requestedHasChildren && extension.hasChildren !== requestedHasChildren) return null;
                if (requestedResidentialStatus && extension.residentialStatus !== requestedResidentialStatus) return null;
                if (requestedDistrict && !String(extension.district || '').toLowerCase().includes(requestedDistrict)) return null;

                const canViewPhotos = privacyService.canViewPhotos({
                    settings: privacySettings,
                    isOwner: false,
                    isMutualMatch: false,
                    viewerIsPremium,
                });
                if (boolValue(filters.withPhotoOnly) && !canViewPhotos) return null;

                const matchScore = deterministicScore(userId, userRow, profile);
                const photo = canViewPhotos
                    ? (userRow.photos[0]?.thumbnailUrl || userRow.photos[0]?.photoUrl || 'https://via.placeholder.com/150')
                    : 'https://via.placeholder.com/150?text=Photo+Protected';
                const reasons = buildMatchReasons({
                    candidateUser: userRow,
                    candidateProfile: profile,
                    filters,
                    viewerProfile,
                    isPremium,
                });

                return {
                    id: userRow.id,
                    userId: userRow.id,
                    firstName: profile.firstName,
                    lastName: profile.lastName,
                    age: getAge(profile.dateOfBirth),
                    city: profile.city,
                    state: profile.state,
                    country: profile.country,
                    district: extension.district,
                    profession: profile.profession,
                    photo,
                    photoLocked: !canViewPhotos,
                    isVerified: userRow.isVerified,
                    isPremium,
                    match: matchScore,
                    religion: profile.religion,
                    caste: profile.caste,
                    motherTongue: profile.motherTongue,
                    maritalStatus: profile.maritalStatus,
                    education: profile.educationLevel,
                    income: profile.incomeBand,
                    heightCm: profile.heightCm,
                    hasChildren: extension.hasChildren,
                    residentialStatus: extension.residentialStatus,
                    lastActiveAt: privacySettings.showLastSeen ? userRow.lastLogin : null,
                    reasons,
                };
            })
            .filter(Boolean);

        const sorted = [...mapped].sort((a, b) => {
            switch (sort) {
                case 'newest':
                    return new Date(b.lastActiveAt || 0).getTime() - new Date(a.lastActiveAt || 0).getTime();
                case 'activity':
                    return new Date(b.lastActiveAt || 0).getTime() - new Date(a.lastActiveAt || 0).getTime();
                case 'compatibility':
                case 'relevance':
                default:
                    return (b.match || 0) - (a.match || 0);
            }
        });

        if (!includeMeta) {
            return sorted.slice(0, 50);
        }

        const start = (page - 1) * limit;
        const items = sorted.slice(start, start + limit);
        const total = sorted.length;
        return {
            items,
            page,
            limit,
            total,
            hasNextPage: start + limit < total,
            sort,
        };
    },
};

module.exports = matchingService;
