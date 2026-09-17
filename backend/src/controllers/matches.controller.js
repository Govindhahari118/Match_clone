const matchingService = require('../services/matching.service');

const ALLOWED_FILTER_KEYS = new Set([
    'gender', 'minAge', 'maxAge', 'religion', 'caste', 'maritalStatus',
    'city', 'state', 'country', 'district', 'education', 'profession', 'income',
    'motherTongue', 'minHeight', 'maxHeight', 'verifiedOnly', 'withPhotoOnly',
    'onlineNow', 'premiumOnly', 'withHoroscopeOnly', 'lastActiveDays',
    'hasChildren', 'residentialStatus', 'photoVisibility', 'profileVisibility',
    'verificationLevel', 'keyword', 'query', 'sort', 'page', 'limit',
]);

function parseInteger(value) {
    const number = Number.parseInt(value, 10);
    return Number.isNaN(number) ? null : number;
}

function parseBoolean(value) {
    if (typeof value === 'boolean') return value;
    if (value === undefined || value === null) return null;
    const lowered = String(value).trim().toLowerCase();
    if (['true', '1', 'yes'].includes(lowered)) return true;
    if (['false', '0', 'no'].includes(lowered)) return false;
    return null;
}

function sanitizeFilters(raw) {
    const filters = {};
    for (const [key, value] of Object.entries(raw || {})) {
        if (!ALLOWED_FILTER_KEYS.has(key)) continue;
        if (typeof value === 'string') {
            const trimmed = value.trim();
            if (trimmed !== '') filters[key] = trimmed;
        } else {
            filters[key] = value;
        }
    }

    const minAge = parseInteger(filters.minAge);
    const maxAge = parseInteger(filters.maxAge);
    const minHeight = parseInteger(filters.minHeight);
    const maxHeight = parseInteger(filters.maxHeight);
    const lastActiveDays = parseInteger(filters.lastActiveDays);

    if (filters.minAge !== undefined && minAge === null) return { error: 'minAge must be a valid number' };
    if (filters.maxAge !== undefined && maxAge === null) return { error: 'maxAge must be a valid number' };
    if (filters.minHeight !== undefined && minHeight === null) return { error: 'minHeight must be a valid number' };
    if (filters.maxHeight !== undefined && maxHeight === null) return { error: 'maxHeight must be a valid number' };
    if (filters.lastActiveDays !== undefined && lastActiveDays === null) return { error: 'lastActiveDays must be a valid number' };
    if (minAge !== null && (minAge < 18 || minAge > 80)) return { error: 'minAge must be between 18 and 80' };
    if (maxAge !== null && (maxAge < 18 || maxAge > 80)) return { error: 'maxAge must be between 18 and 80' };
    if (minAge !== null && maxAge !== null && minAge > maxAge) return { error: 'minAge cannot be greater than maxAge' };
    if (minHeight !== null && (minHeight < 120 || minHeight > 250)) return { error: 'minHeight must be between 120 and 250 cm' };
    if (maxHeight !== null && (maxHeight < 120 || maxHeight > 250)) return { error: 'maxHeight must be between 120 and 250 cm' };
    if (minHeight !== null && maxHeight !== null && minHeight > maxHeight) return { error: 'minHeight cannot be greater than maxHeight' };

    for (const key of ['verifiedOnly', 'withPhotoOnly', 'onlineNow', 'premiumOnly', 'withHoroscopeOnly']) {
        if (filters[key] !== undefined) {
            const parsed = parseBoolean(filters[key]);
            if (parsed === null) return { error: `${key} must be true or false` };
            filters[key] = parsed;
        }
    }

    if (minAge !== null) filters.minAge = minAge;
    if (maxAge !== null) filters.maxAge = maxAge;
    if (minHeight !== null) filters.minHeight = minHeight;
    if (maxHeight !== null) filters.maxHeight = maxHeight;
    if (lastActiveDays !== null) filters.lastActiveDays = lastActiveDays;
    return { filters };
}

const getMatches = async (req, res) => {
    try {
        const { filters, error } = sanitizeFilters(req.query);
        if (error) return res.status(400).json({ error });
        const matches = await matchingService.getMatches(req.user.sub, filters);
        return res.status(200).json(matches);
    } catch (error) {
        console.error('Get matches error:', error);
        const status = Number(error.statusCode) || 500;
        return res.status(status).json({ error: status >= 500 ? 'Failed to load matches' : error.message });
    }
};

module.exports = { getMatches };
