const express = require('express');
const router = express.Router();
const shortlistController = require('../controllers/shortlist.controller');
const authMiddleware = require('../middleware/authMiddleware');

// Protected routes
router.use(authMiddleware);

router.post('/add', shortlistController.shortlistProfile);
router.post('/remove', shortlistController.removeShortlist);
router.get('/', shortlistController.getShortlistedProfiles);

module.exports = router;
