const prisma = require('../config/prisma');

const PLAN_ENTITLEMENTS = {
    free: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: false,
        canVoiceCall: false,
        canVideoCall: false,
        dailyRecommendations: 10,
        visibilityBoost: false,
    },
    silver: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: true,
        canVoiceCall: false,
        canVideoCall: false,
        dailyRecommendations: 40,
        visibilityBoost: true,
    },
    gold: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: true,
        canVoiceCall: true,
        canVideoCall: false,
        dailyRecommendations: 80,
        visibilityBoost: true,
    },
    platinum: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: true,
        canVoiceCall: true,
        canVideoCall: true,
        dailyRecommendations: 150,
        visibilityBoost: true,
    },
    till_marriage: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: true,
        canVoiceCall: true,
        canVideoCall: true,
        dailyRecommendations: 200,
        visibilityBoost: true,
    },
};

function normalizePlan(plan) {
    const raw = String(plan || 'free').trim().toLowerCase();
    if (raw.includes('platinum')) return 'platinum';
    if (raw.includes('gold')) return 'gold';
    if (raw.includes('silver')) return 'silver';
    if (raw.includes('till')) return 'till_marriage';
    if (raw.includes('free')) return 'free';
    return raw;
}

const subscriptionService = {
    normalizePlan,

    async getActiveSubscription(userId) {
        return prisma.subscription.findFirst({
            where: {
                userId,
                status: 'active',
                OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }],
            },
            orderBy: { createdAt: 'desc' },
        });
    },

    async getEntitlements(userId) {
        const active = await this.getActiveSubscription(userId);
        const normalized = normalizePlan(active?.plan || 'free');
        const entitlements = PLAN_ENTITLEMENTS[normalized] || PLAN_ENTITLEMENTS.free;

        return {
            plan: normalized,
            status: active?.status || 'active',
            startedAt: active?.startedAt || null,
            expiresAt: active?.expiresAt || null,
            entitlements,
        };
    },
};

module.exports = subscriptionService;
