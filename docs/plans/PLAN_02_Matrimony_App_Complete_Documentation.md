# PLAN 02 — Matrimony_App_Complete_Documentation.docx
## End-to-End Pin-to-Pin Implementation Plan (15 Chapters)

> **Source:** `docs/Matrimony_App_Complete_Documentation.docx`  
> **Chapters:** 15 (Executive Overview → Compliance & Legal)  
> **Tech Stack:** Kotlin + Jetpack Compose, Firebase-first, Room v16, Hilt, Razorpay  
> **Current State:** Sprints 1–9 complete. DB v16. Firebase backend live.

---

## STATUS LEGEND
- ✅ **Done** — Implemented in Sprints 1–9
- 🟡 **Partial** — Started but incomplete
- ❌ **Not Done** — Zero implementation

---

## CHAPTER 1 — Executive Overview & Market Landscape

| Req | Requirement | Status | Sprint | Notes |
|-----|-------------|--------|--------|-------|
| CH1-01 | Firebase-first backend (not microservices) | ✅ Done | 1 | Architecture decision confirmed |
| CH1-02 | Kotlin + Jetpack Compose Android app | ✅ Done | 1 | Primary platform |
| CH1-03 | Market positioning vs BharatMatrimony / Shaadi | 🟡 Partial | 11 | Competitive feature gaps to close |
| CH1-04 | Primary India + NRI focus | 🟡 Partial | 10 | NRI filter not implemented |

---

## CHAPTER 2 — Core Application Architecture

> **Note:** Doc describes microservices (Kubernetes, Kafka, Elasticsearch) — adapted below to Firebase equivalent for our stack.

### 2.1 Presentation Layer
| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| AR-01 | Android native (Kotlin/Compose) | ✅ Done | 1 | All screens |  |
| AR-02 | iOS app (Swift/SwiftUI) | ❌ Not Done | Future | — | Post-Android v1 |
| AR-03 | PWA / Web portal for SEO-optimised profile pages | ❌ Not Done | Sprint 15 | Firebase Hosting | React.js profile pages |
| AR-04 | Offline capability (Room cache) | ✅ Done | 1 | `AppDatabase.kt` |  |

### 2.2 Backend Layer (Firebase Equivalent)
| Microservice Concept | Firebase Equivalent | Status | Sprint | File(s) |
|---------------------|---------------------|--------|--------|---------|
| User Service | Firestore users/{uid} | ✅ Done | 1 | `FirestoreProfileService.kt` |
| Matching Service | Firestore queries + compatibility engine | 🟡 Partial | 10 | `FirestorePagingSource.kt` |
| Search Service | Firestore composite index queries | 🟡 Partial | 9 | `SearchFilters.kt` |
| Messaging Service | Firestore chats/{threadId}/messages | ✅ Done | 4 | `ChatRepository.kt` |
| Notification Service | FCM + Cloud Functions | 🟡 Partial | 7 | `FirebaseNotificationService.kt` |
| Media Service | Firebase Storage | ✅ Done | 3 | `FirebaseStorageService.kt` |
| Payment Service | Razorpay + Cloud Function verify | 🟡 Partial | 6 | `SubscriptionRepository.kt` |
| Verification Service | Manual upload + Firestore | ❌ Not Done | Sprint 12 | New: `VerificationRepository.kt` |
| Analytics Service | Firebase Analytics + Crashlytics | 🟡 Partial | 7 | `profileAnalytics` Firestore collection |
| Admin Service | Admin SDK (Cloud Functions) | ❌ Not Done | Sprint 15 | New Cloud Functions |
| Recommendation Engine | On-device rule-based + Firestore ML | ❌ Not Done | Sprint 13 | New: `RecommendationEngine.kt` |

### 2.3 Data Layer
| Store | Use Case | Status | Sprint | File(s) |
|-------|---------|--------|--------|---------|
| Firestore | Primary source of truth (profiles, interests, matches, chats, blocks, shortlists) | ✅ Done | 1 | Multiple services |
| Firebase Storage | Photos, videos, documents | ✅ Done | 3 | `FirebaseStorageService.kt` |
| Room DB v16 | Offline cache | ✅ Done | 1 | `AppDatabase.kt`, `UserEntity.kt` |
| SharedPreferences | Feature states, FCM token | ✅ Done | 9 | Multiple ViewModels |
| Firebase Remote Config | Feature flags | ✅ Done | 9 | `RemoteConfigManager.kt` |

---

## CHAPTER 3 — User Registration & Profile Management

