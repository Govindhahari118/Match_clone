# PLAN 06 — Matrimony_Platform_Build_Spec_v2.pdf
## End-to-End Pin-to-Pin Implementation Plan

> **Source:** `docs/Matrimony_Platform_Build_Spec_v2.pdf`  
> **Original Architecture:** Cloudflare Workers + D1 + KV + R2 + Durable Objects + Workers AI  
> **Our Stack:** Firebase-first Android (Kotlin + Compose + Firestore + Cloud Functions + Storage)  
> **Extracted Content:** Workers AI for Kundali, authentication flows, payment integration,  
>   cold-start optimization, scaling costs, deployment timeline, SEO, multilingual support  
> **Approach:** Extract every BUILD SPECIFICATION, adapt to Firebase stack, implement pin-to-pin

---

## STATUS LEGEND
- ✅ **Done** — Implemented in Sprints 1–9
- 🟡 **Partial** — Started but incomplete
- ❌ **Not Done** — Zero implementation

---

## SECTION 1 — AUTHENTICATION BUILD SPECIFICATION

> Source specifies: Better Auth + Workers. Firebase adaptation below.

| Spec | Original (Cloudflare) | Firebase Equivalent | Status | Sprint | File(s) | Task |
|------|----------------------|---------------------|--------|--------|---------|------|
| Mobile OTP (primary) | Fast2SMS → D1 otp_log | Firebase Phone Auth | ✅ Done | 1 | `AuthRepository.kt` |  |
| OTP TTL | 10 min, 3 attempts max | Cloud Function rate limit | 🟡 Partial | Sprint 10 | `AuthRepository.kt` | Add 3-attempt lockout |
| OTP hash | Argon2id | Firebase Auth managed | ✅ Done | 1 | Firebase managed |  |
| JWT Access Token | RS256, 15-min TTL | Firebase ID Token (1h) | ✅ Done | 1 | Firebase managed |  |
| Refresh Token | 30-day, HttpOnly cookie | Firebase Refresh Token | ✅ Done | 1 | Firebase managed |  |
| Social Login | Better Auth Google + Apple | Firebase Auth providers | ✅ Done | 1 | `AuthRepository.kt` |  |
| Apple Sign-In mandatory (iOS) | Required per Apple policy | Firebase Apple provider | ❌ Not Done | Sprint 14 | iOS build only |  |
| Session storage | D1 sessions + KV hot cache | Firestore `sessions/{uid}` | ❌ Not Done | Sprint 10 | Cloud Function | Server-side session revocation |
| Device binding | Canvas + WebGL fingerprint | Android ID + device model | ❌ Not Done | Sprint 11 | `AuthRepository.kt` | `Settings.Secure.ANDROID_ID` |
| Admin Auth | Separate tenant + TOTP | Firebase separate project | ❌ Not Done | Sprint 15 | Admin Firebase project |  |
| OTP audit log | `otp_log` D1 table | Firestore `otpLog/{hash}` | ❌ Not Done | Sprint 10 | Cloud Function | IP + attempt tracking |

---

## SECTION 2 — SERVICE STACK BUILD SPECIFICATION

> Source maps each Cloudflare service to its equivalent in Firebase.

