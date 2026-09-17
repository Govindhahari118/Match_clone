# Matree Production 10/10 Execution Plan

This document is the release contract for Matree. A feature is not rated 10/10 because the UI exists or because one CI run passed. It reaches 10/10 only when the implementation, security model, automated tests, device behavior, external production configuration, and truthful UX are all complete for that feature.

## 1. Definition of 10/10

A feature may be marked **10/10** only when all applicable gates are satisfied:

1. **Functional completeness** — every visible action has a real implementation, including loading, success, empty, offline/retry and failure states.
2. **Authoritative state** — identity, privacy, relationships, payments, entitlements, verification and destructive operations are enforced by Firebase/Auth/Functions/Rules, not trusted to the Android client.
3. **Security and privacy** — direct-client bypasses are denied; block/hide/privacy changes take effect on reads as well as writes; sensitive fields are not leaked through public documents, logs, notifications or long-lived media URLs.
4. **Cross-device correctness** — server-owned state survives reinstall/account switch and is visible consistently on a second authenticated device.
5. **Automated validation** — Android unit tests, lint, debug assembly, release AAB/R8, Functions TypeScript/tests, Firestore Rules emulator tests and Storage Rules tests are green on the exact PR head.
6. **Regression protection** — the defect that motivated the change has a test wherever an automated test is technically practical.
7. **Truthful UX** — no fake success, placeholder CTA, unsupported marketing statistic, fabricated profile default, misleading premium state or claim such as true E2EE without a real protocol.
8. **Accessibility** — meaningful controls have labels, touch targets and readable states; the feature remains usable with TalkBack, larger font scale and dark/light themes where applicable.
9. **Performance/reliability** — no obvious main-thread I/O, unbounded listener, retry storm, excessive query fan-out or uncontrolled media payload; failure/retry behavior is deterministic.
10. **External production evidence** — any Play Console, Firebase console, domain/App Links, legal, billing, real SMS, Play Integrity, signing or physical-device requirement is verified instead of assumed.

If an external or physical-device gate is still open, the code portion can be complete but the feature does **not** receive a final 10/10 production rating.

## 2. Scoring rubric

For every feature, post-implementation scoring uses the same rubric:

- Functional completeness: 2.0
- Backend/data authority: 1.5
- Security/privacy: 1.5
- Automated tests/CI: 1.5
- UX truthfulness/accessibility: 1.0
- Cross-device/offline/retry reliability: 1.0
- Performance/observability: 0.5
- Real-device/external production evidence: 1.0

Total: 10.0.

## 3. Release invariants

These are non-negotiable across the app:

- Firebase UID is the remote identity authority. Local Room IDs are cache/navigation details only.
- No Android client may mint premium, verification, match, block, username, Matrimony ID, payment or KYC authority by directly writing trusted fields.
- No new protected profile media may persist a tokenized Firebase download URL.
- A block must revoke relationship/profile/chat/media access immediately in both directions where product semantics require it.
- A mutual match cannot be silently removed by a pending-interest withdrawal action.
- Paid entitlements must be server-authoritative and replay/idempotency safe.
- Account deletion must be restartable and must not delete Firebase Auth until required cleanup has completed.
- Missing demographic or compatibility data must never be fabricated to improve UX or scores.
- Notifications must not disclose sensitive content on the lock screen and must not cross accounts after token/account switches.
- Release-visible controls must be READY, PREMIUM, BETA or intentionally hidden. STUB/UNSAFE controls do not ship.

## 4. Execution waves

### Wave 0 — Repository and release governance

**Goal:** make every later change reproducible and merge-safe.

Implementation:
- Keep production work on isolated branches/PRs.
- Require the full Production CI workflow for merge.
- Prevent force updates to `Gpt_matree` once repository administration is available.
- Configure required status checks and review protection in GitHub.
- Keep release evidence tied to an exact commit SHA.
- Remove/supersede stale PRs instead of force-merging them after production moves.

10/10 acceptance:
- Protected production branch.
- Required checks enforced by GitHub.
- No unresolved release-blocking review thread.
- Final release SHA has a complete green Production CI run.

### Wave 1 — Identity, relationship and privacy authority

#### Matrimony ID
- Replace regional/short random IDs with nationwide-neutral IDs.
- Generate with cryptographic randomness.
- Reserve transactionally in a server-only global registry.
- Make assignment idempotent across trigger retries.
- Release registry ownership during restartable deletion.
- Deny client registry reads/writes.
- Do not silently migrate historical public IDs without a migration product decision.

