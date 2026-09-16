const prisma = require('../config/prisma');

const HIGH_SEVERITY_REPORTS = new Set(['scam', 'money_request', 'underage', 'stolen_photo', 'fake_identity']);
const CRITICAL_REPORTS = new Set(['underage']);

function badRequest(message) {
    const error = new Error(message);
    error.statusCode = 400;
    return error;
}

async function isBlocked(userAId, userBId) {
    if (!userAId || !userBId || userAId === userBId) return false;
    const row = await prisma.userBlock.findFirst({
        where: {
            OR: [
                { blockerId: userAId, blockedUserId: userBId },
                { blockerId: userBId, blockedUserId: userAId },
            ],
        },
        select: { id: true },
    });
    return Boolean(row);
}

async function ensureNotBlocked(userAId, userBId) {
    if (await isBlocked(userAId, userBId)) {
        const error = new Error('Interaction is unavailable between these profiles');
        error.statusCode = 403;
        throw error;
    }
}

async function areMatched(userAId, userBId) {
    const match = await prisma.match.findFirst({
        where: {
            isActive: true,
            OR: [
                { userAId, userBId: userBId },
                { userAId: userBId, userBId: userAId },
            ],
        },
        select: { id: true },
    });
    return match;
}

async function blockUser(blockerId, blockedUserId, reason = null) {
    if (!blockedUserId || blockerId === blockedUserId) throw badRequest('Invalid profile to block');

    return prisma.$transaction(async (tx) => {
        const blocked = await tx.userBlock.upsert({
            where: { blockerId_blockedUserId: { blockerId, blockedUserId } },
            update: { reason: reason || null },
            create: { blockerId, blockedUserId, reason: reason || null },
        });

        await tx.like.updateMany({
            where: {
                OR: [
                    { senderId: blockerId, receiverId: blockedUserId },
                    { senderId: blockedUserId, receiverId: blockerId },
                ],
            },
            data: { status: 'blocked' },
        });

        await tx.match.updateMany({
            where: {
                OR: [
                    { userAId: blockerId, userBId: blockedUserId },
                    { userAId: blockedUserId, userBId: blockerId },
                ],
            },
            data: { isActive: false },
        });

        await tx.contactRequest.updateMany({
            where: {
                OR: [
                    { requesterId: blockerId, targetId: blockedUserId },
                    { requesterId: blockedUserId, targetId: blockerId },
                ],
                status: 'pending',
            },
            data: { status: 'declined' },
        });

        await tx.photoAccessRequest.updateMany({
            where: {
                OR: [
                    { requesterId: blockerId, targetId: blockedUserId },
                    { requesterId: blockedUserId, targetId: blockerId },
                ],
                status: 'pending',
            },
            data: { status: 'declined' },
        });

        await tx.auditLog.create({
            data: {
                userId: blockerId,
                action: 'user_blocked',
                resourceType: 'user',
                resourceId: blockedUserId,
                changes: reason ? { reason } : undefined,
            },
        });

        return blocked;
    });
}

async function unblockUser(blockerId, blockedUserId) {
    await prisma.userBlock.deleteMany({ where: { blockerId, blockedUserId } });
    await prisma.auditLog.create({
        data: {
            userId: blockerId,
            action: 'user_unblocked',
            resourceType: 'user',
            resourceId: blockedUserId,
        },
    });
    // Deliberately do not restore old matches/likes automatically.
    return { success: true };
}

async function requestContact(requesterId, targetId) {
    if (!targetId || requesterId === targetId) throw badRequest('Invalid contact request');
    await ensureNotBlocked(requesterId, targetId);
    const match = await areMatched(requesterId, targetId);
    if (!match) {
        const error = new Error('Contact can be requested only after a mutual connection');
        error.statusCode = 403;
        throw error;
    }

    return prisma.contactRequest.upsert({
        where: { requesterId_targetId: { requesterId, targetId } },
        update: { status: 'pending' },
        create: { requesterId, targetId, status: 'pending' },
    });
}

async function respondContactRequest(targetId, requestId, action) {
    const status = action === 'approve' ? 'approved' : action === 'decline' ? 'declined' : null;
    if (!status) throw badRequest('Action must be approve or decline');

    const request = await prisma.contactRequest.findFirst({
        where: { id: requestId, targetId },
    });
    if (!request) {
        const error = new Error('Contact request not found');
        error.statusCode = 404;
        throw error;
    }
    await ensureNotBlocked(request.requesterId, targetId);

    return prisma.contactRequest.update({
        where: { id: request.id },
        data: { status },
    });
}

async function canViewContact(viewerId, targetId) {
    if (viewerId === targetId) return true;
    if (await isBlocked(viewerId, targetId)) return false;

    const approved = await prisma.contactRequest.findFirst({
        where: {
            status: 'approved',
            OR: [
                { requesterId: viewerId, targetId },
                { requesterId: targetId, targetId: viewerId },
            ],
        },
        select: { id: true },
    });
    return Boolean(approved);
}

async function requestPhotoAccess(requesterId, targetId) {
    if (!targetId || requesterId === targetId) throw badRequest('Invalid photo access request');
    await ensureNotBlocked(requesterId, targetId);
    return prisma.photoAccessRequest.upsert({
        where: { requesterId_targetId: { requesterId, targetId } },
        update: { status: 'pending' },
        create: { requesterId, targetId, status: 'pending' },
    });
}

async function respondPhotoAccess(targetId, requestId, action) {
    const status = action === 'approve' ? 'approved' : action === 'decline' ? 'declined' : null;
    if (!status) throw badRequest('Action must be approve or decline');

    const request = await prisma.photoAccessRequest.findFirst({ where: { id: requestId, targetId } });
    if (!request) {
        const error = new Error('Photo access request not found');
        error.statusCode = 404;
        throw error;
    }
    return prisma.photoAccessRequest.update({ where: { id: request.id }, data: { status } });
}

async function hasPhotoAccess(viewerId, targetId) {
    if (viewerId === targetId) return true;
    if (await isBlocked(viewerId, targetId)) return false;
    const access = await prisma.photoAccessRequest.findFirst({
        where: { requesterId: viewerId, targetId, status: 'approved' },
        select: { id: true },
    });
    return Boolean(access);
}

function reportSeverity(reportType) {
    if (CRITICAL_REPORTS.has(reportType)) return 'critical';
    if (HIGH_SEVERITY_REPORTS.has(reportType)) return 'high';
    return 'normal';
}

async function createReport({ reporterId, reportedUserId, reportType, description }) {
    if (!reportedUserId || reporterId === reportedUserId) throw badRequest('Invalid report target');
    const severity = reportSeverity(reportType);

    return prisma.$transaction(async (tx) => {
        const report = await tx.report.create({
            data: {
                reporterId,
                reportedUserId,
                reportType,
                description: description || null,
                severity,
                status: severity === 'critical' ? 'escalated' : 'open',
            },
        });

        if (severity !== 'normal') {
            await tx.riskSignal.create({
                data: {
                    userId: reportedUserId,
                    signalType: reportType,
                    severity,
                    source: 'user_report',
                    metadata: { reportId: report.id },
                },
            });
        }

        return report;
    });
}

module.exports = {
    isBlocked,
    ensureNotBlocked,
    areMatched,
    blockUser,
    unblockUser,
    requestContact,
    respondContactRequest,
    canViewContact,
    requestPhotoAccess,
    respondPhotoAccess,
    hasPhotoAccess,
    reportSeverity,
    createReport,
};
