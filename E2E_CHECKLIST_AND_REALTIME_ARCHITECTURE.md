# MONY: E2E Checklist and Real-Time Project Architecture

## 1. Current State Summary

- Backend stack: Express + Prisma + Socket.IO (`backend/src/index.js`)
- Web stack: Next.js App Router (`frontend/src/app`)
- DB: PostgreSQL (`backend/prisma/schema.prisma`)
- Infra present: PostgreSQL + Redis + API in Docker Compose (`docker-compose.yml`)
- Mobile app: not implemented yet (only bootstrap notes) (`mobile/README.md`)

## 2. Critical Integration Gaps Found During E2E Analysis

| Gap | Evidence | Impact | Priority |
|---|---|---|---|
| API base URL points to wrong port | `frontend/src/services/api.js:4`, `backend/src/index.js:23` | Web cannot reach backend | P0 |
| Token key naming is inconsistent | `frontend/src/services/api.js:14`, `frontend/src/context/AuthContext.js:18`, `frontend/src/app/(auth)/login/page.js:43` | Auth state breaks across pages | P0 |
| Socket URL points to wrong port | `frontend/src/context/AuthContext.js:36`, `backend/src/index.js:23` | Real-time events do not connect | P0 |
| Shortlist route imports non-existent middleware path | `backend/src/routes/shortlist.routes.js:4` | Shortlist endpoints crash on boot | P0 |
| Review route expects `authenticate` export that does not exist | `backend/src/routes/review.routes.js:3`, `backend/src/middleware/auth.middleware.js:3` | Review endpoints fail | P0 |
| Multiple controllers use wrong JWT payload field | `backend/src/controllers/review.controller.js:7`, `backend/src/controllers/shortlist.controller.js:6`, `backend/src/controllers/verification.controller.js:6` | User identity resolves incorrectly | P0 |
| User controller uses schema-mismatched fields/relations | `backend/src/controllers/user.controller.js:9`, `backend/src/controllers/user.controller.js:17`, `backend/prisma/schema.prisma:36`, `backend/prisma/schema.prisma:84` | Profile APIs are unstable | P0 |
| Frontend calls several endpoints that backend does not expose | `frontend/src/app/(onboarding)/step-1/page.js:23`, `frontend/src/app/(onboarding)/step-5/page.js:18`, `backend/src/routes/user.routes.js:10` | Onboarding/profile saves fail | P0 |
| Docs/ports are inconsistent | `README.md:40`, `frontend/package.json:6` | Setup confusion and failed local runs | P1 |
| Prisma client not generated in current environment | Backend start failed with `.prisma/client/default` missing | Backend cannot start | P0 |

## 3. End-to-End Delivery Checklist

Use this as the execution checklist. Do not start feature expansion before all P0 items are complete.

### A. Foundation and Environment

- [ ] Standardize ports and URLs:
- [ ] Backend HTTP + Socket on one port (recommended `4000`)
- [ ] Frontend on one port (recommended `3000` or `8000`, pick one)
- [ ] Use `.env` values (`NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_SOCKET_URL`) instead of hardcoded URLs
- [ ] Run Prisma generation/migrations successfully:
- [ ] `npx prisma generate`
- [ ] `npx prisma migrate dev` (or `deploy` in non-dev)
- [ ] Confirm health endpoint works: `GET /health`
- [ ] Remove duplicate middleware calls (`app.use(cors())` appears twice in `backend/src/index.js`)

### B. API Contract Alignment

- [ ] Publish a single source of truth for API routes (`/api/...`)
- [ ] Align frontend endpoint calls with backend routes:
- [ ] Profile update endpoints
- [ ] Preference endpoints
- [ ] Verification endpoints
- [ ] Admin endpoints
- [ ] Remove dead/placeholder route calls from frontend (`/dashboard`, `/signup`, `/upgrade`, `/user/privacy` if unsupported)
- [ ] Version API contracts in docs and Postman from live code, not aspirational docs

### C. Authentication and Session

- [ ] Normalize token keys in client storage:
- [ ] Access token key: one name only
- [ ] Refresh token key: one name only
- [ ] Ensure Axios interceptor reads same key written by login flow
- [ ] Ensure auth middleware payload usage is consistent (`req.user.sub`)
- [ ] Add refresh token endpoint or remove refresh token references from client
- [ ] Add signout endpoint behavior (server-side invalidation if required)

### D. Profile and Onboarding

- [ ] Ensure schema and controller field names match (`heightCm` vs `height`, `phone` vs `phoneNumber`, relation naming)
- [ ] Ensure onboarding pages call real backend routes
- [ ] Add validation layer (zod/joi) for all profile writes
- [ ] Add idempotent upsert behavior for onboarding steps
- [ ] Define profile completion rule centrally (not scattered per page)

### E. Matching, Interests, Shortlist

- [ ] Fix shortlist middleware import and auth field usage
- [ ] Ensure shortlist API request payloads are consistent (`profileId` vs `shortlistedUserId`)
- [ ] Make matching score deterministic (replace random score in service)
- [ ] Add pagination and sorting on matches/interests APIs
- [ ] Add guardrails for self-like, duplicate-like, and blocked users

