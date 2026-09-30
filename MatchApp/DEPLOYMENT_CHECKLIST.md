# Matree — Production Release Gates

This checklist describes the current production architecture on canonical `main`. A release is **not production-ready** merely because it compiles. Every P0 gate below must be satisfied with the production Firebase/Google Play configuration and real-device validation.

## 1. Source and CI gates

- [x] Canonical production and release branch is `main`. Never delete or replace it with a prototype branch.
- [ ] Protect `main` in GitHub and require the Production CI checks (external repository administration).
- [ ] Production CI is green on the exact release commit:
  - Android JVM tests
  - Android lint
  - debug build
  - release AAB/R8 validation
  - release configuration + merged-manifest scan
  - generated AAB debug/test leakage audit
  - exact-SHA release evidence manifest
  - Firebase Functions lint + TypeScript build
  - Firestore + Storage emulator security tests
- [x] No known repository-controlled P0/P1 is knowingly exposed: incomplete/unsafe surfaces are hidden or fail-closed, and the screen-classification CI gate prevents accidental re-exposure.

## 2. Android / Play requirements

- [x] `compileSdk = 36`
- [x] `targetSdk = 36`
- [x] Java/Kotlin JVM target 17
- [x] Release minification and resource shrinking enabled
- [x] Release Gradle configuration accepts protected CI signing inputs via `MATREE_KEYSTORE_*` with local `keystore.properties` fallback and rejects partial signing configuration.
- [x] Release Gradle configuration accepts `MATREE_VERSION_CODE` / `MATREE_VERSION_NAME`; the production build script requires explicit values.
- [ ] Release signing uses Play App Signing / protected CI secrets in the actual release environment. Never commit keystores or passwords.
- [ ] Set the final intended `MATREE_VERSION_CODE` / `MATREE_VERSION_NAME` for the Play candidate and verify the code exceeds every previously uploaded build.
- [ ] Install the signed release build on physical devices covering Android 7+ and current Android releases.
- [ ] Validate fresh install, upgrade install, process death and offline/online recovery.

## 3. Production Firebase project

Use a production Firebase project separate from development/staging.

- [ ] Add the production Android app/package and download its `google-services.json` outside source control where appropriate for the release process.
- [ ] Enable Firebase Authentication providers actually used by the app (Phone, Email/Password, and Google if offered in production UI).
- [ ] Configure authorized domains and Google Sign-In SHA-1/SHA-256 fingerprints for release signing.
- [ ] Create Firestore in production mode.
- [ ] Deploy `firestore.rules` and `firestore.indexes.json`.
- [ ] Enable Firebase Storage and deploy `storage.rules`.
- [ ] Deploy Cloud Functions from `functions/` using Node 22.
- [x] Repository Firebase deploy scripts require explicit `MATREE_FIREBASE_PROJECT_ID` and never rely on an ambient `firebase use` selection.
- [ ] Enable FCM, Crashlytics, Analytics and Remote Config only where the application actually consumes them.
- [ ] Set Firebase/Google Cloud billing alerts and budget monitoring.

## 4. App Check / abuse protection

- [x] Android release builds use Firebase App Check with Play Integrity.
- [x] Debug builds use the App Check debug provider.
- [ ] Register the production Play Integrity/App Check configuration in Firebase Console.
- [ ] Observe legitimate production/test traffic before enforcement.
- [ ] Set `security.enforce_app_check=true` for callable Functions only after valid release traffic is confirmed.
- [ ] Enforce App Check for supported Firebase products after staged verification; do not lock out legitimate testers by enabling it blindly.

## 5. Authentication and account lifecycle

- [x] Firebase Auth is the password authority; Room is not an offline password database.
- [x] Server-confirmed account erasure is required before the client reports success.
- [ ] Test phone OTP signup/login, resend/expiry/provider abuse limits and auto-verification on release devices.
- [ ] Test email sign-up/sign-in/reset on two physical devices.
- [ ] Test Google sign-in/linking on release credentials if Google login is exposed.
- [ ] Test disabled/restricted/suspended/deleted accounts cannot regain access from local cache, stale deep links or chat.
- [ ] Test single-device sign-out plus "sign out all devices" refresh-token revocation and FCM cleanup.
- [ ] Verify the privacy-safe signed-in-device list never exposes FCM tokens.
- [ ] Test account deletion removes/anonymizes all user data according to the data-retention policy, including exact Nearby location.

