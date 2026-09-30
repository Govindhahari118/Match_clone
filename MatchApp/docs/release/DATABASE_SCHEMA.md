# Matree production data/schema ownership

Matree uses Firebase Auth/Firestore/Storage as server authority and Room/DataStore as Android cache or
device preference storage. Room must never become authority for identity, entitlement, verification,
blocking, moderation, deletion or protected-media access.

## Firestore domain collections

The production code uses domain collections/subcollections equivalent to:

- profile/account: `users`, `userPrivate`, usernames, religion/profile state;
- preferences/privacy: `partnerPreferences`, `privacySettings`, `privacyRelations`,
  appearance and notification preferences;
- interaction: `interests`, `interestResponses`, `matches`, shortlist/saved data,
  `profileViews`;
- messaging: `chats/{thread}/messages`;
- notifications: persisted `notifications`, private FCM installation ownership;
- presence/location: server-only presence and exact `userLocations`;
- billing: subscriptions, usage, payment/purchase/reconciliation ledgers;
- verification/consent: verification requests/results, consent projection and append-only consent
  ledger;
- trust/risk: private risk signals/activity/assessments;
- moderation/operations: reports, enforcement state, support tickets and immutable operations audit;
- media moderation: profile photo/video moderation and private duplicate-photo fingerprints;
- product/analytics: recommendation feedback and privacy-safe account analytics references.

Firestore Rules deny client writes to server-authoritative collections/fields and hide sensitive
collections from member clients.

## Storage namespaces

- `photos/{uid}/{file}`: owner upload; peer access only to the exact backend-published primary object;
- `videos/{uid}/{file}`: owner upload; peer access only to backend-published object when enabled;
- `voicebios/{uid}/{file}`: protected profile media;
- `verifications/{uid}/...`: never client-readable;
- `chat-media/{threadId}/{file}`: current authorized chat participants only.

Profile photo/video objects are immutable after upload; publication is backend/moderator-owned.

## Room

`MatchDatabase` current schema version is defined by `Migrations.CURRENT_VERSION`. Supported upgrade
history starts at `Migrations.OLDEST_SUPPORTED_VERSION`, and every registered hop is required to be
contiguous and non-destructive.

Current Room entities include user/profile cache, questionnaire, photos, messages, likes, blocks,
shortlist, notifications, profile views, saved searches, pending-message outbox and notes.

The emulator-backed CI migration matrix inserts an existing row, runs every supported migration path,
checks retained data and asserts current migration artifacts/indexes.

## Integrity invariants

- duplicate/replayed payment verification cannot grant duplicate entitlement;
- pending interest/match transitions are trusted-backend transactions;
- a blocked pair cannot continue authorized discovery/interest/chat;
- a non-active/deleting account is suppressed from peer-visible flows;
- raw KYC/risk/exact-location state is not client-readable;
- public profile media must have backend publication authority;
- compatibility is pair-specific; legacy global client-authored match scores are not authority;
- server-owned profile fields cannot be self-granted by Android;
- deletion is resumable and must not recreate a publicly deleted profile.

## Indexes

Composite Firestore indexes are committed in `firestore.indexes.json`. Exact production index
deployment/query verification remains an external release gate.
