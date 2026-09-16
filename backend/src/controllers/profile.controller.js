const prisma = require('../config/prisma');
const interactionService = require('../services/interaction.service');
const privacyService = require('../services/privacy.service');
const profileExtensionService = require('../services/profile-extension.service');
const trustService = require('../services/trust.service');
const safetyService = require('../services/safety.service');

const getProfileById = async (req, res) => {
    try {
        const { id } = req.params;
        const viewerId = req.user?.sub;

        if (viewerId && viewerId !== id && await safetyService.isBlocked(viewerId, id)) {
            return res.status(404).json({ error: 'Profile not found' });
        }

        const [targetUser, viewer] = await Promise.all([
            prisma.user.findUnique({
                where: { id },
                include: {
                    profile: true,
                    partnerPreference: true,
                    photos: { where: { moderationStatus: 'approved' }, orderBy: [{ isPrimary: 'desc' }, { uploadedAt: 'asc' }] },
                    verifications: true,
                },
            }),
            viewerId
                ? prisma.user.findUnique({
                    where: { id: viewerId },
                    select: {
                        id: true,
                        isVerified: true,
                        subscriptions: {
                            where: {
                                status: { in: ['active', 'grace'] },
                                OR: [{ expiresAt: null }, { expiresAt: { gt: new Date() } }],
                            },
                            select: { id: true },
                            take: 1,
                        },
                    },
                })
                : null,
        ]);

        if (!targetUser?.profile) return res.status(404).json({ error: 'Profile not found' });
        const profile = targetUser.profile;
        const isOwner = Boolean(viewerId && viewerId === id);

        if (!isOwner && (
            !targetUser.isActive ||
            targetUser.isBanned ||
            targetUser.deletedAt ||
            !['active', 'low_activity'].includes(targetUser.searchStatus) ||
            trustService.isStale(targetUser)
        )) {
            return res.status(404).json({ error: 'Profile not found' });
        }

        const viewerIsPremium = Boolean(viewer?.subscriptions?.length);
        const viewerIsVerified = Boolean(viewer?.isVerified);
        const [{ settings }, { extension }] = await Promise.all([
            privacyService.getUserPrivacySettings(id),
            profileExtensionService.getUserProfileExtension(id),
        ]);

        if (!privacyService.canViewProfile({ settings, isOwner, viewerIsPremium, viewerIsVerified })) {
            return res.status(403).json({ error: 'Profile visibility restricted by user privacy settings' });
        }

        let isMutualMatch = false;
        let hasExplicitPhotoAccess = false;
        let canViewContact = false;
        if (viewerId && viewerId !== id) {
            const [mutualMatch, explicitPhotoAccess, contactAccess] = await Promise.all([
                prisma.match.findFirst({
                    where: {
                        isActive: true,
                        OR: [{ userAId: viewerId, userBId: id }, { userAId: id, userBId: viewerId }],
                    },
                    select: { id: true },
                }),
                safetyService.hasPhotoAccess(viewerId, id),
                safetyService.canViewContact(viewerId, id),
            ]);
            isMutualMatch = Boolean(mutualMatch);
            hasExplicitPhotoAccess = explicitPhotoAccess;
            canViewContact = contactAccess;
        }

        const canViewPhotos = privacyService.canViewPhotos({
            settings,
            isOwner,
            isMutualMatch,
            hasExplicitPhotoAccess,
        });

        if (viewerId && viewerId !== id) interactionService.recordProfileView(viewerId, id).catch(() => {});

        const [compatibility, trust] = await Promise.all([
            viewerId && viewerId !== id
                ? interactionService.getCompatibilitySummary(viewerId, id).catch(() => ({ score: null, strength: 'unknown', reasons: [] }))
                : Promise.resolve(null),
            trustService.getTrustSummary(id),
        ]);

        const publicTrust = trust ? {
            ...trust,
            lastActiveAt: (settings.showLastSeen || isOwner) ? trust.lastActiveAt : null,
        } : null;

        return res.json({
            ...profile,
            photos: canViewPhotos ? targetUser.photos : [],
            user: {
                id: targetUser.id,
                isVerified: targetUser.isVerified,
                identityStatus: targetUser.identityStatus,
                searchStatus: targetUser.searchStatus,
                lastLogin: (settings.showLastSeen || isOwner) ? targetUser.lastLogin : null,
            },
            contact: {
                canView: isOwner || (canViewContact && settings.showPhone),
                phone: isOwner || (canViewContact && settings.showPhone) ? targetUser.phone : null,
                requiresApproval: !isOwner && !canViewContact,
            },
            hasChildren: extension.hasChildren,
            residentialStatus: extension.residentialStatus,
            district: extension.district,
            compatibilityScore: compatibility?.score ?? null,
            compatibilityStrength: compatibility?.strength || 'unknown',
            whyRecommended: compatibility?.reasons || [],
            trust: publicTrust,
            privacy: {
                canViewPhotos,
                hasExplicitPhotoAccess,
                photoVisibility: settings.photoVisibility,
                profileVisibility: settings.profileVisibility,
            },
        });
    } catch (error) {
        console.error('Get Profile Error:', error);
        const status = Number(error.statusCode) || 500;
        return res.status(status).json({ error: status >= 500 ? 'Failed to fetch profile' : error.message });
    }
};

module.exports = { getProfileById };
