# Matree production route inventory

> Scope: reachable production navigation on `gpt/matree-pin-to-pin-completion-20260925`.
>
> This inventory classifies navigation exposure, not overall release readiness. A `READY` route still
> depends on the exact-head CI, Firebase/provider configuration, security rules and external gates.

## Status contract

- **READY** — route is wired to a real repository/backend path and has no known simulated-success path.
- **PREMIUM** — real route, gated by a server-owned entitlement where required.
- **BETA** — real implementation is present but operator/provider evidence is still required before broad exposure.
- **HIDDEN/UNAVAILABLE** — implementation/prototype may exist in source, but it is intentionally not reachable from the production shell.
- **REMOVE** — obsolete route that should not remain reachable or be reintroduced.

## Reachable production routes

| Route | Status | Authority / release note |
|---|---|---|
| Home | READY | Real account/profile state and real navigation callbacks only. |
| Discover / Matches | READY | Server/Firebase eligibility and current profile data; hard rules remain authoritative. |
| Nearby | BETA | Foreground-only location, private exact coordinates, server-derived coarse distance; production location/load evidence still required. |
| Interests | READY | Server-authoritative interest transitions and block checks. |
| Shortlist | READY | Server-backed shortlist; no demo fallback. |
| Messages list | READY | Real conversations only. |
| Chat | READY | Authenticated sender, durable outbox/retry and current block authorization. |
| Questionnaire | READY | Persisted user answers; no fabricated compatibility result. |
| Profile | READY | Canonical profile and privacy/trust state. |
| Profile detail | READY | Re-authorizes peer profile access before display. |
| Settings | READY | Appearance, saved searches, pause and permanent deletion are real operations. |
| Language | READY | Supported locale catalog only. |
| Notifications | READY | Server-persisted real events, FCM delivery and cross-device read state. |
| Who Viewed | READY | Server-recorded view events; client cannot forge view authority. |
| Kundali | BETA | Available only where applicable; provider/production evidence required for any provider-backed interpretation. |
| Membership / Pricing | PREMIUM | Google Play product/verification path; production Play Console evidence required. |
| Verification | BETA | Server-authoritative statuses; production KYC/provider evidence required. |
| Privacy dashboard | READY | Real privacy settings/relationship controls. |
| Help | READY | Support/help navigation; support operations depend on backend records where shown. |
| Terms / Privacy / Guidelines / Security / Refunds | READY | Static legal/support surfaces; final operator/legal approval is external. |
| Biodata | READY | Real profile data only; no fabricated defaults. |

## Intentionally not reachable from the production shell

The following source surfaces remain **HIDDEN/UNAVAILABLE** unless they are separately promoted after
their end-to-end provider/data/entitlement contract passes the same Definition of Done:

`AI Match Insights`, `Profile Analytics`, `Assisted Matchmaking`, `Background Check`,
`Bio Generator`, `Profile Boost standalone surface`, `Circles`, `Community Browse`,
`Counselling`, `Compatibility Deep Dive`, `Swipe Discovery`, `Live Events`, `Family tools`,
`Guides`, `Advanced Horoscope`, `Likes`, `Virtual Meet`, `Muhurat/Astro Calendar`,
`NRI discovery`, `Photo Editor`, `Referral`, `Regions`, `Daily Rewards`, `Safety Center
standalone surface`, `Second Marriage discovery`, `Secure Call`, `Success Stories`,
`Testimonials`, `Relationship Timeline`, `Video Profile`, and `Wedding Planner`.

Their Kotlin files are not proof of product availability. They must not be re-added to production
navigation merely because a screen renders.

## Deep-link exposure

Production currently accepts the documented `matrimonyconnect://` internal scheme for typed,
validated destinations. Verified HTTPS App Links remain **HIDDEN/UNAVAILABLE** until a real production
domain and matching `.well-known/assetlinks.json` exist. No placeholder domain is permitted.

## Promotion rule

A hidden/BETA route may become READY/PREMIUM only when UI + state + authorization + persistence +
failure/offline handling + tests + telemetry + provider/operator evidence (where relevant) all pass on
the same release candidate.
