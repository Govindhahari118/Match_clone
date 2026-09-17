# Matrimony Trust/Freshness Hardening — Implementation Status

Date: 2026-09-17
Branch: `feature/matrimony-trust-freshness-hardening-20260916`

## Implemented

- Granular verification states instead of one generic badge.
- Activity/freshness buckets and status reconfirmation.
- Profile lifecycle states: active, low-activity, paused, found-match, married, closed.
- Stale/inactive lifecycle suppression in discovery.
- Hard vs soft partner preferences; hard filters applied before ranking.
- Explainable compatibility reasons.
- Exposure/repetition history and cooldowns.
- Skip/hide/block distinctions.
- Blocking enforced across interaction/messaging/privacy paths.
- Contact and protected-photo consent requests.
- Profile versioning to reject stale concurrent edits.
- Immediate removal from discovery on delete/deactivation.
- JWT-authenticated realtime messaging; sender identity server-derived.
- Message idempotency, sent/delivered/read/failed states.
- Recipient-confirmed delivery receipts.
- Secure OTP generation with expiry/attempt/rate controls.
- Idempotent payment order creation and cryptographic verification.
- Transactional subscription activation and payment-status recovery.
- Explicit plan entitlements/limits.
- Support tickets with event history and user-confirmed closure.
- Admin moderation/risk/support operational queues.
- Photo moderation states and atomic primary-photo replacement.
- Removal of random-human photo fallback in live flows.
- Removal of authenticated demo match/chat/interest/payment fallbacks.
- Interest withdrawal.
- Neutral local profile placeholder.
- CI release gates for schema validation, trust regression checks, frontend lint/build, and database migration drift.

## Remaining release gates

This branch must not be called production-complete until all GitHub Actions checks pass and any failures are corrected. External provider configuration (SMS/OTP, Razorpay production credentials/webhooks, identity/liveness provider, object storage/moderation) must also be validated in the production environment. Native Android implementation is not present in this repository; `mobile/` remains a placeholder and is outside the claim of completion for this branch.
