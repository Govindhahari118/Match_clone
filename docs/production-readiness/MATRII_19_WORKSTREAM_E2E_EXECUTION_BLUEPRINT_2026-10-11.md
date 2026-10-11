# Matrii: 19-workstream end-to-end implementation and release execution blueprint

Version: 1.0 | Prepared: 2026-10-11 | Repository: Govindhahari118/Match_clone  
Scope: Android (Kotlin/Compose) + Firebase Authentication, Firestore, Storage, Cloud Functions, FCM, Room, payments, provider services, QA and rollout.  
Status: **EXECUTION PLAN ONLY — NO PRODUCTION GO OR 100% FEATURE SIGN-OFF**.

## 0. Baseline — verified facts and assumptions

- PR #60: ready for review, unmerged; head 714fb5d6344a5748ea49a52e134ef7d7a025804c, targets main; exact-head Production CI four jobs PASS (run 38074352075).
- PR #61: draft, unmerged; head 5c79ed3d50efea3ea865b7e739109a3945b578ea when this blueprint was prepared; targets the #60 branch; exact-head four jobs PASS (run 38135912428). **This plan commit itself moves the head; check its new CI.**
- PR #43, #50, #51, #53, #56 are open overlaps. Only reviewed code/test parity permits them to be superseded.
- The 100-feature tracker has exactly 100 MAT IDs; the evidence JSON has 100 entries, all AUDIT_REQUIRED. This is **not** evidence that no features exist, or that they have all been delivered.
- Screen classification registry enumerates 57 screens: 34 READY, 6 BETA, 1 UNSAFE (BackgroundCheckScreen, hidden), 16 POST_LAUNCH. These are routing classifications, **not** screen acceptance scores. Verification must respect beta flags, including VerificationScreen currently BETA/reachable.
- GitHub branch-protection API returned 403 to the current integration; branch protection must be **confirmed by admin**, not assumed absent or enabled. Neither #60 nor #61 has submitted independent reviews.
- Launch issue #26 is open and NO-GO. The production gate source declares **49 prelaunch** and **two additional full-release** requirements; all need genuine operator evidence on the same release SHA.
- Historical handoff notes nine inherited moderate Functions audit findings; obtain the current dependency report, triage each one, and document accepted/mitigated risk. A green high/critical CI threshold is not zero residual risk.

## 1. Decision rules and release invariants

1. The objective is a **verified and securely deployable Matrii application**, not a checklist marked green. Do not assume all 100 MAT requirements must ship in the first release; scope decisions must be explicit and cannot bypass launch-critical obligations.
2. Every workstream has one accountable engineering owner, an independent reviewer, source/test paths, an issue/PR, accepted positive and adversarial tests, and traceable evidence. Never mark Done from screenshots, plans, CI alone, or an unmerged branch.
3. Protect least privilege: Firebase server/rules must authorize all sensitive reads/writes; a Compose disabled button is never an authorization layer. Unknown field values cannot be interpreted as affirmative eligibility or consent.
4. No direct edits to main or force-push/rebase of shared approved branches; merge dependency order #60 then #61. Re-run exact-commit CI after conflicts, rebase, merge and release freeze.
5. Guardian actions, photos, identity, messaging, calls, payments, reporting and deletion require explicit consent, lifecycle cleanup and cross-account denial tests.
6. Real call-request **coordination is not voice/video media transport**. The external gate presently includes provider-integrated, webhook-security and physical-call checks, as well as disabled-provider surface checks. The product/release owner must explicitly resolve how the launch gate and any provider-off mode can be satisfied without falsifying evidence; prefer implementing and testing the provider if required for launch.
7. Never commit credentials, production contact details, signing keys, purchase tokens, service-account files or device PII. Use delegated account roles and secret management.
8. Decision states: AUDIT_REQUIRED -> EXISTS_TEST_NEEDED / PARTIAL / MISSING / DEFECTIVE / BLOCKED_EXTERNAL -> CODE_COMPLETE -> CI_VERIFIED -> DEVICE_VERIFIED -> IMPLEMENTED_VERIFIED. OUT_OF_SCOPE_APPROVED only after a documented accountable optional-P2 deferral. Track scope and actual release launch separately.
9. Repeated re-planning is not implementation. After this blueprint, the next engineering work unit is a real diff/defect/test with a passing commit, not another substitute plan.

## 2. Ownership, sequence, and release train

| Wave | Workstreams | Mandatory exit and handoff |
|---|---|---|
| A — Integrate the reviewed baseline | W01 W02 W03 W04 | #60 merged first, then #61; independent review, protected-main checks and parity report |
| B — Establish truthful inventory | W05 W14 (inventory portion) | Every MAT ID mapped to code/status/owner; all 57 registered screens accounted for, plus any newly discovered screens |
| C — Close P0 functional/security gaps | W06 W07 W08 W09 W11 | End-to-end Auth/profile/discovery/chat/privacy/billing happy and denial paths passing |
| D — Complete remaining scope | W10 W12 W13 | Provider/guardian/locales/moderation/operations approved and tested; unsafe flags closed or safely hidden |
| E — Full quality + security | W14 W15 W16 | QA matrices, CI, adversarial tests, accessibility, physical device evidence and no open release-blocking defects |
| F — Provision, approve and launch | W17 W18 W19 | Frozen exact SHA, 49 prelaunch PASS, approved staged rollout, 2 additional full-release checks PASS |

Independent owner responsibilities: Android/Compose engineer for UI, Backend/Firebase engineer for rules/functions, Security/privacy reviewer for access control and data boundaries, QA lead for device/screens, Billing/provider owner for accounts, DevOps/release owner for CI/deployment, and Legal/product owner for approved scope and disclosures. The user handles authorized manual setup and approvals; engineers still implement the required code and integration tests.

**Parallelism:** Inventory, negative test development, and screen audits can begin while W01–W04 await review. Do not merge conflicted feature branches, deploy to production, or assign final merged-SHA verification until dependencies resolve.

## 3. Nineteen work packages — exact execution and acceptance

