const { createClient } = require('redis');
const { createAdapter } = require('@socket.io/redis-adapter');
const messageService = require('../services/message.service');
const { buildUserRoom, buildMatchRoom } = require('./socket.rooms');

const MESSAGE_RATE_WINDOW_MS = Number(process.env.SOCKET_MESSAGE_RATE_WINDOW_MS) || 10 * 1000;
const MESSAGE_RATE_LIMIT = Number(process.env.SOCKET_MESSAGE_RATE_LIMIT) || 12;
const MAX_MESSAGE_LENGTH = Number(process.env.SOCKET_MESSAGE_MAX_LENGTH) || 2000;

function nowIso() {
    return new Date().toISOString();
}

function normalizeId(value) {
    const text = String(value || '').trim();
    return text || null;
}

function createRateLimiter() {
    const buckets = new Map();

    function check(key, limit = MESSAGE_RATE_LIMIT, windowMs = MESSAGE_RATE_WINDOW_MS) {
        const now = Date.now();
        const existing = buckets.get(key) || [];
        const recent = existing.filter((timestamp) => now - timestamp < windowMs);
        if (recent.length >= limit) {
            const retryInMs = windowMs - (now - recent[0]);
            buckets.set(key, recent);
            return { limited: true, retryInMs };
        }
        recent.push(now);
        buckets.set(key, recent);
        return { limited: false, retryInMs: 0 };
    }

    return { check };
}

async function setupRedisAdapter(io) {
    const redisUrl = String(process.env.REDIS_URL || '').trim();
    if (!redisUrl) {
        return { enabled: false };
    }

    const pubClient = createClient({ url: redisUrl });
    const subClient = pubClient.duplicate();

    await Promise.all([pubClient.connect(), subClient.connect()]);
    io.adapter(createAdapter(pubClient, subClient));

    return { enabled: true, pubClient, subClient };
}

