const crypto = require('crypto');
const prisma = require('../config/prisma');

const FEATURE_SCHEMA_VERSION = 'v2.0.0';
const CACHE_TTL_MS = Number.parseInt(process.env.RECOMMENDATION_CACHE_TTL_MS || '300000', 10);
const CACHE_MAX_ENTRIES = Number.parseInt(process.env.RECOMMENDATION_CACHE_MAX_ENTRIES || '400', 10);
const EXPERIMENT_SPLIT = Math.min(
    Math.max(Number.parseInt(process.env.RANKING_EXPERIMENT_SPLIT || '50', 10), 0),
    100,
);

const FEEDBACK_ACTIONS = new Set(['like', 'skip', 'hide', 'report', 'open']);
const SCHEMA_KEYS = ['demographic', 'profile_quality', 'preference_fit', 'activity', 'interaction_history', 'session_intent'];

const recommendationCache = new Map();

function hashString(value) {
    return crypto.createHash('sha1').update(String(value)).digest('hex');
}

function normalize(value) {
    return String(value || '')
        .trim()
        .toLowerCase();
}

function normalizeAge(dateOfBirth) {
    if (!dateOfBirth) return null;
    const now = new Date();
    const dob = new Date(dateOfBirth);
    if (Number.isNaN(dob.getTime())) return null;

    let age = now.getFullYear() - dob.getFullYear();
    const monthDelta = now.getMonth() - dob.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < dob.getDate())) {
        age -= 1;
    }
    return age;
}

function stableSerialize(input) {
    if (Array.isArray(input)) {
        return `[${input.map((item) => stableSerialize(item)).join(',')}]`;
    }
    if (input && typeof input === 'object') {
        const entries = Object.entries(input)
            .filter(([, value]) => value !== undefined)
            .sort(([a], [b]) => a.localeCompare(b));
        return `{${entries.map(([key, value]) => `${JSON.stringify(key)}:${stableSerialize(value)}`).join(',')}}`;
    }
    return JSON.stringify(input);
}

function getVariantForUser(userId) {
    const hash = hashString(userId).slice(0, 6);
    const bucket = Number.parseInt(hash, 16) % 100;
    return bucket < EXPERIMENT_SPLIT ? 'quality_v2' : 'control_v1';
}

function qualityScoreBoost({ candidateUser, candidateProfile }) {
    return (
        (candidateUser.isVerified ? 10 : 0) +
        (candidateProfile.educationLevel ? 5 : 0) +
        (candidateProfile.profession ? 4 : 0) +
        (candidateProfile.bio ? 3 : 0) +
        (candidateProfile.completionPercentage ? Math.min(8, Math.floor(candidateProfile.completionPercentage / 15)) : 0)
    );
}

function deterministicAffinitySeed(viewerId, candidateUserId, context = '') {
    const hash = hashString(`${viewerId}|${candidateUserId}|${context}`).slice(0, 8);
    return Number.parseInt(hash, 16) % 100;
}

function preferenceFitScore(viewerPreference, candidateProfile) {
    if (!viewerPreference || !candidateProfile) return 0;
    let score = 0;

    const candidateAge = normalizeAge(candidateProfile.dateOfBirth);
    if (candidateAge && candidateAge >= viewerPreference.minAge && candidateAge <= viewerPreference.maxAge) {
        score += 8;
    }

    if (viewerPreference.preferredReligions?.length > 0 && viewerPreference.preferredReligions.includes(candidateProfile.religion)) {
        score += 6;
    } else if (viewerPreference.religionOpen) {
        score += 2;
    }

    if (viewerPreference.preferredLocations?.length > 0) {
        const matchesLocation = viewerPreference.preferredLocations.some((location) => {
            const normalized = normalize(location);
            return normalized && (
                normalized === normalize(candidateProfile.city) ||
                normalized === normalize(candidateProfile.state)
            );
        });
        if (matchesLocation) score += 5;
    }

    if (viewerPreference.foodHabitPreferences?.length > 0 && candidateProfile.foodHabit) {
        if (viewerPreference.foodHabitPreferences.includes(candidateProfile.foodHabit)) score += 3;
    }

    return score;
}

function coldStartScore(viewerProfile, candidateProfile) {
    if (!viewerProfile || !candidateProfile) return 0;
    let score = 0;
    if (viewerProfile.city && normalize(viewerProfile.city) === normalize(candidateProfile.city)) score += 4;
    if (viewerProfile.state && normalize(viewerProfile.state) === normalize(candidateProfile.state)) score += 3;
    if (viewerProfile.religion && normalize(viewerProfile.religion) === normalize(candidateProfile.religion)) score += 5;
    if (viewerProfile.intent && viewerProfile.intent === candidateProfile.intent) score += 3;
    return score;
}

