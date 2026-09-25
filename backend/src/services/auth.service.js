const crypto = require('crypto');
const prisma = require('../config/prisma'); // Import singleton
const { generateTokens } = require('../utils/jwt');
const { hashPassword, comparePassword } = require('../utils/password');
const admin = require('../config/firebase');

const authService = {
    // Request OTP for phone. Signup and login remain separate operations.
    async requestOtp(phone, type = 'signup') {
        const normalizedType = String(type || 'signup').toLowerCase();
        if (!['signup', 'login'].includes(normalizedType)) {
            throw new Error('Invalid OTP purpose');
        }

        let user = await prisma.user.findUnique({ where: { phone } });
        if (!user && normalizedType === 'login') {
            throw new Error('User not found. Please sign up.');
        }
        if (user && normalizedType === 'signup' && user.isVerified) {
            throw new Error('Account already exists. Please log in.');
        }

        const now = new Date();
        const resendCooldownMs = Number(process.env.OTP_RESEND_COOLDOWN_SECONDS || 60) * 1000;
        if (user?.otpLastSentAt && now.getTime() - new Date(user.otpLastSentAt).getTime() < resendCooldownMs) {
            const retryAfter = Math.ceil((resendCooldownMs - (now.getTime() - new Date(user.otpLastSentAt).getTime())) / 1000);
            const error = new Error('OTP recently sent. Please wait before retrying.');
            error.code = 'OTP_RESEND_COOLDOWN';
            error.retryAfter = retryAfter;
            throw error;
        }

        const otp = String(crypto.randomInt(100000, 1000000));
        const otpSecret = process.env.OTP_HASH_SECRET || process.env.JWT_SECRET;
        if (!otpSecret) throw new Error('OTP security secret is not configured');
        const otpCodeHash = crypto.createHmac('sha256', otpSecret).update(`${phone}:${normalizedType}:${otp}`).digest('hex');
        const ttlSeconds = Number(process.env.OTP_TTL_SECONDS || 600);
        const otpExpiresAt = new Date(Date.now() + ttlSeconds * 1000);

        if (!user) {
            user = await prisma.user.create({
                data: {
                    phone,
                    isVerified: false,
                    otpCodeHash,
                    otpExpiresAt,
                    otpAttempts: 0,
                    otpLastSentAt: now,
                    otpPurpose: normalizedType,
                    verificationCode: null
                }
            });
        } else {
            user = await prisma.user.update({
                where: { id: user.id },
                data: {
                    otpCodeHash,
                    otpExpiresAt,
                    otpAttempts: 0,
                    otpLastSentAt: now,
                    otpPurpose: normalizedType,
                    verificationCode: null
                }
            });
        }

        // Production must have a real delivery provider; never pretend an OTP was sent.
        const providerUrl = process.env.OTP_PROVIDER_URL;
        const providerKey = process.env.OTP_PROVIDER_API_KEY;
        if (process.env.NODE_ENV === 'production' && (!providerUrl || !providerKey)) {
            await prisma.user.update({
                where: { id: user.id },
                data: { otpCodeHash: null, otpExpiresAt: null, otpPurpose: null }
            });
            throw new Error('OTP provider is not configured');
        }

        if (providerUrl && providerKey) {
            const response = await fetch(providerUrl, {
                method: 'POST',
                headers: {
                    'content-type': 'application/json',
                    authorization: `Bearer ${providerKey}`
                },
                body: JSON.stringify({ phone, otp, purpose: normalizedType, expiresIn: ttlSeconds })
            });
            if (!response.ok) {
                await prisma.user.update({
                    where: { id: user.id },
                    data: { otpCodeHash: null, otpExpiresAt: null, otpPurpose: null }
                });
                throw new Error('OTP delivery failed');
            }
        }

        return {
            otpId: user.id,
            message: providerUrl ? `OTP sent to ${phone}` : 'Development OTP generated',
            expiresIn: ttlSeconds,
            ...(process.env.NODE_ENV !== 'production' && !providerUrl ? { debugOtp: otp } : {})
        };
    },

    // Verify OTP with expiry and attempt controls.
    async verifyOtp(phone, otp) {
        const user = await prisma.user.findUnique({ where: { phone } });

        if (!user) {
            throw new Error('User not found');
        }
        if (!user.otpCodeHash || !user.otpExpiresAt || !user.otpPurpose) {
            throw new Error('OTP not requested or already used');
        }
        if (new Date(user.otpExpiresAt).getTime() < Date.now()) {
            throw new Error('OTP expired');
        }

        const maxAttempts = Number(process.env.OTP_MAX_ATTEMPTS || 5);
        if (user.otpAttempts >= maxAttempts) {
            throw new Error('OTP attempt limit exceeded');
        }

        const otpSecret = process.env.OTP_HASH_SECRET || process.env.JWT_SECRET;
        const suppliedHash = crypto.createHmac('sha256', otpSecret).update(`${phone}:${user.otpPurpose}:${otp}`).digest('hex');
        const valid = crypto.timingSafeEqual(Buffer.from(user.otpCodeHash, 'hex'), Buffer.from(suppliedHash, 'hex'));
        if (!valid) {
            await prisma.user.update({
                where: { id: user.id },
                data: { otpAttempts: { increment: 1 } }
            });
            throw new Error('Invalid OTP');
        }

        // Mark verified and clear single-use OTP state.
        await prisma.user.update({
            where: { id: user.id },
            data: {
                isVerified: true,
                verificationCode: null,
                otpCodeHash: null,
                otpExpiresAt: null,
                otpAttempts: 0,
                otpPurpose: null,
                lastLogin: new Date()
            }
        });

        // Check if new user (no profile details)
        const profile = await prisma.profile.findUnique({ where: { userId: user.id } });
        const isNewUser = !profile;

        // Generate tokens
        const tokens = generateTokens(user.id, user.role);

        return {
            user: {
                id: user.id,
                phone: user.phone,
                isNewUser,
                profileCompletion: profile ? profile.completionPercentage : 0
            },
            ...tokens,
            userId: user.id // Returning user_id at top level for convenience
        };
    },

    // Email Login
    async loginEmail(email, password) {
        const user = await prisma.user.findUnique({ where: { email } });

        if (!user || !user.passwordHash) {
            throw new Error('Invalid credentials');
        }

        const isMatch = await comparePassword(password, user.passwordHash);
        if (!isMatch) {
            throw new Error('Invalid credentials');
        }

        // Update last login
        await prisma.user.update({
            where: { id: user.id },
            data: { lastLogin: new Date() }
        });

        const tokens = generateTokens(user.id, user.role);
        return {
            user: {
                id: user.id,
                email: user.email,
                role: user.role
            },
            ...tokens
        };
    },

    // Firebase Login (Mobile & Social)
    async loginWithFirebase(idToken) {
        try {
            // 1. Verify Token
            const decodedToken = await admin.auth().verifyIdToken(idToken);
            const { uid, email, phone_number, name, picture } = decodedToken;

            // 2. Check if user exists by socialId (uid) or email/phone
            let user = await prisma.user.findFirst({
                where: {
                    OR: [
                        { socialId: uid },
                        { email: email || undefined }, // undefined to avoid matching nulls
                        { phone: phone_number || undefined }
                    ]
                }
            });

            if (!user) {
                // 3. Create New User
                const newUser = await prisma.user.create({
                    data: {
                        socialId: uid,
                        email: email || null,
                        phone: phone_number || null,
                        isVerified: !!email, // Email verified by Google usually
                        isActive: true,
                        role: 'user',
                        profile: {
                            create: {
                                firstName: name ? name.split(' ')[0] : 'User',
                                lastName: name ? name.split(' ').slice(1).join(' ') : '',
                            }
                        }
                    }
                });

                // Add photo if provided
                if (picture) {
                    await prisma.photo.create({
                        data: {
                            userId: newUser.id,
                            photoUrl: picture,
                            isPrimary: true
                        }
                    });
                }
                user = newUser;
            } else {
                // 4. Link Social ID if missing
                if (!user.socialId) {
                    user = await prisma.user.update({
                        where: { id: user.id },
                        data: { socialId: uid }
                    });
                }
            }

            // 5. Generate JWT
            const tokens = generateTokens(user.id, user.role);

            // Check profile completion
            const profile = await prisma.profile.findUnique({ where: { userId: user.id } });

            return {
                user: {
                    id: user.id,
                    email: user.email,
                    phone: user.phone,
                    role: user.role,
                    profileCompletion: profile ? profile.completionPercentage : 0
                },
                ...tokens
            };

        } catch (error) {
            console.error("Firebase Login Error:", error);
            throw new Error('Invalid Firebase Token');
        }
    }
};

module.exports = authService;
