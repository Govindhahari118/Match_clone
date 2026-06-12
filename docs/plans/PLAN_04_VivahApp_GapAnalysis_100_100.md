# PLAN 04 — VivahApp_GapAnalysis_100_100.docx
## End-to-End Pin-to-Pin Implementation Plan (17 Critical Gaps)

> **Source:** `docs/VivahApp_GapAnalysis_100_100.docx`  
> **Purpose:** 17 critical gaps that can block launch, delay revenue, or get app removed from stores  
> **Tech Stack Adaptation:** All Cloudflare/D1/Workers references → Firebase/Firestore/Cloud Functions  
> **Priority:** Sections A = Launch Blockers (must fix first)

---

## SECTION A — LAUNCH BLOCKERS (Fix Before Any Sprint Proceeds)

### A1. Legal Entity & Payment Setup

| Req | Requirement | Status | Sprint | Task |
|-----|-------------|--------|--------|------|
| LGL-01 | Registered Indian business entity (LLP/Pvt Ltd) | ❌ Not Done | **PRE-SPRINT** | Register company. Razorpay requires this. |
| LGL-02 | GST number | ❌ Not Done | **PRE-SPRINT** | File GST registration |
| LGL-03 | Business bank account | ❌ Not Done | **PRE-SPRINT** | Open current account linked to business entity |
| LGL-04 | Razorpay production activation (not test mode) | 🟡 Partial | Sprint 10 | Complete KYC docs on Razorpay dashboard |
| LGL-05 | Trademark "MATCH" or app name under Class 45 | ❌ Not Done | Sprint 10 | File trademark (₹4,500/class, optional but protects brand) |
| LGL-06 | Privacy Policy page (live URL) | 🟡 Partial | Sprint 10 | Update `privacy-policy.html` for DPDP Act 2023 |

### A2. Authentication — Correct Implementation

> Doc identified auth as "Sprint 0 dependency" — every other feature depends on identity.  
> **Our stack uses Firebase Auth** — adaptation below.

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| AUTH-01 | Mobile OTP (primary) | ✅ Done | Sprint 1 | `AuthRepository.kt` |  |
| AUTH-02 | OTP: max 3 attempts, then 10-min lockout | 🟡 Partial | Sprint 10 | `AuthRepository.kt` | Add attempt counter + lockout in Firestore |
| AUTH-03 | JWT access token (Firebase Auth ID Token: RS256, 1h TTL) | ✅ Done | Sprint 1 | Firebase Auth managed |  |
| AUTH-04 | Refresh token (Firebase auto-managed, 30 days) | ✅ Done | Sprint 1 | Firebase Auth managed |  |
| AUTH-05 | Google Sign-In | ✅ Done | Sprint 1 | `AuthRepository.kt` |  |
| AUTH-06 | Apple Sign-In (mandatory for iOS App Store) | ❌ Not Done | Sprint 14 (iOS) | — | Required by Apple Guideline 4.8 |
| AUTH-07 | Device binding (session tagged to device fingerprint) | ❌ Not Done | Sprint 11 | `AuthRepository.kt` | Store `deviceId` in Firestore session doc |
| AUTH-08 | Admin auth: separate Firebase project + TOTP mandatory | ❌ Not Done | Sprint 15 | Firebase project config | Separate admin Firebase project |
| AUTH-09 | Session revocation on logout (Firebase token revocation) | ❌ Not Done | Sprint 10 | Cloud Function | Call `firebase.auth().revokeRefreshTokens(uid)` |
| AUTH-10 | Separate OTP audit log in Firestore | ❌ Not Done | Sprint 10 | New: `otpLog/{hash}` Firestore collection | IP + attempt tracking |

### A3. App Store Compliance (Launch Blocker)

