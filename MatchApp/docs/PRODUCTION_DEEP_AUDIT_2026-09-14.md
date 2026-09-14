# MatrimonyConnect — Deep Production Audit

**Audit date:** 2026-09-14  
**Audited branch:** `Gpt_matree`  
**Goal:** a smaller, truthful, secure and testable Android production surface. Source files that exist in the repository are **not** considered production-ready features unless they are reachable, backed by real data, authorized server-side and covered by release validation.

## Read this first

Production readiness is a gate, not a percentage. The application can move to release only after the exact candidate commit passes CI and the external production configuration/real-device matrix in `DEPLOYMENT_CHECKLIST.md` is complete.

The current production architecture remains intentionally conservative:

- Android: Kotlin + Jetpack Compose
- Local cache: Room + DataStore
- Identity: Firebase Auth UID is the network identity
- Backend: Firestore + Cloud Functions + Firebase Storage
- Push/observability: FCM + Crashlytics/Analytics where configured
- Payments: server-authoritative Razorpay integration
- No pre-launch PostgreSQL, Redis, queue or microservice migration

## Severity model

| Severity | Meaning |
|---|---|
| P0 | Can create security, data-loss, payment, privacy, launch-policy or core-functional failure. Must be resolved before release. |
| P1 | Material reliability/UX/operability problem. Resolve before broad production rollout. |
| P2 | Improvement that can follow once the core release is stable. |

## Current production surface

The audited shell intentionally exposes a smaller feature set than the source tree contains.

| Journey | Status | Notes |
|---|---|---|
| Email/Google authentication | READY WITH PROD CONFIG | Firebase Auth is authoritative; local password fallback was removed. Release OAuth credentials still require production console validation. |
| Profile/onboarding/edit | READY WITH E2E TEST | Server-owned fields are protected; private account/contact data is separated from public profile data. |
| Profile photos | READY WITH E2E TEST | Firebase UID paths are used. Must validate upload/delete/primary selection on two devices. |
| Discovery/filters | READY WITH E2E TEST | Production repository hydrates profiles from trusted server discovery; seed profiles are excluded from production recommendations. |
| Interests/mutual matches | READY WITH E2E TEST | Firestore state is authoritative and active inbox/home state is being moved to cross-device streams. |
| Shortlist | READY WITH E2E TEST | Firestore state is authoritative; shortlist screen uses the remote stream. |
| Chat text/image/voice | READY WITH E2E TEST | Authenticated mutual-match flow, durable media upload/download and retry paths exist; two-device/process-death validation remains required. |
| Blocking/reporting | READY WITH E2E TEST | Security-sensitive paths must continue to enforce either-direction blocks server-side. |
| Verification | READY WITH OPERATOR CONFIG | Approval/rejection requires trusted admin claim. A real admin provisioning/review process is still external work. |
| Membership/payment | READY WITH LIVE-CONFIG TEST | Server creates/verifies orders and activates idempotently. Live credentials/webhook/recovery testing remains external. |
| Notifications | P1 VALIDATION | Token lifecycle and production push/deep-navigation matrix still need physical-device validation. |
| Privacy/account deletion | READY WITH E2E TEST | Server-confirmed erasure and local wipe are implemented. Retention/legal validation remains required. |
| Nearby/location | READY WITH E2E TEST | Real foreground location, coarse/fine permission, server-only coordinates, radius filtering, blocks/stealth and stop-sharing are implemented. |
| Language selector | SAFE REDUCED SURFACE | Only independently present production language assets are exposed. Placeholder packs stay hidden. |
| Experimental source-tree screens | POST-LAUNCH / HIDDEN | Existence in source does not make a feature production-supported. |

## Completed P0 hardening

### Build/runtime baseline

- Android compiles/targets API 36.
- Java/Kotlin target is JVM 17.
- Cloud Functions runtime is Node 22.
- Release minification/resource shrinking is enabled.
- Production CI builds/tests Android, Functions and Firebase rules.
- Retired Firebase Dynamic Links is not a production dependency/path.

### Authentication

- Firebase Authentication is the password authority.
- Room does not grant offline password authentication.
- Deleted/disabled server accounts must not regain access from local cache.
- FCM token cleanup is attempted before sign-out while the user is still authenticated.

### Firestore/profile security

- Client cannot self-grant premium or verification.
- Billing/verification fields are server-owned.
- Private contact/account fields use owner-only documents.
- Direct profile visibility respects blocking and stealth rules.
- Rules have emulator regression coverage.

### Payments

- Plan and amount are server-owned.
- Razorpay order creation occurs on the backend.
- Signature is checked server-side.
- Razorpay order/payment are fetched authoritatively and amount/currency/status/ownership are validated.
- Activation is idempotent and payment IDs cannot be reused for another order/account.

### Storage/chat media

- Firebase UID is used for remote storage identity.
- Chat media is uploaded to durable cloud storage rather than sharing a sender-local URI.
- Message retries use stable client message IDs/outbox state.
- Storage rules constrain content type/size/ownership.

### Location/Nearby

The previous fake/approximate Nearby implementation was replaced.

