# MatrimonyConnect — Production Release Gates

This checklist describes the current production architecture on `Gpt_matree`. A release is **not production-ready** merely because it compiles. Every P0 gate below must be satisfied with the production Firebase/Google Play configuration and real-device validation.

## 1. Source and CI gates

- [ ] Choose the canonical production branch. `main` and `Gpt_matree` have unrelated history; do not force-merge or force-push them together.
- [ ] Protect the canonical release branch and require the Production CI checks.
- [ ] Production CI is green on the exact release commit:
  - Android JVM tests
  - Android lint
  - debug build
  - release AAB/R8 validation
  - Firebase Functions lint + TypeScript build
  - Firestore + Storage emulator security tests
- [ ] No P0/P1 issue is knowingly deferred without disabling the affected user-facing route.

## 2. Android / Play requirements

- [x] `compileSdk = 36`
- [x] `targetSdk = 36`
- [x] Java/Kotlin JVM target 17
- [x] Release minification and resource shrinking enabled
- [ ] Release signing uses Play App Signing / protected CI secrets. Never commit keystores or passwords.
- [ ] Increment `versionCode` and set the intended `versionName`.
- [ ] Install the signed release build on physical devices covering Android 7+ and current Android releases.
- [ ] Validate fresh install, upgrade install, process death and offline/online recovery.

## 3. Production Firebase project

Use a production Firebase project separate from development/staging.

- [ ] Add the production Android app/package and download its `google-services.json` outside source control where appropriate for the release process.
- [ ] Enable Firebase Authentication providers actually used by the app (Email/Password and Google if offered in production UI).
- [ ] Configure authorized domains and Google Sign-In SHA-1/SHA-256 fingerprints for release signing.
- [ ] Create Firestore in production mode.
- [ ] Deploy `firestore.rules` and `firestore.indexes.json`.
- [ ] Enable Firebase Storage and deploy `storage.rules`.
- [ ] Deploy Cloud Functions from `functions/` using Node 22.
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
- [ ] Test email sign-up/sign-in/reset on two physical devices.
- [ ] Test Google sign-in/linking on release credentials if Google login is exposed.
- [ ] Test disabled/deleted accounts cannot regain access from local cache.
- [ ] Test sign-out token cleanup and re-login token registration.
- [ ] Test account deletion removes/anonymizes all user data according to the data-retention policy, including exact Nearby location.

## 6. Firestore / authorization

- [x] Premium, verification and payment authority fields cannot be self-granted by Android clients.
- [x] Private/contact profile data lives outside the public/discovery profile document.
- [x] Blocking is enforced in security-sensitive interactions.
- [x] Chat requires authenticated participants and mutual interest.
- [x] Security-rule emulator tests cover privileged profile fields, private data, blocking, chat, payments, profile views, Storage and exact Nearby coordinates.
- [ ] Re-run emulator tests against every release rule change.
- [ ] Verify every active query against production composite indexes before staged rollout.

## 7. Payments / entitlements

Razorpay must be server-authoritative. Do not trust a client success callback, plan name, amount or locally cached premium flag.

- [x] Server creates Razorpay orders from a server-owned plan catalogue.
- [x] Server verifies signature plus authoritative Razorpay order/payment state, amount, currency and ownership.
- [x] Payment activation is idempotent using deterministic payment records.
- [ ] Configure Razorpay live key ID and secret in secure production configuration; never commit the secret.
- [ ] Configure and verify the production webhook secret/endpoint if webhook recovery is enabled for the release.
- [ ] Test checkout success, cancellation, app process death after payment, duplicate verification, webhook replay and wrong-user order access.
- [ ] Test refund/chargeback business handling before advertising irreversible entitlements.
- [ ] Ensure Play listing/payment declarations are consistent with the actual business/payment model and applicable Play policies.

## 8. Storage and media

- [x] Remote user media paths use Firebase Auth UID, not local Room numeric IDs.
- [x] Chat image/voice media has authenticated participant metadata/rules.
- [ ] Test profile photo upload/delete/primary-photo update between devices.
- [ ] Test chat image and voice upload/download between two release devices.
- [ ] Test retry after temporary network loss and verify failed uploads are not shown as successful cloud media.
- [ ] Verify Storage lifecycle/cost policy for abandoned chat/media files.

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
- [x] Location data is excluded after 30 days without refresh and deleted with the user profile.
- [ ] Deploy the new location Functions before exposing Nearby in production.
- [ ] Real-device test: denied permission, approximate permission, precise permission, GPS/network provider, location services off, no results, 5/25/100 km radii, block/stealth behavior and stop-sharing.
- [ ] Data Safety and privacy policy explicitly describe foreground location collection, purpose, retention and deletion.

