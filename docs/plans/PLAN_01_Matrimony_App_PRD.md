# PLAN 01 — Matrimony_App_PRD.docx
## End-to-End Pin-to-Pin Implementation Plan

> **Source:** `docs/Matrimony_App_PRD.docx`  
> **Tech Stack:** Kotlin + Jetpack Compose, Firebase (Auth/Firestore/Storage/Functions/FCM), Room, Hilt, Razorpay  
> **Current State:** Sprints 1–9 complete. DB v16. BUILD SUCCESSFUL.  
> **Plan Scope:** Every requirement in this PRD mapped to specific files, sprint assignments, and status.

---

## STATUS LEGEND
- ✅ **Done** — Implemented in Sprints 1–9
- 🟡 **Partial** — Started but incomplete
- ❌ **Not Done** — Not yet implemented

---

## SECTION 1 — EXECUTIVE SUMMARY REQUIREMENTS

| Req | Requirement | Status | Sprint | File(s) |
|-----|-------------|--------|--------|---------|
| ES-01 | AI-powered matchmaking platform | 🟡 Partial | Sprint 10 | `data/remote/FirestoreProfileService.kt` |
| ES-02 | Deep cultural understanding (caste, gotra, manglik) | ✅ Done | Sprint 8 | `UserEntity.kt`, `SeedProvider.kt` |
| ES-03 | Verified, compatible life partner focus | 🟡 Partial | Sprint 10 | `FakeProfileDetector.kt` |
| ES-04 | Faster, safer, more private than existing solutions | 🟡 Partial | Sprint 10+ | Multiple screens |

---

## SECTION 2 — USER PERSONAS (Implementation Requirements)

### Persona 1: Arjun (IT Engineer, 28, Bangalore) — Tech-savvy
| Req | Requirement | Status | File(s) |
|-----|-------------|--------|---------|
| P1-01 | Efficient modern UX with swipe interface | ✅ Done | `SwipeDiscoveryScreen.kt` |
| P1-02 | AI match score displayed on cards | ❌ Not Done | `DiscoveryCard.kt` — add compatibility score |
| P1-03 | Video call within app | ❌ Not Done | New: `SecureCallViewModel.kt` + WebRTC |
| P1-04 | Clean profile cards without clutter | ✅ Done | `ProfileCard.kt` |

### Persona 2: Priya (MBA, 26, Mumbai) — Privacy-first
| Req | Requirement | Status | File(s) |
|-----|-------------|--------|---------|
| P2-01 | Photo privacy controls | 🟡 Partial | `PrivacyDashboardScreen.kt` |
| P2-02 | Masked calling (number never revealed) | ❌ Not Done | `FirestoreFeatureService.kt` — callRequest |
| P2-03 | Block/Report mechanism | ✅ Done | `FirestoreBlockService.kt` |
| P2-04 | Anti-screenshot protection (FLAG_SECURE) | ❌ Not Done | `MainActivity.kt` — add FLAG_SECURE |

### Persona 3: Sanjay & Meera (Parents, 50s, Hyderabad) — Family portal
| Req | Requirement | Status | File(s) |
|-----|-------------|--------|---------|
| P3-01 | Simple UI for non-tech-savvy users | ✅ Done | Material3 theme |
| P3-02 | Family portal / co-browsing | ❌ Not Done | New: `FamilyPortalScreen.kt` |
| P3-03 | Horoscope match display | 🟡 Partial | `KundliScreen.kt` |
| P3-04 | Profile for son/daughter registration flow | ❌ Not Done | `SignUpScreen.kt` — add "Profile For" step |

---

## SECTION 3 — FEATURE SPECIFICATIONS (Pin-to-Pin)

### 3.1 Onboarding & Registration

| ID | Feature | Status | Sprint | File(s) | Task |
|----|---------|--------|--------|---------|------|
| F-01 | Quick Registration via OTP/email/Google/Apple | 🟡 Partial | Sprint 10 | `SignUpScreen.kt`, `AuthRepository.kt` | Add Apple SSO |
| F-02 | Profile Wizard (5 steps: Personal→Education→Career→Family→Preferences) | ❌ Not Done | Sprint 10 | New: `ProfileWizardScreen.kt` | Create full multi-step wizard |
| F-03 | Family/Parent Profile ("Profile for son/daughter") | ❌ Not Done | Sprint 10 | `SignUpScreen.kt` | Add "Profile For" dropdown + consent checkbox |
| F-04 | Photo Upload with AI checks (no sunglasses, no groups, face visible) | 🟡 Partial | Sprint 11 | `PhotoEditorScreen.kt` | Add ML Kit face detection validation |
| F-05 | Personality Quiz (5 questions, lifestyle/values) | ❌ Not Done | Sprint 11 | New: `PersonalityQuizScreen.kt` | Create quiz + save to UserEntity |

