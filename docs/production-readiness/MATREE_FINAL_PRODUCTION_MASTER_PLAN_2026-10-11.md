# MATREE — FINAL PRODUCTION IMPLEMENTATION AND SIGN-OFF MASTER PLAN

**Version:** 1.0 · **Date:** 2026-10-11 · **Repository:** `Govindhahari118/Match_clone` · **Status:** PLAN APPROVED FOR ENGINEERING REVIEW; PRODUCTION NO-GO

> This is the source-of-truth execution protocol, **not** a claim that features, deployment or approval are complete. Companion: `MATREE_100_FEATURE_E2E_DELIVERY_2026-10-10.md` (MAT-001 through MAT-100), #60 integration, #61 hardening, release issue #26 and production gate scripts. Treat any contradictions as blockers until the release owner resolves them with a recorded decision.

## 0. Objectives and non-negotiables

Deliver a secure, fast, accessible Android matrimony platform with correctly enforced age/identity/consent, accurate discovery, private profiles/media, trustworthy messaging, mature moderation, reliable billing and defensible production release. Preserve already shipped working behavior. No feature is counted by screen presence, compile success or a favorable rating alone. Never weaken rules to make tests pass, fabricate signed builds, proof or reviewer approvals, merge into unprotected main, or enable nonfunctional calling.

**Operating modes:** Code-ready (merged + tested), Device-ready (physical matrix), Production-ready (all external gates), Released (observed staged release). These are distinct.

## 1. Locked dependency order

| Gate | Phase | Owner | Implementation checklist | Required proof |
|---|---|---|---|---|
| G0 | Governance, scope and baseline | Repository admin + release lead | Protect main; decide in-scope and deferred features; map 100 IDs to code/test/UI; freeze P0/P1 criteria; appoint owners; validate source SHA and PR dependencies. | Branch protection evidence; written scope decision; baseline CI; 100-row evidence matrix. |
| G1 | PR #60/#61 integration | Android + Firebase + independent security reviewer | Compare #50/#56/#43/#51/#53 with #60; preserve #59 Storage requirements and dependency lockfile; review privacy grant race and call requests; merge #60 only when independently approved; rebase #61; rerun all four checks and review UI. | Reviewed diff, parity matrix, 4/4 exact-head green after each merge and on main. |
| G2 | Identity/registration | Auth + Android + security | OTP rate limiting, provider eligibility, consent age gate and server DOB policy, guardian role isolation, profile onboarding state, saved progress, sessions, account lifecycle and delete/export. | New/existing user, provider failure, resend cooldown, suspended account, token revocation tests. |
| G3 | Profiles and protected media | Android + Functions + rules | Data field contract, completeness score, moderation, private contact/income, PUBLIC/ACCEPTED_ONLY/HIDDEN photo grants, request versus access collections, revocation, protected video, cache handling. | Owner/unrelated/blocked/matched roles verified by Firebase emulators and two devices; photo state matrix. |
| G4 | Discovery and matching | Android + Firebase | Server-side status/block/stealth/freshness checks, mutual preference fit, strict filters, saved searches, pagination/cursors, rank explanations, inactive/missing values, optional astrology and fairness. | Reproducible results, boundaries, input validation, security tests, search performance against seeded data. |
| G5 | Interests/chat/notifications | Android + Functions | Interest state machine; chat authorization/read receipts; media restrictions; offline queue; exactly-once UX and server idempotency; mute/archive/block; FCM per-device locale and token rotation. | Two accounts: send/decline/accept/block/delete; interrupted network and notification retries no leakage. |
| G6 | Voice/video provider | Platform + provider + security | Approve provider and data-processing terms; server token allocation with short TTL; webhook signatures/replay protection; call session lifecycle; deny on block/revoke; connection error/timeout/disconnect; usage guardrails. | Real two-device call proof, webhook negative suite and security sign-off. Transport off until then. |
| G7 | Family/cultural workflows | Product + backend + QA | Explicit adult-owned guardian invitations, scoped permissions, removal/revocation, EN/HI/TE, regional taxonomy, optional horoscope, biodata PDF, family shortlist/meetings without revealing private chats. | Permission matrix, consent E2E, localization/device-font screenshots, delete/revoke propagation. |
| G8 | Trust/moderation/admin | Security + trust operations | Account verification policy, duplicate abuse, fraud/reporting, moderator queue and RBAC, audit retention, appeals, rate limits, ID data minimization, export/delete and incident routing. | Negative authorization tests, independent assessment, support workflow and retention checks. |
| G9 | Subscription/payments | Billing + backend + QA | Products/store account mapping, Play Billing Library, Developer API verified entitlement, RTDN replay/idempotency, restore, pending, cancel, grace, refund/revoke, downgrade, operator recovery. | Licensed physical-device tests, sandbox purchase evidence, protected entitlement reads, zero unauthorized unlock. |
| G10 | Operations/quality | Android + QA + SRE | Cold starts, performance budgets, low-memory process death, Room migrations, accessibility and large text, UI state fixtures, crash/ANR, logs redacted, SLO alerts, resource budgets. | Full functional matrix, no P0/P1, non-regressing baseline metrics, runbooks. |
| G11 | External release evidence | Release owner + legal + infra | Production Firebase project/rules/indexes/functions, App Check Play Integrity, real app-links domain, signing, privacy/legal, Data Safety, closed test and listing, operator proof. | Every mandatory prelaunch gate evidenced on frozen SHA; security/legal approval and rollback rehearsal. |
| G12 | Staged rollout and closure | Release owner + SRE + support | Internal then closed then phased rollout; monitor auth, crash, billing, privacy, chat, callbacks; stop/rollback thresholds; incident response; post-launch verification. | Full production gate PASS incl 2 release checks; telemetry, app-store proof and closure evidence. |