### 3.1 Registration
| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| REG-01 | Mobile OTP (primary) | ✅ Done | 1 | `AuthRepository.kt` |  |
| REG-02 | Email verification | ✅ Done | 1 | `AuthRepository.kt` |  |
| REG-03 | Google OAuth 2.0 | ✅ Done | 1 | `AuthRepository.kt` |  |
| REG-04 | Apple Sign-In (iOS mandatory) | ❌ Not Done | Sprint 14 (iOS) | — | Required for iOS App Store |
| REG-05 | Third-party registration (parent/sibling creates profile) | ❌ Not Done | Sprint 10 | `SignUpScreen.kt` | Add "Profile For" selector |
| REG-06 | Phone OTP min 3 attempts, then lockout | 🟡 Partial | Sprint 10 | `AuthRepository.kt` | Add rate-limit check |

### 3.2 Profile Attributes (Full Schema)

| Attribute Category | Fields | Status | Sprint | Task |
|-------------------|--------|--------|--------|------|
| Personal Details | Full name, DOB, height, weight, **complexion**, physical disability | 🟡 Partial | Sprint 10 | Add `complexion`, `physicalDisability`, `weight` to UserEntity + Migration_16_17 |
| Location | Current city, home state, country, **willingness to relocate** | 🟡 Partial | Sprint 10 | Add `relocate` field |
| Religious/Cultural | Religion, caste, sub-caste, mother tongue, gotra, nakshatra | ✅ Done | Sprint 8 | |
| Education | Highest qualification, field of study, institution, **graduation year** | 🟡 Partial | Sprint 10 | Add `educationField`, `institution`, `graduationYear` |
| Profession | Occupation type, **employer name**, income range, profession category | 🟡 Partial | Sprint 10 | Add `employer`, `professionCategory` |
| Family Background | Father's occupation, mother's occupation, siblings, family type, family values | 🟡 Partial | Sprint 8 | Add `fatherOccupation`, `motherOccupation`, `siblingsCount` |
| Lifestyle | Diet, **smoking, alcohol**, hobbies, fitness activities | 🟡 Partial | Sprint 10 | Add `smoking`, `alcohol`, `fitnessActivities` |
| Partner Preferences | All profile fields as ranges/preferences | 🟡 Partial | Sprint 10 | Complete all preference fields |
| Horoscope/Kundali | Rashi, Nakshatra, TOB, Manglik status, Kundali chart upload | 🟡 Partial | Sprint 10 | Add chart upload |
| About Me | Free-text up to 500 chars | ✅ Done | Sprint 2 | |

### 3.3 Photo Management (Critical — High Priority)

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| PH-01 | Upload up to 20 photos (primary + gallery) | 🟡 Partial | Sprint 10 | `PhotoEditorScreen.kt` | Implement multi-photo gallery (currently 1 photo) |
| PH-02 | In-app camera capture with preview | 🟡 Partial | Sprint 9 | `PhotoEditorScreen.kt` | Add camera option |
| PH-03 | Crop, brightness, contrast editing | ❌ Not Done | Sprint 11 | `PhotoEditorScreen.kt` | Add uCrop or Coil transform |
| PH-04 | AI face detection (reject non-face uploads) | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | ML Kit face detection on upload |
| PH-05 | NSFW/content moderation filter | ❌ Not Done | Sprint 11 | Cloud Function `onPhotoUpload` | Firebase Extensions or Vision API |
| PH-06 | Auto WebP compression on upload | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | Compress + convert before upload |
| PH-07 | Watermarking on photos | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | Add watermark layer |
| PH-08 | Blurred photo (consent-required) | ❌ Not Done | Sprint 12 | `ProfileDetailScreen.kt` | Blur until connection established |
| PH-09 | Photo privacy: All / Matches Only / Premium Members | 🟡 Partial | Sprint 9 | `PrivacyDashboardScreen.kt` | Enforce in display logic |

### 3.4 Profile Completeness & Gamification

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| PC-01 | Completeness progress % with bar | ❌ Not Done | Sprint 11 | New: `ProfileCompletenessUtil.kt` | 0–100% across all fields |
| PC-02 | Minimum 70% before visible in search | ❌ Not Done | Sprint 11 | `FirestoreProfileService.kt` | Gate profile publication |
| PC-03 | Profile Score badge on card | ❌ Not Done | Sprint 11 | `ProfileCard.kt` | Show score chip |
| PC-04 | Step-by-step guided wizard for new users | ❌ Not Done | Sprint 10 | New: `ProfileWizardScreen.kt` | 5-step onboarding wizard |
| PC-05 | Push nudges for incomplete sections | ❌ Not Done | Sprint 11 | `FirebaseNotificationService.kt` | Weekly nudge Cloud Function |
| PC-06 | Horoscope completion unlocks Kundali feature | ❌ Not Done | Sprint 11 | `KundliScreen.kt` | Gate Kundali behind horoscope data |

