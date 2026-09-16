const prisma = require('../config/prisma');
const safetyService = require('./safety.service');
const trustService = require('./trust.service');
const privacyService = require('./privacy.service');

function appError(message, statusCode = 400) {
    const error = new Error(message);
    error.statusCode = statusCode;
    return error;
}

async function findActiveMatch(user1Id, user2Id) {
    return prisma.match.findFirst({
        where: {
            isActive: true,
            OR: [
                { userAId: user1Id, userBId: user2Id },
                { userAId: user2Id, userBId: user1Id },
            ],
        },
    });
}

const messageService = {
    async areUsersMatched(user1Id, user2Id) {
        if (await safetyService.isBlocked(user1Id, user2Id)) return false;
        return Boolean(await findActiveMatch(user1Id, user2Id));
    },

    async saveMessage(senderId, receiverId, content, clientMessageId = null) {
        const text = String(content || '').trim();
        if (!text) throw appError('Message content is empty');
        if (text.length > 5000) throw appError('Message is too long');
        if (!receiverId || senderId === receiverId) throw appError('Invalid message recipient');

        await safetyService.ensureNotBlocked(senderId, receiverId);
        const [{ settings }, match] = await Promise.all([
            privacyService.getUserPrivacySettings(receiverId),
            findActiveMatch(senderId, receiverId),
        ]);
        if (!settings.allowMessages) throw appError('This profile is not accepting messages', 403);
        if (!match) throw appError('No active connection found between users', 403);

        if (clientMessageId) {
            const existing = await prisma.message.findUnique({ where: { clientMessageId } });
            if (existing) {
                if (existing.senderId !== senderId) throw appError('Message id is already in use', 409);
                return existing;
            }
        }

        const now = new Date();
        const message = await prisma.$transaction(async (tx) => {
            const created = await tx.message.create({
                data: {
                    matchId: match.id,
                    senderId,
                    content: text,
                    clientMessageId: clientMessageId || null,
                    status: 'sent',
                    createdAt: now,
                },
            });
            await tx.match.update({ where: { id: match.id }, data: { lastMessageAt: now } });
            return created;
        });

        await trustService.recordActivity(senderId);
        return message;
    },

    async markDelivered(messageId) {
        return prisma.message.updateMany({
            where: { id: messageId, status: 'sent' },
            data: { status: 'delivered', deliveredAt: new Date() },
        });
    },

    async markRead(readerId, otherUserId) {
        await safetyService.ensureNotBlocked(readerId, otherUserId);
        const match = await findActiveMatch(readerId, otherUserId);
        if (!match) return { updated: 0 };
        const now = new Date();
        const result = await prisma.message.updateMany({
            where: {
                matchId: match.id,
                senderId: { not: readerId },
                isRead: false,
            },
            data: { isRead: true, status: 'read', readAt: now, deliveredAt: now },
        });
        await trustService.recordActivity(readerId);
        return { updated: result.count };
    },

    async getMessages(user1Id, user2Id) {
        await safetyService.ensureNotBlocked(user1Id, user2Id);
        const match = await findActiveMatch(user1Id, user2Id);
        if (!match) return [];
        return prisma.message.findMany({
            where: { matchId: match.id },
            orderBy: { createdAt: 'asc' },
        });
    },

    async getConnectedUsers(userId) {
        const [matches, blocks] = await Promise.all([
            prisma.match.findMany({
                where: { OR: [{ userAId: userId }, { userBId: userId }], isActive: true },
                include: {
                    userA: {
                        select: { id: true, lastActiveAt: true, lastLogin: true, searchStatus: true },
                        include: {
                            profile: true,
                            photos: { where: { isPrimary: true, moderationStatus: 'approved' }, take: 1 },
                        },
                    },
                    userB: {
                        select: { id: true, lastActiveAt: true, lastLogin: true, searchStatus: true },
                        include: {
                            profile: true,
                            photos: { where: { isPrimary: true, moderationStatus: 'approved' }, take: 1 },
                        },
                    },
                },
                orderBy: [{ lastMessageAt: 'desc' }, { createdAt: 'desc' }],
            }),
            prisma.userBlock.findMany({
                where: { OR: [{ blockerId: userId }, { blockedUserId: userId }] },
                select: { blockerId: true, blockedUserId: true },
            }),
        ]);

        const blockedIds = new Set(blocks.map((row) => row.blockerId === userId ? row.blockedUserId : row.blockerId));
        return matches
            .map((match) => {
                const other = match.userAId === userId ? match.userB : match.userA;
                if (blockedIds.has(other.id)) return null;
                const photo = other.photos?.[0];
                return {
                    userId: other.id,
                    name: [other.profile?.firstName, other.profile?.lastName].filter(Boolean).join(' ') || 'Member',
                    photo: photo?.thumbnailUrl || photo?.photoUrl || null,
                    matchId: match.id,
                    lastMessageAt: match.lastMessageAt,
                    activity: trustService.activityBucket(other),
                    managedBy: other.profile?.role || 'self',
                };
            })
            .filter(Boolean);
    },
};

module.exports = messageService;
