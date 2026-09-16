const crypto = require('crypto');
const prisma = require('../config/prisma');
const { generateTokens } = require('../utils/jwt');
const { comparePassword } = require('../utils/password');
const admin = require('../config/firebase');

const OTP_TTL_MS = 10 * 60 * 1000;
const OTP_RESEND_MS = 60 * 1000;
const OTP_MAX_ATTEMPTS = 5;

function appError(message, statusCode = 400) {
    const error = new Error(message);
    error.statusCode = statusCode;
    return error;
}

function otpSecret() {
    const secret = process.env.OTP_HASH_SECRET || process.env.JWT_SECRET;
    if (!secret) throw appError('OTP security secret is not configured', 503);
    return secret;
}

function hashOtp(phone, otp) {
    return crypto.createHmac('sha256', otpSecret()).update(`${phone}:${otp}`).digest('hex');
}

function safeEqualHex(a, b) {
    const left = Buffer.from(String(a || ''), 'hex');
    const right = Buffer.from(String(b || ''), 'hex');
    return left.length === right.length && left.length > 0 && crypto.timingSafeEqual(left, right);
}

async function deliverOtp(phone, otp, type) {
    if (process.env.OTP_WEBHOOK_URL) {
        const response = await fetch(process.env.OTP_WEBHOOK_URL, {
            method: 'POST',
            headers: {
                'content-type': 'application/json',
                ...(process.env.OTP_WEBHOOK_TOKEN ? { authorization: `Bearer ${process.env.OTP_WEBHOOK_TOKEN}` } : {}),
            },
            body: JSON.stringify({ phone, otp, type }),
        });
        if (!response.ok) throw appError('OTP delivery provider failed', 503);
        return { delivered: true, devOtp: null };
    }

    if (process.env.NODE_ENV === 'production') {
        throw appError('OTP delivery provider is not configured', 503);
    }

    // Development only. Never expose or log OTPs in production.
    console.warn(`[DEV OTP] ${phone}: ${otp}`);
    return { delivered: true, devOtp: otp };
}

async function latestOtpIssue(userId) {
    return prisma.auditLog.findFirst({
        where: { userId, action: 'otp_issued' },
        orderBy: { createdAt: 'desc' },
    });
}