## 6. Firestore / authorization

- [x] Premium, verification and payment authority fields cannot be self-granted by Android clients.
- [x] Private/contact profile data lives outside the public/discovery profile document.
- [x] Blocking is enforced in security-sensitive interactions.
- [x] Chat requires authenticated participants and mutual interest.
- [x] Security-rule emulator tests cover privileged profile fields, private data, blocking, chat, payments, profile views, Storage and exact Nearby coordinates.
- [x] Production CI re-runs Firestore/Storage emulator authorization tests for pull-request rule changes; exact-release deployment evidence is still required externally.
- [ ] Verify every active query against production composite indexes before staged rollout.

## 7. Payments / entitlements

Google Play Billing is server-authoritative. Do not trust a client purchase callback, product label, cached premium flag or local boost state.

- [x] Android purchase flow uses Google Play Billing.
- [x] Backend verifies purchase tokens with Google Play before granting subscription/boost entitlements.
- [x] Purchase processing is idempotent and rebuilds entitlement from the server payment ledger.
- [x] Raw purchase tokens are not exposed through operations reconciliation views.
- [ ] Configure the production Google Play service-account/API access and actual product IDs outside source control.
- [ ] Verify acknowledgement/consumption and voided/refunded purchase reconciliation against the production Play account.
- [ ] Test success, cancellation, pending purchase, process death, duplicate verification, replay, wrong-user token and refund/void scenarios.
- [ ] Ensure Play Console products, pricing, refund copy and Data Safety/payment declarations match the shipped entitlement model.

## 8. Storage and media

- [x] Remote user media paths use Firebase Auth UID, not local Room numeric IDs.
- [x] Chat image/voice media has authenticated participant metadata/rules.
- [ ] Test profile photo upload → moderation queue → short-lived operator review → approve/reject → approved primary-photo publication between devices.
- [ ] Keep Video Profile fail-closed with Remote Config `show_video_profiles=false` and backend Functions config `features.video_profiles=false` until real-device upload/playback and moderation evidence pass; disabled direct uploads are expected to be purged server-side.
- [ ] Test chat image and voice upload/download between two release devices.
- [ ] Test retry after temporary network loss and verify failed uploads are not shown as successful cloud media.
- [x] Backend records valid unreferenced chat-media uploads in a server-only orphan ledger and scheduled cleanup re-checks the canonical message before deleting stale bytes.
- [ ] Verify the orphan-cleanup schedule, deletion metrics and Storage cost behavior in the production project.

## 9. Nearby / location

Nearby is foreground-only and should remain opt-in.

- [x] Manifest requests coarse/fine foreground location only; no background-location permission.
- [x] Runtime permission is requested only when Nearby is used.
- [x] Approximate location is supported; precise location is optional.
- [x] Android obtains a fresh foreground location and sends it through authenticated callable Functions.
- [x] Exact coordinates are stored in server-only `userLocations/{uid}` documents and are denied to clients by Firestore rules.
- [x] Nearby returns only profile identity + computed distance, never another member's coordinates.
- [x] Server filters self, stale locations, either-direction blocks, stealth profiles and incompatible gender preferences.
- [x] User can explicitly stop sharing and delete the stored location.
- [x] Exact location expires after 24 hours without refresh and is deleted immediately on stop-sharing, consent withdrawal or account deletion.
- [ ] Keep Remote Config `enable_nearby=false` **and** backend Functions config `features.nearby=false` until production location/security/load evidence passes; enable both only after validating the location Functions on the exact release.
- [ ] Real-device test: denied permission, approximate permission, precise permission, GPS/network provider, location services off, no results, 5/25/100 km radii, block/stealth behavior and stop-sharing.
- [x] Repository Privacy/Data Safety drafts explicitly describe opt-in foreground Nearby location, approximate/precise behavior, server-only exact coordinates, purpose, stop-sharing and deletion behavior.
- [ ] Final legal/Data Safety wording and retention obligations are approved against the exact production deployment.

