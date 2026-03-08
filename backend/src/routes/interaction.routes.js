const express = require('express');
const router = express.Router();
const interactionController = require('../controllers/interaction.controller');
const authMiddleware = require('../middleware/auth.middleware');

router.use(authMiddleware);

// ── Interests CRUD ────────────────────────────────────────
router.get('/interests', interactionController.getInterests);          // ?type=received|sent|mutual
router.post('/like', interactionController.likeUser);
router.post('/reject', interactionController.rejectUser);
router.post('/decline', interactionController.declineInterest);        // receiver declines an incoming interest

// ── Report / Safety ────────────────────────────────────────
router.post('/report', interactionController.reportUser);
router.post('/safety-action', interactionController.applySafetyAction);
router.get('/safety-status', interactionController.getSafetyStatus);

// ── Profile Views ─────────────────────────────────────────
// ── Profile Views ─────────────────────────────────────────
router.get('/profile-viewers', interactionController.getProfileViewers);

// ── Features ─────────────────────────────────────────
router.get('/horoscope/:targetUserId', interactionController.getHoroscopeMatch);

module.exports = router;
