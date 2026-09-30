# Matree production readiness — current completion branch

> Branch: `gpt/matree-master-plan-hardening-20260929` (PR #22)
>
> Base: canonical `main`; this branch carries the September 29 production-hardening completion pass.
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
- Matree design tokens, canonical profile-card components and religion-aware/Neutral palette matrix are
  implemented with independent Automatic/Neutral/Manual appearance and device light/dark/system mode
- account appearance preference sync is cross-device and cannot mutate canonical religion
- supported production locale authority is centralized; incomplete prototype translation packs are removed
  rather than advertised as translated UI
- Room migration coverage now spans every registered supported migration hop and runs as a dedicated
  emulator-backed Production CI job
- deletion policy now has regression coverage for large-account batching, partial failure/retry and the
  invariant that a publicly deleted profile is never recreated during resume
- operations Functions now include least-privilege role resolution, support/moderation lifecycle,
  privacy-safe queue metrics, audit events and payment reconciliation views with RBAC tests
- Play purchase reconciliation covers void/refund and entitlement-expiry handling in repository code
- deep-link routing is extracted into a tested policy and handles cold/warm notification destinations
- privacy-safe product telemetry/Crashlytics boundaries are explicit and contract-tested
- chat local storage now uses versioned Android-Keystore-backed ciphertext for new Room/outbox message
  bodies; legacy pending plaintext is migrated in-place before retry instead of being retained in clear
- profile completeness is now backend-calculated from the same 20-field wizard contract; clients
  cannot author the authoritative aggregate, while DOB/income inputs remain in owner-private data
- declined interests are server-authoritative and cannot be immediately recreated by the sender;
  recipient initiation is the explicit path that can reopen the pair
- discovery hard filters now evaluate private astrology/income inputs only inside the trusted callable
  and respect horoscope/income disclosure settings without returning those private values
- activity filtering now reads server-only presence only when the target's last-active visibility
  permits that viewer; unknown activity is no longer fabricated as "Active recently"
- the unauthorised client-side "Recently active" sort was removed because discovery does not expose
  precise presence timestamps
- profile writes now use monotonic `profileRevision` transactions across Android, Room migration
  and Firestore Rules; stale writers refresh the server profile and surface a conflict instead of
  silently overwriting a newer device
- chat attachment payloads are bound to the exact thread/message object identity and voice duration;
  receipt updates enforce delivered-before-read semantics at the Rules boundary
- the durable chat outbox deletes only its own managed local media after authoritative send success,
  while failed/exhausted rows retain bytes for explicit retry
- Nearby now suppresses inactive/deleting/suspended accounts for both viewer and candidate, and its
  profile hydration forces a current server-authorized read rather than reviving stale cache data
- Shortlist and Who Viewed profile hydration now re-authorize current peer visibility; Who Viewed
  rebuilds its local history only from cloud-authoritative viewer IDs that remain readable
- pending Sent/Received interest lists explicitly exclude mutual matches; the Sent tab exposes the
  trusted pending-only withdrawal operation
- legacy global public `matchScore` authority is removed: clients cannot author it, Android ignores
  historical values, and a server migration deletes old copies because compatibility is pairwise
- generic profile hydration keeps activity hidden unless an authorized presence lookup is performed
- notification intents are account-bound, revoked Firebase sessions are checked without breaking
  offline startup, and expired/revoked sessions perform a full sign-out
- Google Play entitlement reconciliation has deterministic policy/ledger regression coverage for
  void/refund/expiry paths, and compatibility scoring carries formula version/factor evidence
- legacy protected-media token references have an idempotent owner-path migration policy and tests
- every Android Screen source is exhaustively classified with a CI gate that prevents hidden/STUB/UNSAFE/POST_LAUNCH exposure drift
- release-candidate privacy/legal copy was reconciled to the implemented Firebase/Google Play/Nearby/media architecture; CI rejects legacy E2EE/Razorpay/BCrypt/no-GPS claims
- role-scoped operations provisioning has a dry-run-first audited bootstrap that preserves unrelated custom claims and revokes refresh tokens after changes
- production rollback has an explicit safe-off/maintenance/release-rollback runbook tied to the existing Remote Config kill switches

## Exact-head evidence

This document deliberately does **not** hard-code a supposedly current green SHA. Any documentation
commit would immediately make that SHA stale. The authoritative exact-head result is the latest
`Production CI` run and its SHA-labelled evidence artifacts for the candidate commit.

Mandatory CI jobs are:

1. Android source/integrity gates + unit tests + lint + debug build + non-production release/R8
   validation + exact-SHA evidence.
2. Emulator-backed Room migration matrix across every registered supported migration hop.
3. Firebase Functions lint + build/typecheck + tests + production dependency audit.
4. Firestore + Storage emulator security-rules tests.

A production handoff is repository-GO only when all four jobs are green on the **same current head**.
Historical green runs never certify a later commit.

## Remaining repository verification before release

These are evidence/verification tasks that cannot be honestly marked complete merely because the
corresponding code path exists:

- execute the full two-user/two-device matrix for interests, chat, notification/deep-link, block,
  privacy-revocation, discovery, payment recovery and deletion
- supplement the automated large-account deletion/retry coverage with production-like data-volume
  evidence and forced infrastructure/provider failure drills
- supplement the registered Room migration matrix with real historical production database fixtures
  where such released database versions actually exist; destructive migration remains debug-only
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

The repository-side exact-head CI baseline is green at the SHA recorded above, but this branch and
PR #22 must remain Draft until all required external/operator gates have current evidence. Code
volume, screenshots, historical green commits or a rendering Compose screen are not completion
evidence.