### 3.5 Privacy Controls (Field-Level)

| Privacy Control | Options | Status | Sprint | File(s) | Task |
|-----------------|---------|--------|--------|---------|------|
| Profile Visibility | Public / Matches Only / Hidden | 🟡 Partial | Sprint 9 | `UserEntity.isIncognito` | Add 3-level visibility enum |
| Photo Visibility | All / Connected / Premium / None | 🟡 Partial | Sprint 9 | `PrivacyDashboardScreen.kt` | Enforce All 4 levels |
| Contact Visibility | Mutual interest / Premium / Masked | 🟡 Partial | Sprint 8 | `MatchDetailScreen.kt` | Full consent flow |
| Last Active | All / Matches / Hidden | ❌ Not Done | Sprint 10 | `UserEntity.kt` | Add `showLastActive` setting |
| Profile Viewed By | Premium feature | ❌ Not Done | Sprint 11 | New: `ProfileViewsScreen.kt` | Track + display viewers |
| Horoscope Visibility | All / Matches / Hidden | ❌ Not Done | Sprint 10 | `UserEntity.kt` | Add `showHoroscope` setting |
| Salary Disclosure | Exact / Range / Hidden | ❌ Not Done | Sprint 10 | `UserEntity.kt` | Add `incomeDisclosure` field |

---

## CHAPTER 4 — Matchmaking & Search Features

### 4.1 Algorithm Architecture

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| ALG-01 | Rule-based filter engine (hard filters first) | ✅ Done | Sprint 9 | `FirestorePagingSource.kt` |  |
| ALG-02 | Weighted compatibility scoring (9 dimensions) | ❌ Not Done | Sprint 10 | New: `CompatibilityEngine.kt` | Full 9-dimension scoring |
| ALG-03 | ML collaborative filtering | ❌ Not Done | Sprint 13 | New: `RecommendationEngine.kt` | TFLite on-device or Cloud Functions |
| ALG-04 | Implicit signals (time spent, photos viewed) | ❌ Not Done | Sprint 12 | New: `BehavioralSignalService.kt` | Track view time/scroll depth |
| ALG-05 | Explicit signals (interest/decline feedback loop) | 🟡 Partial | Sprint 9 | `FirestoreInterestService.kt` | Feed back into scoring |
| ALG-06 | Kundali AI (NLP horoscope compatibility) | ❌ Not Done | Sprint 13 | `KundliScreen.kt` | Natural language interpretation |
| ALG-07 | Churn prediction model | ❌ Not Done | Sprint 14 | Cloud Functions ML | BigQuery + ML for inactive users |

### 4.2 Compatibility Scoring Weights

