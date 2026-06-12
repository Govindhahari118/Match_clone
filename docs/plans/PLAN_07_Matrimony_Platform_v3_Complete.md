# PLAN 07 — Matrimony_Platform_v3_Complete.pdf
## End-to-End Pin-to-Pin Implementation Plan

> **Source:** `docs/Matrimony_Platform_v3_Complete.pdf`  
> **Context:** Platform v3 "Complete" specification — the full production platform spec document  
>   covering all systems: auth, profiles, matching, communication, payments, moderation, SEO  
> **Architecture:** Cloudflare-based (Workers + D1 + KV + R2) — adapted to Firebase  
> **Key Focus of v3 Complete:** Full production system including all previously identified gaps resolved

---

## OVERLAP NOTE
> PLAN_07 builds on PLAN_05 (VivahApp Blueprint) and PLAN_06 (Build Spec) — focusing on  
> production-readiness aspects and complete system integration that "v3 Complete" implies.

---

## STATUS LEGEND
- ✅ **Done** — Implemented in Sprints 1–9
- 🟡 **Partial** — Started but incomplete
- ❌ **Not Done** — Zero implementation

---

## SECTION 1 — COMPLETE PROFILE SCHEMA (Production v3)

> v3 "Complete" implies all profile fields finalised for production. Full Room + Firestore schema.

### 1.1 Room DB — Complete User Schema (Target State)

