# PLAN 03 — TeluguMatrimony_PRD_v1.docx
## End-to-End Pin-to-Pin Implementation Plan (Telugu-Specific)

> **Source:** `docs/TeluguMatrimony_PRD_v1.docx`  
> **Focus:** Telugu/AP/Telangana/NRI community — mobile-first, culturally deep  
> **Tech Stack Adaptation:** Firebase-first Android (not React Native + AWS as in doc)  
> **Model:** Freemium ₹399/₹799/₹1,499 per month + à la carte  
> **40-Week Plan → Adapted to Firebase Sprints**

---

## STATUS LEGEND
- ✅ **Done** — Implemented in Sprints 1–9
- 🟡 **Partial** — Started but incomplete
- ❌ **Not Done** — Zero implementation

---

## SECTION 1 — CRITICAL CORRECTIONS (16 Verified Issues from Source Doc)

> These 16 issues were identified across 8 source documents and resolved in the PRD. All must be honoured in implementation.

| # | Issue | Resolution Required | Status | Sprint | Task |
|---|-------|---------------------|--------|--------|------|
| 1 | Vivaah described incorrectly | NOT adopted as reference | ✅ Done | 1 | Vivaah architecture ignored |
| 2 | BharatMatrimony mandates complete photo | Photo required but with guide | 🟡 Partial | Sprint 11 | Add AI quality guide on upload |
| 3 | AI bio generator "writes for user" violates stores | AI assists, user writes | 🟡 Partial | Sprint 9 | `BioGeneratorScreen.kt` — confirm it's assistance only |
| 4 | Free messaging unlimited | 5 icebreakers/day free, chat free after accept | 🟡 Partial | Sprint 8 | Verify FREE_MSG_LIMIT logic |
| 5 | Dual currency confusion | INR primary, USD shown for NRI only | ❌ Not Done | Sprint 10 | Add NRI currency display logic |
| 6 | AI conversation coach (reads live chat) | REMOVED — privacy-invasive | ✅ Done | — | Not implemented |
| 7 | Elite Matrimony ₹75,000 pricing | RM add-on at ₹4,999/month accessible | 🟡 Partial | Sprint 9 | `AssistedServiceScreen.kt` — set price ₹4,999 |
| 8 | Locked filters shown prominently | Show after 3 searches only | ❌ Not Done | Sprint 11 | Gate advanced filter UI behind search count |
| 9 | Manglik filter without logic | With clear labels + tooltip explanation | ✅ Done | Sprint 8 | `UserEntity.manglik` |
| 10 | Vivaah Intelli-match "sophisticated" | Confirmed basic — NOT adopted | ✅ Done | — | Our engine uses behavioral signals |
| 11 | 'Near Me' GPS no privacy safeguards | City/district level only (10km min, no GPS coords) | ❌ Not Done | Sprint 12 | No GPS storage, city-only search |
| 12 | Marriage timeline prediction | REMOVED — replaced with response rate % | ❌ Not Done | Sprint 11 | Show "45% respond in 48h" instead |
| 13 | Virtual gifts (frivolous for matrimony) | REMOVED — engagement features only | ✅ Done | — | Not implemented |
| 14 | Multi-person photo blanket reject | Primary must show owner; family photos in album | 🟡 Partial | Sprint 11 | ML Kit: detect multiple faces in primary slot only |
| 15 | Astro compatibility no formula | Telugu 10-Porutham system implemented | 🟡 Partial | Sprint 10 | Complete 10-Porutham in `KundliScreen.kt` |
| 16 | Duplicate subscription counting after discount | Yearly plan = 20% off, shown clearly | ❌ Not Done | Sprint 11 | Add annual plan option with discount display |

---

## SECTION 2 — PRODUCT VISION & MARKET POSITIONING

| Req | Requirement | Status | Sprint | Task |
|-----|-------------|--------|--------|------|
| VIS-01 | Telugu-first branding (not generic Indian) | ❌ Not Done | Sprint 10 | Add Telugu script support, Telugu-first UI strings |
| VIS-02 | Andhra Pradesh + Telangana geography pre-loaded | ❌ Not Done | Sprint 10 | Pre-populate city lists: Hyderabad, Vijayawada, Vizag, Guntur, Tirupati, Warangal |
| VIS-03 | NRI market (USA, UAE, Australia, UK, Singapore) | ❌ Not Done | Sprint 10 | Add country of residence + NRI flag in profile |
| VIS-04 | Modern UI superior to BharatMatrimony's 2005-era | ✅ Done | 1 | Compose Material3 |
| VIS-05 | Multi-layer verification trust anchor | 🟡 Partial | Sprint 12 | Complete 5-level verification |

