# Matree competitive P0/P1 audit — 2026-10-01

## Scope and release boundary

This audit compares the current Matree source/release architecture with leading matrimonial products and a safety-focused marriage/dating benchmark. It separates repository-controlled gaps from production/operator/provider work.

- Frozen production candidate: `5a4cda4087e5b49706dd6ea02b30d3d8d744eae5`
- Frozen branch: `release/matree-prod-2026-10-01`
- Post-freeze hardening branch: `gpt/matree-competitive-p0-p1-hardening-20261001`
- Any source change after the frozen SHA requires fresh exact-head Production CI and fresh external evidence before it can replace the frozen candidate.
- A source file existing in the repository does not make a feature production-ready. Reachability, server authority, provider readiness, security, observability and real-device evidence all matter.

## Benchmarks used

Primary feature claims are taken from first-party or store listings current during the audit:

- Shaadi.com support: https://support.shaadi.com/support/solutions/articles/48000953202-what-additional-benefits-do-i-get-as-a-premium-member-
- Shaadi Calls: https://support.shaadi.com/support/solutions/articles/48001159195-what-is-shaadi-meet-how-does-it-work-
- BharatMatrimony Google Play: https://play.google.com/store/apps/details?id=com.bharatmatrimony
- Jeevansathi: https://www.jeevansathi.com/
- Jeevansathi Google Play: https://play.google.com/store/apps/details?id=com.jeevansathi.android
- Betterhalf Google Play: https://play.google.com/store/apps/details?id=com.betterhalf
- Muzz safety: https://muzz.com/us/en/help/safety-and-privacy/how-does-muzz-keep-users-safe/

Store reviews are treated only as anecdotal product signals, not representative population evidence.

## Matree strengths to preserve

1. Bilateral STRICT partner-preference enforcement before discovery and again before interest writes.
2. Reciprocal A→B/B→A preference scoring where mutual fit is the weaker directional score rather than an average.
3. 90%+ Strong Mutual Match requires a minimum evidence count on both sides.
4. Income can participate in trusted matching without being exposed as a public discovery field.
5. Consent-aware personalization and sensitive-preference lifecycle.
6. Server-authoritative interests, matches, chat, billing entitlements, account lifecycle and privacy relations.
7. Cross-device relationship state and durable/idempotent chat retry.
8. Protected media moderation, private risk signals and audit-oriented operator workflows.
9. Time-limited/revocable Family Access with server restrictions and auditability.
10. Provider-sensitive capabilities fail closed instead of exposing simulated success.
11. Data export, deletion, pause, session revocation and privacy controls exist in the real production surface.
12. Private precise presence with relationship-aware visibility.

## P0 — public launch blockers

These are release blockers, not optional competitive improvements.

- Protect `main` and require Production CI before merge.
- Create immutable release tag for the chosen exact production SHA.
- Dedicated production Firebase project and release `google-services.json`.
- Production Auth provider configuration plus release SHA-1/SHA-256 fingerprints.
- Deploy Firestore rules, indexes, Storage rules and Functions from the exact frozen SHA.
- Production Firebase secrets/runtime configuration.
- App Check + Play Integrity signed-device validation before enforcement.
- FCM token/deep-link matrix on physical devices.
- Crashlytics alerts and Analytics production verification.
- Production operator/RBAC bootstrap and approved master-data seed.
- Play App Signing, production product catalog/pricing, service account, Developer API and RTDN.
- Signed release AAB, artifact hash and exact-source provenance.
- Licensed Play Billing matrix: purchase, pending, restore, replay/idempotency, renewal, expiry, cancellation, refund and revocation.
- Fresh-install, upgrade-install, multi-device, process-death and poor-network matrices.
- Accessibility device matrix, performance SLO evidence and penetration/security review.
- Privacy Policy, Terms, refund language, Data Safety, account-deletion declaration, content rating, listing assets and support/escalation readiness.
- Play pre-launch report, closed testing, rollback drill, staged rollout and post-rollout health gate.
- Confirm all unvalidated provider-sensitive surfaces remain OFF/fail-closed.

## Provider-dependent P0 if the feature is enabled

These are not blockers while the feature remains hidden/off, but are P0 prerequisites for enabling it.

- Selfie verification provider.
- Liveness provider.
- Face similarity provider.
- Aadhaar/approved government-ID provider path.
- Secure masked voice/video provider, relay/TURN, abuse controls and billing/privacy integration.
- Independent Kundali reference validation.
- Nearby physical-device/privacy/load validation.

