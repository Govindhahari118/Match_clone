const prisma = require('../config/prisma');

const DEFAULT_PRIVACY_SETTINGS = {
    showPhone: false,
    showPhoto: true,
    showProfile: true,
    allowSearch: true,
    showLastSeen: false,
    allowMessages: true,
    photoVisibility: 'public', // public | protected | request_access
    profileVisibility: 'public', // public | premium_only | verified_only | hidden
};

function normalizePrivacySettings(input) {
    const settings = { ...DEFAULT_PRIVACY_SETTINGS, ...(input || {}) };

    const normalizedPhotoVisibility = String(settings.photoVisibility || 'public').toLowerCase();
    if (!['public', 'protected', 'request_access'].includes(normalizedPhotoVisibility)) {
        settings.photoVisibility = 'public';
    } else {
        settings.photoVisibility = normalizedPhotoVisibility;
    }

    const normalizedProfileVisibility = String(settings.profileVisibility || 'public').toLowerCase();
    if (!['public', 'premium_only', 'verified_only', 'hidden'].includes(normalizedProfileVisibility)) {
        settings.profileVisibility = 'public';
    } else {
        settings.profileVisibility = normalizedProfileVisibility;
    }

    settings.showPhone = Boolean(settings.showPhone);
    settings.showPhoto = Boolean(settings.showPhoto);
    settings.showProfile = Boolean(settings.showProfile);
    settings.allowSearch = Boolean(settings.allowSearch);
    settings.showLastSeen = Boolean(settings.showLastSeen);
    settings.allowMessages = Boolean(settings.allowMessages);

    return settings;
}

async function fetchLatestPrivacyRows(userIds) {
    if (!Array.isArray(userIds) || userIds.length === 0) {
        return [];
    }

    return prisma.auditLog.findMany({
        where: {
            userId: { in: userIds },
            action: 'privacy_settings_updated',
        },
        orderBy: { createdAt: 'desc' },
        distinct: ['userId'],
        select: {
            userId: true,
            changes: true,
            createdAt: true,
        },
    });
}

const privacyService = {
    DEFAULT_PRIVACY_SETTINGS,
    normalizePrivacySettings,

    async getUserPrivacySettings(userId) {
        const rows = await fetchLatestPrivacyRows([userId]);
        const row = rows[0];
        return {
            settings: normalizePrivacySettings(row?.changes || {}),
            updatedAt: row?.createdAt || null,
        };
    },

    async getUsersPrivacySettings(userIds) {
        const rows = await fetchLatestPrivacyRows(userIds);
        const map = new Map();
        for (const row of rows) {
            map.set(row.userId, normalizePrivacySettings(row.changes || {}));
        }
        return map;
    },

    canViewProfile({ settings, isOwner, viewerIsPremium, viewerIsVerified }) {
        if (isOwner) return true;
        if (!settings.showProfile) return false;
        if (!settings.allowSearch) return false;

        if (settings.profileVisibility === 'hidden') return false;
        if (settings.profileVisibility === 'premium_only' && !viewerIsPremium) return false;
        if (settings.profileVisibility === 'verified_only' && !viewerIsVerified) return false;

        return true;
    },

    canViewPhotos({ settings, isOwner, isMutualMatch, viewerIsPremium }) {
        if (isOwner) return true;
        if (!settings.showPhoto) return false;

        if (settings.photoVisibility === 'public') return true;
        if (settings.photoVisibility === 'protected') return Boolean(isMutualMatch);
        if (settings.photoVisibility === 'request_access') return Boolean(isMutualMatch || viewerIsPremium);

        return true;
    },
};

module.exports = privacyService;
