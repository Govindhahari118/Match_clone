const crypto = require('crypto');
const prisma = require('../config/prisma');

const EVENT_TAXONOMY = {
    auth: ['auth_login_success', 'auth_login_failure', 'auth_signup_started', 'auth_signup_completed'],
    search: ['search_executed', 'search_zero_results', 'search_saved', 'search_rail_opened'],
    matches: ['match_impression', 'match_opened', 'match_feedback'],
    onboarding: ['onboarding_step_completed', 'onboarding_resumed', 'onboarding_completed'],
    chat: ['chat_message_sent', 'chat_message_delivered', 'chat_message_seen'],
    subscription: ['checkout_started', 'checkout_completed', 'subscription_renewed', 'subscription_cancelled'],
    trust_safety: ['report_submitted', 'moderation_case_updated', 'safety_state_changed'],
};

const DEFAULT_FEATURE_FLAGS = [
    {
        key: 'ranking_quality_v2',
        description: 'Recommendation ranking algorithm variant',
        rolloutPercent: Number.parseInt(process.env.FLAG_RANKING_QUALITY_V2 || '50', 10),
    },
    {
        key: 'onboarding_adaptive_flow',
        description: 'Adaptive onboarding step sequencing',
        rolloutPercent: Number.parseInt(process.env.FLAG_ONBOARDING_ADAPTIVE_FLOW || '30', 10),
    },
    {
        key: 'checkout_v2',
        description: 'Optimized checkout with personalized prompts',
        rolloutPercent: Number.parseInt(process.env.FLAG_CHECKOUT_V2 || '40', 10),
    },
];

function hashToBucket(value) {
    const digest = crypto.createHash('sha1').update(String(value)).digest('hex');
    return Number.parseInt(digest.slice(0, 8), 16) % 100;
}

function normalizeEventName(name) {
    return String(name || '').trim().toLowerCase();
}

function isEventAllowed(name) {
    const event = normalizeEventName(name);
    return Object.values(EVENT_TAXONOMY).some((items) => items.includes(event));
}

function getEventOwner(name) {
    const normalized = normalizeEventName(name);
    for (const [domain, events] of Object.entries(EVENT_TAXONOMY)) {
        if (events.includes(normalized)) return domain;
    }
    return null;
}

function sanitizeProperties(input = {}) {
    if (!input || typeof input !== 'object' || Array.isArray(input)) return {};
    const entries = Object.entries(input).slice(0, 40);
    const out = {};
    for (const [key, value] of entries) {
        const normalizedKey = String(key || '').trim().slice(0, 80);
        if (!normalizedKey) continue;
        if (value === null || value === undefined) continue;
        if (typeof value === 'string') {
            out[normalizedKey] = value.slice(0, 400);
            continue;
        }
        if (typeof value === 'number' || typeof value === 'boolean') {
            out[normalizedKey] = value;
            continue;
        }
        out[normalizedKey] = JSON.stringify(value).slice(0, 400);
    }
    return out;
}

function normalizeRollout(value) {
    const parsed = Number.parseInt(value || '0', 10);
    if (Number.isNaN(parsed)) return 0;
    return Math.min(100, Math.max(0, parsed));
}

const analyticsService = {
    getTaxonomy() {
        return EVENT_TAXONOMY;
    },

    getFlagsForUser(userId) {
        return DEFAULT_FEATURE_FLAGS.map((flag) => {
            const rollout = normalizeRollout(flag.rolloutPercent);
            const bucket = hashToBucket(`${userId}:${flag.key}`);
            return {
                key: flag.key,
                description: flag.description,
                enabled: bucket < rollout,
                rolloutPercent: rollout,
                bucket,
            };
        });
    },

    async trackEvent(userId, payload = {}, context = {}) {
        const eventName = normalizeEventName(payload.eventName || payload.event || '');
        if (!eventName) {
            return { error: 'eventName is required', statusCode: 400 };
        }
        if (!isEventAllowed(eventName)) {
            return { error: 'eventName is not part of the approved taxonomy', statusCode: 400 };
        }

        const properties = sanitizeProperties(payload.properties || payload.props || {});
        const owner = getEventOwner(eventName);

        const record = await prisma.auditLog.create({
            data: {
                userId,
                action: 'analytics_event',
                resourceType: 'event',
                resourceId: eventName,
                changes: {
                    owner,
                    properties,
                    eventAt: payload.eventAt || new Date().toISOString(),
                    schemaVersion: 'events-v1',
                },
                ipAddress: context.ipAddress || null,
                userAgent: context.userAgent || null,
            },
        });

        return {
            success: true,
            id: record.id,
            eventName,
            owner,
        };
    },

    async logDecision(userId, payload = {}) {
        const title = String(payload.title || '').trim();
        if (!title) {
            return { error: 'title is required', statusCode: 400 };
        }

        const decision = await prisma.auditLog.create({
            data: {
                userId,
                action: 'product_decision_log',
                resourceType: 'decision',
                resourceId: String(payload.experimentId || '').trim() || null,
                changes: {
                    title: title.slice(0, 180),
                    experimentId: payload.experimentId || null,
                    outcome: payload.outcome || null,
                    followUp: payload.followUp || null,
                    owner: payload.owner || null,
                    loggedAt: new Date().toISOString(),
                },
            },
        });

        return {
            success: true,
            decisionId: decision.id,
        };
    },

    async logFlagExposure(userId, payload = {}) {
        const flagKey = String(payload.flagKey || '').trim();
        if (!flagKey) {
            return { error: 'flagKey is required', statusCode: 400 };
        }
        const enabled = Boolean(payload.enabled);
        const experimentId = payload.experimentId ? String(payload.experimentId).trim() : null;

        const log = await prisma.auditLog.create({
            data: {
                userId,
                action: 'feature_flag_exposure',
                resourceType: 'feature_flag',
                resourceId: flagKey,
                changes: {
                    enabled,
                    experimentId,
                    exposedAt: new Date().toISOString(),
                },
            },
        });

        return {
            success: true,
            exposureId: log.id,
            flagKey,
            enabled,
        };
    },
};

module.exports = analyticsService;
