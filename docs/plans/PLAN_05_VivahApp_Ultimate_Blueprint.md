# PLAN 05 — VivahApp_Ultimate_Blueprint.docx
## End-to-End Pin-to-Pin Implementation Plan (Feature Extraction)

> **Source:** `docs/VivahApp_Ultimate_Blueprint.docx`  
> **Architecture in Doc:** Cloudflare Edge (Workers + D1 + KV + R2 + Durable Objects) — a DIFFERENT product  
> **Our Stack:** Firebase-first Android (Kotlin + Compose + Firestore + Cloud Functions + Storage + FCM)  
> **Approach:** Extract every FEATURE from VivahApp Blueprint, adapt to Firebase stack, implement the best features  
> **What to SKIP:** The entire Cloudflare infrastructure — we use Firebase instead

---

## STATUS LEGEND
- ✅ **Done** — Implemented in Sprints 1–9
- 🟡 **Partial** — Started but incomplete
- ❌ **Not Done** — Zero implementation

---

## SECTION 1 — FEATURE MATRIX EXTRACTION

### 1.1 Profile Features (from VivahApp vs Competitors Matrix)

| Feature | Shaadi/BM | Jeevansathi | **VivahApp** | Our Status | Sprint | Task |
|---------|----------|-------------|-------------|------------|--------|------|
| Photos | 12 photos | 12 photos | 25 photos + AI beauty retouch | 🟡 Partial | Sprint 11 | Multi-photo gallery up to 25 |
| Bio-Data PDF | Basic | Basic | Branded PDF with QR code + share link | ❌ Not Done | Sprint 12 | `BiodataGenerator.kt` + QR code |
| AI Profile Writer | ❌ | ❌ | 1-click AI About Me generation | 🟡 Partial | Sprint 9 | `BioGeneratorScreen.kt` — improve |
| Video Introduction | ❌ | ❌ | 60-sec in-app video profile | 🟡 Partial | Sprint 9 | `VideoProfileScreen.kt` — complete |
| Voice Bio | ❌ | ❌ | 30-sec voice introduction | ❌ Not Done | Sprint 12 | New: `VoiceBioScreen.kt` + Firebase Storage audio |
| Multilingual Profile | Hindi + English | Hindi + English | 22 Indian languages | ❌ Not Done | Sprint 13 | Telugu + Hindi strings.xml first |
| Family Co-Access Login | Basic | Basic | Granular read/approve roles per family member | ❌ Not Done | Sprint 13 | `FamilyPortalScreen.kt` |
| Dark Mode | ❌ | Partial | Full adaptive dark/light | 🟡 Partial | Sprint 10 | Complete dark theme in `ui/theme/` |

### 1.2 Matchmaking & Discovery Features

| Feature | Shaadi/BM | Jeevansathi | **VivahApp** | Our Status | Sprint | Task |
|---------|----------|-------------|-------------|------------|--------|------|
| Daily Recommendations | Rule-based | Rule-based | Hybrid ML + Collaborative Filter | ❌ Not Done | Sprint 13 | `RecommendationEngine.kt` |
| Filter Depth | 12 filters | 15 filters | 40+ filters inc. personality type | 🟡 Partial | Sprint 10 | Add 25+ missing filters |
| Kundali Matching | Manual upload | Manual upload | AI auto-generated + 36 Gun | 🟡 Partial | Sprint 10 | Complete ephemeris calculation |
| NRI / Diaspora Search | ✅ | ✅ | + Timezone-aware scheduling | ❌ Not Done | Sprint 12 | Add timezone scheduling for calls |
| 'Near Me' GPS Search | ❌ | ❌ | City-district level (safe, no GPS coords) | ❌ Not Done | Sprint 12 | City-level proximity search |
| Personality Match | ❌ | ❌ | 16-personality type compatibility | ❌ Not Done | Sprint 13 | `PersonalityQuizScreen.kt` + matching |
| Lifestyle Score | ❌ | ❌ | Diet + fitness + values alignment | ❌ Not Done | Sprint 11 | Add to `CompatibilityEngine.kt` |
| Reverse Compatibility | ❌ | ❌ | See who YOU match FOR THEM | ❌ Not Done | Sprint 12 | Bidirectional compatibility display |
| AI Conversation Starter | ❌ | ❌ | Context-aware first message | ❌ Not Done | Sprint 11 | AI icebreaker on match card |
| AI Mutual Friends Match | ❌ | ❌ | LinkedIn graph overlap detection | ❌ Not Done | Sprint 15 | Optional future feature |
| Compatibility Report | Basic | Basic | 12-page AI-generated PDF report | ❌ Not Done | Sprint 14 | `CompatibilityReportGenerator.kt` |

