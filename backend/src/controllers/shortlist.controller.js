const prisma = require('../config/prisma');

exports.shortlistProfile = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { shortlistedUserId } = req.body;

        if (!userId || !shortlistedUserId) return res.status(400).json({ error: "Missing parameters" });

        if (userId === shortlistedUserId) return res.status(400).json({ error: "Cannot shortlist yourself" });

        const blocked = await prisma.block.findFirst({
            where: {
                OR: [
                    { blockerId: userId, blockedId: shortlistedUserId },
                    { blockerId: shortlistedUserId, blockedId: userId }
                ]
            }
        });
        if (blocked) return res.status(403).json({ error: "Profile unavailable" });

        const target = await prisma.user.findUnique({ where: { id: shortlistedUserId }, select: { isActive: true, isBanned: true } });
        if (!target || !target.isActive || target.isBanned) return res.status(404).json({ error: "Profile unavailable" });

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
        const userId = req.user.sub;
        const { shortlistedUserId } = req.body;

        await prisma.shortlist.deleteMany({
            where: { userId, shortlistedUserId }
        });

        res.json({ message: "Removed from shortlist" });
    } catch (error) {
        console.error("Remove Shortlist error:", error);
        res.status(500).json({ error: "Failed to remove" });
    }
};

exports.getShortlistedProfiles = async (req, res) => {
    try {
        const userId = req.user.sub;
        const blockedPairs = await prisma.block.findMany({
            where: { OR: [{ blockerId: userId }, { blockedId: userId }] },
            select: { blockerId: true, blockedId: true }
        });
        const blockedIds = new Set(blockedPairs.map(b => b.blockerId === userId ? b.blockedId : b.blockerId));

        const shortlists = await prisma.shortlist.findMany({
            where: { userId, shortlistedUserId: { notIn: Array.from(blockedIds) } },
            include: {
                shortlistedUser: {
                    select: {
                        id: true,
                        isActive: true,
                        isBanned: true,
                        profile: {
                            include: { photos: true }
                        }
                    }
                }
            }
        });

        // Transform for frontend
        const profiles = shortlists
            .filter(s => s.shortlistedUser?.isActive && !s.shortlistedUser?.isBanned && s.shortlistedUser?.profile)
            .map(s => ({
                ...s.shortlistedUser.profile,
                userId: s.shortlistedUser.id,
                shortlistId: s.id,
                shortlistedAt: s.createdAt
            }));

        res.json(profiles);
    } catch (e) {
        console.error("Get Shortlist error:", e);
        res.status(500).json({ error: e.message });
    }
};
