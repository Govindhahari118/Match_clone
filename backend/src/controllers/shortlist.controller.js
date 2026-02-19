const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

exports.shortlistProfile = async (req, res) => {
    try {
        const userId = req.user.userId;
        const { shortlistedUserId } = req.body;

        if (!userId || !shortlistedUserId) return res.status(400).json({ error: "Missing parameters" });

        // Check if exists
        const existing = await prisma.shortlist.findUnique({
            where: {
                userId_shortlistedUserId: { userId, shortlistedUserId }
            }
        });

        if (existing) return res.status(200).json({ message: "Already shortlisted", id: existing.id });

        const shortlist = await prisma.shortlist.create({
            data: {
                userId,
                shortlistedUserId
            }
        });

        res.status(201).json(shortlist);
    } catch (error) {
        console.error("Shortlist error:", error);
        res.status(500).json({ error: "Failed to shortlist profile" });
    }
};

exports.removeShortlist = async (req, res) => {
    try {
        const userId = req.user.userId;
        const { shortlistedUserId } = req.body;

        await prisma.shortlist.delete({
            where: {
                userId_shortlistedUserId: { userId, shortlistedUserId }
            }
        });

        res.json({ message: "Removed from shortlist" });
    } catch (error) {
        console.error("Remove Shortlist error:", error);
        res.status(500).json({ error: "Failed to remove" });
    }
};

exports.getShortlistedProfiles = async (req, res) => {
    try {
        const userId = req.user.userId;
        const shortlists = await prisma.shortlist.findMany({
            where: { userId },
            include: {
                shortlistedUser: {
                    select: {
                        id: true,
                        profile: {
                            include: { photos: true }
                        }
                    }
                }
            }
        });

        // Transform for frontend
        const profiles = shortlists.map(s => ({
            ...s.shortlistedUser.profile,
            userId: s.shortlistedUser.id,
            shortlistId: s.id
        })).filter(p => p !== null);

        res.json(profiles);
    } catch (e) {
        console.error("Get Shortlist error:", e);
        res.status(500).json({ error: e.message });
    }
};