### F. Real-Time Chat and Notifications

- [ ] Move socket event handlers from `index.js` to dedicated realtime module
- [ ] Persist message + emit ack with server timestamp and message ID
- [ ] Add message delivery state (`sent`, `delivered`, `read`)
- [ ] Add room strategy:
- [ ] User room: direct notifications
- [ ] Match room: conversation broadcast
- [ ] Add Redis adapter for horizontal scaling
- [ ] Add rate limit + payload validation for `send_message`

### G. Payments and Subscriptions

- [ ] Keep mock mode behind feature flag (`PAYMENT_MODE=mock`)
- [ ] Add signature verification in production flow
- [ ] Enforce idempotency for payment verification callbacks
- [ ] Add subscription reconciliation job
- [ ] Add audit trail for payment status transitions

### H. Admin and Moderation

- [ ] Confirm admin middleware and role checks use same auth payload
- [ ] Add list filters + pagination for user and report moderation
- [ ] Add unblock/ban expiry handling
- [ ] Add immutable audit logs for moderation actions

### I. Quality Gates

- [ ] Fix frontend lint errors before feature work (currently fails with many errors)
- [ ] Add backend test harness (Jest/Vitest + supertest)
- [ ] Minimum test coverage targets:
- [ ] Auth, profile update, like/match, shortlist, chat persistence, payment verify
- [ ] Add contract tests (frontend vs backend API)
- [ ] Add pre-commit checks (`lint`, `typecheck`, `test`)

### J. Observability and Operations

- [ ] Add structured logging with request IDs
- [ ] Add centralized error handler and error taxonomy
- [ ] Add metrics:
- [ ] API latency, 4xx/5xx, socket connects/disconnects, message throughput
- [ ] Add tracing for auth, matching, payment, and chat flows
- [ ] Add runtime dashboards + alert thresholds

### K. Security and Compliance

- [ ] Stop using fixed OTP in non-dev
- [ ] Move secrets to secret manager for prod
- [ ] Add input sanitization and abuse controls on chat/report endpoints
- [ ] Add CSRF/CORS policy hardening for production domains
- [ ] Add PII data retention and deletion policy

## 4. Recommended Real-Time Architecture (Production)

```text
Clients
  - Web (Next.js)
  - Mobile (React Native)
        |
        | HTTPS + WebSocket
        v
API Edge (Nginx / API Gateway)
        |
        +------------------------------+
        |                              |
        v                              v
Express REST API                  Socket.IO Gateway
  - Auth, Profile, Match,         - Connection auth
    Payment, Admin                  - Room management
  - Validation + Rate limits        - Event acks
        |                              |
        +--------------+---------------+
                       |
                       v
                Domain Services
         (auth, matching, chat, payments)
                       |
          +------------+------------+
          |                         |
          v                         v
      PostgreSQL                Redis
  - Source of truth        - Socket adapter
  - Transactional data     - Caching + rate limits
          |
          v
   Async Workers/Queues (BullMQ)
  - Notifications
  - Email/SMS
  - Payment reconciliation
  - Search index updates

Object Storage (S3/Cloudinary) for media
Observability stack for logs, metrics, tracing
```

## 5. Recommended Folder Structure (Real-Time Friendly)

Keep current `backend/`, `frontend/`, `mobile/` split, but reorganize internals by module.

```text
MONY/
  backend/
    src/
      app.js
      server.js
      core/
        config/
        middleware/
        errors/
        logger/
        validation/
      modules/
        auth/
          auth.routes.js
          auth.controller.js
          auth.service.js
          auth.repository.js
          auth.validators.js
        user/
          user.routes.js
          user.controller.js
          user.service.js
          user.repository.js
        profile/
        preference/
        match/
        interaction/
        shortlist/
        review/
        payment/
        admin/
        verification/
      realtime/
        socket.server.js
        socket.auth.js
        socket.rooms.js
        socket.handlers.js
        socket.events.js
      infra/
        prisma/
        redis/
        storage/
        queue/
      jobs/
        payment-reconcile.job.js
        notifications.job.js
      tests/
        unit/
        integration/
        contract/
    prisma/
      schema.prisma
      migrations/
      seed/

  frontend/
    src/
      app/
      features/
        auth/
        onboarding/
        profile/
        matches/
        chat/
        payments/
        admin/
      shared/
        api/
        ui/
        hooks/
        utils/
        constants/
        types/
      state/
      tests/

  mobile/
    src/
      features/
      shared/
      state/
      services/

  packages/
    shared-types/
    api-contracts/
    eslint-config/

  infra/
    docker/
    ci/
    monitoring/

  docs/
    architecture/
    api/
    runbooks/
```

## 6. Suggested Execution Order

1. Stabilize P0 integration issues (ports, auth keys, broken routes, schema mismatches).
2. Lock API contracts and update frontend calls.
3. Extract real-time logic into dedicated `realtime/` module and add Redis adapter.
4. Add tests and CI quality gates.
5. Finish observability and production hardening.

