const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth.middleware');
const notificationController = require('../controllers/notification.controller');

router.use(authMiddleware);
router.get('/', notificationController.list);
router.post('/read-all', notificationController.markAllRead);
router.post('/:notificationId/read', notificationController.markRead);

module.exports = router;
