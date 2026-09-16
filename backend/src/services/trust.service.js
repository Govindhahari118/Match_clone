const prisma = require('../config/prisma');

const SEARCH_STATUSES = new Set([
    'active',
    'low_activity',
    'paused',
    'found_match',
    'married',
    'closed',
]);

const DISCOVERABLE_SEARCH_STATUSES = new Set(['active', 'low_activity']);

function hasValue(value) {
    if (Array.isArray(value)) return value.length > 0;
    return value !== undefined && value !== null && String(value).trim() !== '';
}

function calculateAge(dateOfBirth) {
    if (!dateOfBirth) return null;
    const now = new Date();
    const dob = new Date(dateOfBirth);
    if (Number.isNaN(dob.getTime())) return null;
    let age = now.getFullYear() - dob.getFullYear();
    const delta = now.getMonth() - dob.getMonth();
    if (delta < 0 || (delta === 0 && now.getDate() < dob.getDate())) age -= 1;
    return age;
}

function latestActivity(user) {
    const candidates = [user?.lastActiveAt, user?.lastLogin, user?.updatedAt]
        .filter(Boolean)
        .map((value) => new Date(value))
        .filter((value) => !Number.isNaN(value.getTime()));
    if (candidates.length === 0) return null;
    return new Date(Math.max(...candidates.map((value) => value.getTime())));
}

function activityBucket(user, now = new Date()) {
    const last = latestActivity(user);
    if (!last) return { code: 'unknown', label: 'Activity unavailable', days: null };

    const days = Math.max(0, Math.floor((now.getTime() - last.getTime()) / 86400000));
    if (days <= 1) return { code: 'active_today', label: 'Active today', days };
    if (days <= 7) return { code: 'active_this_week', label: 'Active this week', days };
    if (days <= 30) return { code: 'active_recently', label: 'Active recently', days };
    if (days <= 60) return { code: 'low_activity', label: 'Low recent activity', days };
    if (days <= 90) return { code: 'reconfirmation_due', label: 'Activity needs reconfirmation', days };
    return { code: 'stale', label: 'Inactive for over 3 months', days };
}

function isStale(user, now = new Date()) {
    const bucket = activityBucket(user, now);
    return bucket.code === 'stale';
}

function verificationSummary(user) {
    const rows = user?.verifications || [];
    const byType = new Map(rows.map((row) => [row.type, row]));
    const photoVerified = Boolean(
        user?.photos?.some((photo) => photo.verificationStatus === 'verified') ||
        byType.get('photo')?.status === 'verified' ||
        byType.get('selfie')?.status === 'verified'
    );

    const statusFor = (type, fallback = 'not_started') => byType.get(type)?.status || fallback;

    return {
        phone: statusFor('phone', user?.phone && user?.isVerified ? 'verified' : 'not_started'),
        email: statusFor('email', user?.email && user?.isVerified ? 'verified' : 'not_started'),
        photo: photoVerified ? 'verified' : statusFor('photo'),
        selfie: statusFor('selfie'),
        identity: user?.identityStatus || statusFor('id', 'unverified'),
        employment: statusFor('employment'),
        education: statusFor('education'),
        updatedAt: rows
            .map((row) => row.updatedAt || row.verifiedAt)
            .filter(Boolean)
            .sort((a, b) => new Date(b) - new Date(a))[0] || null,
    };
}