#### Google Play Requirements

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| PS-01 | 18+ age declaration in store listing | ❌ Not Done | Sprint 12 | Play Store console | Set content rating: Adults Only |
| PS-02 | In-app safety page: block, report, emergency contact | 🟡 Partial | Sprint 12 | New: `SafetyCenterScreen.kt` | Must exist before submission |
| PS-03 | Human moderation process described in store listing | ❌ Not Done | Sprint 12 | Store listing copy | Add moderation description |
| PS-04 | AI + human photo review process in store description | ❌ Not Done | Sprint 12 | Store listing copy | Describe both review types |
| PS-05 | Age verification mechanism declared | ❌ Not Done | Sprint 12 | `SignUpScreen.kt` | DOB field mandatory + 18+ validation |
| PS-06 | Contact email live before submission | ❌ Not Done | **PRE-SPRINT** | — | Set up support email |
| PS-07 | Privacy policy URL live before submission | 🟡 Partial | Sprint 12 | `privacy-policy.html` | Deploy to Firebase Hosting |

#### Apple App Store Requirements (Future iOS)

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| AS-01 | Apple Sign-In if ANY other social login present | ❌ Not Done | Sprint 14 | iOS build | Mandatory — no exceptions |
| AS-02 | In-app reporting mechanism before submission | ❌ Not Done | Sprint 12 | `SafetyCenterScreen.kt` | Block + Report must exist |
| AS-03 | Account deletion in-app (not just via email) | ✅ Done | Sprint 9 | Cloud Function `deleteUserAccount` |  |
| AS-04 | No misleading screenshots (no verified badge on unverified profiles) | ❌ Not Done | Sprint 12 | Store assets | Audit all screenshots |
| AS-05 | Subscription pricing displayed clearly in-app before purchase | 🟡 Partial | Sprint 10 | `SubscriptionScreen.kt` | All prices visible before Razorpay checkout |

---

## SECTION B — CLOUDFLARE HARD LIMITS (Adapted to Firebase)

> Original doc identified Cloudflare free-tier bottlenecks. Below is the equivalent analysis for Firebase free/Blaze plan.

### B1. Firebase Free Tier vs. Blaze Plan Analysis

| Service | Free Tier Limit | When You Hit It | Firebase Blaze Cost | Action |
|---------|----------------|----------------|---------------------|--------|
| Firestore reads | 50K reads/day | ~2,500 DAU (20 reads/session) | $0.06/100K reads | Switch to Blaze before 1,000 DAU |
| Firestore writes | 20K writes/day | ~1,000 DAU (20 writes/session) | $0.18/100K writes | Switch to Blaze before 500 DAU |
| Firebase Storage | 5GB storage | ~5,000 profiles at 3 photos | $0.026/GB/month | Compress photos before upload |
| Cloud Functions | 125K invocations/month | ~4,000 DAU | $0.40/million | Monitor in Firebase console |
| FCM | Unlimited | Never | Free | No issue |
| Firebase Auth | Unlimited | Never | Free | No issue |

**Action Required:** Set up billing alerts in Firebase console before launch. Status: ❌ Not Done, Sprint 10.

### B2. Room DB Limitations (Equivalent to D1 Limitations)

> Firebase equivalent of D1 limitations.

| Limitation | Design Workaround | Status | Sprint | Task |
|------------|-------------------|--------|--------|------|
| No full-text search in Firestore | Use Algolia or Cloud Firestore search extension | ❌ Not Done | Sprint 13 | Add Algolia integration |
| Firestore query limit (1 inequality filter per query) | Use composite indexes + Firestore 500-doc limit | ✅ Done | Sprint 9 | Composite index added |
| No real-time subscriptions on queries > 30 sec | Use Firestore snapshots for real-time, pagination for lists | ✅ Done | Sprint 9 | Paging 3 + Firestore |
| Single-region Firestore write latency | Choose nearest region (asia-south1 for India) | ❌ Not Done | Sprint 10 | Verify Firestore region is `asia-south1` |
| Room DB migration safety | `fallbackToDestructiveMigration()` gated on DEBUG only | ✅ Done | Sprint 8 | BuildConfig.DEBUG gate |

