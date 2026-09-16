const prisma = require('../config/prisma');
const safetyService = require('./safety.service');
const exposureService = require('./exposure.service');
const trustService = require('./trust.service');

const incomeBands = ['below_5L', '5-10L', '10-25L', '25-50L', '50L+'];
const PUBLIC_USER_SELECT = {
    id: true,
    isVerified: true,
    lastActiveAt: true,
    lastLogin: true,
    searchStatus: true,
    profile: {
        select: {
            firstName: true,
            lastName: true,
            dateOfBirth: true,
            city: true,
            profession: true,
            role: true,
        },
    },
    photos: {
        where: { isPrimary: true, moderationStatus: 'approved' },
        take: 1,
        select: { photoUrl: true, thumbnailUrl: true },
    },
};

function appError(message, statusCode = 400) {
    const error = new Error(message);
    error.statusCode = statusCode;
    return error;
}

async function assertInteractionTarget(senderId, receiverId) {
    if (!receiverId || senderId === receiverId) throw appError('Invalid profile interaction');
    await safetyService.ensureNotBlocked(senderId, receiverId);
    const receiver = await prisma.user.findUnique({
        where: { id: receiverId },
        select: { id: true, isActive: true, isBanned: true, deletedAt: true, searchStatus: true },
    });
    if (
        !receiver ||
        !receiver.isActive ||
        receiver.isBanned ||
        receiver.deletedAt ||
        !['active', 'low_activity'].includes(receiver.searchStatus)
    ) {
        throw appError('Profile is unavailable', 404);
    }
    return receiver;
}

