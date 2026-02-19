const express = require('express');
const router = express.Router();
const profileController = require('../controllers/profile.controller');
const authMiddleware = require('../middleware/auth.middleware');

// Public Profile View (Protected by verification)
router.get('/:id', authMiddleware, profileController.getProfileById);

module.exports = router;