function initRealtime(io) {
    const connectedUsers = new Map();
    const rateLimiter = createRateLimiter();

    io.on('connection', (socket) => {
        socket.on('join_room', (userId) => {
            const normalizedUserId = normalizeId(userId);
            if (!normalizedUserId) {
                return;
            }

            socket.data.userId = normalizedUserId;
            socket.join(buildUserRoom(normalizedUserId));

            const activeSockets = connectedUsers.get(normalizedUserId) || new Set();
            activeSockets.add(socket.id);
            connectedUsers.set(normalizedUserId, activeSockets);

            io.to(buildUserRoom(normalizedUserId)).emit('presence_update', {
                userId: normalizedUserId,
                isOnline: true,
            });
        });

        socket.on('join_match', async (payload = {}) => {
            const userId = normalizeId(socket.data.userId);
            const matchId = normalizeId(payload.matchId);
            if (!userId || !matchId) {
                return;
            }

            try {
                const allowed = await messageService.isUserInMatch(userId, matchId);
                if (!allowed) {
                    return;
                }
            } catch (error) {
                console.error('join_match validation failed', error);
                return;
            }

            socket.join(buildMatchRoom(matchId));
        });

        socket.on('leave_match', (payload = {}) => {
            const matchId = normalizeId(payload.matchId);
            if (!matchId) return;
            socket.leave(buildMatchRoom(matchId));
        });

        socket.on('send_message', async (data = {}) => {
            const socketUserId = normalizeId(socket.data.userId);
            const payloadSenderId = normalizeId(data.senderId);
            const senderId = socketUserId || payloadSenderId;
            const receiverId = normalizeId(data.receiverId);
            const content = typeof data.content === 'string' ? data.content.trim() : '';
            const clientMessageId = normalizeId(data.clientMessageId);

            const ack = (payload) => {
                if (senderId) {
                    io.to(buildUserRoom(senderId)).emit('message_ack', payload);
                } else {
                    socket.emit('message_ack', payload);
                }
            };

            if (!socketUserId) {
                ack({
                    clientMessageId,
                    status: 'failed',
                    error: 'Join room before sending messages',
                    serverTs: nowIso(),
                });
                return;
            }

            if (payloadSenderId && payloadSenderId !== socketUserId) {
                ack({
                    clientMessageId,
                    status: 'failed',
                    error: 'Sender mismatch',
                    serverTs: nowIso(),
                });
                return;
            }

            if (!senderId || !receiverId) {
                ack({
                    clientMessageId,
                    status: 'failed',
                    error: 'senderId and receiverId are required',
                    serverTs: nowIso(),
                });
                return;
            }

            if (senderId === receiverId) {
                ack({
                    clientMessageId,
                    status: 'failed',
                    error: 'Cannot send message to yourself',
                    serverTs: nowIso(),
                });
                return;
            }

            if (!content) {
                ack({
                    clientMessageId,
                    status: 'failed',
                    error: 'Message content empty',
                    serverTs: nowIso(),
                });
                return;
            }

            if (content.length > MAX_MESSAGE_LENGTH) {
                ack({
                    clientMessageId,
                    status: 'failed',
                    error: `Message exceeds ${MAX_MESSAGE_LENGTH} characters`,
                    serverTs: nowIso(),
                });
                return;
            }

            const rate = rateLimiter.check(senderId);
            if (rate.limited) {
                ack({
                    clientMessageId,
                    status: 'failed',
                    error: 'Rate limit exceeded',
                    retryInMs: rate.retryInMs,
                    serverTs: nowIso(),
                });
                return;
            }

            try {
                const saved = await messageService.saveMessage(senderId, receiverId, content, { clientMessageId });
                const matchId = saved.matchId;
                const matchRoom = matchId ? buildMatchRoom(matchId) : null;

                if (matchRoom) {
                    socket.join(matchRoom);
                    io.in(buildUserRoom(receiverId)).socketsJoin(matchRoom);
                    io.in(buildUserRoom(senderId)).socketsJoin(matchRoom);
                }

                const payload = {
                    id: saved.id,
                    matchId: saved.matchId,
                    senderId,
                    receiverId,
                    content: saved.content,
                    createdAt: saved.createdAt,
                    status: saved.status || 'sent',
                    clientMessageId: clientMessageId || null,
                };

                if (matchRoom) {
                    io.to(matchRoom).emit('receive_message', payload);
                } else {
                    io.to(buildUserRoom(receiverId)).emit('receive_message', payload);
                }

                ack({
                    clientMessageId,
                    messageId: saved.id,
                    status: saved.status || 'sent',
                    createdAt: saved.createdAt,
                    matchId: saved.matchId,
                    serverTs: nowIso(),
                });

                const receiverSockets = connectedUsers.get(receiverId);
                if (receiverSockets && receiverSockets.size > 0) {
                    await messageService.updateMessageStatus(receiverId, saved.id, 'delivered', {
                        source: 'socket_realtime_delivery',
                    });
                    const statusPayload = {
                        messageId: saved.id,
                        status: 'delivered',
                        receiverId,
                        serverTs: nowIso(),
                    };
                    io.to(buildUserRoom(senderId)).emit('message_status', statusPayload);
                    if (matchRoom) {
                        io.to(matchRoom).emit('message_status', statusPayload);
                    }
                }
            } catch (error) {
                ack({
                    clientMessageId,
                    status: 'failed',
                    error: error.message,
                    serverTs: nowIso(),
                });
            }
        });

        socket.on('typing', (data = {}) => {
            const senderId = normalizeId(socket.data.userId || data.senderId);
            const receiverId = normalizeId(data.receiverId);
            if (!senderId || !receiverId) return;
            io.to(buildUserRoom(receiverId)).emit('typing', {
                senderId,
                receiverId,
                isTyping: Boolean(data.isTyping),
                ts: nowIso(),
            });
        });

        socket.on('conversation_seen', async (data = {}) => {
            const viewerId = normalizeId(socket.data.userId || data.viewerId);
            const peerId = normalizeId(data.peerId);
            if (!viewerId || !peerId) return;
            try {
                const seenUpdate = await messageService.markConversationSeen(viewerId, peerId);
                if (seenUpdate.updated > 0) {
                    io.to(buildUserRoom(peerId)).emit('conversation_seen', {
                        viewerId,
                        updated: seenUpdate.updated,
                    });
                }
            } catch (error) {
                console.error('conversation_seen handler error', error);
            }
        });

        socket.on('disconnect', () => {
            const userId = socket.data.userId;
            if (userId) {
                const activeSockets = connectedUsers.get(userId);
                if (activeSockets) {
                    activeSockets.delete(socket.id);
                    if (activeSockets.size === 0) {
                        connectedUsers.delete(userId);
                        io.emit('presence_update', { userId, isOnline: false });
                    } else {
                        connectedUsers.set(userId, activeSockets);
                    }
                }
            }
        });
    });

    return { connectedUsers };
}

module.exports = {
    initRealtime,
    setupRedisAdapter,
};