**Critical path:** G0 → G1 → G2/G3 → G4/G5 → G6/G8/G9 → G10 → G11 → G12. In parallel only where independent; no provider-dependent work counted complete before live provider proof.

## 2. Exact implementation algorithm for every MAT-001–MAT-100 item

1. **Discover:** Name Android composable/viewmodel, repository, schema, callable, rules, permission gate, localization keys, test files. Mark `AUDIT_REQUIRED` until inspected.
2. **Classify:** `EXISTS_TEST_NEEDED`, `PARTIAL`, `MISSING`, `DEFECTIVE`, `BLOCKED_EXTERNAL`, or `OUT_OF_SCOPE_APPROVED`; record user impact, priority P0/P1/P2, dependencies, release scope and owner. A screen alone is never complete.
3. **Specify:** Given/when/then behavior, adult/guardian/moderator entitlements, data flows, legal/privacy requirements, loading/empty/offline/error, abuse/rate limits, i18n/a11y and migration/backward compatibility.
4. **Test first:** Create positive, boundary, negative, multi-account and concurrency cases; add Firebase emulator rules and Android test where relevant; explicitly verify duplicate/reordered events, blocked states and malformed input.
5. **Implement:** Small cohesive PR; server-authoritative security, least privilege, idempotent writes, redacted logs, confirmed dependencies, no fake provider endpoints; document schema/index updates.
6. **Verify code:** Functions lint/build/tests/audit, Firebase rules tests, Android build/lint/unit and Room migration/instrumentation; all four GitHub Production CI jobs green on the **exact head SHA**. Fix regressions and rerun after changes.
7. **Verify product:** Two-account (and guardian/admin where relevant) physical-device E2E, screenshots/recordings, real environment integration evidence; EN/HI/TE; low-end/offline/restart/device rotation; QA and independent security review for P0/privacy/payment changes.
8. **Mark done:** `IMPLEMENTED_VERIFIED` **only** after merged protected-main SHA, exact-head CI, QA evidence, policy review and acceptance check. Otherwise retain accurate status. The team updates the 100-row tracker after **each item**, with evidence links, test count/date, owner and residual risks.

**Progress arithmetic:** `Verified = count(MAT-001..MAT-100 with IMPLEMENTED_VERIFIED)`; `Completion % = Verified / 100 × 100`. `OUT_OF_SCOPE_APPROVED` remains visible and is **not** represented as implemented. Separate `releaseGatePassed` from feature count. Do not mark 100/100 if 100 records have no evidence.

## 3. Priority and workflow

- **P0:** Authorization/privacy leakage, age eligibility, data deletion, account exposure, chat/blocked interaction, incorrect billing access, crashes, call-provider security, main/CI/release sign-off.
- **P1:** Essential onboarding, profile, reciprocal matching, searches, notification lifecycle, moderated media, accessibility, family access boundaries.
- **P2:** Optional astrology enhancements, profile boosts, concierge polish, configurable engagement experiments (unless approved launch scope promotes them).
- Ticket flow: `AUDIT_REQUIRED → READY → IN_PROGRESS → CODE_COMPLETE → CI_VERIFIED → DEVICE_VERIFIED → REVIEWED → IMPLEMENTED_VERIFIED`. On any regression, revert to `DEFECTIVE`. External-dependency items become `BLOCKED_EXTERNAL`, never forced Done.
- Use issue and PR references for each change. PRs organized by security domain, not all 100 in a single unreviewable merge. Code owner approvals cannot be self-approved.

## 4. Comprehensive acceptance gates