## P1 — product/trust gaps

### Implemented in the 2026-10-01 hardening branch

- **Screen-capture protection:** Android `FLAG_SECURE`, ON by default, with transparent user control.
- **Stale profile hygiene:** profiles with a trustworthy activity/creation anchor older than 90 days are suppressed from discovery by trusted backend logic; precise presence remains private.
- **Quiet hours:** optional 22:00–07:00 local-time push suppression; durable in-app events remain and critical safety/verification alerts bypass it.
- **Personal introduction with interest:** optional 280-character note; URLs, email, phone and payment/money-request bypasses are blocked before mutual match; recipient sees the note.
- **Repeated-profile rotation:** recent consented recommendation-impression batches apply a small novelty penalty while reciprocal preference fit remains dominant.
- **Release documentation drift:** current lineage replaces old branch identity; historic Razorpay instructions are explicitly superseded by Google Play Billing.

### Still open

1. **Mandatory/strong selfie authenticity tier.** Muzz requires selfie verification; Betterhalf advertises selfie + government-ID verification. Matree currently has generic manual document review but advanced selfie/liveness/face-similarity providers remain unavailable.
2. **Production secure voice/video calling.** Shaadi, Jeevansathi and BharatMatrimony expose secure calls; Matree's secure-call surface remains hidden until a real provider and privacy/safety architecture are configured.
3. **Video profile production launch.** Jeevansathi and Muzz expose profile video; Matree has a BETA/off implementation pending production validation.
4. **Profile-management identity.** Family Access is strong, but Matree does not yet show whether a matrimonial profile is managed by self, parent, sibling or guardian.
5. **Assisted matchmaking depth.** Matree has assisted-service request/status foundations, but not a full relationship-manager workflow with advisor identity, assignment, SLA, curated shortlist, follow-up log and meeting coordination comparable to personalized plans.
6. **Live/community matchmaking events.** Jeevansathi advertises Match Hour; Matree's Live Events route remains post-launch/hidden.
7. **Boost/featured placement production surface.** Major competitors sell/promote spotlight/featured visibility; Matree contains boost foundations but the standalone production surface remains hidden.
8. **Kundali production readiness.** Server-owned compatibility exists but remains off pending independent validation; major Indian competitors expose mature horoscope/kundli flows.
9. **Terminal success lifecycle.** Add a first-class “found my match / married” close-search state, optional success-story consent and reactivation policy instead of relying only on generic pause/delete.
10. **Notification controls beyond quiet hours.** Add custom schedules, frequency caps/digest controls and per-conversation controls without suppressing critical notices.
11. **Engagement-quality signals.** Add carefully governed response/seriousness/reliability signals with appeal/moderation safeguards; never create opaque social-credit scoring.
12. **Search novelty and exhaustion UX.** The new recent-impression penalty helps rotation; add explicit “seen”, “new since last visit”, result exhaustion and transparent broaden/relax controls.
13. **Verification freshness.** Add expiry/re-verification policy for long-lived identity documents and profile-photo/selfie refresh where legally appropriate.
14. **Support SLA visibility.** Competitor reviews frequently criticize post-purchase support. Matree needs measurable ticket SLA, escalation state and entitlement/billing support runbooks visible to operators.
15. **Trust explanation.** Continue surfacing why a profile is verified/matched while keeping fraud signals private.

## P1 — technical/platform gaps

1. **No iOS client in this repository.** Leading competitors serve iOS as well as Android.
2. **No production web/SEO acquisition surface.** Major incumbents have indexable community/city/religion landing pages and desktop web journeys.
3. **Verified HTTPS App Links still require a production domain and `assetlinks.json`.**
4. **No dedicated search service.** Firestore-backed discovery is appropriate for the release scale, but advanced full-text/faceted search, typo tolerance, synonym/community dictionaries, geo ranking and large-catalog query observability may eventually justify OpenSearch/Algolia/Typesense or equivalent.
5. **No mature learning-to-rank platform.** Current personalization is deliberately bounded and explainable. Future LTR should remain subordinate to hard eligibility/privacy constraints and require offline evaluation, holdouts, bias/fairness checks and rollback.
6. **Experimentation platform is incomplete.** Need server-assigned experiments, exposure logging, guardrail metrics, holdouts, sequential-test discipline and kill switches.
7. **Search/ranking observability needs deeper funnel metrics:** eligible pool size, filter attrition, novelty, repetition, response rate, mutual conversion, block/report rate and latency by cohort without exposing sensitive traits.
8. **Provider orchestration is incomplete** for KYC biometrics and communications.
9. **Production SLO/error-budget practice is not yet externally evidenced.**
10. **Data warehouse/offline quality loop** is not established for long-horizon ranking/support/fraud analysis.
11. **Web/iOS cross-platform contract testing** does not exist because those clients do not yet exist.
12. **Branch governance:** `main` was observed unprotected during this audit; repository admin settings must enforce required checks/reviews.

