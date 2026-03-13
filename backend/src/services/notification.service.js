const prisma = require('../config/prisma');

function timeAgo(date) {
    const diff = Date.now() - new Date(date).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
}

function displayName(profile) {
    if (!profile) return 'Someone';
    const name = [profile.firstName, profile.lastName].filter(Boolean).join(' ').trim();
    return name || 'Someone';
}

async function getLastReadAt(userId) {
    const last = await prisma.auditLog.findFirst({
        where: { userId, action: 'notifications_read' },
        orderBy: { createdAt: 'desc' },
    });
    return last?.createdAt || null;
}

async function listNotifications(userId, options = {}) {
    const limit = Math.min(Math.max(Number.parseInt(options.limit, 10) || 20, 5), 50);
    const lastReadAt = await getLastReadAt(userId);

    const [likes, matches, views, messages] = await Promise.all([
        prisma.like.findMany({
            where: { receiverId: userId, status: 'sent' },
            orderBy: { createdAt: 'desc' },
            take: 8,
            include: {
                sender: {
                    select: { id: true, isVerified: true },
                    include: {
                        profile: { select: { firstName: true, lastName: true } },
                    },
                },
            },
        }),
        prisma.match.findMany({
            where: { OR: [{ userAId: userId }, { userBId: userId }], isActive: true },
            orderBy: { createdAt: 'desc' },
            take: 6,
            include: {
                userA: { select: { id: true, profile: { select: { firstName: true, lastName: true } } } },
                userB: { select: { id: true, profile: { select: { firstName: true, lastName: true } } } },
            },
        }),
        prisma.auditLog.findMany({
            where: { action: 'profile_view', resourceId: userId },
            orderBy: { createdAt: 'desc' },
            take: 6,
            include: {
                user: { select: { id: true, profile: { select: { firstName: true, lastName: true } } } },
            },
        }),
        prisma.message.findMany({
            where: {
                senderId: { not: userId },
                match: { OR: [{ userAId: userId }, { userBId: userId }] },
            },
            orderBy: { createdAt: 'desc' },
            take: 6,
            include: {
                sender: { select: { id: true, profile: { select: { firstName: true, lastName: true } } } },
            },
        }),
    ]);

    const items = [];

    likes.forEach((like) => {
        const senderName = displayName(like.sender?.profile);
        items.push({
            id: `interest:${like.id}`,
            type: 'interest',
            title: 'New Interest',
            message: `${senderName} sent you an interest.`,
            createdAt: like.createdAt,
            time: timeAgo(like.createdAt),
            link: `/interests`,
        });
    });

    matches.forEach((match) => {
        const other = match.userAId === userId ? match.userB : match.userA;
        const otherName = displayName(other?.profile);
        items.push({
            id: `match:${match.id}`,
            type: 'match',
            title: 'New Match',
            message: `You matched with ${otherName}.`,
            createdAt: match.createdAt,
            time: timeAgo(match.createdAt),
            link: `/interests`,
        });
    });

    views.forEach((view) => {
        const viewerName = displayName(view.user?.profile);
        items.push({
            id: `view:${view.id}`,
            type: 'visitor',
            title: 'Profile View',
            message: `${viewerName} viewed your profile.`,
            createdAt: view.createdAt,
            time: timeAgo(view.createdAt),
            link: `/who-viewed`,
        });
    });

    messages.forEach((message) => {
        const senderName = displayName(message.sender?.profile);
        items.push({
            id: `message:${message.id}`,
            type: 'message',
            title: 'New Message',
            message: `New message from ${senderName}.`,
            createdAt: message.createdAt,
            time: timeAgo(message.createdAt),
            link: `/chat`,
        });
    });

    const sorted = items.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    const clipped = sorted.slice(0, limit);

    const enriched = clipped.map((item) => ({
        ...item,
        read: lastReadAt ? new Date(item.createdAt) <= new Date(lastReadAt) : false,
    }));

    return {
        items: enriched,
        meta: {
            total: enriched.length,
            unread: enriched.filter((item) => !item.read).length,
            lastReadAt,
        },
    };
}

async function markAllRead(userId) {
    const entry = await prisma.auditLog.create({
        data: {
            userId,
            action: 'notifications_read',
            resourceType: 'notification',
            resourceId: userId,
        },
    });
    return { lastReadAt: entry.createdAt };
}

module.exports = { listNotifications, markAllRead };
