# Matree production-hardening continuation handoff

This file is the continuity source for the production-hardening work previously tracked in ChatGPT.
The prior chat is not required if this document, the branch, PR #22, and the release documentation remain available.

## 1. Repository coordinates

- Repository: `Govindhahari118/Match_clone`
- Hardening branch: `gpt/matree-master-plan-hardening-20260929`
- Pull request: #22 — Production master-plan hardening: trust, consent, verification, release gates
- Base branch: `main`
- Do not replace or delete canonical `main`.
- Use the **latest branch head** as the candidate. A historical green SHA never certifies a newer commit.

Validated baseline immediately before this handoff document was added:

- SHA: `c2bcdf854ffdeab3aadea4ee045cae580049be82`
- Production CI run: `36674680513`
- Result: all mandatory jobs passed.

Because this document itself creates a newer commit, the next operator/assistant must verify Production CI again on the
new exact head before calling the repository candidate green.

## 2. Product/release objective

Ship Matree as a production/user/launch-ready matrimonial Android app without pretending that source code proves
external deployment, provider, legal, device, Play Console, or infrastructure evidence.

Definition:

- **Repository GO**: all repository-controlled launch-scope implementation is complete, unsafe/incomplete routes are
  hidden or fail-closed, no known exposed repository-controlled P0/P1 remains, and all mandatory Production CI jobs
  are green on the exact branch head.
- **Production GO**: Repository GO **plus** the exact-SHA external evidence gate passes for production Firebase,
  signing, App Check, licensed billing, provider flows, physical-device E2E, accessibility, performance, security,
  legal/Data Safety, Play pre-launch, rollback, controlled rollout and post-rollout health.

Never convert missing external evidence into a fake checked box.

## 3. Architecture rules that must not regress

1. Firebase Auth / Firestore / Storage / Cloud Functions are server authority.
2. Room/DataStore are cache or local preferences, never authority for premium, verification, trust/risk, moderation,
   block, deletion, protected media, account enforcement, or payment.
3. Google Play is the digital entitlement authority. Do not restore Razorpay/client-authored premium.
4. Raw KYC, exact location, risk signals, FCM tokens and operator-only data must remain client-unreadable.
5. Protected profile media is published by backend/moderation authority. Tokenized download URLs are not authority.
6. Blocking/privacy/account-state checks must be enforced on trusted backend/rules, not UI only.
7. Profile readiness controls discovery/peer-profile eligibility. Account lifecycle status controls whether an account
   is active. Do not reuse profile-completeness as the generic established-relationship lifecycle predicate.
8. Optional/provider-dependent features default OFF and fail closed.
9. Debug App Check may use the debug provider; release must use Play Integrity.
10. Do not claim E2E encryption. Local encryption and protected transport/storage are not end-to-end encryption.
11. Never commit release Firebase config, provider secrets, keystores or passwords.
12. Any new commit invalidates prior exact-SHA CI/release evidence until CI passes again.

## 4. Repository-controlled implementation already completed

Do not reopen these areas unless CI, testing, or a concrete audit exposes a defect:

- Phone OTP, email/password and Google authentication paths.
- Session/account-switch cleanup, FCM registration/revocation, signed-in device visibility and sign-out-all-devices.
- Profile onboarding/wizard, profile revision handling and backend profile-readiness authority.
- Server-authorized discovery, pagination, privacy/block/stealth filtering and reciprocal gender/preference checks.
- Partner-preference authority and explainable versioned matching.
- Interests, decline/withdraw, mutual-match state and shortlist consistency.
- Server-authoritative chat creation/message send, text/image/voice media binding, retry/outbox and constrained receipts.
- Notification persistence, preferences, FCM lifecycle and server-owned notification authority.
- Privacy dashboard, contact visibility, hidden relationships, pause/deletion lifecycle and exact-location cleanup.
- Trust summary and reviewed risk architecture; payment does not increase Trust.
- Private fraud/risk review signals including volume, money/link indicators, duplicate-photo and copied-bio signals.
- Government-ID verification architecture, consent ledger and provider-gated advanced verification.
- Protected photo moderation, approval/rejection, exact-object publication and immutable profile media.
- Protected video moderation/publication backend; user exposure remains feature-flagged/off by default.
- Account ACTIVE / UNDER_REVIEW / RESTRICTED / SUSPENDED enforcement with audit and token revocation.
- Support, appeal/moderation queues, operator audit trail, request correlation and staged privileged MFA enforcement.
- Google Play purchase verification, entitlement capability model, refund/void/expiry reconciliation and idempotency.
- Nearby exact-coordinate privacy architecture and server-derived results; feature remains OFF by default.
- Kundali and NRI optional surfaces remain OFF by default pending their evidence gates.
- Health liveness/readiness endpoints.
- Firestore and Storage rules hardening.
- Room registered migration matrix and emulator-backed migration CI.
- Production truthfulness, screen-classification, callable-contract, release-candidate, handoff-package and external-gate
  CI scans.