### W01 — PR #60 canonical privacy/call/notification review and merge (P0)
- Freeze the reviewed head SHA; inspect all 45 changed paths and the actual patch vs main. Confirm Android client, Functions, Firestore and Storage contract changes agree.
- Verify photoAccessRequests (view consent) and photoRequests (upload invitation) are separate; PUBLIC/ACCEPTED_ONLY/HIDDEN reads/writes, block cleanup, grant/revoke, cached media and #59 Storage metadata protections deny unauthorized users.
- Verify call-request transactions, participant identity, matching/blocked state, expiry/replay handling, notification retry triggers and locale-specific durable cards. Confirm call scheduling UI clearly does not promise live media.
- Run four exact-SHA production CI jobs, adversarial emulator rules tests and app/Functions unit tests; capture source/test mapping and security reviewer disposition for changed rules.
- Independent Android and Firebase/security reviewers approve; repository admin confirms protected main; merge once allowed and verify new main SHA CI.
- **Done evidence:** reviewed #60 diff and parity matrix, approvals, protected-branch proof, merge SHA, 4/4 post-merge CI, zero unresolved high-risk comments. Without these, status is READY_FOR_REVIEW, not DONE.

### W02 — PR #61 matching/discovery hardening integration (P0)
- Confirm #60 has actually landed on main; retarget/rebase the dependent #61 so it contains only intentional follow-on changes. Reconcile conflicts without overwriting privacy/call/security rules.
- Preserve numeric edge-case matching fixes; reciprocal eligibility, scorer monotonicity, overflow/NaN guards, pagination, stale profile handling, filter fail-closed semantics, and explicit true/false/unknown children status.
- Preserve the MAT-001..100 validator, 100-record ledger, safe P2 deferral rules, source contract tests, existing screen classification guard and documentation.
- Execute Kotlin unit tests, TS/Node tests, emulator rules tests, all four CI jobs on the rebased head. Review changed permissions and Firestore queries/security.
- Obtain independent approval, merge, verify merged main exact SHA all four jobs. Update source-of-truth docs after merge.
- **Done evidence:** new base SHA, conflict-resolution diff, review decision, rebase/head test URLs, merge SHA and post-merge 4/4.

### W03 — Historical PR parity and closure (P0)
- Diff #43, #50, #51, #53 and #56 against #60 and post-merge main at **patch/behavior/test level**, not only matching filenames.
- Explicit exceptions: MatchFcmService.kt naming from #43 already in main; interest_chat_contract_scan.py checks from #51/#53 have been adapted in #61; #56 package-lock.json is an older large diff and must be reconciled against current npm audit/dependency graph rather than blindly restored.
- Parity matrix must record each original requirement, source path, introduced data contract, test, canonical location, missing/different behavior, security assessment and reviewer resolution.
- Backport each real missing behavior as separately tested commit; check notification refresh/locale changes, photo grants, message deep links, callable routes and provider-off screens.
- Only after canonical code is merged and approved, comment with exact replacement commit and close overlapping PRs as superseded (never as release-ready).
- **Done evidence:** five signed parity dispositions, links to merged code/tests, explanatory close comments, no silently lost behavior.

### W04 — Protected main, governance and CI (P0; admin action)
- Repo admin chooses ruleset/branch protection for main: PR-required, independent approvals, required four real CI check contexts, resolved review threads, no force pushes/direct pushes/bypass, minimum privileged access.
- Resolve available GitHub connector permission limits (prior 403); record actual owner-side ruleset evidence rather than guessing its configuration.
- CI must validate exact PR head and synthetic merge where relevant; include Android lint/unit, Room migration/privacy, Functions lint/build/dependency audit, Firebase emulator rules and release/source guards.
- Add CODEOWNERS for security-sensitive files if not already present; require Firebase/rules and billing/provider reviewers for relevant paths. Confirm merge queue/update settings and reproducibility.
- Test protection using an authorized non-admin account without conducting destructive changes; publish approval and merge runbook.
- **Done evidence:** admin screenshot/export or API output, enforced check names, review/merge proof, clean post-merge main CI.

### W05 — MAT-001 through MAT-100 auditable implementation inventory (P0)
- Enumerate **all 100** existing tracker rows; keep IDs/names stable and evidence JSON complete. Do not relabel features from assumptions.
- For each MAT ID, inspect Compose route/screens/view models, repositories, callable functions, Firestore/Storage rules, schemas/migrations, i18n keys, flags, tests and live/external prerequisites.
- Record: owner; priority based on launch scope; planned vs actual behavior; affected screen/role; sourcePaths/testPaths; status; gap and severity; PR/merge SHA; positive and denial tests; device evidence; reviewer and last verification.
- Partition required launch features from explicit optional/P2 deferrals; reconcile consumer descriptions and paywall promises with available backend capabilities. P0/P1 cannot be out-scoped by altering the ledger.
- Review backend query limits/indexes, remote-config flag defaults and stale data/migrated accounts; compile dependencies into reviewable domain backlogs.
- **Done evidence:** 100 distinct reconciled feature entries with no AUDIT_REQUIRED left, documented classification and owner, broken/missing features moved to actionable defect issues. **Inventory Done does not mean all 100 features implemented.**

### W06 — Auth, onboarding, profile, preferences and account lifecycle (P0)
- Covers MAT-001..010 and MAT-011..020 primarily, with privacy-sensitive MAT-019 owned jointly by W09.
- Inspect phone OTP/email/Google-or-Apple auth availability and supported platforms; protect session expiry/revocation, brute-force, duplicate account, unauthorized guardian onboarding, minimum-age checks, consent and privacy acknowledgments.
- Define owner/guardian/dependent authority graph, explicit permission grants, guardian handoff/revocation, profile created-by role and adult subject consent. Prevent invisible account takeover.
- Validate progressive onboarding save/resume across app death/offline/reinstall, required vs optional fields, public/private split, education/family/income visibility, image/video uploads, draft/published state and profile completion.
- Build/update server-side profile invariants and Firestore/Storage rules; enforce same semantics after migration and cross-account reads. Account export/deletion flows invoke downstream cleanup.
- **Acceptance:** OTP replay/rate-limit; duplicate and underage denial; wrong-owner profile edit; consent revocation; process-death resume; password/session resets; offline drafts; partial upload retries; migration fallback; EN/HI/TE form copy. Only supported identity providers are claimed in product UI.