| Column | Type | Default | Status | Sprint | Task |
|--------|------|---------|--------|--------|------|
| `id` | INTEGER PK | auto | ✅ Done | 1 |  |
| `firebaseUid` | TEXT | — | ✅ Done | 1 |  |
| `name` | TEXT | — | ✅ Done | 1 |  |
| `email` | TEXT | null | ✅ Done | 1 |  |
| `phone` | TEXT | null | ✅ Done | Sprint 8 |  |
| `gender` | TEXT | — | ✅ Done | 1 |  |
| `dateOfBirth` | TEXT | — | ✅ Done | 1 |  |
| `age` | INTEGER | — | ✅ Done | 1 |  |
| `ageBucket` | TEXT | — | ✅ Done | Sprint 9 |  |
| `height` | REAL | null | ✅ Done | 1 |  |
| `weight` | REAL | null | ❌ Not Done | Sprint 10 | Add `weight` column (Migration 16→17) |
| `complexion` | TEXT | null | ❌ Not Done | Sprint 10 | Add `complexion` column |
| `physicalStatus` | TEXT | null | ❌ Not Done | Sprint 10 | Add `physicalStatus` column |
| `motherTongue` | TEXT | null | ❌ Not Done | Sprint 10 | Add `motherTongue` column |
| `religion` | TEXT | — | ✅ Done | 1 |  |
| `caste` | TEXT | null | ✅ Done | 1 |  |
| `subCaste` | TEXT | null | ❌ Not Done | Sprint 10 | Add `subCaste` column |
| `gothra` | TEXT | null | ❌ Not Done | Sprint 10 | Add `gothra` column |
| `nakshatra` | TEXT | null | ❌ Not Done | Sprint 10 | Add `nakshatra` column |
| `rashi` | TEXT | null | ❌ Not Done | Sprint 10 | Add `rashi` column |
| `birthTime` | TEXT | null | ❌ Not Done | Sprint 10 | Add `birthTime` column |
| `manglik` | TEXT | null | ✅ Done | Sprint 8 |  |
| `familyValues` | TEXT | null | ✅ Done | Sprint 8 |  |
| `aboutFamily` | TEXT | null | ✅ Done | Sprint 8 |  |
| `fatherOccupation` | TEXT | null | ❌ Not Done | Sprint 10 | Add `fatherOccupation` |
| `motherOccupation` | TEXT | null | ❌ Not Done | Sprint 10 | Add `motherOccupation` |
| `siblingsCount` | INTEGER | null | ❌ Not Done | Sprint 10 | Add `siblingsCount` |
| `familyType` | TEXT | null | ❌ Not Done | Sprint 10 | Add `familyType` (Joint/Nuclear) |
| `familyStatus` | TEXT | null | ❌ Not Done | Sprint 10 | Add `familyStatus` (Middle/Affluent) |
| `education` | TEXT | null | ✅ Done | 1 |  |
| `educationField` | TEXT | null | ❌ Not Done | Sprint 10 | Add `educationField` |
| `institution` | TEXT | null | ❌ Not Done | Sprint 10 | Add `institution` |
| `graduationYear` | INTEGER | null | ❌ Not Done | Sprint 10 | Add `graduationYear` |
| `occupation` | TEXT | null | ✅ Done | 1 |  |
| `occupationCategory` | TEXT | null | ❌ Not Done | Sprint 10 | Add `occupationCategory` |
| `employer` | TEXT | null | ❌ Not Done | Sprint 10 | Add `employer` |
| `employerType` | TEXT | null | ❌ Not Done | Sprint 10 | Add `employerType` |
| `income` | TEXT | null | ✅ Done | 1 |  |
| `city` | TEXT | null | ✅ Done | 1 |  |
| `state` | TEXT | null | ✅ Done | 1 |  |
| `country` | TEXT | null | ❌ Not Done | Sprint 10 | Add `country` column |
| `countryOfResidence` | TEXT | null | ❌ Not Done | Sprint 10 | Add `countryOfResidence` (NRI) |
| `relocate` | TEXT | null | ❌ Not Done | Sprint 10 | Add `relocate` |
| `timezone` | TEXT | null | ❌ Not Done | Sprint 10 | Add `timezone` |
| `diet` | TEXT | null | ✅ Done | 1 |  |
| `smoking` | TEXT | null | ❌ Not Done | Sprint 10 | Add `smoking` |
| `alcohol` | TEXT | null | ❌ Not Done | Sprint 10 | Add `alcohol` |
| `hobbies` | TEXT | null | ❌ Not Done | Sprint 10 | Add `hobbies` (JSON array) |
| `fitnessActivities` | TEXT | null | ❌ Not Done | Sprint 10 | Add `fitnessActivities` |
| `citizenship` | TEXT | null | ❌ Not Done | Sprint 10 | Add `citizenship` |
| `isNRI` | INTEGER | 0 | ❌ Not Done | Sprint 10 | Add `isNRI` flag |
| `matrimonyId` | TEXT | null | ❌ Not Done | Sprint 10 | Add `matrimonyId` (TLG-XXXXX format) |
| `photoUrl` | TEXT | null | ✅ Done | 3 |  |
| `videoUrl` | TEXT | null | 🟡 Partial | Sprint 9 |  |
| `voiceBioUrl` | TEXT | null | ❌ Not Done | Sprint 12 | Add `voiceBioUrl` |
| `profileCompleteness` | REAL | 0.0 | ❌ Not Done | Sprint 11 | Add calculated completeness |
| `verificationLevel` | INTEGER | 0 | ❌ Not Done | Sprint 10 | Add `verificationLevel` (0–5) |
| `isIncognito` | INTEGER | 0 | ✅ Done | Sprint 8 |  |
| `stealthMode` | INTEGER | 0 | ❌ Not Done | Sprint 11 | Add full stealth (Shadow Profile Mode) |
| `showLastActive` | INTEGER | 1 | ❌ Not Done | Sprint 10 | Add `showLastActive` privacy |
| `showHoroscope` | INTEGER | 1 | ❌ Not Done | Sprint 10 | Add `showHoroscope` privacy |
| `incomeDisclosure` | TEXT | "range" | ❌ Not Done | Sprint 10 | Add `incomeDisclosure` setting |
| `boostedUntil` | INTEGER | null | ❌ Not Done | Sprint 11 | Add `boostedUntil` timestamp |
| `isFakeProfile` | INTEGER | 0 | 🟡 Partial | Sprint 9 | `FakeProfileDetector.kt` |
| `trustScore` | REAL | 0.0 | 🟡 Partial | Sprint 9 | `FakeProfileDetector.kt` |
| `fcmToken` | TEXT | null | ✅ Done | Sprint 7 |  |
| `lastActiveAt` | INTEGER | — | ✅ Done | Sprint 8 |  |
| `createdAt` | INTEGER | — | ✅ Done | 1 |  |
| `subscriptionPlan` | TEXT | "FREE" | ✅ Done | Sprint 6 |  |
| `subscriptionExpiry` | INTEGER | null | 🟡 Partial | Sprint 10 | Add expiry field |
| `personalityType` | TEXT | null | ❌ Not Done | Sprint 13 | Add `personalityType` (16 types) |
| `matchScore` | REAL | null | ❌ Not Done | Sprint 11 | Computed Match Score™ |

