const prisma = require('../config/prisma');
const { uploadToCloudinary } = require('../config/cloudinary');

const uploadPhoto = async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }

        const userId = req.user.sub;
        const isPrimary = req.body.is_primary === 'true';

        let photoUrl = '';
        let thumbnailUrl = '';
        let fileSizeKb = Math.round(req.file.size / 1024);

        // If Cloudinary configured, upload
        if (process.env.CLOUDINARY_API_KEY) {
            const result = await uploadToCloudinary(req.file.buffer);
            photoUrl = result.secure_url;
            thumbnailUrl = result.secure_url; // Cloudinary supports transforms, using same for now
        } else {
            // Mock/Dev fallback if no keys present
            console.warn('Cloudinary keys missing. Using placeholder image.');
            photoUrl = `https://picsum.photos/seed/${userId}-${Date.now()}/400/600`;
            thumbnailUrl = `https://picsum.photos/seed/${userId}-${Date.now()}/100/150`;
        }

        // Unset current primary if new one is primary
        if (isPrimary) {
            await prisma.photo.updateMany({
                where: { userId, isPrimary: true },
                data: { isPrimary: false }
            });
        }

        // Save to DB
        const photo = await prisma.photo.create({
            data: {
                userId,
                photoUrl,
                thumbnailUrl,
                isPrimary,
                fileSizeKb
            }
        });

        res.status(201).json(photo);
    } catch (error) {
        console.error('Upload photo error:', error);
        res.status(500).json({ error: error.message });
    }
};

const deletePhoto = async (req, res) => {
    try {
        const { id } = req.params;
        const userId = req.user.sub;

        const photo = await prisma.photo.findUnique({ where: { id } });

        if (!photo) {
            return res.status(404).json({ error: 'Photo not found' });
        }

        if (photo.userId !== userId) {
            return res.status(403).json({ error: 'Unauthorized' });
        }

        // Ideally delete from Cloudinary too via public_id stored in DB or derived

        await prisma.photo.delete({ where: { id } });

        res.status(200).json({ message: 'Photo deleted successfully' });
    } catch (error) {
        console.error('Delete photo error:', error);
        res.status(500).json({ error: error.message });
    }
};

module.exports = { uploadPhoto, deletePhoto };
