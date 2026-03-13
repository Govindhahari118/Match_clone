const authRoutes = require('./auth.routes');
const userRoutes = require('./user.routes');
const postRoutes = require('./post.routes');
const metaRoutes = require('./meta.routes');
const searchRoutes = require('./search.routes');
const subscriptionRoutes = require('./subscription.routes');
const callRoutes = require('./call.routes');
const profileRoutes = require('./profile.routes');
const photoRoutes = require('./photo.routes');
const matchesRoutes = require('./matches.routes');
const interactionRoutes = require('./interaction.routes');
const chatRoutes = require('./chat.routes');
const paymentRoutes = require('./payment.routes');
const adminRoutes = require('./admin.routes');
const mediaRoutes = require('./media.routes');
const shortlistRoutes = require('./shortlist.routes');
const reviewRoutes = require('./review.routes');
const verificationRoutes = require('./verification.routes');
const analyticsRoutes = require('./analytics.routes');
const notificationRoutes = require('./notification.routes');

function buildRouteRegistry({ searchLimiter, profileLimiter, chatLimiter, callLimiter }) {
    return [
        { base: '/api/auth', router: authRoutes },
        { base: '/api/meta', router: metaRoutes },
        { base: '/api/search', router: searchRoutes, middleware: [searchLimiter] },
        { base: '/api/subscription', router: subscriptionRoutes },
        { base: '/api/call', router: callRoutes, middleware: [callLimiter] },
        { base: '/api/users', router: userRoutes },
        { base: '/api/profiles', router: profileRoutes, middleware: [profileLimiter] },
        { base: '/api/photos', router: photoRoutes },
        { base: '/api/matches', router: matchesRoutes },
        { base: '/api/interactions', router: interactionRoutes },
        { base: '/api/chat', router: chatRoutes, middleware: [chatLimiter] },
        { base: '/api/payment', router: paymentRoutes },
        { base: '/api/admin', router: adminRoutes },
        { base: '/api/posts', router: postRoutes },
        { base: '/api/media', router: mediaRoutes },
        { base: '/api/shortlist', router: shortlistRoutes },
        { base: '/api/reviews', router: reviewRoutes },
        { base: '/api/verification', router: verificationRoutes },
        { base: '/api/analytics', router: analyticsRoutes },
        { base: '/api/notifications', router: notificationRoutes },
    ];
}

module.exports = { buildRouteRegistry };