## 10. Discovery / preferences / social / messaging

- [x] Durable partner preferences are separate from transient discovery filters and support STRICT / PREFERRED / NO_PREFERENCE.
- [x] Discovery enforces strict preferences bilaterally and uses bilateral preferred fit for ordering.
- [ ] Test partner preferences across devices, including strict exclusion, preferred ordering and deliberate no-preference choices.
- [ ] Keep dedicated NRI discovery fail-closed with Remote Config `enable_nri_features=false` and backend Functions config `features.nri_features=false` until inventory/load/device evidence passes. This does not disable ordinary country or saved NRI partner preferences.
- [ ] Keep Kundali fail-closed with Remote Config `enable_kundali=false` and backend Functions config `features.kundali=false` until the versioned Rasi/Nakshatra reference policy is independently validated.
- [ ] Discovery on a fresh second device shows authorized server-backed profiles without relying on demo/seed Room data.
- [ ] Sent/received interests and mutual matches stay consistent across two devices.
- [ ] Shortlists stay consistent across devices.
- [ ] Blocking immediately prevents discovery/interest/chat in both directions.
- [ ] Chat availability is derived from the server-authoritative mutual match state.
- [ ] Text/image/voice messages synchronize across devices and retries do not create duplicate remote messages.
- [x] Production source contains no demo/seed account authority or bundled fake member/event inventory; release CI rejects reintroduction.

## 11. Notifications

- [ ] Test FCM token registration, token refresh, sign-out cleanup and stale-token cleanup.
- [ ] Test interest, match and message notifications against production Functions.
- [x] Push copy is deliberately generic and does not include message bodies, KYC data, contact data or other profile-sensitive content.
- [x] Optional interest/match/message/system notification preferences are checked server-side; critical account/safety notifications remain transactional.

## 12. Verification / moderation / support

- [x] Verification, moderation, payment and support actions require explicit least-privilege operations roles.
- [x] Raw KYC files remain client-unreadable; operator review uses short-lived audited access.
- [x] Profile-photo publication requires server-owned moderation approval.
- [x] Moderators can place accounts under review, restrict, suspend or restore with immutable audit evidence; suspension revokes refresh tokens.
- [x] A separate operator console is wired to role-scoped callable APIs; it is not embedded in the consumer app.
- [x] Controlled dry-run-first role-bootstrap tooling exists, preserves unrelated claims, requires approval/actor evidence, revokes refresh tokens and writes a server-only audit record.
- [ ] Execute approved production role provisioning with the real Firebase project and attach the resulting operator evidence.
- [ ] Deploy the hardened operator Hosting surface and verify CSP/Auth/App Check behavior with real operator accounts.
- [ ] Test verification submission/review/approve/reject, photo review, report enforcement, appeal/support and payment reconciliation end-to-end.

## 13. Privacy, legal and product claims

- [x] Repository privacy-policy draft reflects the implemented Auth, Firestore, Storage, FCM, Analytics/Crashlytics, Google Play, verification-media and foreground-Nearby flows without legacy Razorpay/E2EE/BCrypt claims.
- [ ] Final Privacy Policy wording, controller/contact details, lawful-basis/rights language and retention schedule are approved by legal and published at the production HTTPS URL.
- [x] Repository Data Safety engineering worksheet inventories the implemented data flows and explicitly requires exact-release reconciliation.
- [ ] Final Google Play Data Safety answers are approved against the exact production SDK/provider configuration.
- [ ] Publish Terms, Privacy, Community/Safety and Refund pages at stable HTTPS URLs.
- [x] Release-candidate legal/help copy does not claim E2EE; repository CI rejects reintroduction of the stale end-to-end-encryption claim.
- [x] Repository truthfulness/release scans guard known overclaim classes; paid, verification, distance and safety flows use server/provider-derived state rather than fabricated success.
- [ ] Final product/legal copy receives release-owner review on the exact candidate.
- [ ] Complete content rating, target audience and account-deletion declarations.

## 14. Production surface / stale features

The production shell intentionally exposes a smaller audited surface. Source files for experimental ideas are not automatically production features.

