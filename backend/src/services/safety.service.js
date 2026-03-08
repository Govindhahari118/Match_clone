const prisma = require('../config/prisma');

const SAFETY_STATES = ['normal', 'watchlist', 'restricted', 'suspended'];
const MODERATION_STATUSES = ['open', 'reviewing', 'resolved', 'dismissed'];

const REPORT_SEVERITY = {
    scam: 'critical',
    harassment: 'high',
    fake_profile: 'high',
    inappropriate: 'medium',
    solicitation: 'high',
    spam: 'medium',
    other: 'low',
};

const SEVERITY_WEIGHT = {
    critical: 35,
    high: 20,
    medium: 10,
    low: 4,
};

const SLA_MINUTES = {
    critical: 15,
    high: 30,
    medium: 120,
    low: 360,
};

const SAFETY_ACTION_TYPES = new Set(['block', 'mute', 'unblock', 'unmute']);

function normalize(value) {
    return String(value || '').trim().toLowerCase();
}

function isObject(value) {
    return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function safeJsonParse(value, fallback = null) {
    try {
        return JSON.parse(value);
    } catch {
        return fallback;
    }
}

function resolveSeverity(reportType) {
    return REPORT_SEVERITY[normalize(reportType)] || 'low';
}

function getSlaDeadline(createdAt, severity) {
    const minutes = SLA_MINUTES[severity] || SLA_MINUTES.low;
    return new Date(new Date(createdAt).getTime() + (minutes * 60 * 1000));
}

function formatModerationNote(existing, update) {
    const baseText = String(existing || '').trim();
    const incoming = String(update || '').trim();
    if (!incoming) return baseText || null;
    const timestamped = `[${new Date().toISOString()}] ${incoming}`;
    return baseText ? `${baseText}\n${timestamped}` : timestamped;
}

async function getLatestSafetyState(userId) {
    const log = await prisma.auditLog.findFirst({
        where: {
            action: 'safety_state_changed',
            resourceType: 'user',
            resourceId: userId,
        },
        orderBy: { createdAt: 'desc' },
    });
    if (!log || !isObject(log.changes)) return 'normal';
    const candidate = normalize(log.changes.toState);
    return SAFETY_STATES.includes(candidate) ? candidate : 'normal';
}

async function getReportSummary(userId) {
    const reports = await prisma.report.findMany({
        where: { reportedUserId: userId },
        select: {
            status: true,
            reportType: true,
            createdAt: true,
        },
        orderBy: { createdAt: 'desc' },
        take: 150,
    });

    const openReports = reports.filter((item) => item.status === 'open' || item.status === 'reviewing');
    const recentWindowMs = 30 * 24 * 60 * 60 * 1000;
    const recentReports = reports.filter((item) => Date.now() - new Date(item.createdAt).getTime() <= recentWindowMs);

    let weightedSeverity = 0;
    for (const item of openReports) {
        weightedSeverity += SEVERITY_WEIGHT[resolveSeverity(item.reportType)] || 0;
    }

    return {
        totalReports: reports.length,
        openReports: openReports.length,
        recentReports: recentReports.length,
        weightedSeverity,
    };
}

async function getLikeVelocity(userId) {
    const oneHour = new Date(Date.now() - (60 * 60 * 1000));
    const oneDay = new Date(Date.now() - (24 * 60 * 60 * 1000));
    const [hourly, daily] = await Promise.all([
        prisma.like.count({
            where: {
                senderId: userId,
                createdAt: { gte: oneHour },
            },
        }),
        prisma.like.count({
            where: {
                senderId: userId,
                createdAt: { gte: oneDay },
            },
        }),
    ]);
    return { hourly, daily };
}

function deriveRiskLevel(score) {
    if (score >= 80) return 'critical';
    if (score >= 55) return 'high';
    if (score >= 30) return 'medium';
    return 'low';
}

function deriveSafetyState(score) {
    if (score >= 90) return 'suspended';
    if (score >= 65) return 'restricted';
    if (score >= 35) return 'watchlist';
    return 'normal';
}

async function applySystemSafetyState(targetUserId, fromState, toState, reason) {
    const shouldSuspend = toState === 'suspended';
    await prisma.$transaction([
        prisma.auditLog.create({
            data: {
                action: 'safety_state_changed',
                resourceType: 'user',
                resourceId: targetUserId,
                changes: {
                    fromState,
                    toState,
                    reason,
                    actor: 'system',
                },
            },
        }),
        prisma.user.update({
            where: { id: targetUserId },
            data: shouldSuspend
                ? {
                    isBanned: true,
                    banReason: reason || 'Safety automation suspension',
                }
                : {
                    isBanned: false,
                    banReason: null,
                },
        }),
    ]);
}

const safetyService = {
    SAFETY_STATES,
    MODERATION_STATUSES,
    SAFETY_ACTION_TYPES,

    normalizeSafetyState(value) {
        const normalized = normalize(value);
        return SAFETY_STATES.includes(normalized) ? normalized : null;
    },

    normalizeModerationStatus(value) {
        const normalized = normalize(value);
        return MODERATION_STATUSES.includes(normalized) ? normalized : null;
    },

    resolveSeverity,

    async evaluateRisk(targetUserId) {
        const [user, reportSummary, velocity, currentState] = await Promise.all([
            prisma.user.findUnique({
                where: { id: targetUserId },
                select: {
                    id: true,
                    createdAt: true,
                    isVerified: true,
                    identityStatus: true,
                    isBanned: true,
                },
            }),
            getReportSummary(targetUserId),
            getLikeVelocity(targetUserId),
            getLatestSafetyState(targetUserId),
        ]);

        if (!user) {
            return null;
        }

        const accountAgeDays = Math.max(
            0,
            Math.floor((Date.now() - new Date(user.createdAt).getTime()) / (24 * 60 * 60 * 1000)),
        );

        let score = 0;
        const reasons = [];

        score += reportSummary.weightedSeverity;
        if (reportSummary.weightedSeverity > 0) {
            reasons.push(`open_report_severity=${reportSummary.weightedSeverity}`);
        }

        if (reportSummary.recentReports >= 5) {
            score += 12;
            reasons.push(`recent_reports=${reportSummary.recentReports}`);
        } else if (reportSummary.recentReports >= 3) {
            score += 6;
            reasons.push(`recent_reports=${reportSummary.recentReports}`);
        }

        if (velocity.daily >= 80 || velocity.hourly >= 20) {
            score += 18;
            reasons.push(`like_velocity=high(${velocity.hourly}/h,${velocity.daily}/d)`);
        } else if (velocity.daily >= 40 || velocity.hourly >= 10) {
            score += 9;
            reasons.push(`like_velocity=medium(${velocity.hourly}/h,${velocity.daily}/d)`);
        }

        if (!user.isVerified || user.identityStatus !== 'verified') {
            score += 6;
            reasons.push('unverified_identity');
        }

        if (accountAgeDays <= 3) {
            score += 7;
            reasons.push('new_account');
        }

        if (user.isBanned) {
            score = Math.max(score, 100);
            reasons.push('already_banned');
        }

        score = Math.min(100, score);
        const riskLevel = deriveRiskLevel(score);
        const recommendedState = deriveSafetyState(score);

        return {
            userId: targetUserId,
            score,
            riskLevel,
            currentState,
            recommendedState,
            factors: reasons,
            reportSummary,
            velocity,
            accountAgeDays,
        };
    },

    async createReportCase(reporterId, payload = {}, context = {}) {
        const reportedUserId = String(payload.reportedUserId || '').trim();
        const reportType = normalize(payload.reportType || 'other');
        const description = payload.description ? String(payload.description).slice(0, 2000) : null;
        if (!reportedUserId) {
            return { error: 'reportedUserId is required', statusCode: 400 };
        }
        if (reportedUserId === reporterId) {
            return { error: 'Cannot report yourself', statusCode: 400 };
        }

        const severity = resolveSeverity(reportType);
        const report = await prisma.report.create({
            data: {
                reporterId,
                reportedUserId,
                reportType,
                description,
                status: 'open',
                adminNotes: `severity=${severity}`,
            },
        });

        await prisma.auditLog.create({
            data: {
                userId: reporterId,
                action: 'report_created',
                resourceType: 'report',
                resourceId: report.id,
                changes: {
                    reportType,
                    severity,
                    reportedUserId,
                    context: {
                        source: payload.source || 'app',
                        channel: payload.channel || 'profile',
                    },
                },
                ipAddress: context.ipAddress || null,
                userAgent: context.userAgent || null,
            },
        });

        const risk = await this.evaluateRisk(reportedUserId);
        if (risk && risk.recommendedState !== risk.currentState && ['restricted', 'suspended'].includes(risk.recommendedState)) {
            await applySystemSafetyState(
                reportedUserId,
                risk.currentState,
                risk.recommendedState,
                `Automated escalation: risk_score=${risk.score}`,
            );
        }

        return {
            success: true,
            reportId: report.id,
            severity,
            risk: risk ? { score: risk.score, level: risk.riskLevel, recommendedState: risk.recommendedState } : null,
        };
    },

    async getModerationQueue(params = {}) {
        const status = this.normalizeModerationStatus(params.status);
        const severityFilter = normalize(params.severity || '');
        const limit = Math.min(Math.max(Number.parseInt(params.limit || '50', 10) || 50, 1), 200);

        const where = status
            ? { status }
            : { status: { in: ['open', 'reviewing'] } };

        const reports = await prisma.report.findMany({
            where,
            orderBy: { createdAt: 'asc' },
            take: limit,
            include: {
                reporter: {
                    select: {
                        id: true,
                        isVerified: true,
                        profile: { select: { firstName: true, lastName: true } },
                    },
                },
                reportedUser: {
                    select: {
                        id: true,
                        isVerified: true,
                        isBanned: true,
                        profile: { select: { firstName: true, lastName: true } },
                    },
                },
            },
        });

        const rows = [];
        for (const report of reports) {
            const severity = resolveSeverity(report.reportType);
            if (severityFilter && severity !== severityFilter) {
                continue;
            }
            const slaDueAt = getSlaDeadline(report.createdAt, severity);
            const ageMinutes = Math.max(0, Math.floor((Date.now() - new Date(report.createdAt).getTime()) / 60000));
            const overdue = Date.now() > slaDueAt.getTime();

            rows.push({
                id: report.id,
                status: report.status,
                reportType: report.reportType,
                severity,
                reporter: report.reporter,
                reportedUser: report.reportedUser,
                description: report.description,
                adminNotes: report.adminNotes,
                createdAt: report.createdAt,
                updatedAt: report.updatedAt,
                sla: {
                    dueAt: slaDueAt.toISOString(),
                    overdue,
                    targetMinutes: SLA_MINUTES[severity],
                    ageMinutes,
                    remainingMinutes: Math.max(0, Math.floor((slaDueAt.getTime() - Date.now()) / 60000)),
                },
                priorityScore: (SEVERITY_WEIGHT[severity] || 1) * 1000 + ageMinutes,
            });
        }

        rows.sort((a, b) => b.priorityScore - a.priorityScore);

        return {
            total: rows.length,
            items: rows,
        };
    },

    async updateModerationCase(actorId, reportId, payload = {}) {
        const status = payload.status ? this.normalizeModerationStatus(payload.status) : null;
        const safetyState = payload.safetyState ? this.normalizeSafetyState(payload.safetyState) : null;
        if (payload.status && !status) {
            return { error: 'Invalid moderation status', statusCode: 400 };
        }
        if (payload.safetyState && !safetyState) {
            return { error: 'Invalid safety state', statusCode: 400 };
        }

        const existing = await prisma.report.findUnique({
            where: { id: reportId },
            include: {
                reportedUser: {
                    select: { id: true, isBanned: true, banReason: true },
                },
            },
        });
        if (!existing) {
            return { error: 'Report not found', statusCode: 404 };
        }

        const previousState = await getLatestSafetyState(existing.reportedUserId);
        const nextState = safetyState || previousState;
        const shouldBan = nextState === 'suspended';

        const updated = await prisma.$transaction(async (tx) => {
            const report = await tx.report.update({
                where: { id: reportId },
                data: {
                    status: status || existing.status,
                    adminNotes: formatModerationNote(existing.adminNotes, payload.adminNote),
                },
            });

            if (safetyState && safetyState !== previousState) {
                await tx.user.update({
                    where: { id: existing.reportedUserId },
                    data: shouldBan
                        ? { isBanned: true, banReason: payload.adminNote || 'Suspended by moderation action' }
                        : { isBanned: false, banReason: null },
                });
            }

            await tx.auditLog.create({
                data: {
                    userId: actorId,
                    action: 'moderation_case_updated',
                    resourceType: 'report',
                    resourceId: reportId,
                    changes: {
                        previousStatus: existing.status,
                        nextStatus: report.status,
                        safetyStateFrom: previousState,
                        safetyStateTo: nextState,
                        note: payload.adminNote || null,
                    },
                },
            });

            if (safetyState && safetyState !== previousState) {
                await tx.auditLog.create({
                    data: {
                        userId: actorId,
                        action: 'safety_state_changed',
                        resourceType: 'user',
                        resourceId: existing.reportedUserId,
                        changes: {
                            fromState: previousState,
                            toState: safetyState,
                            reason: payload.adminNote || 'Updated from moderation console',
                            reportId,
                            actor: 'admin',
                        },
                    },
                });
            }

            return report;
        });

        return {
            success: true,
            report: updated,
            safetyState: nextState,
        };
    },

    async getRiskSnapshot(targetUserId) {
        const result = await this.evaluateRisk(targetUserId);
        if (!result) {
            return { error: 'User not found', statusCode: 404 };
        }
        return result;
    },

    async applyUserSafetyAction(actorId, targetUserId, actionType, context = {}) {
        const action = normalize(actionType);
        if (!SAFETY_ACTION_TYPES.has(action)) {
            return { error: 'Invalid safety action type', statusCode: 400 };
        }
        if (!targetUserId || targetUserId === actorId) {
            return { error: 'Invalid target user', statusCode: 400 };
        }

        const primary = action.includes('block') ? 'block' : 'mute';
        const isActive = !action.startsWith('un');

        await prisma.auditLog.create({
            data: {
                userId: actorId,
                action: 'user_safety_action',
                resourceType: 'user_relationship',
                resourceId: targetUserId,
                changes: {
                    type: primary,
                    active: isActive,
                },
                ipAddress: context.ipAddress || null,
                userAgent: context.userAgent || null,
            },
        });

        if (primary === 'block' && isActive) {
            await prisma.like.updateMany({
                where: {
                    OR: [
                        { senderId: actorId, receiverId: targetUserId },
                        { senderId: targetUserId, receiverId: actorId },
                    ],
                },
                data: { status: 'blocked' },
            });
        }

        return {
            success: true,
            action: primary,
            active: isActive,
            targetUserId,
        };
    },

    async getSafetyRelationship(userId, otherUserId) {
        if (!userId || !otherUserId) {
            return {
                viewerBlockedTarget: false,
                targetBlockedViewer: false,
                viewerMutedTarget: false,
                targetMutedViewer: false,
            };
        }

        const logs = await prisma.auditLog.findMany({
            where: {
                action: 'user_safety_action',
                OR: [
                    { userId, resourceId: otherUserId },
                    { userId: otherUserId, resourceId: userId },
                ],
            },
            orderBy: { createdAt: 'desc' },
            take: 100,
        });

        let viewerBlockedTarget = false;
        let targetBlockedViewer = false;
        let viewerMutedTarget = false;
        let targetMutedViewer = false;
        let viewerBlockResolved = false;
        let targetBlockResolved = false;
        let viewerMuteResolved = false;
        let targetMuteResolved = false;

        for (const log of logs) {
            const changes = isObject(log.changes)
                ? log.changes
                : (typeof log.changes === 'string' ? safeJsonParse(log.changes, {}) : {});
            const type = normalize(changes.type);
            const active = Boolean(changes.active);
            const byViewer = log.userId === userId;

            if (type === 'block') {
                if (byViewer && !viewerBlockResolved) {
                    viewerBlockedTarget = active;
                    viewerBlockResolved = true;
                }
                if (!byViewer && !targetBlockResolved) {
                    targetBlockedViewer = active;
                    targetBlockResolved = true;
                }
            }

            if (type === 'mute') {
                if (byViewer && !viewerMuteResolved) {
                    viewerMutedTarget = active;
                    viewerMuteResolved = true;
                }
                if (!byViewer && !targetMuteResolved) {
                    targetMutedViewer = active;
                    targetMuteResolved = true;
                }
            }

            if (viewerBlockResolved && targetBlockResolved && viewerMuteResolved && targetMuteResolved) {
                break;
            }
        }

        return {
            viewerBlockedTarget,
            targetBlockedViewer,
            viewerMutedTarget,
            targetMutedViewer,
        };
    },

    async isCommunicationBlocked(userId, otherUserId) {
        const relation = await this.getSafetyRelationship(userId, otherUserId);
        return relation.viewerBlockedTarget || relation.targetBlockedViewer;
    },

    async assignModerationCase(actorId, reportId, payload = {}) {
        const ownerId = String(payload.ownerId || '').trim();
        if (!ownerId) {
            return { error: 'ownerId is required', statusCode: 400 };
        }

        const report = await prisma.report.findUnique({ where: { id: reportId } });
        if (!report) {
            return { error: 'Report not found', statusCode: 404 };
        }

        const note = payload.note ? String(payload.note).slice(0, 500) : null;
        const adminNote = formatModerationNote(report.adminNotes, note ? `Assigned to ${ownerId}: ${note}` : `Assigned to ${ownerId}`);
        const updated = await prisma.report.update({
            where: { id: reportId },
            data: {
                status: report.status === 'open' ? 'reviewing' : report.status,
                adminNotes: adminNote,
            },
        });

        await prisma.auditLog.create({
            data: {
                userId: actorId,
                action: 'moderation_case_assigned',
                resourceType: 'report',
                resourceId: reportId,
                changes: {
                    ownerId,
                    note: note || null,
                },
            },
        });

        return {
            success: true,
            report: updated,
            ownerId,
        };
    },

    async bulkModerationAction(actorId, payload = {}) {
        const reportIds = Array.isArray(payload.reportIds)
            ? payload.reportIds.map((item) => String(item || '').trim()).filter(Boolean)
            : [];
        const nextStatus = this.normalizeModerationStatus(payload.status);
        const previewOnly = Boolean(payload.preview);

        if (reportIds.length === 0) {
            return { error: 'reportIds is required', statusCode: 400 };
        }
        if (!nextStatus) {
            return { error: 'status must be open/reviewing/resolved/dismissed', statusCode: 400 };
        }

        const reports = await prisma.report.findMany({
            where: { id: { in: reportIds } },
            select: { id: true, status: true, reportType: true, reportedUserId: true, createdAt: true },
        });

        if (previewOnly) {
            return {
                success: true,
                preview: true,
                requested: reportIds.length,
                found: reports.length,
                nextStatus,
                reportIds: reports.map((item) => item.id),
            };
        }

        const previousStates = reports.map((item) => ({ id: item.id, status: item.status }));
        const operationId = `bulk_${Date.now()}_${Math.floor(Math.random() * 100000)}`;

        await prisma.$transaction(async (tx) => {
            await tx.report.updateMany({
                where: { id: { in: reports.map((item) => item.id) } },
                data: {
                    status: nextStatus,
                },
            });

            await tx.auditLog.create({
                data: {
                    userId: actorId,
                    action: 'moderation_bulk_updated',
                    resourceType: 'report_batch',
                    resourceId: operationId,
                    changes: {
                        operationId,
                        nextStatus,
                        previousStates,
                    },
                },
            });
        });

        return {
            success: true,
            preview: false,
            operationId,
            updatedCount: reports.length,
            nextStatus,
        };
    },

    async rollbackBulkModeration(actorId, operationId) {
        const operation = await prisma.auditLog.findFirst({
            where: {
                action: 'moderation_bulk_updated',
                resourceType: 'report_batch',
                resourceId: operationId,
            },
            orderBy: { createdAt: 'desc' },
        });
        if (!operation || !isObject(operation.changes)) {
            return { error: 'Bulk operation not found', statusCode: 404 };
        }

        const previousStates = Array.isArray(operation.changes.previousStates)
            ? operation.changes.previousStates
            : [];
        if (previousStates.length === 0) {
            return { error: 'Rollback metadata missing', statusCode: 400 };
        }

        await prisma.$transaction(async (tx) => {
            for (const item of previousStates) {
                if (!item?.id || !item?.status) continue;
                await tx.report.update({
                    where: { id: item.id },
                    data: { status: item.status },
                });
            }

            await tx.auditLog.create({
                data: {
                    userId: actorId,
                    action: 'moderation_bulk_rollback',
                    resourceType: 'report_batch',
                    resourceId: operationId,
                    changes: {
                        restoredCount: previousStates.length,
                    },
                },
            });
        });

        return {
            success: true,
            operationId,
            restoredCount: previousStates.length,
        };
    },

    async sampleModerationDecisions(limit = 20) {
        const take = Math.min(Math.max(Number.parseInt(limit, 10) || 20, 5), 100);
        const decisions = await prisma.report.findMany({
            where: {
                status: { in: ['resolved', 'dismissed'] },
            },
            orderBy: { updatedAt: 'desc' },
            take: take * 2,
            select: {
                id: true,
                status: true,
                reportType: true,
                description: true,
                adminNotes: true,
                updatedAt: true,
                reportedUserId: true,
            },
        });

        const sampled = decisions
            .sort(() => (Math.random() > 0.5 ? 1 : -1))
            .slice(0, take);

        return {
            sampledCount: sampled.length,
            items: sampled,
        };
    },
};

module.exports = safetyService;
