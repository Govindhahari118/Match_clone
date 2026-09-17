const prisma = require('../config/prisma');
const { uploadToCloudinary } = require('../config/cloudinary');
const trustService = require('../services/trust.service');

const ALLOWED_MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

function sendError(res, error, fallback = 'Photo request failed') {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? fallback : error.message });
}

const uploadPhoto = async (req, res) => {
    try {
        if (!req.file) return res.status(400).json({ error: 'No file uploaded' });
        if (!ALLOWED_MIME_TYPES.has(req.file.mimetype)) return res.status(400).json({ error: 'Only JPEG, PNG and WebP images are supported' });
        if (!process.env.CLOUDINARY_API_KEY) {
            return res.status(503).json({ error: 'Photo storage is not configured. Your profile has not been changed.' });
        }

        const userId = req.user.sub;
        const requestedPrimary = req.body.is_primary === 'true';
        const result = await uploadToCloudinary(req.file.buffer);
        if (!result?.secure_url) throw new Error('Image storage returned no URL');

        const moderationStatus = process.env.PHOTO_MODERATION_MODE === 'trusted_auto' ? 'approved' : 'review_required';
        const photo = await prisma.photo.create({
            data: {
                userId,
                photoUrl: result.secure_url,
                thumbnailUrl: result.secure_url,
                isPrimary: false,
                fileSizeKb: Math.round(req.file.size / 1024),
                width: result.width || null,
                height: result.height || null,
                moderationStatus,
                verificationStatus: 'pending',
            },
        });

        await prisma.auditLog.create({
            data: {
                userId,
                action: 'photo_uploaded',
                resourceType: 'photo',
                resourceId: photo.id,
                changes: { requestedPrimary, moderationStatus, verificationStatus: 'pending' },
            },
        });

        if (requestedPrimary && moderationStatus === 'approved') {
            await prisma.$transaction([
                prisma.photo.updateMany({ where: { userId, isPrimary: true }, data: { isPrimary: false } }),
                prisma.photo.update({ where: { id: photo.id }, data: { isPrimary: true } }),
            ]);
            photo.isPrimary = true;
        }

        await trustService.recordActivity(userId);
        return res.status(201).json({
            ...photo,
            requestedPrimary,
            message: moderationStatus === 'approved'
                ? 'Photo uploaded.'
                : 'Photo uploaded and queued for review. Your current approved photo remains unchanged.',
        });
    } catch (error) {
        console.error('Upload photo error:', error);
        return sendError(res, error, 'Photo upload failed');
    }
};

const setPrimaryPhoto = async (req, res) => {
    try {
        const userId = req.user.sub;
        const photo = await prisma.photo.findFirst({ where: { id: req.params.id, userId } });
        if (!photo) return res.status(404).json({ error: 'Photo not found' });
        if (photo.moderationStatus !== 'approved') return res.status(409).json({ error: 'Only an approved photo can become the primary photo' });

        await prisma.$transaction([
            prisma.photo.updateMany({ where: { userId, isPrimary: true }, data: { isPrimary: false } }),
            prisma.photo.update({ where: { id: photo.id }, data: { isPrimary: true } }),
            prisma.auditLog.create({
                data: { userId, action: 'primary_photo_changed', resourceType: 'photo', resourceId: photo.id },
            }),
        ]);
        return res.json({ success: true, photoId: photo.id });
    } catch (error) {
        return sendError(res, error);
    }
};

const deletePhoto = async (req, res) => {
    try {
        const userId = req.user.sub;
        const photo = await prisma.photo.findUnique({ where: { id: req.params.id } });
        if (!photo) return res.status(404).json({ error: 'Photo not found' });
        if (photo.userId !== userId) return res.status(403).json({ error: 'Unauthorized' });

        await prisma.$transaction([
            prisma.photo.delete({ where: { id: photo.id } }),
            prisma.auditLog.create({
                data: { userId, action: 'photo_deleted', resourceType: 'photo', resourceId: photo.id },
            }),
        ]);
        return res.status(200).json({ message: 'Photo deleted successfully' });
    } catch (error) {
        return sendError(res, error, 'Photo deletion failed');
    }
};

module.exports = { uploadPhoto, setPrimaryPhoto, deletePhoto };