### W07 — Discovery, matching, filters and recommendation trust (P0)
- Covers MAT-021..040: basic/advanced filters, active/new, NRI/geography, exclusions, mutual fit, explainability, feedback and fairness.
- Cross-check bilateral preferences on the server; age/height/marital/children/religion/dealbreaker precision; unknown or malformed fields fail closed, never widening results.
- Stabilize ranking under NaN/infinity/extreme numeric vectors, tie sorting, duplicate profiles, cold start, unfair boost, stale timestamps and remote-config outage. Keep optional horoscope scoring opt-in and non-discriminatory policy reviewed.
- Implement deterministic cursor pagination and indexing; prevent same-profile leakage/reappearance across pages and races when block/reject/preferences change.
- Instrument impression/click/interest ranking metrics with pseudonymous consent-safe events; limit exposure of explanations that could reveal private attributes.
- **Acceptance:** predictable fixture rankings, reciprocal eligibility, no blocked/deleted/hidden/ineligible results, age boundaries, empty and degraded results, concurrent pagination, fairness test suite and relevance performance budget.

### W08 — Interests, messages, FCM, navigation and retention (P0)
- Covers MAT-041..044, 048..049, 081..090 (call items handled by W10; blocking by W09).
- Transactionally enforce interest accept/decline and mutual matching; chat/message entry allowed only under current server-owned relationship. Enforce message permissions/anti-spam/rate limits and contact sharing opt-in.
- Test realtime chat send/receive/delete/edit where actually supported, delivery/read states, attachments and voice-note permissions, retries/dedup/order under network failure, blocked/unmatched cleanup.
- FCM per-installation locale EN/HI/TE, token rotation, logout cleanup, notification sound/category, opt-out/quiet hours, deep link recipient identity, privacy-safe content and inactive/deleted account suppression.
- Retention mechanics (digests, saved matches, feedback, reactivation, email/WhatsApp) must enforce clear consent/opt-out, throttling and marketing category separation; unsupported capabilities stay hidden.
- **Acceptance:** two accounts and token/locale switch, spam denial, duplicate notification replay, offline message recovery, blocked user cannot message, deep links from cold start, quiet-hour matrix, deleted session isolation.

### W09 — Privacy, protected media, verification and abuse safety (P0)
- Covers MAT-019 and MAT-050..060; partner with W06/W08/W13.
- Make privacy server-authoritative: profile/media visibility, accepted-only photo access, explicit viewing grants, watermark/cache/revocation, private URLs, Storage object ownership and metadata validation.
- Rate-limit access requests and reporting; clear consent grants and chat/session capability on block/account deletion; treat previous relationships and screenshots/recents policy explicitly.
- Verify email/phone claims, optional selfie/liveness or minimized ID checks only through supported/approved processors; prevent identity-document leakage and retain only necessary data.
- Implement report, evidence attachment, moderation queue, escalation, impersonation/scam flags, appeals, support actions and audit trace with scoped moderator rights.
- User export and deletion must cover public/private Firestore documents, Storage, chat and notification artifacts under a documented retention/legal policy.
- **Acceptance:** multi-account adversarial Firestore/Storage tests for PUBLIC/ACCEPTED_ONLY/HIDDEN, blocked/stale grants, cross-user uploads/downloads, IDOR, denied reads/updates, cache invalidation and deletion/export reconciliation.

### W10 — True voice/video and scheduling (P1; provider-blocked)
- Covers MAT-045, 046 and 047. Separate existing matched-call request coordination from actual voice/video media transport. Do not display Start Call as available without working media and consent.
- Choose approved provider architecture (or an explicit launch scope change approved by release owners); configure SDK, secure server-issued per-participant short-lived credentials, caller/callee match/block validation, rate limits and billing controls.
- Enforce offer/answer and session state machine, missed/cancel/reject/expire, reconnect, background/foreground, network switching, camera/mic permissions and teardown when match revoked.
- Authenticate webhooks with signature, timestamp, replay/nonces, idempotency and event ordering; no secret material in client, logs, DB or analytics.
- **Acceptance:** two physical devices, secure media negotiation, no third-party join, denied blocked/inactive calls, provider failure fallback, no ghost sessions, call quality and abuse reporting tests.
- **Gate conflict:** current 49-prelaunch validator lists secureCallProviderIntegrated, secureCallWebhookSecurityPassed, secureCallPhysicalMatrixPassed, disabledProviderSurfacesVerified. Product/release owner must explicitly reconcile provider-disabled scope vs the existing gate, with code/test changes and an approved decision. Never supply synthetic truth values to pass.

### W11 — Membership, Play Billing, web-pay applicability and entitlement integrity (P0)
- Covers MAT-071..080; inventory plans/products and any web-payment eligibility/legal rules before implementing. Ensure no unsupported products are offered.
- Server-valid purchase lifecycle and mapping: Play purchase token validation, product/base-plan IDs, order/account attribution, pending/acknowledge, subscription activation, renewal, grace, pause, expiry, cancellation, refunds/chargebacks/revocation.
- Idempotent and order-independent real-time developer notifications (RTDN); recovery polling/reconciliation, device/account switching and restore/revoke; never trust client boolean for paid access.
- Reconcile offers/referrals, boosts, verified premium and concierge with explicit entitlements, expiration and fraud/rate limiting; hide benefits without implemented backend authority.
- **Acceptance:** licensed Play test accounts, real signed build and provider-side evidence for every state; unit/emulator tests for forged receipts, token replay, duplicate/out-of-order events, invalid SKU, wrong-user purchase and backend outages; receipts and GDPR/Indian-law-consistent data minimization reviewed.

