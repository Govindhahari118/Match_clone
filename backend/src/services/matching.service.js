const prisma = require('../config/prisma');
const privacyService = require('./privacy.service');
const profileExtensionService = require('./profile-extension.service');
const trustService = require('./trust.service');
const exposureService = require('./exposure.service');

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
    return String(value || '').trim().toLowerCase().replace(/[_\s]+/g, '_');
}
function isAny(value) {
    return value === undefined || value === null || String(value).trim() === '' || String(value).trim().toLowerCase() === 'any';
}
function parseInteger(value) {
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? null : parsed;
}
function parsePositiveInteger(value, fallback) {
    const parsed = parseInteger(value);
    return parsed && parsed > 0 ? parsed : fallback;
}
function boolValue(value) {
    if (typeof value === 'boolean') return value;
    if (value === undefined || value === null) return false;
    return ['true', '1', 'yes'].includes(String(value).trim().toLowerCase());
}
function expandAliasValue(value, map) {
    const input = normalize(value);
    const matched = Object.entries(map).find(([canonical, aliases]) =>
        normalize(canonical) === input || aliases.some((alias) => normalize(alias) === input));
    return matched ? Array.from(new Set([matched[0], ...matched[1]])) : [value];
}
function getAge(dateOfBirth) {
    if (!dateOfBirth) return null;
    const now = new Date();
    const dob = new Date(dateOfBirth);
    let age = now.getFullYear() - dob.getFullYear();
    const monthDelta = now.getMonth() - dob.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < dob.getDate())) age -= 1;
    return age;
}
function hasHoroscopeDetails(profile) {
    return Boolean(profile?.gothra || profile?.zodiacSign || profile?.nakshatra || profile?.birthTime || profile?.birthPlace);
}
function activeSubscriptionFilter() {
    return { some: { status: { in: ['active', 'grace'] }, OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }] } };
}
function normalizePhotoVisibilityValue(value) {
    const parsed = normalize(value);
    if (!parsed || parsed === 'any') return null;
    if (['request_access', 'request', 'requestaccess'].includes(parsed)) return 'request_access';
    return ['public', 'protected'].includes(parsed) ? parsed : null;
}
function normalizeProfileVisibilityValue(value) {
    const parsed = normalize(value);
    if (!parsed || parsed === 'any') return null;
    if (['premium', 'premium_only'].includes(parsed)) return 'premium_only';
    if (['verified', 'verified_only'].includes(parsed)) return 'verified_only';
    return ['public', 'hidden'].includes(parsed) ? parsed : null;
}
function normalizeVerificationLevelValue(value) {
    const parsed = normalize(value);
    if (!parsed || parsed === 'any') return null;
    if (['blue_tick', 'blue', 'tier_2', 'id_verified'].includes(parsed)) return 'id_verified';
    if (['basic', 'phone_email', 'contact_verified'].includes(parsed)) return 'basic';
    return null;
}
function passesVerificationLevel(user, level) {
    if (!level) return true;
    if (level === 'basic') return Boolean(user.isVerified || user.verifications?.some((row) => row.status === 'verified'));
    if (level === 'id_verified') return user.identityStatus === 'verified';
    return true;
}

function preferenceCompatibility(viewerProfile, viewerPreference, candidateProfile) {
    const reasons = [];
    let earned = 0;
    let possible = 0;

    if (viewerPreference) {
        possible += 25;
        const age = getAge(candidateProfile.dateOfBirth);
        if (age !== null && age >= viewerPreference.minAge && age <= viewerPreference.maxAge) {
            earned += 25;
            reasons.push('Age preference matches');
        }

        if (viewerPreference.preferredLocations?.length) {
            possible += 20;
            const locations = viewerPreference.preferredLocations.map((item) => String(item).toLowerCase());
            if ([candidateProfile.city, candidateProfile.state, candidateProfile.country]
                .filter(Boolean)
                .some((item) => locations.includes(String(item).toLowerCase()))) {
                earned += 20;
                reasons.push('Location preference matches');
            }
        }

        if (viewerPreference.preferredReligions?.length) {
            possible += 15;
            if (viewerPreference.preferredReligions.includes(candidateProfile.religion)) {
                earned += 15;
                reasons.push('Religion preference aligns');
            }
        }

        if (viewerPreference.preferredCastes?.length) {
            possible += 10;
            if (viewerPreference.preferredCastes.includes(candidateProfile.caste)) {
                earned += 10;
                reasons.push('Community preference aligns');
            }
        }

        if (viewerPreference.foodHabitPreferences?.length) {
            possible += 10;
            if (viewerPreference.foodHabitPreferences.includes(candidateProfile.foodHabit)) {
                earned += 10;
                reasons.push('Lifestyle preference aligns');
            }
        }
    }

    if (viewerProfile?.city && candidateProfile.city) {
        possible += 10;
        if (viewerProfile.city.toLowerCase() === candidateProfile.city.toLowerCase()) {
            earned += 10;
            reasons.push('Same city');
        }
    }
    if (viewerProfile?.motherTongue && candidateProfile.motherTongue) {
        possible += 10;
        if (viewerProfile.motherTongue.toLowerCase() === candidateProfile.motherTongue.toLowerCase()) {
            earned += 10;
            reasons.push('Same mother tongue');
        }
    }

    const score = possible > 0 ? Math.round((earned / possible) * 100) : null;
    return {
        score,
        strength: score === null ? 'unknown' : score >= 75 ? 'strong' : score >= 50 ? 'good' : 'partial',
        reasons: Array.from(new Set(reasons)),
    };
}