### 1.3 Communication Features

| Feature | Shaadi/BM | Jeevansathi | **VivahApp** | Our Status | Sprint | Task |
|---------|----------|-------------|-------------|------------|--------|------|
| In-App Messaging | Paywalled | Paywalled | Free for all matches | ✅ Done | Sprint 4 | Already free post-accept |
| Voice Messages | ❌ | ❌ | Up to 10 min, edge-stored | ❌ Not Done | Sprint 12 | AudioRecord + Firebase Storage |
| Video Calling | ❌ | ❌ | WebRTC via Agora/Daily.co | ❌ Not Done | Sprint 12 | Agora SDK |
| Group Family Call | ❌ | ❌ | Up to 10 participants | ❌ Not Done | Sprint 14 | Multi-party call (Agora channel) |
| Message Translation | ❌ | ❌ | Real-time 22-language translation | ❌ Not Done | Sprint 14 | Firebase ML + Google Translate API |
| Typing Indicator | ✅ | ❌ | + read receipts + delivery status | ❌ Not Done | Sprint 11 | Firestore presence field |
| Message Reactions | ❌ | ❌ | Full emoji reaction layer | ❌ Not Done | Sprint 12 | `reactions` array in message doc |
| Voice-to-Text Chat | ❌ | ❌ | Whisper AI transcription | ❌ Not Done | Sprint 14 | Google Speech-to-Text API |
| Schedule Video Date | ❌ | ❌ | Calendar invite + reminders | ❌ Not Done | Sprint 12 | Calendar integration + FCM reminder |
| E2E Encryption | ❌ | ❌ | AES-256-GCM, keys never on server | ❌ Not Done | Sprint 13 | Signal Protocol for messages |
| Anti-Contact-Share AI | Basic | Basic | Real-time number masking in chat | 🟡 Partial | Sprint 8 | Complete masking enforcement |

### 1.4 Safety, Trust & Verification Features

| Feature | Status | Sprint | File(s) | Task |
|---------|--------|--------|---------|------|
| Aadhaar eKYC (UIDAI sandbox) | ❌ Not Done | Sprint 12 | New: `VerificationScreen.kt` | UIDAI sandbox API integration |
| DigiLocker integration (degree, employment certs) | ❌ Not Done | Sprint 13 | New: `VerificationScreen.kt` | DigiLocker API |
| Live selfie verification (face match vs Aadhaar) | ❌ Not Done | Sprint 12 | `LivenessCheckScreen.kt` | ML Kit FaceDetection |
| Duplicate face detection across database | ❌ Not Done | Sprint 12 | Cloud Function `onPhotoUpload` | pHash + face embedding comparison |
| Photo reverse-search (detect stock/stolen images) | ❌ Not Done | Sprint 13 | Cloud Function | Google Vision API reverse image search |
| Device fingerprinting (canvas + WebGL hash) | ❌ Not Done | Sprint 11 | `AuthRepository.kt` | Android `Settings.Secure.ANDROID_ID` + device model |
| Criminal background check API (AuthBridge) | 🟡 Partial | Sprint 9 | `BackgroundCheckScreen.kt` | Wire to AuthBridge API |
| Employment verification (company email ping) | ❌ Not Done | Sprint 13 | `VerificationScreen.kt` | Company email OTP verification |
| Emergency SOS (GPS share with trusted contact) | ❌ Not Done | Sprint 13 | `SafetyCenterScreen.kt` | SOS button → share GPS + alert |
| Scam sentence detector (flag romance scam scripts) | 🟡 Partial | Sprint 9 | `FakeProfileDetector.kt` | Add script pattern detection |
| Photo watermarking (invisible steganographic) | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | Add watermark on upload |
| Zero-knowledge contact reveal (mutual explicit consent) | 🟡 Partial | Sprint 8 | `MatchDetailScreen.kt` | Both must explicitly consent |

---

## SECTION 2 — WORLD-EXCLUSIVE FEATURES (10 Unique Differentiators)

> These features don't exist on any Indian matrimony platform. Implementing 3+ creates viral moment.