---

## SECTION 3 — USER PERSONAS (4 Archetypes)

### 3.1 Priya — Modern Telugu Woman
| Feature Needed | Status | Sprint | File(s) | Task |
|----------------|--------|--------|---------|------|
| Photo privacy until mutual interest | 🟡 Partial | Sprint 10 | `PrivacyDashboardScreen.kt` | Enforce blur until match |
| Block & report | ✅ Done | Sprint 5 | `FirestoreBlockService.kt` |  |
| Icebreaker system (5/day free) | 🟡 Partial | Sprint 8 | `ChatRepository.kt` | Confirm 5/day limit |
| Filter by profession + city + verification | 🟡 Partial | Sprint 10 | `SearchFilters.kt` | Add profession + verification filters |

### 3.2 Ravi — NRI Groom
| Feature Needed | Status | Sprint | File(s) | Task |
|----------------|--------|--------|---------|------|
| "Willing to relocate" filter | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `relocate` filter |
| USD/INR dual pricing for NRI | ❌ Not Done | Sprint 10 | `SubscriptionScreen.kt` | Detect NRI location, show USD |
| NRI profile flag | ❌ Not Done | Sprint 10 | `UserEntity.kt` | Add `isNRI`, `countryOfResidence` fields |
| Video call scheduling (not just live) | ❌ Not Done | Sprint 12 | `VirtualMeetScreen.kt` | Add calendar-based scheduling |
| Telugu-speaking brides in India + USA | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Telugu mother tongue + country filter combo |

### 3.3 Suresh — Traditional Father
| Feature Needed | Status | Sprint | File(s) | Task |
|----------------|--------|--------|---------|------|
| "Profile for daughter" flag | ❌ Not Done | Sprint 10 | `SignUpScreen.kt` | Add "Profile For" selector |
| Telugu language UI option | ❌ Not Done | Sprint 13 | `strings.xml` | Telugu localization strings |
| Physical verification assurance badge | ❌ Not Done | Sprint 12 | `ProfileCard.kt` | Show `verificationLevel` badge |
| Phone-based support option | ❌ Not Done | Sprint 14 | `SettingsScreen.kt` | Add support phone number |

### 3.4 Ananya — RM Client
| Feature Needed | Status | Sprint | File(s) | Task |
|----------------|--------|--------|---------|------|
| Platinum plan with RM access | 🟡 Partial | Sprint 9 | `AssistedServiceScreen.kt` | Wire to actual RM workflow |
| Curated match delivery (3/month) | ❌ Not Done | Sprint 14 | New: `RMDashboard` + Firestore `rmRequests` | RM assigns curated profiles |
| Simple accept/decline interface | ❌ Not Done | Sprint 13 | New: `CuratedMatchesScreen.kt` | Simple card accept/decline |
| Preference update call with RM monthly | ❌ Not Done | Sprint 14 | `AssistedServiceScreen.kt` | Add RM call scheduling |

---

## SECTION 4 — SEARCH & FILTERING (35 Filters, 8 Sections)

### 4.1 Search Modes (All 10)

| Mode | Status | Sprint | File(s) | Task |
|------|--------|--------|---------|------|
| Quick Search (3 fields: Gender, Religion, Age) | ✅ Done | 1 | `HomeScreen.kt` |  |
| Basic Search (8 core filters) | ✅ Done | 1 | `SearchFilters.kt` |  |
| Advanced Search (35+ filters) | 🟡 Partial | Sprint 10 | `SearchFilters.kt` | Add missing filters (see below) |
| Keyword Search (free text: name, caste, city) | ❌ Not Done | Sprint 13 | Algolia integration | Full-text search |
| Profile ID Search (TLG-XXXXX format) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `matrimonyId` field + search |
| Who's Online Search | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Filter by `lastActiveAt` < 30 min |
| Special Cases Search (differently-abled inclusive/exclusive) | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Add `physicalStatus` filter with compassionate labels |
| Location Search (city + 10km radius, no GPS) | ❌ Not Done | Sprint 12 | `SearchFilters.kt` | City-level proximity, no GPS coords |
| Occupation Search | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add profession/industry filter |
| AI Smart Search (pre-fills based on profile + history) | ❌ Not Done | Sprint 13 | New: `SmartSearchEngine.kt` | Behavioral pre-fill |

