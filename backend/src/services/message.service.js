const prisma = require('../config/prisma');
const safetyService = require('./safety.service');

const MESSAGE_STATUS_VALUES = ['sent', 'delivered', 'seen', 'failed'];

function normalizeStatus(value) {
    const normalized = String(value || '').trim().toLowerCase();
    return MESSAGE_STATUS_VALUES.includes(normalized) ? normalized : null;
}

function normalizeClientMessageId(value) {
    const id = String(value || '').trim();
    if (!id) return null;
    return id.slice(0, 120);
}

function buildClientMessageKey(senderId, clientMessageId) {
    return `${senderId}:${clientMessageId}`;
}

async function getMatchRecord(user1Id, user2Id) {
    return prisma.match.findFirst({
        where: {
            OR: [
                { userAId: user1Id, userBId: user2Id },
                { userAId: user2Id, userBId: user1Id },
            ],
        },
    });
}

async function isUserInMatch(userId, matchId) {
    if (!userId || !matchId) return false;
    const match = await prisma.match.findFirst({
        where: {
            id: matchId,
            OR: [{ userAId: userId }, { userBId: userId }],
        },
        select: { id: true },
    });
    return Boolean(match);
}

async function enforceFirstMessageThrottle(senderId, receiverId, matchId) {
    const priorSenderMessageCount = await prisma.message.count({
        where: { matchId, senderId },
    });
    if (priorSenderMessageCount > 0) {
        return;
    }

    const oneHour = new Date(Date.now() - 60 * 60 * 1000);
    const firstMessageCountLastHour = await prisma.message.count({
        where: {
            senderId,
            createdAt: { gte: oneHour },
            match: {
                OR: [
                    { userAId: senderId, userBId: receiverId },
                    { userAId: receiverId, userBId: senderId },
                ],
            },
        },
    });

    if (firstMessageCountLastHour >= 20) {
        throw new Error('Rate limit reached for initiating new chats');
    }
}

async function preventDuplicateBurst(senderId, matchId, content) {
    const normalized = String(content || '').trim();
    const recentWindow = new Date(Date.now() - 30 * 1000);
    const duplicate = await prisma.message.findFirst({
        where: {
            matchId,
            senderId,
            createdAt: { gte: recentWindow },
            content: normalized,
        },
        orderBy: { createdAt: 'desc' },
    });
    if (duplicate) {
        return duplicate;
    }
    return null;
}

async function findIdempotentMessage(senderId, clientMessageId) {
    if (!clientMessageId) return null;
    const key = buildClientMessageKey(senderId, clientMessageId);
    const log = await prisma.auditLog.findFirst({
        where: {
            userId: senderId,
            action: 'message_client_id',
            resourceType: 'message_client',
            resourceId: key,
        },
        orderBy: { createdAt: 'desc' },
    });
    if (!log || !log.changes || typeof log.changes !== 'object') {
        return null;
    }
    const existingMessageId = log.changes.messageId;
    if (!existingMessageId) return null;
    return prisma.message.findUnique({ where: { id: existingMessageId } });
}

async function writeMessageStatusAudit(actorId, messageId, status, metadata = {}) {
    await prisma.auditLog.create({
        data: {
            userId: actorId,
            action: 'message_status_updated',
            resourceType: 'message',
            resourceId: messageId,
            changes: {
                status,
                ...metadata,
            },
        },
    });
}

async function fetchLatestMessageStatuses(messageIds = []) {
    if (messageIds.length === 0) return new Map();
    const logs = await prisma.auditLog.findMany({
        where: {
            action: 'message_status_updated',
            resourceType: 'message',
            resourceId: { in: messageIds },
        },
        orderBy: { createdAt: 'desc' },
        take: Math.max(messageIds.length * 3, 50),
    });

    const statusMap = new Map();
    for (const log of logs) {
        if (statusMap.has(log.resourceId)) continue;
        const changes = log.changes && typeof log.changes === 'object' ? log.changes : {};
        const status = normalizeStatus(changes.status);
        if (status) {
            statusMap.set(log.resourceId, status);
        }
    }
    return statusMap;
}

