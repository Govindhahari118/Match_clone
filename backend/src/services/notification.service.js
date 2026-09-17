const prisma = require('../config/prisma');

const ALLOWED_TYPES = new Set([
    'interest_received',
    'match_created',
    'message_received',
    'verification_update',
    'status_reconfirmation',
    'subscription_update',
    'support_update',
    'security_alert',
    'system',
]);

function safeDeepLink(value) {
    const link = String(value || '').trim();
    if (!link || !link.startsWith('/') || link.startsWith('//')) return null;
    return link.slice(0, 500);
}

function text(value, max) {
    return String(value || '').trim().slice(0, max);
}

async function createNotification(userId, payload = {}) {
    const type = ALLOWED_TYPES.has(payload.type) ? payload.type : 'system';
    const title = text(payload.title, 120);
    const message = text(payload.message, 500);
    if (!userId || !title || !message) return null;

    return prisma.auditLog.create({
        data: {
            userId,
            action: 'notification_created',
            resourceType: 'notification',
            resourceId: payload.entityId ? String(payload.entityId).slice(0, 200) : null,
            changes: {
                type,
                title,
                message,
                entityType: payload.entityType ? text(payload.entityType, 80) : null,
                entityId: payload.entityId ? text(payload.entityId, 200) : null,
                deepLink: safeDeepLink(payload.deepLink),
            },
        },
    });
}

async function listNotifications(userId, { limit = 50 } = {}) {
    const safeLimit = Math.max(1, Math.min(Number(limit) || 50, 100));
    const [notifications, readEvents, readAllEvent] = await Promise.all([
        prisma.auditLog.findMany({
            where: { userId, action: 'notification_created' },
            orderBy: { createdAt: 'desc' },
            take: safeLimit,
        }),
        prisma.auditLog.findMany({
            where: { userId, action: 'notification_read' },
            select: { resourceId: true },
        }),
        prisma.auditLog.findFirst({
            where: { userId, action: 'notification_read_all' },
            orderBy: { createdAt: 'desc' },
            select: { createdAt: true },
        }),
    ]);

    const individuallyRead = new Set(readEvents.map((row) => row.resourceId).filter(Boolean));
    const readAllAt = readAllEvent?.createdAt ? new Date(readAllEvent.createdAt).getTime() : 0;

    return notifications.map((row) => {
        const data = row.changes && typeof row.changes === 'object' ? row.changes : {};
        const createdAt = new Date(row.createdAt);
        return {
            id: row.id,
            type: data.type || 'system',
            title: data.title || 'Account update',
            message: data.message || '',
            entityType: data.entityType || null,
            entityId: data.entityId || row.resourceId || null,
            deepLink: safeDeepLink(data.deepLink),
            createdAt: row.createdAt,
            read: individuallyRead.has(row.id) || createdAt.getTime() <= readAllAt,
        };
    });
}

async function markRead(userId, notificationId) {
    const notification = await prisma.auditLog.findFirst({
        where: { id: notificationId, userId, action: 'notification_created' },
        select: { id: true },
    });
    if (!notification) {
        const error = new Error('Notification not found');
        error.statusCode = 404;
        throw error;
    }

    const existing = await prisma.auditLog.findFirst({
        where: { userId, action: 'notification_read', resourceId: notificationId },
        select: { id: true },
    });
    if (!existing) {
        await prisma.auditLog.create({
            data: {
                userId,
                action: 'notification_read',
                resourceType: 'notification',
                resourceId: notificationId,
            },
        });
    }
    return { success: true };
}

async function markAllRead(userId) {
    await prisma.auditLog.create({
        data: {
            userId,
            action: 'notification_read_all',
            resourceType: 'notification',
        },
    });
    return { success: true, readAt: new Date().toISOString() };
}

module.exports = {
    createNotification,
    listNotifications,
    markRead,
    markAllRead,
};