- Environment, secrets, API, database-schema, feature-flag, known-issues, GO/NO-GO, rollback and Play handoff docs.

## 5. Launch route policy

Treat `docs/production-readiness/screen-classification.json` and
`docs/production-readiness/route-inventory.md` as source-controlled release policy.

Important OFF-by-default/BETA capabilities include:

- Nearby — `enable_nearby=false`
- Kundali — `enable_kundali=false`
- NRI promoted surface — `enable_nri_features=false`
- Profile video — `show_video_profiles=false`

Do not expose STUB / UNSAFE / POST_LAUNCH screens merely because Kotlin source exists. Promotion requires complete
authority, persistence, failure handling, tests, telemetry and provider/operator evidence.

## 6. Mandatory Production CI

Every current candidate SHA must pass all four jobs in `.github/workflows/production-ci.yml`:

1. Android build, lint and tests
   - production truthfulness
   - screen classification
   - Android/Functions callable contract
   - release-candidate scan
   - production handoff package
   - external-gate validator
   - unit tests
   - lint
   - debug build
   - non-production release/R8 validation
   - exact-SHA evidence
2. Room migration matrix
3. Firestore and Storage emulator security rules
4. Firebase Functions lint, build, tests and production dependency audit

If one fails, inspect the failed job/log and fix the underlying contract. Do not weaken a security rule just to make a
legacy test pass; update stale fixtures only when the product contract intentionally changed.

## 7. Exact next execution plan

### Phase A — Freeze repository candidate

1. Check latest head of `gpt/matree-master-plan-hardening-20260929`.
2. Verify all four Production CI jobs pass on that exact head.
3. Review PR #22 changed files for accidental debug/test/secrets leakage.
4. Confirm no new reachable screen violates screen classification.
5. Confirm no repository-controlled P0/P1 is knowingly exposed.
6. From this point, freeze feature development; accept only release-blocking fixes.

Exit criterion: **Repository GO**.

### Phase B — Production Firebase and credentials

Release/operator work:

1. Create/use separate production Firebase project.
2. Register package `com.match.app` and supply production `google-services.json` outside source control.
3. Enable only Auth providers actually exposed: Phone, Email/Password and Google where applicable.
4. Configure authorized domains and release SHA-1/SHA-256.
5. Create/deploy Firestore, indexes, Storage rules and Node 22 Functions.
6. Enable/configure FCM, Crashlytics, Analytics and Remote Config.
7. Configure budgets/alerts.
8. Record evidence references against the exact candidate SHA.

Exit criterion: production Firebase deployment evidence attached.

### Phase C — App Check / Play Integrity

1. Register release app with Play Integrity/App Check.
2. Validate legitimate release traffic first.
3. Stage callable enforcement.
4. Enforce supported Firebase products only after successful observation.
5. Verify legitimate testers are not locked out.
6. Attach evidence.

Exit criterion: App Check/Play Integrity gate passed.

### Phase D — Signing and Play Billing

1. Configure Play App Signing / protected release signing.
2. Set final `versionCode` and intended `versionName`.
3. Configure real Google Play product IDs and Play Developer API/service account access.
4. Licensed-tester matrix:
   success, cancellation, pending, process death, duplicate/replay, wrong user/token, restore, refund and void.
5. Verify acknowledgement/reconciliation/expiry behavior.
6. Match Play pricing/refund copy to shipped entitlement behavior.

Exit criterion: signed candidate and licensed billing evidence passed.

### Phase E — Verification and operator operations

1. Configure real verification/KYC provider credentials and production policy.
2. Provision moderator/ops roles with least privilege.
3. Enforce operator MFA according to staged policy.
4. Deploy hardened operator Hosting surface.
5. E2E test:
   verification submit/review/approve/reject,
   photo moderation,
   report enforcement,
   restriction/suspension/restore,
   appeal/support,
   payment reconciliation.
6. Verify audit evidence and request IDs.

Exit criterion: provider/operator evidence passed.

### Phase F — Physical-device E2E

Use at least two real release devices, including a lower-end supported device and a current Android device.

Test:

