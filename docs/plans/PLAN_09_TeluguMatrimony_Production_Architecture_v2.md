# PLAN 09 — TeluguMatrimony_Production_Architecture_v2.pdf
## End-to-End Pin-to-Pin Implementation Plan

> **Source:** `docs/TeluguMatrimony_Production_Architecture_v2.pdf`  
> **Context:** Production architecture specification for TeluguMatrimony app —  
>   deployment topology, infrastructure scaling, data architecture, CDN strategy,  
>   monitoring & observability, disaster recovery, Telugu-specific data concerns  
> **Architecture Described:** Cloudflare Edge + AWS fallback (production-grade)  
> **Adapted To:** Firebase production-grade deployment for Android app

---

## OVERLAP NOTE
> PLAN_09 is the infrastructure/deployment layer of PLAN_03 (TeluguMatrimony PRD).  
> Together they form the complete Telugu Matrimony implementation roadmap.

---

## STATUS LEGEND
- ✅ **Done** — Implemented in Sprints 1–9
- 🟡 **Partial** — Started but incomplete
- ❌ **Not Done** — Zero implementation

---

## SECTION 1 — PRODUCTION INFRASTRUCTURE ARCHITECTURE

### 1.1 Firebase Production Setup

| Component | Configuration | Status | Sprint | Task |
|-----------|--------------|--------|--------|------|
| Firebase Project | Production project (separate from dev) | ❌ Not Done | Sprint 12 | Create separate Firebase production project |
| Firestore Region | `asia-south1` (Mumbai) — closest to Telugu user base (AP/TG) | ❌ Not Done | Sprint 10 | Verify/set region in Firebase console |
| Firebase Storage Region | `asia-south1` | ❌ Not Done | Sprint 10 | Verify storage bucket region |
| Cloud Functions Region | `asia-south1` | ❌ Not Done | Sprint 11 | Set region in all Cloud Functions |
| Firebase Hosting | Closest CDN pop to India | ❌ Not Done | Sprint 15 | Deploy web profile pages |
| Firebase Blaze Plan | Required for production | ❌ Not Done | Sprint 10 | Upgrade before launch |
| Budget Alerts | ₹1,000 / $10 / $50 / $100 monthly alerts | ❌ Not Done | Sprint 10 | Firebase console budget alerts |
| Separate Environments | dev / staging / production | ❌ Not Done | Sprint 12 | Multiple Firebase projects + `google-services-{env}.json` |

### 1.2 Production vs Development App Config

| Config | Development | Production | Status | Sprint | Task |
|--------|-----------|------------|--------|--------|------|
| Firebase project | `match-dev` | `match-production` | ❌ Not Done | Sprint 12 | Create separate production Firebase project |
| Razorpay keys | Test keys in `local.properties` | Live keys in CI secrets | 🟡 Partial | Sprint 12 | Add live keys to GitHub Secrets |
| Root detection | Disabled in debug | Enabled in release | ❌ Not Done | Sprint 10 | `BuildConfig.DEBUG` gate |
| Crashlytics | Debug disabled | Release enabled | ✅ Done | Sprint 7 |  |
| ProGuard/R8 | Debug disabled | Release enabled | ✅ Done | Sprint 1 |  |
| FLAG_SECURE | Optional in debug | Required in release | ❌ Not Done | Sprint 10 | `!BuildConfig.DEBUG` gate |
| Fallback to destructive migration | DEBUG only | NEVER in release | ✅ Done | Sprint 8 |  |
| Log level | DEBUG verbose | ERROR only | ❌ Not Done | Sprint 12 | Strip debug logs in release build |
| API timeouts | Generous | Strict (10s read, 30s write) | ❌ Not Done | Sprint 11 | Add timeout configuration |

---

## SECTION 2 — DATA ARCHITECTURE (Telugu-Specific)

### 2.1 Telugu Community Data Initialization

> Production database must be pre-seeded with Telugu-specific reference data.

