const prisma = require('../config/prisma');

function sendError(res, error, fallback = 'Admin request failed') {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? fallback : error.message });
}

async function auditAdmin(req, action, resourceType, resourceId, changes) {
    return prisma.auditLog.create({
        data: {
            userId: req.user.sub,
            action,
            resourceType,
            resourceId,
            changes: changes || undefined,
            ipAddress: req.ip,
            userAgent: req.get('user-agent')?.slice(0, 500) || null,
        },
    });
}

const getDashboardStats = async (req, res) => {
    try {
        const staleCutoff = new Date(Date.now() - 90 * 86400000);
        const [
            totalUsers,
            discoverableProfiles,
            staleProfiles,
            activeSubscriptions,
            pendingReports,
            criticalReports,
            pendingPhotoModeration,
            openSupportTickets,
            unresolvedRiskSignals,
            revenue,
        ] = await Promise.all([
            prisma.user.count(),
            prisma.user.count({
                where: {
                    isActive: true,
                    isBanned: false,
                    deletedAt: null,
                    searchStatus: { in: ['active', 'low_activity'] },
                    profile: { isNot: null },
                },
            }),
            prisma.user.count({
                where: {
                    deletedAt: null,
                    profile: { isNot: null },
                    OR: [{ lastActiveAt: null }, { lastActiveAt: { lt: staleCutoff } }],
                },
            }),
            prisma.subscription.count({ where: { status: { in: ['active', 'grace'] } } }),
            prisma.report.count({ where: { status: { in: ['open', 'reviewing', 'escalated'] } } }),
            prisma.report.count({ where: { severity: 'critical', status: { notIn: ['resolved', 'dismissed'] } } }),
            prisma.photo.count({ where: { moderationStatus: { in: ['review_required', 'processing'] } } }),
            prisma.supportTicket.count({ where: { status: { notIn: ['closed'] } } }),
            prisma.riskSignal.count({ where: { resolvedAt: null } }),
            prisma.payment.aggregate({ where: { status: 'completed' }, _sum: { amountInr: true } }),
        ]);

        return res.json({
            totalUsers,
            discoverableProfiles,
            staleProfiles,
            activeSubscriptions,
            pendingReports,
            criticalReports,
            pendingPhotoModeration,
            openSupportTickets,
            unresolvedRiskSignals,
            completedRevenueInr: Number(revenue._sum.amountInr || 0),
        });
    } catch (error) {
        console.error('Admin dashboard error:', error);
        return sendError(res, error);
    }
};

const getAllUsers = async (req, res) => {
    try {
        const limit = Math.min(Math.max(Number.parseInt(req.query.limit, 10) || 50, 1), 100);
        const users = await prisma.user.findMany({
            orderBy: { createdAt: 'desc' },
            select: {
                id: true,
                email: true,
                phone: true,
                role: true,
                isBanned: true,
                isActive: true,
                searchStatus: true,
                identityStatus: true,
                lastActiveAt: true,
                statusReconfirmedAt: true,
                createdAt: true,
                profile: { select: { firstName: true, lastName: true, completionPercentage: true, role: true } },
            },
            take: limit,
        });
        return res.json(users);
    } catch (error) {
        return sendError(res, error);
    }
};

const banUser = async (req, res) => {
    try {
        const { userId, reason } = req.body || {};
        if (!userId || !reason) return res.status(400).json({ error: 'userId and reason are required' });
        await prisma.$transaction([
            prisma.user.update({
                where: { id: userId },
                data: { isBanned: true, banReason: String(reason), searchStatus: 'closed' },
            }),
            prisma.match.updateMany({
                where: { OR: [{ userAId: userId }, { userBId: userId }] },
                data: { isActive: false },
            }),
        ]);
        await auditAdmin(req, 'admin_user_banned', 'user', userId, { reason });
        return res.json({ success: true, message: 'User banned and removed from active matching' });
    } catch (error) {
        return sendError(res, error);
    }
};

const listPhotoModeration = async (req, res) => {
    try {
        const photos = await prisma.photo.findMany({
            where: { moderationStatus: { in: ['review_required', 'processing'] } },
            orderBy: { uploadedAt: 'asc' },
            take: 100,
            include: {
                user: {
                    select: {
                        id: true,
                        profile: { select: { firstName: true, lastName: true } },
                    },
                },
            },
        });
        return res.json(photos);
    } catch (error) {
        return sendError(res, error);
    }
};