Acceptance:
- Concurrent account creation cannot duplicate an ID.
- Trigger retry returns the same assignment.
- Client cannot forge or enumerate registry documents.
- Deletion releases only the caller-owned reservation.

#### Blocking
- Mutate block/unblock only through trusted callable Functions.
- Block and relationship cleanup are atomic.
- Remove both-direction interests, mutual match, response state and shortlist state as appropriate.
- Deny direct block-document writes.
- Make profile, relationship, chat, message and protected-media reads block-aware.
- Preserve evidence/report records according to retention rules.

Acceptance:
- Modified clients cannot create/remove trusted block state directly.
- Existing open chat/message/media access terminates after block.
- Unblock does not silently restore deleted relationship state.
- Emulator regressions cover before/after block reads and writes.

#### Interests and matches
- Server-authoritative daily quota.
- Mutual preference/privacy checks on server.
- Pending withdrawal rejected when relationship is already mutual.
- Decline state is explicit and auditable.
- Stable deterministic match identifiers.

Acceptance:
- Client clock, local premium flag or patched APK cannot bypass quota/eligibility.
- Repeated requests are idempotent.

#### Contact sharing
- Private email/phone remain outside public profile documents.
- Reveal is permission-checked and selected-person grants cannot be forged.
- Grants revoke correctly on block/hide/account deletion.

### Wave 2 — Protected profile media and video

#### Photos
- New uploads return `gs://` object identity rather than Firebase download-token URL.
- Render protected `gs://` through authenticated Firebase Storage SDK.
- Re-authorize cached protected media before reuse.
- Ensure Coil memory/disk behavior cannot bypass that authorization.
- Preserve legacy HTTPS/local media during migration.
- Add one-time migration/revocation plan for historical tokenized URLs.

Acceptance:
- New upload never calls `downloadUrl` for protected profile photos.
- A previously viewed protected photo stops rendering after permission is revoked.
- Legacy data still renders until migration.
- Storage rules reject unauthorized viewer.

#### Video Profile
- Use Firebase UID in Storage path.
- Enforce payload size/type.
- Publish `videoUrl` to authoritative Firestore profile and cache it in Room.
- No local-URI fake-success fallback.
- Protected playback uses authenticated resolution.
- Removal deletes Storage object and authoritative profile field with visible failure state.
- Remove no-op record CTA unless a complete capture flow exists.
- Remove unsupported response-multiplier claims.

Acceptance:
- Uploaded video appears on a second device/account session according to privacy rules.
- Failed upload never displays "saved".
- Remove failure preserves truthful state.

### Wave 3 — Release-visible UI truthfulness and stale-feature elimination

Repository-wide audit terms:
- `TODO`
- `future enhancement`
- `coming soon`
- empty/no-op `onClick`
- hard-coded fake success
- unsupported `2x/3x` claims
- fake verified/premium states
- local-only writes presented as cross-device success
- placeholder contact/payment/KYC actions

For every route classify it as:
- READY
- PREMIUM
- BETA
- STUB
- UNSAFE
- POST-LAUNCH

Rules:
- READY must work end-to-end.
- PREMIUM must have a real entitlement gate and restore behavior.
- BETA must be explicitly labeled and safe.
- STUB/UNSAFE/POST-LAUNCH must not expose a misleading active CTA.

#### Biodata
- Remove/wire top-bar dead Share action.
- "PDF Saved" may only be shown if the file was actually persisted to user-visible storage; cache/FileProvider generation should say "PDF Ready" or "Generated".
- Generic chooser must not be labeled as email-only unless an email intent is actually constrained.
- Remove unsupported "3x responses" verification claim.
- Preserve real PDF export/share behavior.
- Handle profile null/export failure/startActivity failure.

### Wave 4 — Profile, onboarding, religion, search, discovery and matching

#### Profile/onboarding
- Never fabricate religion, language, profession, education, marital status, height or other identity/profile values.
- Canonical religion is distinct from discovery/theme preferences.
- Religion-specific fields appear only when applicable.
- Server-owned confirmation/lock metadata cannot be client forged.
- Completion percentage only penalizes applicable fields.

#### Search
- Exact username/Matrimony-ID and broad name/profession/city paths are deterministic.
- Pagination is bounded.
- Private/blocked/hidden/incompatible profiles are excluded server-side or before exposure.
- Required indexes are documented/deployed.

#### Matching
- Mutual `lookingFor` compatibility.
- Block/hide/stealth rules applied.
- Missing data never receives synthetic neutral compatibility values.
- Compatibility scores expose coverage/confidence and avoid unsupported physical scoring.