function calculateCompleteness({ profile, photos = [], preferences = null, verification = null }) {
    if (!profile) return 0;

    const groups = [
        {
            weight: 20,
            fields: [profile.firstName, profile.dateOfBirth, profile.gender, profile.maritalStatus, profile.heightCm],
        },
        { weight: 20, fields: [photos.length > 0 ? 'yes' : null] },
        { weight: 10, fields: [profile.educationLevel, profile.educationField] },
        { weight: 10, fields: [profile.profession, profile.incomeBand] },
        { weight: 10, fields: [profile.city, profile.state, profile.country] },
        { weight: 10, fields: [profile.bio] },
        {
            weight: 10,
            fields: preferences
                ? [preferences.minAge, preferences.maxAge, preferences.preferredLocations?.length ? 'yes' : null]
                : [],
        },
        {
            weight: 10,
            fields: verification
                ? [
                    verification.phone === 'verified' ? 'yes' : null,
                    verification.photo === 'verified' ? 'yes' : null,
                    verification.identity === 'verified' ? 'yes' : null,
                ]
                : [],
        },
    ];

    const score = groups.reduce((total, group) => {
        if (group.fields.length === 0) return total;
        const completed = group.fields.filter(hasValue).length;
        return total + (group.weight * completed) / group.fields.length;
    }, 0);

    return Math.max(0, Math.min(100, Math.round(score)));
}

function reconfirmationLabel(date) {
    if (!date) return 'Not reconfirmed yet';
    const days = Math.max(0, Math.floor((Date.now() - new Date(date).getTime()) / 86400000));
    if (days === 0) return 'Reconfirmed today';
    if (days === 1) return 'Reconfirmed yesterday';
    return `Reconfirmed ${days} days ago`;
}

async function getTrustSummary(userId) {
    const user = await prisma.user.findUnique({
        where: { id: userId },
        include: {
            profile: true,
            partnerPreference: true,
            photos: true,
            verifications: true,
        },
    });
    if (!user) return null;

    const verification = verificationSummary(user);
    const completeness = calculateCompleteness({
        profile: user.profile,
        photos: user.photos,
        preferences: user.partnerPreference,
        verification,
    });
    const activity = activityBucket(user);

    return {
        verification,
        activity,
        lastActiveAt: latestActivity(user),
        profileCompleteness: completeness,
        managedBy: user.profile?.role || 'self',
        managerRelationship: user.profile?.managerRelationship || null,
        searchStatus: user.searchStatus,
        statusReconfirmedAt: user.statusReconfirmedAt,
        statusReconfirmationLabel: reconfirmationLabel(user.statusReconfirmedAt),
        profileCreatedAt: user.profile?.createdAt || user.createdAt,
        profileVersion: user.profileVersion,
        age: calculateAge(user.profile?.dateOfBirth),
        discoverable: Boolean(
            user.isActive &&
            !user.isBanned &&
            !user.deletedAt &&
            DISCOVERABLE_SEARCH_STATUSES.has(user.searchStatus) &&
            !isStale(user)
        ),
    };
}

async function recordActivity(userId) {
    const now = new Date();
    await prisma.user.updateMany({
        where: { id: userId, deletedAt: null },
        data: { lastActiveAt: now },
    });
    return now;
}

async function reconfirmSearchStatus(userId, requestedStatus) {
    const status = String(requestedStatus || '').trim().toLowerCase();
    if (!SEARCH_STATUSES.has(status)) {
        const error = new Error('Invalid search status');
        error.statusCode = 400;
        throw error;
    }

    const now = new Date();
    const user = await prisma.user.update({
        where: { id: userId },
        data: {
            searchStatus: status,
            statusReconfirmedAt: now,
            lastActiveAt: now,
        },
        select: {
            searchStatus: true,
            statusReconfirmedAt: true,
        },
    });

    await prisma.auditLog.create({
        data: {
            userId,
            action: 'matrimony_search_status_reconfirmed',
            resourceType: 'user',
            resourceId: userId,
            changes: { searchStatus: status },
        },
    });

    return {
        ...user,
        statusReconfirmationLabel: reconfirmationLabel(user.statusReconfirmedAt),
    };
}

module.exports = {
    SEARCH_STATUSES,
    DISCOVERABLE_SEARCH_STATUSES,
    activityBucket,
    latestActivity,
    isStale,
    verificationSummary,
    calculateCompleteness,
    reconfirmationLabel,
    getTrustSummary,
    recordActivity,
    reconfirmSearchStatus,
};
