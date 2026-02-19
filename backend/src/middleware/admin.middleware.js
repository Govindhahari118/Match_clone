const jwt = require('jsonwebtoken');
const prisma = require('../config/prisma');

const adminMiddleware = async (req, res, next) => {
    try {
        const token = req.headers.authorization?.split(' ')[1];
        if (!token) return res.status(401).json({ error: 'Access denied. No token provided.' });

        const decoded = jwt.verify(token, process.env.JWT_SECRET);

        // check if user is admin in DB
        const user = await prisma.user.findUnique({
            where: { id: decoded.sub },
            select: { role: true }
        });

        if (!user || user.role !== 'admin') {
            return res.status(403).json({ error: 'Access denied. High table only.' });
        }

        req.user = decoded;
        next();
    } catch (error) {
        res.status(400).json({ error: 'Invalid token.' });
    }
};

module.exports = adminMiddleware;
