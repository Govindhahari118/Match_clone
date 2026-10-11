# Matree: 100-feature end-to-end delivery plan (engineering tracker)

**Date:** 2026-10-10  
**Scope:** `Govindhahari118/Match_clone` (Matrii/Matree matrimony), Android + Firebase Auth / Firestore / Storage / Cloud Functions + Room. Not agriculture Agree_clone.

## Source baseline and honesty contract

- Integration reference: draft PR #60, `docs/production-readiness/IMPLEMENTATION_EXECUTION_2026-10-10.md` on its head. Rebase this branch onto its *reviewed* merged main before code implementation; this document must not override the release gates already there.
- PR #60 includes server-owned photo visibility/consent, missing-photo requests, chat improvements, match-call *coordination* and EN/HI/TE notifications. Scheduling does **not** mean functional live voice/video transport. No real service may be enabled before security and two-device sign-off.
- No feature below is marked 'missing' based only on absence in a competitor list. **Status defaults to AUDIT_REQUIRED** until actual route, Function, Firestore authorization, client state, test and running build are inspected. Evidence means file paths + test run/SHA + screenshot or device log.
- Current production readiness: **NO-GO**. Respect independent review, privacy law, Play Billing and real-device checks; do not fabricate evidence.

## Definition of done for *each* requirement

1. Product behavior and roles specified; accessibility/localization strings defined (EN/HI/TE at minimum).
2. Database fields, indexing, migrations and server-only access enforced; security rules deny cross-user access.
3. Android UX includes loading, empty, error, retry, offline, blocked/deleted user, and small/large screen states.
4. Functions/API have validation, auth, abuse limits, idempotency and logging without PII spills.
5. Unit + Firebase emulator rules + integration + two-device E2E tests pass on exact commit SHA; relevant negative/security tests included.
6. Screenshots/recordings and QA sign-off linked; docs and release notes updated; no P0/P1 unresolved.

## Sequencing and ownership

| Stage | Delivery | Responsible | Completion condition |
|---|---|---|---|
| 0 | Inventory + evidence baseline | Engineer+QA | Map every feature to existing Android component, Function, Firestore rule, test, screenshot; mark implemented/partial/missing/blocked; inventory roles and safe defaults. |
| 1 | Identity and permissions | Android+Functions | Auth flows, age gate, role-specific guardian consent, duplicate detection, test-user separation, explicit deletion and privacy notice. |
| 2 | Profile and moderated media | Android+Functions+Moderation | Profile schema, profile completeness, media uploads, grants/revokes, video, protected storage, fail-closed cached media handling. |
| 3 | Discovery and match engine | Backend+Android | Indexed filters, pagination, saved searches, exclusion logic, explainable two-way scoring, ranking fairness, availability freshness. |
| 4 | Interest and messaging | Backend+Android | Mutual-interest state machine, realtime chat, receipts, mute/archive, block, media, retry/idempotency, offline handling. |
| 5 | Verified secure communication | Backend+Mobile+Security | Real provider selected and integrated with ephemeral tokens, abuse controls, replay-safe webhook handling, two-device voice/video tests. Disabled until certified. |
| 6 | Family and cultural workflows | Product+Backend+Android | Guardian RBAC, consent boundaries, Telugu/Hindi content, biodata and horoscope opt-in, shared shortlists, meeting consent. |
| 7 | Trust and safety operations | Backend+Ops+Security | ID/liveness as appropriate, report queues, audit, moderation appeals, retention/delete enforcement, abuse rate limiting. |
| 8 | Billing and concierge | Android+Backend+Ops | Play Billing authority, purchases/renewals/refunds, replay-safe RTDN, entitlements, invoicing, promotions, optional staffed service. |
| 9 | Engagement and lifecycle | Android+Backend | Locale-aware opted-in push/digest, quiet-hours, accurate activity data, referral abuse limits, deletion-safe notifications. |
| 10 | Admin, accessibility, performance | Android+Ops+QA | Support admin RBAC, analytics with privacy budget, EN/HI/TE a11y QA, slow/offline/low-end testing, tracing. |
| 11 | Production gates and rollout | Security+Release | Independent review, exact-head CI all jobs, Firebase/App Check, signing, provider, Play, physical-device and legal proof, GO_NO_GO, staged rollback. |

## 100-feature execution tracker

*Status values: AUDIT_REQUIRED, EXISTS_TEST_NEEDED, PARTIAL, MISSING, BLOCKED_EXTERNAL, IMPLEMENTED_VERIFIED. Evidence must be attached before changes.*

