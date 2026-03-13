# API Route Source of Truth

All API routes are defined once in `backend/src/routes/routes.registry.js`.

Use this registry as the canonical source for route paths and mounting order.
When adding or changing endpoints, update the registry first, then adjust
controllers and frontend calls to match.