| Data Set | Description | Status | Sprint | File(s) | Task |
|---------|-------------|--------|--------|---------|------|
| Telugu castes | Reddy, Kamma, Kapu, Velama, Brahmin, Naidu, Yadav, SC/ST — 1,000+ entries | ❌ Not Done | Sprint 10 | `assets/castes_telugu.json` | Create comprehensive caste list |
| Telugu sub-castes | Dynamic sub-caste list per caste | ❌ Not Done | Sprint 11 | `assets/subcaste_telugu.json` | Map sub-castes to each caste |
| Telugu gothras | Kasyapa, Bharadwaja, Vasishta, Vishwamitra + 200+ others | ❌ Not Done | Sprint 11 | `assets/gothra_telugu.json` | Common Telugu gothras list |
| AP/TG cities | Hyderabad, Vijayawada, Vizag, Guntur, Tirupati, Warangal, Nellore + all districts | ❌ Not Done | Sprint 10 | `assets/cities_aptelangana.json` | Pre-load city suggestions |
| Global cities for NRI | USA (NJ, TX, CA hubs), UAE (Dubai), AUS (Sydney, Melbourne), UK (London), Singapore | ❌ Not Done | Sprint 10 | `assets/cities_nri.json` | NRI city suggestions |
| 27 Nakshatras | Complete list with Telugu names | ❌ Not Done | Sprint 11 | `assets/nakshatra.json` | Nakshatra list with Telugu labels |
| 12 Rashis | Complete list with Telugu names | ❌ Not Done | Sprint 11 | `assets/rashi.json` | Rashi list with Telugu labels |
| 10-Porutham rules | Algorithm rules for all 10 Poruthams | ❌ Not Done | Sprint 13 | `domain/KundliRepository.kt` | Encode 10-Porutham logic |
| Occupation categories | IT, Government, Healthcare, Business, Defence (Telugu-first order) | ❌ Not Done | Sprint 10 | `assets/occupations.json` | Occupation taxonomy |

### 2.2 Production Seed Data

| Data | Status | Sprint | File(s) | Task |
|------|--------|--------|---------|------|
| Production `SeedProvider.kt` disabled in release | ❌ Not Done | Sprint 12 | `SeedProvider.kt` | Gate seed data behind `BuildConfig.DEBUG` only |
| Admin console to manually add verified test profiles | ❌ Not Done | Sprint 15 | Admin panel | Backend seeding without app |
| Firestore indexes pre-built before launch | ❌ Not Done | Sprint 12 | `firestore.indexes.json` | Deploy all indexes before first user |

---

## SECTION 3 — SCALABILITY ARCHITECTURE

### 3.1 Firestore Scaling Plan

| Scale | Users | Action | Status | Sprint |
|-------|-------|--------|--------|--------|
| Pre-launch | 0 | Deploy all indexes | ❌ Not Done | Sprint 12 |
| Soft launch | 0–500 | Monitor reads/writes, Firebase Spark | 🟡 Partial | Sprint 12 |
| 500–2,000 DAU | Growth | Enable Firebase Blaze billing | ❌ Not Done | Sprint 10 |
| 2,000–10,000 DAU | Scale | Add Algolia for full-text search | ❌ Not Done | Sprint 13 |
| 10,000–50,000 DAU | Growth | Enable Firestore bundles for homepage data | ❌ Not Done | Sprint 15 |
| 50,000+ DAU | Scale | Evaluate Firestore collection group queries | ❌ Not Done | Sprint 16 |

### 3.2 Firebase Cloud Functions Scaling

| Function | Concurrency | Memory | Timeout | Status | Sprint | Task |
|---------|-------------|--------|---------|--------|--------|------|
| `onPhotoUpload` | 1 (sequential per user) | 1GB | 60s | ❌ Not Done | Sprint 11 | Set concurrency + memory |
| `computeDailyMatches` | 200 | 512MB | 540s | ❌ Not Done | Sprint 11 | Scheduled function config |
| `computeKundali` | 10 | 2GB | 300s | ❌ Not Done | Sprint 11 | High memory for ephemeris |
| `verifyRazorpayPayment` | 100 | 256MB | 30s | 🟡 Partial | Sprint 10 | Set memory + timeout |
| `deleteUserAccount` | 10 | 512MB | 120s | ✅ Done | Sprint 9 |  |
| `onInterestSend` | 200 | 256MB | 15s | 🟡 Partial | Sprint 11 | Set configuration |
| `onNewMessage` | 500 | 256MB | 10s | 🟡 Partial | Sprint 11 | Set configuration |