### 3.2 Profile Management

| ID | Field | Status | Sprint | File(s) | Task |
|----|-------|--------|--------|---------|------|
| PM-01 | Full name, DOB, height, weight, complexion, mother tongue | 🟡 Partial | Sprint 10 | `UserEntity.kt` | Add `complexion`, `motherTongue`, `weight` columns + Migration_16_17 |
| PM-02 | Marital status (Single/Divorced/Widowed) | ✅ Done | Sprint 1 | `UserEntity.kt` |  |
| PM-03 | Education: degree, field, institute | 🟡 Partial | Sprint 10 | `UserEntity.kt` | Add `educationField`, `institute` columns |
| PM-04 | Career: profession, employer, annual income range | 🟡 Partial | Sprint 10 | `UserEntity.kt` | Add `employer`, `incomeRange` columns |
| PM-05 | Family: father's occupation, mother's occupation, siblings count, family type, family values | 🟡 Partial | Sprint 8 | `UserEntity.kt` | Add `fatherOccupation`, `motherOccupation`, `siblingsCount` |
| PM-06 | Religion, caste, sub-caste, gotra, manglik | ✅ Done | Sprint 8 | `UserEntity.kt` |  |
| PM-07 | Lifestyle: diet, smoking, drinking, hobbies, pets | 🟡 Partial | Sprint 5 | `UserEntity.kt` | Add `smoking`, `drinking`, `hobbies`, `pets` |
| PM-08 | Horoscope: Nakshatra, Rashi, time of birth | 🟡 Partial | Sprint 5 | `UserEntity.kt` | Add `nakshatra`, `rashi`, `birthTime` |
| PM-09 | Partner Preferences (mirrors profile fields with ranges) | 🟡 Partial | Sprint 5 | `UserEntity.kt` | Add all preference range fields |
| PM-10 | Video Introduction (30-sec, visible to verified matches) | 🟡 Partial | Sprint 9 | `VideoProfileScreen.kt` | Gate by verified status |
| PM-11 | Biodata PDF auto-generator | ❌ Not Done | Sprint 12 | New: `BiodataGenerator.kt` | Use iText/AndroidPDF library |
| PM-12 | Profile Completeness Score (0–100%, gamified) | ❌ Not Done | Sprint 11 | New: `ProfileCompletenessUtil.kt` | Calculate score across all fields |

### 3.3 Verification & Trust System (5 Levels)

| Level | Method | Badge | Status | Sprint | File(s) | Task |
|-------|--------|-------|--------|--------|---------|------|
| L1 | Phone OTP | 📱 Phone Verified | ✅ Done | 1 | `AuthRepository.kt` |  |
| L2 | Aadhaar/PAN/Passport/Driving Licence upload | 🛡️ ID Verified | ❌ Not Done | Sprint 12 | New: `VerificationRepository.kt` | DigiLocker API or manual upload |
| L3 | LinkedIn link or salary slip | 💼 Employment Verified | ❌ Not Done | Sprint 12 | New: `VerificationRepository.kt` | LinkedIn OAuth or document upload |
| L4 | Selfie liveness check vs ID photo | ✅ Photo Verified | ❌ Not Done | Sprint 12 | New: `LivenessCheckScreen.kt` | ML Kit face comparison |
| L5 | PRIME: All 4 + manual review | ⭐ PRIME Member | ❌ Not Done | Sprint 13 | Admin panel + backend Cloud Function |  |
| AI | Duplicate photo detection, bot patterns, scam detection | — | 🟡 Partial | Sprint 9 | `FakeProfileDetector.kt` | Add pHash duplicate detection |

### 3.4 Matchmaking Engine (AI Compatibility Score)

