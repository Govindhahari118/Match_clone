const express = require('express');
const router = express.Router();
const userController = require('../controllers/user.controller');
const preferenceController = require('../controllers/preference.controller');
const authMiddleware = require('../middleware/auth.middleware');

router.use(authMiddleware);

router.get('/profile', userController.getProfile);
router.put('/profile', userController.updateProfile);

router.get('/profile/preferences', preferenceController.getPreferences);
router.put('/profile/preferences', preferenceController.savePreferences);
router.post('/profile/preferences', preferenceController.savePreferences);

router.put('/password', userController.updatePassword);
router.get('/privacy', userController.getPrivacySettings);
router.post('/privacy', userController.updatePrivacySettings);
router.post('/verification', userController.submitVerification);

module.exports = router;
