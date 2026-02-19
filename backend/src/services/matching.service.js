const prisma = require('../config/prisma');

const matchingService = {
    async getMatches(userId, filters = {}) {
        // 1. Get user preferences to exclude blocked/rejected users
        const user = await prisma.user.findUnique({
            where: { id: userId },
            include: {
                likesSent: { select: { receiverId: true } }, // Exclude people I already liked/rejected
                likesReceived: { where: { status: 'rejected' }, select: { senderId: true } }, // Exclude people who rejected me? Or just I rejected.
                matches: { select: { userBId: true } },      // Exclude existing matches
                matchesAsUserB: { select: { userAId: true } }
            }
        });

        const excludeIds = [
            userId,
            ...user.likesSent.map(l => l.receiverId),
            ...user.matches.map(m => m.userBId),
            ...user.matchesAsUserB.map(m => m.userAId)
        ];

        // 2. Build Query
        const where = {
            id: { notIn: excludeIds },
            isBanned: false,
            isActive: true,
            profile: {
                is: {
                    gender: filters.gender, // Mandatory filter typically
                }
            }
        };

        // Apply Filters
        if (filters.minAge) {
            const maxBirthDate = new Date();
            maxBirthDate.setFullYear(maxBirthDate.getFullYear() - parseInt(filters.minAge));
            where.profile.is.dateOfBirth = { ...where.profile.is.dateOfBirth, lte: maxBirthDate };
        }
        if (filters.maxAge) {
            const minBirthDate = new Date();
            minBirthDate.setFullYear(minBirthDate.getFullYear() - parseInt(filters.maxAge) - 1);
            where.profile.is.dateOfBirth = { ...where.profile.is.dateOfBirth, gte: minBirthDate };
        }

        if (filters.religion && filters.religion !== 'Any') where.profile.is.religion = filters.religion;
        if (filters.caste && filters.caste !== 'Any') where.profile.is.caste = filters.caste;
        if (filters.maritalStatus && filters.maritalStatus !== 'Any') where.profile.is.maritalStatus = filters.maritalStatus;
        if (filters.city) where.profile.is.city = { contains: filters.city, mode: 'insensitive' };
        if (filters.state) where.profile.is.state = { contains: filters.state, mode: 'insensitive' };

        // Advanced Filters
        if (filters.education && filters.education !== 'Any') where.profile.is.educationLevel = filters.education;
        if (filters.profession && filters.profession !== 'Any') where.profile.is.profession = filters.profession;
        if (filters.income && filters.income !== 'Any') where.profile.is.incomeBand = filters.income;

        // 3. Execute Query
        const profiles = await prisma.user.findMany({
            where,
            take: 50,
            include: {
                profile: true,
                photos: { where: { isPrimary: true }, take: 1 }
            }
        });

        // 4. Transform & Calculate Match Score
        return profiles.map(u => {
            const p = u.profile;
            const photo = u.photos[0]?.thumbnailUrl || u.photos[0]?.photoUrl || "https://via.placeholder.com/150";
            const age = new Date().getFullYear() - new Date(p.dateOfBirth).getFullYear();

            // Simple match score randomization for demo (real would use preferences)
            const matchScore = 70 + Math.floor(Math.random() * 25);

            return {
                id: u.id,
                userId: u.id,
                firstName: p.firstName,
                lastName: p.lastName,
                age,
                city: p.city,
                profession: p.profession,
                photo,
                isVerified: u.isVerified,
                match: matchScore,
                religion: p.religion,
                education: p.educationLevel,
                income: p.incomeBand
            };
        });
    }
};

module.exports = matchingService;