### W12 — Family/guardian, culture, consent and localization (P1)
- Covers MAT-061..070 plus optional MAT-014..016 and 082/088. Clarify which culture, religion and family fields are user-provided/optional and who can view/edit them.
- Parent/guardian delegated roles are explicit, scoped, revocable and audited. Prevent a family account from impersonating an adult subject or silently reading private communication/media.
- Validate family-approved introduction, chaperone consent, shared shortlist and meeting scheduling with privacy and eligibility rules. Print/export biodata redacts private contact, photos, income and identity fields by policy.
- Optional horoscope/taxonomy uploads are access-controlled, versioned and disabled when incomplete, not marketed as objective compatibility. Localized terminology reviewed for user consent and sensitivity.
- EN/HI/TE all launch routes, notifications, errors, permission messages and legal notices; RTL/missing key fallback checked where applicable, locale-switch without logout, robust date/number/timezone formatting.
- **Acceptance:** consent grant/revoke with two actors, invalid guardian access denial, redacted printable output, locale switch + token refresh, string scans, human translation QA and device screenshots.

### W13 — Moderation, admin, support, offline, observability and resilience (P1)
- Covers MAT-091..100; reconcile launch-critical admin/abuse responsibilities separately from priority label so urgent rights defects remain P0.
- Support/moderation RBAC, impersonation prevention, immutable incident/audit logs, appeal lifecycle, triage SLA, abuse escalation and entitlements administration with dual-approval where privileged.
- Resilience: offline local caches and Room schema migrations, retry/idempotency, function trigger replay, consistency after app kill, database index budgets and operational migration/rollback procedure.
- Feature flags/experiments default safely OFF, role/region/locale aware, separated from billing authority and security permissions. Relevance analytics and conversion funnels exclude sensitive data and honor consent.
- SLOs: select p95 UI latency, cold start, crashes/ANRs, FCM and call quality thresholds from real production baselines; load/backpressure/budget/quotas tested. Do not invent achieved targets.
- **Acceptance:** adversarial admin-role rules tests, appeals and audit trail, data export/delete consistency, schema upgrade/downgrade handling, offline/online restoration, crash/perf traces, simulated provider outage and rollback drill.

### W14 — Every Android screen, route and visual QA (P0)
- Start with the existing screen-classification.json and matching scan: **57 registered screens**, not an assumed count of every file/route; detect newly introduced or unregistered screens.
- Record for EACH screen: source path/symbol, route, entry points, authorized user roles, feature flag, risk/privacy tier, design reference, screen state coverage, accessibility, locale and exact device evidence link.
- Test entry/back/deep-link navigation, first-run/loading/empty/populated/errors, offline/retry, invalid/denied, account-blocked/deleted, keyboard/IME, rotation, large text, TalkBack and notification target.
- Audit 34 READY screens for functionality (READY is not QA-approved); 6 BETA for enforced flags and safe copy, 16 POST_LAUNCH for no accidental production routing, and hidden UNSAFE BackgroundCheckScreen for zero exposure. VerificationScreen is BETA but reachable — explicit ship/no-ship decision and tests required.
- Match layouts to approved reference screenshots where available; images/screen quality may not be inferred from source alone. Capture device dimensions/theme/locale, reproduce and fix clipping, spacing and contrast regressions.
- **Done evidence:** each screen register row independently signed by QA + feature owner with repeatable build/device screenshot/video, full journey and relevant negative states. No global "all screens Done" until all in-scope rows are signed.

### W15 — CI, integration, security and release regression (P0)
- Build a stable test pyramid: Kotlin/JVM policy and Compose tests; Node/TS Functions policy; Firestore and Storage emulator denial matrices; Room instrumentation; representative authenticated app-to-Functions integration flows.
- Adopt deterministic fixtures for roles, ages, guardian, consent, matching relationships, paid/free, block, locale, race/retry and deleted users; no actual member data in CI.
- Enforce release guard and evidence validator on immutable SHA, fail closed on missing MAT entries/invalid status, fail on unsafe screen exposure, dangling localization keys and insecure rule changes.
- Test CI itself: negative fixtures must fail, including provider-off false positives, fake or missing evidence links, mismatched tracker rows, changed source state or unsigned release artifacts.
- Run dependency/SBOM/license scanning, secrets scans, static/dynamic auth vulnerability review, fuzz/property tests for filters and event transitions, performance thresholds and recovery tests. Triage historical moderate dependencies.
- **Exit:** green current SHA all four job contexts AND no outstanding critical/high security findings, test artifacts archived, QA sign-off on regression matrix; do not equate green draft with full-app E2E.

### W16 — Physical-device E2E, accessibility, locales and signed candidate (P0; QA/operator)
- Build signed candidate from frozen reviewed main; record git SHA, versionCode, package ID, signing fingerprint, file hashes and release distribution/test track.
- At minimum two isolated Android identities/devices with supported OS, low/high memory and screen size, Wi-Fi/mobile data, background restrictions and network degradation. Record exact model/OS/result evidence.
- Journey matrix: install/login/profile/preferences -> discovery/interest/accept -> chat/notification -> photo request/revoke -> block/report -> subscription purchase/restore -> account export/delete; separately two-device provider calling if in scope.
- Negative journey: wrong actor/guardian, expired token, stale protected URL, payment fraud, cross-account notification, denied permissions, process kill, uninstall/reinstall, locale switch, Airplane Mode, screenshot/recents.
- Accessibility: TalkBack traversal, semantic labels and focus order, 200% font, touch targets, contrast and dynamic color; locales EN/HI/TE plus device timezone variations.
- **Exit:** signed artefact, device matrix, reproducible QA reports and defect closure, independent security review; owner accepts remaining low-risk issues explicitly. Automated simulator CI is not physical proof.

