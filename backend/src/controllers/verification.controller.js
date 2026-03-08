const prisma = require('../config/prisma');

const submitIdDoc = async (req, res) => {
    try {
        const { docUrl } = req.body;
        const userId = req.user.sub;

        if (!docUrl) {
            return res.status(400).json({ error: 'Document URL is required' });
        }

        // Update user status
        await prisma.user.update({
            where: { id: userId },
            data: {
                identityDocUrl: docUrl,
                identityStatus: 'pending',
            }
        });

        await prisma.verification.upsert({
            where: { userId_type: { userId, type: 'id_card' } },
            update: { status: 'pending', documentUrl: docUrl, rejectionReason: null },
            create: { userId, type: 'id_card', status: 'pending', documentUrl: docUrl },
        });

        await prisma.auditLog.create({
            data: {
                userId,
                action: 'verification_submitted',
                resourceType: 'verification',
                resourceId: userId,
                changes: {
                    type: 'id_card',
                    status: 'pending',
                    documentSubmitted: true,
                },
                ipAddress: req.ip || null,
                userAgent: req.get('user-agent') || null,
            },
        });

        res.status(200).json({
            success: true,
            message: 'ID Document submitted for verification'
        });

    } catch (error) {
        console.error('Verification submission error:', error);
        res.status(500).json({ error: 'Failed to submit verification' });
    }
};

const getStatus = async (req, res) => {
    try {
        const verification = await prisma.verification.findFirst({
            where: { userId: req.user.sub, type: 'id_card' },
            orderBy: { createdAt: 'desc' }
        });

        res.status(200).json({
            success: true,
            status: verification ? verification.status : 'not_started',
            rejectionReason: verification ? verification.rejectionReason : null
        });
    } catch (error) {
        console.error('Get Verification Status error:', error);
        res.status(500).json({ error: 'Failed to get status' });
    }
};

const getBadges = async (req, res) => {
    try {
        const userId = req.user.sub;
        const [user, verifications] = await Promise.all([
            prisma.user.findUnique({
                where: { id: userId },
                select: {
                    phone: true,
                    email: true,
                    isVerified: true,
                    identityStatus: true,
                },
            }),
            prisma.verification.findMany({
                where: { userId },
                select: { type: true, status: true, updatedAt: true },
            }),
        ]);

        const byType = new Map(verifications.map((entry) => [entry.type, entry]));
        const phoneVerification = byType.get('phone');
        const emailVerification = byType.get('email');
        const idVerification = byType.get('id') || byType.get('id_card');

        const badges = {
            phoneVerified: Boolean(user?.phone && (phoneVerification?.status === 'verified' || user?.isVerified)),
            emailVerified: Boolean(user?.email && (emailVerification?.status === 'verified' || user?.isVerified)),
            idVerified: idVerification?.status === 'verified' || user?.identityStatus === 'verified',
            profileScreened: user?.identityStatus === 'verified',
            blueTick: (idVerification?.status === 'verified' || user?.identityStatus === 'verified') && Boolean(user?.isVerified),
        };

        res.status(200).json({
            success: true,
            badges,
            details: verifications,
        });
    } catch (error) {
        console.error('Get verification badges error:', error);
        res.status(500).json({ error: 'Failed to fetch verification badges' });
    }
};

module.exports = { submitIdDoc, getStatus, getBadges };