#### Nearby
- Foreground-only location unless a documented feature requires background access.
- Never expose exact coordinates to another client.
- Exclude self/blocked/reverse-blocked/hidden/incompatible/stale-location profiles.
- Explicit stop-sharing deletion/expiry.
- Bounded geospatial reads and dense-city cost/load test.

### Wave 5 — Chat, notifications and activity

#### Chat
- Firebase UID authoritative participants.
- Stable deterministic thread/message identifiers.
- Durable image/voice media paths.
- WorkManager retry/outbox states for transient failure.
- Retry must not duplicate sent message.
- Read receipts only recipient-authorized.
- Block immediately revokes read/send/media access.
- Do not claim E2EE unless a real cryptographic E2EE protocol is implemented.

#### Notifications
- Every push includes recipient identity where account-switch isolation is required.
- Client drops a push tagged for another currently authenticated account before hydration/display/persist.
- Generic lock-screen copy; sensitive message/profile content only after authenticated in-app fetch.
- Remove invalid/unregistered FCM tokens server-side after send failures.
- Test foreground/background/killed/account-switch and notification-permission-denied behavior.

#### Activity/presence
- Precise activity timestamp remains private/server-owned.
- User visibility preference gates what another member can infer.
- Inactivity automation reads private server data rather than public timestamps.

### Wave 6 — Verification, KYC and abuse prevention

- Verification result/admin flags server-owned.
- One-person/one-profile policy enforced by approved KYC/liveness/duplicate-identity process before claiming it publicly.
- KYC identifiers stored minimally; hash/tokenize where appropriate.
- Report/block/support evidence is retained according to policy.
- Ban identity policy is explicit and auditable.
- Admin role claims are custom-claim/server-controlled, never public-profile booleans.

10/10 requires live-provider sandbox/production workflow evidence, retry/error handling and documented retention/deletion behavior.

### Wave 7 — Payments, Play Billing and premium entitlement

#### Direct/server payments
- Server creates authoritative amount/plan/order.
- Signature/webhook verification.
- Idempotent fulfillment.
- Replay protection.
- Refund/cancel state reconciles entitlement.
- Android never promotes local success to premium before server confirmation.

#### Google Play Billing
- Real product IDs from Play Console.
- Purchase acknowledgement/consumption rules appropriate to product type.
- Server purchase-token validation where entitlement security requires it.
- Restore purchases on reinstall/new device.
- Pending purchase support.
- Cancel/refund/revoke test.
- Billing unavailable/network failure/user cancel states are truthful.

10/10 requires real Play internal/closed-track purchase evidence; emulator-only success is insufficient.

### Wave 8 — Account deletion and data lifecycle

- Deletion request is authenticated, App Check protected and idempotent.
- Use explicit states such as REQUESTED/PROCESSING/FAILED_RETRYABLE/COMPLETED.
- Delete bounded batches and persist progress if the data set can exceed one execution.
- Remove public/private profile, discovery indexes, relationships, chats/media according to product retention policy, saved searches, devices/tokens, location, media, identity registries and billing-linked app data where required.
- Firebase Auth deletion occurs last.
- Retained fraud/financial/legal records are documented and minimized.
- Retry after partial failure resumes safely.

### Wave 9 — Room/DataStore migration and offline correctness

- Explicit migration path from every released Room schema version.
- Upgrade tests using representative old DB files.
- No `fallbackToDestructiveMigration` for user-owned production data.
- Room remains a cache when Firestore is authoritative.
- Logout/account switch clears or namespaces account-specific cached sensitive state.
- Offline actions that require server authority remain pending, not falsely successful.

### Wave 10 — Android App Links and navigation integrity

- Use HTTPS Android App Links for externally shareable routes.
- Production-owned HTTPS domain.
- `android:autoVerify="true"` only for configured domain.
- Publish `/.well-known/assetlinks.json` containing production package and Play signing certificate SHA-256.
- Keep custom scheme only as an explicitly documented fallback if needed.
- Validate cold-start and warm-start routes for match/profile/chat/notification destinations.
- Invalid/deleted/unauthorized deep link must fail safely.

10/10 requires the real domain and final Play signing fingerprint; placeholders do not count.

### Wave 11 — Security hardening and configuration

- Debug App Check provider only in debug source/dependency graph.
- Release uses Play Integrity.
- Stage App Check enforcement using metrics before full enforcement.
- Firestore and Storage rules default deny for unknown paths.
- Production Firebase project separated from development/test where operationally required.
- No secrets/private keys committed.
- Crash/error logs scrub PII, tokens, OTPs, exact location and payment/KYC secrets.
- Rate-limit abuse-prone callable Functions.

