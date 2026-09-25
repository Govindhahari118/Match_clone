const prisma = require('../config/prisma');

function domainError(message, code) {
    const error = new Error(message);
    error.code = code;
    return error;
}

async function assertPairEligible(userAId, userBId) {
    if (!userBId || userAId === userBId) throw domainError('Invalid target user', 'INVALID_TARGET');

    const [target, blocked] = await Promise.all([
        prisma.user.findUnique({ where: { id: userBId }, select: { id: true, isActive: true, isBanned: true } }),
        prisma.block.findFirst({
            where: {
                OR: [
                    { blockerId: userAId, blockedId: userBId },
                    { blockerId: userBId, blockedId: userAId }
                ]
            },
            select: { id: true }
        })
    ]);

    if (!target || !target.isActive || target.isBanned) throw domainError('Profile unavailable', 'PROFILE_UNAVAILABLE');
    if (blocked) throw domainError('Interaction unavailable', 'BLOCKED');
}

function ageFromDob(dateOfBirth) {
    if (!dateOfBirth) return null;
    const now = new Date();
    const dob = new Date(dateOfBirth);
    let age = now.getFullYear() - dob.getFullYear();
    const monthDelta = now.getMonth() - dob.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < dob.getDate())) age -= 1;
    return age;
}

const interactionService = {
    async likeUser(senderId, receiverId) {
        await assertPairEligible(senderId, receiverId);

        return prisma.$transaction(async tx => {
            const [outgoing, incoming] = await Promise.all([
                tx.like.findUnique({ where: { senderId_receiverId: { senderId, receiverId } } }),
                tx.like.findUnique({ where: { senderId_receiverId: { senderId: receiverId, receiverId: senderId } } })
            ]);

            if (outgoing?.status === 'accepted' || incoming?.status === 'accepted') {
                const existingMatch = await tx.match.findFirst({
                    where: { OR: [{ userAId: senderId, userBId: receiverId }, { userAId: receiverId, userBId: senderId }] }
                });
                return { status: 'accepted', isMatch: Boolean(existingMatch), matchId: existingMatch?.id || null, idempotent: true };
            }

            if (outgoing?.status === 'sent') {
                return { status: 'sent', isMatch: false, matchId: null, idempotent: true };
            }

            // A reciprocal send is treated as an explicit acceptance only when an incoming request is currently pending.
            if (incoming?.status === 'sent') {
                const matchRecord = await tx.match.findFirst({
                    where: { OR: [{ userAId: senderId, userBId: receiverId }, { userAId: receiverId, userBId: senderId }] }
                }) || await tx.match.create({ data: { userAId: senderId, userBId: receiverId, isActive: true } });

                await tx.like.update({
                    where: { id: incoming.id },
                    data: { status: 'accepted' }
                });

                await tx.like.upsert({
                    where: { senderId_receiverId: { senderId, receiverId } },
                    update: { status: 'accepted' },
                    create: { senderId, receiverId, status: 'accepted' }
                });

                return { status: 'accepted', isMatch: true, matchId: matchRecord.id };
            }

            await tx.like.upsert({
                where: { senderId_receiverId: { senderId, receiverId } },
                update: { status: 'sent' },
                create: { senderId, receiverId, status: 'sent' }
            });
            return { status: 'sent', isMatch: false, matchId: null };
        });
    },

    async acceptInterest(receiverId, senderId) {
        await assertPairEligible(receiverId, senderId);

        return prisma.$transaction(async tx => {
            const incoming = await tx.like.findUnique({
                where: { senderId_receiverId: { senderId, receiverId } }
            });
            if (!incoming) throw domainError('Pending interest not found', 'INTEREST_NOT_FOUND');

            if (incoming.status === 'accepted') {
                const existing = await tx.match.findFirst({
                    where: { OR: [{ userAId: senderId, userBId: receiverId }, { userAId: receiverId, userBId: senderId }] }
                });
                return { status: 'accepted', isMatch: Boolean(existing), matchId: existing?.id || null, idempotent: true };
            }
            if (incoming.status !== 'sent') throw domainError('Interest is not pending', 'INVALID_INTEREST_STATE');

            const match = await tx.match.findFirst({
                where: { OR: [{ userAId: senderId, userBId: receiverId }, { userAId: receiverId, userBId: senderId }] }
            }) || await tx.match.create({ data: { userAId: senderId, userBId: receiverId, isActive: true } });

            await tx.like.update({ where: { id: incoming.id }, data: { status: 'accepted' } });
            await tx.like.upsert({
                where: { senderId_receiverId: { senderId: receiverId, receiverId: senderId } },
                update: { status: 'accepted' },
                create: { senderId: receiverId, receiverId: senderId, status: 'accepted' }
            });

            return { status: 'accepted', isMatch: true, matchId: match.id };
        });
    },

    async declineInterest(receiverId, senderId) {
        const incoming = await prisma.like.findUnique({
            where: { senderId_receiverId: { senderId, receiverId } }
        });
        if (!incoming) throw domainError('Pending interest not found', 'INTEREST_NOT_FOUND');
        if (incoming.status === 'rejected') return { status: 'declined', idempotent: true };
        if (incoming.status !== 'sent') throw domainError('Interest is not pending', 'INVALID_INTEREST_STATE');

        await prisma.like.update({ where: { id: incoming.id }, data: { status: 'rejected' } });
        return { status: 'declined' };
    },

    async withdrawInterest(senderId, receiverId) {
        const outgoing = await prisma.like.findUnique({
            where: { senderId_receiverId: { senderId, receiverId } }
        });
        if (!outgoing) return { status: 'withdrawn', idempotent: true };
        if (outgoing.status === 'accepted') throw domainError('Accepted connection cannot be withdrawn as a pending interest', 'INVALID_INTEREST_STATE');
        if (outgoing.status === 'withdrawn') return { status: 'withdrawn', idempotent: true };
        if (outgoing.status !== 'sent') throw domainError('Interest is not pending', 'INVALID_INTEREST_STATE');

        await prisma.like.update({ where: { id: outgoing.id }, data: { status: 'withdrawn' } });
        return { status: 'withdrawn' };
    },

    async rejectUser(senderId, receiverId) {
        await assertPairEligible(senderId, receiverId);
        const existing = await prisma.like.findUnique({ where: { senderId_receiverId: { senderId, receiverId } } });
        if (existing?.status === 'accepted') throw domainError('Connected relationship cannot be rejected through discovery', 'INVALID_INTEREST_STATE');

        await prisma.like.upsert({
            where: { senderId_receiverId: { senderId, receiverId } },
            update: { status: 'rejected' },
            create: { senderId, receiverId, status: 'rejected' }
        });
        return { status: 'rejected' };
    },

    async blockUser(blockerId, blockedId) {
        if (!blockedId || blockerId === blockedId) throw domainError('Invalid target user', 'INVALID_TARGET');
        const target = await prisma.user.findUnique({ where: { id: blockedId }, select: { id: true } });
        if (!target) throw domainError('Profile unavailable', 'PROFILE_UNAVAILABLE');

        return prisma.$transaction(async tx => {
            const block = await tx.block.upsert({
                where: { blockerId_blockedId: { blockerId, blockedId } },
                update: {},
                create: { blockerId, blockedId }
            });

            await tx.match.updateMany({
                where: { OR: [{ userAId: blockerId, userBId: blockedId }, { userAId: blockedId, userBId: blockerId }] },
                data: { isActive: false }
            });
            await tx.like.updateMany({
                where: { OR: [{ senderId: blockerId, receiverId: blockedId }, { senderId: blockedId, receiverId: blockerId }] },
                data: { status: 'blocked' }
            });

            return { success: true, blockId: block.id };
        });
    },

    async unblockUser(blockerId, blockedId) {
        await prisma.block.deleteMany({ where: { blockerId, blockedId } });
        // Deliberately do not restore old matches, interests or grants.
        return { success: true };
    },

    async getInterests(userId, type) {
        const blockedPairs = await prisma.block.findMany({
            where: { OR: [{ blockerId: userId }, { blockedId: userId }] },
            select: { blockerId: true, blockedId: true }
        });
        const blockedIds = blockedPairs.map(b => b.blockerId === userId ? b.blockedId : b.blockerId);

        if (type === 'received') {
            const likes = await prisma.like.findMany({
                where: { receiverId: userId, senderId: { notIn: blockedIds }, status: 'sent' },
                orderBy: { createdAt: 'desc' },
                include: {
                    sender: {
                        select: { id: true, isVerified: true, isActive: true, isBanned: true },
                        include: {
                            profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                            photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                        }
                    }
                }
            });
            return likes.filter(l => l.sender.isActive && !l.sender.isBanned).map(l => _formatLikeUser(l.sender, l.createdAt));
        }

        if (type === 'sent') {
            const likes = await prisma.like.findMany({
                where: { senderId: userId, receiverId: { notIn: blockedIds } },
                orderBy: { createdAt: 'desc' },
                include: {
                    receiver: {
                        select: { id: true, isVerified: true, isActive: true, isBanned: true },
                        include: {
                            profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                            photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                        }
                    }
                }
            });
            return likes
                .filter(l => l.receiver.isActive && !l.receiver.isBanned)
                .map(l => ({ ..._formatLikeUser(l.receiver, l.createdAt), status: l.status, sentAt: _timeAgo(l.createdAt) }));
        }

        if (type === 'mutual') {
            const matches = await prisma.match.findMany({
                where: {
                    isActive: true,
                    OR: [{ userAId: userId, userBId: { notIn: blockedIds } }, { userBId: userId, userAId: { notIn: blockedIds } }]
                },
                orderBy: { createdAt: 'desc' },
                include: {
                    userA: {
                        select: { id: true, isVerified: true, isActive: true, isBanned: true },
                        include: {
                            profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                            photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                        }
                    },
                    userB: {
                        select: { id: true, isVerified: true, isActive: true, isBanned: true },
                        include: {
                            profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                            photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                        }
                    }
                }
            });
            return matches.map(m => {
                const other = m.userAId === userId ? m.userB : m.userA;
                if (!other?.isActive || other?.isBanned) return null;
                return { ..._formatLikeUser(other, m.createdAt), matchedAt: _timeAgo(m.createdAt) };
            }).filter(Boolean);
        }

        return [];
    },

    async reportUser(reporterId, reportedUserId, reportType, description) {
        if (!reportedUserId || reporterId === reportedUserId) throw domainError('Invalid report target', 'INVALID_TARGET');
        const allowed = new Set(['fake_identity','already_married','scam','money_request','harassment','offensive_content','wrong_information','stolen_photo','underage_concern','spam','other','fake_profile','inappropriate']);
        if (!allowed.has(String(reportType))) throw domainError('Invalid report category', 'INVALID_REPORT_CATEGORY');

        const report = await prisma.report.create({
            data: { reporterId, reportedUserId, reportType, description: description || null }
        });
        return { success: true, reportId: report.id, status: report.status };
    },

    async getCompatibilityExplanation(userId, targetProfileId) {
        const [myPrefs, myProfile, target] = await Promise.all([
            prisma.partnerPreference.findUnique({ where: { userId } }),
            prisma.profile.findUnique({ where: { userId } }),
            prisma.profile.findFirst({ where: { id: targetProfileId } })
        ]);
        if (!target || !myProfile) return null;

        const reasons = [];
        const mismatches = [];
        const targetAge = ageFromDob(target.dateOfBirth);

        if (myPrefs) {
            if (targetAge !== null) {
                if (targetAge >= myPrefs.minAge && targetAge <= myPrefs.maxAge) reasons.push('Age preference matches');
                else mismatches.push('Outside your age preference');
            }

            if (myPrefs.preferredLocations?.length) {
                if (myPrefs.preferredLocations.some(l => l === target.city || l === target.state)) reasons.push('Location preference matches');
                else mismatches.push('Location differs from your saved preference');
            }

            if (!myPrefs.religionOpen && myPrefs.preferredReligions?.length) {
                if (myPrefs.preferredReligions.includes(target.religion)) reasons.push('Religion preference matches');
                else mismatches.push('Religion differs from your saved preference');
            } else if (myPrefs.preferredReligions?.includes(target.religion)) {
                reasons.push('Religion preference aligns');
            }

            if (myPrefs.minEducation && target.educationLevel) {
                reasons.push('Education information is available for comparison');
            }
            if (myPrefs.foodHabitPreferences?.length && target.foodHabit && myPrefs.foodHabitPreferences.includes(target.foodHabit)) {
                reasons.push('Lifestyle preference aligns');
            }
        }

        if (myProfile.city && target.city && myProfile.city === target.city) reasons.push('Both profiles are in the same city');

        return {
            level: reasons.length >= 4 ? 'strong_mutual_signals' : reasons.length >= 2 ? 'some_aligned_signals' : 'limited_data',
            reasons: Array.from(new Set(reasons)),
            mismatches: Array.from(new Set(mismatches)),
            percentage: null
        };
    },

    async recordProfileView(viewerId, viewedUserId) {
        if (!viewerId || !viewedUserId || viewerId === viewedUserId) return;
        const blocked = await prisma.block.findFirst({
            where: { OR: [{ blockerId: viewerId, blockedId: viewedUserId }, { blockerId: viewedUserId, blockedId: viewerId }] }
        });
        if (blocked) return;
        await prisma.auditLog.create({
            data: { userId: viewerId, action: 'profile_view', resourceType: 'profile', resourceId: viewedUserId }
        });
    },

    async getProfileViewers(userId, limit = 20) {
        const blockedPairs = await prisma.block.findMany({
            where: { OR: [{ blockerId: userId }, { blockedId: userId }] },
            select: { blockerId: true, blockedId: true }
        });
        const blockedIds = blockedPairs.map(b => b.blockerId === userId ? b.blockedId : b.blockerId);

        const views = await prisma.auditLog.findMany({
            where: { action: 'profile_view', resourceId: userId, userId: { notIn: blockedIds } },
            orderBy: { createdAt: 'desc' },
            take: limit,
            distinct: ['userId'],
            include: {
                user: {
                    select: { id: true, isVerified: true, isActive: true, isBanned: true },
                    include: {
                        profile: { select: { firstName: true, lastName: true, dateOfBirth: true, city: true, profession: true } },
                        photos: { where: { isPrimary: true }, take: 1, select: { photoUrl: true, thumbnailUrl: true } }
                    }
                }
            }
        });
        return views
            .filter(v => v.user?.isActive && !v.user?.isBanned)
            .map(v => _formatLikeUser(v.user, v.createdAt));
    }
};

function _formatLikeUser(user, date) {
    const p = user.profile;
    return {
        id: user.id,
        userId: user.id,
        firstName: p?.firstName || null,
        age: ageFromDob(p?.dateOfBirth),
        city: p?.city || null,
        profession: p?.profession || null,
        photo: user.photos?.[0]?.thumbnailUrl || user.photos?.[0]?.photoUrl || null,
        isVerified: user.isVerified,
        receivedAt: _timeAgo(date)
    };
}

function _timeAgo(date) {
    const diff = Math.max(0, Date.now() - new Date(date).getTime());
    const m = Math.floor(diff / 60000);
    if (m < 60) return `${m || 1}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;
    return `${Math.floor(h / 24)}d ago`;
}

module.exports = interactionService;
