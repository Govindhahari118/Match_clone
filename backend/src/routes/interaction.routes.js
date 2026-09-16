const express = require('express');
const router = express.Router();
const interactionController = require('../controllers/interaction.controller');
const authMiddleware = require('../middleware/auth.middleware');

router.use(authMiddleware);

router.get('/interests', interactionController.getInterests);
router.post('/like', interactionController.likeUser);
router.post('/reject', interactionController.rejectUser);
router.post('/decline', interactionController.declineInterest);
router.post('/withdraw', interactionController.withdrawInterest);

router.post('/report', interactionController.reportUser);
router.post('/block/:userId', interactionController.blockUser);
router.delete('/block/:userId', interactionController.unblockUser);

router.post('/contact/:userId', interactionController.requestContact);
router.post('/contact-requests/:requestId/respond', interactionController.respondContact);
router.post('/photo-access/:userId', interactionController.requestPhotoAccess);
router.post('/photo-access-requests/:requestId/respond', interactionController.respondPhotoAccess);

router.get('/profile-viewers', interactionController.getProfileViewers);
router.get('/horoscope/:targetUserId', interactionController.getHoroscopeMatch);

module.exports = router;