### W17 — Production Firebase, external infrastructure, Play and App Links (P0; manual/operator)
- Repo/admin and product owners set final application ID, domain, release track, signing identity, Play Console roles and production Firebase project; keep staging isolated from production.
- Configure Auth providers/allowed domains/release fingerprints, App Check/Play Integrity, production Firestore rules/indexes, Storage policies, callable Functions, scheduled triggers, FCM, Crashlytics/analytics consent, budgets, RBAC and master data.
- Integrate provider secrets through approved secrets manager; rotate/test credentials and least-privilege accounts; no service keys in repository or chat.
- Validate HTTPS App Links, Digital Asset Links and package fingerprint; ensure cold-start/unauthenticated/wrong-account deep links fail safely.
- Execute migration, index deployment order, smoke tests, backup verification and rollback rehearsal on frozen SHA with logs and operator approval.
- **Exit:** exact infrastructure versions/deployment SHA, checkable logs, service owners, monitors, signed Android artefact and resolved checklist gate evidence. Development tests can run earlier; production switch requires owner authorization.

### W18 — Independent security, privacy/legal and store readiness (P0; independent reviewers)
- Security review covers Auth and Firebase Rules/Functions/Storage, horizontal privilege escalation, guardian impersonation, photo privacy, calls/webhook, billing token replay, rate limits, secrets, telemetry and supply chain.
- Threat-model account takeover, grooming/minor safety, harassment, social engineering, impersonation, identity/media retention, data export/deletion and sensitive cultural data; record severity, exploit preconditions and repair evidence.
- Legal/product owners review actual Privacy Policy, Terms, Age/Guardian policy, Support/Deletion, Data Safety, Play content rating, third-party SDK disclosures and any India/regional law implications. Do not fabricate legal sign-off.
- Prepare store listing, support contacts, internal/closed testing track, reporting/escalation and moderation operations; reconcile user-visible claims with actually delivered functionality.
- **Exit:** independent signed security findings disposition, approved legal/store texts, privacy mapping and actual release permission. No self-approval or "reviewed by CI" substitutions.

### W19 — 49 + 2 machine-governed release and monitoring (P0; release owner)
- Freeze one reviewed, merged and signed exact main SHA after W01–W18; verify all CI, evidence references, version and branch protections before continuing.
- Use the repository's authoritative production_external_gate.py and PRODUCTION_EXTERNAL_EVIDENCE.template.json; fill every prelaunch field with genuine operator evidence, signer, capture time and exact SHA. Synthetic CI fixture permitted **only in self-tests**, never production.
- Pass all 49 prelaunch gates, including protected source, Firebase, signing, Play, security/privacy, two devices, moderation, links, calls, accessibility, locale and rollback; do not omit inconvenient gates.
- Conduct internal/closed Play tests, Play prelaunch review, rollback/kill switch drill, response ownership, cost and crash monitoring; promote staged rollout only on written GO.
- Monitor crash/ANR, auth, matching, messaging, purchases, billing reconciliation, Rules denials, Storage abuse, cloud costs and provider quality during rollout; use published stop/rollback thresholds.
- Supply controlledRolloutCompleted and postRolloutHealthPassed proofs; pass --mode full on **the same frozen SHA**. Only then close launch issue #26, noting approvers, build, fingerprints, run URLs, rollout metrics and rollback owner.
- **Exit:** signed prelaunch and full-release gate results + real operator and QA evidence. No partial sign-off disguised as production-ready.

## 4. MAT-001..100 traceability — one primary work package per feature

The primary mapping below routes review ownership; each feature still invokes W05 inventory, W14 if it has UI, W15 tests, and W16 physical QA where launch-scope. The mapped work package is **not an implementation assertion**. Status values are copied from the tracker at plan-generation time; the live evidence JSON remains authoritative.