**Total columns to add in Migration 16→17:** 30+ new columns

### 1.2 Migration 16→17 Task

**File:** `data/local/Migrations.kt`  
**Status:** ❌ Not Done  
**Sprint:** 10  

```kotlin
val MIGRATION_16_17 = Migration(16, 17) { db ->
    db.execSQL("ALTER TABLE users ADD COLUMN weight REAL")
    db.execSQL("ALTER TABLE users ADD COLUMN complexion TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN physicalStatus TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN motherTongue TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN subCaste TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN gothra TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN nakshatra TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN rashi TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN birthTime TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN fatherOccupation TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN motherOccupation TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN siblingsCount INTEGER")
    db.execSQL("ALTER TABLE users ADD COLUMN familyType TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN familyStatus TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN educationField TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN institution TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN graduationYear INTEGER")
    db.execSQL("ALTER TABLE users ADD COLUMN occupationCategory TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN employer TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN employerType TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN country TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN countryOfResidence TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN relocate TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN timezone TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN smoking TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN alcohol TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN hobbies TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN fitnessActivities TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN citizenship TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN isNRI INTEGER DEFAULT 0")
    db.execSQL("ALTER TABLE users ADD COLUMN matrimonyId TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN voiceBioUrl TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN profileCompleteness REAL DEFAULT 0.0")
    db.execSQL("ALTER TABLE users ADD COLUMN verificationLevel INTEGER DEFAULT 0")
    db.execSQL("ALTER TABLE users ADD COLUMN stealthMode INTEGER DEFAULT 0")
    db.execSQL("ALTER TABLE users ADD COLUMN showLastActive INTEGER DEFAULT 1")
    db.execSQL("ALTER TABLE users ADD COLUMN showHoroscope INTEGER DEFAULT 1")
    db.execSQL("ALTER TABLE users ADD COLUMN incomeDisclosure TEXT DEFAULT 'range'")
    db.execSQL("ALTER TABLE users ADD COLUMN boostedUntil INTEGER")
    db.execSQL("ALTER TABLE users ADD COLUMN subscriptionExpiry INTEGER")
    db.execSQL("ALTER TABLE users ADD COLUMN personalityType TEXT")
    db.execSQL("ALTER TABLE users ADD COLUMN matchScore REAL")
}
```

---

## SECTION 2 — COMPLETE FIRESTORE SCHEMA (Production v3)

