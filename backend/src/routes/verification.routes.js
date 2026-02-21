const express = require('express');
const router = express.Router();
const verificationController = require('../controllers/verification.controller');
const authMiddleware = require('../middleware/auth.middleware');

router.post('/submit-id', authMiddleware, verificationController.submitIdDoc);
router.get('/status', authMiddleware, verificationController.getStatus);
router.get('/badges', authMiddleware, verificationController.getBadges);

module.exports = router;
