function getAuthUserId(req) {
    if (!req || !req.user) {
        return null;
    }
    return req.user.sub || req.user.id || req.user.userId || null;
}

module.exports = { getAuthUserId };
