# Matree production readiness — current implementation branch

Branch: `feature/matree-production-hardening-20260925`

## Source contract

This branch is implementing the uploaded Matree master source and production-ready pin-to-pin implementation plan. Historical DONE/IMPLEMENTED labels are not treated as current proof. Each release claim must be re-established on the exact branch HEAD.

## Repository reality discovered

The connected repository is currently a Next.js + Express + Prisma/PostgreSQL application with an Expo placeholder mobile directory. It is not currently the later Kotlin/Compose + Firebase architecture described by the latest master planning documents. This branch therefore applies urgent source-of-truth/security/trust invariants to the repository that actually exists, without pretending an Android/Firebase migration is already present.

## P0 defects reproduced and hardened in this branch

- Fixed OTP `123456` removed.
- OTP is random, hashed, expiring, attempt-limited and resend-throttled.
- Production OTP requires a real configured delivery provider; it no longer reports fake delivery success.
- Socket.IO authenticates JWT during handshake and derives sender identity from the authenticated subject.
- Arbitrary room joins are removed; clients can only join their own authenticated room.
- Chat persistence includes receiver, stable client message ID, SENT/DELIVERED/READ timestamps, retry deduplication and block enforcement.
- Payment mock order/verification path replaced with server-created Razorpay orders, HMAC signature verification, gateway verification, amount/currency/order binding and transactional/idempotent subscription activation.
- Premium no longer bypasses protected/request-access photo privacy.
- Block is now an authoritative database state and cascades into relationship/chat access.
- Interest transitions prevent repeated send from downgrading an accepted connection; explicit accept/decline/withdraw routes exist.
- Shortlist now uses the canonical JWT subject and suppresses blocked/inactive/banned profiles.
- Discovery/search no longer invent compatibility percentages or human placeholder photos.
- Profile compatibility returns explainable reasons with no fabricated percentage.
- Social login no longer fabricates an incomplete matrimony profile.
- Database migration added for OTP, block and reliable message-state fields.
- Node regression tests and GitHub production gates added.

## Important remaining release blockers

This branch is intentionally NOT marked production-ready yet. The two uploaded plans require much more than the P0 hardening above. Remaining work includes, among other items:

1. Complete account/session lifecycle (refresh-token rotation/revocation or converge fully on the later canonical auth architecture).
2. Full profile versioning, draft recovery, reconfirmation/search-status lifecycle and activity freshness engine.
3. Complete media processing/moderation pipeline and protected-media grants.
4. Full contact-request/photo-access grant data model and revocation.
5. Notification persistence/FCM/deep-link authorization.
6. Complete entitlement/quota model, refunds/reconciliation and policy-aligned Android billing if/when Android is built.
7. Support ticket lifecycle, moderation queues, risk engine and admin RBAC.
8. Account pause/deletion resumable server cleanup.
9. Exposure ledger, skip-vs-hide, zero-result diagnostics, mutual preference importance model.
10. Route-by-route frontend state normalization (loading/content/empty/error/offline/retry) and removal/hiding of every unsupported/dead feature.
11. Anti-fabrication sweep across every frontend route and resource, not only the high-risk backend paths hardened here.
12. Accessibility/localization/theme system and the later religion-aware appearance architecture.
13. Full automated integration/concurrency/abuse/load/device testing.
14. Decide and execute the architecture reconciliation required by the latest canonical source: migrate/build Kotlin + Jetpack Compose + Firebase, or formally supersede that later requirement. The existing repository cannot truthfully be called complete against that requirement while it remains Next.js/Express with only an Expo placeholder.
15. Production provider configuration, legal/privacy/store configuration, physical-device QA and staged rollout.

## Release rule

Do not merge or label this work complete solely because the code volume is large. The exact final release SHA must pass all mandatory gates, migrations, two-user/two-device journeys, payment recovery, privacy/block tests and external provider/configuration checks.
