const prisma = require('../config/prisma');
const { comparePassword } = require('../utils/password');
const privacyService = require('../services/privacy.service');
const profileExtensionService = require('../services/profile-extension.service');
const trustService = require('../services/trust.service');

function sendError(res, error, fallback = 'Request failed') {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? fallback : error.message });
}

const userController = {
    async getProfile(req, res) {
        try {
            const user = await prisma.user.findUnique({
                where: { id: req.user.sub },
                include: { profile: true, photos: true, partnerPreference: true, verifications: true },
            });
            if (!user) return res.status(404).json({ error: 'User not found' });
            const [{ extension }, trust] = await Promise.all([
                profileExtensionService.getUserProfileExtension(req.user.sub),
                trustService.getTrustSummary(req.user.sub),
            ]);

            return res.json({
                id: user.id,
                email: user.email,
                phone: user.phone,
                role: user.role,
                isVerified: user.isVerified,
                profileVersion: user.profileVersion,
                searchStatus: user.searchStatus,
                statusReconfirmedAt: user.statusReconfirmedAt,
                ...(user.profile || {}),
                photos: user.photos,
                partnerPreference: user.partnerPreference,
                hasChildren: extension.hasChildren,
                residentialStatus: extension.residentialStatus,
                district: extension.district,
                trust,
            });
        } catch (error) {
            console.error('Get Profile Error:', error);
            return sendError(res, error, 'Server error');
        }
    },

    async updateProfile(req, res) {
        try {
            const body = req.body || {};
            const dob = body.dateOfBirth ? new Date(body.dateOfBirth) : undefined;
            if (dob && Number.isNaN(dob.getTime())) return res.status(400).json({ error: 'Invalid dateOfBirth' });

            const currentUser = await prisma.user.findUnique({
                where: { id: req.user.sub },
                select: { profileVersion: true },
            });
            if (!currentUser) return res.status(404).json({ error: 'User not found' });

            const expectedVersion = Number.parseInt(body.expectedProfileVersion, 10);
            if (Number.isFinite(expectedVersion) && expectedVersion !== currentUser.profileVersion) {
                return res.status(409).json({
                    error: 'Profile was updated on another device. Refresh before saving.',
                    currentProfileVersion: currentUser.profileVersion,
                });
            }

            const existingProfile = await prisma.profile.findUnique({ where: { userId: req.user.sub } });
            const normalizedHeightCm = Number.isFinite(Number(body.heightCm))
                ? Number(body.heightCm)
                : (Number.isFinite(Number(body.height)) ? Number(body.height) : undefined);
            const profileData = {
                firstName: body.firstName,
                lastName: body.lastName,
                dateOfBirth: dob,
                gender: body.gender,
                bio: body.bio,
                city: body.city,
                state: body.state,
                country: body.country,
                religion: body.religion,
                caste: body.caste,
                subCaste: body.subCaste,
                motherTongue: body.motherTongue,
                maritalStatus: body.maritalStatus,
                heightCm: normalizedHeightCm,
                educationLevel: body.educationLevel,
                educationField: body.educationField,
                profession: body.profession,
                company: body.company,
                incomeBand: body.incomeBand,
                foodHabit: body.foodHabit,
                drinks: body.drinks,
                smokes: body.smokes,
                hobbies: Array.isArray(body.hobbies) ? body.hobbies : undefined,
                religiousness: body.religiousness,
                role: body.managedBy || body.profileRole || body.role,
                managerRelationship: body.managerRelationship,
                intent: body.intent,
                videoUrl: body.videoUrl,
                videoThumbnail: body.videoThumbnail,
                birthTime: body.birthTime,
                birthPlace: body.birthPlace,
                gothra: body.gothra,
                zodiacSign: body.zodiacSign,
                nakshatra: body.nakshatra,
            };
            const upsertData = Object.fromEntries(Object.entries(profileData).filter(([, value]) => value !== undefined));

            if (!existingProfile) {
                const missing = ['firstName', 'dateOfBirth', 'gender', 'maritalStatus'].filter((field) => upsertData[field] === undefined);
                if (missing.length) return res.status(400).json({ error: `Missing required fields for new profile: ${missing.join(', ')}` });
            }

            const now = new Date();
            const result = await prisma.$transaction(async (tx) => {
                const updatedProfile = await tx.profile.upsert({
                    where: { userId: req.user.sub },
                    update: upsertData,
                    create: { userId: req.user.sub, ...upsertData },
                });
                const updatedUser = await tx.user.update({
                    where: { id: req.user.sub },
                    data: { profileVersion: { increment: 1 }, lastActiveAt: now },
                    select: { profileVersion: true },
                });
                await tx.auditLog.create({
                    data: {
                        userId: req.user.sub,
                        action: 'profile_updated',
                        resourceType: 'profile',
                        resourceId: updatedProfile.id,
                        changes: {
                            changedFields: Object.keys(upsertData),
                            previousVersion: currentUser.profileVersion,
                            newVersion: updatedUser.profileVersion,
                        },
                    },
                });
                return { updatedProfile, updatedUser };
            });

            const extensionPayload = {};
            if (body.hasChildren !== undefined) extensionPayload.hasChildren = body.hasChildren;
            if (body.residentialStatus !== undefined) extensionPayload.residentialStatus = body.residentialStatus;
            if (body.district !== undefined) extensionPayload.district = body.district;
            const extension = Object.keys(extensionPayload).length
                ? await profileExtensionService.saveUserProfileExtension(req.user.sub, extensionPayload)
                : (await profileExtensionService.getUserProfileExtension(req.user.sub)).extension;

            const trust = await trustService.getTrustSummary(req.user.sub);
            if (trust && result.updatedProfile.completionPercentage !== trust.profileCompleteness) {
                await prisma.profile.update({
                    where: { userId: req.user.sub },
                    data: { completionPercentage: trust.profileCompleteness },
                });
            }

            return res.json({
                ...result.updatedProfile,
                completionPercentage: trust?.profileCompleteness ?? result.updatedProfile.completionPercentage,
                profileVersion: result.updatedUser.profileVersion,
                hasChildren: extension.hasChildren,
                residentialStatus: extension.residentialStatus,
                district: extension.district,
                trust,
            });
        } catch (error) {
            console.error('Update Profile Error:', error);
            return sendError(res, error, 'Update failed');
        }
    },

    async updatePassword(req, res) {
        return res.status(501).json({ error: 'Password change requires the secure reauthentication flow and is not available through this legacy endpoint.' });
    },

    async getPrivacySettings(req, res) {
        try {
            const { settings, updatedAt } = await privacyService.getUserPrivacySettings(req.user.sub);
            return res.json({ success: true, settings, updatedAt });
        } catch (error) {
            return sendError(res, error, 'Failed to load privacy settings');
        }
    },

    async updatePrivacySettings(req, res) {
        try {
            const settings = privacyService.normalizePrivacySettings(req.body && typeof req.body === 'object' ? req.body : {});
            await prisma.auditLog.create({
                data: {
                    userId: req.user.sub,
                    action: 'privacy_settings_updated',
                    resourceType: 'user',
                    resourceId: req.user.sub,
                    changes: settings,
                },
            });
            return res.json({ success: true, settings });
        } catch (error) {
            return sendError(res, error, 'Failed to update privacy settings');
        }
    },

    async submitVerification(req, res) {
        try {
            const { docUrl, docType } = req.body || {};
            if (!docUrl) return res.status(400).json({ error: 'docUrl is required' });
            const verificationType = docType || 'id';
            if (!['id', 'education', 'employment', 'photo', 'selfie'].includes(verificationType)) {
                return res.status(400).json({ error: 'Unsupported verification type' });
            }

            await prisma.$transaction(async (tx) => {
                if (verificationType === 'id') {
                    await tx.user.update({
                        where: { id: req.user.sub },
                        data: { identityStatus: 'pending', identityDocUrl: docUrl, lastActiveAt: new Date() },
                    });
                }
                await tx.verification.upsert({
                    where: { userId_type: { userId: req.user.sub, type: verificationType } },
                    update: { documentUrl: docUrl, status: 'pending', rejectionReason: null },
                    create: { userId: req.user.sub, type: verificationType, documentUrl: docUrl, status: 'pending' },
                });
                await tx.auditLog.create({
                    data: {
                        userId: req.user.sub,
                        action: 'verification_submitted',
                        resourceType: 'verification',
                        resourceId: req.user.sub,
                        changes: { type: verificationType, status: 'pending' },
                    },
                });
            });
            return res.json({ success: true, message: 'Verification submitted for review', type: verificationType });
        } catch (error) {
            return sendError(res, error, 'Failed to submit verification');
        }
    },

    async deleteMatrimonyProfile(req, res) {
        try {
            const { confirmation, password } = req.body || {};
            if (confirmation !== 'DELETE') return res.status(400).json({ error: 'Type DELETE to confirm profile deletion' });

            const user = await prisma.user.findUnique({ where: { id: req.user.sub }, select: { id: true, passwordHash: true } });
            if (!user) return res.status(404).json({ error: 'User not found' });
            if (user.passwordHash) {
                if (!password || !(await comparePassword(password, user.passwordHash))) {
                    return res.status(401).json({ error: 'Reauthentication failed' });
                }
            }

            const now = new Date();
            await prisma.$transaction(async (tx) => {
                await tx.user.update({
                    where: { id: req.user.sub },
                    data: { searchStatus: 'closed', deletedAt: now, statusReconfirmedAt: now },
                });
                await tx.match.updateMany({
                    where: { OR: [{ userAId: req.user.sub }, { userBId: req.user.sub }] },
                    data: { isActive: false },
                });
                await tx.auditLog.create({
                    data: {
                        userId: req.user.sub,
                        action: 'matrimony_profile_deleted',
                        resourceType: 'user',
                        resourceId: req.user.sub,
                        changes: { publicVisibilityRemovedAt: now.toISOString() },
                    },
                });
            });
            return res.json({ success: true, publicVisibilityRemoved: true });
        } catch (error) {
            return sendError(res, error, 'Failed to delete matrimony profile');
        }
    },
};

module.exports = userController;