- phone OTP, email reset/login and Google where exposed;
- fresh install, upgrade, offline/online recovery and process death;
- onboarding/profile/edit/preferences;
- discovery/filter/pagination/no-results;
- interests/mutual matches/shortlists across devices;
- block mid-flow and privacy/stealth behavior;
- chat text/image/voice/read/retry/process death;
- FCM foreground/background/killed-process delivery and deep navigation;
- photo upload → moderation → publication;
- sign-out and sign-out-all-devices;
- suspended/restricted/deleted account cannot recover via stale local state/deep link;
- permanent deletion and exact Nearby-location cleanup.

Keep Nearby, Kundali, NRI promotion and Video OFF unless their separate device/provider evidence is completed.

Exit criterion: physical-device E2E matrix passed.

### Phase G — Performance, accessibility and security

1. Validate performance against `docs/production-readiness/performance-gate.md`.
2. Exercise realistic discovery volume and committed production indexes.
3. Test lower-end-device cold start, discovery scrolling, image memory, chat and process death.
4. Review Crash/ANR behavior.
5. Accessibility: TalkBack, font scaling, contrast, touch targets, keyboard/input.
6. Complete penetration/security review.
7. Fix every P0/P1. Any code fix creates a new candidate SHA and requires full Production CI again.

Exit criterion: performance, accessibility and penetration-test evidence passed.

### Phase H — Legal and Play compliance

Release/legal owner must finalize against the actual deployment:

- Privacy Policy and controller/contact/rights/retention language;
- Terms;
- Community/Safety rules;
- Refund policy;
- account-deletion declaration;
- Data Safety form;
- content rating and target audience;
- stable production HTTPS URLs;
- final product/legal copy review.

Exit criterion: legal/Data Safety evidence passed.

### Phase I — Pre-launch and production promotion

1. Build/sign final AAB from the exact frozen candidate.
2. Run Play pre-launch report and resolve release-blocking issues.
3. Execute production-like rollback drill.
4. Populate an operator-controlled copy of
   `docs/release/PRODUCTION_EXTERNAL_EVIDENCE.template.json`.
5. Run:
   `python3 scripts/ci/production_external_gate.py --evidence <file> --sha <exact-sha> --mode prelaunch`
6. Do not promote unless it passes.

Exit criterion: **Production prelaunch GO**.

### Phase J — Controlled rollout

Suggested release progression:

internal/closed testing → 5% → 20% → 50% → 100%.

At each stage monitor:

- Crashlytics / ANRs;
- Functions errors/latency;
- Auth failures;
- payment verification/reconciliation failures;
- notification failures;
- Firestore/Storage/Functions cost;
- moderation/support operational load.

Have rollback criteria and owner identified before each increase.

After stable rollout, attach controlled-rollout and post-rollout-health evidence and run:

`python3 scripts/ci/production_external_gate.py --evidence <file> --sha <exact-sha> --mode full`

Exit criterion: **Full Production GO**.

## 8. Final done definition

Matree is 100% complete for the agreed launch only when all are true:

- exact-head Production CI fully green;
- no known exposed repository P0/P1;
- production Firebase deployed;
- release signing verified;
- App Check/Play Integrity verified;
- real Auth device matrix passed;
- licensed Play Billing matrix passed;
- provider/KYC flow passed where exposed;
- operator roles/MFA/console passed;
- physical-device E2E passed;
- accessibility passed;
- performance SLO passed;
- penetration/security review passed;
- legal/Data Safety approved and published;
- Play pre-launch report passed;
- rollback drill passed;
- external prelaunch gate passed;
- controlled rollout completed;
- post-rollout health passed;
- external full gate passed.

Anything else is a lower readiness state and must be described truthfully.

## 9. Do not spend time on these unless deliberately added to launch scope

Do not attempt to reach “10/10” by enabling every source screen. Hidden/post-launch/unsafe surfaces should stay hidden
unless there is a real product requirement and a complete authority/provider/test contract.

Repository quality should be judged on the launch surface, not on the existence of dormant prototypes.

## 10. New-chat bootstrap prompt

If work continues in a new ChatGPT conversation, use:

> Continue Matree production hardening from repository `Govindhahari118/Match_clone`, branch
> `gpt/matree-master-plan-hardening-20260929`, PR #22. Read
> `MatchApp/docs/release/CONTINUATION_HANDOFF.md`,
> `MatchApp/DEPLOYMENT_CHECKLIST.md`,
> `MatchApp/docs/release/GO_NO_GO.md`,
> `docs/production-readiness/route-inventory.md`, and
> `docs/production-readiness/screen-classification.json` first. Check the latest branch head and exact-head
> Production CI before changing code. Continue autonomously from the first incomplete gate. Do not claim external
> production evidence without real proof, do not re-enable hidden/BETA features without their gates, and do not
> weaken security/backend authority to satisfy stale tests.

