const crypto = require('crypto');

function sanitizeCorrelationId(value) {
    const candidate = String(value || '').trim();
    if (!candidate) return null;
    return candidate.slice(0, 80);
}

function requestContextMiddleware(req, res, next) {
    const incoming = sanitizeCorrelationId(req.get('x-correlation-id'));
    const correlationId = incoming || crypto.randomUUID();
    req.correlationId = correlationId;
    req.requestStartedAt = Date.now();

    res.setHeader('x-correlation-id', correlationId);
    next();
}

function requestLoggingMiddleware(req, res, next) {
    const start = Date.now();
    res.on('finish', () => {
        const durationMs = Date.now() - start;
        const payload = {
            ts: new Date().toISOString(),
            correlationId: req.correlationId || null,
            method: req.method,
            path: req.originalUrl,
            status: res.statusCode,
            durationMs,
            userId: req.user?.sub || null,
            ip: req.ip || null,
        };
        console.log(JSON.stringify({ type: 'http_request', ...payload }));
    });
    next();
}

module.exports = {
    requestContextMiddleware,
    requestLoggingMiddleware,
};
