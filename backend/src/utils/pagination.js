function toPositiveInteger(value, fallback) {
    const parsed = Number.parseInt(value, 10);
    if (Number.isNaN(parsed) || parsed < 1) {
        return fallback;
    }
    return parsed;
}

function parsePagination(query = {}, options = {}) {
    const defaultPage = options.defaultPage || 1;
    const defaultLimit = options.defaultLimit || 20;
    const maxLimit = options.maxLimit || 50;

    const page = toPositiveInteger(query.page, defaultPage);
    const requestedLimit = toPositiveInteger(query.limit, defaultLimit);
    const limit = Math.min(requestedLimit, maxLimit);
    const skip = (page - 1) * limit;

    return { page, limit, skip };
}

module.exports = {
    parsePagination,
};