| Service Function | Cloudflare Original | Firebase Equivalent | Status | Sprint | Task |
|-----------------|--------------------|--------------------|--------|--------|------|
| Backend API | Workers (V8 isolates, <2ms CPU) | Cloud Functions (Node.js) | ✅ Done | 1 | Multiple Cloud Functions |
| Frontend + SSR cache | Cloudflare Pages + Edge Cache | Firebase Hosting + Next.js | ❌ Not Done | Sprint 15 | Web profile pages |
| Session / OTP / cache | KV namespace (1ms lookup) | Firestore + in-memory cache | 🟡 Partial | Sprint 10 | Add session docs |
| Photos, docs, audio | R2 (S3-compatible, zero egress) | Firebase Storage | ✅ Done | 3 | `FirebaseStorageService.kt` |
| WebSocket / real-time | Durable Objects | Firestore real-time listeners | ✅ Done | 4 | `ChatRepository.kt` |
| Semantic search / embeddings | Workers AI embeddings | Algolia / Firebase extension | ❌ Not Done | Sprint 13 | Algolia integration |
| Bot detection | Better than reCAPTCHA (Workers) | Firebase App Check | ❌ Not Done | Sprint 11 | Enable App Check in `app/build.gradle.kts` |
| OTP / alerts / welcome | Fast2SMS (₹0 startup) | Firebase Auth SMS + Resend.com | 🟡 Partial | Sprint 1 | Add email notifications |
| Rate limiting | D1 + KV counter | `firestore.rules` rate limits | ❌ Not Done | Sprint 11 | Add rate-limit rules |
| Photo crop/resize/NSFW | Workers AI (Cloudflare) | Cloud Functions + Vision API | ❌ Not Done | Sprint 11 | `onPhotoUpload` Cloud Function |
| City/religion lists (static) | KV cached | Assets JSON files in APK | 🟡 Partial | Sprint 10 | Add complete city lists JSON |
| Email templates | React Email + Workers | Firebase Email Extension (Resend) | ❌ Not Done | Sprint 12 | Install Trigger Email extension |
| Profile SEO pages | Cloudflare Pages (SSR) | Firebase Hosting + Next.js | ❌ Not Done | Sprint 15 | Profile page generation |
| FCM/APNS | Firebase | Firebase | ✅ Done | 7 | `FirebaseNotificationService.kt` |
| Payments | Razorpay / Stripe | Razorpay + Cloud Function | 🟡 Partial | Sprint 9 | Complete all tiers |

---

## SECTION 3 — KUNDALI ENGINE BUILD SPECIFICATION

> Source spec: Swiss Ephemeris WASM + Workers AI LLM → async queue pattern.  
> Firebase adaptation: Cloud Function async queue.

| Spec | Original | Firebase Equivalent | Status | Sprint | File(s) | Task |
|------|---------|---------------------|--------|--------|---------|------|
| Kundali computation | Swiss Ephemeris WASM in Worker | Async Cloud Function (Node.js ephemeris lib) | 🟡 Partial | Sprint 10 | `KundliScreen.kt` + Cloud Function | Move computation to Cloud Function |
| Async queue for Kundali | Cloudflare Queue | Cloud Tasks / Firestore job queue | ❌ Not Done | Sprint 11 | Cloud Function | Queue-based Kundali computation |
| Kundali result storage | D1 horoscopes + KV cache | Firestore `horoscopes/{uid}` | ❌ Not Done | Sprint 11 | Cloud Function | Store result in Firestore |
| LLM interpretation | Workers AI (Llama-3.1) | Gemini API via Cloud Function | ❌ Not Done | Sprint 13 | Cloud Function `interpretKundali` | Natural language horoscope narrative |
| Push notification on complete | Workers + FCM | FCM from Cloud Function | ❌ Not Done | Sprint 11 | Cloud Function | "Your Kundali is ready" notification |
| Cache subsequent requests | KV (zero compute) | Firestore cache hit | ✅ Done | Sprint 9 | Room + Firestore |  |
| Rashi calculation | Workers AI / ephemeris | On-device or Cloud Function | 🟡 Partial | Sprint 10 | `KundliScreen.kt` | Complete Rashi calculation |
| Nakshatra calculation | Workers AI / ephemeris | On-device or Cloud Function | 🟡 Partial | Sprint 10 | `KundliScreen.kt` | Complete Nakshatra |

---

## SECTION 4 — PHOTO PIPELINE BUILD SPECIFICATION

| Spec | Action | Status | Sprint | File(s) | Task |
|------|--------|--------|--------|---------|------|
| Crop + resize on upload | Compress + resize before Firebase Storage upload | 🟡 Partial | Sprint 11 | `FirebaseStorageService.kt` | Add `Bitmap.compress(WebP)` before upload |
| NSFW detection | Vision API SafeSearch on upload | ❌ Not Done | Sprint 11 | Cloud Function `onPhotoUpload` | Flag/reject NSFW on upload trigger |
| Face detection (reject non-face) | ML Kit FaceDetection client-side | ❌ Not Done | Sprint 11 | `PhotoEditorScreen.kt` | ML Kit before upload |
| pHash duplicate detection | Perceptual hash comparison against DB | ❌ Not Done | Sprint 12 | Cloud Function `onPhotoUpload` | pHash + similarity threshold |
| Photo watermarking | Invisible watermark on upload | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | Canvas bitmap watermark |
| Blurred until consent | Blur photo in profile card for non-matches | ❌ Not Done | Sprint 11 | `ProfileCard.kt` | Blur Composable until match established |
| WebP conversion | Convert all uploads to WebP for compression | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | Bitmap.compress(WebP, 85) |
| Photo privacy enforcement | All/Connected/Premium/None levels | 🟡 Partial | Sprint 9 | `PrivacyDashboardScreen.kt` | Enforce in display logic |
| Profile photo: single face only | ML Kit: reject if no clear face in primary slot | ❌ Not Done | Sprint 11 | `PhotoEditorScreen.kt` | Primary photo face detection |