| # | Feature | Description | Status | Sprint | File(s) | Task |
|---|---------|-------------|--------|--------|---------|------|
| EX-01 | **Vivah Score™** (our: Match Score™) | Proprietary 100-point trust + compatibility score (verification level + completeness + match history + AI personality) | ❌ Not Done | Sprint 11 | New: `MatchScoreEngine.kt` | Composite score: verification(40%) + completeness(20%) + compatibility(30%) + behavior(10%) |
| EX-02 | **Kundali AI Pro™** | Real-time Vedic astrology from DOB+time+place. 36-gun, Manglik, muhurat, remedial stones in natural language | 🟡 Partial | Sprint 13 | `KundliScreen.kt` + Cloud Function | Add natural language LLM interpretation |
| EX-03 | **Voice Personality Match** | 30-sec voice recording analyzed for speech patterns + personality compatibility | ❌ Not Done | Sprint 14 | New: `VoiceBioScreen.kt` | Audio analysis + personality tagging |
| EX-04 | **Shadow Profile Mode** | User exists in algorithm but invisible to direct search (VIP lounge) | ❌ Not Done | Sprint 11 | `UserEntity.isIncognito` | Full stealth mode — appears in matches for others, not in browse |
| EX-05 | **Family Approval Workflow** | User → Parent → Elder. Each has own dashboard, approve/reject with notes | ❌ Not Done | Sprint 13 | `FamilyPortalScreen.kt` | Multi-stage family approval flow |
| EX-06 | **Shagun Board™** (our: Success Stories) | Wedding photos from matched couples — massive social proof engine | ❌ Not Done | Sprint 15 | New: `SuccessStoriesScreen.kt` | User-submitted success stories |
| EX-07 | **Community Circles™** | Private community groups by city + community (structured, moderated) | 🟡 Partial | Sprint 9 | `CirclesScreen.kt` | Complete circles functionality |
| EX-08 | **Match Timeline** | Visual relationship timeline: interest→accepted→first call→family intro→engagement→wedding | ❌ Not Done | Sprint 13 | New: `MatchTimelineScreen.kt` | Milestone tracking per match pair |
| EX-09 | **AI Icebreaker Engine** | 5 personalized conversation starters based on both profiles' shared interests | ❌ Not Done | Sprint 11 | `ProfileCard.kt` | AI-generated icebreaker chips on match card |
| EX-10 | **Vivah Verified™ Badge** (our: Match Verified™) | Gold-tier badge: all verification levels + manual team review | ❌ Not Done | Sprint 13 | Admin workflow + `VerificationScreen.kt` | Highest trust tier |

---

## SECTION 3 — SECURITY ARCHITECTURE

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| SEC-01 | Root detection | ❌ Not Done | Sprint 10 | `MainActivity.kt` | RootBeer library |
| SEC-02 | SSL/TLS certificate pinning | ❌ Not Done | Sprint 10 | `network_security_config.xml` | Pin Firebase + Razorpay domains |
| SEC-03 | FLAG_SECURE on all photo/contact/chat screens | ❌ Not Done | Sprint 10 | `MainActivity.kt` | Per-screen FLAG_SECURE |
| SEC-04 | Anti-bot rate limiting for OTP | 🟡 Partial | Sprint 10 | `firestore.rules` | Add write rate limits |
| SEC-05 | Photo content moderation (NSFW) | ❌ Not Done | Sprint 11 | Cloud Function `onPhotoUpload` | Vision API NSFW detection |
| SEC-06 | Argon2id password hashing (Firebase Auth handles this) | ✅ Done | 1 | Firebase Auth managed | Firebase uses bcrypt equivalent |
| SEC-07 | Device fingerprinting for multi-account prevention | ❌ Not Done | Sprint 11 | `AuthRepository.kt` | Android ID + device fingerprint |
| SEC-08 | Admin TOTP authentication | ❌ Not Done | Sprint 15 | Firebase Admin project | Separate admin project + TOTP |

---

## SECTION 4 — AI & SMART MATCHMAKING ENGINE

| Req | Feature | Status | Sprint | File(s) | Task |
|-----|---------|--------|--------|---------|------|
| AI-01 | Compatibility score (0–100) shown on every profile card | ❌ Not Done | Sprint 11 | `ProfileCard.kt` + `CompatibilityEngine.kt` | Compute + display |
| AI-02 | Collaborative filtering (users similar to you liked X) | ❌ Not Done | Sprint 13 | `RecommendationEngine.kt` | TFLite on-device |
| AI-03 | Content-based filtering (you liked attribute Y → suggest more) | ❌ Not Done | Sprint 13 | `RecommendationEngine.kt` | Feature vector similarity |
| AI-04 | Behavioral implicit signals (time on profile, photos viewed) | ❌ Not Done | Sprint 12 | `BehavioralSignalService.kt` | Track and feed into model |
| AI-05 | Personality quiz (16 types) | ❌ Not Done | Sprint 13 | `PersonalityQuizScreen.kt` | 5-10 question quiz → type mapping |
| AI-06 | AI-powered bio generator | 🟡 Partial | Sprint 9 | `BioGeneratorScreen.kt` | Multiple templates + random selection |
| AI-07 | Kundali AI interpretation (natural language) | ❌ Not Done | Sprint 13 | Cloud Function | Gemini/GPT-based kundali narrative |
| AI-08 | Fake profile detection (weighted heuristics) | 🟡 Partial | Sprint 9 | `FakeProfileDetector.kt` | Improve: add image analysis |
| AI-09 | Scam pattern detection in messages | 🟡 Partial | Sprint 9 | `FakeProfileDetector.kt` | Add message pattern scanning |
| AI-10 | Churn prediction | ❌ Not Done | Sprint 14 | Cloud Functions ML | Boost inactive user matches |

