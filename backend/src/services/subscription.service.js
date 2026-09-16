const prisma = require('../config/prisma');

const PLAN_VERSION = 2;

const PLAN_ENTITLEMENTS = {
    free: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: false,
        canVoiceCall: false,
        canVideoCall: false,
        dailyRecommendations: 10,
        interestsPerDay: 5,
        contactRequestsPerMonth: 2,
        messagesWithConnections: 'unlimited',
        visibilityBoostsPerWeek: 0,
    },
    silver: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: true,
        canVoiceCall: false,
        canVideoCall: false,
        dailyRecommendations: 40,
        interestsPerDay: 20,
        contactRequestsPerMonth: 10,
        messagesWithConnections: 'unlimited',
        visibilityBoostsPerWeek: 1,
    },
    gold: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: true,
        canVoiceCall: true,
        canVideoCall: false,
        dailyRecommendations: 80,
        interestsPerDay: 50,
        contactRequestsPerMonth: 30,
        messagesWithConnections: 'unlimited',
        visibilityBoostsPerWeek: 2,
    },
    platinum: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: true,
        canVoiceCall: true,
        canVideoCall: true,
        dailyRecommendations: 150,
        interestsPerDay: 100,
        contactRequestsPerMonth: 60,
        messagesWithConnections: 'unlimited',
        visibilityBoostsPerWeek: 4,
    },
    till_marriage: {
        canSendInterests: true,
        canUseAdvancedSearch: true,
        canViewWhoViewed: true,
        canVoiceCall: true,
        canVideoCall: true,
        dailyRecommendations: 200,
        interestsPerDay: 100,
        contactRequestsPerMonth: 100,
        messagesWithConnections: 'unlimited',
        visibilityBoostsPerWeek: 4,
    },
};

function normalizePlan(plan) {
    const raw = String(plan || 'free').trim().toLowerCase();
    if (raw.includes('platinum')) return 'platinum';
    if (raw.includes('gold')) return 'gold';
    if (raw.includes('silver')) return 'silver';
    if (raw.includes('till')) return 'till_marriage';
    if (raw.includes('free')) return 'free';
    return PLAN_ENTITLEMENTS[raw] ? raw : 'free';
}

const subscriptionService = {
    PLAN_VERSION,
    PLAN_ENTITLEMENTS,
    normalizePlan,

    async getActiveSubscription(userId) {
        return prisma.subscription.findFirst({
            where: {
                userId,
                status: { in: ['active', 'grace'] },
                OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }],
            },
            orderBy: { createdAt: 'desc' },
        });
    },

    async getEntitlements(userId) {
        const active = await this.getActiveSubscription(userId);
        const normalized = normalizePlan(active?.plan || 'free');
        return {
            plan: normalized,
            planVersion: active?.planVersion || PLAN_VERSION,
            status: active?.status || 'free',
            startedAt: active?.startedAt || null,
            expiresAt: active?.expiresAt || null,
            autoRenew: Boolean(active?.autoRenew),
            renewalStatus: active?.renewalStatus || 'none',
            entitlements: { ...PLAN_ENTITLEMENTS[normalized] },
        };
    },
};

module.exports = subscriptionService;