---

## SECTION C — MISSING DATABASE TABLES (Firebase Collections Equivalent)

> Doc identified 8 missing tables. Below is the Firebase Firestore equivalent.

| Missing Table (Original D1) | Firestore Collection Equivalent | Status | Sprint | Task |
|-----------------------------|--------------------------------|--------|--------|------|
| `otp_log` | `otpLog/{hash}` — IP, attempt count, expires, used flag | ❌ Not Done | Sprint 10 | Create Firestore OTP log in `AuthRepository.kt` |
| `sessions` | `sessions/{uid}/devices/{deviceId}` — token hash, lastActive, revoked | ❌ Not Done | Sprint 10 | Server-side session tracking |
| `verification_requests` | `verifications/{uid}` — type, document, status, reviewer | ❌ Not Done | Sprint 12 | `VerificationRepository.kt` |
| `notifications` | `notifications/{uid}/items/{notifId}` — type, body, read_at | ❌ Not Done | Sprint 11 | Full notification log in Firestore |
| `notification_prefs` | `notificationPrefs/{uid}` — per-type toggles | ❌ Not Done | Sprint 11 | `NotificationPrefsScreen.kt` |
| `feature_flags` | Already: Firebase Remote Config | ✅ Done | Sprint 9 | `RemoteConfigManager.kt` |
| `audit_events` | `auditLog/{eventId}` — actor, action, target, metadata, timestamp | ❌ Not Done | Sprint 12 | DPDP Act compliance requirement |
| `community_groups` | `communities/{groupId}` | 🟡 Partial | Sprint 9 | `FirestoreFeatureService.kt` communities |

### Critical Indexes (Firestore Composite Index Equivalents)

| Collection | Index Fields | Status | Sprint | Task |
|-----------|-------------|--------|--------|------|
| `notifications` | `userId, readAt` | ❌ Not Done | Sprint 11 | Add to `firestore.indexes.json` |
| `users` | `gender, ageBucket, lastActiveAt` | ✅ Done | Sprint 9 | Already in `firestore.indexes.json` |
| `users` | `religion, caste, lastActiveAt` | ❌ Not Done | Sprint 10 | Add to `firestore.indexes.json` |
| `users` | `countryOfResidence, gender, lastActiveAt` | ❌ Not Done | Sprint 10 | Add for NRI search |
| `interests` | `toUid, status, createdAt` | ❌ Not Done | Sprint 11 | Add to `firestore.indexes.json` |
| `auditLog` | `actorId, timestamp` | ❌ Not Done | Sprint 12 | Add on creation |

---

## SECTION D — NOTIFICATION SYSTEM (Highest Retention Lever)

> Doc identifies notification strategy as "#1 retention feature" — entirely missed in both previous docs.

### D1. Notification Type Matrix

