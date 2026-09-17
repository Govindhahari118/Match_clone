const express = require('express');
const router = express.Router();
const adminController = require('../controllers/admin.controller');
const authMiddleware = require('../middleware/auth.middleware');
const isAdmin = require('../middleware/admin.middleware');

router.use(authMiddleware);
router.use(isAdmin);

router.get('/dashboard', adminController.getDashboardStats);
router.get('/users', adminController.getAllUsers);
router.post('/users/ban', adminController.banUser);

router.get('/moderation/photos', adminController.listPhotoModeration);
router.post('/moderation/photos/:photoId', adminController.moderatePhoto);

router.get('/reports', adminController.listReports);
router.post('/reports/:reportId', adminController.resolveReport);

router.get('/support', adminController.listSupportTickets);
router.post('/support/:ticketId', adminController.updateSupportTicket);

module.exports = router;