## Competitor feature comparison

| Capability | Shaadi | BharatMatrimony | Jeevansathi | Betterhalf | Muzz safety benchmark | Matree |
|---|---|---|---|---|---|---|
| Advanced partner filters | Yes | Yes | Yes | Yes | Yes | Yes; bilateral STRICT + reciprocal fit |
| Verified profiles | Yes | Prime | Screened/verified | Selfie/ID advertised | Mandatory selfie | Manual govt-ID foundation; biometrics provider pending |
| In-app voice/video | Yes | Voice / SecureConnect | Voice/video | Not the primary benchmark claim | Voice/video | Hidden pending provider |
| Masked/number-private calls | Shaadi Calls/privacy controls | SecureConnect | Secure calls | — | In-app calling | Pending provider |
| Video profiles | — | — | Yes | — | Yes | BETA/off |
| Personal note with request | Premium interest/contact messaging | Personalized messages advertised | First-message/interest patterns | Yes | Icebreakers | Implemented safely in hardening branch |
| Featured/boost | Yes | Yes | Paid boost tiers | Crown/Sparkle/24h boost | Premium visibility features | Foundation exists; public surface hidden |
| Relationship advisor | Personalized plans | Assisted services vary | Premium services vary | Adjacent services | — | Assisted foundation; full RM workbench/SLA missing |
| Family involvement | Profile management common | Family-led model | Family-led model | — | Chaperone/Wali | Strong audited Family Access; manager identity missing |
| Screenshot blocking | Not primary public claim | Not primary public claim | Not primary public claim | Not primary public claim | Yes | Implemented in hardening branch |
| Stale-profile suppression | Not transparently documented | Review pain signal | Review pain signal | Review pain signal | — | 90-day backend suppression in hardening branch |
| Quiet hours | Not primary public claim | Not primary public claim | Not primary public claim | Not primary public claim | — | Implemented in hardening branch |
| Web acquisition | Mature | Mature/community portals | Mature | Web presence | Web presence | Production SEO/web app missing |
| iOS | Yes | Yes | Yes | Yes | Yes | Missing |

## User-signal themes to exploit, not copy

Current Play Store reviews are noisy/anecdotal, but recurring themes across competitors are strategically useful:

- Users value simple navigation, usable filters and straightforward connection flows.
- Users dislike repeated profiles and stale/inactive inventory.
- Users react badly when paid “boost” or visibility claims are opaque or hard to verify.
- Irrelevant recommendations despite explicit preferences destroy trust quickly.
- Chat outages and delayed messages are especially damaging after payment.
- Incomplete/fake profiles and weak verification reduce willingness to engage.
- Slow/no post-purchase support magnifies every billing or matching problem.
- Profile-level private notes are remembered as useful by some long-time users.

Matree should compete on **truthful relevance, freshness, privacy, verification quality and reliable communication** before expanding into wedding-commerce breadth.

## P2 — post-launch growth / ecosystem

- Wedding planning, photography and vendor marketplace.
- Astrologer marketplace after Kundali validation.
- Love/relationship coaching and counselling.
- Success stories and consented social proof.
- Referral program and rewards.
- Community/live matchmaking events.
- First-class remarriage/second-marriage discovery experience.
- Advanced horoscope/muhurat tools.
- AI icebreakers/conversation assistance with safety constraints.
- Desktop/web product and SEO content engine.
- Additional languages and regional editorial/community content.
- iOS client.
- Ethical premium discovery products with transparent delivery metrics.

## Execution order

1. Finish all P0 external launch evidence on the frozen candidate without changing it.
2. Land this post-freeze P1 hardening only after exact-head CI is green.
3. Decide whether the hardening branch becomes a new release candidate; if yes, freeze a new SHA and rerun every SHA-bound external gate.
4. Complete identity/provider verification before exposing selfie/liveness/Aadhaar claims.
5. Complete secure calling provider architecture before exposing calls.
6. Build profile-manager identity, success lifecycle, support SLA and notification controls.
7. Build web/iOS/search/experimentation foundations.
8. Add P2 ecosystem features only after core matching, trust, chat and support metrics are stable.