---

## SECTION 4 — MONITORING & OBSERVABILITY

### 4.1 Firebase Monitoring Stack

| Tool | Purpose | Status | Sprint | Task |
|------|---------|--------|--------|------|
| Firebase Crashlytics | Crash reporting + ANR detection | ✅ Done | Sprint 7 |  |
| Firebase Performance Monitoring | Screen load times, network latency | 🟡 Partial | Sprint 9 | Add custom traces |
| Firebase Analytics | User behavior funnels | 🟡 Partial | Sprint 7 | Add all 20 required events |
| Firebase App Check | Bot/abuse prevention | ❌ Not Done | Sprint 11 | Enable in console + code |
| Firebase Remote Config | Feature flags + A/B | ✅ Done | Sprint 9 |  |
| Google Cloud Logging | Cloud Functions logs | ❌ Not Done | Sprint 12 | Set up Cloud Logging alerts |
| Firebase Alerts | Budget + error rate alerts | ❌ Not Done | Sprint 10 | Configure in Firebase console |

### 4.2 Custom Performance Traces

> Add these Firebase Performance custom traces in Sprint 11.

| Trace Name | What It Measures | File(s) | Task |
|------------|-----------------|---------|------|
| `profile_load_time` | Time to render full profile detail screen | `ProfileDetailScreen.kt` | `FirebasePerformance.getInstance().newTrace()` |
| `search_results_load` | Time from filter apply to results shown | `SearchScreen.kt` | Add trace around Firestore query |
| `chat_message_send` | Time from send tap to message visible | `ChatScreen.kt` | Trace message write + snapshot |
| `photo_upload_time` | Time from file select to Storage upload complete | `PhotoEditorScreen.kt` | Trace upload |
| `kundali_compute_time` | Time from request to Kundali displayed | `KundliScreen.kt` | Cloud Function duration |
| `daily_match_render` | Time from app open to home feed rendered | `HomeScreen.kt` | App startup trace |

### 4.3 Alerting Rules

| Alert | Condition | Action | Status | Sprint |
|-------|----------|--------|--------|--------|
| Crash rate > 1% | Crashlytics > 1% crash-free users | PagerDuty/email alert | ❌ Not Done | Sprint 12 |
| ANR rate > 0.5% | Crashlytics ANR rate | Alert | ❌ Not Done | Sprint 12 |
| Cloud Function errors > 5/hour | Cloud Logging | Alert | ❌ Not Done | Sprint 12 |
| Firestore cost > $10 | Firebase Budget alert | Alert | ❌ Not Done | Sprint 10 |
| FCM delivery rate < 90% | Firebase Messaging analytics | Alert | ❌ Not Done | Sprint 12 |
| Active users drop > 20% day-over-day | Analytics custom alert | Alert | ❌ Not Done | Sprint 12 |

---

## SECTION 5 — DISASTER RECOVERY & DATA BACKUP

| Req | Requirement | Status | Sprint | Task |
|-----|-------------|--------|--------|------|
| DR-01 | Firestore automatic backups (daily) | ❌ Not Done | Sprint 12 | Enable Firestore scheduled exports to Cloud Storage |
| DR-02 | Backup retention: 30 days | ❌ Not Done | Sprint 12 | Set lifecycle policy on backup bucket |
| DR-03 | Firebase Storage backup (critical: profile photos) | ❌ Not Done | Sprint 13 | Set up Storage transfer service |
| DR-04 | Firestore restore procedure documented | ❌ Not Done | Sprint 12 | Create runbook in `docs/deployment/` |
| DR-05 | Multi-region Firestore reads (failover) | ❌ Not Done | Sprint 16 | Advanced — evaluate at scale |
| DR-06 | Cloud Function version rollback procedure | ❌ Not Done | Sprint 12 | Document in `scripts/deploy/` |
| DR-07 | Room DB migration rollback not possible | ✅ Done | Sprint 8 | Handled by `fallbackToDestructiveMigration` (DEBUG only) |

