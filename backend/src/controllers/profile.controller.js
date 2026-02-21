const prisma = require('../config/prisma');
const interactionService = require('../services/interaction.service');
const privacyService = require('../services/privacy.service');
const profileExtensionService = require('../services/profile-extension.service');

const getProfileById = async (req, res) => {
    try {
        const { id } = req.params;
        const viewerId = req.user?.sub;

        const [profile, viewer] = await Promise.all([
            prisma.profile.findUnique({
                where: { userId: id },
                include: {
                    photos: true,
                    user: {
                        select: {
                            id: true,
                            lastLogin: true,
                            isVerified: true,
                            isActive: true,
                            isBanned: true,
                        }
                    }
                }
            }),
            viewerId
                ? prisma.user.findUnique({
                    where: { id: viewerId },
                    select: {
                        id: true,
                        isVerified: true,
                        subscriptions: {
                            where: {
                                status: 'active',
                                OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }],
                            },
                            select: { id: true },
                            take: 1,
                        },
                    },
                })
                : null,
        ]);

        if (!profile) return res.status(404).json({ error: 'Profile not found' });
        const isOwner = viewerId && viewerId === id;

        if (!isOwner && (!profile.user.isActive || profile.user.isBanned)) {
            return res.status(404).json({ error: 'Profile not found' });
        }

        const viewerIsPremium = Boolean(viewer?.subscriptions?.length);
        const viewerIsVerified = Boolean(viewer?.isVerified);

        const [{ settings }, { extension }] = await Promise.all([
            privacyService.getUserPrivacySettings(id),
            profileExtensionService.getUserProfileExtension(id),
        ]);

        const canViewProfile = privacyService.canViewProfile({
            settings,
            isOwner,
            viewerIsPremium,
            viewerIsVerified,
        });
        if (!canViewProfile) {
            return res.status(403).json({ error: 'Profile visibility restricted by user privacy settings' });
        }

        let isMutualMatch = false;
        if (viewerId && viewerId !== id) {
            const mutualMatch = await prisma.match.findFirst({
                where: {
                    isActive: true,
                    OR: [
                        { userAId: viewerId, userBId: id },
                        { userAId: id, userBId: viewerId },
                    ],
                },
                select: { id: true },
            });
            isMutualMatch = Boolean(mutualMatch);
        }

        const canViewPhotos = privacyService.canViewPhotos({
            settings,
            isOwner,
            isMutualMatch,
            viewerIsPremium,
        });

        // Record profile view asynchronously so profile read path stays fast.
        if (viewerId && viewerId !== id) {
            interactionService.recordProfileView(viewerId, id).catch(() => { });
        }

        let compatibilityScore = null;
        if (viewerId && viewerId !== id) {
            try {
                compatibilityScore = await interactionService.getCompatibilityScore(viewerId, profile.id);
            } catch {
                compatibilityScore = null;
            }
        }

        const projectedProfile = {
            ...profile,
            photos: canViewPhotos ? profile.photos : [],
            user: {
                ...profile.user,
                lastLogin: (settings.showLastSeen || isOwner) ? profile.user.lastLogin : null,
            },
            hasChildren: extension.hasChildren,
            residentialStatus: extension.residentialStatus,
            district: extension.district,
        };

        return res.json({
            ...projectedProfile,
            compatibilityScore,
            privacy: {
                canViewPhotos,
                photoVisibility: settings.photoVisibility,
                profileVisibility: settings.profileVisibility,
            },
        });
    } catch (error) {
        console.error('Get Profile Error:', error);
        return res.status(500).json({ error: 'Failed to fetch profile' });
    }
};

module.exports = { getProfileById };
