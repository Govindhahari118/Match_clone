# Matree production readiness — current completion branch

> Branch: `gpt/matree-pin-to-pin-completion-20260925`
>
> Baseline: `gpt/matree-pin-to-pin-20260923` (itself based on canonical `Gpt_matree`)
>
> Status: **DRAFT / NOT PRODUCTION-READY**
>
> Rule: a green workflow certifies only the exact SHA it ran against. Any later code or
> configuration commit invalidates that release claim until mandatory gates rerun.

## Requirements baseline

Implementation is governed by the uploaded `matree_all.txt` source and
`MATREE_PRODUCTION_READY_PIN_TO_PIN_IMPLEMENTATION_PLAN.md`. Latest explicit canonical decisions
win over earlier exploratory text; security/privacy invariants are never weakened for UI convenience.

## Existing hardening inherited from the integration branch

- Android Kotlin + Jetpack Compose + Firebase authority model retained
- protected Firebase media resolution through authenticated object identity/current authorization
- truthful video capture/upload/playback states; no local fallback as uploaded success
- server/privacy-authoritative contact reveal and grant flow
- collision-safe nationwide-neutral `MAT-` Matrimony ID reservation and deletion release
- server-authoritative religion confirmation/lock and appearance separation
- onboarding fabrication removal and supported-locale restriction
- neutral pre-religion onboarding
- release network policy with emulator cleartext debug-only
- server-authoritative Boost; legacy client/local Boost grant disabled
- Biodata dead-action/fabricated-default cleanup
- production Home callback surface reduced to rendered/audited routes
- no fabricated local fallback for missing Matrimony ID
- resumable account deletion with recent-auth requirement and immediate public suppression
- pause/resume lifecycle callable and Settings UI
- peer reads of deleting accounts denied by Firestore Rules
- server-authoritative saved searches and private Nearby coordinates

## Completion work added on this branch

- persisted notification event ledger before FCM delivery
- deterministic notification event IDs so retried triggers do not duplicate user-visible events
- cross-device Firestore notification history and read-state synchronization
- private per-installation FCM registration/revocation with account-switch transfer
- per-channel notification preferences enforced in Settings, Firestore Rules and backend delivery
- FCM-to-Room deduplication using the same persisted notification identity
- authenticated actor hydration for notification navigation after reinstall/second-device use
- Firestore Rules regression coverage for notification ownership/read-only mutation/preferences
- account logout/account-switch/deletion cache cleanup includes protected profile and chat media
- Nearby sharing status is explicitly non-interactive instead of a dead callback
- chat message tap has a real action instead of an empty callback
- CI production-integrity scan added before Android tests for executable stubs and enabled empty callbacks
- CI now uploads SHA-labelled Android, Functions and Rules evidence artifacts
- production route inventory added at `docs/production-readiness/route-inventory.md`
- architecture/authority/environment map added at
  `docs/production-readiness/architecture-authority-environments.md`
- Firebase Remote Config no longer uses a production fake/bypass path; real SDK values are used and
  optional/risky features fail safe OFF
- placeholder Firebase `google-services.json` is scoped to `src/debug`; release source has no
  committed placeholder production project
- CI release/R8 validation may temporarily inject the debug placeholder strictly for compile/R8
  validation, deletes any resulting AAB, and emits a provenance marker instead
- release AAB creation therefore remains blocked until a real release Firebase config is supplied
- Razorpay membership/Boost Functions and Android dependency removed; Google Play verification is
  the single deployed digital-entitlement authority
- stale optional/provider callables (`features.ts`, `backgroundChecks.ts`) removed from production
  deployment, eliminating the alternate reward→Boost path and unlaunched service request APIs
- pricing claims narrowed to actual server-enforced membership duration/contact quotas and
  Google-Play-backed restore/verification behavior
- profile-view notification trigger now binds the Firestore event context used in its deterministic ID

## Exact-head evidence

The branch had an all-green exact-head Production CI at SHA
`65608f49375152bfff4c6995e5d2cc9e85cb1602` before the later environment/billing hardening commits.

That prior green run is **historical evidence only**. The current branch must obtain a new all-green
Production CI after this final documentation/code state before it can be treated as the current
repository-side baseline.

Mandatory CI jobs:

1. production-integrity source gate + Android unit tests + lint + debug build + non-production
   release/R8 validation + evidence artifact
2. Firebase Functions lint + build/typecheck + production dependency audit + evidence artifact
3. Firestore + Storage emulator security-rules tests + evidence artifact

## Remaining repository verification before release

These are evidence/verification tasks that cannot be honestly marked complete merely because the
corresponding code path exists:

- execute the full two-user/two-device matrix for interests, chat, notification/deep-link, block,
  privacy-revocation, discovery, payment recovery and deletion
- execute large-account deletion/retry verification with >500 related records and forced partial failure
- prove Room upgrade migrations from every actually supported historical production DB version;
  destructive migration remains debug-only
- finish physical-device/process-death/network-chaos evidence for supported Android versions/OEMs
- verify all BETA/provider-backed routes against production provider state before promotion
- add verified HTTPS App Links only after the real production domain and Digital Asset Links file exist
- repeat the exact-head production-integrity sweep after every subsequent production-source change

## External / operator gates

These cannot be truthfully completed by repository code alone:

- real production Firebase `google-services.json`, project IDs and dev/staging/prod project mapping
- production Firebase indexes/rules/Functions deployment evidence
- App Check production enforcement + Play Integrity console evidence
- production KYC/provider credentials and real provider failure-path verification
- real Google Play Console product configuration, licensed test purchases, restore/refund/expiry evidence
- Play App Signing/release signing and production SHA fingerprints
- production FCM delivery matrix on real devices
- real production domain + `.well-known/assetlinks.json` for verified HTTPS App Links
- final Privacy Policy, Terms, Data Safety, account-deletion, location, KYC and media store declarations
- physical-device matrix (Pixel-equivalent, Samsung, low/mid-range; supported Android versions)
- production-like two-user/two-device journey recording
- repository branch-protection/required-check policy where hosting permissions permit
- final signed production AAB hash, staged rollout, monitoring and rollback criteria

## Release statement

This branch and PR #18 must remain Draft until the current exact HEAD is green and all required
external gates have current evidence. Code volume, screenshots, historical green commits or a
rendering Compose screen are not completion evidence.
