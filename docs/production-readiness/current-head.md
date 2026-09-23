# Matree production readiness — current implementation branch

> Branch: `gpt/matree-pin-to-pin-20260923`
>
> Baseline: `Gpt_matree`
>
> Status: **DRAFT / NOT PRODUCTION-READY**
>
> Rule: a green workflow certifies only the exact SHA it ran against. Any later code or configuration commit invalidates that release claim until mandatory gates rerun.

## Requirements baseline

Implementation is governed by the uploaded Matree production source plus the pin-to-pin implementation plan. Latest explicit canonical decisions win over earlier exploratory text; security/privacy invariants are never weakened for UI convenience.

## Implemented hardening on this integration branch

- protected Firebase media resolution through authenticated object identity/current authorization
- truthful video capture/upload/playback states; no local fallback as uploaded success
- server/privacy-authoritative contact reveal and grant flow
- collision-safe nationwide-neutral `MAT-` Matrimony ID reservation and deletion release
- server-authoritative religion confirmation/lock
- client denial for religion authority metadata
- onboarding fabrication removal and supported-locale restriction
- neutral pre-religion onboarding (no forced astrology)
- release network policy without brittle Google/Firebase certificate pinning; emulator cleartext debug-only
- server-authoritative Boost; legacy client/local Boost grant disabled
- Biodata dead-action/fabricated-default cleanup
- production Home callback surface reduced to actually rendered/audited routes
- visible Matree branding alignment on primary app/sign-in shell
- no fabricated local fallback for missing Matrimony ID
- deletion requires recent authentication and immediately marks profile `DELETING`/hidden before deep cleanup
- peer reads of deleting accounts denied by Firestore Rules

## Exact-head evidence

Do not hard-code a green claim here unless the workflow completed for the exact branch HEAD. Consult the Production CI workflow run associated with the current commit.

Mandatory CI jobs:
1. Android unit tests, lint, debug build and release/R8 validation
2. Firebase Functions lint, build/typecheck and production dependency audit
3. Firestore + Storage emulator security rules tests

## Known remaining code/repository work

- continue route-by-route/button-by-button truthfulness audit
- finish pause vs permanent-delete lifecycle and deletion checkpoint/large-account verification
- verify account-switch/cache/media clearing across every repository
- verify remaining optional/provider-backed routes are READY/BETA/HIDDEN according to real backend state
- verify Saved Search, Nearby, notifications/deep links, chat, interests and discovery against the full two-device/state matrices
- complete source-wide mock/demo/fake/placeholder scan and classify every occurrence
- verify Room migrations from every supported production DB version
- add/finish App Links only after the real production domain + Digital Asset Links are available; never invent a domain
- final exact-head release evidence after the last change

## External / operator gates (cannot be truthfully marked complete by repository code alone)

- production Firebase projects/configuration/indexes/rules/functions deployment
- App Check production enforcement and Play Integrity evidence
- production KYC/provider credentials and provider error-path verification
- real Google Play Console product configuration and licensed test purchases
- Play App Signing/release signing/fingerprints
- production FCM real-device delivery matrix
- real domain + `.well-known/assetlinks.json` for verified HTTPS App Links
- privacy policy/terms/data-safety/account-deletion/location/KYC/media store declarations
- physical-device matrix (Pixel-equivalent, Samsung, low/mid-range; supported Android versions)
- two-user/two-device production-like journey recording
- branch protection/repository policy where hosting permissions permit
- final signed AAB hash, staged rollout and monitored rollback criteria

## Release statement

This branch/PR must remain draft until all release-critical P0/P1 requirements and external gates have current evidence. Code volume, screenshots, historical green commits or UI presence are not completion evidence.
