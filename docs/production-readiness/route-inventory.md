# Matree production route inventory

> Scope: reachable production navigation on `gpt/matree-master-plan-hardening-20260929`.
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
| Nearby | BETA / OFF BY DEFAULT | Foreground-only location, private exact coordinates and server-derived coarse distance. Production route is fail-closed behind Remote Config `enable_nearby`; production location/load evidence is required before enabling. |
| NRI discovery | BETA / OFF BY DEFAULT | Real server-authorized NRI filtering with reciprocal preferences/privacy and no synthetic inventory. Fail-closed behind `enable_nri_features`; market inventory, load and device evidence are required before enabling. |
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
| Recently joined | READY | Uses the same server-authorized discovery path with authoritative profile creation timestamps and normal privacy/block/reciprocal-preference checks. |
| Family details | READY | Edits the signed-in member's real family-background fields with cloud-authoritative profile revision/sync handling and bounded inputs. |
| Family access | READY | Explicit 24-hour invite flow, revocable view/edit permissions, server allowlisted non-sensitive edits, lifecycle rechecks and audit trail. |
| Assisted matchmaking request | READY | Records a human-service callback/request with server status/cancellation and ops workflow. It does not purchase, activate or guarantee an RM service; pricing/service activation remain operator-confirmed. |
| Kundali | BETA / OFF BY DEFAULT | Available only where applicable. Production route is fail-closed behind Remote Config `enable_kundali`; validation/provider evidence is required before enabling. |
| Membership / Pricing | PREMIUM | Google Play is the single digital-entitlement authority. Displayed paid benefits are limited to enforced duration/contact quotas; production Play Console evidence is still required. |
| Verification | BETA | Server-authoritative government-ID statuses; production KYC/provider evidence required for provider-backed advanced identity methods. |
| Phone verification | READY | Firebase Phone Auth credential linking plus backend confirmation; explicitly separate from government-ID/KYC verification. |
| Video profile | BETA / OFF BY DEFAULT | Consented protected upload, server-owned moderation, audited review and backend-only publication/removal. Route is fail-closed behind `show_video_profiles` until real-device upload/playback and moderation-operations evidence pass. |
| Privacy dashboard | READY | Real privacy settings/relationship controls. |
| Help | READY | Support/help navigation; support operations depend on backend records where shown. |
| Terms / Privacy / Guidelines / Security / Refunds | READY | Static legal/support surfaces; final operator/legal approval is external. |
| Biodata | READY | Real profile data only; no fabricated defaults. |

## Intentionally not reachable from the production shell

The following source surfaces remain **HIDDEN/UNAVAILABLE** unless they are separately promoted after
their end-to-end provider/data/entitlement contract passes the same Definition of Done:

`AI Match Insights`, `Profile Analytics`, `Background Check`,
`Bio Generator`, `Profile Boost standalone surface`, `Circles`, `Community Browse`,
`Counselling`, `Compatibility Deep Dive`, `Swipe Discovery`, `Live Events`,
`Guides`, `Advanced Horoscope`, `Likes`, `Virtual Meet`, `Muhurat/Astro Calendar`,
`Referral`, `Regions`, `Daily Rewards`, `Safety Center
standalone surface`, `Second Marriage discovery`, `Secure Call`, `Success Stories`,
`Testimonials`, `Relationship Timeline`, and `Wedding Planner`.

`Video Profile` now has consented protected upload, server-owned moderation, audited operator review and backend-only publication/removal in source, but it remains hidden/off by default until real-device playback/upload, moderation-operations and performance evidence pass.

`Profile photo upload` is production-backed through protected Storage plus server moderation; the old simulated Photo Editor controls were removed. The upload surface must not claim crop/filter/brightness transforms until transformed bytes are actually produced and validated.

Their Kotlin files are not proof of product availability. They must not be re-added to production
navigation merely because a screen renders. Provider-backed optional callables for the retired
feature bundle/background-check prototype are also removed from the deployed Functions export
surface; promotion requires a new audited backend/provider contract, not just re-enabling a route.

## Exhaustive screen classification

The route table above describes launch navigation. The complete Android screen inventory is maintained
in `docs/production-readiness/screen-classification.json`. Production CI discovers every
`app/src/main/**/**Screen.kt` and fails if a screen is unclassified, if a STUB/UNSAFE/POST_LAUNCH
screen becomes routed, or if a flagged BETA route loses its fail-closed Remote Config default.
This makes source presence non-authoritative: adding a screen file cannot silently make it a launch
feature.

## Deep-link exposure

Production currently accepts the documented `matrimonyconnect://` internal scheme for typed,
validated destinations. Verified HTTPS App Links remain **HIDDEN/UNAVAILABLE** until a real production
domain and matching `.well-known/assetlinks.json` exist. No placeholder domain is permitted.

## Promotion rule

A hidden/BETA route may become READY/PREMIUM only when UI + state + authorization + persistence +
failure/offline handling + tests + telemetry + provider/operator evidence (where relevant) all pass on
the same release candidate.


## Fail-closed release promotion

BETA capability presence in source does not make it launch-visible. `enable_nearby`, `enable_kundali` and `enable_nri_features` default to `false` in the Android Remote Config contract. Drawer, Home, profile,
interest and direct/deep navigation paths all enforce the same flags. A direct navigation attempt
while disabled renders a truthful unavailable state rather than entering the feature.