---

## SECTION 6 — CDN & MEDIA DELIVERY

| Req | Requirement | Status | Sprint | File(s) | Task |
|-----|-------------|--------|--------|---------|------|
| CDN-01 | Firebase Storage CDN URLs for all photos | ✅ Done | Sprint 3 | `FirebaseStorageService.kt` | Storage download URLs are CDN-backed |
| CDN-02 | Image resizing Cloud Function (produce multiple sizes) | ❌ Not Done | Sprint 11 | Cloud Function `onPhotoUpload` | Generate thumbnail (200px) + medium (600px) + original |
| CDN-03 | Coil image cache strategy: memory (50MB) + disk (200MB) | ❌ Not Done | Sprint 11 | `AppModule.kt` | Configure Coil `OkHttpClient` with cache |
| CDN-04 | Progressive JPEG / WebP for all uploads | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | Convert to WebP 85% before upload |
| CDN-05 | Video profiles CDN delivery (Firebase Storage) | 🟡 Partial | Sprint 9 | `VideoProfileScreen.kt` | Verify Storage CDN URL used |
| CDN-06 | Voice bio CDN delivery | ❌ Not Done | Sprint 12 | `VoiceBioScreen.kt` | Firebase Storage URL |

---

## SECTION 7 — RELEASE & DEPLOYMENT PIPELINE (Production)

### 7.1 Play Store Submission Checklist

| Item | Status | Sprint | Task |
|------|--------|--------|------|
| Privacy policy URL (live HTTPS) | 🟡 Partial | Sprint 12 | Deploy to `https://match.app/privacy` via Firebase Hosting |
| Terms of Service URL | ❌ Not Done | Sprint 12 | Create + deploy |
| App icon (512×512 PNG, no alpha) | ✅ Done | Sprint 1 | `assets/branding/` |
| Feature graphic (1024×500) | ✅ Done | Sprint 1 | `assets/store/` |
| Screenshots (phone + tablet) | ✅ Done | Sprint 1 | `assets/store/screenshots/` |
| Content rating declaration (18+) | ❌ Not Done | Sprint 12 | Play Console questionnaire |
| Sensitive permissions declaration (Location, Camera) | ❌ Not Done | Sprint 12 | Play Console permissions declaration |
| Target API level ≥ 34 (Android 14) | ❌ Not Done | Sprint 12 | `app/build.gradle.kts` `targetSdk 34` |
| 64-bit support required | ✅ Done | Sprint 1 | Gradle `ndk.abiFilters` |
| AAB (App Bundle) instead of APK for Play Store | ❌ Not Done | Sprint 12 | Use `bundleRelease` |
| Signed release keystore | 🟡 Partial | Sprint 1 | `keystore.properties.template` | Generate production keystore |
| Play Store internal track (testers) | ❌ Not Done | Sprint 12 | Play Console + CI |
| Play Store review submission | ❌ Not Done | Sprint 15 | Play Console submission |

### 7.2 GitHub Actions CI/CD Production Pipeline

**File:** `.github/workflows/android.yml`

| Step | Status | Sprint | Task |
|------|--------|--------|------|
| Checkout code | ✅ Done | Sprint 1 |  |
| Setup JDK 17 | ✅ Done | Sprint 1 |  |
| Cache Gradle dependencies | ✅ Done | Sprint 1 |  |
| Run Ktlint | ✅ Done | Sprint 9 |  |
| Run Detekt | ✅ Done | Sprint 9 |  |
| Run unit tests + coverage | 🟡 Partial | Sprint 11 | Add coverage threshold |
| Build debug APK | ✅ Done | Sprint 1 |  |
| Build release AAB (signed) | ❌ Not Done | Sprint 12 | Add `bundleRelease` step + signing |
| Upload to Firebase App Distribution | ❌ Not Done | Sprint 11 | Add `firebase appdistribution:distribute` step |
| Deploy Firestore rules | ❌ Not Done | Sprint 12 | Add `firebase deploy --only firestore:rules` |
| Deploy Cloud Functions | ❌ Not Done | Sprint 12 | Add `firebase deploy --only functions` |
| Upload to Play Store internal track | ❌ Not Done | Sprint 15 | Add `fastlane supply` step |

