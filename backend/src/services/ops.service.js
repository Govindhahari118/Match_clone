const prisma = require('../config/prisma');
const safetyService = require('./safety.service');

function severitySlaMinutes(severity) {
    const map = { critical: 15, high: 30, medium: 120, low: 360 };
    return map[severity] || map.low;
}

function resolveSeverity(reportType) {
    return safetyService.resolveSeverity(reportType);
}

const INCIDENT_RUNBOOKS = [
    {
        key: 'sev1_platform_degradation',
        title: 'Sev-1 Platform Degradation',
        steps: [
            'Declare incident and assign incident commander.',
            'Enable protective rate limits and pause non-critical workloads.',
            'Validate DB/cache health and rollback latest risky deploy if required.',
            'Post status updates every 10 minutes until stable.',
        ],
    },
    {
        key: 'trust_safety_spike',
        title: 'Trust & Safety Abuse Spike',
        steps: [
            'Switch moderation queue to critical-first mode.',
            'Enable stricter anti-spam thresholds temporarily.',
            'Run bulk triage for duplicate abuse reports.',
            'Escalate repeat offenders to restricted or suspended.',
        ],
    },
];

const opsService = {
    async getQueueHealth() {
        const reports = await prisma.report.findMany({
            where: { status: { in: ['open', 'reviewing'] } },
            select: {
                id: true,
                reportType: true,
                createdAt: true,
                status: true,
            },
            take: 5000,
        });

        let overdue = 0;
        let criticalOpen = 0;
        const bySeverity = { critical: 0, high: 0, medium: 0, low: 0 };

        for (const item of reports) {
            const severity = resolveSeverity(item.reportType);
            bySeverity[severity] = (bySeverity[severity] || 0) + 1;
            const dueAt = new Date(new Date(item.createdAt).getTime() + severitySlaMinutes(severity) * 60 * 1000);
            if (Date.now() > dueAt.getTime()) overdue += 1;
            if (severity === 'critical') criticalOpen += 1;
        }

        return {
            openCases: reports.length,
            overdueCases: overdue,
            criticalOpen,
            bySeverity,
        };
    },

    async getReliabilitySnapshot() {
        const since = new Date(Date.now() - 24 * 60 * 60 * 1000);

        const [statusUpdates, requestLogs] = await Promise.all([
            prisma.auditLog.findMany({
                where: {
                    action: 'message_status_updated',
                    createdAt: { gte: since },
                },
                select: { changes: true },
                take: 5000,
            }),
            prisma.auditLog.count({
                where: {
                    action: { in: ['payment_completed', 'payment_order_created', 'moderation_case_updated'] },
                    createdAt: { gte: since },
                },
            }),
        ]);

        const messageStatusCounts = { sent: 0, delivered: 0, seen: 0, failed: 0 };
        for (const entry of statusUpdates) {
            const status = String(entry?.changes?.status || '').toLowerCase();
            if (messageStatusCounts[status] !== undefined) {
                messageStatusCounts[status] += 1;
            }
        }

        const totalLifecycle = Object.values(messageStatusCounts).reduce((sum, value) => sum + value, 0);
        const deliverySuccessRate = totalLifecycle
            ? ((messageStatusCounts.delivered + messageStatusCounts.seen) / totalLifecycle)
            : 1;

        return {
            windowHours: 24,
            messageLifecycle: messageStatusCounts,
            deliverySuccessRate,
            criticalOpsEvents: requestLogs,
        };
    },

    async getOpsMetrics() {
        const [queue, reliability] = await Promise.all([
            this.getQueueHealth(),
            this.getReliabilitySnapshot(),
        ]);

        return {
            generatedAt: new Date().toISOString(),
            queue,
            reliability,
        };
    },

    getIncidentRunbooks() {
        return INCIDENT_RUNBOOKS;
    },
};

module.exports = opsService;
