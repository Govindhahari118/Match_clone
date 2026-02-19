const prisma = require('../config/prisma');

const incomeBands = ['below_5L', '5-10L', '10-25L', '25-50L', '50L+'];

const interactionService = {
    // ── Like / Interest ──────────────────────────────────────────────────────
    async likeUser(senderId, receiverId) {
        const incomingLike = await prisma.like.findFirst({
            where: { senderId: receiverId, receiverId: senderId }
        });

        await prisma.like.upsert({
            where: { senderId_receiverId: { senderId, receiverId } },
            update: { status: 'sent' },
            create: { senderId, receiverId, status: 'sent' }
        });

        let isMatch = false;
        let matchRecord = null;

        if (incomingLike) {
            isMatch = true;
            const existing = await prisma.match.findFirst({
                where: { OR: [{ userAId: senderId, userBId: receiverId }, { userAId: receiverId, userBId: senderId }] }
            });
            matchRecord = existing || await prisma.match.create({ data: { userAId: senderId, userBId: receiverId } });
            // Update both like statuses to "accepted"
            await prisma.like.updateMany({
                where: { OR: [{ senderId, receiverId }, { senderId: receiverId, receiverId: senderId }] },
                data: { status: 'accepted' }
            });
        }

        return { status: 'liked', isMatch, matchId: matchRecord?.id };
    },

    // ── Reject / Decline ─────────────────────────────────────────────────────
    async rejectUser(senderId, receiverId) {
        // Upsert so repeated passes don't create duplicates
        await prisma.like.upsert({
            where: { senderId_receiverId: { senderId, receiverId } },
            update: { status: 'rejected' },
            create: { senderId, receiverId, status: 'rejected' }
        });
        return { status: 'rejected' };
    },

    // ── Decline (receiver declines incoming interest) ─────────────────────────
    async declineInterest(receiverId, senderId) {
        await prisma.like.upsert({
            where: { senderId_receiverId: { senderId, receiverId } },
            update: { status: 'rejected' },
            create: { senderId, receiverId, status: 'rejected' }
        });
        return { status: 'declined' };
    },

    // ── Get Interests ─────────────────────────────────────────────────────────
    async getInterests(userId, type) {
        if (type === 'received') {
            const likes = await prisma.like.findMany({
                where: { receiverId: userId, status: { notIn: ['rejected', 'accepted'] } },
                orderBy: { createdAt: 'desc' },
                include: {
                    sender: {
                        select: { id: true, isVerified: true },
                        include: {
                            profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                            photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                        }
                    }
                }
            });
            return likes.map(l => _formatLikeUser(l.sender, l.createdAt));
        }

        if (type === 'sent') {
            const likes = await prisma.like.findMany({
                where: { senderId: userId },
                orderBy: { createdAt: 'desc' },
                include: {
                    receiver: {
                        select: { id: true, isVerified: true },
                        include: {
                            profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                            photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                        }
                    }
                }
            });
            return likes.map(l => ({ ..._formatLikeUser(l.receiver, l.createdAt), status: l.status, sentAt: _timeAgo(l.createdAt) }));
        }

        if (type === 'mutual') {
            const matches = await prisma.match.findMany({
                where: { OR: [{ userAId: userId }, { userBId: userId }], isActive: true },
                orderBy: { createdAt: 'desc' },
                include: {
                    userA: {
                        select: { id: true, isVerified: true },
                        include: {
                            profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                            photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                        }
                    },
                    userB: {
                        select: { id: true, isVerified: true },
                        include: {
                            profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                            photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                        }
                    }
                }
            });
            return matches.map(m => {
                const other = m.userAId === userId ? m.userB : m.userA;
                return { ..._formatLikeUser(other, m.createdAt), matchedAt: _timeAgo(m.createdAt) };
            });
        }

        return [];
    },

    // ── Report / Block ────────────────────────────────────────────────────────
    async reportUser(reporterId, reportedUserId, reportType, description) {
        const report = await prisma.report.create({
            data: { reporterId, reportedUserId, reportType, description: description || null }
        });
        return { success: true, reportId: report.id };
    },

    // ── Compatibility Score ───────────────────────────────────────────────────
    async getCompatibilityScore(userId, targetProfileId) {
        const [myPrefs, myProfile, target] = await Promise.all([
            prisma.partnerPreference.findUnique({ where: { userId } }),
            prisma.profile.findUnique({ where: { userId } }),
            prisma.profile.findFirst({ where: { id: targetProfileId } })
        ]);
        if (!target || !myProfile) return 60; // default fallback

        let score = 50;
        if (!myPrefs) return score;

        // Age match
        const age = new Date().getFullYear() - new Date(target.dateOfBirth).getFullYear();
        if (age >= myPrefs.minAge && age <= myPrefs.maxAge) score += 15;

        // Religion match
        if (myPrefs.preferredReligions?.length > 0 && myPrefs.preferredReligions.includes(target.religion)) score += 10;
        if (myPrefs.religionOpen) score += 5;

        // Income match
        if (myPrefs.minIncomeBand && target.incomeBand) {
            const minIdx = incomeBands.indexOf(myPrefs.minIncomeBand);
            const targetIdx = incomeBands.indexOf(target.incomeBand);
            if (targetIdx >= minIdx) score += 10;
        }

        // Location match
        if (myPrefs.preferredLocations?.length > 0 && myPrefs.preferredLocations.some(l => l === target.city || l === target.state)) score += 10;

        return Math.min(score, 99);
    },

    // ── Profile Views ─────────────────────────────────────────────────────────
    async recordProfileView(viewerId, viewedUserId) {
        // We store in AuditLog as a cheap lightweight solution without a new migration
        await prisma.auditLog.create({
            data: { userId: viewerId, action: 'profile_view', resourceType: 'profile', resourceId: viewedUserId }
        });
    },

    async getProfileViewers(userId, limit = 20) {
        const views = await prisma.auditLog.findMany({
            where: { action: 'profile_view', resourceId: userId },
            orderBy: { createdAt: 'desc' },
            take: limit,
            distinct: ['userId'],
            include: {
                user: {
                    select: { id: true, isVerified: true },
                    include: {
                        profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                        photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                    }
                }
            }
        });
        return views.filter(v => v.user).map(v => _formatLikeUser(v.user, v.createdAt));
    }
};

// ── Helpers ──────────────────────────────────────────────────────────────────
function _formatLikeUser(user, date) {
    const p = user.profile;
    const age = p?.dateOfBirth ? new Date().getFullYear() - new Date(p.dateOfBirth).getFullYear() : null;
    return {
        id: user.id, userId: user.id,
        firstName: p?.firstName || 'Unknown',
        age,
        city: p?.city || '',
        profession: p?.profession || '',
        photo: user.photos?.[0]?.thumbnailUrl || user.photos?.[0]?.photoUrl || null,
        isVerified: user.isVerified,
        receivedAt: _timeAgo(date)
    };
}

function _timeAgo(date) {
    const diff = Date.now() - new Date(date).getTime();
    const m = Math.floor(diff / 60000);
    if (m < 60) return `${m || 1}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;
    return `${Math.floor(h / 24)}d ago`;
}

module.exports = interactionService;
