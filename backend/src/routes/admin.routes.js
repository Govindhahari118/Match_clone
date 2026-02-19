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

module.exports = router;
