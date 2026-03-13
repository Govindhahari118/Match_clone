const prisma = require('../config/prisma'); // Import singleton
const { generateTokens } = require('../utils/jwt');
const { hashPassword, comparePassword } = require('../utils/password');
const admin = require('../config/firebase');
const jwt = require('jsonwebtoken');

const authService = {
    // Request OTP for phone (signup/login)
    async requestOtp(phone, type = 'signup') {
        // Check if user exists or create placeholder
        let user = await prisma.user.findUnique({ where: { phone } });

        // Keep fixed OTP only in development to avoid insecure non-dev defaults.
        const otp = process.env.NODE_ENV === 'development'
            ? '123456'
            : String(Math.floor(100000 + Math.random() * 900000));
        const otpExpiry = new Date(Date.now() + 10 * 60 * 1000); // 10 mins

        if (!user) {
            if (type === 'login') {
                throw new Error('User not found. Please sign up.');
            }
            // Create temporary user record
            user = await prisma.user.create({
                data: {
                    phone,
                    isVerified: false,
                    verificationCode: otp,
                    // We can store expiry if we add a field, or rely on created_at logic
                }
            });
        } else {
            // Update existing user
            await prisma.user.update({
                where: { id: user.id },
                data: { verificationCode: otp }
            });
        }

        return {
            otpId: user.id, // Using user ID as reference for simplicity
            message: `OTP sent to ${phone}`,
            expiresIn: 600
        };
    },

    // Verify OTP
    async verifyOtp(phone, otp) {
        const user = await prisma.user.findUnique({ where: { phone } });

        if (!user) {
            throw new Error('User not found');
        }

        // In production, check expiry
        if (user.verificationCode !== otp) {
            throw new Error('Invalid OTP');
        }

        // Mark verified
        await prisma.user.update({
            where: { id: user.id },
            data: {
                isVerified: true,
                verificationCode: null, // Clear OTP
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
    },

    async refreshAccessToken(refreshToken) {
        if (!refreshToken) {
            throw new Error('Refresh token is required');
        }

        let decoded;
        try {
            decoded = jwt.verify(refreshToken, process.env.JWT_SECRET);
        } catch (_error) {
            throw new Error('Invalid refresh token');
        }

        const user = await prisma.user.findUnique({ where: { id: decoded.sub } });
        if (!user || !user.isActive) {
            throw new Error('User not found');
        }

        const tokens = generateTokens(user.id, user.role);
        return {
            user: {
                id: user.id,
                email: user.email,
                phone: user.phone,
                role: user.role,
            },
            ...tokens,
        };
    }
};

module.exports = authService;
