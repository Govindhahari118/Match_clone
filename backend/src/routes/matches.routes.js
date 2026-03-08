const express = require('express');
const router = express.Router();
const matchesController = require('../controllers/matches.controller');
const authMiddleware = require('../middleware/auth.middleware');

router.get('/', authMiddleware, matchesController.getMatches);
router.post('/feedback', authMiddleware, matchesController.submitMatchFeedback);

module.exports = router;