---

## SECTION 5 — SCALING COST BUILD SPECIFICATION

> Source analyzed Cloudflare cost tiers. Firebase equivalent cost planning.

| User Scale | Firebase Services Needed | Monthly Cost | Action | Status |
|-----------|------------------------|-------------|--------|--------|
| 0–500 DAU | Firebase Spark (free) | ₹0 | Active | ✅ Current |
| 500–2,000 DAU | Firebase Blaze + basic Cloud Functions | ~$5–20/month | Upgrade billing | ❌ Not Done |
| 2,000–10,000 DAU | Blaze + Algolia free tier + FCM | ~$30–80/month | Add Algolia | ❌ Sprint 13 |
| 10,000–50,000 DAU | Blaze + Algolia Grow + CDN + Crashlytics | ~$100–400/month | Monitor + optimize | ❌ Sprint 15 |
| 50,000+ DAU | Firestore sharding + Firebase App Check | ~$500+/month | Architecture review | ❌ Sprint 16+ |

**Actions Required:**
1. Sprint 10: Set up Firebase Billing alerts at $10, $50, $100 thresholds
2. Sprint 10: Verify Firestore region = `asia-south1` (Mumbai) for India latency
3. Sprint 13: Evaluate Algolia vs. Firestore for search at scale

---

## SECTION 6 — DEPLOYMENT TIMELINE BUILD SPECIFICATION

> Source specified 16-week deploy plan. Mapped to our Firebase CI/CD.

| Phase | Original (Cloudflare) | Firebase Equivalent | Status | Sprint | Task |
|-------|----------------------|---------------------|--------|--------|------|
| Phase 0: Auth + OTP | Week 1–2: auth Worker + D1 | Firebase Auth + Cloud Functions | ✅ Done | Sprint 1 |  |
| Phase 1: Profile + Matching | Week 1–4: rule-based v1 + Android | Firestore profile + Paging 3 | ✅ Done | Sprint 1–5 |  |
| Phase 2: Communication | Week 5–8: Durable Objects chat + privacy | Firestore real-time chat + privacy | ✅ Done | Sprint 4–6 |  |
| Phase 3: AI + Kundali | Week 5–8: WASM ephemeris + LLM | Cloud Function async Kundali | 🟡 Partial | Sprint 11 | Async Kundali Cloud Function |
| Phase 4: Payments | Week 9–12: Razorpay + Cloud Function verify | Razorpay + Cloud Function | 🟡 Partial | Sprint 9 | Complete all tiers |
| Phase 5: Photo pipeline | Week 9–12: R2 + Vision AI | Firebase Storage + Vision API | ❌ Not Done | Sprint 11 | NSFW + face detection |
| Phase 6: Analytics + SEO | Week 13–16: PostHog + Cloudflare Pages | Firebase Analytics + Hosting | 🟡 Partial | Sprint 15 | Add web profile pages |
| Phase 7: iOS + Multilingual | Week 13–16: iOS app + 22 languages | — (Android first, Telugu + Hindi) | ❌ Not Done | Sprint 13 | Telugu strings |

### CI/CD Pipeline Build Spec

