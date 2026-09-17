# Competitor Issue Defense Matrix

This document turns recurring matrimony-app complaints into product invariants and testable requirements. It is not a claim that every competitor has every issue; it is a defensive checklist based on recurring complaint patterns observed across major matrimony products.

| User failure pattern | Platform defense | Release invariant |
|---|---|---|
| Same profiles shown repeatedly | ProfileExposure history, interaction state, cooldownUntil | Hidden/blocked profiles never resurface; skipped profiles respect cooldown; repeat rate is measurable |
| Hard preferences ignored | `hardFields` persisted and applied before ranking | Ranking cannot reintroduce a profile rejected by a hard filter |
| Fake or misleading profile inventory | No authenticated mock inventory; granular verification; moderation/risk signals | Logged-in API failure produces error/empty state, never synthetic people |
| Human-looking placeholder photos | Neutral local placeholder + explicit photo state | Missing/private/under-review photo is never represented by a different human face |
| “Verified” badge is ambiguous | Separate phone/photo/identity verification states | UI must identify what was actually verified |
| Old/inactive profiles dominate results | lastActiveAt, searchStatus, reconfirmation, stale suppression | Paused/found-match/married/closed/stale profiles are not normally discoverable |
| Premium appears to change compatibility | Entitlements separated from matching | Payment cannot increase compatibility score or bypass hard eligibility |
| “Unlimited” later reveals hidden limits | Explicit plan limits/usage/renewal state | Every quota shown to user must match backend entitlement counters |
| Payment succeeds but access is missing | Idempotent payment + transactional activation + status recovery | A verified payment can be safely retried without double charge/duplicate subscription |
| Repeated payment after network failure | Idempotency key + payment status endpoint | Client checks existing payment state before creating another order |
| Support active before purchase, absent after | Persistent tickets/events, priority/status, user-confirmed closure | Staff may resolve; only user confirmation (or documented policy timeout) closes a ticket |
| Accidental interest cannot be undone | Withdraw interest endpoint/UI | Pending sent interest can be withdrawn without deleting account/history |
| Chat says delivered when recipient never received it | Recipient-authenticated delivery receipt | Server emit alone never marks a message delivered |
| Duplicate chat messages after retry | clientMessageId unique idempotency key | Retrying the same client message cannot create a duplicate row |
| Blocked user can still message | Block checks on REST/socket message paths and match deactivation | Blocking immediately stops new messages and protected-data access |
| Search filters disappear after navigation/app switch | Persisted filter and scroll state | Returning to discovery restores search/filter context where appropriate |
| Hidden/private data unlocked by payment | Consent-aware privacy services | Premium is capability, never permission to override another member’s privacy |
| Photo upload failure silently changes photo | Atomic photo replacement; explicit storage error | Existing approved photo remains until replacement succeeds and is approved |
| Moderation/reporting auto-punishes from one complaint | Risk signals + review state + audit trail | Reports create evidence/risk signals, not automatic guilt |
| Account deletion leaves profile discoverable | Immediate search deactivation + deletion timestamp | Deleted account disappears from discovery immediately; retention is separate |
| Concurrent parent/self edits overwrite each other | Profile version check | Stale edits return conflict instead of silently overwriting newer data |

## Required launch evidence

1. Backend syntax and trust regression checks pass.
2. Prisma schema validates and target migration produces zero schema drift from the prior production schema.
3. Frontend lint and production build pass.
4. No authenticated screen substitutes demo people, demo payments, demo interests, fake metrics, or random human photos after an API failure.
5. Payment, chat, block, privacy, stale-profile, hard-filter, deletion, support and concurrency invariants are covered by repeatable tests before production merge.
6. External providers (OTP delivery, payment gateway, identity verification, storage/moderation) fail closed in production when not configured; they must not silently simulate success.