### Wave 12 — Accessibility, internationalization and UI system

- All supported production locales have complete critical-flow strings.
- Stored unsupported locales normalize safely.
- RTL-safe layout where an RTL language is supported.
- TalkBack labels for icon-only actions.
- Minimum touch targets.
- 1.3x/1.5x font-scale audit on core flows.
- Light/dark/theme contrast audit.
- Loading, empty, error and retry states on network screens.
- No text clipped on compact/common phones.

### Wave 13 — Performance and reliability

Measure rather than guess:
- cold/warm startup
- ANR/crash-free sessions
- Compose recomposition hotspots
- Firestore reads per major screen
- listener count/lifecycle
- image/video memory use
- WorkManager retry volume
- function latency/error rate
- Nearby query cost

Release blockers:
- main-thread file/network I/O in user paths
- unbounded collection/listener reads
- obvious N+1 profile hydration without bounds/cache
- media OOM risks
- retry loops without backoff/idempotency

### Wave 14 — Observability and incident readiness

- Crashlytics release symbol/mapping upload verified.
- Non-fatal operational errors are observable without leaking PII.
- Payment/KYC/account-deletion failures have correlation IDs or safe structured context.
- Server logs distinguish validation, authorization, transient dependency and internal failures.
- Remote Config has safe defaults and cannot enable unfinished/unsafe routes.
- Rollback/kill-switch path documented for risky launch features.

### Wave 15 — Legal, Play Console and production operations

- Privacy Policy URL live.
- Terms URL live.
- Data Safety declaration matches actual collection/sharing/retention.
- Account deletion disclosure/path meets Play requirements.
- Content/user-generated-content moderation disclosures as applicable.
- Play signing/upload key configured.
- Play Integrity linked to production app.
- FCM, Phone Auth, Storage, Functions, Firestore indexes and App Check deployed to production project.
- Billing products active.
- Closed/internal testing requirements satisfied before production submission.

### Wave 16 — Final physical-device matrix

Minimum release evidence:
- clean install
- upgrade from every shipped version still in the field
- low/normal storage
- notification permission accepted/denied
- camera/gallery/media permission paths
- foreground/background/killed FCM
- sign out/sign in different account with stale token/cache
- two-device interest -> mutual match -> chat
- block while second device has chat open
- protected photo/video after block/hide
- offline send/retry/reconnect
- Nearby permission denied/limited/enabled/stop sharing
- premium purchase/cancel/refund/restore on Play test account
- account deletion interrupted/retried
- accessibility font/TalkBack smoke test

Cover at least a recent Pixel/stock Android device and a current Samsung/One UI device; include the minimum supported API level if materially different.

## 5. CI merge gate

Every release PR must pass, on the exact head SHA:

1. Functions install/build/test/typecheck.
2. Firestore Rules emulator tests.
3. Storage Rules emulator tests where applicable.
4. Android unit tests.
5. Android lint.
6. Debug APK assembly.
7. Release AAB assembly with R8/minification.
8. No new high-severity dependency/security finding in the configured scanner.

A green run from an older base/head is not transferable to a rebased PR.

## 6. Production rating workflow

After each merged batch, update the scorecard using only evidence:

| Feature | Code | Security | Tests/CI | Cross-device/device | External | Final /10 | Open blocker |
|---|---:|---:|---:|---:|---:|---:|---|
| Example | complete | complete | green | pending | n/a | 8.8 | Pixel/Samsung test |

A 10/10 row must have no release blocker remaining.

## 7. Immediate implementation order

1. Protected profile media + truthful cross-device Video Profile.
2. Biodata and repository-wide dead/misleading CTA cleanup.
3. Rebuild atomic block/mutual-interest hardening on current production head.
4. Rebuild transactional nationwide Matrimony-ID reservation on current production head.
5. FCM stale-token cleanup and end-to-end account-switch tests.
6. Payment/Play Billing entitlement audit and missing production paths.
7. Restartable account-deletion stress/failure audit.
8. Room upgrade tests for every released DB schema.
9. App Links once the owned production domain and Play signing SHA-256 are available.
10. Accessibility/performance/i18n sweep.
11. Production Firebase/Play/legal configuration.
12. Final two-device + Pixel/Samsung release matrix and exact-SHA Production CI.

## 8. Release rule

Matree is not called "10/10 production ready" while any P0/P1 code defect, stale release-visible action, security bypass, external production configuration, or required physical-device gate remains open. The target is evidence-backed 10/10, not a cosmetic score.
