# Production Release Gates

A release is blocked if any item below fails.

## P0 correctness and safety

- Blocked users cannot message, request contact, request photos, or reappear in active discovery.
- Hard partner preferences cannot be bypassed by ranking, premium status, or fallback logic.
- Paused, found-match, married, closed, deleted, banned, and stale profiles are suppressed according to lifecycle policy.
- Authenticated API failures never substitute synthetic profiles, interests, messages, payments, or activity metrics.
- Protected contact/photo data requires policy-compliant consent; premium membership cannot override another member's privacy.
- Profile deletion removes discoverability immediately.

## Payments

- Order creation is idempotent.
- Gateway signature verification is mandatory.
- Payment completion and subscription activation are transactionally consistent.
- Client/network retries cannot create duplicate charges or duplicate subscriptions.
- Entitlement usage and visible limits are generated from the same backend policy.
- Production refuses to simulate gateway success when credentials/provider are unavailable.

## Messaging

- Socket identity comes from a verified JWT.
- Client cannot select its sender identity.
- Client message IDs prevent duplicate rows on retry.
- `sent` means persisted by server; `delivered` requires recipient acknowledgement; `read` requires recipient read action.
- Late receipts cannot downgrade a read message.
- Failed sends remain visibly retryable.

## Profile and media integrity

- Concurrent stale profile edits return a conflict instead of overwriting newer data.
- Existing approved primary photo remains active until a replacement upload succeeds and passes required moderation.
- Missing photos use a neutral placeholder, never another person's face.
- Verification badges state the verification type/status actually completed.

## Support and moderation

- Every support case has a durable ticket and event history.
- Staff resolution does not silently equal user-confirmed closure.
- Reports create reviewable risk signals and audit trails; a single report does not automatically establish guilt.
- Critical-risk queues are visible to authorized operations users.

## CI evidence

- `npx prisma validate` passes.
- Backend syntax and trust regression checks pass.
- Frontend ESLint passes.
- Next.js production build passes.
- Main-schema -> hardening-SQL -> target-schema migration drift check passes against PostgreSQL.

No release should be described as production-ready while any required gate above is red or untested.
