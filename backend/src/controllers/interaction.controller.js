const interactionService = require('../services/interaction.service');
const prisma = require('../config/prisma');
const horoscopeService = require('../services/horoscope.service');

function sendError(res, error, fallback = 'Request failed') {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? fallback : error.message });
}

const likeUser = async (req, res) => {
    try {
        const senderId = req.user.sub;
        const { receiverId } = req.body;
        const result = await interactionService.likeUser(senderId, receiverId);
        const io = req.app.get('io');
        if (result.isMatch && result.matchId) {
            io.to(senderId).emit('new_match', { matchId: result.matchId, userId: receiverId });
            io.to(receiverId).emit('new_match', { matchId: result.matchId, userId: senderId });
        } else if (result.status === 'sent') {
            io.to(receiverId).emit('new_like', { userId: senderId });
        }
        return res.status(200).json(result);
    } catch (error) {
        console.error('Like user error:', error);
        return sendError(res, error, 'Unable to send interest');
    }
};

const rejectUser = async (req, res) => {
    try {
        const result = await interactionService.rejectUser(req.user.sub, req.body?.receiverId);
        return res.status(200).json(result);
    } catch (error) {
        return sendError(res, error, 'Unable to hide profile');
    }
};

const declineInterest = async (req, res) => {
    try {
        const result = await interactionService.declineInterest(req.user.sub, req.body?.userId);
        return res.status(200).json(result);
    } catch (error) {
        return sendError(res, error, 'Unable to decline interest');
    }
};

const withdrawInterest = async (req, res) => {
    try {
        const result = await interactionService.withdrawInterest(req.user.sub, req.body?.receiverId);
        return res.json(result);
    } catch (error) {
        return sendError(res, error, 'Unable to withdraw interest');
    }
};

const getInterests = async (req, res) => {
    try {
        const data = await interactionService.getInterests(req.user.sub, req.query.type || 'received');
        return res.status(200).json(data);
    } catch (error) {
        return sendError(res, error, 'Unable to load interests');
    }
};

const reportUser = async (req, res) => {
    try {
        const { reportedUserId, reportType, description } = req.body || {};
        if (!reportedUserId || !reportType) {
            return res.status(400).json({ error: 'reportedUserId and reportType are required' });
        }
        const result = await interactionService.reportUser(req.user.sub, reportedUserId, reportType, description);
        return res.status(201).json(result);
    } catch (error) {
        return sendError(res, error, 'Unable to submit report');
    }
};

const blockUser = async (req, res) => {
    try {
        const row = await interactionService.blockUser(req.user.sub, req.params.userId, req.body?.reason);
        return res.status(201).json({ success: true, blockId: row.id });
    } catch (error) {
        return sendError(res, error, 'Unable to block profile');
    }
};

const unblockUser = async (req, res) => {
    try {
        return res.json(await interactionService.unblockUser(req.user.sub, req.params.userId));
    } catch (error) {
        return sendError(res, error, 'Unable to unblock profile');
    }
};

const requestContact = async (req, res) => {
    try {
        return res.status(201).json(await interactionService.requestContact(req.user.sub, req.params.userId));
    } catch (error) {
        return sendError(res, error, 'Unable to request contact');
    }
};

const respondContact = async (req, res) => {
    try {
        return res.json(await interactionService.respondContact(req.user.sub, req.params.requestId, req.body?.action));
    } catch (error) {
        return sendError(res, error, 'Unable to update contact request');
    }
};

const requestPhotoAccess = async (req, res) => {
    try {
        return res.status(201).json(await interactionService.requestPhotoAccess(req.user.sub, req.params.userId));
    } catch (error) {
        return sendError(res, error, 'Unable to request photo access');
    }
};

const respondPhotoAccess = async (req, res) => {
    try {
        return res.json(await interactionService.respondPhotoAccess(req.user.sub, req.params.requestId, req.body?.action));
    } catch (error) {
        return sendError(res, error, 'Unable to update photo access');
    }
};

const getProfileViewers = async (req, res) => {
    try {
        const viewers = await interactionService.getProfileViewers(req.user.sub);
        return res.status(200).json(viewers);
    } catch (error) {
        return sendError(res, error, 'Unable to load profile viewers');
    }
};

const getHoroscopeMatch = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { targetUserId } = req.params;
        const [userProfile, targetProfile] = await Promise.all([
            prisma.profile.findUnique({ where: { userId } }),
            prisma.profile.findUnique({ where: { userId: targetUserId } }),
        ]);
        if (!userProfile || !targetProfile) return res.status(404).json({ error: 'Profiles not found' });
        return res.json(horoscopeService.calculateCompatibility(userProfile, targetProfile));
    } catch (error) {
        return sendError(res, error, 'Horoscope calculation failed');
    }
};

module.exports = {
    likeUser,
    rejectUser,
    declineInterest,
    withdrawInterest,
    getInterests,
    reportUser,
    blockUser,
    unblockUser,
    requestContact,
    respondContact,
    requestPhotoAccess,
    respondPhotoAccess,
    getProfileViewers,
    getHoroscopeMatch,
};