| Collection | Document Structure | Status | Sprint | Task |
|-----------|-------------------|--------|--------|------|
| `users/{uid}` | All profile fields | 🟡 Partial | Sprint 10 | Sync all 50+ fields from UserEntity |
| `interests/{fromUid}_{toUid}` | status, isSuperLike, createdAt | ✅ Done | 2 |  |
| `matches/{sortedUids}` | matchedAt, lastMessage, chatId | ✅ Done | 2 |  |
| `shortlists/{uid}/saved/{targetUid}` | addedAt | ✅ Done | 3 |  |
| `blocks/{uid}/blocked/{targetUid}` | blockedAt, reason | ✅ Done | 5 |  |
| `chats/{threadId}/messages/{msgId}` | text, senderId, sentAt, readAt, reactions | 🟡 Partial | Sprint 11 | Add reactions, replyToId |
| `events/{eventId}` | title, date, registeredUsers | 🟡 Partial | Sprint 9 |  |
| `communities/{groupId}` | name, members, city, caste | 🟡 Partial | Sprint 9 |  |
| `referrals/{uid}` | code, usedBy, creditedAt | ✅ Done | Sprint 9 |  |
| `rmRequests/{uid}` | leadName, phone, plan, status | 🟡 Partial | Sprint 9 |  |
| `backgroundChecks/{uid}` | package, status, result | 🟡 Partial | Sprint 9 |  |
| `callRequests/{uid}` | targetUid, status, scheduledAt | 🟡 Partial | Sprint 9 |  |
| `profileAnalytics/{uid}` | viewCount, likeCount, matchCount | 🟡 Partial | Sprint 9 |  |
| `verifications/{uid}` | type, documentUrl, status, reviewer | ❌ Not Done | Sprint 12 | Create collection |
| `boosts/{uid}` | startedAt, endsAt, planType | ❌ Not Done | Sprint 11 | Create collection |
| `dailyMatches/{uid}` | profiles[], computedAt | ❌ Not Done | Sprint 11 | Cloud Function pre-compute |
| `savedFilters/{uid}/filters/{filterId}` | name, filterParams | ❌ Not Done | Sprint 11 | Create collection |
| `savedSearches/{uid}/searches/{searchId}` | name, query, alertEnabled | ❌ Not Done | Sprint 11 | Create collection |
| `notifications/{uid}/items/{notifId}` | type, title, body, readAt | ❌ Not Done | Sprint 11 | Create collection |
| `notificationPrefs/{uid}` | per-type toggles | ❌ Not Done | Sprint 11 | Create collection |
| `familyAccounts/{uid}` | memberUid, role, permissions | ❌ Not Done | Sprint 13 | Create collection |
| `horoscopes/{uid}` | chart JSON, gunascore, interpretation | ❌ Not Done | Sprint 11 | Create on Kundali compute |
| `callLogs/{uid}/calls/{callId}` | duration, type, partnerId, at | ❌ Not Done | Sprint 12 | Create on call completion |
| `auditLog/{eventId}` | actor, action, target, metadata, ts | ❌ Not Done | Sprint 12 | DPDP compliance |
| `otpLog/{hash}` | mobile, attempts, expiresAt, used | ❌ Not Done | Sprint 10 | OTP brute-force protection |
| `sessions/{uid}/devices/{deviceId}` | tokenHash, lastActive, revoked | ❌ Not Done | Sprint 10 | Server-side session management |

---

## SECTION 3 — COMPLETE FIRESTORE INDEXES (Production v3)

**File:** `MatchApp/firestore.indexes.json`

| Collection | Fields | Direction | Status | Sprint |
|-----------|--------|-----------|--------|--------|
| users | gender, ageBucket, lastActiveAt | ASC, ASC, DESC | ✅ Done | Sprint 9 |
| users | religion, caste, lastActiveAt | ASC, ASC, DESC | ❌ Not Done | Sprint 10 |
| users | gender, religion, lastActiveAt | ASC, ASC, DESC | ❌ Not Done | Sprint 10 |
| users | countryOfResidence, gender, lastActiveAt | ASC, ASC, DESC | ❌ Not Done | Sprint 10 |
| users | isNRI, gender, lastActiveAt | ASC, ASC, DESC | ❌ Not Done | Sprint 10 |
| users | verificationLevel, gender, lastActiveAt | ASC, ASC, DESC | ❌ Not Done | Sprint 10 |
| users | boostedUntil, gender, lastActiveAt | ASC, ASC, DESC | ❌ Not Done | Sprint 11 |
| interests | toUid, status, createdAt | ASC, ASC, DESC | ❌ Not Done | Sprint 11 |
| interests | fromUid, status, createdAt | ASC, ASC, DESC | ❌ Not Done | Sprint 11 |
| notifications | userId, readAt | ASC, ASC | ❌ Not Done | Sprint 11 |
| auditLog | actorId, timestamp | ASC, DESC | ❌ Not Done | Sprint 12 |