| Feature ID | Domain | Exact tracker requirement | Primary workstream | Tracker status |
|---|---|---|---|---|
| MAT-001 | 01 Identity/onboarding | Phone OTP login | W06 | AUDIT_REQUIRED |
| MAT-002 | 01 Identity/onboarding | Email sign-in | W06 | AUDIT_REQUIRED |
| MAT-003 | 01 Identity/onboarding | Google/Apple sign-in | W06 | AUDIT_REQUIRED |
| MAT-004 | 01 Identity/onboarding | Self/parent/guardian registration | W06 | AUDIT_REQUIRED |
| MAT-005 | 01 Identity/onboarding | Age eligibility checks | W06 | AUDIT_REQUIRED |
| MAT-006 | 01 Identity/onboarding | Progressive setup | W06 | AUDIT_REQUIRED |
| MAT-007 | 01 Identity/onboarding | Save/resume onboarding | W06 | AUDIT_REQUIRED |
| MAT-008 | 01 Identity/onboarding | Multilingual onboarding | W06 | AUDIT_REQUIRED |
| MAT-009 | 01 Identity/onboarding | Duplicate-account detection | W06 | AUDIT_REQUIRED |
| MAT-010 | 01 Identity/onboarding | Consent and privacy acknowledgement | W06 | AUDIT_REQUIRED |
| MAT-011 | 02 Profiles | Personal biodata | W06 | AUDIT_REQUIRED |
| MAT-012 | 02 Profiles | Education/employment | W06 | AUDIT_REQUIRED |
| MAT-013 | 02 Profiles | Income privacy | W06 | AUDIT_REQUIRED |
| MAT-014 | 02 Profiles | Religion/community/subcaste | W12 | AUDIT_REQUIRED |
| MAT-015 | 02 Profiles | Gotra/native place | W12 | AUDIT_REQUIRED |
| MAT-016 | 02 Profiles | Family contacts and details | W12 | AUDIT_REQUIRED |
| MAT-017 | 02 Profiles | Lifestyle/hobbies/values | W06 | AUDIT_REQUIRED |
| MAT-018 | 02 Profiles | Profile completeness | W06 | AUDIT_REQUIRED |
| MAT-019 | 02 Profiles | Private photo album and selective consent | W09 | AUDIT_REQUIRED |
| MAT-020 | 02 Profiles | Video introductions | W06 | AUDIT_REQUIRED |
| MAT-021 | 03 Discovery | Basic search | W07 | AUDIT_REQUIRED |
| MAT-022 | 03 Discovery | Age and height | W07 | AUDIT_REQUIRED |
| MAT-023 | 03 Discovery | Religion/community/caste | W07 | AUDIT_REQUIRED |
| MAT-024 | 03 Discovery | Education/profession | W07 | AUDIT_REQUIRED |
| MAT-025 | 03 Discovery | City/state/NRI/distance | W07 | AUDIT_REQUIRED |
| MAT-026 | 03 Discovery | Marital status/children | W07 | AUDIT_REQUIRED |
| MAT-027 | 03 Discovery | Lifestyle/family values | W07 | AUDIT_REQUIRED |
| MAT-028 | 03 Discovery | Saved and recent searches | W07 | AUDIT_REQUIRED |
| MAT-029 | 03 Discovery | Active/new profiles | W07 | AUDIT_REQUIRED |
| MAT-030 | 03 Discovery | Exclude declined/blocked/previously contacted | W07 | AUDIT_REQUIRED |
| MAT-031 | 04 Recommendations | Personalized daily matches | W07 | AUDIT_REQUIRED |
| MAT-032 | 04 Recommendations | Mutual preference fit | W07 | AUDIT_REQUIRED |
| MAT-033 | 04 Recommendations | Explainable score | W07 | AUDIT_REQUIRED |
| MAT-034 | 04 Recommendations | Behavior-based ranking | W07 | AUDIT_REQUIRED |
| MAT-035 | 04 Recommendations | Dealbreaker rules | W07 | AUDIT_REQUIRED |
| MAT-036 | 04 Recommendations | Optional horoscope scoring | W07 | AUDIT_REQUIRED |
| MAT-037 | 04 Recommendations | Conversation starters | W07 | AUDIT_REQUIRED |
| MAT-038 | 04 Recommendations | Feedback loop | W07 | AUDIT_REQUIRED |
| MAT-039 | 04 Recommendations | Inactive profile downranking | W07 | AUDIT_REQUIRED |
| MAT-040 | 04 Recommendations | Fairness and bias controls | W07 | AUDIT_REQUIRED |
| MAT-041 | 05 Communication | Interests accept/decline | W08 | AUDIT_REQUIRED |
| MAT-042 | 05 Communication | Intro messages | W08 | AUDIT_REQUIRED |
| MAT-043 | 05 Communication | Realtime chat | W08 | AUDIT_REQUIRED |
| MAT-044 | 05 Communication | Delivery/read indicators | W08 | AUDIT_REQUIRED |
| MAT-045 | 05 Communication | Privacy-safe voice calls | W10 | AUDIT_REQUIRED |
| MAT-046 | 05 Communication | Privacy-safe video calls | W10 | AUDIT_REQUIRED |
| MAT-047 | 05 Communication | Call availability/scheduling | W10 | AUDIT_REQUIRED |
| MAT-048 | 05 Communication | Voice notes/media protections | W08 | AUDIT_REQUIRED |
| MAT-049 | 05 Communication | Explicit contact exchange | W08 | AUDIT_REQUIRED |
| MAT-050 | 05 Communication | Block/mute/disconnect | W09 | AUDIT_REQUIRED |
| MAT-051 | 06 Safety/privacy | Phone/email verification | W09 | AUDIT_REQUIRED |
| MAT-052 | 06 Safety/privacy | Selfie/liveness | W09 | AUDIT_REQUIRED |
| MAT-053 | 06 Safety/privacy | Minimized ID checks | W09 | AUDIT_REQUIRED |
| MAT-054 | 06 Safety/privacy | Impersonation/duplicate detection | W09 | AUDIT_REQUIRED |
| MAT-055 | 06 Safety/privacy | Visibility settings | W09 | AUDIT_REQUIRED |
| MAT-056 | 06 Safety/privacy | Blur/reveal consent | W09 | AUDIT_REQUIRED |
| MAT-057 | 06 Safety/privacy | Reporting and abuse cases | W09 | AUDIT_REQUIRED |
| MAT-058 | 06 Safety/privacy | Scam risk flags | W09 | AUDIT_REQUIRED |
| MAT-059 | 06 Safety/privacy | Screenshot/watermark and cached-media policy | W09 | AUDIT_REQUIRED |
| MAT-060 | 06 Safety/privacy | Export/deletion | W09 | AUDIT_REQUIRED |
| MAT-061 | 07 Family/culture | Parent accounts | W12 | AUDIT_REQUIRED |
| MAT-062 | 07 Family/culture | Guardian access boundaries | W12 | AUDIT_REQUIRED |
| MAT-063 | 07 Family/culture | Printable biodata | W12 | AUDIT_REQUIRED |
| MAT-064 | 07 Family/culture | Horoscope uploads | W12 | AUDIT_REQUIRED |
| MAT-065 | 07 Family/culture | Hindi/Telugu and other languages | W12 | AUDIT_REQUIRED |
| MAT-066 | 07 Family/culture | Community taxonomy | W12 | AUDIT_REQUIRED |
| MAT-067 | 07 Family/culture | Family-approved introduction | W12 | AUDIT_REQUIRED |
| MAT-068 | 07 Family/culture | Meeting scheduling | W12 | AUDIT_REQUIRED |
| MAT-069 | 07 Family/culture | Shared shortlist | W12 | AUDIT_REQUIRED |
| MAT-070 | 07 Family/culture | Optional consensual chaperone | W12 | AUDIT_REQUIRED |
| MAT-071 | 08 Revenue | Free vs paid entitlements | W11 | AUDIT_REQUIRED |
| MAT-072 | 08 Revenue | Monthly/quarterly/annual plans | W11 | AUDIT_REQUIRED |
| MAT-073 | 08 Revenue | Play Billing and applicable web payments | W11 | AUDIT_REQUIRED |
| MAT-074 | 08 Revenue | Offers/referrals | W11 | AUDIT_REQUIRED |
| MAT-075 | 08 Revenue | Profile boost | W11 | AUDIT_REQUIRED |
| MAT-076 | 08 Revenue | Verified premium tier | W11 | AUDIT_REQUIRED |
| MAT-077 | 08 Revenue | Renewals/receipts | W11 | AUDIT_REQUIRED |
| MAT-078 | 08 Revenue | Cancellation/refunds | W11 | AUDIT_REQUIRED |
| MAT-079 | 08 Revenue | Restore/revoke/limit enforcement | W11 | AUDIT_REQUIRED |
| MAT-080 | 08 Revenue | Assisted concierge | W11 | AUDIT_REQUIRED |
| MAT-081 | 09 Retention | Push preferences | W08 | AUDIT_REQUIRED |
| MAT-082 | 09 Retention | Email/WhatsApp consent | W12 | AUDIT_REQUIRED |
| MAT-083 | 09 Retention | Match digests | W08 | AUDIT_REQUIRED |
| MAT-084 | 09 Retention | Profile visitor insights | W08 | AUDIT_REQUIRED |
| MAT-085 | 09 Retention | Saved matches | W08 | AUDIT_REQUIRED |
| MAT-086 | 09 Retention | Activity reminders | W08 | AUDIT_REQUIRED |
| MAT-087 | 09 Retention | Reactivation | W08 | AUDIT_REQUIRED |
| MAT-088 | 09 Retention | Married status/success stories | W12 | AUDIT_REQUIRED |
| MAT-089 | 09 Retention | Post-introduction feedback | W08 | AUDIT_REQUIRED |
| MAT-090 | 09 Retention | Alert frequency controls | W08 | AUDIT_REQUIRED |
| MAT-091 | 10 Operations | Moderation console | W13 | AUDIT_REQUIRED |
| MAT-092 | 10 Operations | Review/appeal workflows | W13 | AUDIT_REQUIRED |
| MAT-093 | 10 Operations | Fraud logs and audit | W13 | AUDIT_REQUIRED |
| MAT-094 | 10 Operations | Support tickets | W13 | AUDIT_REQUIRED |
| MAT-095 | 10 Operations | Entitlement administration | W13 | AUDIT_REQUIRED |
| MAT-096 | 10 Operations | Matching analytics | W13 | AUDIT_REQUIRED |
| MAT-097 | 10 Operations | Conversion funnel | W13 | AUDIT_REQUIRED |
| MAT-098 | 10 Operations | Feature flags/experiments | W13 | AUDIT_REQUIRED |
| MAT-099 | 10 Operations | A11y/localization QA | W13 | AUDIT_REQUIRED |
| MAT-100 | 10 Operations | Security/perf/release observability | W13 | AUDIT_REQUIRED |