| ID | Dimension | Weight | Status | Sprint | File(s) | Task |
|----|-----------|--------|--------|--------|---------|------|
| ME-01 | Community & Religion match | 25% | 🟡 Partial | Sprint 10 | `FirestoreProfileService.kt` | Add weighted compatibility calculation |
| ME-02 | Education & Career compatibility | 20% | ❌ Not Done | Sprint 10 | New: `CompatibilityEngine.kt` | Score based on education level diff |
| ME-03 | Location & Lifestyle | 15% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` | Compare city/diet/habits |
| ME-04 | Partner Preference overlap | 20% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` | Check if user is within partner's ranges |
| ME-05 | Horoscope match (optional, can disable) | 10% | 🟡 Partial | Sprint 10 | `KundliScreen.kt` | Wire guna score to compatibility |
| ME-06 | Behavioral affinity | 10% | ❌ Not Done | Sprint 11 | New: `BehavioralSignalService.kt` | Track views/likes/skips patterns |
| ME-07 | Daily Matches — 5–10 curated push notifications (morning) | ❌ Not Done | Sprint 11 | `FirebaseNotificationService.kt` | Schedule daily match batch |
| ME-08 | MIMA-style algorithm (learns from skips/views/interests) | ❌ Not Done | Sprint 13 | New: `RecommendationEngine.kt` | Collaborative filtering |
| ME-09 | "Recommended Because" explanation on profile cards | ❌ Not Done | Sprint 11 | `DiscoveryCard.kt` | Add reason chip |

### 3.5 Search & Filters

| ID | Filter | Tier | Status | Sprint | File(s) | Task |
|----|--------|------|--------|--------|---------|------|
| SF-01 | Age range | Free | ✅ Done | 1 | `SearchFilters.kt` |  |
| SF-02 | Height range | Free | ✅ Done | 1 | `SearchFilters.kt` |  |
| SF-03 | Religion, caste, mother tongue | Free | ✅ Done | 1 | `SearchFilters.kt` |  |
| SF-04 | City/State/Country | Free | ✅ Done | 1 | `SearchFilters.kt` |  |
| SF-05 | Marital status, education level | Free | ✅ Done | 1 | `SearchFilters.kt` |  |
| SF-06 | Annual income range | Premium | 🟡 Partial | Sprint 10 | `SearchFilters.kt` | Add income range filter |
| SF-07 | Profession / employer type | Premium | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add profession filter |
| SF-08 | Family type & family values | Premium | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add family type filter |
| SF-09 | Complexion, diet preference | Premium | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add lifestyle filters |
| SF-10 | Manglik / Non-Manglik toggle | Premium | ✅ Done | Sprint 8 | `SearchFilters.kt` |  |
| SF-11 | NRI filter (by country) | Premium | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `countryOfResidence` field + filter |
| SF-12 | Verified profiles only toggle | Premium | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Filter by `verificationLevel >= 2` |
| SF-13 | Online in last 7/30 days toggle | Premium | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Filter by `lastActiveAt` |
| SF-14 | Profile photo mandatory toggle | Premium | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Filter by `photoUrl != null` |
| SF-15 | Save & name custom filter sets | Premium | ❌ Not Done | Sprint 11 | New: `SavedFilterRepository.kt` | Persist named filter sets to Firestore |

### 3.6 Communication Features

| ID | Feature | Tier | Status | Sprint | File(s) | Task |
|----|---------|------|--------|--------|---------|------|
| CF-01 | Express Interest (Like with preset message) | Free | ✅ Done | Sprint 2 | `FirestoreInterestService.kt` |  |
| CF-02 | In-App Messenger (read receipts, typing, emoji) | Premium | 🟡 Partial | Sprint 8 | `ChatRepository.kt` | Add typing indicator, read receipts |
| CF-03 | SecureConnect Voice Call (neither number revealed) | Premium | ❌ Not Done | Sprint 12 | `SecureCallScreen.kt` | Implement actual VoIP (Daily.co or WebRTC) |
| CF-04 | Video Call 1-on-1 HD within app | Premium | ❌ Not Done | Sprint 12 | `VirtualMeetScreen.kt` | Implement WebRTC video |
| CF-05 | LiveMatch Events (group speed-matchmaking) | Premium | ❌ Not Done | Sprint 14 | New: `LiveEventCallScreen.kt` | Agora or Jitsi SDK |
| CF-06 | Contact Unlock (requires consent) | Premium | 🟡 Partial | Sprint 8 | `MatchDetailScreen.kt` | Add consent flag before reveal |
| CF-07 | Smart Reply Suggestions (AI conversation starters) | Premium | 🟡 Partial | Sprint 9 | `BioGeneratorScreen.kt` | Apply to chat context |
| CF-08 | Message Request Filter | Free | ❌ Not Done | Sprint 11 | `ChatScreen.kt` | Add filter: accepted only / all verified / all |

### 3.7 Privacy & Safety

