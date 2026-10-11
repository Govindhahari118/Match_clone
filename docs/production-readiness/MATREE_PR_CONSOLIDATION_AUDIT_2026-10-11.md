# Matrii PR consolidation audit — 2026-10-11

**Scope:** development-side PR dependency, file overlap and merge readiness. This is an inventory of inspected GitHub PR file lists and exact-head CI/review metadata, **not** line-by-line independent security approval or runtime E2E sign-off.

## Actual current PR state

| PR | Role | Changed files | Head | Exact head 4 CI | GitHub reviews | Plan |
|---|---|---:|---|---|---|---|
| #43 | Device-locale FCM notifications | 13 | `992fbead` | not independently rechecked | not rechecked | Compare against #60 |
| #50 | Server-authoritative protected photos | 22 | `43a4e411` | not independently rechecked | not rechecked | Compare against #60 |
| #51 | Post-match call lifecycle and notification routes | 21 | `342efd1e` | not independently rechecked | not rechecked | Compare against #60 |
| #53 | Alternative call coordination rebase | 16 | `00de7d8c` | not independently rechecked | not rechecked | Compare against #60 |
| #56 | Matched secure-call scheduling | 15 | `1ccf6aad` | not independently rechecked | not rechecked | Compare against #60 |
| #60 | Consolidated integration candidate for #43/#50/#51/#53/#56 | 45 | `714fb5d6` | **4/4 PASS** | **none** | First reviewed merge candidate into main |
| #61 | Matching/discovery, production plan, evidence guard, CI | 15 | `c86d58f` | **4/4 PASS** | **none** | Second reviewed merge candidate; currently based on #60 branch |

**Dependency order:** #60 → main; #61 → updated main. #61 is currently based on the *branch* for #60, so merging or retargeting #61 before #60 is unsafe. Fresh main-merge SHA CI must pass after each merge. At the time of inspection both PRs are draft and GitHub reports mergeable; this is not approval.

## Actual path overlap inspected

| Original | Overlap with consolidated #60 | Specific review obligations |
|---|---|---|
| #43 | `FcmDeviceRegistry.kt`, `MatchFcmService.kt`, language/notifications screens; `notifications.ts`, `shared.ts`; EN/HI/TE assets; notification scan | Validate token refresh, language change, old device cleanup, localization and deep-link routes |
| #50 | `PhotoRepository.kt`, `MatchDetailScreen.kt`, `PrivacyDashboardScreen.kt`, `firestore.rules`, `storage.rules`, photo policy/Functions/tests | Preserve Storage metadata enforcement from merged #59; test PUBLIC/ACCEPTED_ONLY/HIDDEN, blocked users, grant/revoke and media cache |
| #51 | `SecureCallRepository.kt`, notification deep link/action, call screen, `calls.ts`, policy and rules | Check routing and request lifecycle, reject stale revisions and inactive/blocked participants |
| #53 | Same coordination/routing layer with alternative rebase, rules and tests | Diff original implementations, make parity checklist; do not blindly merge duplicate branch |
| #56 | Call scheduling repository/screen/Functions, `firestore.rules`, policy, notification-link checks | Ensure call-request state is server-authoritative; scheduling is NOT voice/video media transport |

PR #60 integration description records protected-photo request separation, transaction-based call relationship checks, per-device locales and notification retry triggers. That text alone is not independent verification. In each original PR, review **added unique files**, lines changed and tests not merely shared filenames, before closure.

## Secure consolidation sequence

1. Assign independent Android, Firebase rules and backend/security reviewers and record accountable engineer and product owner. Do not self-approve.
2. Compare source diffs and tests for #43/#50/#51/#53/#56 against #60. Record individual behaviors confirmed covered, missing, incompatible, or intentionally deferred, with exact source/test paths and approval.
3. Verify the origin of all relevant security rules and confirm #59 metadata rule cannot regress. Run cross-account storage/read/blocked/privacy permission-negative tests.
4. Mark #60 ready only after complete review, successful exact-head 4 CI and accepted merge-diff parity matrix. Protect main first; merge #60, then check all four Production CI jobs on new main.
5. Rebase or retarget #61 on merged main; ensure it introduces only intended matching/discovery/ledger changes, no regression of #60; run all four checks on new exact-head SHA; independent review; merge; verify new main.
6. Close or supersede originals **only after** reviewers confirm parity and create a written comment linking merged canonical SHA. Do not close simply because #60 exists.
7. Begin feature-by-feature MAT-001..MAT-100 evidence audit and screen inventory on integrated main. Do not award `IMPLEMENTED_VERIFIED` without source/test/device/merged evidence.

## Current blocked authority and external steps

GitHub integration returned 403 trying to read `main` protection; owner must confirm ruleset. No reviewers recorded for #60 or #61. No signed build, two-device test, real provider-backed live transport, production Play Billing test, Firebase production deployment, legal or 49+2 evidence is represented by the passing CI. These remain explicit release NO-GO until supplied by owners.

## Permanent links

- Integration: https://github.com/Govindhahari118/Match_clone/pull/60
- Follow-up hardening: https://github.com/Govindhahari118/Match_clone/pull/61
- Release gate: https://github.com/Govindhahari118/Match_clone/issues/26