| Trigger | Message & Timing | Channel | Status | Sprint | File(s) | Task |
|---------|-----------------|---------|--------|--------|---------|------|
| New Interest Received | Immediate: "[Name] is interested ❤️" | Push + In-app | 🟡 Partial | Sprint 11 | `FirebaseNotificationService.kt` | Verify immediate trigger |
| Interest Accepted | Immediate: "[Name] accepted! Say hello 👋" | Push + In-app | 🟡 Partial | Sprint 11 | `FirebaseNotificationService.kt` | Add acceptance trigger |
| New Message | Immediate if app closed, 30-sec delay if open | Push + In-app | 🟡 Partial | Sprint 7 | `FirebaseNotificationService.kt` | Add app-state delay logic |
| Profile Viewed | Batched at 7 PM: "5 views today" | Push only | ❌ Not Done | Sprint 11 | Cloud Function scheduled | Batch view count, send at 7PM |
| New Match Available | Daily at 9 AM: "12 new matches ✨" | Push + Email | ❌ Not Done | Sprint 11 | Cloud Function daily scheduler | Schedule 9AM daily match digest |
| Profile Incomplete (Day 2) | "Add photo for 3x more matches" (if <70%) | Push + Email | ❌ Not Done | Sprint 11 | Cloud Function D+2 trigger | Check completeness 2 days post-signup |
| Profile Incomplete (Day 7) | Push + Email | Push + Email | ❌ Not Done | Sprint 11 | Cloud Function D+7 trigger | Same, D+7 |
| Inactive User (Day 7) | "[Name] from [City] just joined!" | Push + Email | ❌ Not Done | Sprint 12 | Cloud Function | Check `lastActiveAt` D+7 |
| Inactive User (Day 14) | Weekly digest of top 5 matches | Email only | ❌ Not Done | Sprint 12 | Cloud Function | Email digest via Firebase Email extension |
| Inactive User (Day 30) | "Profile hidden — reactivate" | Email + SMS | ❌ Not Done | Sprint 12 | Cloud Function | Auto-hide + notify |
| Subscription Expiring | 7d / 3d / 1d before expiry | Push + Email | ❌ Not Done | Sprint 11 | Cloud Function | Razorpay subscription end - alert |
| Verification Approved | Immediate: "Badge added!" | Push + Email | ❌ Not Done | Sprint 12 | Cloud Function on verification status change |  |
| Boost Active/End | Start: "Boost live!"; End: summary | Push only | ❌ Not Done | Sprint 11 | Cloud Function | `boostedUntil` timestamp trigger |

### D2. Notification Architecture Rules

| Rule | Status | Sprint | File(s) | Task |
|------|--------|--------|---------|------|
| FCM device tokens stored in Firestore `users/{uid}.fcmToken` | ✅ Done | Sprint 9 | `UserEntity.kt` |  |
| Frequency cap: max 3 push/user/day | ❌ Not Done | Sprint 11 | Cloud Function | Counter in Firestore or Remote Config |
| Quiet hours: no push 10 PM – 8 AM | ❌ Not Done | Sprint 11 | Cloud Function | Check user timezone before dispatch |
| User timezone stored in profile | ❌ Not Done | Sprint 10 | `UserEntity.kt` | Add `timezone` field |
| Notification preferences per-type | ❌ Not Done | Sprint 11 | New: `NotificationPrefsScreen.kt` | Per-type toggle settings screen |
| Email via Firebase Email Extension (Resend.com alternative) | ❌ Not Done | Sprint 12 | Firebase Extensions | Install Trigger Email extension |
| SMS (OTP + critical only, NOT marketing) | 🟡 Partial | Sprint 1 | Firebase Auth SMS |  |

---

## SECTION E — ONBOARDING FUNNEL & COLD-START STRATEGY

### E1. Onboarding Funnel Engineering

| Funnel Stage | Drop-Off Risk | Intervention | Status | Sprint | File(s) | Task |
|-------------|--------------|-------------|--------|--------|---------|------|
| Install → Registration Start | 30% abandon | "Start in 2 minutes" promise; no email at Step 1 | 🟡 Partial | Sprint 10 | `SplashScreen.kt` | Add promise headline on splash |
| Registration → Mobile Verify | 15% abandon | WhatsApp OTP fallback | ❌ Not Done | Sprint 11 | `AuthRepository.kt` | Add WhatsApp OTP as fallback |
| Mobile Verify → Basic Profile | 25% abandon | 3-field MVP (Name, DOB, Gender) only at this step | 🟡 Partial | Sprint 10 | `SignUpScreen.kt` | Reduce Step 1 to 3 fields only |
| Basic Profile → Photo Upload | 40% abandon | Show BLURRED match previews BEFORE photo upload | ❌ Not Done | Sprint 10 | `PhotoUploadScreen.kt` | "Upload photo to reveal matches" curiosity gap |
| Photo Upload → Partner Prefs | 20% abandon | AI pre-fills partner prefs from user's own profile | ❌ Not Done | Sprint 11 | `ProfileWizardScreen.kt` | Auto-suggest prefs from user data |
| Partner Prefs → First Match | 10% abandon | Show match queue IMMEDIATELY after prefs | ✅ Done | Sprint 9 | `HomeScreen.kt` |  |
| Match View → First Interest | 50% abandon | AI icebreaker suggestion on match card with one-tap send | ❌ Not Done | Sprint 11 | `ProfileCard.kt` | Add icebreaker suggestion chip |

