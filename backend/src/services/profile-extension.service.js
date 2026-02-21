const prisma = require('../config/prisma');

const HAS_CHILDREN_VALUES = ['no', 'yes_living_with_me', 'yes_not_living_with_me', 'unknown'];
const RESIDENTIAL_STATUS_VALUES = [
    'citizen',
    'permanent_resident',
    'work_permit',
    'student_visa',
    'other',
    'not_specified',
];

const DEFAULT_PROFILE_EXTENSION = {
    hasChildren: 'unknown',
    residentialStatus: 'not_specified',
    district: null,
};

function normalize(value) {
    return String(value || '')
        .trim()
        .toLowerCase()
        .replace(/[\s-]+/g, '_');
}

function normalizeHasChildren(value) {
    const parsed = normalize(value);
    if (!parsed || parsed === 'any') return null;

    if (['no', 'none'].includes(parsed)) return 'no';
    if (['yes', 'yes_living_with_me', 'living_with_me', 'with_me'].includes(parsed)) return 'yes_living_with_me';
    if (['yes_not_living_with_me', 'not_living_with_me', 'without_me'].includes(parsed)) return 'yes_not_living_with_me';
    if (['unknown', 'dont_know', 'prefer_not_to_say'].includes(parsed)) return 'unknown';
    return HAS_CHILDREN_VALUES.includes(parsed) ? parsed : null;
}

function normalizeResidentialStatus(value) {
    const parsed = normalize(value);
    if (!parsed || parsed === 'any') return null;
    if (['pr', 'permanent_residency', 'permanent'].includes(parsed)) return 'permanent_resident';
    if (['workvisa', 'work_visa'].includes(parsed)) return 'work_permit';
    if (['student', 'student_permit'].includes(parsed)) return 'student_visa';
    if (['citizen', 'citizenship'].includes(parsed)) return 'citizen';
    return RESIDENTIAL_STATUS_VALUES.includes(parsed) ? parsed : 'other';
}

function normalizeDistrict(value) {
    const text = String(value || '').trim();
    return text ? text : null;
}

function normalizeProfileExtension(input) {
    const merged = { ...DEFAULT_PROFILE_EXTENSION, ...(input || {}) };
    const normalized = {
        hasChildren: normalizeHasChildren(merged.hasChildren) || DEFAULT_PROFILE_EXTENSION.hasChildren,
        residentialStatus: normalizeResidentialStatus(merged.residentialStatus) || DEFAULT_PROFILE_EXTENSION.residentialStatus,
        district: normalizeDistrict(merged.district),
    };
    return normalized;
}

async function fetchLatestRows(userIds) {
    if (!Array.isArray(userIds) || userIds.length === 0) {
        return [];
    }

    return prisma.auditLog.findMany({
        where: {
            userId: { in: userIds },
            action: 'profile_extension_updated',
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

const profileExtensionService = {
    DEFAULT_PROFILE_EXTENSION,
    HAS_CHILDREN_VALUES,
    RESIDENTIAL_STATUS_VALUES,
    normalizeHasChildren,
    normalizeResidentialStatus,
    normalizeDistrict,
    normalizeProfileExtension,

    async getUserProfileExtension(userId) {
        const rows = await fetchLatestRows([userId]);
        const row = rows[0];
        return {
            extension: normalizeProfileExtension(row?.changes || {}),
            updatedAt: row?.createdAt || null,
        };
    },

    async getUsersProfileExtensions(userIds) {
        const rows = await fetchLatestRows(userIds);
        const map = new Map();
        for (const row of rows) {
            map.set(row.userId, normalizeProfileExtension(row.changes || {}));
        }
        return map;
    },

    async saveUserProfileExtension(userId, extension = {}) {
        const normalized = normalizeProfileExtension(extension);
        await prisma.auditLog.create({
            data: {
                userId,
                action: 'profile_extension_updated',
                resourceType: 'profile',
                resourceId: userId,
                changes: normalized,
            },
        });
        return normalized;
    },
};

module.exports = profileExtensionService;
