# Match App — Production Launch Plan (v1.0)
## Created: May 7, 2026 | Current State: 6.5/10 | Target: 9.5/10

---

## Current Scorecard

| System | Score | Blocker? |
|--------|-------|----------|
| Matching Engine | 8/10 | No |
| Analytics | 9/10 | No |
| Chat (E2E encrypted, Firestore) | 8/10 | No |
| App Update (Play Core) | 8/10 | No |
| Cloud Functions (15 live) | 7/10 | Minor |
| Subscription/Payment (Razorpay) | 7/10 | **YES** — gates unenforced |
| Notifications (FCM, 6 channels) | 7/10 | No |
| Photo Pipeline (Firebase Storage) | 7/10 | Minor |
| Performance | 6/10 | Minor |
| Security | 6/10 | **YES** — chat rules, no root detect |
| Onboarding | 5/10 | Minor |
| Verification (Level 1 only) | 4/10 | **YES** — trust badges fake |
| Privacy/Stealth | 4/10 | **YES** — not enforced |
| Family Portal | 4/10 | No (Phase 2) |
| Subscription Enforcement | 3/10 | **YES** — revenue broken |
| Voice/Video Calling | 2/10 | No (Phase 2) |

---

## PHASE 1: Revenue & Trust (MUST before launch)

### Sprint A — Subscription Gate Enforcement (Critical)
**Problem:** `SubscriptionPlans.canAccess()` is defined but NEVER called. Free users access everything.

**Tasks:**
1. Create `CheckFeatureAccessUseCase` — single point for all gate checks
2. Gate: Advanced filters (7+) → Silver+ only
3. Gate: "Who Viewed Me" full list → Silver+ only  
4. Gate: Contact reveal → Gold+ only (currently just counting)
5. Gate: Read receipts in chat → Gold+ only
6. Gate: Kundali deep dive report → Gold+ only
7. Gate: Boost activation → Silver+ (3/mo), Gold+ (5/mo), Plat (unlimited)
8. Gate: Stealth/Incognito mode → Platinum only
9. Gate: Voice/Video call (future) → Platinum only
10. Create `PaywallSheet` composable shown when any gate blocks
11. Wire PaywallSheet to navigate to PricingScreen with pre-selected plan
12. Add "Upgrade" CTA on blocked features (blur effect + lock icon)

**Files to create/modify:**
- NEW: `domain/usecase/CheckFeatureAccessUseCase.kt`
- NEW: `ui/common/PaywallSheet.kt`
- MODIFY: `ui/settings/SettingsScreen.kt` (disable advanced filter sections for Free)
- MODIFY: `ui/whoviewed/WhoViewedScreen.kt` (blur list items, show upgrade)
- MODIFY: `ui/deepcompat/CompatibilityDeepDiveScreen.kt` (gate full report)
- MODIFY: `ui/chat/ChatScreen.kt` (gate read receipts)
- MODIFY: `ui/discovery/SwipeDiscoveryScreen.kt` (gate boost button)

---

### Sprint B — Verification Pipeline (Critical for Trust)
**Problem:** Only phone OTP is real. Levels 2-5 use `delay(1500)` fake.

**Tasks:**
1. Level 2 (Government ID): Upload ID doc to Firebase Storage `verification/{uid}/id_doc`
2. Cloud Function `onVerificationSubmitted` → sets status to "pending_review"
3. Admin SDK or manual review flow (Firestore `verificationRequests` collection)
4. Cloud Function `approveVerification` → updates user `verificationLevel`
5. Level 3 (Photo Selfie Match): Upload selfie alongside ID photo
6. Cloud Function compares (can be manual review for v1)
7. Level 4 (Employment): Upload payslip/offer letter
8. Level 5 (Premium Verified): All 4 cleared + manual interview call
9. Show verification badges accurately on cards (1-5 stars or shield icons)
10. Remove fake `delay(1500)` submit logic
11. Add "Pending Review" state to UI with estimated timeframe

**Files to create/modify:**
- MODIFY: `ui/verification/VerificationViewModel.kt` (real upload + Firestore write)
- MODIFY: `functions/src/index.ts` (add `onVerificationSubmitted`, `approveVerification`)
- NEW: `data/remote/VerificationService.kt` (upload + status polling)
- MODIFY: `firestore.rules` (add `verificationRequests` read/write rules)
- MODIFY: `ui/common/` (add `VerificationBadge.kt` component)

---

### Sprint C — Privacy & Stealth Enforcement (Critical for User Safety)
**Problem:** Privacy toggles are stored but never enforced in discovery queries.

