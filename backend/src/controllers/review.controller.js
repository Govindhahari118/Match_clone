const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();

const createReview = async (req, res) => {
    try {
        const { reviewedUserId, rating, comment } = req.body;
        const reviewerId = req.user.id; // From auth middleware

        if (reviewerId === reviewedUserId) {
            return res.status(400).json({ error: "Cannot review yourself" });
        }

        const review = await prisma.review.create({
            data: {
                reviewerId,
                reviewedUserId,
                rating,
                comment,
            },
            include: {
                reviewer: {
                    select: {
                        id: true,
                        // Fetch primary photo for UI
                        photos: { where: { isPrimary: true }, select: { photoUrl: true }, take: 1 },
                        profile: { select: { firstName: true, lastName: true } }
                    }
                }
            }
        });

        // Return structured response
        const formatted = {
            id: review.id,
            reviewerId: review.reviewer.id,
            reviewerName: review.reviewer.profile ? `${review.reviewer.profile.firstName} ${review.reviewer.profile.lastName || ''}`.trim() : "User",
            reviewerPhoto: review.reviewer.photos?.[0]?.photoUrl || null,
            rating: review.rating,
            comment: review.comment,
            createdAt: review.createdAt
        };

        res.status(201).json(formatted);
    } catch (error) {
        if (error.code === 'P2002') {
            return res.status(400).json({ error: "You have already reviewed this user" });
        }
        console.error("Create review error:", error);
        res.status(500).json({ error: "Error creating review" });
    }
};

const getReviews = async (req, res) => {
    try {
        const { userId } = req.params;
        const reviews = await prisma.review.findMany({
            where: { reviewedUserId: userId },
            include: {
                reviewer: {
                    select: {
                        id: true,
                        photos: { where: { isPrimary: true }, select: { photoUrl: true }, take: 1 },
                        profile: { select: { firstName: true, lastName: true } }
                    }
                }
            },
            orderBy: { createdAt: 'desc' }
        });

        const formatted = reviews.map(r => ({
            id: r.id,
            reviewerId: r.reviewer.id,
            reviewerName: r.reviewer.profile ? `${r.reviewer.profile.firstName} ${r.reviewer.profile.lastName || ''}`.trim() : "User",
            reviewerPhoto: r.reviewer.photos?.[0]?.photoUrl || null,
            rating: r.rating,
            comment: r.comment,
            createdAt: r.createdAt
        }));

        res.json(formatted);
    } catch (error) {
        console.error("Get reviews error:", error);
        res.status(500).json({ error: "Error fetching reviews" });
    }
};

const deleteReview = async (req, res) => {
    try {
        const { id } = req.params;
        const userId = req.user.id;

        const review = await prisma.review.findUnique({ where: { id } });
        if (!review) return res.status(404).json({ error: "Review not found" });

        // Allow deletion if you are the reviewer OR the admin (add admin check later if needed)
        if (review.reviewerId !== userId) {
            return res.status(403).json({ error: "Not authorized to delete this review" });
        }

        await prisma.review.delete({ where: { id } });
        res.json({ message: "Review deleted successfully" });
    } catch (error) {
        console.error("Delete review error:", error);
        res.status(500).json({ error: "Error deleting review" });
    }
}

module.exports = { createReview, getReviews, deleteReview };
