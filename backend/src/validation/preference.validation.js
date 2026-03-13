const { z } = require('zod');

function emptyToUndefined(value) {
    if (value === null || value === undefined) return undefined;
    if (typeof value === 'string' && value.trim() === '') return undefined;
    return value;
}

function trimmedString(max = 120) {
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
        z.number().int().min(min).max(max)
    ).optional();
}

const locationsSchema = z.preprocess(
    (value) => {
        if (value === null || value === undefined || value === '') return undefined;
        if (Array.isArray(value)) return value;
        if (typeof value === 'string') return value;
        return value;
    },
    z.union([z.array(z.string().min(1).max(80)).max(25), z.string().max(400)])
).optional();

const preferenceSchema = z.object({
    min_age: numericField({ min: 18, max: 80 }),
    max_age: numericField({ min: 18, max: 80 }),
    minAge: numericField({ min: 18, max: 80 }),
    maxAge: numericField({ min: 18, max: 80 }),
    marital_status: trimmedString(40),
    religion: trimmedString(60),
    preferred_locations: locationsSchema,
    min_income_band: trimmedString(40),
    income: trimmedString(40),
    city: trimmedString(80),
}).passthrough();

function formatZodError(error) {
    return error.errors.map((err) => ({
        path: err.path.join('.'),
        message: err.message,
    }));
}

function validatePreferenceUpdate(payload) {
    const parsed = preferenceSchema.safeParse(payload || {});
    if (!parsed.success) {
        return { success: false, errors: formatZodError(parsed.error) };
    }
    return { success: true, data: parsed.data };
}

module.exports = { validatePreferenceUpdate };