### E2. Cold-Start Problem (Existential Risk for New App)

| Phase | Strategy | Status | Sprint | Task |
|-------|---------|--------|--------|------|
| Phase 0: Seed Community | Pick ONE community + ONE city. 500 profiles before launch. Offer 6 months free. | ❌ Not Done | Pre-launch | Business/marketing task — 250M + 250F verified in one community |
| Phase 1: Controlled Expansion | Open 3 more communities in same city | ❌ Not Done | Post-launch | Never expand community + city simultaneously |
| Phase 2: Viral Loop | Shagun Board™ success stories → 50-200 organic signups per story | ❌ Not Done | Sprint 15 | New: `SuccessStoriesScreen.kt` |
| Phase 3: Network Effect | 1,000+ profiles per community = self-sustaining | ❌ Not Done | Growth KPI | Track per-community density |
| Never show empty state | Always show blurred preview profiles | ❌ Not Done | Sprint 10 | `HomeScreen.kt` | Add preview profiles for empty states |

### E3. Competitive Switch Strategy

| Strategy | Status | Sprint | Task |
|---------|--------|--------|------|
| Pain-point targeting (fake profiles, aggressive sales calls, expensive paywall) | ❌ Not Done | Sprint 15 | Marketing copy + app store description |
| Free profile import from Shaadi/BharatMatrimony URL | ❌ Not Done | Sprint 14 | New: `ProfileImportScreen.kt` — AI pre-fill from public URL |
| "Bring Your Match" referral (both migrate = 3 months free) | ❌ Not Done | Sprint 14 | `MatchmakerReferralScreen.kt` — add cross-platform referral logic |
| Influencer strategy (astrologers, marriage counsellors) | ❌ Not Done | Post-launch | Business/marketing |
| WhatsApp community groups (admin-posted weekly matches) | ❌ Not Done | Post-launch | Operations task |

---

## SECTION F — SEO ARCHITECTURE (Organic Traffic)

> Firebase-hosted web presence to capture organic matrimony search traffic.

| Req | SEO Page Type | URL Pattern | Status | Sprint | Task |
|-----|--------------|-------------|--------|--------|------|
| SEO-01 | Community listing | `/matrimony/telugu-brahmin-brides` | ❌ Not Done | Sprint 15 | Firebase Hosting + Next.js profile pages |
| SEO-02 | City + community | `/matrimony/telugu-brahmin-hyderabad` | ❌ Not Done | Sprint 15 | Dynamic static generation |
| SEO-03 | Individual profile page | `/profile/{slug}` | ❌ Not Done | Sprint 15 | Server-side profile page with Schema.org |
| SEO-04 | Search results (indexable popular combos) | `/search?religion=hindu&caste=reddy&city=hyderabad` | ❌ Not Done | Sprint 15 | React + Firebase Hosting |
| SEO-05 | Success stories | `/weddings` | ❌ Not Done | Sprint 15 | `SuccessStoriesScreen.kt` + web page |
| SEO-06 | Blog / advice | `/blog` | ❌ Not Done | Sprint 15 | Firebase Hosting blog |
| SEO-07 | Schema.org Person markup on profile pages | — | ❌ Not Done | Sprint 15 | JSON-LD in profile page template |
| SEO-08 | Open Graph tags for WhatsApp profile sharing | — | ❌ Not Done | Sprint 15 | OG meta tags on profile pages |

---