| ID | Feature | Status | Sprint | File(s) | Task |
|----|---------|--------|--------|---------|------|
| PS-01 | Photo Album Privacy (Everyone / Connected / No one) | 🟡 Partial | Sprint 10 | `PrivacyDashboardScreen.kt` | Enforce in photo display logic |
| PS-02 | Profile Visibility (Public / Members / Hidden/Stealth) | 🟡 Partial | Sprint 9 | `UserEntity.kt` `isIncognito` | Add `stealth` mode level |
| PS-03 | Last Seen / Online Status hide toggle | ❌ Not Done | Sprint 10 | `UserEntity.kt` | Add `showLastSeen` field |
| PS-04 | Block & Report with categories | ✅ Done | Sprint 5 | `FirestoreBlockService.kt` |  |
| PS-05 | Incognito Browse (view without appearing) | 🟡 Partial | Sprint 8 | `UserEntity.kt` `isIncognito` | Enforce in Firestore view tracking |
| PS-06 | Contact Detail Control (who can unlock) | 🟡 Partial | Sprint 8 | `MatchDetailScreen.kt` | Full consent flow |
| PS-07 | Anti-Screenshot (FLAG_SECURE on photo screens) | ❌ Not Done | Sprint 10 | `MainActivity.kt` | Add per-screen FLAG_SECURE |
| PS-08 | Profile Watermarking (platform name on photos) | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | Add watermark on upload |

### 3.8 Horoscope & Kundli

| ID | Feature | Status | Sprint | File(s) | Task |
|----|---------|--------|--------|---------|------|
| HK-01 | Auto Kundli Generation (DOB + time + place) | 🟡 Partial | Sprint 10 | `KundliScreen.kt` | Complete ephemeris integration |
| HK-02 | Kundli Match Report (36-point Guna) | 🟡 Partial | Sprint 5 | `KundliScreen.kt` | Add detailed per-category breakdown |
| HK-03 | Manglik Compatibility flag with explanation | ✅ Done | Sprint 8 | `UserEntity.kt` |  |
| HK-04 | Nadi Dosha alert with remedies (informational) | ❌ Not Done | Sprint 11 | `KundliScreen.kt` | Add Nadi Dosha check |
| HK-05 | Toggle Off horoscope-based matching | ❌ Not Done | Sprint 10 | `SettingsScreen.kt` | Add toggle to disable kundli in matching |

### 3.9 Family Portal

| ID | Feature | Status | Sprint | File(s) | Task |
|----|---------|--------|--------|---------|------|
| FP-01 | Parent sub-account linked to main profile | ❌ Not Done | Sprint 13 | New: `FamilyAccountRepository.kt` | Firestore subcollection families/{uid} |
| FP-02 | Controlled sharing (what family can see) | ❌ Not Done | Sprint 13 | New: `FamilyPortalScreen.kt` | Granular visibility controls |
| FP-03 | Family Shortlist ("Family Approved" marking) | ❌ Not Done | Sprint 13 | New: `FamilyShortlistRepository.kt` | families/{uid}/approved/{targetUid} |
| FP-04 | Co-Browse Mode (parent + user shared session) | ❌ Not Done | Sprint 14 | New: `CoBrowseManager.kt` | Firestore real-time session sharing |

### 3.10 Notifications & Engagement

| ID | Feature | Status | Sprint | File(s) | Task |
|----|---------|--------|--------|---------|------|
| NE-01 | Daily curated matches push (morning, configurable) | ❌ Not Done | Sprint 11 | `FirebaseNotificationService.kt` | Cloud Function daily scheduler |
| NE-02 | Real-time "someone liked your profile" notification | 🟡 Partial | Sprint 7 | `FirebaseNotificationService.kt` | Verify trigger on every interest |
| NE-03 | "Profile viewed X times today" weekly digest | ❌ Not Done | Sprint 11 | New: `AnalyticsRepository.kt` | Track views → Cloud Function weekly batch |
| NE-04 | Profile completeness nudges | ❌ Not Done | Sprint 11 | `HomeScreen.kt` | Show banner with specific missing field |
| NE-05 | Subscription expiry reminders (7d, 3d, 1d before) | ❌ Not Done | Sprint 11 | Cloud Function + FCM | Schedule expiry alerts |
| NE-06 | Inactivity re-engagement (7 days, 30 days) | ❌ Not Done | Sprint 12 | Cloud Function scheduled job | Check `lastActiveAt` + send push |

### 3.11 Premium Subscription Tiers

| Tier | Monthly Price | Key Unlocks | Status | Sprint | File(s) |
|------|--------------|-------------|--------|--------|---------|
| Free | ₹0 | 20 browses/day, 5 interests/day | ✅ Done | Sprint 6 | `SubscriptionRepository.kt` |
| Gold | ₹499/month | Unlimited browse, read/send messages, voice/video, advanced filters, 1 boost/month | 🟡 Partial | Sprint 6 | `SubscriptionRepository.kt` — add Gold tier |
| Platinum | ₹999/month | Gold + incognito, 4 boosts/month, SecurePlatinum badge | 🟡 Partial | Sprint 6 | `SubscriptionRepository.kt` — add Platinum |
| PRIME | Custom | All + relationship manager | ❌ Not Done | Sprint 14 | New: `AssistedServiceScreen.kt` enhancement |