function hardPreferencePasses(preference, candidateProfile) {
    if (!preference?.hardFields?.length) return true;
    const hard = new Set(preference.hardFields.map(normalize));
    const age = getAge(candidateProfile.dateOfBirth);

    if (hard.has('age') && (age === null || age < preference.minAge || age > preference.maxAge)) return false;
    if (hard.has('location') && preference.preferredLocations?.length) {
        const values = [candidateProfile.city, candidateProfile.state, candidateProfile.country].filter(Boolean).map((v) => v.toLowerCase());
        const wanted = preference.preferredLocations.map((v) => String(v).toLowerCase());
        if (!values.some((value) => wanted.includes(value))) return false;
    }
    if (hard.has('religion') && preference.preferredReligions?.length && !preference.preferredReligions.includes(candidateProfile.religion)) return false;
    if (hard.has('caste') && preference.preferredCastes?.length && !preference.preferredCastes.includes(candidateProfile.caste)) return false;
    if (hard.has('food_habit') && preference.foodHabitPreferences?.length && !preference.foodHabitPreferences.includes(candidateProfile.foodHabit)) return false;
    return true;
}

function freshnessPoints(activity) {
    switch (activity.code) {
        case 'active_today': return 20;
        case 'active_this_week': return 16;
        case 'active_recently': return 10;
        case 'low_activity': return 4;
        default: return 0;
    }
}

