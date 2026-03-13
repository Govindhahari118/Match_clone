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
    fatherOccupation: null,
    motherOccupation: null,
    siblingsCount: null,
    familyType: 'not_specified',
    personalityQuiz: null,
};

const FAMILY_TYPE_VALUES = ['nuclear', 'joint', 'other', 'not_specified'];

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

function normalizeOccupation(value) {
    const text = String(value || '').trim();
    return text ? text : null;
}

function normalizeSiblingsCount(value) {
    if (value === null || value === undefined || value === '') return null;
    const parsed = Number.parseInt(value, 10);
    if (Number.isNaN(parsed)) return null;
    if (parsed < 0) return 0;
    if (parsed > 20) return 20;
    return parsed;
}

function normalizeFamilyType(value) {
    const parsed = normalize(value);
    if (!parsed || parsed === 'any') return null;
    if (parsed === 'joint_family') return 'joint';
    if (parsed === 'nuclear_family') return 'nuclear';
    return FAMILY_TYPE_VALUES.includes(parsed) ? parsed : 'other';
}

function normalizePersonalityQuiz(value) {
    if (!value || typeof value !== 'object') return null;
    const source = value.answers && typeof value.answers === 'object' ? value.answers : value;
    const entries = Object.entries(source)
        .map(([key, val]) => [String(key).trim(), String(val || '').trim()])
        .filter(([key, val]) => key && val);
    if (entries.length === 0) return null;
    const limited = entries.slice(0, 10);
    const answers = Object.fromEntries(limited);
    return {
        answers,
        updatedAt: value.updatedAt || new Date().toISOString(),
    };
}

function normalizeProfileExtension(input) {
    const merged = { ...DEFAULT_PROFILE_EXTENSION, ...(input || {}) };
    const normalizedQuiz = normalizePersonalityQuiz(merged.personalityQuiz);
    const normalized = {
        hasChildren: normalizeHasChildren(merged.hasChildren) || DEFAULT_PROFILE_EXTENSION.hasChildren,
        residentialStatus: normalizeResidentialStatus(merged.residentialStatus) || DEFAULT_PROFILE_EXTENSION.residentialStatus,
        district: normalizeDistrict(merged.district),
        fatherOccupation: normalizeOccupation(merged.fatherOccupation),
        motherOccupation: normalizeOccupation(merged.motherOccupation),
        siblingsCount: normalizeSiblingsCount(merged.siblingsCount),
        familyType: normalizeFamilyType(merged.familyType) || DEFAULT_PROFILE_EXTENSION.familyType,
        personalityQuiz: normalizedQuiz,
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
    FAMILY_TYPE_VALUES,
    normalizeHasChildren,
    normalizeResidentialStatus,
    normalizeDistrict,
    normalizeOccupation,
    normalizeSiblingsCount,
    normalizeFamilyType,
    normalizePersonalityQuiz,
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
