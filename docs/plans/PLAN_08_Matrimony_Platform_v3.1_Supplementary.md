# PLAN 08 — Matrimony_Platform_v3.1_Supplementary.pdf
## End-to-End Pin-to-Pin Implementation Plan

> **Source:** `docs/Matrimony_Platform_v3.1_Supplementary.pdf`  
> **Context:** Supplementary additions to the v3 Complete spec. This document contains  
>   corrections, additions, and refinements that were missing from v3.0 —  
>   specifically: edge cases, advanced features, compliance additions, and production hardening  
> **Architecture:** Cloudflare-based — adapted to Firebase  
> **Focus:** What v3.0 missed — advanced safety, analytics, A/B testing, compliance edge cases

---

## OVERLAP NOTE  
> PLAN_08 supplements PLAN_07. Read both together. PLAN_08 covers the delta between v3.0 and v3.1.

---

## STATUS LEGEND
- ✅ **Done** — Implemented in Sprints 1–9
- 🟡 **Partial** — Started but incomplete
- ❌ **Not Done** — Zero implementation

---

## SECTION 1 — ADVANCED SAFETY FEATURES (v3.1 Additions)

> v3.1 supplements safety features that were described at high level in v3.0.

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| SF-01 | Emergency SOS button (GPS + trusted contact alert) | ❌ Not Done | Sprint 13 | `SafetyCenterScreen.kt` | SOS button → `LocationManager.getCurrentLocation()` → FCM to emergency contact |
| SF-02 | Trusted contact registration | ❌ Not Done | Sprint 13 | New: `EmergencyContactScreen.kt` | Store in Firestore `users/{uid}.emergencyContact` |
| SF-03 | 24-hour cooling period before block becomes visible | ❌ Not Done | Sprint 12 | `FirestoreBlockService.kt` | Add `blockActiveAfter` timestamp |
| SF-04 | Automatic hide after 3 reports from different users | ❌ Not Done | Sprint 12 | Cloud Function `onReportCreate` | Count reports → auto-flag profile |
| SF-05 | Human review queue for flagged profiles | ❌ Not Done | Sprint 15 | Admin panel + Firestore `moderationQueue` | Admin SDK Cloud Function |
| SF-06 | "Safe Date" checklist feature | ❌ Not Done | Sprint 13 | `SafetyCenterScreen.kt` | Checklist: share plans, verify ID, meet public place |
| SF-07 | Anti-grooming pattern detection (in messages) | ❌ Not Done | Sprint 13 | Cloud Function `onMessageCreate` | Regex + ML pattern for grooming scripts |
| SF-08 | Profile link sharing safety (watermarked, no phone) | ❌ Not Done | Sprint 12 | `ProfileDetailScreen.kt` | Dynamic link → watermarked screenshot, no personal data |
| SF-09 | Location spoofing detection | ❌ Not Done | Sprint 13 | `AuthRepository.kt` | Compare GPS location vs profile city |
| SF-10 | Two-person consent for contact reveal | 🟡 Partial | Sprint 8 | `MatchDetailScreen.kt` | Both must explicitly confirm |

---

## SECTION 2 — ADVANCED ANALYTICS & A/B TESTING (v3.1 Additions)

> v3.1 adds specific analytics events and A/B testing framework.

### 2.1 Analytics Events (All Required)