---

## SECTION 4 — TECHNICAL REQUIREMENTS

### 4.1 Performance Targets (from PRD)

| Metric | Target | Status | Task |
|--------|--------|--------|------|
| App cold start | < 2 seconds | 🟡 Partial | Sprint 10: baseline profiling |
| Profile card render | < 100ms | ❌ Not Done | Sprint 10: optimize Coil image pipeline |
| Search results | < 1 second | ❌ Not Done | Sprint 10: composite Firestore indexes |
| Chat delivery | < 500ms | 🟡 Partial | Sprint 8: verify FCM latency |

### 4.2 Security Requirements

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| SEC-01 | Root detection | 🟡 Partial | Sprint 10 | `MainActivity.kt` | Add RootBeer library |
| SEC-02 | Certificate pinning | ❌ Not Done | Sprint 10 | `network_security_config.xml` | Pin Firebase certs |
| SEC-03 | Biometric auth for sensitive screens | ❌ Not Done | Sprint 11 | `BiometricManager.kt` | Gate chat/contact unlock |
| SEC-04 | Secrets in local.properties only | ✅ Done | Sprint 9 | `local.properties` |  |
| SEC-05 | ProGuard obfuscation for release | ✅ Done | Sprint 1 | `proguard-rules.pro` |  |

---

## SECTION 5 — MONETIZATION

| Model | Requirement | Status | Sprint | File(s) | Task |
|-------|-------------|--------|--------|---------|------|
| MON-01 | Razorpay subscription (Gold/Platinum/PRIME) | 🟡 Partial | Sprint 9 | `SubscriptionRepository.kt` | Complete Gold + Platinum tier logic |
| MON-02 | Profile Boost (visibility 2x for 24h) | ❌ Not Done | Sprint 11 | New: `BoostRepository.kt` | Firestore `boostedUntil` timestamp |
| MON-03 | Contact Unlock (per-contact purchase) | 🟡 Partial | Sprint 8 | `MatchDetailScreen.kt` | Add a la carte unlock |
| MON-04 | Rewarded Ad → 1 free interest/day | ❌ Not Done | Sprint 12 | New: `RewardedAdManager.kt` | AdMob rewarded ad integration |
| MON-05 | 30-day money-back guarantee flow | ❌ Not Done | Sprint 13 | Cloud Function `refundSubscription` | Razorpay refund API |
| MON-06 | Biodata PDF (free) as lead-gen | ❌ Not Done | Sprint 12 | `BiodataGenerator.kt` | Free PDF, premium branded |

---

## SECTION 6 — SPRINT ALLOCATION SUMMARY

| Sprint | Focus | Key Deliverables |
|--------|-------|-----------------|
| Sprint 10 | Profile completion + filters | `complexion`, `motherTongue`, income/profession filters, FLAG_SECURE, NRI filter, stealth mode |
| Sprint 11 | AI engine + notifications | `CompatibilityEngine.kt`, daily match push, profile completeness, saved filters, watermarking |
| Sprint 12 | Verification + payments | ID verification (Aadhaar upload), liveness check, Gold/Platinum tiers complete, rewarded ads |
| Sprint 13 | Family portal + biodata | `FamilyPortalScreen.kt`, family sub-account, `BiodataGenerator.kt` |
| Sprint 14 | Live events + co-browse | `LiveEventCallScreen.kt`, `CoBrowseManager.kt`, PRIME tier |

---

## SECTION 7 — DEFINITION OF DONE (10/10 Checklist)

- [ ] All 5 registration channels working (OTP, email, Google, Apple, Family)
- [ ] Profile wizard 5-step complete with progress bar
- [ ] All 5 verification levels implemented and badge displayed
- [ ] Compatibility score (0–100%) shown on every profile card with breakdown
- [ ] All 15 search filters working (5 free + 10 premium)
- [ ] Voice call + video call working without number reveal
- [ ] Family portal with co-browse mode
- [ ] Daily curated match notifications at configurable morning time
- [ ] All 3 subscription tiers (Free/Gold/Platinum) with complete feature gating
- [ ] FLAG_SECURE on all photo/contact screens
- [ ] Biodata PDF generator
- [ ] Profile completeness score with nudges
- [ ] Rewarded ad → 1 free interest
- [ ] 30-day money-back guarantee flow
- [ ] Build passes with 0 errors, all screens reachable