**Tasks:**
1. Firestore query in `FirestorePagingSource`: exclude users where `stealthMode == true`
2. Room query in `UserDao.allExcluding()`: filter out stealth users
3. When `showLastActive == false`: hide activity status from other users' views
4. When `showHoroscope == false`: hide rasi/nakshatra from profile cards
5. When `incomeDisclosure == "hidden"`: hide income band from profile view
6. When `incomeDisclosure == "range"`: show range not exact
7. Block screenshot notification: when FLAG_SECURE is active, log screenshot attempts
8. Enforce: premium contacts only visible after mutual interest (not just premium status)
9. Photo blur for non-premium viewers (applies to photo 2+ only, primary always visible)

**Files to create/modify:**
- MODIFY: `data/remote/FirestorePagingSource.kt` (add stealth/privacy query filters)
- MODIFY: `ui/profile/ProfileDetailScreen.kt` (respect showHoroscope, incomeDisclosure)
- MODIFY: `ui/matches/MatchesScreen.kt` (respect showLastActive)
- MODIFY: `data/local/dao/UserDao.kt` (add stealth filter to discovery query)
- NEW: `ui/common/BlurredPhoto.kt` (glassmorphism blur with upgrade CTA)

---

### Sprint D — Security Hardening (Critical)
**Problem:** Chat Firestore rules are too permissive. No root detection. No cert pinning.