## 10. Discovery / social / messaging

- [ ] Discovery on a fresh second device shows authorized server-backed profiles without relying on demo/seed Room data.
- [ ] Sent/received interests and mutual matches stay consistent across two devices.
- [ ] Shortlists stay consistent across devices.
- [ ] Blocking immediately prevents discovery/interest/chat in both directions.
- [ ] Chat availability is derived from the server-authoritative mutual match state.
- [ ] Text/image/voice messages synchronize across devices and retries do not create duplicate remote messages.
- [ ] Remove or debug-gate any remaining demo seeds/fake activity before production.

## 11. Notifications

- [ ] Test FCM token registration, token refresh, sign-out cleanup and stale-token cleanup.
- [ ] Test interest, match and message notifications against production Functions.
- [ ] Avoid sensitive personal content in lock-screen notification text.
- [ ] Honor user notification preferences server-side where applicable.

## 12. Verification / moderation / support

- [x] Verification approval/rejection requires the server-side `admin` custom claim.
- [ ] Establish a controlled admin-claim provisioning procedure outside the consumer app.
- [ ] Test verification submission, review, approve and reject end-to-end.
- [ ] Test report/support callable paths and establish a real moderation/support handling process.
- [ ] Do not expose unfinished admin/operator screens inside the consumer release.

## 13. Privacy, legal and product claims

- [ ] Privacy Policy reflects the code that actually ships, including Auth, Firestore, Storage, FCM, Crashlytics/Analytics, payments, verification media and foreground location.
- [ ] Google Play Data Safety answers exactly match production behavior.
- [ ] Publish Terms, Privacy, Community/Safety and Refund pages at stable HTTPS URLs.
- [ ] Do not claim end-to-end encryption unless independently verified cryptographic key exchange and message confidentiality actually provide E2EE. Current device-local encryption must not be marketed as E2EE.
- [ ] Ensure premium, verification, distance and safety copy does not overstate guarantees.
- [ ] Complete content rating, target audience and account-deletion declarations.

## 14. Production surface / stale features

The production shell intentionally exposes a smaller audited surface. Source files for experimental ideas are not automatically production features.

- [ ] Inventory every route reachable from `MainShell` before release.
- [ ] Classify every screen as `READY`, `BETA`, `STUB`, `UNSAFE`, or `POST_LAUNCH`.
- [ ] Hide/remove every `STUB`, `UNSAFE` or unvalidated payment/identity/communication route.
- [ ] Placeholder language packs must remain hidden until independent translation and layout QA is complete.
- [ ] Remove stale Firebase Dynamic Links references and any other retired/deprecated integration from docs/code.
- [ ] Remove unused demo credentials, fake data generators and obsolete local-password utilities if no debug/test caller remains.

## 15. Release test matrix

- [ ] Auth: sign-up, login, reset, Google, sign-out, disabled account, deletion.
- [ ] Profile: onboarding, edit, photo, privacy, verification status.
- [ ] Discovery: fresh device, filters, stealth, blocks, pagination, no-results.
- [ ] Social: interests, mutual match, shortlist, unblock/reblock, two-device consistency.
- [ ] Nearby: all permission/service/radius/privacy cases in section 9.
- [ ] Chat: text/image/voice, read state, retry, process death, block mid-thread, two-device sync.
- [ ] Payments: test/live smoke test plus replay/idempotency/recovery cases.
- [ ] Notifications: foreground/background/killed-process receipt and deep navigation.
- [ ] Offline: authenticated cached viewing only; no insecure offline authentication or false server-write success.
- [ ] Room migrations: upgrade from every supported production DB schema to the current schema without data loss.
- [ ] Accessibility: TalkBack labels, font scaling, contrast, touch targets and keyboard/input behavior.
- [ ] Performance: cold start, discovery scroll, image memory, chat list, ANR/crash rate on lower-end devices.

## 16. Rollout

- [ ] Internal test with production-like Firebase configuration.
- [ ] Closed test with required tester participation and real multi-device journeys.
- [ ] Fix all P0/P1 findings and confirm CI green on the exact candidate commit.
- [ ] Upload signed AAB and complete Play declarations.
- [ ] Stage production rollout (for example 5% → 20% → 50% → 100%) while monitoring Crashlytics, ANRs, Functions errors, payment failures and Firebase cost.
- [ ] Have an operational rollback/disable plan using release rollback and narrowly scoped Remote Config feature flags where already implemented.

## Definition of Done

A candidate is production-ready only when the exact release commit has green CI, every active feature has passed its end-to-end real-device test, production Firebase/payment/Play configuration is complete, privacy declarations match the implementation, and there are no known P0/P1 blockers. Hidden source files and future-feature ideas do not count as completed functionality.