function activityScore(candidateUser) {
    if (!candidateUser?.lastLogin) return 0;
    const ageMs = Date.now() - new Date(candidateUser.lastLogin).getTime();
    if (ageMs <= 60 * 60 * 1000) return 8;
    if (ageMs <= 24 * 60 * 60 * 1000) return 5;
    if (ageMs <= 7 * 24 * 60 * 60 * 1000) return 2;
    return 0;
}

function interactionPenalty(interactionSummary = {}, candidateUserId) {
    if (!candidateUserId) return 0;
    if (interactionSummary.hiddenIds?.has(candidateUserId)) return -25;
    if (interactionSummary.reportedIds?.has(candidateUserId)) return -35;
    if (interactionSummary.skippedIds?.has(candidateUserId)) return -10;
    return 0;
}

function sessionIntentBoost(sessionSignals = {}, candidateProfile) {
    if (!sessionSignals || !candidateProfile) return 0;
    let boost = 0;

    if (sessionSignals.preferredProfession && candidateProfile.profession) {
        if (normalize(candidateProfile.profession).includes(normalize(sessionSignals.preferredProfession))) boost += 3;
    }
    if (sessionSignals.preferredCity && candidateProfile.city) {
        if (normalize(candidateProfile.city).includes(normalize(sessionSignals.preferredCity))) boost += 3;
    }
    if (sessionSignals.searchEdits > 2) {
        boost += 1;
    }
    if (sessionSignals.profileOpens > 3) {
        boost += 1;
    }

    return boost;
}

function clampScore(raw) {
    return Math.max(0, Math.min(100, Math.round(raw)));
}

function buildReasonTags({
    candidateUser,
    candidateProfile,
    viewerProfile,
    viewerPreference,
    isColdStart,
}) {
    const tags = [];
    if (candidateUser.isVerified) tags.push('verified_profile');
    if (candidateProfile.completionPercentage >= 80) tags.push('high_profile_quality');
    if (candidateUser.lastLogin && Date.now() - new Date(candidateUser.lastLogin).getTime() <= 24 * 60 * 60 * 1000) {
        tags.push('recently_active');
    }
    if (viewerProfile?.city && candidateProfile.city && normalize(viewerProfile.city) === normalize(candidateProfile.city)) {
        tags.push('same_city');
    }
    if (viewerProfile?.religion && candidateProfile.religion && normalize(viewerProfile.religion) === normalize(candidateProfile.religion)) {
        tags.push('shared_religion');
    }
    if (viewerPreference?.preferredLocations?.length && candidateProfile.city) {
        const match = viewerPreference.preferredLocations.some(
            (loc) => normalize(loc) === normalize(candidateProfile.city) || normalize(loc) === normalize(candidateProfile.state),
        );
        if (match) tags.push('preferred_location_match');
    }
    if (isColdStart) tags.push('cold_start_personalization');
    return tags.slice(0, 5);
}

function reasonTagLabels(tags = []) {
    const labels = {
        verified_profile: 'Verified profile',
        high_profile_quality: 'High profile quality',
        recently_active: 'Recently active',
        same_city: 'Same city',
        shared_religion: 'Shared religion',
        preferred_location_match: 'Matches preferred location',
        cold_start_personalization: 'Recommended from onboarding preferences',
    };
    return tags.map((tag) => labels[tag]).filter(Boolean).slice(0, 3);
}

function buildCacheKey({ userId, filters, options }) {
    return hashString(`${userId}|${stableSerialize(filters)}|${stableSerialize(options || {})}`);
}

function getCacheRecord(key) {
    const entry = recommendationCache.get(key);
    if (!entry) return null;
    if (entry.expiresAt <= Date.now()) {
        recommendationCache.delete(key);
        return null;
    }
    return entry.value;
}

function setCacheRecord(key, value) {
    recommendationCache.set(key, {
        expiresAt: Date.now() + CACHE_TTL_MS,
        value,
    });

    if (recommendationCache.size <= CACHE_MAX_ENTRIES) return;
    const entries = Array.from(recommendationCache.entries());
    entries
        .sort((a, b) => a[1].expiresAt - b[1].expiresAt)
        .slice(0, Math.max(1, Math.floor(CACHE_MAX_ENTRIES * 0.1)))
        .forEach(([staleKey]) => recommendationCache.delete(staleKey));
}

