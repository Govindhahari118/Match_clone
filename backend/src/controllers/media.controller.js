const { S3Client, PutObjectCommand } = require('@aws-sdk/client-s3');
const { getSignedUrl } = require('@aws-sdk/s3-request-presigner');
const crypto = require('crypto');
const { getAuthUserId } = require('../utils/auth');

const s3Client = new S3Client({
    region: process.env.AWS_REGION,
    credentials: {
        accessKeyId: process.env.AWS_ACCESS_KEY_ID,
        secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
    }
});

exports.getPresignedUrl = async (req, res) => {
    try {
        const { fileName, fileType, resourceType = 'misc' } = req.body;

        // Validate required fields
        if (!fileName || !fileType) {
            return res.status(400).json({ error: 'Missing fileName or fileType' });
        }

        const userId = getAuthUserId(req);
        if (!userId) {
            return res.status(401).json({ error: 'Access denied' });
        }

        // Generate unique key
        const uniqueId = crypto.randomUUID();
        const key = `${resourceType}/${userId}/${uniqueId}-${fileName}`;

        // Create S3 Command
        const command = new PutObjectCommand({
            Bucket: process.env.AWS_BUCKET_NAME,
            Key: key,
            ContentType: fileType,
            // Metadata: ...
        });

        // Generate Presigned URL (valid for 15 mins)
        const uploadUrl = await getSignedUrl(s3Client, command, { expiresIn: 900 });

        // Construct public URL (assuming public access)
        // For Cloudinary usage, logic differs, but user asked for S3.
        const publicUrl = `https://${process.env.AWS_BUCKET_NAME}.s3.${process.env.AWS_REGION}.amazonaws.com/${key}`;

        res.json({
            uploadUrl,
            publicUrl,
            key
        });
    } catch (error) {
        console.error('S3 Presigned URL Error:', error);
        res.status(500).json({ error: 'Failed to generate upload URL' });
    }
};
