const authService = require('../services/auth.service');

const requestOtp = async (req, res) => {
    try {
        const { phone, type } = req.body;
        if (!phone) {
            return res.status(400).json({ error: 'Phone number is required' });
        }

        const result = await authService.requestOtp(phone, type);
        res.status(200).json({
            success: true,
            message: result.message,
            otp_id: result.otpId,
            expires_in: result.expiresIn
        });
    } catch (error) {
        console.error('Request OTP error:', error);
        const status = error.code === 'OTP_RESEND_COOLDOWN' ? 429 :
            /already exists|not found|Invalid OTP purpose/i.test(error.message) ? 400 : 500;
        res.status(status).json({
            error: error.message,
            ...(error.retryAfter ? { retry_after: error.retryAfter } : {})
        });
    }
};

const verifyOtp = async (req, res) => {
    try {
        const { phone, otp, otp_id } = req.body;
        // OTP ID can be used for validation if we store otpId <-> phone mapping, skipping for now

        if (!phone || !otp) {
            return res.status(400).json({ error: 'Phone and OTP are required' });
        }

        const { user, accessToken, refreshToken } = await authService.verifyOtp(phone, otp);

        res.status(200).json({
            success: true,
            user,
            access_token: accessToken,
            refresh_token: refreshToken,
            expires_in: 3600
        });
    } catch (error) {
        console.error('Verify OTP error:', error);
        if (['User not found', 'Invalid OTP', 'OTP expired', 'OTP not requested or already used', 'OTP attempt limit exceeded'].includes(error.message)) {
            return res.status(400).json({ error: error.message });
        }
        res.status(500).json({ error: 'Internal server error' });
    }
};

const loginEmail = async (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ error: 'Email and password are required' });
        }

        const { user, accessToken, refreshToken } = await authService.loginEmail(email, password);

        res.status(200).json({
            success: true,
            user,
            access_token: accessToken,
            refresh_token: refreshToken,
            expires_in: 3600
        });
    } catch (error) {
        console.error('Login error:', error);
        if (error.message === 'Invalid credentials') {
            return res.status(401).json({ error: 'Invalid credentials' });
        }
        res.status(500).json({ error: error.message });
    }
};

const loginFirebase = async (req, res) => {
    try {
        const { idToken } = req.body;
        if (!idToken) {
            return res.status(400).json({ error: 'ID Token is required' });
        }
        const result = await authService.loginWithFirebase(idToken);
        res.status(200).json({
            success: true,
            user: result.user,
            access_token: result.accessToken,
            refresh_token: result.refreshToken,
            accessToken: result.accessToken,
            refreshToken: result.refreshToken,
            expires_in: 3600
        });
    } catch (error) {
        console.error('Firebase Login Error:', error);
        res.status(401).json({ error: error.message });
    }
};

const signup = async (req, res) => {
    res.status(501).json({ error: 'Use request-otp for signup' });
};

module.exports = { requestOtp, verifyOtp, login: loginEmail, signup, loginFirebase }; 
