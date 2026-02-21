const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const prisma = require('../config/prisma');
const subscriptionService = require('./subscription.service');
const privacyService = require('./privacy.service');

const CALL_TOKEN_TTL_SECONDS = 15 * 60;

function normalizeCallType(value) {
    const type = String(value || '').trim().toLowerCase();
    if (type === 'voice' || type === 'video') {
        return type;
    }
    return null;
}

function validateEntitlement(type, entitlements) {
    if (type === 'voice' && entitlements.canVoiceCall) {
        return { allowed: true };
    }
    if (type === 'video' && entitlements.canVideoCall) {
        return { allowed: true };
    }
    return { allowed: false };
}

async function findActiveMatchBetweenUsers(userAId, userBId, requestedMatchId) {
    return prisma.match.findFirst({
        where: {
            ...(requestedMatchId ? { id: requestedMatchId } : {}),
            isActive: true,
            OR: [
                { userAId, userBId },
                { userAId: userBId, userBId: userAId },
            ],
        },
        select: { id: true, userAId: true, userBId: true },
    });
}

const callService = {
    normalizeCallType,

    async createSession(callerId, payload = {}) {
        const type = normalizeCallType(payload.type);
        if (!type) {
            return { error: 'type must be either voice or video', statusCode: 400 };
        }

        const calleeId = String(payload.targetUserId || '').trim();
        if (!calleeId) {
            return { error: 'targetUserId is required', statusCode: 400 };
        }
        if (calleeId === callerId) {
            return { error: 'Cannot initiate call with yourself', statusCode: 400 };
        }

        const [callerEntitlement, callee, match] = await Promise.all([
            subscriptionService.getEntitlements(callerId),
            prisma.user.findUnique({
                where: { id: calleeId },
                select: { id: true, isActive: true, isBanned: true, isVerified: true },
            }),
            findActiveMatchBetweenUsers(callerId, calleeId, payload.matchId),
        ]);

        if (!callee || !callee.isActive || callee.isBanned) {
            return { error: 'Target user is unavailable for calls', statusCode: 404 };
        }

        const entitlementCheck = validateEntitlement(type, callerEntitlement.entitlements || {});
        if (!entitlementCheck.allowed) {
            return {
                error: `${type} call requires a higher subscription tier`,
                statusCode: 403,
                upgradeRequired: true,
                plan: callerEntitlement.plan,
                entitlements: callerEntitlement.entitlements,
            };
        }

        if (!match) {
            return { error: 'Calls are allowed only between active matches', statusCode: 403 };
        }

        const { settings: calleePrivacy } = await privacyService.getUserPrivacySettings(calleeId);
        if (!calleePrivacy.allowMessages) {
            return { error: 'This profile is not currently accepting direct calls', statusCode: 403 };
        }

        const sessionId = crypto.randomUUID();
        const issuedAt = new Date();
        const expiresAt = new Date(issuedAt.getTime() + (CALL_TOKEN_TTL_SECONDS * 1000));
        const channel = `call:${match.id}:${sessionId}`;

        const token = jwt.sign(
            {
                sub: callerId,
                peer: calleeId,
                matchId: match.id,
                sessionId,
                channel,
                type,
            },
            process.env.JWT_SECRET,
            { expiresIn: CALL_TOKEN_TTL_SECONDS },
        );

        await prisma.auditLog.create({
            data: {
                userId: callerId,
                action: 'call_session_created',
                resourceType: 'match',
                resourceId: match.id,
                changes: {
                    type,
                    calleeId,
                    sessionId,
                    expiresAt: expiresAt.toISOString(),
                },
            },
        });

        return {
            sessionId,
            matchId: match.id,
            type,
            channel,
            token,
            issuedAt: issuedAt.toISOString(),
            expiresAt: expiresAt.toISOString(),
            plan: callerEntitlement.plan,
        };
    },
};

module.exports = callService;