---

## SECTION 8 — TELUGU-SPECIFIC PRODUCTION CONSIDERATIONS

| Req | Concern | Status | Sprint | Task |
|-----|---------|--------|--------|------|
| TG-PROD-01 | Telugu script rendering (Noto Sans Telugu) | ❌ Not Done | Sprint 13 | Add `font_noto_sans_telugu.ttf` to `assets/fonts/` |
| TG-PROD-02 | Bi-directional text support (Telugu LTR) | ✅ Done | Sprint 1 | Android natively supports Telugu |
| TG-PROD-03 | Telugu keyboard input (system keyboard support) | ✅ Done | Sprint 1 | Android system keyboard |
| TG-PROD-04 | Telugu voice search (future) | ❌ Not Done | Sprint 15 | Google Cloud Speech-to-Text Telugu |
| TG-PROD-05 | AP/TG time zone (IST UTC+5:30) as default | 🟡 Partial | Sprint 10 | `Locale.forLanguageTag("te-IN")` |
| TG-PROD-06 | Indian phone number format (+91 prefix) | 🟡 Partial | Sprint 1 | Firebase Auth phone |
| TG-PROD-07 | INR currency formatting (₹) | 🟡 Partial | Sprint 6 | `NumberFormat.getCurrencyInstance(Locale("en","IN"))` |
| TG-PROD-08 | Low-bandwidth optimization (Jio 4G rural AP/TG) | ❌ Not Done | Sprint 11 | Coil image low-quality placeholders + lazy loading |
| TG-PROD-09 | Date format: DD/MM/YYYY (Indian standard) | 🟡 Partial | Sprint 2 | Verify date formatting across all screens |
| TG-PROD-10 | Festival calendar (Ugadi, Sankranti, Diwali) — auspicious dates for matrimony | ❌ Not Done | Sprint 14 | `assets/telugu_festivals.json` + home screen banner |

---

## SPRINT ALLOCATION SUMMARY

| Sprint | Focus | Key Deliverables |
|--------|-------|-----------------|
| Sprint 10 | Production setup | Firebase Blaze, `asia-south1` region, budget alerts, separate prod project prep |
| Sprint 11 | Data seeding + CDN | Telugu castes/cities/gothras/nakshatras JSON assets, image resize Cloud Function, Coil cache config |
| Sprint 12 | Release pipeline | Separate prod/dev projects, production keystore, release AAB build, Play Store internal track |
| Sprint 13 | Telugua AI + fonts | Noto Sans Telugu font, 10-Porutham production algorithm, Cloud Functions scaling config |
| Sprint 14 | Monitoring + backup | Custom performance traces, alerting rules, Firestore scheduled backups, festival calendar |
| Sprint 15 | Play Store + web | Submission checklist complete, full CI/CD pipeline, Firebase Hosting SEO pages |

---

## DEFINITION OF DONE (10/10 Checklist)

- [ ] Firebase region confirmed as `asia-south1` (Mumbai) for all services
- [ ] Separate Firebase production project created
- [ ] Firebase Blaze billing enabled with budget alerts
- [ ] Production keystore generated and stored securely
- [ ] Release AAB built and signed in CI
- [ ] Play Store internal track upload automated in GitHub Actions
- [ ] All Firestore indexes deployed before first user
- [ ] Telugu caste/city/gothra/nakshatra/rashi JSON assets in APK
- [ ] Image resize Cloud Function (3 sizes: thumbnail/medium/original)
- [ ] Coil configured with 50MB memory + 200MB disk cache
- [ ] Firestore daily backup enabled (30-day retention)
- [ ] Custom Firebase Performance traces for all 6 key flows
- [ ] Alerting rules configured (crash > 1%, cost > $10, error > 5/hour)
- [ ] `SeedProvider.kt` gated behind `BuildConfig.DEBUG`
- [ ] Telugu font (Noto Sans Telugu) bundled in app
- [ ] Low-bandwidth optimization for Jio/rural AP+TG users
- [ ] Festival calendar in app (Ugadi, Sankranti banner)
- [ ] Play Store submission checklist 100% complete
