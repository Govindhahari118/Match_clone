# Matree trusted backend API inventory

Source of truth: `MatchApp/functions/src/index.ts`. Android callable usage is checked against exported
Functions by `scripts/ci/callable_contract_scan.py`.

This inventory documents the production backend surface by domain. Firestore/Storage triggers and
scheduled jobs are included because they are part of the production contract even when Android does
not call them directly.

## Identity and account lifecycle

- `setUsername`
- `listMyDevices`
- `revokeAllSessions`
- `deleteUserAccount`
- `setMatrimonyPaused`
- `getMyAccountLifecycle`
- `confirmReligion`
- `recordProfileView`

Lifecycle/profile triggers include `onUserCreate`, profile-completeness recalculation triggers,
profile-view notification processing and inactivity/profile-completion scheduled notifications.

## Discovery, preferences and recommendations

- `discoverProfiles`
- `getPartnerPreferences`
- `setPartnerPreferences`
- `getPartnerPreferenceSummary` — returns only an explicitly shared non-sensitive expectation summary; sensitive preference dimensions remain private
- `saveSavedSearch`
- `deleteSavedSearch`
- `touchPresence`
- `getMemberPresence`

Recommendation feedback is server-recorded through profile-opened, shortlisted, interest-sent,
mutual-match, hidden and reported triggers. A simple impression is not treated as a strong positive
preference signal.

## Interests, safety and support

- `sendInterest`
- `withdrawInterest`
- `declineInterest`
- `blockUser`
- `unblockUser`
- `submitProfileReport`
- `submitSupportTicket`
- `listMySupportTickets`

## Messaging and media

- `prepareChatThread`
- `sendChatMessage`
- `submitProfilePhoto`
- `setPrimaryApprovedPhoto`
- profile photo/video moderation/operator review callables
- profile-video removal
- chat-media Storage trigger and message-safety triggers

Chat message creation is server-authoritative and rate-limited; Storage rules bind chat media to the
authorized participant/thread contract.

## Secure communication

- `getSecureCallCapability` — returns fresh relationship/privacy eligibility separately from provider readiness.

Live provider session creation remains intentionally unavailable until an audited communications adapter, server-side rollout flag, provider callbacks and physical-device acceptance satisfy `SECURE_CALL_PROVIDER_ACCEPTANCE.md`.

## Verification, consent, trust and risk

- `getVerificationOptions`
- `confirmPhoneVerification`
- `submitVerificationRequest`
- operator verification approval/review callables
- `recordConsent`
- `getConsentState`
- `getTrustSummary`
- `updateRiskAssessment`

Consent withdrawal triggers clean up location, sensitive personalization, identity-verification and
media-processing state where applicable. Trust exposes positive evidence only; raw risk signals remain
server/operator-only.

## Location and optional astrology

- `getNearbyStatus`
- `updateMyLocation`
- `clearMyLocation`
- `nearbyProfiles`
- stale-location cleanup triggers/jobs
- Kundali/horoscope functions exported from `horoscope.ts`

Nearby and Kundali remain fail-closed until their production evidence gates pass.

## Billing

- `verifyGooglePlayPurchase`
- `getMyBoostStatus`
- Google Play RTDN processing
- voided-purchase reconciliation
- expired-entitlement reconciliation

Entitlements are server/provider authoritative and idempotent.

## Notifications and analytics

- `registerFcmDevice`
- `revokeFcmDevice`
- persisted interest/match/message notification triggers
- `getProfileAnalytics`

Push notification content is deliberately generic and notification preferences are consulted by the
backend.

## Family, assisted service and data portability

- family delegate invite/accept/cancel/revoke/list/read/update callables;
- relationship-manager request/read/cancel and operator workflow callables;
- `createMyDataExport` and expiration cleanup.

## Operations / moderation

Role-scoped operations APIs include support-ticket lifecycle, profile-report moderation, queue metrics,
payment reconciliation, moderation cases, account enforcement, operator access introspection,
verification review queue/cases, risk review queue and account payment views.

## Health

- `healthLive`
- `healthReady`

## Authorization rule

Android may request actions, but server-owned state such as entitlement, verification, moderation,
account enforcement, Trust/risk, protected-media publication and lifecycle authority is never accepted
from the client as truth.

- `getSecureCallCapability` — server-authoritative relationship/provider capability check; does not allocate a session.
- `startSecureCallSession` — server-authoritative provider-session allocation boundary. Requires an active mutual match, current block/privacy eligibility, configured provider adapter, rollout flag, per-account/per-pair rate limits, and returns only short-lived client allocation material. The default adapter fails closed.

- `setChatTyping` — App Check/authenticated, mutual-match-only ephemeral typing signal. Re-checks active accounts, both-direction blocks and profile privacy; writes only short-lived server-owned typing state under the authorized chat thread. Clients cannot write typing documents directly.

- `setChatThreadPreferences` — authenticated, App Check-aware per-user conversation preference update. Requires the caller to be a participant in the existing canonical thread; stores only that user's `muted`/`archived` state. Clients may read only their own preference documents and cannot write them directly. Muted threads suppress new-message notification delivery; archived threads are hidden from the default inbox and automatically return to Active on a new incoming message.
