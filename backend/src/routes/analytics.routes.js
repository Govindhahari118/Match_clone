const express = require('express');
const analyticsController = require('../controllers/analytics.controller');
const authMiddleware = require('../middleware/auth.middleware');
const isAdmin = require('../middleware/admin.middleware');

const router = express.Router();

router.use(authMiddleware);

router.get('/taxonomy', analyticsController.getTaxonomy);
router.get('/flags', analyticsController.getFlags);
router.post('/flags/exposure', analyticsController.logFlagExposure);
router.post('/events', analyticsController.trackEvent);
router.post('/decisions', isAdmin, analyticsController.logDecision);

module.exports = router;
