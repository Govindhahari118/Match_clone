const prisma = require('../config/prisma');
const interactionService = require('../services/interaction.service');

const getProfileById = async (req, res) => {
    try {
        const { id } = req.params;
        const viewerId = req.user?.sub;

        const profile = await prisma.profile.findUnique({
            where: { userId: id },
            include: {
                photos: true,
                user: {
                    select: { id: true, lastLogin: true, isVerified: true, isActive: true }
                }
            }
        });

        if (!profile) return res.status(404).json({ error: 'Profile not found' });

        // Record profile view (async, non-blocking — don't let this fail the request)
        if (viewerId && viewerId !== id) {
            interactionService.recordProfileView(viewerId, id).catch(() => { });
        }

        // Attach compatibility score if viewer has preferences
        let compatibilityScore = null;
        if (viewerId && viewerId !== id) {
            try {
                compatibilityScore = await interactionService.getCompatibilityScore(viewerId, profile.id);
            } catch { }
        }

        res.json({ ...profile, compatibilityScore });
    } catch (error) {
        console.error('Get Profile Error:', error);
        res.status(500).json({ error: 'Failed to fetch profile' });
    }
};

module.exports = { getProfileById };
