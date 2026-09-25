const prisma = require('../config/prisma');

function messageError(message, code) {
    const error = new Error(message);
    error.code = code;
    return error;
}

async function assertNotBlocked(userAId, userBId) {
    const block = await prisma.block.findFirst({
        where: {
            OR: [
                { blockerId: userAId, blockedId: userBId },
                { blockerId: userBId, blockedId: userAId }
            ]
        },
        select: { id: true }
    });
    if (block) throw messageError('Messaging is unavailable for this relationship', 'BLOCKED');
}

const messageService = {
    async areUsersMatched(user1Id, user2Id) {
        const match = await prisma.match.findFirst({
            where: {
                isActive: true,
                OR: [
                    { userAId: user1Id, userBId: user2Id },
                    { userAId: user2Id, userBId: user1Id }
                ]
            }
        });
        return !!match;
    },

    async saveMessage(senderId, receiverId, content, { clientMessageId = null } = {}) {
        const normalizedContent = String(content || '').trim();
        if (!normalizedContent) throw messageError('Message content empty', 'EMPTY_MESSAGE');
        if (senderId === receiverId) throw messageError('Cannot message yourself', 'INVALID_RECEIVER');

        await assertNotBlocked(senderId, receiverId);

        const match = await prisma.match.findFirst({
            where: {
                isActive: true,
                OR: [
                    { userAId: senderId, userBId: receiverId },
                    { userAId: receiverId, userBId: senderId }
                ]
            }
        });

        if (!match) throw messageError('No active connection found between users', 'NOT_CONNECTED');

        if (clientMessageId) {
            const existing = await prisma.message.findFirst({
                where: { senderId, clientMessageId }
            });
            if (existing) return existing;
        }

        return prisma.message.create({
            data: {
                matchId: match.id,
                senderId,
                receiverId,
                clientMessageId,
                content: normalizedContent,
                status: 'sent'
            }
        });
    },

    async markDelivered(recipientId, messageId) {
        const message = await prisma.message.findUnique({ where: { id: messageId } });
        if (!message || message.receiverId !== recipientId) {
            throw messageError('Message not found', 'MESSAGE_NOT_FOUND');
        }
        await assertNotBlocked(message.senderId, recipientId);
        if (message.status === 'read' || message.status === 'delivered') return message;

        return prisma.message.update({
            where: { id: message.id },
            data: { status: 'delivered', deliveredAt: new Date() }
        });
    },

    async markRead(recipientId, messageId) {
        const message = await prisma.message.findUnique({ where: { id: messageId } });
        if (!message || message.receiverId !== recipientId) {
            throw messageError('Message not found', 'MESSAGE_NOT_FOUND');
        }
        await assertNotBlocked(message.senderId, recipientId);
        if (message.status === 'read') return message;

        const now = new Date();
        return prisma.message.update({
            where: { id: message.id },
            data: {
                status: 'read',
                deliveredAt: message.deliveredAt || now,
                readAt: now,
                isRead: true
            }
        });
    },

    async getMessages(user1Id, user2Id) {
        await assertNotBlocked(user1Id, user2Id);
        const match = await prisma.match.findFirst({
            where: {
                isActive: true,
                OR: [
                    { userAId: user1Id, userBId: user2Id },
                    { userAId: user2Id, userBId: user1Id }
                ]
            }
        });

        if (!match) return [];

        return prisma.message.findMany({
            where: { matchId: match.id },
            orderBy: { createdAt: 'asc' }
        });
    },

    async getConnectedUsers(userId) {
        const blockedPairs = await prisma.block.findMany({
            where: { OR: [{ blockerId: userId }, { blockedId: userId }] },
            select: { blockerId: true, blockedId: true }
        });
        const blockedIds = new Set(blockedPairs.map(b => b.blockerId === userId ? b.blockedId : b.blockerId));

        const matches = await prisma.match.findMany({
            where: {
                isActive: true,
                OR: [{ userAId: userId }, { userBId: userId }]
            },
            include: {
                userA: { select: { id: true, isActive: true, isBanned: true, profile: true, photos: true } },
                userB: { select: { id: true, isActive: true, isBanned: true, profile: true, photos: true } }
            }
        });

        return matches
            .map(match => {
                const otherUser = match.userAId === userId ? match.userB : match.userA;
                if (!otherUser || blockedIds.has(otherUser.id) || !otherUser.isActive || otherUser.isBanned || !otherUser.profile) return null;
                const primaryPhoto = otherUser.photos.find(photo => photo.isPrimary) || null;
                return {
                    userId: otherUser.id,
                    name: [otherUser.profile.firstName, otherUser.profile.lastName].filter(Boolean).join(' '),
                    photo: primaryPhoto?.thumbnailUrl || primaryPhoto?.photoUrl || null,
                    matchId: match.id
                };
            })
            .filter(Boolean);
    }
};

module.exports = messageService;