| Area | Must be demonstrated |
|---|---|
| AUTH | Eligible adult only; phone/email OTP not reusable or brute-forceable; login/provider changes honor revoked sessions |
| PROFILE | No private identifiers, birth details or income leak through search, push, logs, public media URLs |
| PRIVACY | Blocked/hidden/revoked cannot read protected media or communicate; race and stale-token checks pass |
| DISCOVERY | No inactive, self, blocked, stealth or disallowed reciprocal matches, including deep links/cursors |
| CHAT | Only authorized participants access thread/media; retry/idempotency; delete/block invalidates future writes |
| CALL | Server-minted session+expiry; authenticated webhook; two-device audio/video; negative replay and disconnect matrix |
| BILLING | Entitlements backed by verified provider, lifecycle correct on duplicate/out-of-order RTDN and refund |
| RELEASE | All 4 exact-head CI, real-device sign-off, signed AAB/hash, approved legal/store/infra, prelaunch and full evidence |

### E2E matrix

Run on two physical Android devices and at least two independent accounts plus roles where applicable: new install, existing upgrade and backup restoration; OTP, account suspended, invalid token; incomplete/complete profiles; hidden/public/accepted-only media; photo grant and revoke after cache; blocked from both directions; every interest/chat state; offline queue, process death, network handover; call propose/cancel/accept/session end; purchase/grace/revoke/refund; locale switch EN/HI/TE including notifications; dark/light mode, TalkBack, font scaling, entry deep link, denial of permissions; report/appeal; export/deletion/reinstall. Include SDK/OS/device variation from supported minimum through current, plus lower-memory devices. Protect user data; synthetic test accounts only.

### Backend/rules negative tests

No client-controlled subscription, verification level, grant, role, age assertion, moderation, call token or server visibility mutation; block/read/revoke racing writes fail securely; query/cursor cannot bypass eligibility; unknown privacy state fails closed; upload metadata required; all ID/token fields only accessible to authorized scopes. Test deleted-account reads and multi-user cross-tenant paths.

### Security and privacy

Independent authorization review, OWASP MASVS/ASVS-informed mobile/API checks, least-privilege IAM, App Check enforcement only after signed-device evidence, secret-scanning/dependency audit, signed/reproducible AAB, PII redaction, data retention and consent policies, support/appeal workflow. Log only necessary metrics. No permanent private content in untrusted notifications. Horoscope/religion/caste preferences optional, purpose-specific and consent-aware.

## 5. Release evidence: nothing implied

Release issue #26 is the governing record. The production evidence script requires **49 prelaunch checks and 2 additional full-release checks**, plus the 29 operational groups. Every item needs artifact, owner, signed decision, environment, SHA, timestamp and expected/observed result. Missing or synthetic evidence = NO-GO. Store all secrets and personal records outside issues/PRs. Exact-head checks must be repeated on merged main, frozen release SHA, and final signed candidate as appropriate.

**External items:** main protections, independent reviews, Firebase project/identity and migration approval, Play services configuration, provider selection and webhook security, app links domain/TLS, Play signing/closed-track purchase tests, privacy/terms and Data Safety, human operations on-call, incident and rollback drill, staged rollout metrics, external approvals.

### Explicit no-go conditions

Any P0 or unresolved launch P1; 1 of 4 CI jobs failed/missing; security/privacy access leak; billing unlocked without verified entitlement; live call shown as functional without provider/device validation; missing release evidence; unprotected direct main merge; missing legal/user-data controls; no safe rollback; invalid signing or app links.

## 6. Release sequence and closure record

1. Reconcile #60/#61 and superseded PRs, review, test, protect branch, merge canonical work to main.
2. Complete approved launch-scope feature matrix and create `GO_NO_GO.md` evidence per area; keep deferred improvements as open tickets.
3. Finalize product identity; provision production Firebase, Auth, Play Billing, App Check/links, calling provider, security ops.
4. Freeze exact commit; build signed AAB; test store tracks and real devices; independent security/privacy/legal sign-off.
5. Run `production_external_gate.py --mode prelaunch` with genuine accepted evidence. If failure, fix and reset candidate SHA.
6. Controlled rollout with rollback triggers; verify monitoring and incident response; obtain two additional full-release proofs and run `--mode full`.
7. Only then mark issue #26 CLOSED / production released; record version, Play listing, release SHA, CI run links, signed artifact SHA256, reviewer names and post-release results.

## 7. Daily reporting template (required on every implementation session)

| Date / SHA | MAT IDs changed | Previously verified | Now verified | Partial/missing/blocked | CI | Physical E2E | Security review | Release decision |
|---|---|---|---|---|---|---|---|---|
| YYYY-MM-DD / sha | MAT-xxx | n | n | counts + IDs | 4/4 links or pending | evidence | owner | NO-GO / GO |

**Do not conflate the existence of tests, test execution, actual approval, successful merge or production rollout.**