## 5. Whole-app test/acceptance matrix — mandatory minimum

| Track | Actors / input | Positive expected | Required denial / failure |
|---|---|---|---|
| Identity | New adult, returning, guardian, minor | Correct authority and consent | Underage/duplicate/revoked guardian/invalid OTP |
| Profile | Own/other/blocked | Owner edits; properly public profile displays | Unauthorized private changes or media grants |
| Matching | Two reciprocal profiles / mismatch | Deterministic appropriate match | One-way preference, block, child-status unknown, stale/hidden |
| Messaging | Matched, unmatched, blocked | Real-time authorized thread | Guessable thread ID, revocation race, replay |
| Photos | PUBLIC / ACCEPTED_ONLY / HIDDEN | Authorized retrieval via checked rules | Forwarded URL, expired grant, wrong owner, stale cache |
| Notifications | EN/HI/TE, token old/new, two users | Correct recipient content/route | Cross-account payload, wrong locale, unsubscribed, quiet-hour |
| Calls | Matched + provider; inactive/unmatched | Secure voice/video on two devices if enabled | Unauthorized join, expired token, webhook replay, blocked user |
| Billing | Free/premium, licensed products, RTDN | Server-owned entitlements | Forged client flag, duplicate event, wrong purchase owner |
| Admin | Moderator, support, ordinary member | Limited audited moderation action | Ordinary user promotion, privilege escalation, audit tampering |
| Reliability | Offline/process death/upgrades | Safe resume or informative fail-closed state | Data leak, duplicate billing/message, corrupt Room migration |
| Deletion | Subject, guardian, moderator | User data actions per approved policy | Other-account export, orphaned access, unauthorized retention |
| Accessibility | TalkBack, large text, 3 locales | Navigable, readable and appropriately localized | Unreachable controls, clipped privacy disclaimers, broken focus |

Testing applies per affected device, role, state and relevant locale; record testcase IDs, build SHA, evidence URLs, and whether each result is emulator, automated, or physical.

## 6. Exact 49 prelaunch + 2 postrollout gates — delegation map

**Repository authority:** MatchApp/scripts/ci/production_external_gate.py and MatchApp/docs/release/PRODUCTION_EXTERNAL_EVIDENCE.template.json. The following groupings are assignment aids; do not modify canonical gate keys.

| Accountable lane | Canonical gate field names | Required proof |
|---|---|---|
| Source/governance (W01–04,W19) | sourceMergedAndFrozen; mainBranchProtectionVerified; releaseIdentityVerified | Protected main + merged SHA + approved release identity |
| Links/Firebase (W09,W17) | httpsAppLinksVerified; productionFirebaseConfigured; firebaseRulesIndexesFunctionsDeployed; firebaseSecretsConfigured; firestoreAuthorizationAdversarialPassed; storageAbuseMatrixPassed | Domain association, deployment logs, hostile rules matrix |
| Signing/AppCheck/notifications (W08,W16,W17) | productionSigningVerified; releaseCredentialsVerified; appCheckPlayIntegrityVerified; fcmProductionVerified | Signing hashes, Play Integrity, real devices and push routes |
| Observability/operations (W13,W17,W19) | crashlyticsAlertsVerified; productionMonitoringAlertsVerified; costBudgetAlertsVerified; analyticsProductionVerified; operatorRbacBootstrapVerified; masterDataSeedVerified | Actual alerts, least-privilege operators, seeded production references |
| Play and billing (W11,W17) | playAppSigningVerified; signedReleaseAabVerified; playCatalogConfigured; playServiceAccountApiVerified; playRtdnVerified; billingLicensedMatrixPassed | Signed AAB, licensed transactions, RTDN reconciliation |
| Devices/testing (W14,W15,W16) | authReleaseDeviceMatrixPassed; freshInstallPassed; upgradeInstallPassed; multiDeviceJourneyPassed; poorNetworkRecoveryPassed; physicalDeviceE2EPassed; screenCapturePhysicalMatrixPassed; notificationQuietHoursDeviceMatrixPassed; accountDeletionE2EPassed; accessibilityDeviceMatrixPassed; supportedLocaleQaPassed; performanceSloPassed | Device/video/test logs, OS matrix, signed build fingerprint |
| Legal/security/store (W09,W13,W18,W19) | penetrationTestPassed; legalAndDataSafetyApproved; storeListingAndContentRatingApproved; supportEscalationReady; incidentResponseOwnerVerified; playPreLaunchReportPassed; closedTestingPassed; rollbackDrillPassed | Independent findings, approvals, Play reports, on-call and rollback logs |
| Calls/provider (W10,W17) | secureCallProviderIntegrated; secureCallWebhookSecurityPassed; secureCallPhysicalMatrixPassed; disabledProviderSurfacesVerified | Provider configuration, signed webhook replay-denial, two-device call, safe unavailable/off-flag UI |
| Full release additional two (W19) | controlledRolloutCompleted; postRolloutHealthPassed | Recorded staged rollout and objective postrelease health checks |

