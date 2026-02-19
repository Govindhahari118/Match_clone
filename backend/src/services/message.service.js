const prisma = require('../config/prisma');

const messageService = {
    // Check if users are matched
    async areUsersMatched(user1Id, user2Id) {
        const match = await prisma.match.findFirst({
            where: {
                OR: [
                    { userAId: user1Id, userBId: user2Id },
                    { userAId: user2Id, userBId: user1Id }
                ]
            }
        });
        return !!match;
    },

    // Save a new message
    async saveMessage(senderId, receiverId, content) {
        if (!content) throw new Error("Message content empty");

        // Find match first to link message
        const match = await prisma.match.findFirst({
            where: {
                OR: [
                    { userAId: senderId, userBId: receiverId },
                    { userAId: receiverId, userBId: senderId }
                ]
            }
        });

        if (!match) throw new Error("No match found between users");

        return await prisma.message.create({
            data: {
                matchId: match.id,
                senderId,
                content,
                createdAt: new Date()
            }
        });
    },

    // Get chat history for a match/conversation
    async getMessages(user1Id, user2Id) {
        // Find match first
        const match = await prisma.match.findFirst({
            where: {
                OR: [
                    { userAId: user1Id, userBId: user2Id },
                    { userAId: user2Id, userBId: user1Id }
                ]
            }
        });

        if (!match) return [];

        return await prisma.message.findMany({
            where: { matchId: match.id },
            orderBy: { createdAt: 'asc' }
        });
    },

    // Get list of connected users (Matches) for the sidebar
    async getConnectedUsers(userId) {
        const matches = await prisma.match.findMany({
            where: {
                OR: [{ userAId: userId }, { userBId: userId }]
            },
            include: {
                userA: { select: { id: true, profile: { include: { photos: true } } } },
                userB: { select: { id: true, profile: { include: { photos: true } } } }
            }
        });

        // Format to return just the OTHER user
        return matches.map(match => {
            const otherUser = match.userAId === userId ? match.userB : match.userA;
            // Flatten structure slightly
            return {
                userId: otherUser.id,
                name: `${otherUser.profile?.firstName} ${otherUser.profile?.lastName}`,
                photo: otherUser.profile?.photos?.[0]?.thumbnailUrl || otherUser.profile?.photos?.[0]?.photoUrl,
                matchId: match.id
            };
        });
    }
};

module.exports = messageService;
