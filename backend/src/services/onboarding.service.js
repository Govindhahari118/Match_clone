const prisma = require('../config/prisma');

const COMPLETENESS_WEIGHTS = {
    basic_identity: 35,
    location: 15,
    lifestyle: 15,
    media: 20,
    preferences: 15,
};

function pct(value, total) {
    if (!total) return 0;
    return Math.round((value / total) * 100);
}

function scoreSection(completedFields, totalFields, weight) {
    if (!totalFields || !weight) return 0;
    return (completedFields / totalFields) * weight;
}

function countPresent(values = []) {
    return values.filter((value) => {
        if (Array.isArray(value)) return value.length > 0;
        if (value === null || value === undefined) return false;
        if (typeof value === 'string') return value.trim().length > 0;
        return Boolean(value);
    }).length;
}

function computePrompts({ profile, photosCount, preferences, user }) {
    const prompts = [];
    if (!profile?.bio) prompts.push('Add a short bio to improve trust and match quality.');
    if (!photosCount) prompts.push('Upload at least one clear profile photo.');
    if (!profile?.profession) prompts.push('Add profession details for better discovery ranking.');
    if (!profile?.educationLevel) prompts.push('Add education details to improve compatibility scoring.');
    if (!preferences) prompts.push('Set partner preferences to get better recommendations.');
    if (!user?.isVerified) prompts.push('Verify your profile to increase response rate.');
    if (!profile?.city || !profile?.state) prompts.push('Complete location details for better local matches.');
    return prompts.slice(0, 5);
}

function normalizeResumePayload(payload = {}) {
    const step = Math.min(Math.max(Number.parseInt(payload.step || '1', 10) || 1, 1), 8);
    return {
        step,
        route: String(payload.route || '').trim() || null,
        draft: payload.draft && typeof payload.draft === 'object' ? payload.draft : {},
        updatedAt: new Date().toISOString(),
    };
}

const onboardingService = {
    computeCompletenessSnapshot({ user, profile, photosCount, preferences }) {
        const basicFields = [profile?.firstName, profile?.dateOfBirth, profile?.gender, profile?.maritalStatus, user?.phone || user?.email];
        const locationFields = [profile?.country, profile?.state, profile?.city];
        const lifestyleFields = [profile?.foodHabit, profile?.smokes, profile?.drinks, profile?.religiousness, profile?.hobbies];
        const mediaFields = [photosCount > 0 ? 'photo' : null, profile?.videoUrl];
        const preferenceFields = [preferences?.minAge, preferences?.maxAge, preferences?.preferredLocations, preferences?.preferredReligions];

        const sectionScores = {
            basic_identity: scoreSection(countPresent(basicFields), basicFields.length, COMPLETENESS_WEIGHTS.basic_identity),
            location: scoreSection(countPresent(locationFields), locationFields.length, COMPLETENESS_WEIGHTS.location),
            lifestyle: scoreSection(countPresent(lifestyleFields), lifestyleFields.length, COMPLETENESS_WEIGHTS.lifestyle),
            media: scoreSection(countPresent(mediaFields), mediaFields.length, COMPLETENESS_WEIGHTS.media),
            preferences: scoreSection(countPresent(preferenceFields), preferenceFields.length, COMPLETENESS_WEIGHTS.preferences),
        };

        const weightedScore = Object.values(sectionScores).reduce((sum, value) => sum + value, 0);
        const score = Math.max(0, Math.min(100, Math.round(weightedScore)));
        const prompts = computePrompts({ profile, photosCount, preferences, user });
        const completionBand = score >= 80 ? 'high' : (score >= 50 ? 'medium' : 'low');

        return {
            score,
            completionBand,
            sectionBreakdown: Object.entries(sectionScores).map(([key, value]) => ({
                section: key,
                score: Math.round(value),
                weight: COMPLETENESS_WEIGHTS[key],
                progressPercent: pct(value, COMPLETENESS_WEIGHTS[key]),
            })),
            prompts,
        };
    },

    async refreshCompleteness(userId) {
        const [user, profile, photosCount, preferences] = await Promise.all([
            prisma.user.findUnique({
                where: { id: userId },
                select: {
                    id: true,
                    phone: true,
                    email: true,
                    isVerified: true,
                },
            }),
            prisma.profile.findUnique({
                where: { userId },
            }),
            prisma.photo.count({
                where: { userId },
            }),
            prisma.partnerPreference.findUnique({
                where: { userId },
            }),
        ]);

        if (!user || !profile) {
            return {
                score: 0,
                completionBand: 'low',
                sectionBreakdown: [],
                prompts: ['Complete your profile to unlock better recommendations.'],
            };
        }

        const snapshot = this.computeCompletenessSnapshot({
            user,
            profile,
            photosCount,
            preferences,
        });

        await prisma.profile.update({
            where: { userId },
            data: {
                completionPercentage: snapshot.score,
            },
        });

        await prisma.auditLog.create({
            data: {
                userId,
                action: 'onboarding_completeness_refreshed',
                resourceType: 'profile',
                resourceId: userId,
                changes: {
                    score: snapshot.score,
                    completionBand: snapshot.completionBand,
                },
            },
        });

        return snapshot;
    },

    async saveResumeState(userId, payload = {}) {
        const normalized = normalizeResumePayload(payload);
        const previous = await this.getResumeState(userId);
        const previousSerialized = JSON.stringify(previous);
        const normalizedSerialized = JSON.stringify(normalized);
        if (previousSerialized === normalizedSerialized) {
            return normalized;
        }
        await prisma.auditLog.create({
            data: {
                userId,
                action: 'onboarding_resume_state',
                resourceType: 'onboarding',
                resourceId: userId,
                changes: normalized,
            },
        });
        return normalized;
    },

    async getResumeState(userId) {
        const last = await prisma.auditLog.findFirst({
            where: {
                userId,
                action: 'onboarding_resume_state',
                resourceType: 'onboarding',
                resourceId: userId,
            },
            orderBy: { createdAt: 'desc' },
        });

        if (!last || !last.changes || typeof last.changes !== 'object') {
            return {
                step: 1,
                route: null,
                draft: {},
            };
        }

        return normalizeResumePayload(last.changes);
    },

    async getProgress(userId) {
        const [completeness, resumeState] = await Promise.all([
            this.refreshCompleteness(userId),
            this.getResumeState(userId),
        ]);

        return {
            completeness,
            resumeState,
        };
    },
};

module.exports = onboardingService;