---

## SECTION 5 — PERFORMANCE TARGETS

| Metric | VivahApp Target | Action | Status | Sprint | Task |
|--------|----------------|--------|--------|--------|------|
| App launch | < 2 seconds on mid-range Android | Baseline Profiles + startup tracing | 🟡 Partial | Sprint 10 | Add Baseline Profiles to CI |
| Profile card swipe | < 100ms render | Optimize Coil + lazy list | ❌ Not Done | Sprint 10 | Profile compose tracing |
| Firestore query | < 200ms p95 | Composite indexes + caching | 🟡 Partial | Sprint 9 | Add missing indexes |
| FCM delivery | < 1 second for new interest | Verify trigger chain | ❌ Not Done | Sprint 11 | E2E latency test |
| Search results | < 500ms | Algolia / composite indexes | ❌ Not Done | Sprint 13 | Algolia integration |
| Image load | < 300ms for profile photo | Coil disk cache + WebP | 🟡 Partial | Sprint 11 | Add WebP conversion on upload |

---

## SECTION 6 — SCALE PATH (₹0 → Revenue → Profit)

| Phase | Milestone | Status | Sprint | Key Metric |
|-------|---------|--------|--------|-----------|
| Phase 0 | < 1,000 users — Firebase free tier | 🟡 Active | Current | Monitor Firestore reads/writes daily |
| Phase 1 | 1,000–10,000 users — Firebase Blaze plan | ❌ Not Done | Sprint 10 | Switch billing before 1,000 DAU |
| Phase 2 | 10,000–100,000 — Algolia + Functions optimization | ❌ Not Done | Sprint 13 | Add Algolia for search |
| Phase 3 | 100,000+ — Firestore sharding + CDN | ❌ Not Done | Sprint 16 | Evaluate Firestore capacity |
| Revenue | ₹399/₹799/₹1,499 subscriptions via Razorpay | 🟡 Partial | Sprint 10 | Complete all 3 tiers |
| Ads | AdMob rewarded ads → 1 free contact/day | ❌ Not Done | Sprint 12 | `RewardedAdManager.kt` |

---

## SECTION 7 — SPRINT ALLOCATION

| Sprint | Focus | Key Deliverables |
|--------|-------|-----------------|
| Sprint 10 | Core gaps | Root detection, FLAG_SECURE, dark mode, firebase billing, SSL pinning |
| Sprint 11 | Unique features | Match Score™, AI icebreaker engine, voice bio, shadow profile mode, notification system |
| Sprint 12 | Verification + calling | Aadhaar eKYC, liveness check, pHash, voice/video calling (Agora), emergency SOS |
| Sprint 13 | AI engine | Recommendation engine, personality quiz, Kundali AI, photo reverse-search, Algolia |
| Sprint 14 | Premium features | Family approval workflow, match timeline, group family call, compatibility report PDF |
| Sprint 15 | Community + SEO | Success stories (Shagun Board), community circles, SEO web pages |

---

## DEFINITION OF DONE (10/10 Checklist)

- [ ] Match Score™ (100-point composite) displayed on every profile card
- [ ] AI icebreaker engine: 5 personalized starters on every match card
- [ ] Voice bio (30-sec recording) uploaded and playable on profile
- [ ] Shadow Profile Mode (stealth — in algorithm but invisible in browse) 
- [ ] Family Approval Workflow (3-stage: User → Parent → Elder)
- [ ] Success Stories / Shagun Board (moderated, shareable)
- [ ] Community Circles fully functional
- [ ] Match Timeline (milestone tracking per pair)
- [ ] Voice + video calling (Agora SDK, no number reveal)
- [ ] Aadhaar eKYC + liveness check operational
- [ ] pHash duplicate face detection on photo upload
- [ ] Personality quiz (16 types) + personality-based compatibility
- [ ] 22 language support (Telugu + Hindi minimum)
- [ ] All 10 world-exclusive features shipped