| Step | Status | Sprint | File(s) | Task |
|------|--------|--------|---------|------|
| GitHub Actions lint (Detekt + Ktlint) | ✅ Done | Sprint 9 | `.github/workflows/android.yml` |  |
| Unit test run | 🟡 Partial | Sprint 11 | `.github/workflows/android.yml` | Add test coverage threshold |
| Debug APK build | ✅ Done | Sprint 1 | `build-release.bat` |  |
| Release APK build (signed) | 🟡 Partial | Sprint 12 | `build-release.bat` | Complete signing automation |
| Firebase App Distribution beta | ❌ Not Done | Sprint 11 | `.github/workflows/android.yml` | Add App Distribution step |
| Play Store internal track | ❌ Not Done | Sprint 15 | `.github/workflows/android.yml` | Add Play Publisher API step |
| Firestore rules deploy | 🟡 Partial | Sprint 9 | `firebase-full.bat` | Automate in CI |
| Cloud Functions deploy | 🟡 Partial | Sprint 9 | `firebase-full.bat` | Automate in CI |
| Performance monitoring | 🟡 Partial | Sprint 9 | Firebase Performance |  |

---

## SECTION 7 — SECURITY BUILD SPECIFICATION

| Spec | Status | Sprint | File(s) | Task |
|------|--------|--------|---------|------|
| AES-256 encryption for stored data | ❌ Not Done | Sprint 12 | `ChatRepository.kt` | E2E encrypt message content |
| Firebase App Check (bot prevention) | ❌ Not Done | Sprint 11 | `app/build.gradle.kts` | Enable App Check + Play Integrity API |
| FLAG_SECURE on sensitive screens | ❌ Not Done | Sprint 10 | `MainActivity.kt` | Add per-screen FLAG_SECURE |
| Root/jailbreak detection | ❌ Not Done | Sprint 10 | `MainActivity.kt` | RootBeer library |
| SSL certificate pinning | ❌ Not Done | Sprint 10 | `network_security_config.xml` | Pin Firebase + Razorpay domains |
| Rate limiting in Firestore rules | ❌ Not Done | Sprint 11 | `firestore.rules` | Max write rate per user |
| Anti-screenshot watermark | ❌ Not Done | Sprint 11 | `FirebaseStorageService.kt` | Watermark on upload |
| Secrets management | ✅ Done | Sprint 9 | `local.properties` |  |
| ProGuard/R8 release obfuscation | ✅ Done | Sprint 1 | `proguard-rules.pro` |  |

---

## SECTION 8 — MULTILINGUAL BUILD SPECIFICATION

| Language | Priority | Status | Sprint | Task |
|---------|---------|--------|--------|------|
| Telugu | P1 (primary community) | ❌ Not Done | Sprint 13 | `strings-te.xml` — profile + search + onboarding |
| Hindi | P1 (largest user base) | ❌ Not Done | Sprint 13 | `strings-hi.xml` |
| Tamil | P2 | ❌ Not Done | Sprint 14 | `strings-ta.xml` |
| Bengali | P2 | ❌ Not Done | Sprint 14 | `strings-bn.xml` |
| Marathi | P2 | ❌ Not Done | Sprint 14 | `strings-mr.xml` |
| Gujarati | P3 | ❌ Not Done | Sprint 15 | `strings-gu.xml` |
| English | ✅ Done | Sprint 1 | `strings.xml` |  |

**Implementation Task (Sprint 13):**
1. Add `app/src/main/res/values-te/strings.xml` for Telugu
2. Add `app/src/main/res/values-hi/strings.xml` for Hindi
3. Update `SettingsScreen.kt` — add language toggle
4. Use `Locale.forLanguageTag()` for runtime language change

---

## DEFINITION OF DONE (10/10 Checklist)

- [ ] Firebase Billing alerts configured ($10, $50, $100 thresholds)
- [ ] Firestore region confirmed as `asia-south1`
- [ ] Firebase App Check enabled (Play Integrity API)
- [ ] Async Kundali computation via Cloud Function queue
- [ ] Kundali AI LLM interpretation (Gemini API)
- [ ] NSFW detection on photo upload (Vision API)
- [ ] pHash duplicate face detection
- [ ] Photo WebP compression before upload
- [ ] Full CI/CD pipeline (lint → test → build → App Distribution → Play Store)
- [ ] Telugu + Hindi localization (strings-te.xml, strings-hi.xml)
- [ ] Session revocation via Cloud Function
- [ ] OTP audit log in Firestore
- [ ] Firebase App Check (bot prevention)
- [ ] SSL certificate pinning in `network_security_config.xml`
- [ ] All deployment steps automated in GitHub Actions
