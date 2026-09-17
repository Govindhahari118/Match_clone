const prisma = require('../config/prisma');
const safetyService = require('../services/safety.service');
const trustService = require('../services/trust.service');
const interactionService = require('../services/interaction.service');

function sendError(res, error, fallback) {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? fallback : error.message });
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

exports.shortlistProfile = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { shortlistedUserId } = req.body || {};
        if (!shortlistedUserId || shortlistedUserId === userId) {
            return res.status(400).json({ error: 'Invalid profile' });
        }

        await safetyService.ensureNotBlocked(userId, shortlistedUserId);
        const target = await prisma.user.findUnique({
            where: { id: shortlistedUserId },
            select: { id: true, isActive: true, isBanned: true, deletedAt: true, searchStatus: true },
        });
        if (!target || !target.isActive || target.isBanned || target.deletedAt || !['active', 'low_activity'].includes(target.searchStatus)) {
            return res.status(404).json({ error: 'Profile is unavailable' });
        }

        const shortlist = await prisma.shortlist.upsert({
            where: { userId_shortlistedUserId: { userId, shortlistedUserId } },
            update: {},
            create: { userId, shortlistedUserId },
        });
        await trustService.recordActivity(userId);
        return res.status(201).json({ id: shortlist.id, shortlistedUserId, createdAt: shortlist.createdAt });
    } catch (error) {
        console.error('Shortlist error:', error);
        return sendError(res, error, 'Failed to shortlist profile');
    }
};

exports.removeShortlist = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { shortlistedUserId } = req.body || {};
        if (!shortlistedUserId) return res.status(400).json({ error: 'shortlistedUserId is required' });

        await prisma.shortlist.deleteMany({ where: { userId, shortlistedUserId } });
        await trustService.recordActivity(userId);
        return res.json({ success: true, message: 'Removed from shortlist' });
    } catch (error) {
        console.error('Remove shortlist error:', error);
        return sendError(res, error, 'Failed to remove shortlist');
    }
};

exports.getShortlistedProfiles = async (req, res) => {
    try {
        const userId = req.user.sub;
        const [shortlists, blockedRows] = await Promise.all([
            prisma.shortlist.findMany({
                where: { userId },
                orderBy: { createdAt: 'desc' },
                include: {
                    shortlistedUser: {
                        select: {
                            id: true,
                            isVerified: true,
                            lastActiveAt: true,
                            lastLogin: true,
                            searchStatus: true,
                            isActive: true,
                            isBanned: true,
                            deletedAt: true,
                            profile: true,
                            photos: {
                                where: { isPrimary: true, moderationStatus: 'approved' },
                                take: 1,
                                select: { photoUrl: true, thumbnailUrl: true, verificationStatus: true },
                            },
                            verifications: {
                                where: { status: 'verified' },
                                select: { type: true, verifiedAt: true },
                            },
                        },
                    },
                },
            }),
            prisma.userBlock.findMany({
                where: { OR: [{ blockerId: userId }, { blockedUserId: userId }] },
                select: { blockerId: true, blockedUserId: true },
            }),
        ]);

        const blockedIds = new Set(blockedRows.map((row) => row.blockerId === userId ? row.blockedUserId : row.blockerId));
        const visible = shortlists.filter((row) => {
            const candidate = row.shortlistedUser;
            return candidate && !blockedIds.has(candidate.id) && candidate.isActive && !candidate.isBanned && !candidate.deletedAt && ['active', 'low_activity'].includes(candidate.searchStatus);
        });

        const profiles = await Promise.all(visible.map(async (row) => {
            const candidate = row.shortlistedUser;
            const profile = candidate.profile;
            if (!profile) return null;
            const compatibility = await interactionService.getCompatibilitySummary(userId, candidate.id);
            const photo = candidate.photos?.[0];
            return {
                id: row.id,
                shortlistId: row.id,
                userId: candidate.id,
                firstName: profile.firstName,
                age: ageFromDob(profile.dateOfBirth),
                city: profile.city,
                profession: profile.profession,
                managedBy: profile.role || 'self',
                photo: photo?.thumbnailUrl || photo?.photoUrl || null,
                photoVerified: photo?.verificationStatus === 'verified',
                isVerified: candidate.isVerified,
                verificationTypes: candidate.verifications.map((entry) => entry.type),
                activity: trustService.activityBucket(candidate),
                compatibility,
                shortlistedAt: row.createdAt,
            };
        }));

        await trustService.recordActivity(userId);
        return res.json(profiles.filter(Boolean));
    } catch (error) {
        console.error('Get shortlist error:', error);
        return sendError(res, error, 'Failed to load shortlists');
    }
};