## SECTION G — SAFETY & COMPLIANCE (Gap Analysis Additions)

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| SAFE-01 | Immutable audit trail (DPDP Act 2023 requirement) | ❌ Not Done | Sprint 12 | `auditLog/{eventId}` Firestore | Log all critical actions (block, report, delete) |
| SAFE-02 | Data export on user request | ❌ Not Done | Sprint 14 | Cloud Function `exportUserData` | ZIP + download link |
| SAFE-03 | Right to erasure (full cascade delete) | 🟡 Partial | Sprint 9 | Cloud Function `deleteUserAccount` | Verify all collections cascade |
| SAFE-04 | Consent purpose recording | ❌ Not Done | Sprint 12 | `MainActivity.kt` | Consent dialog on first launch, logged to Firestore |
| SAFE-05 | Grievance officer details in app | ❌ Not Done | Sprint 12 | `SettingsScreen.kt` | Add grievance contact |
| SAFE-06 | Anti-spam rate limiting in Firestore rules | ❌ Not Done | Sprint 11 | `firestore.rules` | Add rate limit rules (max 10 writes/min per user) |
| SAFE-07 | Photo pHash duplicate detection | ❌ Not Done | Sprint 12 | Cloud Function `onPhotoUpload` | Add perceptual hash comparison |

---

## SECTION H — PERFORMANCE TARGETS (From Gap Analysis)

| Metric | Target | Status | Sprint | Task |
|--------|--------|--------|--------|------|
| App cold start | < 2 seconds on mid-range Android | 🟡 Partial | Sprint 10 | Baseline Profiles + startup optimization |
| Profile card render | < 100ms | ❌ Not Done | Sprint 10 | Coil image pipeline profiling |
| Interest send to notification | < 1 second end-to-end | ❌ Not Done | Sprint 11 | Verify FCM delivery latency |
| Match page load | < 500ms | 🟡 Partial | Sprint 9 | Verify Room cache hit rate |
| Firestore region | `asia-south1` (Mumbai) | ❌ Not Done | Sprint 10 | Verify/set Firestore region |

---

## SPRINT ALLOCATION SUMMARY

| Sprint | Priority | Key Deliverables |
|--------|---------|-----------------|
| **PRE-SPRINT** | Launch blocker | Company registration, bank account, Razorpay KYC, support email |
| Sprint 10 | Launch blocker | OTP lockout, session revocation, store compliance prep, firebase region, billing alerts |
| Sprint 11 | Retention | Full notification system (12 types), frequency cap, quiet hours, notification prefs screen |
| Sprint 12 | Trust + Legal | Audit log, DPDP compliance, photo pHash, Safety Center, verification_requests collection |
| Sprint 13 | Growth | Cold-start seed content, blurred preview profiles, AI icebreaker on match card, Algolia |
| Sprint 14 | Scale | Profile import, cross-platform referral, WhatsApp OTP fallback, data export |
| Sprint 15 | SEO & Web | Firebase Hosting + web profile pages + SEO pages + success stories |

---

## DEFINITION OF DONE (10/10 Checklist)

- [ ] Company registered, GST active, Razorpay KYC approved for production
- [ ] Privacy policy deployed as live URL, updated for DPDP Act 2023
- [ ] Play Store compliance: 18+ declaration, Safety Center screen, age verification in-app
- [ ] OTP: max 3 attempts + 10-min lockout enforced
- [ ] Session revocation working on logout
- [ ] All 12 notification types implemented with frequency cap (max 3/day) and quiet hours
- [ ] Notification preferences screen (per-type toggles)
- [ ] Full audit trail in Firestore (DPDP Act)
- [ ] Photo pHash duplicate detection in Cloud Function
- [ ] Cold-start solved: blurred preview profiles shown for empty states
- [ ] AI icebreaker suggestion on match card
- [ ] Firebase region confirmed as `asia-south1`
- [ ] Firebase billing alerts configured
- [ ] All missing Firestore composite indexes added
- [ ] Data export (right to portability) Cloud Function working