| Event Name | Trigger | Status | Sprint | File(s) | Task |
|------------|---------|--------|--------|---------|------|
| `registration_start` | SignUpScreen opens | ❌ Not Done | Sprint 11 | `SignUpScreen.kt` | `FirebaseAnalytics.logEvent("registration_start")` |
| `registration_complete` | Account created | ❌ Not Done | Sprint 11 | `AuthRepository.kt` | Log on successful signup |
| `profile_photo_uploaded` | Photo upload complete | ❌ Not Done | Sprint 11 | `PhotoEditorScreen.kt` | Log upload completion |
| `profile_complete_50` | Completeness hits 50% | ❌ Not Done | Sprint 11 | `ProfileCompletenessUtil.kt` | Log on threshold cross |
| `profile_complete_100` | Completeness hits 100% | ❌ Not Done | Sprint 11 | `ProfileCompletenessUtil.kt` | Log on 100% |
| `interest_sent` | User sends interest | ✅ Done | Sprint 2 | `FirestoreInterestService.kt` | Already in place |
| `super_interest_sent` | Super interest sent | ✅ Done | Sprint 9 | `SocialRepository.kt` |  |
| `match_accepted` | Interest accepted | ❌ Not Done | Sprint 11 | `FirestoreInterestService.kt` | Log on accept |
| `chat_initiated` | First message in new chat | ❌ Not Done | Sprint 11 | `ChatRepository.kt` | Log first message |
| `subscription_purchased` | Razorpay payment success | ❌ Not Done | Sprint 11 | `SubscriptionRepository.kt` | Log with plan type + amount |
| `subscription_expired` | Plan expires | ❌ Not Done | Sprint 12 | Cloud Function | Log on expiry |
| `profile_viewed` | User views another's profile | ❌ Not Done | Sprint 11 | `ProfileDetailScreen.kt` | Log profile view |
| `search_performed` | Search with filters | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Log with filter count |
| `daily_match_opened` | User opens daily match card | ❌ Not Done | Sprint 11 | `HomeScreen.kt` | Log on card tap |
| `verification_started` | User starts ID verification | ❌ Not Done | Sprint 12 | `VerificationScreen.kt` | Log on start |
| `verification_completed` | Verification approved | ❌ Not Done | Sprint 12 | Cloud Function | Log on approval |
| `boost_purchased` | Profile boost bought | ❌ Not Done | Sprint 11 | `BoostRepository.kt` | Log on purchase |
| `voice_call_started` | Voice call initiated | ❌ Not Done | Sprint 12 | `SecureCallScreen.kt` | Log on call start |
| `kundali_viewed` | Kundali match viewed | ❌ Not Done | Sprint 11 | `KundliScreen.kt` | Log on view |
| `app_inactive_7d` | 7 days since last open | ❌ Not Done | Sprint 12 | Cloud Function | Track churn signal |

### 2.2 Funnel Analytics (7-Stage Funnel)

| Funnel Stage | Metric | Status | Sprint | Task |
|-------------|--------|--------|--------|------|
| Install | App installs | ✅ Done | 1 | Firebase Analytics installs |
| Registration | % who complete signup | ❌ Not Done | Sprint 11 | Track `registration_start` vs `registration_complete` |
| Profile completion | % who reach 70% | ❌ Not Done | Sprint 11 | Track `profile_complete_50` + `profile_complete_100` |
| First Interest | % who send first interest | ❌ Not Done | Sprint 11 | Track `interest_sent` Day 1 |
| First Match | % who get first acceptance | ❌ Not Done | Sprint 11 | Track `match_accepted` |
| First Conversation | % who start first chat | ❌ Not Done | Sprint 11 | Track `chat_initiated` |
| Subscription | % who subscribe | ❌ Not Done | Sprint 11 | Track `subscription_purchased` |

### 2.3 A/B Testing Framework

| Test | Hypothesis | Status | Sprint | File(s) | Task |
|------|-----------|--------|--------|---------|------|
| AB-01 | "Upload photo to reveal matches" vs "Complete profile" onboarding hook | ❌ Not Done | Sprint 12 | `HomeScreen.kt` + Remote Config | A/B via `RemoteConfigManager.kt` |
| AB-02 | Icebreaker shown on match card vs chat initiation screen | ❌ Not Done | Sprint 12 | Remote Config + `ProfileCard.kt` | Feature flag for placement |
| AB-03 | "5 interests/day" limit shown upfront vs discovered after hitting limit | ❌ Not Done | Sprint 12 | Remote Config + `HomeScreen.kt` | Test conversion impact |
| AB-04 | Subscription CTA after 3rd interest vs after first match | ❌ Not Done | Sprint 12 | `SubscriptionRepository.kt` + Remote Config | Test paywall placement |

