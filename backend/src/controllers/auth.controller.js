const authService = require('../services/auth.service');

function sendError(res, error, fallback = 'Authentication request failed') {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? fallback : error.message });
}

const requestOtp = async (req, res) => {
    try {
        const { phone, type } = req.body || {};
        if (!phone) return res.status(400).json({ error: 'Phone number is required' });
        const result = await authService.requestOtp(phone, type);
        return res.status(200).json({
            success: true,
            message: result.message,
            otp_id: result.otpId,
            expires_in: result.expiresIn,
            resend_after: result.resendAfter,
            ...(process.env.NODE_ENV !== 'production' && result.devOtp ? { dev_otp: result.devOtp } : {}),
        });
    } catch (error) {
        console.error('Request OTP error:', error);
        return sendError(res, error, 'Unable to send OTP');
    }
};

const verifyOtp = async (req, res) => {
    try {
        const { phone, otp } = req.body || {};
        if (!phone || !otp) return res.status(400).json({ error: 'Phone and OTP are required' });
        const { user, accessToken, refreshToken } = await authService.verifyOtp(phone, otp);
        return res.status(200).json({
            success: true,
            user,
            access_token: accessToken,
            refresh_token: refreshToken,
            expires_in: 3600,
        });
    } catch (error) {
        console.error('Verify OTP error:', error);
        return sendError(res, error, 'Unable to verify OTP');
    }
};

const loginEmail = async (req, res) => {
    try {
        const { email, password } = req.body || {};
        if (!email || !password) return res.status(400).json({ error: 'Email and password are required' });
        const { user, accessToken, refreshToken } = await authService.loginEmail(email, password);
        return res.status(200).json({
            success: true,
            user,
            access_token: accessToken,
            refresh_token: refreshToken,
            expires_in: 3600,
        });
    } catch (error) {
        console.error('Login error:', error);
        return sendError(res, error, 'Login failed');
    }
};

const loginFirebase = async (req, res) => {
    try {
        const { idToken } = req.body || {};
        if (!idToken) return res.status(400).json({ error: 'ID Token is required' });
        const result = await authService.loginWithFirebase(idToken);
        return res.status(200).json({
            success: true,
            user: result.user,
            access_token: result.accessToken,
            refresh_token: result.refreshToken,
            accessToken: result.accessToken,
            refreshToken: result.refreshToken,
            expires_in: 3600,
        });
    } catch (error) {
        console.error('Firebase Login Error:', error);
        return sendError(res, error, 'Firebase login failed');
    }
};

const signup = async (req, res) => res.status(501).json({ error: 'Use request-otp for signup' });

module.exports = { requestOtp, verifyOtp, login: loginEmail, signup, loginFirebase };