### 4.2 All 35 Advanced Filters (Pinned to Files)

#### Section A — Personal Basics
| Filter | Status | Sprint | File(s) | Task |
|--------|--------|--------|---------|------|
| Age range (dual slider) | ✅ Done | 1 | `SearchFilters.kt` |  |
| Height range (dual slider + cm/ft toggle) | ✅ Done | 1 | `SearchFilters.kt` |  |
| Marital status (multi-select: Never/Divorced/Widowed/Awaiting) | ✅ Done | 1 | `SearchFilters.kt` |  |
| Have children (No / Yes-together / Yes-not together / Don't mind) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add children status filter |
| Physical status (Normal / Differently Abled with sub-type) | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Compassionate label, no offensive language |
| Willing to relocate (multi-select: Anywhere/India/USA/UAE/No) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` + `UserEntity.kt` | Critical for NRI matching |

#### Section B — Religion & Community (Telugu-Specific)
| Filter | Status | Sprint | File(s) | Task |
|--------|--------|--------|---------|------|
| Religion | ✅ Done | 1 | `SearchFilters.kt` |  |
| Mother Tongue (Telugu pre-selected for AP/TS) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `motherTongue` filter, Telugu pre-selected |
| Caste (1,000+ entries, Telugu castes first: Reddy, Kamma, Kapu, Velama, Brahmin, Naidu, Yadav) | ✅ Done | Sprint 1 | `SearchFilters.kt` | Verify Telugu castes are first |
| Sub-caste/Division (driven by caste selection) | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Dynamic sub-caste dropdown |
| Gothra (text input + common Telugu gothras pre-loaded) | ❌ Not Done | Sprint 11 | `SearchFilters.kt` + `UserEntity.kt` | Add `gothra` field |
| Nakshatra (27 options) | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Add Nakshatra filter |
| Rasi (12 options) | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Add Rasi filter |
| Manglik (Manglik only / Non-Manglik / Don't mind) | ✅ Done | Sprint 8 | `SearchFilters.kt` |  |

#### Section C — Location & Citizenship
| Filter | Status | Sprint | File(s) | Task |
|--------|--------|--------|---------|------|
| Country (India, USA, UAE, AUS, UK, Singapore, Canada first) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` + `UserEntity.kt` | Add `countryOfResidence` |
| State (AP + Telangana listed first) | 🟡 Partial | Sprint 10 | `SearchFilters.kt` | Ensure AP/TG are top of list |
| City/District (Hyderabad, Vijayawada, Vizag, Guntur, Tirupati, Warangal pre-loaded) | 🟡 Partial | Sprint 10 | `SearchFilters.kt` | Hardcode Telugu cities as suggestions |
| Citizenship (Indian / NRI / British Indian / AUS PR / Any) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` + `UserEntity.kt` | Add `citizenship` field |
| NRI Status (NRI only / Include / Exclude) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add NRI filter toggle |

#### Section D — Education & Career
| Filter | Status | Sprint | File(s) | Task |
|--------|--------|--------|---------|------|
| Education level | ✅ Done | 1 | `SearchFilters.kt` |  |
| Education field (Engineering, Medicine, Management, etc.) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `educationField` filter |
| Specific degree (BTech, MBBS, MBA, CA, etc.) | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Add `specificDegree` searchable |
| Occupation category (IT, Government, Healthcare, Business, Finance, Defence) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `occupationCategory` filter |
| Occupation title (free text search) | ❌ Not Done | Sprint 13 | Algolia | Full-text occupation search |
| Employer type (Private MNC / Government / PSU / Self-employed) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `employerType` filter |
| Income range (₹3L brackets, USD equivalent for NRI) | 🟡 Partial | Sprint 10 | `SearchFilters.kt` | Add income range slider + NRI USD equiv |

#### Section E — Lifestyle & Family Values
| Filter | Status | Sprint | File(s) | Task |
|--------|--------|--------|---------|------|
| Diet (Veg/Non-veg/Eggetarian/Vegan/Jain) | ✅ Done | Sprint 5 | `SearchFilters.kt` |  |
| Smoking (Never/Occasionally/Regularly/Don't mind) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add smoking filter |
| Drinking (Never/Occasionally/Regularly/Don't mind) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add drinking filter |
| Family type (Joint/Nuclear/Either) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add family type filter |
| Family status (Middle/Upper-middle/Affluent/Rich) | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Add family status filter |
| Hobbies & Interests (multi-select: Telugu-first: Cricket, Carnatic music, Temple visits) | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Add hobbies filter with Telugu-first tags |

#### Sections F-H — Astro / Activity / Quality
| Filter | Status | Sprint | File(s) | Task |
|--------|--------|--------|---------|------|
| Astro compatibility (min Porutham score 6/8/10) | ❌ Not Done | Sprint 13 | `SearchFilters.kt` + `KundliScreen.kt` | Phase 2: AstroAPI integration |
| Horoscope available (Yes/No/Don't mind) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `hasHoroscope` filter |
| Profile posted in last 7/15/30/90 days | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Filter by `createdAt` range |
| Last active (today / 7 days / 30 days) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Filter by `lastActiveAt` |
| With photo only (default ON) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Add `hasPhoto` filter, default true |
| Verification status (Any/Blue/Gold) | ❌ Not Done | Sprint 10 | `SearchFilters.kt` | Filter by `verificationLevel` |
| Premium members only | ❌ Not Done | Sprint 11 | `SearchFilters.kt` | Optional filter, not default |

### 4.3 Saved Searches & Alerts

| Feature | Status | Sprint | File(s) | Task |
|---------|--------|--------|---------|------|
| Up to 10 saved searches per user | ❌ Not Done | Sprint 11 | New: `SavedSearchRepository.kt` | Firestore `savedSearches/{uid}` |
| Alert on new matching profile | ❌ Not Done | Sprint 11 | Cloud Functions | Trigger FCM on new profile matching saved search |

---

## SECTION 5 — PROFILE SYSTEM

### 5.1 9-Step Profile Creation Wizard

| Step | Content | Status | Sprint | File(s) | Task |
|------|---------|--------|--------|---------|------|
| Step 1 | Profile For (Myself / Son / Daughter / Sibling) | ❌ Not Done | Sprint 10 | `SignUpScreen.kt` | Add "Profile For" step |
| Step 2 | Basic Details (Name, DOB, Gender, Height, Marital status) | 🟡 Partial | Sprint 1 | `SignUpScreen.kt` | Add Height |
| Step 3 | Location (State, City, Country, Relocate) | 🟡 Partial | Sprint 1 | `ProfileEditScreen.kt` | Add Country + Relocate |
| Step 4 | Religion & Community (Religion, Caste, Sub-caste, Gothra, Nakshatra, Rasi) | 🟡 Partial | Sprint 8 | `ProfileEditScreen.kt` | Add Sub-caste, Gothra, Nakshatra |
| Step 5 | Education & Career (Level, Field, Occupation, Employer, Income) | 🟡 Partial | Sprint 3 | `ProfileEditScreen.kt` | Add Field, Employer |
| Step 6 | Family Background (Father occ, Mother occ, Siblings, Family type, Family values) | 🟡 Partial | Sprint 8 | `ProfileEditScreen.kt` | Complete family section |
| Step 7 | Partner Preferences (age, caste, location, education — all with "Open to any" toggle) | 🟡 Partial | Sprint 5 | `ProfileEditScreen.kt` | Add "Open to any" toggle per field |
| Step 8 | Lifestyle (Diet, Smoking, Drinking, Family Type, Hobbies — Telugu-first) | 🟡 Partial | Sprint 5 | `ProfileEditScreen.kt` | Add Smoking/Drinking |
| Step 9 | Astro (optional, skippable — Nakshatra, Rasi, Horoscope upload, Manglik) | 🟡 Partial | Sprint 5 | `ProfileEditScreen.kt` | Add "Add Later" skip button |

### 5.2 AI Bio Writing Assistant

| Feature | Status | Sprint | File(s) | Task |
|---------|--------|--------|---------|------|
| Profession-specific prompt templates | 🟡 Partial | Sprint 9 | `BioGeneratorScreen.kt` | Add Telugu-specific examples |
| Cliché phrase detector | ❌ Not Done | Sprint 11 | `BioGeneratorScreen.kt` | Flag "I am a simple person", "looking for my better half" |
| Telugu-flavored example library | ❌ Not Done | Sprint 11 | `BioGeneratorScreen.kt` | "Engineer from Vijayawada", "Carnatic music lover" |
| Profile preview mode | ❌ Not Done | Sprint 11 | `BioGeneratorScreen.kt` | Show how profile looks to a match on mobile |

### 5.3 Profile Completeness Rewards

| Level | Unlock | Status | Sprint | Task |
|-------|--------|--------|--------|------|
| 50% | Profile visible in search | ❌ Not Done | Sprint 11 | Gate search visibility at 50% completion |
| 75% | Daily recommendations start | ❌ Not Done | Sprint 11 | Enable daily match queue at 75% |
| 90% | Homepage feature slot eligible | ❌ Not Done | Sprint 12 | Mark eligible for "Recently Active" section |
| 100% | Free 1-day Spotlight boost | ❌ Not Done | Sprint 12 | Auto-grant free boost on 100% completion |

---

## SECTION 6 — MATCHING ENGINE

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| Match-01 | Rule-based engine (Phase 1 weights) | ✅ Done | Sprint 9 | `FirestorePagingSource.kt` |  |
| Match-02 | Weighted compatibility (8 dimensions visible) | ❌ Not Done | Sprint 10 | New: `CompatibilityEngine.kt` | Build weighted score |
| Match-03 | Behavioral signals (view time, scroll depth) | ❌ Not Done | Sprint 12 | New: `BehavioralSignalService.kt` | Track signals |
| Match-04 | Collaborative filtering (ML, Phase 2) | ❌ Not Done | Sprint 13 | New: `RecommendationEngine.kt` | TFLite on-device |
| Match-05 | "Why this match?" explanation on card | ❌ Not Done | Sprint 11 | `ProfileCard.kt` | Expandable reason chip |
| Match-06 | 10-Porutham astro compatibility (Phase 2) | 🟡 Partial | Sprint 13 | `KundliScreen.kt` | AstroAPI integration |
| Match-07 | Churn prediction (inactive user boost) | ❌ Not Done | Sprint 14 | Cloud Functions | Boost recommendations for 14-day inactives |

---

## SECTION 7 — COMMUNICATION

| Feature | Status | Sprint | File(s) | Task |
|---------|--------|--------|---------|------|
| Icebreaker (5/day free, 20/day Standard, unlimited Premium/Platinum) | 🟡 Partial | Sprint 8 | `ChatRepository.kt` | Verify 5/day limit on FREE |
| Post-acceptance chat (free for all tiers) | ✅ Done | Sprint 4 | `ChatRepository.kt` |  |
| Voice calling (VoIP, Standard+) | ❌ Not Done | Sprint 12 | `SecureCallScreen.kt` | Agora SDK integration |
| Video calling (Premium+) | ❌ Not Done | Sprint 12 | `VirtualMeetScreen.kt` | Agora SDK integration |
| Video call scheduling (NRI timezone aware) | ❌ Not Done | Sprint 12 | `VirtualMeetScreen.kt` | Calendar booking + timezone |
| Group family call (Platinum) | ❌ Not Done | Sprint 14 | `VirtualMeetScreen.kt` | Multi-party call |

---

## SECTION 8 — VERIFICATION TIERS

| Level | Method | Badge Color | Status | Sprint | File(s) | Task |
|-------|--------|-------------|--------|--------|---------|------|
| Basic | Phone OTP | Grey | ✅ Done | 1 | `AuthRepository.kt` |  |
| Blue | Aadhaar liveness check + face match | Blue tick | ❌ Not Done | Sprint 12 | New: `VerificationScreen.kt` | UIDAI sandbox API |
| Gold | Address + Employment verification | Gold tick | ❌ Not Done | Sprint 13 | New: `VerificationScreen.kt` | DigiLocker integration |
| Premium | All + manual team review | Premium badge | ❌ Not Done | Sprint 14 | Admin workflow |  |

---

## SECTION 9 — MONETIZATION (Telugu PRD Pricing)

| Tier | Price | Key Features | Status | Sprint | File(s) | Task |
|------|-------|-------------|--------|--------|---------|------|
| Free | ₹0 | 5 icebreakers/day, chat after accept | ✅ Done | Sprint 6 | `SubscriptionRepository.kt` |  |
| Standard | ₹399/month | 20 icebreakers/day, voice calls, advanced filters, see viewers (7d) | ❌ Not Done | Sprint 10 | `SubscriptionRepository.kt` | Add Standard tier |
| Premium | ₹799/month | Standard + video calls, 3 Spotlights/month, invisible mode | ❌ Not Done | Sprint 10 | `SubscriptionRepository.kt` | Add Premium tier |
| Platinum | ₹1,499/month | Premium + RM (3 curated/month), unlimited Spotlights, all-time viewers, Astro PDF | ❌ Not Done | Sprint 11 | `SubscriptionRepository.kt` | Add Platinum tier |
| Annual plans | 20% discount | Standard ₹3,829/yr, Premium ₹7,669/yr, Platinum ₹14,389/yr | ❌ Not Done | Sprint 11 | `SubscriptionScreen.kt` | Add annual plan option |

### Additional Revenue Streams

| Item | Price | Status | Sprint | File(s) | Task |
|------|-------|--------|--------|---------|------|
| Profile Spotlight (24h, top-10 appearance) | ₹149 | ❌ Not Done | Sprint 11 | New: `BoostRepository.kt` | Razorpay one-time + `boostedUntil` timestamp |
| Super Like pack (5 Likes with gold badge) | ₹49/5-pack | 🟡 Partial | Sprint 9 | `SocialRepository.kt` | Wire to Razorpay payment |
| Relationship Manager add-on | ₹4,999/month | 🟡 Partial | Sprint 9 | `AssistedServiceScreen.kt` | Wire to actual backend |
| 10-Porutham Astro Report (per-pair) | ₹299 | ❌ Not Done | Sprint 13 | `KundliScreen.kt` | Generate PDF + Razorpay |
| Community Event Ticket | ₹499–999 | ❌ Not Done | Sprint 14 | `LiveEventsScreen.kt` | Paid events |

---

## SECTION 10 — UI/UX DESIGN SYSTEM

> **Current MatchApp:** Material3 default purple/teal theme  
> **Required:** Telugu cultural design system

| Element | Required | Current | Status | Sprint | File(s) | Task |
|---------|---------|---------|--------|--------|---------|------|
| Primary Color | #8B1A2E (Deep Maroon) | Material3 purple | ❌ Not Done | Sprint 10 | `ui/theme/Color.kt` | Change primary to #8B1A2E |
| Secondary Color | #B8860B (Antique Gold) | Material3 teal | ❌ Not Done | Sprint 10 | `ui/theme/Color.kt` | Change secondary to #B8860B |
| Background | #FFFBF5 (Warm Cream) | White | ❌ Not Done | Sprint 10 | `ui/theme/Color.kt` | Change background |
| Font | Poppins + Noto Sans (Telugu support) | Default Material | ❌ Not Done | Sprint 10 | `ui/theme/Type.kt` | Add Poppins + Noto Sans Telugu |
| Match score badge | Circular, e.g. "82%" | Not shown | ❌ Not Done | Sprint 11 | `ProfileCard.kt` | Add score circle badge |
| Telugu script profile support | Noto Sans Telugu rendering | Not present | ❌ Not Done | Sprint 13 | `strings-te.xml` | Telugu localization |
| Border radius | 12px cards, 8px buttons | Standard | 🟡 Partial | Sprint 10 | `ui/theme/Shape.kt` | Verify spacing |
| Min touch target | 48px all interactive elements | 🟡 Partial | Sprint 10 | All composables | Audit touch targets |

---

## SECTION 11 — 40-WEEK PLAN (Adapted to Firebase Sprints)

| Phase | Original Weeks | Our Sprint | Key Deliverables | Gate |
|-------|---------------|------------|-----------------|------|
| Phase 0 — Foundation | Wk 1–2 | ✅ Done Sprints 1–3 | Firebase setup, Compose UI, design system | BUILD SUCCESSFUL |
| Phase 1A — Core Profile & Search | Wk 3–6 | Sprint 10 | 9-step wizard, all 35 filters, Profile ID search, Blue Verification, photo watermark+blur | Profile in <8 min, all 35 filters functional |
| Phase 1B — Matching & Communication | Wk 7–10 | Sprint 11 | CompatibilityEngine, daily recommendations, icebreaker limits, voice call (Agora), privacy dashboard | Full user flow: create→match→icebreaker→chat→voice call |
| Phase 1C — MVP Launch | Wk 10 | Sprint 12 | Complete subscription tiers (Free/Standard/Premium/Platinum), Spotlight, Safety Center, Play Store submission | All 4 tiers functional. ₹399/₹799/₹1,499 Razorpay working |
| Phase 2A — AI Matching | Wk 11–18 | Sprint 13 | ML collaborative filtering, behavioral signals, "Why this match?", Smart Search, video calling (Agora), Gold Verification | AI score on every card. Smart Search relevant for 80% of users |
| Phase 2B — Telugu-Specific | Wk 19–22 | Sprint 13 | 10-Porutham API, Telugu script UI, city-specific pages (Hyderabad/Vijayawada/Vizag), NRI scheduling | Full Telugu profile browsable. NRI timezone-aware calls |
| Phase 2C — Monetization Depth | Wk 23–26 | Sprint 14 | Platinum RM workflow, RM dashboard, Astro PDF ₹299, annual plans, referral ₹500 credit | Platinum subscriber gets 3 curated profiles within 5 days |
| Phase 3 — Community & Growth | Wk 27–34 | Sprint 15 | Success stories, Telugu blog content, event tickets, SEO landing pages | 100 success stories. App rating >4.2 |
| Phase 4 — Scale & Advanced AI | Wk 35–40 | Sprint 16 | Full ML feedback loop, DPDPA compliance, 100K concurrent load test | All SLAs met. Legal compliance signed off |

---

## SECTION 12 — TELUGU-SPECIFIC FEATURES (Unique Differentiators)

| Feature | Description | Status | Sprint | Task |
|---------|-------------|--------|--------|------|
| TG-01 | Telugu caste hierarchy (Reddy, Kamma, Kapu, Velama, Brahmin, Naidu first) | 🟡 Partial | Sprint 10 | Verify caste list order in `SearchFilters.kt` |
| TG-02 | Telugu gothras pre-loaded (Kasyapa, Bharadwaja, Vasishta, Vishwamitra) | ❌ Not Done | Sprint 11 | Add `gothra` field with pre-loaded list |
| TG-03 | Nakshatra 27 + Rasi 12 dropdowns | ❌ Not Done | Sprint 11 | Add to profile + search filters |
| TG-04 | 10-Porutham compatibility (Dina, Gana, Mahendra, Stree Deergha, Yoni, Rasi, Rajju, Vedha, Vasiya, Mahendram) | 🟡 Partial | Sprint 13 | Complete all 10 Poruthams in `KundliScreen.kt` |
| TG-05 | Telugu cities pre-loaded (Hyderabad, Vijayawada, Vizag, Guntur, Tirupati, Warangal, Nellore) | ❌ Not Done | Sprint 10 | Pre-load city suggestions |
| TG-06 | Telugu script UI (optional language switch) | ❌ Not Done | Sprint 13 | Add `strings-te.xml` Telugu localization |
| TG-07 | Telugu-specific hobbies (Carnatic music, Kuchipudi, Temple visits, Cricket) | ❌ Not Done | Sprint 11 | Add Telugu-first hobby tags |
| TG-08 | Profile ID in TLG-XXXXX format | ❌ Not Done | Sprint 10 | Add `matrimonyId` field generated on signup |
| TG-09 | Matrimony ID search | ❌ Not Done | Sprint 10 | Add ID search in `SearchFilters.kt` |
| TG-10 | Referral ₹500 credit system | ❌ Not Done | Sprint 14 | `MatchmakerReferralScreen.kt` → wire to Razorpay credit |

---

## DEFINITION OF DONE (10/10 Checklist)

- [ ] All 35 search filters implemented and functional
- [ ] 9-step profile wizard complete with "Open to any" toggles
- [ ] Telugu cultural design system (Maroon #8B1A2E + Gold #B8860B theme)
- [ ] Telugu city pre-loading (Hyderabad, Vijayawada, Vizag, Guntur, Tirupati, Warangal)
- [ ] Telugu caste list ordered correctly (Reddy, Kamma, Kapu first)
- [ ] 10-Porutham (all 10 Poruthams calculated and displayed)
- [ ] All 4 subscription tiers (₹399/₹799/₹1,499/Free) operational in Razorpay
- [ ] Spotlight ₹149, Super Like ₹49/5-pack, Astro PDF ₹299 purchasable
- [ ] Blue Verification (Aadhaar liveness) and Gold Verification (DigiLocker) operational
- [ ] NRI features: countryOfResidence, relocate filter, USD price display
- [ ] Telugu language UI option (strings-te.xml)
- [ ] AI bio assistant with Telugu-flavored examples and cliché detector
- [ ] Profile completeness reward system (50%/75%/90%/100% unlocks)
- [ ] Voice + video calling (Agora SDK) operational
- [ ] All 16 critical corrections from source doc honoured
