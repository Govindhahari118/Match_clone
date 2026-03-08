const express = require('express');
const router = express.Router();
const userController = require('../controllers/user.controller');
const preferenceController = require('../controllers/preference.controller');
const authMiddleware = require('../middleware/auth.middleware');

router.use(authMiddleware);

// Profile
router.get('/profile', userController.getProfile);
router.put('/profile', userController.updateProfile);

// Preferences
router.put('/profile/preferences', preferenceController.savePreferences);
router.post('/profile/preferences', preferenceController.savePreferences); // Fallback

// Password
router.put('/password', userController.updatePassword);

// Privacy Settings
router.get('/privacy', userController.getPrivacySettings);
router.post('/privacy', userController.updatePrivacySettings);

// Verification
router.post('/verification', userController.submitVerification);
router.get('/onboarding/progress', userController.getOnboardingProgress);
router.post('/onboarding/progress', userController.saveOnboardingProgress);

module.exports = router;

