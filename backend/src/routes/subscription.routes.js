const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth.middleware');
const subscriptionController = require('../controllers/subscription.controller');

router.use(authMiddleware);

router.get('/entitlements', subscriptionController.getEntitlements);

module.exports = router;