const interactionService = {
    async likeUser(senderId, receiverId) {
        await assertInteractionTarget(senderId, receiverId);

        const result = await prisma.$transaction(async (tx) => {
            const incomingLike = await tx.like.findFirst({
                where: { senderId: receiverId, receiverId: senderId, status: 'sent' },
            });

            await tx.like.upsert({
                where: { senderId_receiverId: { senderId, receiverId } },
                update: { status: incomingLike ? 'accepted' : 'sent' },
                create: { senderId, receiverId, status: incomingLike ? 'accepted' : 'sent' },
            });

            let matchRecord = null;
            if (incomingLike) {
                const existing = await tx.match.findFirst({
                    where: {
                        OR: [
                            { userAId: senderId, userBId: receiverId },
                            { userAId: receiverId, userBId: senderId },
                        ],
                    },
                });
                matchRecord = existing
                    ? await tx.match.update({ where: { id: existing.id }, data: { isActive: true } })
                    : await tx.match.create({ data: { userAId: senderId, userBId: receiverId } });

                await tx.like.updateMany({
                    where: {
                        OR: [
                            { senderId, receiverId },
                            { senderId: receiverId, receiverId: senderId },
                        ],
                    },
                    data: { status: 'accepted' },
                });
            }

            return {
                status: incomingLike ? 'accepted' : 'sent',
                isMatch: Boolean(matchRecord),
                matchId: matchRecord?.id || null,
            };
        });

        await Promise.all([
            exposureService.setInteractionState(senderId, receiverId, result.isMatch ? 'connected' : 'interest_sent'),
            trustService.recordActivity(senderId),
        ]);
        if (result.isMatch) await exposureService.setInteractionState(receiverId, senderId, 'connected');
        return result;
    },

    async rejectUser(senderId, receiverId) {
        await assertInteractionTarget(senderId, receiverId);
        await prisma.like.upsert({
            where: { senderId_receiverId: { senderId, receiverId } },
            update: { status: 'rejected' },
            create: { senderId, receiverId, status: 'rejected' },
        });
        await Promise.all([
            exposureService.setInteractionState(senderId, receiverId, 'hidden'),
            trustService.recordActivity(senderId),
        ]);
        return { status: 'rejected' };
    },

    async declineInterest(receiverId, senderId) {
        await safetyService.ensureNotBlocked(receiverId, senderId);
        await prisma.like.updateMany({
            where: { senderId, receiverId, status: 'sent' },
            data: { status: 'rejected' },
        });
        await trustService.recordActivity(receiverId);
        return { status: 'declined' };
    },

    async withdrawInterest(senderId, receiverId) {
        await prisma.like.updateMany({
            where: { senderId, receiverId, status: 'sent' },
            data: { status: 'withdrawn' },
        });
        return { status: 'withdrawn' };
    },

    async getInterests(userId, type) {
        const blockedRows = await prisma.userBlock.findMany({
            where: { OR: [{ blockerId: userId }, { blockedUserId: userId }] },
            select: { blockerId: true, blockedUserId: true },
        });
        const blockedIds = new Set(
            blockedRows.map((row) => row.blockerId === userId ? row.blockedUserId : row.blockerId)
        );

        if (type === 'received') {
            const likes = await prisma.like.findMany({
                where: { receiverId: userId, status: 'sent' },
                orderBy: { createdAt: 'desc' },
                include: { sender: { select: PUBLIC_USER_SELECT } },
            });
            return likes
                .filter((like) => !blockedIds.has(like.senderId))
                .map((like) => _formatLikeUser(like.sender, like.createdAt));
        }

        if (type === 'sent') {
            const likes = await prisma.like.findMany({
                where: { senderId: userId },
                orderBy: { createdAt: 'desc' },
                include: { receiver: { select: PUBLIC_USER_SELECT } },
            });
            return likes
                .filter((like) => !blockedIds.has(like.receiverId))
                .map((like) => ({
                    ..._formatLikeUser(like.receiver, like.createdAt),
                    status: like.status,
                    sentAt: _timeAgo(like.createdAt),
                }));
        }

        if (type === 'mutual') {
            const matches = await prisma.match.findMany({
                where: { OR: [{ userAId: userId }, { userBId: userId }], isActive: true },
                orderBy: { createdAt: 'desc' },
                include: {
                    userA: { select: PUBLIC_USER_SELECT },
                    userB: { select: PUBLIC_USER_SELECT },
                },
            });
            return matches
                .map((match) => {
                    const other = match.userAId === userId ? match.userB : match.userA;
                    if (blockedIds.has(other.id)) return null;
                    return { ..._formatLikeUser(other, match.createdAt), matchedAt: _timeAgo(match.createdAt) };
                })
                .filter(Boolean);
        }

        return [];
    },

    async reportUser(reporterId, reportedUserId, reportType, description) {
        await assertInteractionTarget(reporterId, reportedUserId);
        const report = await safetyService.createReport({ reporterId, reportedUserId, reportType, description });
        return { success: true, reportId: report.id, status: report.status, severity: report.severity };
    },

    async blockUser(userId, targetId, reason) {
        return safetyService.blockUser(userId, targetId, reason);
    },

    async unblockUser(userId, targetId) {
        return safetyService.unblockUser(userId, targetId);
    },

    async requestContact(userId, targetId) {
        return safetyService.requestContact(userId, targetId);
    },

    async respondContact(userId, requestId, action) {
        return safetyService.respondContactRequest(userId, requestId, action);
    },

    async requestPhotoAccess(userId, targetId) {
        return safetyService.requestPhotoAccess(userId, targetId);
    },

    async respondPhotoAccess(userId, requestId, action) {
        return safetyService.respondPhotoAccess(userId, requestId, action);
    },

    async getCompatibilitySummary(userId, targetUserId) {
        const [myPrefs, myProfile, targetProfile] = await Promise.all([
            prisma.partnerPreference.findUnique({ where: { userId } }),
            prisma.profile.findUnique({ where: { userId } }),
            prisma.profile.findUnique({ where: { userId: targetUserId } }),
        ]);
        if (!myProfile || !targetProfile) return { score: null, strength: 'unknown', reasons: [] };

        const reasons = [];
        let earned = 0;
        let possible = 0;
        if (myPrefs) {
            const targetAge = _age(targetProfile.dateOfBirth);
            possible += 25;
            if (targetAge !== null && targetAge >= myPrefs.minAge && targetAge <= myPrefs.maxAge) {
                earned += 25;
                reasons.push('Age preference matches');
            }
            if (myPrefs.preferredLocations?.length) {
                possible += 20;
                if (myPrefs.preferredLocations.some((value) => value === targetProfile.city || value === targetProfile.state)) {
                    earned += 20;
                    reasons.push('Location preference matches');
                }
            }
            if (!myPrefs.religionOpen && myPrefs.preferredReligions?.length) {
                possible += 20;
                if (myPrefs.preferredReligions.includes(targetProfile.religion)) {
                    earned += 20;
                    reasons.push('Religion preference matches');
                }
            } else if (myPrefs.preferredReligions?.length && myPrefs.preferredReligions.includes(targetProfile.religion)) {
                possible += 10;
                earned += 10;
                reasons.push('Religion preference aligns');
            }
            if (myPrefs.minIncomeBand && targetProfile.incomeBand) {
                possible += 15;
                const minIndex = incomeBands.indexOf(myPrefs.minIncomeBand);
                const targetIndex = incomeBands.indexOf(targetProfile.incomeBand);
                if (minIndex >= 0 && targetIndex >= minIndex) {
                    earned += 15;
                    reasons.push('Income preference matches');
                }
            }
        }

        possible += 20;
        if (myProfile.city && targetProfile.city && myProfile.city.toLowerCase() === targetProfile.city.toLowerCase()) {
            earned += 20;
            reasons.push('Same city');
        }
        const score = possible > 0 ? Math.round((earned / possible) * 100) : null;
        const strength = score === null ? 'unknown' : score >= 75 ? 'strong' : score >= 50 ? 'good' : 'partial';
        return { score, strength, reasons: Array.from(new Set(reasons)).slice(0, 4) };
    },

    async getCompatibilityScore(userId, targetProfileId) {
        const target = await prisma.profile.findUnique({ where: { id: targetProfileId }, select: { userId: true } });
        if (!target) return null;
        return (await this.getCompatibilitySummary(userId, target.userId)).score;
    },

    async recordProfileView(viewerId, viewedUserId) {
        if (!viewerId || !viewedUserId || viewerId === viewedUserId) return;
        await safetyService.ensureNotBlocked(viewerId, viewedUserId);
        await Promise.all([
            prisma.auditLog.create({
                data: { userId: viewerId, action: 'profile_view', resourceType: 'profile', resourceId: viewedUserId },
            }),
            exposureService.recordExposure(viewerId, viewedUserId, 'seen'),
            trustService.recordActivity(viewerId),
        ]);
    },

    async getProfileViewers(userId, limit = 20) {
        const [views, blockedRows] = await Promise.all([
            prisma.auditLog.findMany({
                where: { action: 'profile_view', resourceId: userId },
                orderBy: { createdAt: 'desc' },
                take: limit * 2,
                distinct: ['userId'],
                include: { user: { select: PUBLIC_USER_SELECT } },
            }),
            prisma.userBlock.findMany({
                where: { OR: [{ blockerId: userId }, { blockedUserId: userId }] },
                select: { blockerId: true, blockedUserId: true },
            }),
        ]);
        const blockedIds = new Set(
            blockedRows.map((row) => row.blockerId === userId ? row.blockedUserId : row.blockerId)
        );
        return views
            .filter((view) => view.user && !blockedIds.has(view.user.id))
            .slice(0, limit)
            .map((view) => _formatLikeUser(view.user, view.createdAt));
    },
};

function _age(dateOfBirth) {
    if (!dateOfBirth) return null;
    const now = new Date();
    const dob = new Date(dateOfBirth);
    let age = now.getFullYear() - dob.getFullYear();
    const monthDelta = now.getMonth() - dob.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < dob.getDate())) age -= 1;
    return age;
}

function _formatLikeUser(user, date) {
    const profile = user.profile;
    return {
        id: user.id,
        userId: user.id,
        firstName: profile?.firstName || 'Unknown',
        age: _age(profile?.dateOfBirth),
        city: profile?.city || '',
        profession: profile?.profession || '',
        managedBy: profile?.role || 'self',
        photo: user.photos?.[0]?.thumbnailUrl || user.photos?.[0]?.photoUrl || null,
        isVerified: user.isVerified,
        activity: trustService.activityBucket(user),
        searchStatus: user.searchStatus,
        receivedAt: _timeAgo(date),
    };
}

function _timeAgo(date) {
    const diff = Date.now() - new Date(date).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 60) return `${minutes || 1}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
}

module.exports = interactionService;
