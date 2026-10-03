# Secure Call Provider Acceptance

Matree must not advertise or enable live voice/video calling until every item below is evidenced for the exact release SHA.

## Provider integration

- A real production communications provider adapter is implemented and code-reviewed.
- Provider credentials are stored only in managed server-side secrets.
- The provider account is production-approved for the intended countries and traffic.
- Relay/masked-number or WebRTC session allocation is server-authoritative.
- Android never receives reusable provider master credentials.
- Session creation is limited to active mutual matches and re-checks block/privacy state immediately before allocation.
- Backend rollout flag `features.secure_calls=true` is required in addition to the compiled provider adapter.

## Callback integrity

- Provider callbacks/webhooks use signature verification with replay protection.
- Duplicate callbacks are idempotent.
- Unknown sessions, users, destinations and malformed state transitions are rejected.
- Call lifecycle state cannot move backwards or skip security-critical transitions.
- Provider callback timestamps and identifiers are retained only as needed for support/security.

## Privacy

- Personal phone numbers are not disclosed when number masking is promised.
- Call metadata shown to users comes from verified provider events, not synthetic client timers.
- No audio/video is recorded unless a separately reviewed product requirement, consent model and retention policy exist.
- Logs never contain raw access tokens, call media, secrets or unnecessary phone-number data.

## Abuse and safety

- Per-account and per-pair call initiation rate limits exist.
- Block/report changes terminate or prevent new sessions.
- Suspended/deleted/inactive accounts cannot start or receive new calls.
- Safety operations can disable calling globally without an Android release.
- Provider fraud and spend alerts are configured.

## Billing and entitlement

- Any paid-call entitlement is server-authoritative.
- Failed, refunded or expired purchases cannot unlock provider allocation.
- Provider usage and Play entitlement reconciliation are tested if minutes/credits are sold.
- Pricing shown in the app exactly matches the active Play catalogue.

## Device acceptance

Run on at least two independent physical Android devices/accounts:

1. eligible mutual match -> call capability available;
2. non-mutual pair -> denied;
3. either-direction block -> denied;
4. contact/profile privacy restriction -> denied;
5. inactive/suspended account -> denied;
6. provider unavailable -> graceful fail-closed UI;
7. poor network, background/foreground and app restart behavior;
8. incoming/outgoing call lifecycle;
9. missed/rejected/cancelled/ended states;
10. audio route, microphone permission and Bluetooth/headset behavior for voice;
11. camera permission, rotation and background behavior for video when video is enabled;
12. masked-number privacy verification where applicable.

## Promotion rule

Until this checklist is complete, keep the default `DisabledCommunicationProvider` adapter in place and keep the secure-call rollout disabled. The UI may show relationship eligibility and provider status, but it must not simulate an active dialer, relay number, call history, minutes or billing.


## Repository contract now enforced

- `callSessionPolicy.ts` defines forward-only call lifecycle transitions and initiation rate policy.
- `communicationProvider.ts` defines the provider-neutral allocation interface and ships with a fail-closed disabled adapter.
- `startSecureCallSession` re-checks current mutual-match, account, block and privacy state, reserves per-account/per-pair hourly initiation budget, creates a server-owned session record, and only then requests short-lived provider allocation material.
- Failed provider allocation marks the server session failed and returns no synthetic dialer/session data.
