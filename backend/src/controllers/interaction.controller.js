const interactionService = require('../services/interaction.service');

const likeUser = async (req, res) => {
    try {
        const senderId = req.user.sub;
        const { receiverId } = req.body;
        const result = await interactionService.likeUser(senderId, receiverId);
        const io = req.app.get('io');
        if (result.isMatch && result.matchId) {
            io.to(senderId).emit('new_match', { matchId: result.matchId, userId: receiverId });
            io.to(receiverId).emit('new_match', { matchId: result.matchId, userId: senderId });
        } else if (result.status === 'liked') {
            io.to(receiverId).emit('new_like', { userId: senderId });
        }
        res.status(200).json(result);
    } catch (error) {
        console.error('Like user error:', error);
        res.status(500).json({ error: error.message });
    }
};

const acceptInterest = async (req, res) => {
    try {
        const receiverId = req.user.sub;
        const senderId = req.params.senderId || req.body.userId;
        const result = await interactionService.acceptInterest(receiverId, senderId);
        const io = req.app.get('io');
        if (result.matchId) {
            io.to(receiverId).emit('new_match', { matchId: result.matchId, userId: senderId });
            io.to(senderId).emit('new_match', { matchId: result.matchId, userId: receiverId });
        }
        res.status(200).json(result);
    } catch (error) {
        const status = ['INTEREST_NOT_FOUND'].includes(error.code) ? 404 :
            ['INVALID_INTEREST_STATE', 'BLOCKED', 'PROFILE_UNAVAILABLE', 'INVALID_TARGET'].includes(error.code) ? 409 : 500;
        res.status(status).json({ error: error.message, code: error.code || 'INTERACTION_ERROR' });
    }
};

const withdrawInterest = async (req, res) => {
    try {
        const senderId = req.user.sub;
        const receiverId = req.params.receiverId || req.body.userId;
        const result = await interactionService.withdrawInterest(senderId, receiverId);
        res.status(200).json(result);
    } catch (error) {
        const status = error.code === 'INVALID_INTEREST_STATE' ? 409 : 500;
        res.status(status).json({ error: error.message, code: error.code || 'INTERACTION_ERROR' });
    }
};

const blockUser = async (req, res) => {
    try {
        const blockerId = req.user.sub;
        const { userId } = req.body;
        const result = await interactionService.blockUser(blockerId, userId);
        res.status(200).json(result);
    } catch (error) {
        const status = ['INVALID_TARGET', 'PROFILE_UNAVAILABLE'].includes(error.code) ? 400 : 500;
        res.status(status).json({ error: error.message, code: error.code || 'INTERACTION_ERROR' });
    }
};

const unblockUser = async (req, res) => {
    try {
        const blockerId = req.user.sub;
        const blockedId = req.params.userId;
        const result = await interactionService.unblockUser(blockerId, blockedId);
        res.status(200).json(result);
    } catch (error) {
        res.status(500).json({ error: error.message, code: error.code || 'INTERACTION_ERROR' });
    }
};

const rejectUser = async (req, res) => {
    try {
        const senderId = req.user.sub;
        const { receiverId } = req.body;
        const result = await interactionService.rejectUser(senderId, receiverId);
        res.status(200).json(result);
    } catch (error) {
        console.error('Reject user error:', error);
        res.status(500).json({ error: error.message });
    }
};

const declineInterest = async (req, res) => {
    try {
        const receiverId = req.user.sub;
        const { userId } = req.body; // the sender whose interest we decline
        const result = await interactionService.declineInterest(receiverId, userId);
        res.status(200).json(result);
    } catch (error) {
        console.error('Decline interest error:', error);
        res.status(500).json({ error: error.message });
    }
};

const getInterests = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { type = 'received' } = req.query;
        const data = await interactionService.getInterests(userId, type);
        res.status(200).json(data);
    } catch (error) {
        console.error('Get interests error:', error);
        res.status(500).json({ error: error.message });
    }
};

const reportUser = async (req, res) => {
    try {
        const reporterId = req.user.sub;
        const { reportedUserId, reportType, description } = req.body;
        if (!reportedUserId || !reportType) return res.status(400).json({ error: 'reportedUserId and reportType are required' });
        const result = await interactionService.reportUser(reporterId, reportedUserId, reportType, description);
        res.status(200).json(result);
    } catch (error) {
        console.error('Report user error:', error);
        res.status(500).json({ error: error.message });
    }
};

const getProfileViewers = async (req, res) => {
    try {
        const userId = req.user.sub;
        const viewers = await interactionService.getProfileViewers(userId);
        res.status(200).json(viewers);
    } catch (error) {
        console.error('Get profile viewers error:', error);
        res.status(500).json({ error: error.message });
    }
};

const prisma = require('../config/prisma');
const horoscopeService = require('../services/horoscope.service');

const getHoroscopeMatch = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { targetUserId } = req.params;

        const [userProfile, targetProfile] = await Promise.all([
            prisma.profile.findUnique({ where: { userId } }),
            prisma.profile.findUnique({ where: { userId: targetUserId } })
        ]);

        if (!userProfile || !targetProfile) {
            return res.status(404).json({ error: 'Profiles not found' });
        }

        const matchData = horoscopeService.calculateCompatibility(userProfile, targetProfile);
        res.json(matchData);
    } catch (error) {
        console.error('Horoscope Error:', error);
        res.status(500).json({ error: 'Calculation failed' });
    }
};

module.exports = {
    likeUser,
    acceptInterest,
    withdrawInterest,
    rejectUser,
    declineInterest,
    blockUser,
    unblockUser,
    getInterests,
    reportUser,
    getProfileViewers,
    getHoroscopeMatch
};