const messageService = {
    MESSAGE_STATUS_VALUES,
    isUserInMatch,

    async areUsersMatched(user1Id, user2Id) {
        const match = await getMatchRecord(user1Id, user2Id);
        return !!match;
    },

    async saveMessage(senderId, receiverId, content, options = {}) {
        const trimmed = String(content || '').trim();
        if (!trimmed) throw new Error('Message content empty');

        const communicationBlocked = await safetyService.isCommunicationBlocked(senderId, receiverId);
        if (communicationBlocked) {
            throw new Error('Messaging unavailable due to safety settings');
        }

        const clientMessageId = normalizeClientMessageId(options.clientMessageId);
        const existing = await findIdempotentMessage(senderId, clientMessageId);
        if (existing) {
            return { ...existing, status: existing.isRead ? 'seen' : 'sent', idempotent: true };
        }

        const match = await getMatchRecord(senderId, receiverId);
        if (!match) throw new Error('No match found between users');

        await enforceFirstMessageThrottle(senderId, receiverId, match.id);

        const duplicate = await preventDuplicateBurst(senderId, match.id, trimmed);
        if (duplicate) {
            return { ...duplicate, status: duplicate.isRead ? 'seen' : 'sent', duplicateSuppressed: true };
        }

        const message = await prisma.message.create({
            data: {
                matchId: match.id,
                senderId,
                content: trimmed,
                createdAt: new Date(),
            },
        });

        const auditWrites = [
            writeMessageStatusAudit(senderId, message.id, 'sent', {
                receiverId,
                matchId: match.id,
            }),
        ];
        if (clientMessageId) {
            auditWrites.push(
                prisma.auditLog.create({
                    data: {
                        userId: senderId,
                        action: 'message_client_id',
                        resourceType: 'message_client',
                        resourceId: buildClientMessageKey(senderId, clientMessageId),
                        changes: {
                            messageId: message.id,
                            receiverId,
                        },
                    },
                }),
            );
        }
        await Promise.all(auditWrites);

        return {
            ...message,
            status: 'sent',
        };
    },

    async updateMessageStatus(actorId, messageId, status, metadata = {}) {
        const normalizedStatus = normalizeStatus(status);
        if (!normalizedStatus) {
            return { error: `status must be one of ${MESSAGE_STATUS_VALUES.join(', ')}`, statusCode: 400 };
        }

        const message = await prisma.message.findUnique({
            where: { id: messageId },
            include: {
                match: {
                    select: {
                        id: true,
                        userAId: true,
                        userBId: true,
                    },
                },
            },
        });
        if (!message) {
            return { error: 'Message not found', statusCode: 404 };
        }

        const isParticipant = actorId === message.match.userAId || actorId === message.match.userBId;
        if (!isParticipant) {
            return { error: 'Forbidden', statusCode: 403 };
        }

        const blocked = await safetyService.isCommunicationBlocked(message.match.userAId, message.match.userBId);
        if (blocked) {
            return { error: 'Action unavailable due to safety settings', statusCode: 403 };
        }

        if (normalizedStatus === 'seen') {
            await prisma.message.update({
                where: { id: message.id },
                data: { isRead: true },
            });
        }

        await writeMessageStatusAudit(actorId, messageId, normalizedStatus, metadata);

        return {
            success: true,
            messageId,
            status: normalizedStatus,
        };
    },

    async markConversationSeen(userId, otherUserId) {
        const blocked = await safetyService.isCommunicationBlocked(userId, otherUserId);
        if (blocked) {
            return { updated: 0, blocked: true };
        }

        const match = await getMatchRecord(userId, otherUserId);
        if (!match) return { updated: 0 };

        const unread = await prisma.message.findMany({
            where: {
                matchId: match.id,
                senderId: otherUserId,
                isRead: false,
            },
            select: { id: true },
            take: 500,
        });
        if (unread.length === 0) return { updated: 0 };

        await prisma.message.updateMany({
            where: {
                id: { in: unread.map((item) => item.id) },
            },
            data: {
                isRead: true,
            },
        });

        await Promise.all(
            unread.map((item) => writeMessageStatusAudit(userId, item.id, 'seen', { conversationWith: otherUserId })),
        );

        return {
            updated: unread.length,
        };
    },

    async markConversationDelivered(userId, otherUserId) {
        const blocked = await safetyService.isCommunicationBlocked(userId, otherUserId);
        if (blocked) {
            return { updated: 0, blocked: true };
        }

        const match = await getMatchRecord(userId, otherUserId);
        if (!match) return { updated: 0 };

        const undelivered = await prisma.message.findMany({
            where: {
                matchId: match.id,
                senderId: otherUserId,
                isRead: false,
            },
            select: { id: true },
            take: 500,
        });
        if (undelivered.length === 0) return { updated: 0 };

        await Promise.all(
            undelivered.map((item) => writeMessageStatusAudit(userId, item.id, 'delivered', { conversationWith: otherUserId })),
        );

        return {
            updated: undelivered.length,
        };
    },

    async getMessages(user1Id, user2Id, options = {}) {
        const communicationBlocked = await safetyService.isCommunicationBlocked(user1Id, user2Id);
        if (communicationBlocked) return [];

        const match = await getMatchRecord(user1Id, user2Id);
        if (!match) return [];

        const take = Math.min(Math.max(Number.parseInt(options.limit, 10) || 50, 1), 200);
        const skip = Math.max(Number.parseInt(options.skip, 10) || 0, 0);

        const messagesDesc = await prisma.message.findMany({
            where: { matchId: match.id },
            orderBy: { createdAt: 'desc' },
            skip,
            take,
        });
        const messages = messagesDesc.reverse();
        const statusMap = await fetchLatestMessageStatuses(messages.map((message) => message.id));

        return messages.map((message) => ({
            ...message,
            status: statusMap.get(message.id) || (message.isRead ? 'seen' : 'sent'),
        }));
    },

    async getConnectedUsers(userId, options = {}) {
        const take = Math.min(Math.max(Number.parseInt(options.limit, 10) || 20, 1), 50);
        const skip = Math.max(Number.parseInt(options.skip, 10) || 0, 0);
        const matches = await prisma.match.findMany({
            where: {
                OR: [{ userAId: userId }, { userBId: userId }],
            },
            orderBy: [{ lastMessageAt: 'desc' }, { createdAt: 'desc' }],
            skip,
            take,
            include: {
                userA: {
                    select: {
                        id: true,
                        profile: {
                            select: {
                                firstName: true,
                                lastName: true,
                                city: true,
                                profession: true,
                            },
                        },
                        photos: {
                            where: { isPrimary: true },
                            select: { photoUrl: true, thumbnailUrl: true },
                            take: 1,
                        },
                    },
                },
                userB: {
                    select: {
                        id: true,
                        profile: {
                            select: {
                                firstName: true,
                                lastName: true,
                                city: true,
                                profession: true,
                            },
                        },
                        photos: {
                            where: { isPrimary: true },
                            select: { photoUrl: true, thumbnailUrl: true },
                            take: 1,
                        },
                    },
                },
                messages: {
                    orderBy: { createdAt: 'desc' },
                    select: { content: true, createdAt: true },
                    take: 1,
                },
            },
        });

        const mapped = matches.map((match) => {
            const otherUser = match.userAId === userId ? match.userB : match.userA;
            const latestMessage = match.messages?.[0];
            const fullName = `${otherUser.profile?.firstName || ''} ${otherUser.profile?.lastName || ''}`.trim();
            return {
                userId: otherUser.id,
                name: fullName || 'User',
                photo: otherUser.photos?.[0]?.thumbnailUrl || otherUser.photos?.[0]?.photoUrl || null,
                role: otherUser.profile?.profession || null,
                city: otherUser.profile?.city || null,
                lastMessage: latestMessage?.content || '',
                updatedAt: latestMessage?.createdAt || match.lastMessageAt || match.createdAt,
                matchId: match.id,
            };
        });

        const allowed = await Promise.all(mapped.map(async (item) => {
            const blocked = await safetyService.isCommunicationBlocked(userId, item.userId);
            return blocked ? null : item;
        }));

        return allowed.filter(Boolean);
    },
};

module.exports = messageService;