const moderatePhoto = async (req, res) => {
    try {
        const { action, reason, makePrimary, photoVerified } = req.body || {};
        if (!['approve', 'reject'].includes(action)) return res.status(400).json({ error: 'action must be approve or reject' });
        if (action === 'reject' && !reason) return res.status(400).json({ error: 'reason is required when rejecting a photo' });

        const photo = await prisma.photo.findUnique({ where: { id: req.params.photoId } });
        if (!photo) return res.status(404).json({ error: 'Photo not found' });

        await prisma.$transaction(async (tx) => {
            if (action === 'approve' && makePrimary) {
                await tx.photo.updateMany({ where: { userId: photo.userId, isPrimary: true }, data: { isPrimary: false } });
            }
            await tx.photo.update({
                where: { id: photo.id },
                data: action === 'approve'
                    ? {
                        moderationStatus: 'approved',
                        rejectionReason: null,
                        isPrimary: Boolean(makePrimary),
                        ...(photoVerified ? { verificationStatus: 'verified', verifiedAt: new Date() } : {}),
                    }
                    : {
                        moderationStatus: 'rejected',
                        verificationStatus: 'rejected',
                        rejectionReason: String(reason),
                        isPrimary: false,
                    },
            });
            if (photoVerified && action === 'approve') {
                await tx.verification.upsert({
                    where: { userId_type: { userId: photo.userId, type: 'photo' } },
                    update: { status: 'verified', verifiedAt: new Date(), rejectionReason: null },
                    create: { userId: photo.userId, type: 'photo', status: 'verified', verifiedAt: new Date() },
                });
            }
        });
        await auditAdmin(req, 'admin_photo_moderated', 'photo', photo.id, { action, reason: reason || null, makePrimary: Boolean(makePrimary), photoVerified: Boolean(photoVerified) });
        return res.json({ success: true });
    } catch (error) {
        return sendError(res, error);
    }
};

const listReports = async (req, res) => {
    try {
        return res.json(await prisma.report.findMany({
            where: { status: { in: ['open', 'reviewing', 'escalated'] } },
            orderBy: [{ severity: 'desc' }, { createdAt: 'asc' }],
            take: 100,
        }));
    } catch (error) {
        return sendError(res, error);
    }
};

const resolveReport = async (req, res) => {
    try {
        const { decision, notes } = req.body || {};
        if (!['resolved', 'dismissed', 'escalated', 'reviewing'].includes(decision)) {
            return res.status(400).json({ error: 'Invalid report decision' });
        }
        const report = await prisma.report.update({
            where: { id: req.params.reportId },
            data: {
                status: decision,
                adminNotes: notes || null,
                resolvedAt: ['resolved', 'dismissed'].includes(decision) ? new Date() : null,
            },
        });
        await auditAdmin(req, 'admin_report_updated', 'report', report.id, { decision, notes: notes || null });
        return res.json(report);
    } catch (error) {
        return sendError(res, error);
    }
};

const listSupportTickets = async (req, res) => {
    try {
        return res.json(await prisma.supportTicket.findMany({
            where: req.query.status ? { status: String(req.query.status) } : {},
            orderBy: [{ priority: 'desc' }, { updatedAt: 'asc' }],
            take: 100,
            include: { events: { orderBy: { createdAt: 'asc' } } },
        }));
    } catch (error) {
        return sendError(res, error);
    }
};

const updateSupportTicket = async (req, res) => {
    try {
        const { status, assignedTeam, message, resolution } = req.body || {};
        const allowed = new Set(['open', 'assigned', 'in_progress', 'waiting_user', 'resolved', 'escalated']);
        if (status && !allowed.has(status)) {
            return res.status(400).json({ error: 'Admins may not directly close a ticket; use resolved and let the user confirm closure.' });
        }
        const ticket = await prisma.supportTicket.findUnique({ where: { id: req.params.ticketId } });
        if (!ticket) return res.status(404).json({ error: 'Ticket not found' });

        const updateData = {};
        if (status) updateData.status = status;
        if (assignedTeam !== undefined) updateData.assignedTeam = assignedTeam || null;
        if (resolution !== undefined) updateData.resolution = resolution || null;
        if (status === 'resolved') updateData.resolvedAt = new Date();
        if (status && status !== 'resolved') updateData.resolvedAt = null;

        const updated = await prisma.$transaction(async (tx) => {
            const row = await tx.supportTicket.update({ where: { id: ticket.id }, data: updateData });
            await tx.supportEvent.create({
                data: {
                    ticketId: ticket.id,
                    actorType: 'support',
                    actorId: req.user.sub,
                    eventType: status === 'resolved' ? 'resolved' : status ? 'status_changed' : 'comment',
                    message: message || resolution || null,
                    metadata: { status: status || ticket.status, assignedTeam: assignedTeam ?? ticket.assignedTeam },
                },
            });
            return row;
        });
        await auditAdmin(req, 'admin_support_ticket_updated', 'support_ticket', ticket.id, { status, assignedTeam, resolution: Boolean(resolution) });
        return res.json(updated);
    } catch (error) {
        return sendError(res, error);
    }
};

module.exports = {
    getDashboardStats,
    getAllUsers,
    banUser,
    listPhotoModeration,
    moderatePhoto,
    listReports,
    resolveReport,
    listSupportTickets,
    updateSupportTicket,
};