const matchingService = {
    async getMatches(userId, filters = {}, options = {}) {
        const page = parsePositiveInteger(options.page ?? filters.page, 1);
        const limit = Math.min(parsePositiveInteger(options.limit ?? filters.limit, 20), 50);
        const sort = String(options.sort ?? filters.sort ?? 'relevance').toLowerCase();
        const includeMeta = Boolean(options.returnMeta);

        const viewer = await prisma.user.findUnique({
            where: { id: userId },
            include: {
                profile: true,
                partnerPreference: true,
                likesSent: { select: { receiverId: true } },
                matches: { where: { isActive: true }, select: { userBId: true } },
                matchesAsUserB: { where: { isActive: true }, select: { userAId: true } },
                subscriptions: { where: activeSubscriptionFilter().some, select: { id: true }, take: 1 },
            },
        });
        if (!viewer) return includeMeta ? { items: [], page, limit, total: 0, hasNextPage: false, sort } : [];

        const [blocks, exposureExcluded] = await Promise.all([
            prisma.userBlock.findMany({
                where: { OR: [{ blockerId: userId }, { blockedUserId: userId }] },
                select: { blockerId: true, blockedUserId: true },
            }),
            exposureService.getExcludedCandidateIds(userId),
        ]);
        const blockedIds = blocks.map((row) => row.blockerId === userId ? row.blockedUserId : row.blockerId);
        const excludeIds = Array.from(new Set([
            userId,
            ...blockedIds,
            ...exposureExcluded,
            ...viewer.likesSent.map((row) => row.receiverId),
            ...viewer.matches.map((row) => row.userBId),
            ...viewer.matchesAsUserB.map((row) => row.userAId),
        ]));

        const where = {
            id: { notIn: excludeIds },
            isBanned: false,
            isActive: true,
            deletedAt: null,
            searchStatus: { in: ['active', 'low_activity'] },
            profile: { is: {} },
        };

        if (!isAny(filters.gender)) where.profile.is.gender = filters.gender;
        const minAge = parseInteger(filters.minAge);
        const maxAge = parseInteger(filters.maxAge);
        if (minAge) {
            const maxBirth = new Date(); maxBirth.setFullYear(maxBirth.getFullYear() - minAge);
            where.profile.is.dateOfBirth = { ...(where.profile.is.dateOfBirth || {}), lte: maxBirth };
        }
        if (maxAge) {
            const minBirth = new Date(); minBirth.setFullYear(minBirth.getFullYear() - maxAge - 1);
            where.profile.is.dateOfBirth = { ...(where.profile.is.dateOfBirth || {}), gte: minBirth };
        }
        if (!isAny(filters.religion)) where.profile.is.religion = filters.religion;
        if (!isAny(filters.caste)) where.profile.is.caste = filters.caste;
        if (!isAny(filters.maritalStatus)) {
            const values = expandAliasValue(filters.maritalStatus, MARITAL_STATUS_ALIASES);
            where.profile.is.maritalStatus = values.length === 1 ? values[0] : { in: values };
        }
        if (!isAny(filters.city)) where.profile.is.city = { contains: filters.city, mode: 'insensitive' };
        if (!isAny(filters.state)) where.profile.is.state = { contains: filters.state, mode: 'insensitive' };
        if (!isAny(filters.country)) where.profile.is.country = { contains: filters.country, mode: 'insensitive' };
        if (!isAny(filters.motherTongue)) where.profile.is.motherTongue = { contains: filters.motherTongue, mode: 'insensitive' };

        const minHeight = parseInteger(filters.minHeight);
        const maxHeight = parseInteger(filters.maxHeight);
        if (minHeight || maxHeight) where.profile.is.heightCm = { ...(minHeight ? { gte: minHeight } : {}), ...(maxHeight ? { lte: maxHeight } : {}) };
        if (!isAny(filters.education)) {
            const values = expandAliasValue(filters.education, EDUCATION_ALIASES);
            where.profile.is.educationLevel = values.length === 1 ? values[0] : { in: values };
        }
        if (!isAny(filters.profession)) where.profile.is.profession = { contains: filters.profession, mode: 'insensitive' };
        if (!isAny(filters.income)) {
            const values = expandAliasValue(filters.income, INCOME_ALIASES);
            where.profile.is.incomeBand = values.length === 1 ? values[0] : { in: values };
        }
        if (boolValue(filters.verifiedOnly)) where.isVerified = true;
        if (boolValue(filters.withPhotoOnly)) where.photos = { some: { moderationStatus: 'approved' } };
        if (boolValue(filters.premiumOnly)) where.subscriptions = activeSubscriptionFilter();
        if (boolValue(filters.withHoroscopeOnly)) where.profile.is.OR = [
            { gothra: { not: null } }, { zodiacSign: { not: null } }, { nakshatra: { not: null } },
        ];
        if (boolValue(filters.onlineNow)) where.lastActiveAt = { gte: new Date(Date.now() - 15 * 60000) };
        const lastActiveDays = parseInteger(filters.lastActiveDays);
        if (lastActiveDays && lastActiveDays > 0) where.lastActiveAt = { gte: new Date(Date.now() - lastActiveDays * 86400000) };

        const keyword = String(filters.keyword || filters.query || '').trim();
        if (keyword) {
            where.OR = [
                { profile: { is: { firstName: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { lastName: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { city: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { profession: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { religion: { contains: keyword, mode: 'insensitive' } } } },
                { profile: { is: { caste: { contains: keyword, mode: 'insensitive' } } } },
            ];
        }

        const users = await prisma.user.findMany({
            where,
            include: {
                profile: true,
                partnerPreference: true,
                photos: { where: { isPrimary: true, moderationStatus: 'approved' }, take: 1 },
                verifications: true,
                subscriptions: { where: activeSubscriptionFilter().some, take: 1 },
            },
            take: Math.min(Math.max(limit * 8, 120), 500),
        });

        const candidateIds = users.map((user) => user.id);
        const [privacyMap, extensionMap, photoGrants] = await Promise.all([
            privacyService.getUsersPrivacySettings(candidateIds),
            profileExtensionService.getUsersProfileExtensions(candidateIds),
            prisma.photoAccessRequest.findMany({
                where: { requesterId: userId, targetId: { in: candidateIds }, status: 'approved' },
                select: { targetId: true },
            }),
        ]);
        const photoGrantIds = new Set(photoGrants.map((row) => row.targetId));
        const viewerIsPremium = Boolean(viewer.subscriptions.length);
        const viewerIsVerified = Boolean(viewer.isVerified);
        const requestedPhotoVisibility = normalizePhotoVisibilityValue(filters.photoVisibility);
        const requestedProfileVisibility = normalizeProfileVisibilityValue(filters.profileVisibility);
        const requestedVerificationLevel = normalizeVerificationLevelValue(filters.verificationLevel);
        const requestedHasChildren = profileExtensionService.normalizeHasChildren(filters.hasChildren);
        const requestedResidentialStatus = profileExtensionService.normalizeResidentialStatus(filters.residentialStatus);
        const requestedDistrict = String(filters.district || '').trim().toLowerCase();

        const mapped = users.map((candidate) => {
            const profile = candidate.profile;
            if (!profile || trustService.isStale(candidate)) return null;
            if (!hardPreferencePasses(viewer.partnerPreference, profile)) return null;

            const privacy = privacyMap.get(candidate.id) || privacyService.DEFAULT_PRIVACY_SETTINGS;
            if (!privacyService.canViewProfile({ settings: privacy, isOwner: false, viewerIsPremium, viewerIsVerified })) return null;
            if (requestedPhotoVisibility && privacy.photoVisibility !== requestedPhotoVisibility) return null;
            if (requestedProfileVisibility && privacy.profileVisibility !== requestedProfileVisibility) return null;
            if (!passesVerificationLevel(candidate, requestedVerificationLevel)) return null;

            const extension = extensionMap.get(candidate.id) || profileExtensionService.DEFAULT_PROFILE_EXTENSION;
            if (requestedHasChildren && extension.hasChildren !== requestedHasChildren) return null;
            if (requestedResidentialStatus && extension.residentialStatus !== requestedResidentialStatus) return null;
            if (requestedDistrict && !String(extension.district || '').toLowerCase().includes(requestedDistrict)) return null;

            const canViewPhotos = privacyService.canViewPhotos({
                settings: privacy,
                isOwner: false,
                isMutualMatch: false,
                hasExplicitPhotoAccess: photoGrantIds.has(candidate.id),
            });
            if (boolValue(filters.withPhotoOnly) && (!candidate.photos.length || !canViewPhotos)) return null;

            const verification = trustService.verificationSummary(candidate);
            const profileCompleteness = trustService.calculateCompleteness({
                profile,
                photos: candidate.photos,
                preferences: candidate.partnerPreference,
                verification,
            });
            const activity = trustService.activityBucket(candidate);
            const compatibility = preferenceCompatibility(viewer.profile, viewer.partnerPreference, profile);
            const reasons = [...compatibility.reasons];
            if (verification.identity === 'verified') reasons.push('Identity verification completed');
            else if (verification.photo === 'verified') reasons.push('Photo verified');
            if (['active_today', 'active_this_week'].includes(activity.code)) reasons.push(activity.label);
            const uniqueReasons = Array.from(new Set(reasons)).slice(0, 4);

            const rankingScore = (compatibility.score ?? 40) + freshnessPoints(activity) + Math.round(profileCompleteness / 20) + (verification.identity === 'verified' ? 5 : 0);
            const photo = canViewPhotos
                ? (candidate.photos[0]?.thumbnailUrl || candidate.photos[0]?.photoUrl || null)
                : null;

            return {
                id: candidate.id,
                userId: candidate.id,
                firstName: profile.firstName,
                lastName: profile.lastName,
                age: getAge(profile.dateOfBirth),
                city: profile.city,
                state: profile.state,
                country: profile.country,
                district: extension.district,
                profession: profile.profession,
                education: profile.educationLevel,
                religion: profile.religion,
                caste: profile.caste,
                motherTongue: profile.motherTongue,
                maritalStatus: profile.maritalStatus,
                income: profile.incomeBand,
                heightCm: profile.heightCm,
                hasChildren: extension.hasChildren,
                residentialStatus: extension.residentialStatus,
                photo,
                photoLocked: !canViewPhotos,
                isVerified: candidate.isVerified,
                isPremium: candidate.subscriptions.length > 0,
                compatibilityScore: compatibility.score,
                compatibilityStrength: compatibility.strength,
                match: compatibility.score,
                reasons: uniqueReasons,
                whyRecommended: uniqueReasons,
                verification,
                activity,
                lastActiveAt: privacy.showLastSeen ? trustService.latestActivity(candidate) : null,
                managedBy: profile.role || 'self',
                managerRelationship: profile.managerRelationship,
                profileCompleteness,
                statusReconfirmedAt: candidate.statusReconfirmedAt,
                searchStatus: candidate.searchStatus,
                _rankingScore: rankingScore,
            };
        }).filter(Boolean);

        const sorted = mapped.sort((a, b) => {
            if (sort === 'newest' || sort === 'activity') {
                const rank = { active_today: 4, active_this_week: 3, active_recently: 2, low_activity: 1 };
                return (rank[b.activity.code] || 0) - (rank[a.activity.code] || 0) || b._rankingScore - a._rankingScore;
            }
            return b._rankingScore - a._rankingScore;
        });

        const start = (page - 1) * limit;
        const pageItems = (includeMeta ? sorted.slice(start, start + limit) : sorted.slice(0, 50))
            .map(({ _rankingScore, ...item }) => item);

        await exposureService.recordPage(userId, pageItems.map((item) => item.userId));
        await trustService.recordActivity(userId);

        if (!includeMeta) return pageItems;
        return {
            items: pageItems,
            page,
            limit,
            total: sorted.length,
            hasNextPage: start + limit < sorted.length,
            sort,
            transparency: {
                hardFiltersNeverAutoRelaxed: true,
                paidPlanChangesEligibility: false,
                repetitionCooldownEnabled: true,
                staleProfilesSuppressed: true,
            },
        };
    },
};

module.exports = matchingService;