No blank/false field can be declared complete by this document. All approvals and records must be release-SHA-bound as required by the gate script.

## 7. Work-item template, PR discipline and sequencing algorithm

For each engineering subtask, create or update a traceable issue with: W-ID/MAT-ID/screen, observed defect or requirement, risk/priority, accountable owner, design and data boundary, proposed API/schema migration, flags, negative and positive acceptance tests, affected source files, branch/PR, reviewer, CI and QA evidence, rollback strategy, and explicit status.

**Execution loop:**
1. Inspect live source and existing tests; document observed behavior vs required behavior, avoid changing compliant code.
2. Write/update failing regression test or a measured defect reproduction.
3. Implement smallest end-to-end fix across UI, repository, Functions/rules/schema and i18n as needed; keep privacy default restrictive.
4. Run fast local tests and full four production CI checks on exact pushed SHA; security owner reviews relevant authorization changes.
5. Confirm all affected screen states and E2E journeys. If manual/provider/device proof is missing, mark BLOCKED_EXTERNAL / CI_VERIFIED, not DONE.
6. Obtain review, merge to protected main, verify post-merge SHA; update tracker/evidence ledger and close only the demonstrated issue.
7. Repeat in dependency order and carry unresolved blockers forward with named owner, date and reproducible next action.

**Work-in-progress limits:** keep one canonical integration PR at a time for the core line; run a separate branch/PR per independent defect domain. Do not open duplicate broad "fix everything" branches or close overlapping historical PRs without review. Recheck any snapshots in this blueprint against live GitHub before editing; it is a dated plan, not a live data feed.

## 8. Definition of DONE and final release decision

- **A workstream DONE:** all scoped tasks implemented or explicitly accepted as approved optional scope; reviewed, merged, exact-main CI, tests/QA evidence and correct dependent gates.
- **A feature IMPLEMENTED_VERIFIED:** code + rules/schema/route + tests + current merged SHA + recorded owner + CI URL + physical/device acceptance when applicable + security review + qaSignoff + verification date; evidence validator passes. Optional scope deferral is not implementation.
- **A screen DONE:** correct approved design, navigation, allowed roles, relevant view states, a11y and locale matrix, real build evidence, reviewer sign-off, no blocking defects.
- **Engineering sign-off:** W01–W15 delivered to agreed launch scope, W16 acceptance evidence complete, no P0/P1 issues, 4/4 CI on protected merged main, independent reviews closed.
- **Production GO:** engineering and operator/security/legal sign-offs on same signed release candidate, all 49 prelaunch fields genuine and validator PASS, authorized written go.
- **Full release sign-off:** controlled rollout and postrollout health evidence, then full gate PASS and issue #26 closure by release owner. A chat-deletion handoff alone never means production GO.

## 9. Immediate next actions (not new planning tasks)

- **First developer action:** execute actual parity-review of PR #60 vs #43/#50/#51/#53/#56; carry forward exact missing behavior as reviewed fixes.
- **First repository owner action:** verify main branch protection and recruit independent Android + Firebase/security reviewers for #60. Give actual approvals, not blanket permission.
- **Next developer action:** review/merge #60, then rebase/review/merge #61; confirm exact post-merge main checks.
- **Parallel engineering actions:** inventory 100 MAT rows and 57 screens, identify P0 launch gaps, execute fix/test PRs. Focus on sensitive photo/guardian/rules/call/billing paths first.
- **Provider/release owner action:** choose calling-provider launch policy and test authority, Play/Firebase project IDs and delegated workflows without posting credentials.
- **Last operator action:** complete signed physical E2E, 49+2 genuine release gates, controlled rollout and issue #26 closure.

## 10. Companion source documents and permanent links

- PR #60: https://github.com/Govindhahari118/Match_clone/pull/60
- PR #61: https://github.com/Govindhahari118/Match_clone/pull/61
- Release issue #26: https://github.com/Govindhahari118/Match_clone/issues/26
- docs/production-readiness/MATREE_FINAL_CHAT_INDEPENDENT_HANDOFF_2026-10-11.md
- docs/production-readiness/MATREE_FINAL_PRODUCTION_MASTER_PLAN_2026-10-11.md
- docs/production-readiness/MATREE_100_FEATURE_E2E_DELIVERY_2026-10-10.md
- docs/production-readiness/MATREE_PR_CONSOLIDATION_AUDIT_2026-10-11.md
- docs/production-readiness/matree-feature-evidence.json
- docs/production-readiness/screen-classification.json
- MatchApp/scripts/ci/matree_feature_evidence_scan.py
- MatchApp/scripts/ci/production_external_gate.py
- MatchApp/docs/release/PRODUCTION_EXTERNAL_EVIDENCE.template.json

**Audit contract:** The state of GitHub changes after this document is committed. Refresh every PR head, CI outcome, independent approval, feature ledger, device and release evidence before writing a new status or signing off. No implementation status changes are inferred merely by creating this blueprint.
