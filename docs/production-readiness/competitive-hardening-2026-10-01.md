# Competitive hardening contract — 2026-10-01

This document is the code-truth contract for the October 2026 trust, freshness, notification, and interest-introduction hardening. It is intentionally narrower than the full production launch checklist.

## Screen-capture privacy

- Matree starts every `MainActivity` with Android `FLAG_SECURE` already set, before Compose content or asynchronous preferences can expose account data.
- The persisted user preference defaults to enabled.
- An explicit user opt-out clears `FLAG_SECURE`; re-enabling restores it immediately.
- This protects Android surfaces covered by `FLAG_SECURE`. Device/OEM behavior still requires the physical-device release matrix.
- Instrumentation coverage: `ScreenCaptureProtectionTest`.

## Discovery freshness

Freshness is a trusted-backend eligibility signal and never exposes the exact private presence timestamp to another member.

- Presence anchor: latest trusted private `lastActiveAt`, falling back to public profile `createdAt`.
- Unknown legacy profiles with neither anchor remain eligible until a trustworthy anchor exists.
- A future-dated anchor is clamped to the server's current time so bad clock data cannot grant permanent freshness.
- Default stale threshold: 90 days.
- Supported product range: 14–365 days.
- Server configuration: `discovery.max_inactive_days`.
- Boundary semantics: exactly 90 days remains eligible at the default; 91 days is stale.
- Tests cover 89/90/91 days, legacy unknown activity, recent private activity, future anchors, and configuration clamps.

## Notification quiet hours

Quiet hours affect optional push delivery only. The durable in-app notification is still written first. Critical security/safety/verification notifications bypass optional notification preferences.

- Product window: 22:00–07:00 in the member's current IANA timezone.
- The policy helper validates clock bounds defensively, but production delivery intentionally uses the fixed 22:00–07:00 contract shown in Settings so backend behavior cannot drift from the user-facing promise.
- Invalid/unknown timezone data fails open for optional push rather than silently suppressing delivery.
- Android syncs the current timezone whenever the app returns to the foreground. A per-process account/timezone key avoids unnecessary duplicate writes. DST offset changes require no stored preference rewrite because the server resolves the IANA timezone at delivery time.

## Personalized interest introductions

An optional introduction may accompany a new interest, capped at 280 normalized characters.

The trusted callable is the only write authority. Firestore clients cannot create, edit, or delete interest documents directly.

Before a mutual match, introductions reject:
- URLs and external links;
- direct email addresses;
- direct phone numbers;
- common Unicode/full-width contact obfuscation;
- common `at` / `dot` email/domain obfuscation;
- external contact-channel references such as WhatsApp, Telegram, Signal, Instagram, Snapchat, and WeChat;
- money/payment solicitation signals.

The server applies NFKC normalization, removes zero-width characters, collapses whitespace, validates, and stores only the normalized result.

Interest documents are readable only by their two active, unblocked participants. A block or inactive account immediately removes client read access. The decline-response ledger remains server-only.

## Lifecycle authority

- Sender withdrawal is permitted only while an outgoing interest is still pending.
- Recipient decline is permitted only while an incoming interest is still pending.
- A reverse interest or an existing match prevents either pending-only mutation.
- A prior decline prevents the same sender from immediately recreating the request unless the prior recipient later initiates the pair.
- Blocking remains trusted-callable authority and revokes relationship visibility immediately.

## Promotion requirements

Repository tests are necessary but not sufficient for production promotion. Before merge/release, obtain:
1. Functions lint, TypeScript build, and unit-test green.
2. Android unit/lint/release-R8 green.
3. Firestore/Storage rules-test green.
4. Production scanners green.
5. Physical-device validation of screen capture on supported Android/API/OEM matrix.
6. Two-account/two-device interest lifecycle E2E (send, receive, note, decline, withdraw, mutual, block).
7. Production-like notification checks across foreground/background/cold-start and timezone changes.
8. Exact-head Production CI green before merge.
