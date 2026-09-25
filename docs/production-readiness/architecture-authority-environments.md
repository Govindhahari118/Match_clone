# Matree architecture, authority and environment map

> Applies to `gpt/matree-pin-to-pin-completion-20260925`.
> This records repository-enforceable boundaries. Production project IDs, provider credentials,
> enforcement switches and deployment proof remain operator evidence and are not invented here.

## Canonical v1 architecture

- Android: Kotlin, Jetpack Compose, Hilt, Room/DataStore, WorkManager.
- Backend: Firebase Authentication, Firestore, Storage, Cloud Functions, FCM, Remote Config,
  Crashlytics and App Check.
- Billing: Google Play Billing for app membership/Boost authority; server verification owns
  entitlement activation.
- Client persistence: Room/DataStore are cache/preferences, never authority for premium,
  verification, blocking, deletion, religion lock, provider purchase or protected-media access.
- Protected media: Firebase Storage object identity + current SDK authorization; long-lived token
  URLs are compatibility inputs only, not authorization.

## Critical authority matrix

| State / mutation | Authoritative owner | Client role |
|---|---|---|
| Firebase identity | Firebase Auth | Initiate sign-in/sign-up; never forge UID |
| Public profile fields | Owner client subject to Rules | Submit allowed editable fields |
| Private contact/birth fields | Owner-scoped private document + server privacy logic | Read/write only own allowed fields |
| Religion confirmation/lock | Trusted server mutation | Request/confirm through supported flow; appearance cannot rewrite it |
| Verification/KYC status | Trusted Functions/provider/admin | Submit protected evidence; display returned state |
| Premium/subscription/Boost | Google Play + trusted verification Functions | Launch purchase/restore; never set entitlement/expiry |
| Interest/match authority | Trusted Functions/transactions | Request action; observe server result |
| Block/report/risk/moderation | Trusted Functions/admin where applicable | Request action; cannot self-author server status |
| Chat sender identity | Auth UID + Firestore Rules | Compose payload; Rules bind sender/participants |
| Notification events | Trusted backend triggers | Read own events; may only mutate allowed read state |
| Notification channel preferences | Owner-only Firestore document | Toggle own booleans; backend consults before push |
| Exact Nearby coordinates | Trusted location callable/private collection | Foreground-only submit; never public-read coordinates |
| Account pause/deletion | Trusted callable + lifecycle job | Request pause/resume/delete; local state clears after server result |
| Matrimony ID/username reservation | Trusted server reservation | Display assigned value; no client collision authority |
| FCM token | Owner/server private state | Register/revoke current device token only |

## Build/environment boundaries currently enforced in source

- Debug application ID: `com.match.app.debug`.
- Release application ID: `com.match.app`.
- Debug App Check provider: Firebase Debug provider, available only from `src/debug`.
- Release App Check provider: Play Integrity, available only from `src/release`.
- Cleartext traffic is disabled in the release application policy; emulator/debug exceptions are
  isolated from release.
- Release signing reads local `keystore.properties` only when present; signing secrets are not
  hardcoded in Gradle source.
- Cloud Functions target Node 22.
- Remote Config now uses the real Firebase SDK. Optional/risky flags default OFF and therefore fail
  safe if no production value has been activated.
- `firebase.json` defines Rules/Indexes/Functions/Storage and emulator configuration, but the
  repository intentionally has no committed `.firebaserc` project alias. Production Firebase
  project selection/deployment proof is an external gate.

## Remote Config kill switches

Optional features default to OFF unless the trusted project enables them:

- video profiles
- voice calls
- community
- NRI features
- AI icebreakers
- daily rewards

Operational keys also include maintenance mode/message, forced/recommended update version,
free-message limit, daily-like limit, Boost duration, photo limits and minimum photos for Boost.

A missing/failed fetch must not turn on an optional feature.

## Version/schema ownership

- Room schema version is owned by `MatchDatabase` and explicit `Migrations`; destructive fallback
  is debug-only.
- Firestore schema evolution is owned jointly by Functions/repositories and Rules; server-authority
  fields must not gain a second client-write path.
- Storage object formats use durable Firebase paths/object identity. New protected media must not
  persist device-local `content://` identities as remote truth.
- Notification event IDs are deterministic for trigger events so retries are idempotent.

## Retry/idempotency policy

- Payment verification, Boost activation, relationship authority, notification triggers and account
  deletion must be replay-safe at the trusted backend boundary.
- Client retries may repeat requests but may not grant local success before authoritative
  confirmation.
- Notification triggers persist a deterministic event before attempting FCM; FCM failure does not
  erase in-app history.
- Account deletion is restartable and local account-scoped state is cleared only after trusted
  confirmation.

## Timestamp policy

Device time may be used for non-security local presentation/cache metadata only. Security, billing,
entitlement, account lifecycle, notification authority, moderation and server ordering must use
trusted server/provider timestamps or trusted server calculations.

## Still external / not certified by this document

- actual dev/staging/prod Firebase project IDs and separation
- production App Check enforcement switch and Play Integrity console proof
- secret-manager/provider credentials and rotation evidence
- deployed Remote Config values
- production Firestore indexes/Functions/Rules deployment proof
- Play Console products/signing/licensed-test evidence

Those must be attached to the release evidence pack; repository documentation cannot substitute for
operator proof.
