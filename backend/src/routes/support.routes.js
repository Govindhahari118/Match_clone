const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth.middleware');
const supportController = require('../controllers/support.controller');

router.use(authMiddleware);

router.get('/', supportController.list);
router.post('/', supportController.create);
router.get('/:ticketId', supportController.get);
router.post('/:ticketId/comments', supportController.comment);
router.post('/:ticketId/confirm-resolution', supportController.confirmResolution);

module.exports = router;