| ID | Domain | Feature | Status | Evidence/owner/test |
|---|---|---|---|---|
| MAT-001 | 01 Identity/onboarding | Phone OTP login | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-002 | 01 Identity/onboarding | Email sign-in | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-003 | 01 Identity/onboarding | Google/Apple sign-in | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-004 | 01 Identity/onboarding | Self/parent/guardian registration | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-005 | 01 Identity/onboarding | Age eligibility checks | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-006 | 01 Identity/onboarding | Progressive setup | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-007 | 01 Identity/onboarding | Save/resume onboarding | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-008 | 01 Identity/onboarding | Multilingual onboarding | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-009 | 01 Identity/onboarding | Duplicate-account detection | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-010 | 01 Identity/onboarding | Consent and privacy acknowledgement | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-011 | 02 Profiles | Personal biodata | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-012 | 02 Profiles | Education/employment | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-013 | 02 Profiles | Income privacy | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-014 | 02 Profiles | Religion/community/subcaste | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-015 | 02 Profiles | Gotra/native place | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-016 | 02 Profiles | Family contacts and details | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-017 | 02 Profiles | Lifestyle/hobbies/values | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-018 | 02 Profiles | Profile completeness | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-019 | 02 Profiles | Private photo album and selective consent | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-020 | 02 Profiles | Video introductions | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-021 | 03 Discovery | Basic search | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-022 | 03 Discovery | Age and height | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-023 | 03 Discovery | Religion/community/caste | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-024 | 03 Discovery | Education/profession | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-025 | 03 Discovery | City/state/NRI/distance | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-026 | 03 Discovery | Marital status/children | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-027 | 03 Discovery | Lifestyle/family values | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-028 | 03 Discovery | Saved and recent searches | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-029 | 03 Discovery | Active/new profiles | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-030 | 03 Discovery | Exclude declined/blocked/previously contacted | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-031 | 04 Recommendations | Personalized daily matches | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-032 | 04 Recommendations | Mutual preference fit | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-033 | 04 Recommendations | Explainable score | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-034 | 04 Recommendations | Behavior-based ranking | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-035 | 04 Recommendations | Dealbreaker rules | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-036 | 04 Recommendations | Optional horoscope scoring | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-037 | 04 Recommendations | Conversation starters | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-038 | 04 Recommendations | Feedback loop | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-039 | 04 Recommendations | Inactive profile downranking | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-040 | 04 Recommendations | Fairness and bias controls | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-041 | 05 Communication | Interests accept/decline | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-042 | 05 Communication | Intro messages | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-043 | 05 Communication | Realtime chat | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-044 | 05 Communication | Delivery/read indicators | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-045 | 05 Communication | Privacy-safe voice calls | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-046 | 05 Communication | Privacy-safe video calls | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-047 | 05 Communication | Call availability/scheduling | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-048 | 05 Communication | Voice notes/media protections | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-049 | 05 Communication | Explicit contact exchange | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-050 | 05 Communication | Block/mute/disconnect | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-051 | 06 Safety/privacy | Phone/email verification | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-052 | 06 Safety/privacy | Selfie/liveness | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-053 | 06 Safety/privacy | Minimized ID checks | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-054 | 06 Safety/privacy | Impersonation/duplicate detection | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-055 | 06 Safety/privacy | Visibility settings | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-056 | 06 Safety/privacy | Blur/reveal consent | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-057 | 06 Safety/privacy | Reporting and abuse cases | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-058 | 06 Safety/privacy | Scam risk flags | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-059 | 06 Safety/privacy | Screenshot/watermark and cached-media policy | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-060 | 06 Safety/privacy | Export/deletion | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-061 | 07 Family/culture | Parent accounts | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-062 | 07 Family/culture | Guardian access boundaries | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-063 | 07 Family/culture | Printable biodata | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-064 | 07 Family/culture | Horoscope uploads | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-065 | 07 Family/culture | Hindi/Telugu and other languages | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-066 | 07 Family/culture | Community taxonomy | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-067 | 07 Family/culture | Family-approved introduction | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-068 | 07 Family/culture | Meeting scheduling | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-069 | 07 Family/culture | Shared shortlist | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-070 | 07 Family/culture | Optional consensual chaperone | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-071 | 08 Revenue | Free vs paid entitlements | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-072 | 08 Revenue | Monthly/quarterly/annual plans | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-073 | 08 Revenue | Play Billing and applicable web payments | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-074 | 08 Revenue | Offers/referrals | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-075 | 08 Revenue | Profile boost | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-076 | 08 Revenue | Verified premium tier | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-077 | 08 Revenue | Renewals/receipts | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-078 | 08 Revenue | Cancellation/refunds | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-079 | 08 Revenue | Restore/revoke/limit enforcement | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-080 | 08 Revenue | Assisted concierge | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-081 | 09 Retention | Push preferences | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-082 | 09 Retention | Email/WhatsApp consent | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-083 | 09 Retention | Match digests | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-084 | 09 Retention | Profile visitor insights | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-085 | 09 Retention | Saved matches | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-086 | 09 Retention | Activity reminders | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-087 | 09 Retention | Reactivation | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-088 | 09 Retention | Married status/success stories | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-089 | 09 Retention | Post-introduction feedback | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-090 | 09 Retention | Alert frequency controls | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-091 | 10 Operations | Moderation console | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-092 | 10 Operations | Review/appeal workflows | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-093 | 10 Operations | Fraud logs and audit | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-094 | 10 Operations | Support tickets | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-095 | 10 Operations | Entitlement administration | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-096 | 10 Operations | Matching analytics | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-097 | 10 Operations | Conversion funnel | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-098 | 10 Operations | Feature flags/experiments | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-099 | 10 Operations | A11y/localization QA | AUDIT_REQUIRED | _record code/test/SHA_ |
| MAT-100 | 10 Operations | Security/perf/release observability | AUDIT_REQUIRED | _record code/test/SHA_ |

