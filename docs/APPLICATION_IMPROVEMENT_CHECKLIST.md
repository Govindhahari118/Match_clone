# Application Improvement Checklist

Updated: 2026-03-08

## Immediate Stability and Security

- [x] Fix frontend lint blocker in success stories page (`react/no-unescaped-entities`).
- [x] Add shared auth/session storage helper for consistent token handling.
- [x] Wire Axios and `AuthContext` to shared auth/session storage helper.
- [x] Harden backend with security headers via `helmet`.
- [x] Replace wildcard CORS with environment-driven allowlist (`CORS_ORIGINS` / `FRONTEND_URL`).
- [x] Add backend request body size guard (`BODY_LIMIT`).
- [x] Add backend 404 handler and centralized error middleware.
- [x] Add graceful shutdown for API and Prisma on `SIGINT` / `SIGTERM`.

## Reliability and Observability

- [x] Add backend integration smoke tests for `/health`, auth login, and matches listing.
- [x] Add request logging middleware with correlation IDs.
- [x] Add frontend error boundary coverage for route-level failures.
- [x] Add uptime/dashboard docs for runtime logs and recovery steps.

## Product and UX

- [x] Add loading and empty states consistency audit for all main routes.
- [x] Add form validation standardization with shared schema helpers.
- [x] Add accessibility pass (keyboard focus, contrast, aria labels) for critical screens.

## Data and Performance

- [x] Review slow Prisma queries and add indexes for high-traffic search filters.
- [x] Add pagination defaults and hard caps for all list APIs.
- [x] Add API response caching strategy for metadata endpoints.
