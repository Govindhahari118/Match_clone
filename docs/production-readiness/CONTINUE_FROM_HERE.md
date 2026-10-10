# Matree — durable implementation and release handoff

**Status: NOT PUBLICLY LAUNCHED / PRODUCTION NO-GO.**

This file is the durable continuation point if the development conversation is deleted. GitHub and exact-SHA CI evidence are authoritative; never use this document to claim a run passed without checking its actual conclusion.

## Repository identity and process

- Repository: `Govindhahari118/Match_clone`; target branch: `main`.
- Merge only with exact PR head SHA checked, full mandatory Production CI passing on that exact SHA, no unreviewed conflicts, and a verified clean new `main` candidate.
- Mandatory CI jobs: Android build/lint/tests/release-integrity gates; Room migration and privacy instrumentation; Firebase Functions lint/build/tests and **non-bypassed** production dependency audit; Firestore/Storage emulator security rules.
- Never deploy secrets, configure production providers, enable feature flags, assert physical-device acceptance, or mark release GO without real operator evidence.
- Historical PRs #43, #44, #45, #50, #51 and #53 have dirty/unmergeable branches; do not blindly merge. Recheck what was already integrated into `main`.

## Confirmed 8 October 2026 repository changes

- PR #57 (production dependency remediation) was merged as commit `95bd88d0dc34a2d5c2d5b55e26088648769d595b` after all four jobs in Production CI #1412 passed on PR head `20ca075a60519100e2968ae2767b90d94dd80127`. `proxy-addr` updated from 2.0.7 to patched 2.0.8; final PR contained only `MatchApp/functions/package-lock.json`. A fresh exact-`main` certification is still required.
- PR #56 (`gpt/matree-call-policy-port-20261008`) implements provider-independent call request coordination in Firebase Functions + Android and is pending **new** exact-head CI after bringing the audited lockfile over from main. It must remain unmerged until all checks and security review pass.

## PR #56 scope and important review checks

- Authenticated request/get/respond/cancel callables with current mutual-match, account-active, block, and privacy eligibility checks. Policy limits proposed time to 15 minutes–14 days; active requests have replacement cooldown.
- Transactional state mutations and request revisions for distinct notification IDs; persisted notification delivery may still fail after a successful transaction. Review retry/outbox semantics, permission checks, and idempotency before production.
- Participant-only Firestore reads, server-only writes, and corresponding emulator rules regression tests.
- Android mutual-match scheduling UI, response/cancel actions, recoverable inline errors, English/Hindi/Telugu localization.
- Calling provider still **disabled**; scheduling/accepting a call does not establish any audio/video connection. Do not fabricate a dialer, relay number, or session history.
- Review expiration/obsolete requests, active mutual-match revocation, notification action navigation, supported timezone UX, transaction race conditions, UID consistency, and account deletion cleanup.
- Rerun all four Production CI jobs on **latest PR head**, and after merging certify the exact new main SHA.

## Remaining repository engineering / verification

1. Complete PR #56 review, unit/rules/UI tests and precise-source CI, then merge only if green.
2. Review stale PRs (#43, #44, #45, #50, #51, #53) against current source; close or replace with documented evidence, not guesses.
3. Implement real secure voice/video provider with vetted architecture and server-allocated sessions, signed/replay-safe webhooks, authorization revocation and kill switch. Do not enable rollout until operations/credentials/provider acceptance exist.
4. Verify full multi-account interest → match → notifications → text/image/voice chat → block/privacy/revocation, photo access, deletion, search, payments and recovery flows.
5. Run adversarial security tests, non-debug historical Room upgrade fixtures where applicable, performance/accessibility/localization QA, and device-network/process-death chaos matrix.
6. Keep Nearby, Kundali, NRI promotion, broad profile video, and unvalidated verification/liveness flows feature-gated OFF.

## Required production operator evidence — cannot be completed from GitHub

Supply real production Firebase configuration, secrets, deployed rules/indexes/Functions, App Check/Play Integrity, least-privilege admin operations, release AAB signing and Play App Signing; verify production app domain, HTTPS Digital Asset Links, FCM, Google Play Billing products/API/RTDN/refund/restore, real device E2E, provider credentials, penetration/security review, legal/Play Data Safety, monitoring and incident/rollback exercise. Use `MatchApp/docs/release/PRODUCTION_EXTERNAL_EVIDENCE.template.json` and `GO_NO_GO.md` for evidence fields. Missing evidence stays **false**, not artificially approved.

## Definition of production GO

Release is GO only after exact HEAD Production CI and all mandatory operator-provided external evidence pass, secure-call provider/session/webhook/device gates are met (if launch parity requires it), signed final AAB is verified, and the staged rollout/rollback ownership is documented. Otherwise **NO-GO**.