---

## SECTION 3 — COMPLIANCE EDGE CASES (v3.1 Additions)

> v3.1 adds specific DPDP Act 2023 + IT Rules 2021 compliance requirements.

### 3.1 DPDP Act 2023 (India Data Protection Digital Act)

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| DPDP-01 | Notice of purpose at registration (consent dialog) | ❌ Not Done | Sprint 12 | `MainActivity.kt` + `SignUpScreen.kt` | Show consent dialog on first launch |
| DPDP-02 | Purpose limitation (data used only for matchmaking) | ❌ Not Done | Sprint 12 | Privacy policy update | Document purpose in privacy policy |
| DPDP-03 | Data minimization (collect only what's needed) | ❌ Not Done | Sprint 12 | All screens | Audit all form fields for necessity |
| DPDP-04 | Right to access (download my data) | ❌ Not Done | Sprint 14 | Cloud Function `exportUserData` | ZIP all user data |
| DPDP-05 | Right to correction (edit profile) | ✅ Done | Sprint 2 | `ProfileEditScreen.kt` |  |
| DPDP-06 | Right to erasure (delete account) | ✅ Done | Sprint 9 | Cloud Function `deleteUserAccount` |  |
| DPDP-07 | Right to grievance (grievance officer contact) | ❌ Not Done | Sprint 12 | `SettingsScreen.kt` | Add grievance officer name + email |
| DPDP-08 | Data fiduciary registration (if required >20 lakh users) | ❌ Not Done | Post-launch | Business/legal | Monitor user count |
| DPDP-09 | Audit trail of all data processing activities | ❌ Not Done | Sprint 12 | `auditLog/{eventId}` Firestore | Log all critical operations |
| DPDP-10 | Children's data protection (under 18 blocked) | ❌ Not Done | Sprint 12 | `SignUpScreen.kt` | DOB validation: min 18 years |
| DPDP-11 | Cross-border data transfer disclosure | ❌ Not Done | Sprint 12 | Privacy policy | Disclose Firebase US servers |

### 3.2 IT Rules 2021 (Intermediary Guidelines)

| Req | Requirement | Status | Sprint | Task |
|-----|-------------|--------|--------|------|
| IT-01 | Grievance Officer appointed (Indian resident) | ❌ Not Done | Sprint 12 | Business/legal + app settings update |
| IT-02 | Grievance redressal within 72 hours | ❌ Not Done | Sprint 12 | Support process + `SettingsScreen.kt` |
| IT-03 | Content takedown within 36 hours on court order | ❌ Not Done | Sprint 15 | Admin panel + Cloud Function |
| IT-04 | Monthly compliance report for platforms with 50L+ users | ❌ Not Done | Post-launch | Automated compliance dashboard |
| IT-05 | Retain content removal records for 180 days | ❌ Not Done | Sprint 12 | `auditLog` Firestore | Log all content actions |

### 3.3 Play Store Policy (Dating Apps)

| Req | Requirement | Status | Sprint | Task |
|-----|-------------|--------|--------|------|
| PLAY-01 | 18+ content rating declared | ❌ Not Done | Sprint 12 | Play Console rating declaration |
| PLAY-02 | Safety features screen in app | ❌ Not Done | Sprint 12 | `SafetyCenterScreen.kt` |
| PLAY-03 | Human moderation described in store listing | ❌ Not Done | Sprint 12 | Store listing copy |
| PLAY-04 | Block and report clearly accessible | ✅ Done | Sprint 5 | `FirestoreBlockService.kt` |  |
| PLAY-05 | No misrepresentation of verification state in screenshots | ❌ Not Done | Sprint 12 | Audit store screenshots |
| PLAY-06 | Account deletion available in-app | ✅ Done | Sprint 9 | Cloud Function `deleteUserAccount` |  |

---

## SECTION 4 — PERFORMANCE HARDENING (v3.1 Additions)

| Req | Target | Status | Sprint | File(s) | Task |
|-----|--------|--------|--------|---------|------|
| PERF-01 | Baseline Profiles for startup optimization | ❌ Not Done | Sprint 11 | `baselineProfile.baseline` | Add Baseline Profile generation |
| PERF-02 | R8 full mode (not just ProGuard) | 🟡 Partial | Sprint 10 | `app/build.gradle.kts` | Enable `android.enableR8.fullMode=true` |
| PERF-03 | APK size < 20MB | ❌ Not Done | Sprint 11 | `app/build.gradle.kts` | Enable ABI splits + bundle |
| PERF-04 | AAB (Android App Bundle) for Play Store | 🟡 Partial | Sprint 12 | `build-release.bat` | Use `bundleRelease` instead of `assembleRelease` |
| PERF-05 | Coil image caching strategy (disk + memory) | 🟡 Partial | Sprint 9 | All screens | Set explicit cache policy |
| PERF-06 | Lazy loading of non-critical screens | ✅ Done | Sprint 9 | Navigation | Compose navigation lazy |
| PERF-07 | Firebase Performance custom traces | 🟡 Partial | Sprint 11 | Key screens | Add traces for profile load, search, chat |
| PERF-08 | Firestore offline persistence tuned | 🟡 Partial | Sprint 9 | `AppModule.kt` | Set cache size to 100MB |
| PERF-09 | Paging 3 prefetch distance tuned | 🟡 Partial | Sprint 9 | `FirestorePagingSource.kt` | Set prefetchDistance = 10 |
| PERF-10 | Memory leak detection in debug builds | ❌ Not Done | Sprint 11 | `app/build.gradle.kts` | Add LeakCanary debug dependency |

---

## SECTION 5 — ADVANCED NOTIFICATION STRATEGY (v3.1 Additions)

| Req | Advanced Feature | Status | Sprint | File(s) | Task |
|-----|-----------------|--------|--------|---------|------|
| NOT-01 | Notification channels (Android 8+): Match/Chat/Safety/Marketing | ❌ Not Done | Sprint 11 | `FirebaseNotificationService.kt` | Create 4 notification channels |
| NOT-02 | Per-channel notification preferences | ❌ Not Done | Sprint 11 | `NotificationPrefsScreen.kt` | Map prefs to channels |
| NOT-03 | Rich notifications (profile photo in notification) | ❌ Not Done | Sprint 11 | `FirebaseNotificationService.kt` | Add BigPictureStyle with profile photo |
| NOT-04 | Notification grouping by contact | ❌ Not Done | Sprint 11 | `FirebaseNotificationService.kt` | Group messages by sender with `setGroup()` |
| NOT-05 | Action buttons on notifications (Accept/Decline interest) | ❌ Not Done | Sprint 12 | `FirebaseNotificationService.kt` | Add action intents on interest notification |
| NOT-06 | Notification attribution tracking | ❌ Not Done | Sprint 12 | `FirebaseNotificationService.kt` | Track which notification drove app open |
| NOT-07 | Unsubscribe from marketing notifications (Play policy) | ❌ Not Done | Sprint 12 | `NotificationPrefsScreen.kt` | Must be able to opt out of all non-critical |

---

## SECTION 6 — ADVANCED MONETIZATION (v3.1 Additions)

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| MON-01 | Annual plan with 20% discount | ❌ Not Done | Sprint 11 | `SubscriptionScreen.kt` | Show monthly vs annual toggle |
| MON-02 | "Try Premium free for 7 days" trial | ❌ Not Done | Sprint 12 | `SubscriptionScreen.kt` | Razorpay trial period config |
| MON-03 | Re-subscription win-back offer (30% off on lapse) | ❌ Not Done | Sprint 13 | Cloud Function + FCM | Trigger on subscription lapse |
| MON-04 | Referral ₹500 credit system | 🟡 Partial | Sprint 9 | `MatchmakerReferralScreen.kt` | Wire credit to Razorpay account balance |
| MON-05 | In-app tokens for à la carte features | ❌ Not Done | Sprint 13 | New: `TokenRepository.kt` | Token balance + purchase flow |
| MON-06 | Black Friday / festival offers | ❌ Not Done | Sprint 14 | Remote Config + `SubscriptionScreen.kt` | Dynamic pricing via Remote Config |
| MON-07 | Promo code system | ❌ Not Done | Sprint 13 | New: `PromoCodeRepository.kt` | Razorpay coupon codes |

---

## SECTION 7 — ADVANCED MATCHING FEATURES (v3.1 Additions)

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| ADV-01 | "Dealbreaker" preferences (hard limits user never compromises on) | ❌ Not Done | Sprint 11 | `ProfileEditScreen.kt` | Mark 2–3 preferences as dealbreakers |
| ADV-02 | Match Timeline milestones | ❌ Not Done | Sprint 13 | New: `MatchTimelineScreen.kt` | Record: interest→chat→call→family intro |
| ADV-03 | "On Hold" status for a match (pause conversation without block) | ❌ Not Done | Sprint 12 | `ChatRepository.kt` | Add `onHold` status to chat thread |
| ADV-04 | Mutual friends detection (from LinkedIn) | ❌ Not Done | Sprint 15 | Optional — LinkedIn OAuth | Future feature |
| ADV-05 | "Focus Mode" — hide all new matches, work on existing connections | ❌ Not Done | Sprint 13 | `SettingsScreen.kt` | Toggle to pause new match discovery |
| ADV-06 | Profile quality score (AI-rated: 1–10 on photo quality, bio depth) | ❌ Not Done | Sprint 12 | New: `ProfileQualityEngine.kt` | Rate photo quality + bio completeness |
| ADV-07 | "Pause profile" (hide completely for holiday/busy period) | ❌ Not Done | Sprint 11 | `SettingsScreen.kt` | `pausedUntil` timestamp in Firestore |

---

## SPRINT ALLOCATION SUMMARY

| Sprint | Focus | Key Deliverables |
|--------|-------|-----------------|
| Sprint 10 | Schema + build | `androidR8.fullMode`, R8 full mode, APK size optimization |
| Sprint 11 | Analytics + performance | All Firebase Analytics events, 7-stage funnel, notification channels, Baseline Profiles, LeakCanary |
| Sprint 12 | Compliance + safety | DPDP Act consent dialog, Play Store rating, safety features, 18+ DOB check, audit log |
| Sprint 13 | Advanced features | Dealbreakers, profile pause, Match Timeline, win-back offers, token system |
| Sprint 14 | Monetization depth | Annual plans, promo codes, 7-day trial, referral credit, festival offers |
| Sprint 15 | Admin + SEO | Human moderation queue, compliance dashboard, IT Rules implementation |

---

## DEFINITION OF DONE (10/10 Checklist)

- [ ] All 20 Firebase Analytics events implemented and firing in Firebase console
- [ ] 7-stage funnel visible in Firebase Analytics dashboard
- [ ] DPDP Act compliance: consent dialog, audit log, export, grievance officer
- [ ] 4 notification channels created with per-channel preferences
- [ ] Rich notifications with profile photos
- [ ] Action buttons on interest notifications (Accept/Decline)
- [ ] Emergency SOS button with trusted contact flow
- [ ] Automatic hide after 3 reports from different users
- [ ] Anti-grooming pattern detection in messages
- [ ] Annual plan (20% discount) available in subscription screen
- [ ] 7-day free trial flow in Razorpay
- [ ] Profile pause feature ("pause until date")
- [ ] "Dealbreaker" preferences implemented
- [ ] R8 full mode enabled, APK < 20MB
- [ ] Baseline Profiles added to CI
- [ ] A/B testing framework via Remote Config for 4 experiments
- [ ] Play Store compliance: 18+ rating, safety screen, no misrepresentation