---

## SECTION 4 — COMPLETE FIRESTORE SECURITY RULES (Production v3)

**File:** `MatchApp/firestore.rules`

| Rule | Status | Sprint | Task |
|------|--------|--------|------|
| users: read own only + authenticated reads for matching | ✅ Done | Sprint 9 |  |
| users: write own only | ✅ Done | Sprint 9 |  |
| interests: write requires auth + rate limit | 🟡 Partial | Sprint 11 | Add rate limit check |
| chats: read/write only participants | ✅ Done | Sprint 9 |  |
| notifications: read own only, write by system | ❌ Not Done | Sprint 11 | Add notification rules |
| verifications: write by user, read by admin only | ❌ Not Done | Sprint 12 | Add verification rules |
| auditLog: write by system only (Cloud Function), no user writes | ❌ Not Done | Sprint 12 | Add audit rules |
| sessions: write by system only, read by own uid | ❌ Not Done | Sprint 10 | Add session rules |
| boosts: read own, write by payment Cloud Function only | ❌ Not Done | Sprint 11 | Add boost rules |

---

## SECTION 5 — COMPLETE CLOUD FUNCTIONS (Production v3)

| Function | Trigger | Status | Sprint | Task |
|---------|---------|--------|--------|------|
| `onUserCreate` | Firestore `users/{uid}` create | ❌ Not Done | Sprint 11 | Generate `matrimonyId`, init analytics doc |
| `onPhotoUpload` | Storage `photos/{uid}/*` | ❌ Not Done | Sprint 11 | NSFW check, pHash, watermark |
| `onInterestSend` | Firestore `interests/{id}` create | 🟡 Partial | Sprint 7 | Complete FCM trigger |
| `onInterestAccept` | Firestore `interests/{id}` update | 🟡 Partial | Sprint 7 | Complete match creation |
| `onNewMessage` | Firestore `chats/{}/messages/{}` create | 🟡 Partial | Sprint 7 | Complete FCM trigger |
| `computeDailyMatches` | Scheduled (daily 6 AM) | ❌ Not Done | Sprint 11 | Pre-compute match queue per user |
| `sendDailyMatchNotification` | Scheduled (daily 9 AM IST) | ❌ Not Done | Sprint 11 | FCM daily match push |
| `sendInactivityNudge` | Scheduled (daily) | ❌ Not Done | Sprint 12 | Check D+7, D+14, D+30 inactivity |
| `sendSubscriptionExpiry` | Scheduled (daily) | ❌ Not Done | Sprint 11 | Alert 7d, 3d, 1d before expiry |
| `verifyRazorpayPayment` | HTTPS callable | 🟡 Partial | Sprint 9 | Complete all tier logic |
| `deleteUserAccount` | HTTPS callable | ✅ Done | Sprint 9 |  |
| `computeKundali` | Cloud Task (async) | ❌ Not Done | Sprint 11 | Async ephemeris + LLM interpretation |
| `exportUserData` | HTTPS callable | ❌ Not Done | Sprint 14 | GDPR/DPDP data export |
| `interpretKundali` | HTTPS callable / Cloud Task | ❌ Not Done | Sprint 13 | Gemini API natural language |
| `moderatePhoto` | Storage trigger | ❌ Not Done | Sprint 11 | Vision API NSFW + pHash |
| `activateBoost` | Firestore write trigger | ❌ Not Done | Sprint 11 | Set `boostedUntil`, send FCM |
| `trackProfileView` | HTTPS callable | ❌ Not Done | Sprint 11 | Increment view count, check rate limit |
| `generateBiodata` | HTTPS callable | ❌ Not Done | Sprint 12 | PDF generation from profile data |
| `sendEmailNotification` | Firestore trigger | ❌ Not Done | Sprint 12 | Firebase Email Extension |