function dedupeAndSortByScore(candidates = []) {
    const seen = new Set();
    const filtered = [];
    for (const item of candidates) {
        if (!item || !item.userId || seen.has(item.userId)) continue;
        seen.add(item.userId);
        filtered.push(item);
    }
    return filtered.sort((a, b) => (b.match || 0) - (a.match || 0));
}

function applyDiversityRerank(candidates = [], options = {}) {
    const maxAttributeShare = Number.parseFloat(options.maxAttributeShare || '0.45');
    const minFloor = Number.parseInt(options.minimumPerAttribute || '2', 10);
    const sorted = dedupeAndSortByScore(candidates);
    if (sorted.length <= 3) return sorted;

    const selected = [];
    const overflow = [];
    const religionCounts = new Map();
    const cityCounts = new Map();
    const maxPerAttribute = Math.max(minFloor, Math.ceil(sorted.length * maxAttributeShare));

    function canTake(bucket, key) {
        if (!key) return true;
        return (bucket.get(key) || 0) < maxPerAttribute;
    }

    function increment(bucket, key) {
        if (!key) return;
        bucket.set(key, (bucket.get(key) || 0) + 1);
    }

    for (const candidate of sorted) {
        const religion = normalize(candidate.religion);
        const city = normalize(candidate.city);
        if (canTake(religionCounts, religion) && canTake(cityCounts, city)) {
            selected.push(candidate);
            increment(religionCounts, religion);
            increment(cityCounts, city);
        } else {
            overflow.push(candidate);
        }
    }

    return selected.concat(overflow);
}

function parseSessionSignals(filters = {}) {
    return {
        preferredProfession: String(filters.sessionPreferredProfession || '').trim(),
        preferredCity: String(filters.sessionPreferredCity || '').trim(),
        searchEdits: Number.parseInt(filters.sessionSearchEdits || '0', 10) || 0,
        profileOpens: Number.parseInt(filters.sessionProfileOpens || '0', 10) || 0,
    };
}

function shouldSampleForMonitoring(userId) {
    const bucket = Number.parseInt(hashString(userId).slice(0, 2), 16) % 100;
    return bucket < 5;
}

