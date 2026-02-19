const jwt = require('jsonwebtoken');

const generateTokens = (userId, role = 'user') => {
    const accessToken = jwt.sign(
        { sub: userId, role },
        process.env.JWT_SECRET,
        { expiresIn: '1h' }
    );

    const refreshToken = jwt.sign(
        { sub: userId, role },
        process.env.JWT_SECRET, // Ideally use a separate refresh secret
        { expiresIn: '30d' }
    );

    return { accessToken, refreshToken };
};

module.exports = { generateTokens };
