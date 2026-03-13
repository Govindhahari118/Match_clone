const interactionService = require('../services/interaction.service');
const safetyService = require('../services/safety.service');
const { parsePagination } = require('../utils/pagination');
const { buildUserRoom, buildMatchRoom } = require('../realtime/socket.rooms');

const likeUser = async (req, res) => {
    try {
        const senderId = req.user.sub;
        const { receiverId } = req.body;
        const result = await interactionService.likeUser(senderId, receiverId);
        const io = req.app.get('io');
        if (result.isMatch && result.matchId) {
            io.to(buildUserRoom(senderId)).emit('new_match', { matchId: result.matchId, userId: receiverId });
            io.to(buildUserRoom(receiverId)).emit('new_match', { matchId: result.matchId, userId: senderId });
            io.in(buildUserRoom(senderId)).socketsJoin(buildMatchRoom(result.matchId));
            io.in(buildUserRoom(receiverId)).socketsJoin(buildMatchRoom(result.matchId));
        } else if (result.status === 'liked') {
            io.to(buildUserRoom(receiverId)).emit('new_like', { userId: senderId });
        }
        res.status(200).json(result);
    } catch (error) {
        console.error('Like user error:', error);
        res.status(500).json({ error: error.message });
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
        const { limit, skip } = parsePagination(req.query, { defaultLimit: 20, maxLimit: 50 });
        const data = await interactionService.getInterests(userId, type, { limit, skip });
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
        const result = await interactionService.reportUser(reporterId, reportedUserId, reportType, description, {
            ipAddress: req.ip,
            userAgent: req.get('user-agent'),
            source: req.body?.source || 'interaction_report',
            channel: req.body?.channel || 'profile',
        });
        if (result.error) return res.status(result.statusCode || 400).json({ error: result.error });
        res.status(200).json(result);
    } catch (error) {
        console.error('Report user error:', error);
        res.status(500).json({ error: error.message });
    }
};

const applySafetyAction = async (req, res) => {
    try {
        const actorId = req.user.sub;
        const { targetUserId, action } = req.body || {};
        const result = await interactionService.applySafetyAction(actorId, targetUserId, action, {
            ipAddress: req.ip,
            userAgent: req.get('user-agent'),
        });
        if (result.error) return res.status(result.statusCode || 400).json({ error: result.error });
        return res.status(200).json(result);
    } catch (error) {
        console.error('Apply safety action error:', error);
        return res.status(500).json({ error: error.message });
    }
};

const getSafetyStatus = async (req, res) => {
    try {
        const actorId = req.user.sub;
        const targetUserId = String(req.query.targetUserId || '').trim();
        if (!targetUserId) return res.status(400).json({ error: 'targetUserId is required' });
        const status = await safetyService.getSafetyRelationship(actorId, targetUserId);
        return res.status(200).json(status);
    } catch (error) {
        console.error('Get safety status error:', error);
        return res.status(500).json({ error: error.message });
    }
};

const getProfileViewers = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { limit, skip } = parsePagination(req.query, { defaultLimit: 20, maxLimit: 50 });
        const viewers = await interactionService.getProfileViewers(userId, { limit, skip });
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

module.exports = { likeUser, rejectUser, declineInterest, getInterests, reportUser, applySafetyAction, getSafetyStatus, getProfileViewers, getHoroscopeMatch };
