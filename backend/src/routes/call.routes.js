const express = require('express');
const authMiddleware = require('../middleware/auth.middleware');
const callController = require('../controllers/call.controller');

const router = express.Router();

router.use(authMiddleware);
router.post('/session', callController.createSession);

module.exports = router;
