const prisma = require('../config/prisma');

const privacyService = require('../services/privacy.service');

const profileExtensionService = require('../services/profile-extension.service');
const onboardingService = require('../services/onboarding.service');
const { validateProfileUpdate } = require('../validation/profile.validation');

function normalizeStringArray(value) {
    if (Array.isArray(value)) {
        return value
            .map((item) => String(item || '').trim())
            .filter(Boolean);
    }
    if (typeof value === 'string') {
        return value
            .split(',')
            .map((item) => item.trim())
            .filter(Boolean);
    }
    return [];
}

const userController = {
    async getProfile(req, res) {
        try {
            const user = await prisma.user.findUnique({
                where: { id: req.user.sub },
                include: { profile: true, photos: true, partnerPreference: true }
            });
            if (!user) return res.status(404).json({ error: 'User not found' });
            const { extension } = await profileExtensionService.getUserProfileExtension(req.user.sub);
            const progress = await onboardingService.getProgress(req.user.sub);

            const response = {
                id: user.id,
                email: user.email,
                phone: user.phone,
                role: user.role,
                isVerified: user.isVerified,
                identityStatus: user.identityStatus,
                ...(user.profile || {}),
                photos: user.photos,
                partnerPreference: user.partnerPreference,
                hasChildren: extension.hasChildren,
                residentialStatus: extension.residentialStatus,
                district: extension.district,
                fatherOccupation: extension.fatherOccupation,
                motherOccupation: extension.motherOccupation,
                siblingsCount: extension.siblingsCount,
                familyType: extension.familyType,
                personalityQuiz: extension.personalityQuiz,
                onboarding: progress,
            };

            res.json(response);
        } catch (error) {
            console.error('Get Profile Error:', error);
            res.status(500).json({ error: 'Server error' });
        }
    },

    async updateProfile(req, res) {
        try {
            const validation = validateProfileUpdate(req.body);
            if (!validation.success) {
                return res.status(400).json({
                    error: 'Invalid profile payload',
                    details: validation.errors,
                });
            }

            const payload = validation.data;
            const {
                firstName,
                lastName,
                dateOfBirth,
                gender,
                bio,
                city,
                state,
                country,
                religion,
                caste,
                subCaste,
                motherTongue,
                maritalStatus,
                height,
                heightCm,
                educationLevel,
                educationField,
                profession,
                company,
                incomeBand,
                foodHabit,
                drinks,
                smokes,
                hobbies,
                religiousness,
                role,
                intent,
                videoUrl,
                videoThumbnail,
                birthTime,
                birthPlace,
                gothra,
                zodiacSign,
                nakshatra,
                dosha,
                hasChildren,
                residentialStatus,
                district,
                fatherOccupation,
                motherOccupation,
                siblingsCount,
                familyType,
                personalityQuiz
            } = payload;

            const dob = dateOfBirth ? new Date(dateOfBirth) : undefined;
            if (dob && Number.isNaN(dob.getTime())) {
                return res.status(400).json({ error: 'Invalid dateOfBirth' });
            }

            const existingProfile = await prisma.profile.findUnique({
                where: { userId: req.user.sub }
            });

            const normalizedHeightCm = Number.isFinite(Number(heightCm))
                ? Number(heightCm)
                : (Number.isFinite(Number(height)) ? Number(height) : undefined);

            const normalizedHobbies = hobbies !== undefined ? normalizeStringArray(hobbies) : undefined;

            const profileData = {
                firstName,
                lastName,
                dateOfBirth: dob,
                gender,
                bio,
                city,
                state,
                country,
                religion,
                caste,
                subCaste,
                motherTongue,
                maritalStatus,
                heightCm: normalizedHeightCm,
                educationLevel,
                educationField,
                profession,
                company,
                incomeBand,
                foodHabit,
                drinks,
                smokes,
                hobbies: normalizedHobbies,
                religiousness,
                role,
                intent,
                videoUrl,
                videoThumbnail,
                birthTime,
                birthPlace,
                gothra,
                zodiacSign,
                nakshatra,
                dosha
            };

            // Prisma ignores undefined values. Keep this controller safe for partial updates.
            const upsertData = Object.fromEntries(
                Object.entries(profileData).filter(([, value]) => value !== undefined)
            );

            if (!existingProfile) {
                const requiredFields = ['firstName', 'dateOfBirth', 'gender', 'maritalStatus'];
                const missing = requiredFields.filter((field) => upsertData[field] === undefined);
                if (missing.length > 0) {
                    return res.status(400).json({
                        error: `Missing required fields for new profile: ${missing.join(', ')}`
                    });
                }
            }

            const updatedProfile = await prisma.profile.upsert({
                where: { userId: req.user.sub },
                update: upsertData,
                create: {
                    userId: req.user.sub,
                    ...upsertData
                }
            });

            const extensionPayload = {};
            if (hasChildren !== undefined) extensionPayload.hasChildren = hasChildren;
            if (residentialStatus !== undefined) extensionPayload.residentialStatus = residentialStatus;
            if (district !== undefined) extensionPayload.district = district;
            if (fatherOccupation !== undefined) extensionPayload.fatherOccupation = fatherOccupation;
            if (motherOccupation !== undefined) extensionPayload.motherOccupation = motherOccupation;
            if (siblingsCount !== undefined) extensionPayload.siblingsCount = siblingsCount;
            if (familyType !== undefined) extensionPayload.familyType = familyType;
            if (personalityQuiz !== undefined) extensionPayload.personalityQuiz = personalityQuiz;

            const extension = Object.keys(extensionPayload).length > 0
                ? await profileExtensionService.saveUserProfileExtension(req.user.sub, extensionPayload)
                : (await profileExtensionService.getUserProfileExtension(req.user.sub)).extension;
            const completeness = await onboardingService.refreshCompleteness(req.user.sub);

            res.json({
                ...updatedProfile,
                hasChildren: extension.hasChildren,
                residentialStatus: extension.residentialStatus,
                district: extension.district,
                fatherOccupation: extension.fatherOccupation,
                motherOccupation: extension.motherOccupation,
                siblingsCount: extension.siblingsCount,
                familyType: extension.familyType,
                personalityQuiz: extension.personalityQuiz,
                onboardingCompleteness: completeness,
            });
        } catch (error) {
            console.error('Update Profile Error:', error);
            res.status(500).json({ error: 'Update failed' });
        }
    },

    async updatePassword(req, res) {
        // Mock implementation
        res.json({ message: 'Password updated successfully (Demo)' });
    },

    async getPrivacySettings(req, res) {
        try {
            const { settings, updatedAt } = await privacyService.getUserPrivacySettings(req.user.sub);

            res.json({
                success: true,
                settings,
                updatedAt,
            });
        } catch (error) {
            console.error('Get privacy settings error:', error);
            res.status(500).json({ error: 'Failed to load privacy settings' });
        }
    },

    async updatePrivacySettings(req, res) {
        try {
            const settings = privacyService.normalizePrivacySettings(
                req.body && typeof req.body === 'object' ? req.body : {}
            );
            await prisma.auditLog.create({
                data: {
                    userId: req.user.sub,
                    action: 'privacy_settings_updated',
                    resourceType: 'user',
                    resourceId: req.user.sub,
                    changes: settings,
                },
            });

            res.json({ success: true, settings });
        } catch (error) {
            console.error('Update privacy settings error:', error);
            res.status(500).json({ error: 'Failed to update privacy settings' });
        }
    },

    async submitVerification(req, res) {
        try {
            const { docUrl, docType } = req.body;
            if (!docUrl) {
                return res.status(400).json({ error: 'docUrl is required' });
            }

            const verificationType = docType || 'id';

            await prisma.user.update({
                where: { id: req.user.sub },
                data: {
                    identityStatus: 'pending',
                    identityDocUrl: docUrl
                }
            });

            // Respect unique (userId, type) constraint in schema.
            await prisma.verification.upsert({
                where: {
                    userId_type: {
                        userId: req.user.sub,
                        type: verificationType
                    }
                },
                update: {
                    documentUrl: docUrl,
                    status: 'pending',
                    rejectionReason: null
                },
                create: {
                    userId: req.user.sub,
                    type: verificationType,
                    documentUrl: docUrl,
                    status: 'pending'
                }
            });

            await prisma.auditLog.create({
                data: {
                    userId: req.user.sub,
                    action: 'verification_submitted',
                    resourceType: 'verification',
                    resourceId: req.user.sub,
                    changes: {
                        type: verificationType,
                        status: 'pending',
                        documentSubmitted: true,
                    },
                    ipAddress: req.ip || null,
                    userAgent: req.get('user-agent') || null,
                },
            });
            const completeness = await onboardingService.refreshCompleteness(req.user.sub);

            res.json({ success: true, message: 'Verification submitted for review', onboardingCompleteness: completeness });
        } catch (error) {
            console.error('Verification Error:', error);
            res.status(500).json({ error: 'Failed to submit verification' });
        }
    },

    async getOnboardingProgress(req, res) {
        try {
            const progress = await onboardingService.getProgress(req.user.sub);
            return res.json(progress);
        } catch (error) {
            console.error('Get onboarding progress error:', error);
            return res.status(500).json({ error: 'Failed to load onboarding progress' });
        }
    },

    async saveOnboardingProgress(req, res) {
        try {
            const state = await onboardingService.saveResumeState(req.user.sub, req.body || {});
            return res.json({ success: true, state });
        } catch (error) {
            console.error('Save onboarding progress error:', error);
            return res.status(500).json({ error: 'Failed to save onboarding progress' });
        }
    }
};

module.exports = userController;
