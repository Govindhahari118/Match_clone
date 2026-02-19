const prisma = require('../config/prisma');

const profileService = {
    // Get user profile
    async getProfile(userId) {
        return await prisma.profile.findUnique({
            where: { userId },
            include: {
                user: { select: { phone: true, email: true, isVerified: true } },
                photos: true
            }
        });
    },

    // Update profile step-by-step
    async updateProfile(userId, data) {
        const { step, ...profileData } = data;

        // Check if profile exists
        let profile = await prisma.profile.findUnique({ where: { userId } });

        // Format usage for specific fields
        if (profileData.date_of_birth) {
            profileData.date_of_birth = new Date(profileData.date_of_birth);
        }
        if (profileData.hobbies && typeof profileData.hobbies === 'string') {
            profileData.hobbies = profileData.hobbies.split(',').map(h => h.trim());
        }

        // Calculate completion percentage based on step or fields
        let completionPercentage = profile ? profile.completionPercentage : 0;
        if (step === 1) completionPercentage = 25;
        if (step === 2) completionPercentage = 50;
        if (step === 3) completionPercentage = 75;
        if (step === 4) completionPercentage = 100;

        if (!profile) {
            // Create new profile
            return await prisma.profile.create({
                data: {
                    userId,
                    ...profileData,
                    completionPercentage
                }
            });
        } else {
            // Update existing
            return await prisma.profile.update({
                where: { userId },
                data: {
                    ...profileData,
                    completionPercentage,
                    updatedAt: new Date()
                }
            });
        }
    }
};

module.exports = profileService;