**Tasks:**
1. Fix Firestore chat rules: participants array check (`request.auth.uid in resource.data.participants`)
2. Add `network_security_config.xml` with certificate pinning for Firebase domains
3. Integrate RootBeer library for root/emulator detection (warn, don't block)
4. Firebase App Check with Play Integrity (blocks bots/scripts from calling Cloud Functions)
5. Rate-limit profile views more aggressively (currently 50/hr, change to 20/hr for Free)
6. Add brute-force detection to sign-in (lock after 5 failed attempts, 15-min cooldown)
7. Validate all user inputs server-side in Cloud Functions (display name length, bio length, XSS)

**Files to create/modify:**
- MODIFY: `firestore.rules` (fix chat thread access rules)
- NEW: `app/src/main/res/xml/network_security_config.xml`
- MODIFY: `AndroidManifest.xml` (reference network_security_config)
- MODIFY: `app/build.gradle.kts` (add `rootbeer`, `firebase-appcheck-playintegrity`)
- NEW: `core/security/RootDetector.kt`
- MODIFY: `functions/src/index.ts` (input validation on callable functions)
- MODIFY: `ui/auth/SignInScreen.kt` (lockout after 5 failures)

---

## PHASE 2: Enhancement & Polish (Week 2-3)

### Sprint E — Photo Moderation & Multi-Gallery
**Tasks:**
1. Cloud Function `onPhotoUpload`: integrate Vision API SafeSearch (replace placeholder)
2. Auto-reject EXPLICIT/VERY_LIKELY photos, flag POSSIBLE for review
3. Client-side face detection before upload (ML Kit Face Detection)
4. Support up to 8 photos per profile with drag-to-reorder
5. WebP conversion in compression pipeline (30% smaller)
6. Light watermark with matrimony ID on shared/downloaded photos

**Files to modify/create:**
- MODIFY: `functions/src/index.ts` (`onPhotoUpload` → real Vision API call)
- MODIFY: `data/repo/PhotoRepository.kt` (multi-photo support, WebP)
- MODIFY: `ui/profile/ProfileScreen.kt` (photo gallery grid with reorder)
- NEW: `core/security/NsfwValidator.kt` (client-side pre-check)

---

### Sprint F — Notification Center & Preferences
**Tasks:**
1. Create `NotificationCenterScreen` — grouped list (Today / This Week / Earlier)
2. Per-type toggle preferences (messages, interests, matches, reminders, promotions)
3. Add Cloud Functions: `sendProfileIncompleteD2`, `sendProfileIncompleteD7`, `sendInactiveD30`
4. Add "Daily Match Digest" preference toggle
5. Badge count on bottom nav notification icon (real unread count)
6. Mark-all-as-read functionality

**Files to create/modify:**
- NEW: `ui/notifications/NotificationCenterScreen.kt` (replace current stub)
- NEW: `ui/notifications/NotificationPreferencesScreen.kt`
- MODIFY: `functions/src/index.ts` (add 3 scheduled functions)
- MODIFY: `data/session/SessionStore.kt` (notification preference keys)
- MODIFY: `ui/main/MainShell.kt` (badge count on nav icon)

---

### Sprint G — Profile Wizard (Replace Marketing Pager)
**Tasks:**
1. Replace OnboardingScreen 5-page pager with a real 7-step profile wizard:
   - Step 1: Basic Info (name, DOB, gender, looking for)
   - Step 2: Community (religion, caste, sub-caste, mother tongue, gothra)
   - Step 3: Education & Career (degree, field, institution, occupation, employer)
   - Step 4: Physical (height, weight, complexion, physical status)
   - Step 5: Family (father/mother occupation, siblings, family type/status/values)
   - Step 6: Lifestyle (diet, smoking, drinking, hobbies, fitness)
   - Step 7: Astrology (rasi, nakshatra, manglik, birth time, birth place)
2. Each step validates before allowing "Next"
3. ProfileCompletenessBar shown at top
4. "Skip for now" on optional steps (marks profile incomplete)
5. Save each step immediately to Room + Firestore (no data loss on back press)
6. End: celebratory animation + redirect to photo upload

**Files to create/modify:**
- NEW: `ui/onboarding/ProfileWizardScreen.kt` (7-step pager with data entry)
- NEW: `ui/onboarding/ProfileWizardViewModel.kt` (step validation, save)
- MODIFY: `ui/MatchRoot.kt` (route new users to ProfileWizard after auth)
- Keep existing OnboardingScreen as first-launch marketing intro (before sign-up)

---

### Sprint H — Performance & Play Store Readiness
**Tasks:**
1. Generate Baseline Profile (macrobenchmark module)
2. Firebase App Check integration in Cloud Functions (`context.app` verification)
3. Play Store Data Safety form documentation (what data collected, shared, retained)
4. Content rating questionnaire prep (18+ app due to matrimony)
5. Add Firebase Performance Monitoring for screen transitions and network calls
6. Enable R8 full mode with proper keep rules validation
7. Cold start optimization: defer non-critical init to after first frame

**Files to create/modify:**
- NEW: `benchmark/` module (Baseline Profile generator)
- MODIFY: `app/build.gradle.kts` (Firebase Performance, App Check deps)
- MODIFY: `functions/src/index.ts` (App Check enforcement)
- NEW: `docs/deployment/PLAY_STORE_DATA_SAFETY.md`

---

## PHASE 3: Differentiators (Post-Launch v1.1)

### Sprint I — Voice/Video Calling (Agora)
**Deferred to post-launch.** Requires:
- Agora SDK dependency + token server
- Cloud Function `generateAgoraToken` 
- Call UI with timer, mute, speaker toggle
- Call credit system (free: 2 calls/week, premium: unlimited)
- Call scheduling with calendar integration

### Sprint J — Family Portal
**Deferred to post-launch.** Requires:
- Family member invitation system (link-based)
- Parent sub-account with limited permissions
- Shared shortlist between family members
- Family approval workflow (accept/reject with reason)
- Biodata PDF template selection + generation (already partially built)

### Sprint K — AI Enhancements
**Deferred to post-launch.** Requires:
- "Why this match" natural language explanation (Gemini API)
- AI-generated bio suggestions
- Smart reply suggestions in chat
- Profile photo quality scoring with improvement tips

---

## Execution Priority & Dependencies

```
PHASE 1 (Must-have for launch):
  Sprint A (Sub Gates)  ──┐
  Sprint B (Verification) ├── Can run in parallel
  Sprint C (Privacy)     ──┤
  Sprint D (Security)    ──┘

PHASE 2 (Should-have):
  Sprint E (Photos)      ──┐
  Sprint F (Notif Center) ├── After Phase 1 complete
  Sprint G (Prof Wizard)  ──┤
  Sprint H (Performance)  ──┘ 

PHASE 3 (Post-launch):
  Sprint I (Voice/Video)  ── v1.1
  Sprint J (Family Portal) ── v1.2
  Sprint K (AI)           ── v1.3
```

---

## Success Criteria for Launch (9.5/10)

- [ ] Free users hit paywall when accessing premium features
- [ ] Subscription gates block at least 7 features correctly
- [ ] Verification upload works (admin reviews within 24-48h)
- [ ] Stealth/incognito users excluded from discovery queries
- [ ] Firestore chat rules validate participant membership
- [ ] App Check blocks unauthorized API access
- [ ] Photo moderation flags NSFW with Vision API
- [ ] 7-step profile wizard captures all Sprint-10 fields
- [ ] Notification preferences allow per-type toggle
- [ ] Play Store data safety form filled accurately
- [ ] Cold start < 2 seconds on mid-range device
- [ ] All 41 Gradle tasks pass (`assembleRelease`)
- [ ] No crash on fresh install → sign up → full flow
