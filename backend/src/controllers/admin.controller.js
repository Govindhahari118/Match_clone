const prisma = require('../config/prisma');
const safetyService = require('../services/safety.service');
const opsService = require('../services/ops.service');
const { parsePagination } = require('../utils/pagination');

const getDashboardStats = async (req, res) => {
    try {
        const totalUsers = await prisma.user.count();
        const activeSubscriptions = await prisma.subscription.count({ where: { status: 'active' } });
        const pendingReports = await prisma.report.count({ where: { status: 'open' } });

        // Example: Monthly revenue
        // const revenue = await prisma.payment.aggregate({ _sum: { amountInr: true } });

        res.json({
            totalUsers,
            activeSubscriptions,
            pendingReports,
            revenue: 25000 // Mock
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

const getAllUsers = async (req, res) => {
    try {
        const { limit, skip } = parsePagination(req.query, { defaultLimit: 20, maxLimit: 100 });
        const users = await prisma.user.findMany({
            select: { id: true, email: true, phone: true, role: true, isBanned: true, profile: { select: { firstName: true, lastName: true } } },
            orderBy: { createdAt: 'desc' },
            skip,
            take: limit,
        });
        res.json(users);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

const banUser = async (req, res) => {
    try {
        const { userId } = req.body;
        await prisma.user.update({
            where: { id: userId },
            data: { isBanned: true }
        });
        await prisma.auditLog.create({
            data: {
                userId: req.user.sub,
                action: 'admin_user_banned',
                resourceType: 'user',
                resourceId: userId,
                changes: { reason: req.body?.reason || null },
            },
        });
        res.json({ success: true, message: "User banned successfully" });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

const getModerationQueue = async (req, res) => {
    try {
        const queue = await safetyService.getModerationQueue(req.query || {});
        return res.json(queue);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
};

const updateModerationCase = async (req, res) => {
    try {
        const actorId = req.user.sub;
        const { reportId } = req.params;
        const result = await safetyService.updateModerationCase(actorId, reportId, req.body || {});
        if (result.error) {
            return res.status(result.statusCode || 400).json({ error: result.error });
        }
        return res.json(result);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
};

const getUserRiskSnapshot = async (req, res) => {
    try {
        const { userId } = req.params;
        const result = await safetyService.getRiskSnapshot(userId);
        if (result.error) {
            return res.status(result.statusCode || 400).json({ error: result.error });
        }
        return res.json(result);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
};

const assignModerationCase = async (req, res) => {
    try {
        const actorId = req.user.sub;
        const { reportId } = req.params;
        const result = await safetyService.assignModerationCase(actorId, reportId, req.body || {});
        if (result.error) {
            return res.status(result.statusCode || 400).json({ error: result.error });
        }
        return res.json(result);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
};

const bulkModerationAction = async (req, res) => {
    try {
        const actorId = req.user.sub;
        const result = await safetyService.bulkModerationAction(actorId, req.body || {});
        if (result.error) {
            return res.status(result.statusCode || 400).json({ error: result.error });
        }
        return res.json(result);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
};

const rollbackBulkModeration = async (req, res) => {
    try {
        const actorId = req.user.sub;
        const { operationId } = req.params;
        const result = await safetyService.rollbackBulkModeration(actorId, operationId);
        if (result.error) {
            return res.status(result.statusCode || 400).json({ error: result.error });
        }
        return res.json(result);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
};

const getModerationQaSample = async (req, res) => {
    try {
        const result = await safetyService.sampleModerationDecisions(req.query?.limit || 20);
        return res.json(result);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
};

const getOpsMetrics = async (_req, res) => {
    try {
        const result = await opsService.getOpsMetrics();
        return res.json(result);
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
};

const getRunbooks = async (_req, res) => {
    try {
        return res.json({
            runbooks: opsService.getIncidentRunbooks(),
        });
    } catch (error) {
        return res.status(500).json({ error: error.message });
    }
};

module.exports = {
    getDashboardStats,
    getAllUsers,
    banUser,
    getModerationQueue,
    updateModerationCase,
    getUserRiskSnapshot,
    assignModerationCase,
    bulkModerationAction,
    rollbackBulkModeration,
    getModerationQaSample,
    getOpsMetrics,
    getRunbooks,
};
