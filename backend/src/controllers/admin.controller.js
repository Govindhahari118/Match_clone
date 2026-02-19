const prisma = require('../config/prisma');

const getDashboardStats = async (req, res) => {
    try {
        const totalUsers = await prisma.user.count();
        const activeSubscriptions = await prisma.subscription.count({ where: { status: 'active' } });
        const pendingReports = await prisma.report.count({ where: { status: 'open' } });

        // Example: Monthly revenue
        // const revenue = await prisma.payment.aggregate({ _sum: { amountInr: true } });

        res.json({
            totalUsers,
            activeSubscriptions,
            pendingReports,
            revenue: 25000 // Mock
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

const getAllUsers = async (req, res) => {
    try {
        const users = await prisma.user.findMany({
            select: { id: true, email: true, phone: true, role: true, isBanned: true, profile: { select: { firstName: true, lastName: true } } },
            take: 20
        });
        res.json(users);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

const banUser = async (req, res) => {
    try {
        const { userId } = req.body;
        await prisma.user.update({
            where: { id: userId },
            data: { isBanned: true }
        });
        res.json({ success: true, message: "User banned successfully" });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

module.exports = { getDashboardStats, getAllUsers, banUser };
