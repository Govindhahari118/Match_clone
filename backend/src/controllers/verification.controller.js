const prisma = require('../config/prisma');

const submitIdDoc = async (req, res) => {
    try {
        const { docUrl } = req.body;
        const userId = req.user.id;

        if (!docUrl) {
            return res.status(400).json({ error: 'Document URL is required' });
        }

        // Update user status
        await prisma.user.update({
            where: { id: userId },
            data: {
                identityDocUrl: docUrl,
                identityStatus: 'pending',
                // Create a Verification record for history
                verifications: {
                    create: {
                        type: 'id_card',
                        status: 'pending',
                        documentUrl: docUrl
                    }
                }
            }
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
            where: { userId: req.user.id, type: 'id_card' },
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

module.exports = { submitIdDoc, getStatus };
