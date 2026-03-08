const express = require('express');
const router = express.Router();
const adminController = require('../controllers/admin.controller');
const authMiddleware = require('../middleware/auth.middleware');
const isAdmin = require('../middleware/admin.middleware');

router.use(authMiddleware);
router.use(isAdmin); // Protect all admin routes

router.get('/dashboard', adminController.getDashboardStats);
router.get('/users', adminController.getAllUsers);
router.post('/users/ban', adminController.banUser);
router.get('/moderation/queue', adminController.getModerationQueue);
router.patch('/moderation/:reportId', adminController.updateModerationCase);
router.patch('/moderation/:reportId/assign', adminController.assignModerationCase);
router.post('/moderation/bulk', adminController.bulkModerationAction);
router.post('/moderation/bulk/:operationId/rollback', adminController.rollbackBulkModeration);
router.get('/moderation/qa-sample', adminController.getModerationQaSample);
router.get('/users/:userId/risk', adminController.getUserRiskSnapshot);
router.get('/ops/metrics', adminController.getOpsMetrics);
router.get('/ops/runbooks', adminController.getRunbooks);

module.exports = router;
