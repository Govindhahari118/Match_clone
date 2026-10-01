# Matree production environment specification

This document defines repository-controlled environment boundaries for the release candidate. It does
not contain production project IDs, credentials, signing secrets or provider secrets.

## Environments

| Environment | Android package | Firebase/App Check | Purpose |
|---|---|---|---|
| Debug/local | `com.match.app.debug` | Debug Firebase config + App Check debug provider | Developer/emulator validation only |
| Staging/closed test | release package/config supplied outside source | Play Integrity-capable App Check | Production-like E2E, billing, device and provider validation |
| Production | `com.match.app` | Production Firebase + Play Integrity | Store release |

Production Firebase must be separate from development/staging. The repository intentionally does not
commit a production `.firebaserc` alias or release `google-services.json`.

## Runtime services

Android consumes Firebase Authentication, Firestore, Storage, Cloud Functions, FCM, Remote Config,
Analytics/Crashlytics and App Check. Digital entitlements are Google Play Billing verified by trusted
Cloud Functions.

Cloud Functions runtime target: Node 22.

## Required deployment inputs

- production Firebase project/application registration;
- release `google-services.json` supplied by the release environment;
- release signing / Play App Signing configuration;
- Firebase Auth provider enablement for Phone, Email/Password and Google when exposed;
- deployed Firestore rules/indexes, Storage rules and Functions;
- Remote Config production values;
- App Check/Play Integrity registration and staged enforcement;
- Google Play product IDs/API access and licensed tester configuration;
- operator Hosting deployment and controlled initial roles;
- production legal URLs and, if used, verified App Links domain.

## Fail-closed feature defaults

Provider/evidence-dependent features remain OFF unless the production project deliberately enables
them after the corresponding evidence gate passes. Current safe-off production flags include Nearby,
Kundali, NRI-specific discovery and profile video where represented by the Android Remote Config
contract.

Core authentication, profile, discovery, interests, shortlist, chat, privacy, notifications and
account lifecycle must not depend on an optional feature flag for security correctness.

## Health

- `healthLive` verifies Functions runtime liveness without a dependency probe.
- `healthReady` verifies Firestore reachability and returns HTTP 503 when the dependency is not ready.

Neither endpoint returns secrets, project identifiers or member data.