- No background-location permission.
- Location is requested only when Nearby is used.
- Coarse location works; precise location is optional.
- A fresh foreground location is submitted through an authenticated/App-Check-aware callable.
- Exact coordinates live in server-only `userLocations/{uid}`.
- Client rules deny reading/writing those coordinates.
- Backend searches geohash cells then applies Haversine distance.
- Stale location (>30 days), self, blocked users, reverse blockers, stealth profiles and incompatible gender preferences are excluded.
- Client receives distance, never another user's coordinates.
- User can stop sharing immediately.
- Location is deleted with the user profile.

## Findings discovered/reconfirmed in this audit

### P0/P1 — cross-device social state

Historical screens were written around Room flows, which can be stale on a fresh second device even when Firestore writes are correct.

Actions taken:

- `SocialRepository` now makes Firestore authoritative for like/unlike/isLiked/block/unblock and exposes remote received/sent/mutual flows.
- Remote UIDs are hydrated into the Room presentation cache without inventing real accounts.
- `InterestsScreen` now uses the server-backed flows.
- `HomeViewModel` uses server-backed received-interest and mutual-match counts.
- `ShortlistRepository` now makes Firestore authoritative.
- `ShortlistScreen` uses the server-backed saved-profile stream.
- A dead/no-op shortlist chat button was removed rather than leaving a fake interaction.

Remaining validation: two signed-in devices must be used to test transitions and recovery after process death/offline periods.

### P1 — incoming-interest “decline” semantics

The old Received Interests UI called `unlike(me, sender)`, which does **not** delete the sender's incoming interest and therefore presented a false action. That control was removed from the production screen. Users can accept the interest or open the profile and use real block/report controls. A future non-blocking “dismiss request” feature should use its own server-side state rather than pretending to delete another user's document.

### P1 — demo/seed utilities remain in source

`SeedProvider` and old local password helper code remain present as source utilities, but production recommendations explicitly exclude seed users and the root/application startup path audited here does not invoke `SeedProvider`.

Before final release:

- confirm no debug/test-only caller is required in the release source set;
- delete or move seed/password utilities to debug/test sources;
- remove the obsolete BCrypt dependency if no supported code uses it.

Do not ship demo credentials or fake social activity as production data.

### P1 — language overclaim

The repository contained many nominal locale JSON files that were duplicated placeholders. The production selector now exposes only the real language packs currently intended for QA and hides placeholder locales until translated/layout-tested.

### P1 — external operational controls

Code cannot finish these owner-controlled items:

- production Firebase project and release `google-services.json`;
- release Google Sign-In fingerprints/authorized domains;
- Play Integrity/App Check registration and staged enforcement;
- Razorpay live secret/key/webhook configuration;
- release keystore/Play App Signing;
- production support/moderation/admin-claim workflow;
- Play Console Data Safety/content rating/privacy/account-deletion declarations;
- closed testing and staged rollout;
- branch protection/canonical history decision.

## Stale/experimental feature policy

The source tree contains many concepts such as AI insights, analytics dashboards, assisted service, background check, circles/community, counselling, events, family modules, nearby variants, NRI modules, referral, rewards, secure calling, video, wedding planning and other experiments.

Release rule:

1. A source file alone is **not** a product promise.
2. A feature becomes `READY` only when it has a reachable production route, real repository/backend state, authorization/privacy enforcement, error/loading/empty states, analytics/operability where needed, and E2E tests.
3. Incomplete or mocked features stay hidden from production navigation.
4. Do not add infrastructure merely to make a prototype screen appear functional.

## Warning/error policy

A candidate release must have:

- zero compile errors;
- zero failing JVM tests;
- zero Firebase Functions lint/build failures;
- zero Firebase rules-test failures;
- zero Android lint errors;
- a successful minified release bundle/R8 validation;
- no known security/payment/privacy P0;
- no misleading dead buttons or fake-success flows on the production surface.

Warnings are triaged, not ignored wholesale. Deprecation warnings are upgraded deliberately only after compatibility/regression checks; Firebase/Google-managed certificate pinning and hard root blocking are intentionally not used as launch gates because they create brittle failure modes without replacing backend authorization/App Check.

## Final implementation order from this audit

1. Keep CI green on the exact latest commit after every hardening batch.
2. Finish cross-device social/shortlist/home/chat state validation.
3. Remove/move seed/demo/password leftovers once references are proven absent.
4. Exercise Nearby on physical devices with approximate/precise/denied/service-off scenarios.
5. Exercise chat text/image/voice on two devices, including retry/process death/block mid-thread.
6. Exercise payment recovery/idempotency with Razorpay test mode, then controlled live smoke test.
7. Exercise account deletion and all privacy visibility combinations.
8. Validate Room migrations from every released schema.
9. Complete production Firebase/App Check/payment/signing/legal/Play setup.
10. Internal → closed test → staged rollout only with a green exact release commit.

## Release decision

Do **not** label the app “production-ready” until the external production configuration and physical-device matrix are complete. The codebase should instead be treated as a hardened release candidate whose remaining gates are explicit and testable.
