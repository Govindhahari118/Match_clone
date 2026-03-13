const matchingService = require('../services/matching.service');

const ALLOWED_FILTER_KEYS = new Set([
    'gender',
    'minAge',
    'maxAge',
    'religion',
    'caste',
    'subCaste',
    'maritalStatus',
    'city',
    'state',
    'country',
    'district',
    'education',
    'profession',
    'income',
    'motherTongue',
    'diet',
    'gothra',
    'nakshatra',
    'rashi',
    'dosha',
    'minHeight',
    'maxHeight',
    'minMatch',
    'verifiedOnly',
    'withPhotoOnly',
    'photoVisibility',
    'profileVisibility',
    'verificationLevel',
    'onlineNow',
    'premiumOnly',
    'withHoroscopeOnly',
    'lastActiveDays',
    'hasChildren',
    'residentialStatus',
    'shortlistedOnly',
    'diversityCap',
    'sessionPreferredProfession',
    'sessionPreferredCity',
    'sessionSearchEdits',
    'sessionProfileOpens',
    'keyword',
    'query',
    'sort',
    'page',
    'limit',
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
            continue;
        }
        filters[key] = value;
    }

    const minAge = parseInteger(filters.minAge);
    const maxAge = parseInteger(filters.maxAge);
    const minHeight = parseInteger(filters.minHeight);
    const maxHeight = parseInteger(filters.maxHeight);
    const minMatch = parseInteger(filters.minMatch);

    if (filters.minAge !== undefined && minAge === null) return { error: 'minAge must be a valid number' };
    if (filters.maxAge !== undefined && maxAge === null) return { error: 'maxAge must be a valid number' };
    if (filters.minHeight !== undefined && minHeight === null) return { error: 'minHeight must be a valid number' };
    if (filters.maxHeight !== undefined && maxHeight === null) return { error: 'maxHeight must be a valid number' };
    if (filters.minMatch !== undefined && minMatch === null) return { error: 'minMatch must be a valid number' };

    if (minAge !== null && (minAge < 18 || minAge > 80)) return { error: 'minAge must be between 18 and 80' };
    if (maxAge !== null && (maxAge < 18 || maxAge > 80)) return { error: 'maxAge must be between 18 and 80' };
    if (minAge !== null && maxAge !== null && minAge > maxAge) return { error: 'minAge cannot be greater than maxAge' };

    if (minHeight !== null && (minHeight < 120 || minHeight > 250)) return { error: 'minHeight must be between 120 and 250 cm' };
    if (maxHeight !== null && (maxHeight < 120 || maxHeight > 250)) return { error: 'maxHeight must be between 120 and 250 cm' };
    if (minHeight !== null && maxHeight !== null && minHeight > maxHeight) return { error: 'minHeight cannot be greater than maxHeight' };
    if (minMatch !== null && (minMatch < 0 || minMatch > 100)) return { error: 'minMatch must be between 0 and 100' };

    if (filters.verifiedOnly !== undefined) {
        const parsed = parseBoolean(filters.verifiedOnly);
        if (parsed === null) return { error: 'verifiedOnly must be true or false' };
        filters.verifiedOnly = parsed;
    }

    if (filters.withPhotoOnly !== undefined) {
        const parsed = parseBoolean(filters.withPhotoOnly);
        if (parsed === null) return { error: 'withPhotoOnly must be true or false' };
        filters.withPhotoOnly = parsed;
    }

    if (filters.onlineNow !== undefined) {
        const parsed = parseBoolean(filters.onlineNow);
        if (parsed === null) return { error: 'onlineNow must be true or false' };
        filters.onlineNow = parsed;
    }

    if (filters.premiumOnly !== undefined) {
        const parsed = parseBoolean(filters.premiumOnly);
        if (parsed === null) return { error: 'premiumOnly must be true or false' };
        filters.premiumOnly = parsed;
    }

    if (filters.withHoroscopeOnly !== undefined) {
        const parsed = parseBoolean(filters.withHoroscopeOnly);
        if (parsed === null) return { error: 'withHoroscopeOnly must be true or false' };
        filters.withHoroscopeOnly = parsed;
    }

    if (filters.shortlistedOnly !== undefined) {
        const parsed = parseBoolean(filters.shortlistedOnly);
        if (parsed === null) return { error: 'shortlistedOnly must be true or false' };
        filters.shortlistedOnly = parsed;
    }

    if (minAge !== null) filters.minAge = minAge;
    if (maxAge !== null) filters.maxAge = maxAge;
    if (minHeight !== null) filters.minHeight = minHeight;
    if (maxHeight !== null) filters.maxHeight = maxHeight;
    if (minMatch !== null) filters.minMatch = minMatch;

    return { filters };
}

const getMatches = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { filters, error } = sanitizeFilters(req.query);
        if (error) {
            return res.status(400).json({ error });
        }

        const matches = await matchingService.getMatches(userId, filters);
        res.status(200).json(matches);
    } catch (error) {
        console.error('Get matches error:', error);
        res.status(500).json({ error: error.message });
    }
};

const submitMatchFeedback = async (req, res) => {
    try {
        const userId = req.user.sub;
        const result = await matchingService.submitFeedback(userId, req.body, {
            ipAddress: req.ip,
            userAgent: req.get('user-agent'),
        });

        if (result.error) {
            return res.status(result.statusCode || 400).json({ error: result.error });
        }

        return res.status(200).json(result);
    } catch (error) {
        console.error('Submit match feedback error:', error);
        return res.status(500).json({ error: error.message });
    }
};

module.exports = { getMatches, submitMatchFeedback };