## Contract schema / data boundaries (proposed; reconcile before migrations)

- `users/{uid}`: Auth identity, lifecycle (active/suspended/deleted), locale, consent versions and per-installation FCM registrations. Never expose ID documents to discovery.
- `profiles/{uid}`: moderated public biodata, indexed partner preferences and server-owned visibility flags; private fields stored separately and read only by owner or narrow Functions.
- `matches/{pairId}`, `interests/{id}`: deterministic ordered pair key; server-checked state machine, block as overriding veto.
- `messages/{threadId}/items/{id}`: mutually authorized participants, bounded content and upload policies; revoke access on block/deletion.
- `photoAccessRequests` and `photoRequests` stay independent (per PR #60); never collapse them.
- `callRequests`: request/accept/decline/cancel coordination; transport session separate and provider-issued only after fresh authorization.
- `subscriptions/{uid}`, `purchaseEvents/{eventId}`: entitlement derived from verified payment authority; replay-safe event application.
- `reports/{id}`, `adminAudits/{id}`: strict staff RBAC, immutable event trail and retention policy; least privilege.
- Never collect IDs, caste, religion, precise location, intimate values or horoscope by default; require valid purpose, consent and controls. Age minimum must follow the relevant law/policy and be enforced server-side.

## Reference E2E journeys to automate

1. New adult user → OTP → consent → onboarding → save/resume → edit → published moderated profile → first eligible match.
2. Two eligible users → mutual interest → match → chat → send/read → block → all media, calls, notifications and search no longer grant access.
3. Protected photo published → viewer request → owner grants → authorized read → owner revokes → future reads denied, explaining already-downloaded bytes cannot be recalled.
4. Accepted match → propose call → accept → provider tokens minted server-side → two physical devices connect → hang up; rejected/blocked/replayed request fails.
5. Parent with granted scope → view shared shortlist → cannot read private chats or documents without explicit authorization.
6. Paid purchase → server confirmation → entitlement → app reinstall/restore → renew/grace/refund/revoke → correct final access under duplicate/reordered events.
7. Locale EN/HI/TE switch → notifications/cards/deep links after process death/account switch preserve language and privacy.
8. Delete account → Auth and server records/media/notifications scrubbed per retention policy → old session denied → exported data and audit handling verified.
9. Abuse report → moderator review with separation of duties → action/appeal → no leaked reporter identity.
10. Slow network, notification denied, offline, process death, retries, old app version, screen reader, large text, lower-tier device.

## Engineering implementation loop

For each ID: (1) inspect current implementation and existing tests; (2) record observed state and gaps; (3) write failing unit/rules/E2E test; (4) implement smallest safe vertical slice; (5) run exact-head CI; (6) security/privacy review; (7) real-device evidence; (8) update tracker to IMPLEMENTED_VERIFIED only on proof. Commit PRs by bounded domains, not a single unreviewable mega-PR. No direct changes to production config, billing products, call provider, release signing or merge policy without real owner validation.

## Metrics and release gates

- Coverage target: **100/100 audited**, and **100/100 IMPLEMENTED_VERIFIED** only if accepted as in-scope. Unselected optional product features must be explicitly DEFERRED with rationale and never counted as delivered.
- Product funnel: verified onboarding completion, valid daily matches, two-sided interest acceptance, useful conversations, blocks/reports, successful purchases, refund and cancellation rates. Aggregate/anonymize where appropriate.
- Quality: no open P0/P1, crash/ANR within product release policy, critical auth/rules tests green, no privacy leakage, no live call without provider certification.
- External production handoff remains governed by existing PR #60 execution document and release issue #26. **Do not equate green CI with production GO.**
