const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth.middleware');
const trustController = require('../controllers/trust.controller');

router.use(authMiddleware);

router.get('/me', trustController.getMine);
router.get('/users/:userId', trustController.getForUser);
router.post('/activity', trustController.recordActivity);
router.post('/reconfirm', trustController.reconfirm);
router.post('/users/:userId/hide', trustController.hideProfile);
router.post('/users/:userId/skip', trustController.skipProfile);
router.delete('/users/:userId/exposure', trustController.restoreProfile);

module.exports = router;
