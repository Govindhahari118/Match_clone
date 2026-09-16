const prisma = require('../config/prisma');

const VALID_STATES = new Set(['seen', 'skipped', 'hidden', 'shortlisted', 'interest_sent', 'connected']);

function cooldownForState(state) {
    if (state === 'skipped') return new Date(Date.now() + 14 * 86400000);
    if (state === 'seen') return new Date(Date.now() + 3 * 86400000);
    return null;
}

async function recordExposure(viewerId, candidateId, state = 'seen') {
    if (!viewerId || !candidateId || viewerId === candidateId) return null;
    if (!VALID_STATES.has(state)) state = 'seen';
    const now = new Date();

    return prisma.profileExposure.upsert({
        where: { viewerId_candidateId: { viewerId, candidateId } },
        update: {
            lastSeenAt: now,
            impressionCount: { increment: 1 },
            interactionState: state,
            cooldownUntil: cooldownForState(state),
        },
        create: {
            viewerId,
            candidateId,
            firstSeenAt: now,
            lastSeenAt: now,
            interactionState: state,
            cooldownUntil: cooldownForState(state),
        },
    });
}

async function setInteractionState(viewerId, candidateId, state) {
    if (!VALID_STATES.has(state)) {
        const error = new Error('Invalid exposure state');
        error.statusCode = 400;
        throw error;
    }
    return recordExposure(viewerId, candidateId, state);
}

async function restoreProfile(viewerId, candidateId) {
    await prisma.profileExposure.deleteMany({ where: { viewerId, candidateId } });
    return { success: true };
}

async function getExcludedCandidateIds(viewerId) {
    const now = new Date();
    const rows = await prisma.profileExposure.findMany({
        where: {
            viewerId,
            OR: [
                { interactionState: 'hidden' },
                { interactionState: 'interest_sent' },
                { interactionState: 'connected' },
                {
                    interactionState: { in: ['seen', 'skipped'] },
                    cooldownUntil: { gt: now },
                },
            ],
        },
        select: { candidateId: true },
    });
    return rows.map((row) => row.candidateId);
}

async function recordPage(viewerId, candidateIds) {
    const ids = Array.from(new Set((candidateIds || []).filter((id) => id && id !== viewerId)));
    if (ids.length === 0) return;
    await Promise.all(ids.map((candidateId) => recordExposure(viewerId, candidateId, 'seen')));
}

module.exports = {
    VALID_STATES,
    recordExposure,
    setInteractionState,
    restoreProfile,
    getExcludedCandidateIds,
    recordPage,
};