| Factor | Weight | Status | Sprint | Task |
|--------|--------|--------|--------|------|
| Religion & Caste | 25% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` |
| Age Range Match | 15% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` |
| Education Compatibility | 12% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` |
| Location / Relocation | 12% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` |
| Income Range | 10% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` |
| Family Values Alignment | 10% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` |
| Horoscope Compatibility | 8% | 🟡 Partial | Sprint 10 | `KundliScreen.kt` |
| Lifestyle Compatibility | 5% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` |
| Physical Preference Match | 3% | ❌ Not Done | Sprint 10 | `CompatibilityEngine.kt` |

### 4.3 Search Filter Implementation

| Filter Type | Filter | Status | Sprint | File(s) | Task |
|-------------|--------|--------|--------|---------|------|
| Basic | Age, height, religion, caste, city/state, education, marital status | ✅ Done | 1 | `SearchFilters.kt` |  |
| Advanced | Mother tongue, country, relocate willingness | 🟡 Partial | Sprint 10 | `SearchFilters.kt` | Add relocate, country |
| Advanced | Diet, Manglik | ✅ Done | Sprint 8 | `SearchFilters.kt` |  |
| Advanced | Physical appearance (complexion, body type) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add complexion filter |
| Advanced | Profile completeness threshold | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Add min-completeness filter |
| Advanced | Verified only | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Filter by `verificationLevel` |
| Advanced | Recently joined (7/30 days) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Filter by `createdAt` |
| Advanced | Online now | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Filter by `lastActiveAt` |
| Location | GPS/radius-based "Near Me" | ❌ Not Done | Sprint 12 | `SearchFilters.kt` | Add geo query (city-level only, no GPS) |

### 4.4 Daily Match Queue

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| DM-01 | Top Picks (highest compatibility of day) | ❌ Not Done | Sprint 11 | `HomeScreen.kt` | Daily batch computation Cloud Function |
| DM-02 | Community Matches (same community priority) | ❌ Not Done | Sprint 11 | `FirestorePagingSource.kt` | Add community-weighted sort |
| DM-03 | NRI Matches (international, by country) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `countryOfResidence` filter |
| DM-04 | Recently Joined (fresh profiles) | ❌ Not Done | Sprint 11 | `HomeScreen.kt` | Carousel of last 7-day profiles |
| DM-05 | Profile of the Day (featured premium) | ❌ Not Done | Sprint 12 | `HomeScreen.kt` | Highlighted card at top |

### 4.5 Kundali/Horoscope (Vedic 36-Gun System)

| Req | Koota | Status | Sprint | File(s) | Task |
|-----|-------|--------|--------|---------|------|
| KUN-01 | Varna (caste compatibility) | 🟡 Partial | Sprint 10 | `KundliScreen.kt` | Complete all 8 kootas |
| KUN-02 | Vashya (control/dominance) | ❌ Not Done | Sprint 10 | `KundliScreen.kt` | Add |
| KUN-03 | Tara (birth star compatibility) | ❌ Not Done | Sprint 10 | `KundliScreen.kt` | Add |
| KUN-04 | Yoni (nature/instinct) | ❌ Not Done | Sprint 10 | `KundliScreen.kt` | Add |
| KUN-05 | Graha Maitri (planetary friendship) | ❌ Not Done | Sprint 10 | `KundliScreen.kt` | Add |
| KUN-06 | Gana (temperament) | 🟡 Partial | Sprint 5 | `KundliScreen.kt` | Verify completeness |
| KUN-07 | Bhakoot (moon sign) | ❌ Not Done | Sprint 10 | `KundliScreen.kt` | Add |
| KUN-08 | Nadi (pulse/nerve) | ❌ Not Done | Sprint 10 | `KundliScreen.kt` | Add (most critical koota) |
| KUN-09 | Manglik Dosha detection | ✅ Done | Sprint 8 | `UserEntity.kt` | |
| KUN-10 | Visual chakra chart rendered from DOB | ❌ Not Done | Sprint 11 | `KundliScreen.kt` | Chart rendering component |
| KUN-11 | AI natural language interpretation | ❌ Not Done | Sprint 13 | Cloud Functions | GPT/Gemini interpretation |
| KUN-12 | Live astrologer booking | ❌ Not Done | Sprint 15 | New: `AstrologerScreen.kt` | Third-party integration |

---

## CHAPTER 5 — Communication Features

### 5.1 Interest & Connection Workflow
| Step | Req | Status | Sprint | File(s) |
|------|-----|--------|--------|---------|
| 1 | User A sends Interest with optional message | ✅ Done | 2 | `FirestoreInterestService.kt` |
| 2 | User B gets push notification | ✅ Done | 7 | `FirebaseNotificationService.kt` |
| 3 | User B accepts/declines | ✅ Done | 2 | `InterestScreen.kt` |
| 4 | Match created + chat unlocked on accept | ✅ Done | 2 | `FirestoreInterestService.kt` |
| 5 | Premium: direct message without waiting | ❌ Not Done | Sprint 11 | `ChatRepository.kt` | Premium message bypass |

### 5.2 Messaging Features

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| MSG-01 | Real-time delivery with read receipts | 🟡 Partial | Sprint 8 | `ChatRepository.kt` | Add 3-state receipt (sent/delivered/read) |
| MSG-02 | Typing indicator | ❌ Not Done | Sprint 11 | `ChatScreen.kt` | Firestore presence field |
| MSG-03 | Message reactions (emoji) | ❌ Not Done | Sprint 12 | `ChatScreen.kt` | Add reactions array in message doc |
| MSG-04 | Reply-to-message threading | ❌ Not Done | Sprint 12 | `ChatScreen.kt` | Add `replyToId` field |
| MSG-05 | Message forwarding (within platform) | ❌ Not Done | Sprint 13 | `ChatScreen.kt` | Copy message to another chat |
| MSG-06 | Delete for self / for everyone (24h window) | ❌ Not Done | Sprint 12 | `ChatRepository.kt` | Soft delete + `deletedFor` array |
| MSG-07 | Message search within conversation | ❌ Not Done | Sprint 12 | `ChatScreen.kt` | Client-side search in loaded messages |
| MSG-08 | Conversation archiving | ❌ Not Done | Sprint 12 | `ChatRepository.kt` | Add `archived` flag to thread |
| MSG-09 | Offline message queuing | 🟡 Partial | Sprint 8 | Room + Firestore sync | Verify offline delivery |

### 5.3 Rich Media Messaging

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| RM-01 | Photo sharing in chat | ❌ Not Done | Sprint 11 | `ChatScreen.kt` | Add image picker + Firebase Storage upload |
| RM-02 | Voice messages (up to 5 min) | ❌ Not Done | Sprint 12 | `ChatScreen.kt` | AudioRecord + Storage upload |
| RM-03 | Document sharing (biodata PDF, kundali PDF) | ❌ Not Done | Sprint 13 | `ChatScreen.kt` | Share generated PDFs |
| RM-04 | Location sharing (optional, safety) | ❌ Not Done | Sprint 13 | `ChatScreen.kt` | Share via Google Maps link |

### 5.4 Voice & Video Calling

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| CALL-01 | VoIP voice call (number masked) | ❌ Not Done | Sprint 12 | `SecureCallScreen.kt` | Agora/Daily.co SDK |
| CALL-02 | HD video call within app | ❌ Not Done | Sprint 12 | `VirtualMeetScreen.kt` | Agora/Daily.co SDK |
| CALL-03 | Call history log | ❌ Not Done | Sprint 13 | New: `CallHistoryRepository.kt` | Firestore callLogs/{uid} |
| CALL-04 | Emergency SOS during call (share GPS) | ❌ Not Done | Sprint 13 | `SecureCallScreen.kt` | SOS button → shares location to emergency contact |

---

## CHAPTER 6 — Privacy, Safety & Trust

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| SAF-01 | FLAG_SECURE on all sensitive screens | ❌ Not Done | Sprint 10 | `MainActivity.kt` | Per-screen FLAG_SECURE |
| SAF-02 | Profile watermarking (deter screenshot misuse) | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | Watermark on upload |
| SAF-03 | Emergency SOS button | ❌ Not Done | Sprint 13 | New: `SafetyCenterScreen.kt` | SOS button, GPS share |
| SAF-04 | Safety Center screen | ❌ Not Done | Sprint 12 | New: `SafetyCenterScreen.kt` | Required by Play Store policy |
| SAF-05 | Block & Report (with reason categories) | ✅ Done | Sprint 5 | `FirestoreBlockService.kt` |  |
| SAF-06 | Incognito Browse | 🟡 Partial | Sprint 8 | `UserEntity.isIncognito` | Enforce in Firestore view tracking |
| SAF-07 | Anti-screenshot for photos | ❌ Not Done | Sprint 10 | `MainActivity.kt` | FLAG_SECURE on photo screen |
| SAF-08 | Contact masking (number never shown directly) | 🟡 Partial | Sprint 8 | `MatchDetailScreen.kt` | Full masking enforcement |
| SAF-09 | AI scam/fraud detection | 🟡 Partial | Sprint 9 | `FakeProfileDetector.kt` | Add pHash + duplicate detection |
| SAF-10 | Criminal background check opt-in (Premium) | 🟡 Partial | Sprint 9 | `BackgroundCheckScreen.kt` | Wire to actual API (AuthBridge) |
| SAF-11 | Safe Matrimony page (safety tips, resources) | ❌ Not Done | Sprint 12 | New: `SafeMatrimonyScreen.kt` | Required by Play Store |

---

## CHAPTER 7 — Advanced & Premium Features

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| ADV-01 | Profile Boost (2x visibility, timed) | ❌ Not Done | Sprint 11 | New: `BoostRepository.kt` | `boostedUntil` Firestore timestamp |
| ADV-02 | Super Interest / Super Like | ✅ Done | Sprint 9 | `SocialRepository.kt` `isSuperLike` |  |
| ADV-03 | AI Bio Generator | 🟡 Partial | Sprint 9 | `BioGeneratorScreen.kt` | Improve templates |
| ADV-04 | Video Profile (15-60 sec) | 🟡 Partial | Sprint 9 | `VideoProfileScreen.kt` | Complete upload + gating |
| ADV-05 | Relationship Manager (Assisted Service) | 🟡 Partial | Sprint 9 | `AssistedServiceScreen.kt` | Wire to real backend |
| ADV-06 | Live Events (group video matchmaking) | 🟡 Partial | Sprint 9 | `LiveEventsScreen.kt` | Wire to real backend |
| ADV-07 | Community Circles | ❌ Not Done | Sprint 14 | `CirclesScreen.kt` | Complete circles functionality |
| ADV-08 | Virtual Meet | 🟡 Partial | Sprint 9 | `VirtualMeetScreen.kt` | Wire to WebRTC |
| ADV-09 | Counselling / Relationship Advice | 🟡 Partial | Sprint 9 | `CounsellingScreen.kt` | Wire to booking backend |
| ADV-10 | Photo Editing Suite | 🟡 Partial | Sprint 9 | `PhotoEditorScreen.kt` | Add crop/filters |

---

## CHAPTER 8 — Admin Panel & CMS

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| ADM-01 | User management (activate/deactivate/ban) | ❌ Not Done | Sprint 15 | Cloud Functions admin | Admin SDK Firebase |
| ADM-02 | Profile moderation (approve photos, flag fake) | ❌ Not Done | Sprint 15 | Cloud Functions | Moderation queue |
| ADM-03 | Content moderation tools | ❌ Not Done | Sprint 15 | Cloud Functions | Flag + review workflow |
| ADM-04 | Analytics dashboard | ❌ Not Done | Sprint 15 | Firebase Analytics | Custom BigQuery dashboard |
| ADM-05 | Revenue & subscription management | ❌ Not Done | Sprint 15 | Cloud Functions | Razorpay webhook dashboard |
| ADM-06 | Fraud detection alerts | 🟡 Partial | Sprint 9 | `FakeProfileDetector.kt` | Admin notification on flag |
| ADM-07 | CMS for static content (about, FAQ, T&C) | ❌ Not Done | Sprint 15 | Firebase Remote Config / Firestore | Dynamic content loading |

---

## CHAPTER 9 — Backend Technology Stack

> Already implemented as Firebase-first. Below are gaps vs. doc recommendations.

| Req | Recommendation | Firebase Equivalent | Status | Task |
|-----|---------------|---------------------|--------|------|
| BE-01 | Elasticsearch for full-text search | Firestore + Algolia | ❌ Not Done | Sprint 13: Algolia integration for search |
| BE-02 | Redis for session caching | Firebase Auth tokens | ✅ Done | — |
| BE-03 | CDN for media delivery | Firebase Storage + CDN | ✅ Done | — |
| BE-04 | Message queue (Kafka) | Cloud Tasks / PubSub | 🟡 Partial | Sprint 13 |
| BE-05 | Event streaming for match events | Firestore triggers | 🟡 Partial | Sprint 12 |

---

## CHAPTER 10 — Database Design & Data Management

### Room Schema — Required Additions (Migration 16→17)

| New Column | Table | Type | Default | Purpose |
|------------|-------|------|---------|---------|
| `complexion` | users | TEXT | null | Profile field |
| `motherTongue` | users | TEXT | null | Filter field |
| `weight` | users | REAL | null | Profile field |
| `physicalDisability` | users | TEXT | null | Profile field |
| `smoking` | users | TEXT | null | Lifestyle |
| `alcohol` | users | TEXT | null | Lifestyle |
| `hobbies` | users | TEXT | null | Lifestyle |
| `fitnessActivities` | users | TEXT | null | Lifestyle |
| `employer` | users | TEXT | null | Career |
| `educationField` | users | TEXT | null | Education |
| `institution` | users | TEXT | null | Education |
| `graduationYear` | users | INTEGER | null | Education |
| `fatherOccupation` | users | TEXT | null | Family |
| `motherOccupation` | users | TEXT | null | Family |
| `siblingsCount` | users | INTEGER | null | Family |
| `relocate` | users | TEXT | null | Location prefs |
| `showLastActive` | users | INTEGER | 1 | Privacy |
| `showHoroscope` | users | INTEGER | 1 | Privacy |
| `incomeDisclosure` | users | TEXT | "range" | Privacy |
| `verificationLevel` | users | INTEGER | 0 | Trust (0–5) |
| `boostedUntil` | users | INTEGER | null | Boost feature |

**File:** `data/local/UserEntity.kt` — add all above columns  
**File:** `data/local/Migrations.kt` — add `MIGRATION_16_17`  
**File:** `data/local/AppModule.kt` — add `addMigrations(MIGRATION_16_17)`

### Firestore Collections — Required Additions

| Collection | Status | Purpose |
|-----------|--------|---------|
| `verifications/{uid}` | ❌ Not Done | ID documents, liveness check results |
| `profileViews/{uid}/viewers/{viewerId}` | ❌ Not Done | Who viewed me |
| `boosts/{uid}` | ❌ Not Done | Boost metadata |
| `dailyMatches/{uid}` | ❌ Not Done | Pre-computed daily match queue |
| `savedFilters/{uid}` | ❌ Not Done | Named filter presets |
| `familyAccounts/{uid}` | ❌ Not Done | Family portal sub-accounts |
| `callLogs/{uid}` | ❌ Not Done | Call history |
| `safetyCenterReports/{reportId}` | ❌ Not Done | Safety reports |

---

## CHAPTER 11 — Security Architecture

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| SEC-01 | TLS/HTTPS for all traffic | ✅ Done | 1 | Firebase managed |  |
| SEC-02 | Firebase Auth JWT tokens | ✅ Done | 1 | `AuthRepository.kt` |  |
| SEC-03 | Firestore security rules (auth-gated) | ✅ Done | Sprint 9 | `firestore.rules` |  |
| SEC-04 | Storage security rules | ✅ Done | Sprint 9 | `storage.rules` |  |
| SEC-05 | Root/Jailbreak detection | ❌ Not Done | Sprint 10 | `MainActivity.kt` | RootBeer library |
| SEC-06 | Certificate pinning (Firebase domains) | ❌ Not Done | Sprint 10 | `network_security_config.xml` | Pin Firebase cert hashes |
| SEC-07 | API key obfuscation (not in APK) | ✅ Done | Sprint 9 | `local.properties` |  |
| SEC-08 | Biometric auth for sensitive screens | ❌ Not Done | Sprint 11 | New: `BiometricManager.kt` | Gate contact unlock, chat |
| SEC-09 | AES-256 encryption for chat messages | ❌ Not Done | Sprint 12 | `ChatRepository.kt` | E2E encryption (Signal Protocol) |
| SEC-10 | Anti-spam rate limiting | ❌ Not Done | Sprint 11 | `firestore.rules` | Add rate limit rules |
| SEC-11 | Account deletion cascade | 🟡 Partial | Sprint 9 | Cloud Function `deleteUserAccount` | Verify full cascade |
| SEC-12 | GDPR-style data export | ❌ Not Done | Sprint 14 | Cloud Functions | Download-my-data endpoint |

---

## CHAPTER 12 — API Design & Integration

| API / Integration | Status | Sprint | Task |
|-------------------|--------|--------|------|
| Razorpay payments | 🟡 Partial | Sprint 10 | Complete subscription tiers |
| FCM push notifications | 🟡 Partial | Sprint 7 | Complete all notification types |
| Firebase Remote Config | ✅ Done | Sprint 9 | Feature flags |
| Google Maps / Geocoding | ❌ Not Done | Sprint 12 | City-level location lookup |
| Aadhaar eKYC API | ❌ Not Done | Sprint 12 | UIDAI sandbox integration |
| DigiLocker API | ❌ Not Done | Sprint 13 | Employment/education verification |
| Algolia Search | ❌ Not Done | Sprint 13 | Full-text profile search |
| Agora / Daily.co WebRTC | ❌ Not Done | Sprint 12 | Voice + video calling SDK |
| AuthBridge (background check) | 🟡 Partial | Sprint 9 | `BackgroundCheckScreen.kt` |
| AstroAPI (kundali) | ❌ Not Done | Sprint 11 | Third-party horoscope calc |

---

## CHAPTER 13 — Monetization & Revenue Models

| Model | Description | Status | Sprint | File(s) | Task |
|-------|-------------|--------|--------|---------|------|
| MON-01 | Freemium subscriptions (Free/Gold/Platinum/PRIME) | 🟡 Partial | Sprint 10 | `SubscriptionRepository.kt` | Complete all tier logic |
| MON-02 | Profile Boost (₹49–199/boost) | ❌ Not Done | Sprint 11 | New: `BoostRepository.kt` | Razorpay one-time payment |
| MON-03 | Contact Unlock (per-contact, à la carte) | 🟡 Partial | Sprint 8 | `MatchDetailScreen.kt` | Razorpay one-time |
| MON-04 | Super Interest pack (buy 5/10) | 🟡 Partial | Sprint 9 | `SocialRepository.kt` | Wire payment |
| MON-05 | Virtual gifts (tokens purchase) | ❌ Not Done | Sprint 13 | New: `TokenRepository.kt` | Token economy |
| MON-06 | Live Events ticket (₹99–499 per session) | ❌ Not Done | Sprint 14 | `LiveEventsScreen.kt` | Paid events |
| MON-07 | Counselling session fee | ❌ Not Done | Sprint 13 | `CounsellingScreen.kt` | Wire to Razorpay |
| MON-08 | Rewarded ad → free unlock | ❌ Not Done | Sprint 12 | New: `RewardedAdManager.kt` | AdMob integration |
| MON-09 | Biodata PDF — branded premium version | ❌ Not Done | Sprint 12 | `BiodataGenerator.kt` | Premium PDF template |

---

## CHAPTER 14 — Performance, Scalability & DevOps

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| PERF-01 | Cold start < 2s | 🟡 Partial | Sprint 10 | `SplashScreen` | Profile startup with Baseline Profiles |
| PERF-02 | ProGuard/R8 release optimization | ✅ Done | Sprint 1 | `proguard-rules.pro` |  |
| PERF-03 | Paging 3 for profile lists | ✅ Done | Sprint 9 | `FirestorePagingSource.kt` |  |
| PERF-04 | Image lazy loading + caching (Coil) | ✅ Done | Sprint 3 | All screen composables |  |
| PERF-05 | Firebase Performance Monitoring | 🟡 Partial | Sprint 9 | `AppModule.kt` | Add custom traces |
| PERF-06 | CI/CD pipeline (GitHub Actions) | 🟡 Partial | Sprint 9 | `.github/workflows/android.yml` | Add test coverage reporting |
| PERF-07 | Crashlytics integration | ✅ Done | Sprint 7 | `app/build.gradle.kts` |  |
| PERF-08 | Firebase App Distribution (beta) | ❌ Not Done | Sprint 11 | `.github/workflows/` | Add App Distribution step |
| PERF-09 | Play Store internal track auto-deploy | ❌ Not Done | Sprint 15 | `.github/workflows/` | Fastlane / Play Publisher API |
| PERF-10 | Detekt + Ktlint lint enforcement | ✅ Done | Sprint 9 | `.github/workflows/android.yml` |  |

---

## CHAPTER 15 — Compliance, Legal & Future Roadmap

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| COMP-01 | Privacy Policy page (live URL) | 🟡 Partial | Sprint 1 | `privacy-policy.html` | Update for DPDP Act 2023 |
| COMP-02 | Terms of Service | ❌ Not Done | Sprint 12 | New HTML page | Create ToS page |
| COMP-03 | DPDP Act 2023 compliance | ❌ Not Done | Sprint 12 | Privacy policy + consent flow | Data principal rights, consent purpose |
| COMP-04 | IT Rules 2021 compliance | ❌ Not Done | Sprint 12 | Backend | Grievance officer, takedown within 36h |
| COMP-05 | Play Store policy (dating app declarations) | ❌ Not Done | Sprint 12 | Store listing + `SafetyCenterScreen.kt` | 18+ declaration, safety features |
| COMP-06 | In-app account deletion (Apple/Google mandate) | ✅ Done | Sprint 9 | Cloud Function `deleteUserAccount` |  |
| COMP-07 | Data export on request | ❌ Not Done | Sprint 14 | Cloud Functions | Download-my-data |
| COMP-08 | Cookie consent / consent management | ❌ Not Done | Sprint 12 | `MainActivity.kt` | Consent dialog on first launch |
| COMP-09 | Grievance Officer details in app | ❌ Not Done | Sprint 12 | `SettingsScreen.kt` | Add contact for grievances |
| COMP-10 | Age verification (18+) | ❌ Not Done | Sprint 12 | `SignUpScreen.kt` | DOB validation + declaration |

---

## SPRINT ALLOCATION SUMMARY

| Sprint | Primary Chapter | Key Deliverables |
|--------|----------------|-----------------|
| Sprint 10 | Ch3 + Ch4 | Profile schema complete (17 new fields), advanced search filters, privacy toggles |
| Sprint 11 | Ch4 + Ch7 | CompatibilityEngine, profile completeness, photo pipeline, daily matches |
| Sprint 12 | Ch5 + Ch11 | Voice/video SDK, message features, security hardening, compliance, Safety Center |
| Sprint 13 | Ch9 + Ch10 | Algolia search, recommendation engine, behavioral signals, Firestore schema additions |
| Sprint 14 | Ch8 + Ch13 | Family portal, live events, community circles, monetization features |
| Sprint 15 | Ch8 + Ch14 | Admin panel, CI/CD automation, web portal, Play Store submission |

---

## DEFINITION OF DONE (10/10 Checklist)

- [ ] All 22 new Room columns added (Migration 16→17) with zero data loss
- [ ] Compatibility score (9 dimensions) shown on every profile card
- [ ] Multi-photo gallery (up to 20 photos) with face detection + NSFW filter
- [ ] All 8 Kundali kootas calculated and displayed
- [ ] Voice + video calling operational (no number reveal)
- [ ] Typing indicator + read receipts + reactions in chat
- [ ] Safety Center screen with block/report/emergency SOS
- [ ] FLAG_SECURE on all sensitive screens
- [ ] Full compliance: DPDP Act, Play Store dating policy, account deletion
- [ ] Admin panel operational for moderation
- [ ] CI/CD pipeline deploys to Play Store internal track
- [ ] All Firestore security rules tested with emulator
