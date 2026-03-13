const express = require('express');
const router = express.Router();
const authController = require('../controllers/auth.controller');
const authService = require('../services/auth.service'); // Using service for direct logic? No, use controller.

// OTP Flow
router.post('/request-otp', authController.requestOtp);
router.post('/verify-otp', authController.verifyOtp);

// Email Flow
router.post('/login-email', authController.login);
router.post('/signup', authController.signup);

// Firebase Flow (Mobile/Social)
router.post('/login-firebase', authController.loginFirebase);

// Token Refresh
router.post('/refresh', authController.refreshToken);

// Logout
router.post('/logout', authController.logout);

module.exports = router;
