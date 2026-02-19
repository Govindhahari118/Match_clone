const express = require('express');
const router = express.Router();
const photoController = require('../controllers/photo.controller');
const uploadMiddleware = require('../middleware/upload.middleware');
const authMiddleware = require('../middleware/auth.middleware');

// POST /photos/upload
router.post('/upload', authMiddleware, uploadMiddleware.single('file'), photoController.uploadPhoto);

// DELETE /photos/:id
router.delete('/:id', authMiddleware, photoController.deletePhoto);

module.exports = router;