const authService = {
    async requestOtp(phoneInput, type = 'signup') {
        const phone = String(phoneInput || '').trim();
        const normalizedType = type === 'login' ? 'login' : 'signup';
        let user = await prisma.user.findUnique({ where: { phone } });

        if (!user && normalizedType === 'login') throw appError('User not found. Please sign up.', 404);
        if (!user) {
            user = await prisma.user.create({
                data: { phone, isVerified: false, isActive: true },
            });
        }
        if (user.isBanned) throw appError('Account access is restricted', 403);

        const previous = await latestOtpIssue(user.id);
        if (previous && Date.now() - new Date(previous.createdAt).getTime() < OTP_RESEND_MS) {
            const remaining = Math.ceil((OTP_RESEND_MS - (Date.now() - new Date(previous.createdAt).getTime())) / 1000);
            throw appError(`Please wait ${remaining}s before requesting another OTP`, 429);
        }

        const otp = crypto.randomInt(0, 1000000).toString().padStart(6, '0');
        const expiresAt = new Date(Date.now() + OTP_TTL_MS);
        const verificationCode = hashOtp(phone, otp);

        await prisma.$transaction([
            prisma.user.update({ where: { id: user.id }, data: { verificationCode } }),
            prisma.auditLog.create({
                data: {
                    userId: user.id,
                    action: 'otp_issued',
                    resourceType: 'auth',
                    resourceId: user.id,
                    changes: { type: normalizedType, expiresAt: expiresAt.toISOString() },
                },
            }),
        ]);

        try {
            const delivery = await deliverOtp(phone, otp, normalizedType);
            return {
                otpId: user.id,
                message: `OTP sent to ${phone}`,
                expiresIn: Math.floor(OTP_TTL_MS / 1000),
                resendAfter: Math.floor(OTP_RESEND_MS / 1000),
                devOtp: delivery.devOtp,
            };
        } catch (error) {
            await prisma.$transaction([
                prisma.user.update({ where: { id: user.id }, data: { verificationCode: null } }),
                prisma.auditLog.create({
                    data: {
                        userId: user.id,
                        action: 'otp_delivery_failed',
                        resourceType: 'auth',
                        resourceId: user.id,
                    },
                }),
            ]);
            throw error;
        }
    },

    async verifyOtp(phoneInput, otpInput) {
        const phone = String(phoneInput || '').trim();
        const otp = String(otpInput || '').trim();
        const user = await prisma.user.findUnique({ where: { phone } });
        if (!user) throw appError('User not found', 404);
        if (!user.verificationCode) throw appError('No active OTP request. Request a new OTP.', 400);
        if (user.isBanned) throw appError('Account access is restricted', 403);

        const issue = await latestOtpIssue(user.id);
        const expiresAt = issue?.changes?.expiresAt ? new Date(issue.changes.expiresAt) : null;
        if (!issue || !expiresAt || Number.isNaN(expiresAt.getTime()) || expiresAt <= new Date()) {
            await prisma.user.update({ where: { id: user.id }, data: { verificationCode: null } });
            throw appError('OTP expired. Request a new OTP.', 400);
        }

        const failedAttempts = await prisma.auditLog.count({
            where: {
                userId: user.id,
                action: 'otp_verification_failed',
                createdAt: { gte: issue.createdAt },
            },
        });
        if (failedAttempts >= OTP_MAX_ATTEMPTS) {
            await prisma.user.update({ where: { id: user.id }, data: { verificationCode: null } });
            throw appError('Too many incorrect attempts. Request a new OTP.', 429);
        }

        const candidateHash = hashOtp(phone, otp);
        if (!safeEqualHex(user.verificationCode, candidateHash)) {
            await prisma.auditLog.create({
                data: {
                    userId: user.id,
                    action: 'otp_verification_failed',
                    resourceType: 'auth',
                    resourceId: user.id,
                },
            });
            throw appError('Invalid OTP', 400);
        }

        const now = new Date();
        await prisma.$transaction([
            prisma.user.update({
                where: { id: user.id },
                data: { isVerified: true, verificationCode: null, lastLogin: now, lastActiveAt: now },
            }),
            prisma.verification.upsert({
                where: { userId_type: { userId: user.id, type: 'phone' } },
                update: { status: 'verified', verifiedAt: now, rejectionReason: null },
                create: { userId: user.id, type: 'phone', status: 'verified', verifiedAt: now },
            }),
            prisma.auditLog.create({
                data: { userId: user.id, action: 'otp_verified', resourceType: 'auth', resourceId: user.id },
            }),
        ]);

        const profile = await prisma.profile.findUnique({ where: { userId: user.id } });
        const tokens = generateTokens(user.id, user.role);
        return {
            user: {
                id: user.id,
                phone: user.phone,
                isNewUser: !profile,
                profileCompletion: profile?.completionPercentage || 0,
            },
            ...tokens,
            userId: user.id,
        };
    },

    async loginEmail(email, password) {
        const user = await prisma.user.findUnique({ where: { email } });
        if (!user || !user.passwordHash || !(await comparePassword(password, user.passwordHash))) {
            throw appError('Invalid credentials', 401);
        }
        if (user.isBanned) throw appError('Account access is restricted', 403);

        const now = new Date();
        await prisma.user.update({ where: { id: user.id }, data: { lastLogin: now, lastActiveAt: now } });
        const tokens = generateTokens(user.id, user.role);
        return { user: { id: user.id, email: user.email, role: user.role }, ...tokens };
    },

    async loginWithFirebase(idToken) {
        try {
            const decoded = await admin.auth().verifyIdToken(idToken);
            const { uid, email, email_verified, phone_number, name, picture } = decoded;
            const identityCandidates = [{ socialId: uid }];
            if (email) identityCandidates.push({ email });
            if (phone_number) identityCandidates.push({ phone: phone_number });

            let user = await prisma.user.findFirst({ where: { OR: identityCandidates } });
            if (!user) {
                user = await prisma.user.create({
                    data: {
                        socialId: uid,
                        socialProvider: 'firebase',
                        email: email || null,
                        phone: phone_number || null,
                        isVerified: Boolean(email_verified || phone_number),
                        isActive: true,
                        role: 'user',
                        lastLogin: new Date(),
                        lastActiveAt: new Date(),
                    },
                });
                if (picture) {
                    await prisma.photo.create({
                        data: {
                            userId: user.id,
                            photoUrl: picture,
                            thumbnailUrl: picture,
                            isPrimary: false,
                            moderationStatus: 'review_required',
                            verificationStatus: 'pending',
                        },
                    });
                }
                if (name) {
                    await prisma.auditLog.create({
                        data: {
                            userId: user.id,
                            action: 'social_profile_hint',
                            resourceType: 'auth',
                            resourceId: user.id,
                            changes: { suggestedName: name },
                        },
                    });
                }
            } else {
                if (user.isBanned) throw appError('Account access is restricted', 403);
                user = await prisma.user.update({
                    where: { id: user.id },
                    data: { socialId: user.socialId || uid, socialProvider: user.socialProvider || 'firebase', lastLogin: new Date(), lastActiveAt: new Date() },
                });
            }

            const verificationWrites = [];
            const now = new Date();
            if (email && email_verified) {
                verificationWrites.push(prisma.verification.upsert({
                    where: { userId_type: { userId: user.id, type: 'email' } },
                    update: { status: 'verified', verifiedAt: now },
                    create: { userId: user.id, type: 'email', status: 'verified', verifiedAt: now },
                }));
            }
            if (phone_number) {
                verificationWrites.push(prisma.verification.upsert({
                    where: { userId_type: { userId: user.id, type: 'phone' } },
                    update: { status: 'verified', verifiedAt: now },
                    create: { userId: user.id, type: 'phone', status: 'verified', verifiedAt: now },
                }));
            }
            if (verificationWrites.length) await prisma.$transaction(verificationWrites);

            const tokens = generateTokens(user.id, user.role);
            const profile = await prisma.profile.findUnique({ where: { userId: user.id } });
            return {
                user: {
                    id: user.id,
                    email: user.email,
                    phone: user.phone,
                    role: user.role,
                    isNewUser: !profile,
                    profileCompletion: profile?.completionPercentage || 0,
                },
                ...tokens,
            };
        } catch (error) {
            console.error('Firebase Login Error:', error);
            if (error.statusCode) throw error;
            throw appError('Invalid Firebase token', 401);
        }
    },
};

module.exports = authService;