---

## SECTION 6 — COMPLETE API INTEGRATION LIST (Production v3)

| API | Purpose | Status | Sprint | Task |
|-----|---------|--------|--------|------|
| Firebase Auth | Authentication | ✅ Done | 1 |  |
| Firestore | Database | ✅ Done | 1 |  |
| Firebase Storage | Media | ✅ Done | 3 |  |
| FCM | Push notifications | ✅ Done | 7 |  |
| Firebase Crashlytics | Error tracking | ✅ Done | 7 |  |
| Firebase Analytics | User analytics | ✅ Done | 7 |  |
| Firebase Remote Config | Feature flags | ✅ Done | 9 |  |
| Firebase Performance | Performance monitoring | 🟡 Partial | Sprint 11 | Add custom traces |
| Razorpay | Payments | 🟡 Partial | Sprint 9 | Complete all tiers |
| Google Vision API | Photo NSFW + face detection | ❌ Not Done | Sprint 11 | Cloud Function |
| ML Kit FaceDetection | Client-side face validation | ❌ Not Done | Sprint 11 | `PhotoEditorScreen.kt` |
| Algolia | Full-text profile search | ❌ Not Done | Sprint 13 | Replace basic Firestore text search |
| Agora SDK | Voice + video calls | ❌ Not Done | Sprint 12 | `SecureCallScreen.kt`, `VirtualMeetScreen.kt` |
| Google Maps SDK | City-level location lookup | ❌ Not Done | Sprint 12 | Address autocomplete |
| UIDAI Aadhaar eKYC | Identity verification | ❌ Not Done | Sprint 12 | Sandbox API |
| DigiLocker API | Education/employment verification | ❌ Not Done | Sprint 13 | DigiLocker OAuth |
| Gemini API | Kundali interpretation + AI bio | ❌ Not Done | Sprint 13 | Cloud Function |
| AdMob (rewarded) | Rewarded ad → free unlock | ❌ Not Done | Sprint 12 | `RewardedAdManager.kt` |
| Firebase App Check | Bot/abuse prevention | ❌ Not Done | Sprint 11 | `app/build.gradle.kts` |

---

## SPRINT ALLOCATION SUMMARY

| Sprint | Focus | Key Tasks |
|--------|-------|---------|
| Sprint 10 | Complete schema | Migration 16→17 (30+ fields), all missing indexes, privacy columns, NRI fields |
| Sprint 11 | Cloud Functions | All 19 Cloud Functions stubbed, daily match compute, notification system, boost, App Check |
| Sprint 12 | Integrations | Agora voice/video, UIDAI eKYC, Vision API photo moderation, AdMob, audit log |
| Sprint 13 | AI features | Gemini Kundali, Algolia search, DigiLocker, personality quiz, TFLite recommendation |
| Sprint 14 | Family + export | Family portal, data export, match timeline, group call |
| Sprint 15 | Web + Admin | Firebase Hosting profile pages, admin panel, Play Store submission |

---

## DEFINITION OF DONE (10/10 Checklist)

- [ ] Migration 16→17: all 30+ columns added, no data loss
- [ ] All Firestore collections created with proper security rules
- [ ] All 8 missing composite indexes added to `firestore.indexes.json`
- [ ] All 19 Cloud Functions implemented and deployed
- [ ] Complete API integration list: Vision API, Agora, UIDAI, Algolia, Gemini
- [ ] `matrimonyId` auto-generated for every new user (TLG-XXXXX format)
- [ ] Daily match computation running at 6 AM IST
- [ ] Profile completeness score calculated and stored
- [ ] All Firestore security rules for all 25+ collections
- [ ] Firebase App Check enabled
- [ ] All subscription tiers (Free/Standard/Premium/Platinum) gated correctly
- [ ] Cloud Function `exportUserData` operational
