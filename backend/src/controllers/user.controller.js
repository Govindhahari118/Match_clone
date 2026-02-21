const prisma = require('../config/prisma');

const privacyService = require('../services/privacy.service');

const profileExtensionService = require('../services/profile-extension.service');

const userController = {
    async getProfile(req, res) {
        try {
            const user = await prisma.user.findUnique({
                where: { id: req.user.sub },
                include: { profile: true, photos: true, partnerPreference: true }
            });
            if (!user) return res.status(404).json({ error: 'User not found' });
            const { extension } = await profileExtensionService.getUserProfileExtension(req.user.sub);

            const response = {
                id: user.id,
                email: user.email,
                phone: user.phone,
                role: user.role,
                isVerified: user.isVerified,
                ...(user.profile || {}),
                photos: user.photos,
                partnerPreference: user.partnerPreference,
                hasChildren: extension.hasChildren,
                residentialStatus: extension.residentialStatus,
                district: extension.district
            };

            res.json(response);
        } catch (error) {
            console.error('Get Profile Error:', error);
            res.status(500).json({ error: 'Server error' });
        }
    },

    async updateProfile(req, res) {
        try {
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
                maritalStatus,
                height,
                heightCm,
                educationLevel,
                profession,
                incomeBand,
                videoUrl,
                videoThumbnail,
                birthTime,
                birthPlace,
                gothra,
                zodiacSign,
                nakshatra,
                hasChildren,
                residentialStatus,
                district
            } = req.body;

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
                maritalStatus,
                heightCm: normalizedHeightCm,
                educationLevel,
                profession,
                incomeBand,
                videoUrl,
                videoThumbnail,
                birthTime,
                birthPlace,
                gothra,
                zodiacSign,
                nakshatra
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

            const extension = Object.keys(extensionPayload).length > 0
                ? await profileExtensionService.saveUserProfileExtension(req.user.sub, extensionPayload)
                : (await profileExtensionService.getUserProfileExtension(req.user.sub)).extension;

            res.json({
                ...updatedProfile,
                hasChildren: extension.hasChildren,
                residentialStatus: extension.residentialStatus,
                district: extension.district
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

            res.json({ success: true, message: 'Verification submitted for review' });
        } catch (error) {
            console.error('Verification Error:', error);
            res.status(500).json({ error: 'Failed to submit verification' });
        }
    }
};

module.exports = userController;
