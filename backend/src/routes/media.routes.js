const router = require('express').Router();
const authMiddleware = require('../middleware/auth.middleware');
const mediaController = require('../controllers/media.controller');

// Generate Presigned URL for direct S3 Upload
router.post('/presigned-url', authMiddleware, mediaController.getPresignedUrl);

module.exports = router;
