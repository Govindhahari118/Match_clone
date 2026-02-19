const express = require('express');
const router = express.Router();
const matchesController = require('../controllers/matches.controller');
const authMiddleware = require('../middleware/auth.middleware');

router.get('/', authMiddleware, matchesController.getMatches);

module.exports = router;