const recommendationService = {
    FEATURE_SCHEMA_VERSION,
    FEEDBACK_ACTIONS,
    parseSessionSignals,
    reasonTagLabels,
    applyDiversityRerank,
    getVariantForUser,
    buildCacheKey,

    getCachedRecommendations(cacheKey) {
        return getCacheRecord(cacheKey);
    },

    cacheRecommendations(cacheKey, value) {
        setCacheRecord(cacheKey, value);
    },

    async getManualOverrides(userId, candidateIds = []) {
        if (!userId || candidateIds.length === 0) return new Map();
        const logs = await prisma.auditLog.findMany({
            where: {
                userId,
                action: 'recommendation_override',
                resourceType: 'match_candidate',
                resourceId: { in: candidateIds },
            },
            orderBy: { createdAt: 'desc' },
            take: Math.min(candidateIds.length * 2, 200),
        });

        const overrideMap = new Map();
        for (const log of logs) {
            if (overrideMap.has(log.resourceId)) continue;
            const changes = log.changes && typeof log.changes === 'object' ? log.changes : {};
            const delta = Number(changes.scoreDelta || 0);
            if (delta !== 0) {
                overrideMap.set(log.resourceId, Math.max(-30, Math.min(30, delta)));
            }
        }
        return overrideMap;
    },

    scoreCandidate({
        viewerId,
        variant,
        viewerProfile,
        viewerPreference,
        candidateUser,
        candidateProfile,
        interactionSummary,
        sessionSignals,
        isColdStart,
        manualScoreDelta = 0,
    }) {
        const seedContext = `${candidateProfile.religion || ''}|${candidateProfile.city || ''}|${candidateProfile.profession || ''}`;
        const affinity = deterministicAffinitySeed(viewerId, candidateUser.id, seedContext);
        const profileQuality = qualityScoreBoost({ candidateUser, candidateProfile });
        const preferenceFit = preferenceFitScore(viewerPreference, candidateProfile);
        const activity = activityScore(candidateUser);
        const historyPenalty = interactionPenalty(interactionSummary, candidateUser.id);
        const coldStart = isColdStart ? coldStartScore(viewerProfile, candidateProfile) : 0;
        const sessionBoost = sessionIntentBoost(sessionSignals, candidateProfile);

        const variantBase = variant === 'quality_v2' ? 56 : 52;
        const affinityWeight = variant === 'quality_v2' ? 0.24 : 0.18;
        const raw =
            variantBase +
            (affinity * affinityWeight) +
            profileQuality +
            preferenceFit +
            activity +
            historyPenalty +
            coldStart +
            sessionBoost +
            manualScoreDelta;

        const reasonTags = buildReasonTags({
            candidateUser,
            candidateProfile,
            viewerProfile,
            viewerPreference,
            isColdStart,
        });

        const featureVector = {
            schemaVersion: FEATURE_SCHEMA_VERSION,
            keys: SCHEMA_KEYS,
            demographic: {
                candidateAge: normalizeAge(candidateProfile.dateOfBirth),
                city: candidateProfile.city || null,
                religion: candidateProfile.religion || null,
            },
            profile_quality: {
                completionPercentage: candidateProfile.completionPercentage || 0,
                isVerified: Boolean(candidateUser.isVerified),
                hasProfession: Boolean(candidateProfile.profession),
                hasEducation: Boolean(candidateProfile.educationLevel),
            },
            preference_fit: {
                score: preferenceFit,
            },
            activity: {
                lastLogin: candidateUser.lastLogin ? new Date(candidateUser.lastLogin).toISOString() : null,
                score: activity,
            },
            interaction_history: {
                penalty: historyPenalty,
            },
            session_intent: {
                score: sessionBoost,
            },
        };

        return {
            score: clampScore(raw),
            reasonTags,
            featureVector,
            components: {
                affinity,
                profileQuality,
                preferenceFit,
                activity,
                historyPenalty,
                coldStart,
                sessionBoost,
                manualScoreDelta,
            },
        };
    },

    async recordFeedback({
        userId,
        targetUserId,
        action,
        metadata = {},
        ipAddress = null,
        userAgent = null,
    }) {
        const normalizedAction = normalize(action);
        if (!FEEDBACK_ACTIONS.has(normalizedAction)) {
            throw new Error('Invalid feedback action');
        }

        return prisma.auditLog.create({
            data: {
                userId,
                action: 'recommendation_feedback',
                resourceType: 'match_candidate',
                resourceId: targetUserId,
                changes: {
                    schemaVersion: FEATURE_SCHEMA_VERSION,
                    feedbackAction: normalizedAction,
                    metadata,
                },
                ipAddress,
                userAgent,
            },
        });
    },

    async getInteractionSummary(viewerId, candidateIds = []) {
        if (!viewerId || candidateIds.length === 0) {
            return {
                skippedIds: new Set(),
                hiddenIds: new Set(),
                reportedIds: new Set(),
            };
        }

        const feedbackLogs = await prisma.auditLog.findMany({
            where: {
                userId: viewerId,
                action: 'recommendation_feedback',
                resourceType: 'match_candidate',
                resourceId: { in: candidateIds },
            },
            orderBy: { createdAt: 'desc' },
            take: Math.min(candidateIds.length * 6, 600),
        });

        const skippedIds = new Set();
        const hiddenIds = new Set();
        const reportedIds = new Set();
        for (const log of feedbackLogs) {
            const changes = log.changes && typeof log.changes === 'object' ? log.changes : {};
            const feedbackAction = normalize(changes.feedbackAction);
            if (feedbackAction === 'skip') skippedIds.add(log.resourceId);
            if (feedbackAction === 'hide') hiddenIds.add(log.resourceId);
            if (feedbackAction === 'report') reportedIds.add(log.resourceId);
        }

        return { skippedIds, hiddenIds, reportedIds };
    },

    async recordMonitoringSnapshot(userId, variant, candidates = []) {
        if (!userId || !shouldSampleForMonitoring(userId) || candidates.length === 0) return;
        const scores = candidates.map((item) => Number(item.match || 0)).filter((score) => !Number.isNaN(score));
        if (scores.length === 0) return;

        const min = Math.min(...scores);
        const max = Math.max(...scores);
        const avg = Math.round(scores.reduce((acc, value) => acc + value, 0) / scores.length);
        const byReligion = {};
        for (const item of candidates.slice(0, 30)) {
            const key = normalize(item.religion) || 'unknown';
            byReligion[key] = (byReligion[key] || 0) + 1;
        }

        await prisma.auditLog.create({
            data: {
                userId,
                action: 'recommendation_monitoring_snapshot',
                resourceType: 'recommendations',
                changes: {
                    schemaVersion: FEATURE_SCHEMA_VERSION,
                    variant,
                    count: candidates.length,
                    distribution: { min, max, avg },
                    topReligionMix: byReligion,
                },
            },
        });
    },
};

module.exports = recommendationService;