- [x] Inventory every route reachable from `MainShell` before release (`docs/production-readiness/route-inventory.md`).
- [x] Every `*Screen.kt` is classified as `READY`, `BETA`, `STUB`, `UNSAFE`, or `POST_LAUNCH` in `docs/production-readiness/screen-classification.json`, and CI fails on inventory/exposure drift.
- [x] Hide/remove every `STUB`/`UNSAFE` route; Nearby and Kundali remain fail-closed behind Remote Config until their production evidence passes.
- [x] Only the explicitly supported production locale catalog is exposed; unsupported placeholder locales are excluded and regression-tested. Independent translation/layout QA remains an external release evidence item.
- [x] Production source is guarded against Firebase Dynamic Links reintroduction; retired provider/client integrations identified by the hardening audit are removed or fail-closed.
- [x] Production scan rejects demo/seed account source, device-local password authority and obsolete BCrypt; synthetic production inventory identified in the audit has been removed/fail-closed.

## 15. Release test matrix

- [ ] Auth: phone OTP signup/login, email signup/login/reset, Google, sign-out, sign-out-all-devices, restricted/suspended account and deletion.
- [ ] Profile: onboarding, edit, weighted strength, partner preferences, moderated photo, privacy and verification status.
- [ ] Discovery: fresh device, filters, stealth, blocks, pagination, no-results.
- [ ] Social: interests, mutual match, shortlist, unblock/reblock, two-device consistency.
- [ ] Nearby: all permission/service/radius/privacy cases in section 9.
- [ ] Chat: text/image/voice, read state, retry, process death, block mid-thread, two-device sync.
- [ ] Payments: test/live smoke test plus replay/idempotency/recovery cases.
- [ ] Notifications: foreground/background/killed-process receipt and deep navigation.
- [ ] Offline: authenticated cached viewing only; no insecure offline authentication or false server-write success.
- [x] CI emulator migration matrix upgrades every registered supported schema hop (v13 through current) and asserts retained rows/current artifacts.
- [ ] Where real historical production database fixtures exist, validate those exact fixtures on the release build before rollout.
- [ ] Accessibility: TalkBack labels, font scaling, contrast, touch targets and keyboard/input behavior.
- [ ] Performance: cold start, discovery scroll, image memory, chat list, ANR/crash rate on lower-end devices.

## 16. Rollout

- [ ] Internal test with production-like Firebase configuration.
- [ ] Closed test with required tester participation and real multi-device journeys.
- [ ] Fix all P0/P1 findings and confirm CI green on the exact candidate commit.
- [ ] Upload signed AAB and complete Play declarations.
- [ ] Stage production rollout (for example 5% → 20% → 50% → 100%) while monitoring Crashlytics, ANRs, Functions errors, payment failures and Firebase cost.
- [x] Repository rollback/kill-switch runbook documents safe-off Remote Config keys, maintenance use, core rollback actions and re-enable exit criteria (`docs/release/ROLLBACK_RUNBOOK.md`).
- [ ] Execute and attach a real production-like rollback drill for the exact release SHA.

## 17. Machine-verified external evidence

- [ ] Copy the external evidence template to an operator-controlled release record for the exact Git SHA.
- [ ] Attach concrete references for Firebase production configuration, signing, release credentials, App Check/Play Integrity, auth device matrix, licensed billing, physical E2E, accessibility, performance SLO, penetration test, legal/Data Safety, Play pre-launch report and rollback drill.
- [ ] Run `python3 scripts/ci/production_external_gate.py --evidence <file> --sha <exact-sha> --mode prelaunch` before production promotion.
- [ ] After controlled rollout and post-rollout health review, run the same exact SHA with `--mode full`.
- [x] External-evidence gate rejects templates/CI synthetic provenance by default; CI synthetic validation requires the explicit `--allow-ci-synthetic` self-test flag.

## Definition of Done

A candidate is production-ready only when the exact release commit has green CI, every active feature has passed its end-to-end real-device test, production Firebase/payment/Play configuration is complete, privacy declarations match the implementation, and there are no known P0/P1 blockers. Hidden source files and future-feature ideas do not count as completed functionality.
