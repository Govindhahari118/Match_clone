const { z } = require('zod');

function emptyToUndefined(value) {
    if (value === null || value === undefined) return undefined;
    if (typeof value === 'string' && value.trim() === '') return undefined;
    return value;
}

function trimmedString(max = 200) {
    return z.preprocess(
        (value) => {
            if (value === null || value === undefined) return undefined;
            if (typeof value !== 'string') return value;
            const trimmed = value.trim();
            return trimmed === '' ? undefined : trimmed;
        },
        z.string().max(max)
    ).optional();
}

function numericField({ min, max }) {
    return z.preprocess(
        (value) => {
            if (value === null || value === undefined || value === '') return undefined;
            const parsed = Number(value);
            return Number.isFinite(parsed) ? parsed : value;
        },
        z.number().min(min).max(max)
    ).optional();
}

const hobbiesSchema = z.preprocess(
    (value) => {
        if (value === null || value === undefined || value === '') return undefined;
        if (Array.isArray(value)) return value;
        if (typeof value === 'string') {
            return value
                .split(',')
                .map((item) => item.trim())
                .filter(Boolean);
        }
        return value;
    },
    z.array(z.string().min(1).max(40)).max(20)
).optional();

const dateStringSchema = z.preprocess(
    emptyToUndefined,
    z.string().refine((value) => !Number.isNaN(Date.parse(value)), {
        message: 'Invalid date value',
    })
).optional();

const profileUpdateSchema = z.object({
    firstName: trimmedString(80),
    lastName: trimmedString(80),
    dateOfBirth: dateStringSchema,
    gender: trimmedString(20),
    bio: trimmedString(2000),
    city: trimmedString(80),
    state: trimmedString(80),
    country: trimmedString(80),
    religion: trimmedString(80),
    caste: trimmedString(80),
    subCaste: trimmedString(80),
    motherTongue: trimmedString(80),
    maritalStatus: trimmedString(40),
    height: numericField({ min: 80, max: 250 }),
    heightCm: numericField({ min: 80, max: 250 }),
    educationLevel: trimmedString(50),
    educationField: trimmedString(80),
    profession: trimmedString(80),
    company: trimmedString(120),
    incomeBand: trimmedString(40),
    foodHabit: trimmedString(40),
    drinks: trimmedString(40),
    smokes: trimmedString(40),
    hobbies: hobbiesSchema,
    religiousness: trimmedString(40),
    role: trimmedString(40),
    intent: trimmedString(40),
    videoUrl: trimmedString(300),
    videoThumbnail: trimmedString(300),
    birthTime: trimmedString(20),
    birthPlace: trimmedString(80),
    gothra: trimmedString(80),
    zodiacSign: trimmedString(80),
    nakshatra: trimmedString(80),
    dosha: trimmedString(40),
    hasChildren: trimmedString(40),
    residentialStatus: trimmedString(40),
    district: trimmedString(80),
    fatherOccupation: trimmedString(120),
    motherOccupation: trimmedString(120),
    siblingsCount: numericField({ min: 0, max: 20 }),
    familyType: trimmedString(40),
    personalityQuiz: z.preprocess(
        emptyToUndefined,
        z
            .object({
                answers: z.record(z.string()).optional(),
                updatedAt: z.string().optional(),
            })
            .passthrough()
    ).optional(),
}).passthrough();

function formatZodError(error) {
    return error.errors.map((err) => ({
        path: err.path.join('.'),
        message: err.message,
    }));
}

function validateProfileUpdate(payload) {
    const parsed = profileUpdateSchema.safeParse(payload || {});
    if (!parsed.success) {
        return {
            success: false,
            errors: formatZodError(parsed.error),
        };
    }
    return { success: true, data: parsed.data };
}

module.exports = { validateProfileUpdate };
