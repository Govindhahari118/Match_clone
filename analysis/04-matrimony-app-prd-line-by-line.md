# Matrimony_App_PRD.docx — Line-by-Line Analysis, Research, and Implementation

- Source: `docs/Matrimony_App_PRD.docx`
- Extracted requirement lines: 520
- Method: sequential extraction from DOCX text nodes with XML/artifact filtering and de-duplication.

## Phase A — Deep Line-by-Line Analysis

### Auth & Identity

| Line | Requirement text | Research note | Implementation mapping |
|---:|---|---|---|
| 143 | Sign up via mobile OTP, email, or Google/Apple SSO. Minimal fields: Name, DOB, Gender, Religion, Location. | Map requirement to module owner and define Definition of Done. | backend/src/routes/auth.routes.js + backend/src/controllers/auth.controller.js + frontend/src/app/(auth)/* |
| 173 | Mobile OTP | Map requirement to module owner and define Definition of Done. | backend/src/routes/auth.routes.js + backend/src/controllers/auth.controller.js + frontend/src/app/(auth)/* |
| 421 | OTP-based 2FA available for account access | Map requirement to module owner and define Definition of Done. | backend/src/routes/auth.routes.js + backend/src/controllers/auth.controller.js + frontend/src/app/(auth)/* |

### Onboarding & Profile

| Line | Requirement text | Research note | Implementation mapping |
|---:|---|---|---|
| 31 | Fake and unverified profiles reduce trust and waste time | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 43 | Profile Creation | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 44 | ✅ Full profile | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 58 | Profile Boost / Visibility | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 62 | Photo Privacy Control | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 83 | Photo Privacy (blur/lock) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 90 | 3-Level Profile Verification | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 91 | Privacy Controls (photos, last seen) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 95 | 10 Lakh+ Profiles | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 107 | Fake profiles despite "verification" | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 109 | Family/parent profile management | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 112 | Verified video intro on profiles | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 122 | Profile | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 125 | 50s, Hyderabad. Managing son's profile. Not tech-savvy | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 131 | Too many fake profiles, premium too expensive | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 133 | App is too complex, unsure if profiles are genuine | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 136 | Photo privacy, masked calling, block/report | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 139 | 4.1 Onboarding & Registration | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 145 | Profile Wizard | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 146 | Step-by-step guided profile builder: Personal → Education → Career → Family → Partner Preferences. Progress bar shown. | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 148 | Family / Parent Profile | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 149 | Option to register as 'creating profile for son/daughter/sibling' with consent confirmation checkbox. | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 151 | Photo Upload | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 152 | Minimum 1 photo required (mandatory). AI auto-check: no sunglasses, no group photos, face clearly visible. | Treat as mandatory acceptance criterion; add explicit test coverage. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 154 | Personality Quiz (Onboarding) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 156 | 4.2 Profile Management | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 164 | Partner Preferences: Mirror of profile fields with min/max ranges (age, height, income, education level) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 166 | Biodata PDF: Auto-generate printable biodata in traditional format with one click | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 167 | Profile Completeness Score: Gamified meter (0–100%) with nudges to complete missing fields | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 181 | Level 4 — Photo Verified | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 182 | Selfie liveness check matched against ID photo | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 183 | ✅ Photo Verified | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 187 | AI Fraud Detection: Automated scanning for duplicate photos (reverse image search), suspicious activity patterns, bot-like behavior, and Malaysia-style scam accounts (flagged based on research of user complaints). | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 193 | Partner Preference overlap: 20% | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 195 | Behavioral affinity (who viewed/liked similar profiles): 10% | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 213 | Verified profiles only toggle | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 215 | Profile photo mandatory toggle | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 235 | AI suggests 3 conversation starter options based on shared profile details | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 239 | Photo Album Privacy: Show photos to — Everyone / Connected members only / No one (blur) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 240 | Profile Visibility: Public / Members only / Hidden from search (stealth mode) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 243 | Incognito Browse: View profiles without appearing in their 'Who Viewed Me' list (Premium) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 245 | Anti-Screenshot Protection: Screenshots of profile photos disabled in app | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 246 | Profile Watermarking: All photos watermarked with platform name to deter misuse | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 254 | Parent/Family Access: Parents create a sub-account linked to the main profile | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 256 | Family Shortlist: Family can mark profiles as 'Family Approved' for user to review | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 260 | 'Someone liked your profile' — real-time notification | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 261 | 'Your profile was viewed X times today' — weekly digest | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 262 | Profile completeness nudges with specific tips | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 268 | Create Profile | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 269 | Browse Profiles | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 279 | Profile Boost (2x visibility) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 293 | Trust-First: Verification badges visible on every profile card; never hide trust signals | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 312 | Profile view analytics | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 313 | 👤 Profile | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 314 | My profile, Edit, Preview as match sees it | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 319 | Each match card shows: Photo, Name, Age, City, Profession, Compatibility %, Verification badges | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 320 | Action buttons: ❤️ Like / ✖ Skip / ⭐ Super Interest / 📋 View Full Profile | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 322 | Profile Detail Screen | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 323 | Full-width photo carousel (up to 10 photos) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 325 | Sections: About Me → Education & Career → Family Background → Partner Preferences → Horoscope | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 328 | Similar profiles suggestion at bottom | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 332 | In-chat photo sharing (with watermark protection) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 333 | Smart reply chip suggestions (AI-generated from profile context) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 362 | All section titles and profile names | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 368 | DO NOT show 'My Matches' tab that contains un-matched profiles — causes confusion (Shaadi.com complaint) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 369 | DO NOT allow profiles without photos — mandatory photo to complete profile | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 371 | DO NOT hide verification status — show clearly on every profile card and in search results | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 394 | Relational for profiles, fast reads | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 397 | Full-text + filter-based profile search | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 406 | Fast photo/video delivery | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 415 | All photos stored encrypted at rest in S3 | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 441 | Profile Photo CDN Cache | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 450 | Registration, Profile creation, Basic search & filters, Interest / Like system, Simple inbox, Phone & ID verification (2 levels), iOS + Android apps | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 453 | In-app messaging, SecureConnect voice calls, AI compatibility scoring (v1), Advanced search filters, Premium subscription (Gold tier), Profile boost, Horoscope matching | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 456 | Video calls, LiveMatch virtual events, Photo album privacy controls, Family portal, Incognito browse, Platinum tier, Biodata PDF export, Push notification engine | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 459 | Behavioral AI (v2 — learns from skips/likes), Smart reply AI, Fraud detection ML model, NRI time-zone aware features, Video introduction on profiles, Personality quiz integration | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 471 | Profile Completion Rate | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 479 | Verified Profile % | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 490 | Fake Profile Takedown Time | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |
| 497 | Fake profile proliferation (common in Jeevansathi reviews) | Map requirement to module owner and define Definition of Done. | backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/* |

### Search & Matching

| Line | Requirement text | Research note | Implementation mapping |
|---:|---|---|---|
| 12 | Based on Competitive Research: Shaadi.com · BharatMatrimony · Jeevansathi · Matrimony.com | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 29 | 1.3 Core Problem Statements (from User Reviews & Research) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 34 | Assisted matchmaking services are overpriced and under-deliver | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 35 | Spam messages and irrelevant matches frustrate users | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 36 | No meaningful AI intelligence — filters are just manual search | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 45 | Browse Matches | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 55 | Advanced Filters (24+ categories) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 59 | AI-Powered Matchmaking | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 66 | AI Matching (MIMA™ Algorithm) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 72 | Horoscope / Kundli Matching | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 74 | ✅ Full match report | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 75 | Interest-Based Matching (hobbies) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 88 | Online Matchmaking Events | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 93 | Caste/Community Search | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 105 | No real AI — just manual filters | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 114 | Time-zone aware scheduling, global search | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 115 | No personality/compatibility quiz | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 116 | AI compatibility scoring with test | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 127 | Find compatible match without wasting time | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 129 | Find reputable match from same community | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 135 | AI match score, video call, clean UI | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 137 | Simple UI, family portal, horoscope match | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 155 | Optional 5-question quiz on lifestyle, values, family expectations. Used to seed AI compatibility scoring. | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 163 | Horoscope: Star/Nakshatra, rashi, time of birth for kundli matching | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 165 | Video Introduction: 30-second self-recorded intro video visible to verified matches only | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 188 | 4.4 Matchmaking Engine (AI) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 189 | Compatibility Score (0–100%): Calculated across 8 dimensions: | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 190 | Community & Religion match weight: 25% | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 191 | Education & Career compatibility: 20% | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 194 | Horoscope match: 10% (optional, can be disabled) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 196 | Daily Matches: Send 5–10 curated matches every morning as push notifications | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 197 | MIMA-style Algorithm: Learn from user actions — skips, views, interests, ignores — and adapt recommendations | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 198 | Mutual Match Alert: Notify both users when they like each other simultaneously | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 199 | Recommended Because: Explain to user why a match is suggested (e.g. 'Both from Pune, same education level, overlapping hobbies') | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 200 | 4.5 Search & Filters | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 201 | Basic Filters (Free): | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 206 | Advanced Filters (Premium): | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 212 | NRI filter (by country of residence) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 216 | Save & name custom filter sets | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 230 | LiveMatch Events | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 231 | Group video speed-matchmaking sessions — meet 8–10 matches in 60 minutes | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 233 | View phone number + email of a match (requires their consent setting) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 236 | Message Request Filter | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 237 | Filter messages: Accepted matches only / All verified / All | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 249 | Kundli Match Report: Full 36-point Guna matching with detailed explanation per category | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 250 | Manglik Compatibility: Flag and explain Manglik status clearly | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 251 | Nadi Dosha alert with recommended remedies (informational only) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 252 | Toggle Off: Users can opt out of horoscope-based matching entirely | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 255 | Controlled Sharing: User chooses what family can see (matches list, messages, contact details) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 259 | Daily Curated Matches push notification (morning, configurable time) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 264 | Inactivity re-engagement: 'You haven't logged in — here are 3 new matches' | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 278 | Advanced Search Filters | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 282 | AI Compatibility Score | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 296 | Speed: Every key action (loading match list, opening chat) must complete in under 1.5 seconds | Treat as mandatory acceptance criterion; add explicit test coverage. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 302 | Daily match cards (swipe or list view) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 303 | Match detail, Compatibility report | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 304 | 🔍 Search | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 305 | Full filter-based search | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 306 | Advanced filters, Saved searches | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 321 | 'Why this match?' tooltip explaining AI reasoning | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 327 | Compatibility breakdown chart (radar/spider chart across 6 dimensions) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 367 | DO NOT gate messaging immediately after sign-up — allow 3 free messages per match before paywall | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 373 | 6.1 Recommended Tech Stack | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 395 | Search Engine | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 396 | Elasticsearch | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 400 | Compatibility scoring, fraud detection | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 429 | Match List Load | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 503 | Low female user ratio (skews match quality for male users) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 509 | Poor matchmaking quality hurts retention | Attach measurable target + dashboard source of truth. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 511 | Start with rule-based matching, layer AI after data accumulates (3+ months) | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 512 | 10. Appendix — Competitor URLs Researched | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |
| 518 | Apptunix Matrimonial App Market Research 2025 | Map requirement to module owner and define Definition of Done. | backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js |

### Interaction & Realtime

| Line | Requirement text | Research note | Implementation mapping |
|---:|---|---|---|
| 48 | Send Interest / Like | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 49 | Read & Send Messages | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 52 | Video Call (ShaadiMeet) | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 67 | SecureConnect® (masked calling) | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 85 | Voice & Video Call (Number Hidden) | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 87 | Chat & Hangouts | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 132 | Scared to share contact info, too many spam messages | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 162 | Lifestyle: Diet (veg/non-veg), smoking, drinking, hobbies & interests, pets | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 220 | Express Interest | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 221 | Send a 'Like' / Interest with a preset message | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 224 | Real-time chat with read receipts, typing indicator, emoji | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 226 | SecureConnect Voice Call | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 227 | VoIP call through app — neither party's phone number is revealed | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 228 | Video Call | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 229 | 1-on-1 HD video call within app (no external app needed) | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 258 | 4.10 Notifications & Engagement | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 272 | Send Interests | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 274 | Read Messages | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 275 | Send Messages | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 276 | Voice & Video Calls | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 307 | 💬 Messages | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 308 | Inbox: Interests received, Chats | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 309 | Chat window, Voice/Video call | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 311 | Who viewed me, Who liked me, Mutual interests | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 326 | Sticky bottom bar: Connect / Message / Voice Call / Video Call | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 329 | Chat Screen | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 331 | In-chat voice note recording | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 334 | Call button to initiate SecureConnect call without leaving chat | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 388 | Low-latency chat and presence | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 389 | Video/Voice Calls | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 391 | Proven WebRTC abstraction, masked calls | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 407 | Notifications | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 409 | Push notifications iOS + Android | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 414 | End-to-end encryption for all messages (Signal Protocol or AES-256) | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 416 | HTTPS/TLS 1.3 for all API calls | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 432 | Message Delivery Latency | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |
| 476 | Message Response Rate | Map requirement to module owner and define Definition of Done. | backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js |

### Subscription & Billing

| Line | Requirement text | Research note | Implementation mapping |
|---:|---|---|---|
| 42 | Premium Tier | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 64 | ✅ Eligible plans | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 225 | Premium | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 263 | Subscription expiry reminders (7 days, 3 days, 1 day before) | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 265 | 4.11 Premium Subscription Tiers | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 294 | Progressive Disclosure: Don't overwhelm — show free features fully, then reveal premium features contextually | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 315 | Verification center, Subscription, Settings | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 357 | Premium Badge | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 359 | Premium member indicators | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 410 | Payments | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 411 | Razorpay + Apple Pay / Google Pay | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 412 | India + international payments | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 474 | Premium Conversion Rate | Attach measurable target + dashboard source of truth. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |
| 505 | Free premium for women in first 6 months; women-first UI design | Map requirement to module owner and define Definition of Done. | backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js |

### Trust, Safety & Privacy

| Line | Requirement text | Research note | Implementation mapping |
|---:|---|---|---|
| 15 | Build India's most trusted, AI-powered matrimony platform that combines deep cultural understanding with modern technology to help users find a verified, compatible life partner — faster, safer, and with greater privacy than any existing solution. | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 84 | JEEVANSATHI — 25+ Years · Privacy-First · Nayatel Group | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 92 | Album Privacy (show to accepted only) | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 108 | Multi-layer verification + AI fraud detection | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 124 | MBA graduate, Mumbai. Career-focused. Values privacy | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 168 | 4.3 Verification & Trust System | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 169 | Verification Level | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 238 | 4.7 Privacy & Safety | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 242 | Block & Report: Block any user permanently; report with reason categories (fake/scam/harassment/obscene) | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 356 | Verification checkmarks | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 370 | DO NOT use aggressive upsell popups on every interaction — use contextual, non-blocking upgrade nudges | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |
| 487 | Success Stories Reported | Validate with production runbook and incident ownership before release. | backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js |

### Admin & Analytics

| Line | Requirement text | Research note | Implementation mapping |
|---:|---|---|---|
| 462 | Web platform launch, Advisor marketplace, Community-specific sub-brands, API partnerships, Analytics dashboard for admins, A/B testing framework | Map requirement to module owner and define Definition of Done. | backend/src/routes/admin.routes.js + backend/src/routes/analytics.routes.js + frontend/src/app/(main)/admin/page.js |
| 463 | 8. Success Metrics & KPIs | Map requirement to module owner and define Definition of Done. | backend/src/routes/admin.routes.js + backend/src/routes/analytics.routes.js + frontend/src/app/(main)/admin/page.js |
| 464 | KPI | Attach measurable target + dashboard source of truth. | backend/src/routes/admin.routes.js + backend/src/routes/analytics.routes.js + frontend/src/app/(main)/admin/page.js |

### Architecture & Ops

| Line | Requirement text | Research note | Implementation mapping |
|---:|---|---|---|
| 372 | 6. Technical Architecture | Validate with production runbook and incident ownership before release. | README_PRODUCTION.md + UPTIME_DASHBOARD_RUNBOOK.md + docker-compose.yml |
| 393 | PostgreSQL (primary) + Redis (cache) | Validate with production runbook and incident ownership before release. | README_PRODUCTION.md + UPTIME_DASHBOARD_RUNBOOK.md + docker-compose.yml |
| 401 | Cloud Infrastructure | Validate with production runbook and incident ownership before release. | README_PRODUCTION.md + UPTIME_DASHBOARD_RUNBOOK.md + docker-compose.yml |
| 499 | ML fraud detection + human review queue + scam pattern database | Validate with production runbook and incident ownership before release. | README_PRODUCTION.md + UPTIME_DASHBOARD_RUNBOOK.md + docker-compose.yml |

### General

| Line | Requirement text | Research note | Implementation mapping |
|---:|---|---|---|
| 1 | MATRIMONY APP | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 2 | Product Requirements Document | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 3 | Competitive Analysis & Full Feature Specification | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 4 | Version | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 5 | v1.0 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 6 | Status | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 7 | Draft | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 8 | Market | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 9 | India + NRI | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 10 | Date | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 11 | Mar 2026 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 13 | 1. Executive Summary | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 14 | 1.1 Product Vision | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 16 | 1.2 Market Opportunity | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 17 | Market Metric | Attach measurable target + dashboard source of truth. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 18 | 2024 Value | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 19 | 2033 Projection | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 20 | Global Matrimony Market | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 21 | USD 5.51 Billion | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 22 | USD 13.22 Billion | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 23 | CAGR Growth Rate | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 24 | 10.2% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 25 | Active Indian Users (Shaadi) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 26 | 35 Million+ | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 27 | Daily New Registrations (BharatMatrimony) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 28 | 1000+ / day | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 30 | Too many features locked behind expensive paywalls — users feel tricked after downloading | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 32 | UI is cluttered and not intuitive compared to modern dating apps | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 33 | No way to communicate without revealing personal phone number | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 37 | 2. Competitive Analysis | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 38 | 2.1 Platform-by-Platform Feature Comparison | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 39 | SHAADI.COM — Pioneer Since 1996 · 35M+ Members · 8M+ Success Stories | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 40 | Feature | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 41 | Free Tier | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 46 | ✅ Limited | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 47 | ✅ Unlimited | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 50 | View Contact Details | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 51 | ✅ Phone + Email | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 53 | ShaadiLive Events | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 54 | ✅ Meet 10 in 1 hr | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 56 | Basic only | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 57 | ✅ Full | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 60 | Basic | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 61 | ✅ Advanced | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 63 | 30-Day Money Back Guarantee | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 65 | BHARATMATRIMONY — No.1 in India · Since 2000 · Community-First | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 68 | ✅ No number reveal | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 69 | BharatMatrimony Prime (Verified) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 70 | ✅ Govt. ID verified | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 71 | Featured Listing | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 73 | View only | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 76 | ✅ Enhanced | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 77 | Community-Specific Sub-Apps | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 78 | ✅ 50+ communities | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 79 | Monthly Advisor Consultation | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 80 | ✅ 1-on-1 session | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 81 | Multi-language App Support | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 82 | ✅ Hindi + Regional | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 86 | ✅ UNIQUE Feature | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 89 | ✅ Hangouts feature | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 94 | ✅ Priority | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 96 | Browse | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 97 | ✅ Full access | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 98 | 2.2 Feature Gap Analysis — What's Missing Across ALL Platforms | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 99 | Current Market Gap | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 100 | Our Opportunity | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 101 | Poor UI compared to Tinder/Hinge | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 102 | Modern swipe + list hybrid UI | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 103 | Aggressive paywalls after sign-up | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 104 | Transparent freemium with real free value | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 106 | Behavioral AI that learns preferences | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 110 | Family portal with controlled access | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 111 | No video/audio before messaging | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 113 | Poor NRI/global experience | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 117 | 3. User Personas | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 118 | Persona | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 119 | Arjun, 28 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 120 | Priya, 26 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 121 | Sanjay & Meera (Parents) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 123 | IT Engineer, Bangalore. Busy, wants efficient modern UX | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 126 | Goal | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 128 | Find educated partner who respects independence | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 130 | Pain Point | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 134 | Must-Have Features | Treat as mandatory acceptance criterion; add explicit test coverage. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 138 | 4. Feature Specifications | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 140 | Description | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 141 | F-01 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 142 | Quick Registration | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 144 | F-02 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 147 | F-03 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 150 | F-04 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 153 | F-05 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 157 | Personal Details: Full name, DOB, height, weight, complexion, mother tongue, marital status (Single / Divorced / Widowed) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 158 | Education: Degree, field, institute | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 159 | Career: Profession, employer, annual income range | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 160 | Family: Father's occupation, mother's occupation, number of siblings, family type (nuclear/joint), family values (traditional/moderate/liberal) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 161 | Religious: Religion, caste, sub-caste (optional), gotra, manglik status | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 170 | Method | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 171 | Badge Awarded | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 172 | Level 1 — Phone Verified | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 174 | 📱 Phone Verified | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 175 | Level 2 — ID Verified | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 176 | Aadhaar, PAN, Passport, or Driving Licence upload | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 177 | 🛡️ ID Verified | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 178 | Level 3 — Employment Verified | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 179 | LinkedIn link or salary slip upload | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 180 | 💼 Employment Verified | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 184 | Level 5 — PRIME Verified | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 185 | All 4 above + manual review by team | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 186 | ⭐ PRIME Member | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 192 | Location & Lifestyle: 15% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 202 | Age range, height range | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 203 | Religion, caste, mother tongue | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 204 | City / State / Country | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 205 | Marital status, education level | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 207 | Annual income range | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 208 | Profession / employer type | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 209 | Family type, family values | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 210 | Complexion, diet preference | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 211 | Manglik / non-Manglik | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 214 | Online in last 7 / 30 days | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 217 | 4.6 Communication Features | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 218 | How It Works | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 219 | Tier | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 222 | Free | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 223 | In-App Messenger | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 232 | Contact Unlock | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 234 | Smart Reply Suggestions | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 241 | Last Seen & Online Status: Show or hide from non-connected users | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 244 | Contact Detail Control: Choose who can unlock your contact info | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 247 | 4.8 Horoscope & Kundli Features | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 248 | Auto Kundli Generation: Generate birth chart from DOB, time, and place | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 253 | 4.9 Family Portal | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 257 | Co-Browse Mode: User and parent browse together in a shared session | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 266 | Gold | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 267 | Platinum | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 270 | Limited 20/day | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 271 | Unlimited | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 273 | 5/day | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 277 | 20 unlocks/month | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 280 | 1/month | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 281 | 4/month | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 283 | Incognito Browse | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 284 | 2/month | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 285 | Dedicated Relationship Advisor | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 286 | 1 session/month | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 287 | Suggested Price | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 288 | ₹1,499/mo | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 289 | ₹2,999/mo | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 290 | 5. UI/UX Design Specifications | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 291 | 5.1 Design Principles | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 292 | Emotionally Warm: Use warm tones (reds, creams, golds) that feel culturally familiar and celebratory | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 295 | Family-Friendly: UI must be usable by users aged 22–65, including non-tech-savvy parents | Treat as mandatory acceptance criterion; add explicit test coverage. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 297 | 5.2 App Navigation Structure | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 298 | Tab / Section | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 299 | Primary Content | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 300 | Sub-Screens | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 301 | 🏠 Home / Discover | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 310 | ❤️ Activity | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 316 | 5.3 Key Screens | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 317 | Home / Discover Screen | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 318 | Toggle between Card View (swipe-style) and List View (BharatMatrimony-style grid) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 324 | Video intro player (30 sec, prominent placement) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 330 | WhatsApp-inspired bubble layout | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 335 | 5.4 Color & Typography System | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 336 | Element | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 337 | Color | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 338 | Usage | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 339 | Primary Brand | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 340 | #C0392B (Deep Red) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 341 | CTA buttons, key icons, headings | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 342 | Secondary | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 343 | #E74C3C (Coral Red) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 344 | Hover states, secondary actions | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 345 | Background | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 346 | #FFF8F8 (Warm White) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 347 | App background, cards | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 348 | Text Primary | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 349 | #1A1A2E (Dark Navy) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 350 | Headings, primary text | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 351 | Text Secondary | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 352 | #7F8C8D (Gray) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 353 | Subtitles, metadata | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 354 | Verified Badge | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 355 | #27AE60 (Green) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 358 | #F39C12 (Gold) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 360 | Font — Headings | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 361 | Poppins Bold | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 363 | Font — Body | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 364 | Inter Regular | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 365 | All body copy, metadata | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 366 | 5.5 UX Pain Points from Competitors to Avoid | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 374 | Layer | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 375 | Technology | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 376 | Reason | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 377 | Mobile Frontend | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 378 | Flutter (iOS + Android) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 379 | Single codebase, native performance, fast UI | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 380 | Web Frontend | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 381 | React.js + Next.js | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 382 | SEO-friendly, server-side rendering | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 383 | Backend API | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 384 | Node.js + Express / Go | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 385 | High concurrency for real-time features | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 386 | Real-Time Messaging | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 387 | WebSockets + Redis Pub/Sub | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 390 | Agora.io or Daily.co SDK | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 392 | Database | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 398 | AI/ML | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 399 | Python (FastAPI) + TensorFlow / PyTorch | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 402 | AWS (India region ap-south-1) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 403 | Low latency for Indian users, PDPA compliance | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 404 | Storage | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 405 | AWS S3 + CloudFront CDN | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 408 | Firebase Cloud Messaging (FCM) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 413 | 6.2 Security & Compliance | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 417 | PDPA / IT Act 2000 compliance for Indian user data | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 418 | GDPR compliance for EU/UK NRI users | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 419 | Rate limiting on all APIs to prevent scraping | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 420 | Automatic PII redaction in logs | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 422 | 6.3 Non-Functional Requirements | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 423 | Metric | Attach measurable target + dashboard source of truth. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 424 | Target | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 425 | How Measured | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 426 | App Load Time (cold start) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 427 | < 2.5 seconds | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 428 | Firebase Performance | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 430 | < 1.5 seconds | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 431 | API response time | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 433 | < 300ms | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 434 | WebSocket ping | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 435 | API Uptime | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 436 | 99.9% SLA | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 437 | AWS CloudWatch | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 438 | Concurrent Users (launch) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 439 | 50,000+ | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 440 | Load testing | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 442 | < 200ms globally | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 443 | CloudFront metrics | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 444 | 7. Launch Roadmap | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 445 | Phase | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 446 | Timeline | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 447 | Deliverables | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 448 | Phase 1 — MVP | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 449 | Months 1–3 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 451 | Phase 2 — Core | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 452 | Months 4–5 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 454 | Phase 3 — Growth | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 455 | Months 6–7 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 457 | Phase 4 — AI+ | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 458 | Months 8–10 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 460 | Phase 5 — Scale | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 461 | Months 11–12 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 465 | Month 3 Target | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 466 | Month 12 Target | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 467 | Priority | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 468 | Daily Active Users (DAU) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 469 | 5,000 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 470 | 100,000 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 472 | >60% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 473 | >80% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 475 | 8–12% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 477 | >25% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 478 | >40% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 480 | >50% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 481 | >70% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 482 | Day-7 Retention | Attach measurable target + dashboard source of truth. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 483 | >55% | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 484 | App Store Rating | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 485 | >4.0 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 486 | >4.4 | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 488 | 50+ | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 489 | 1,000+ | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 491 | < 24 hours | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 492 | < 4 hours | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 493 | 9. Risks & Mitigations | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 494 | Risk | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 495 | Impact | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 496 | Mitigation | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 498 | High — destroys user trust | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 500 | Paywall frustration causing churn (Shaadi.com major complaint) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 501 | High — early drop-off | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 502 | Free tier must deliver genuine value; delay paywalls to Day 3+ | Treat as mandatory acceptance criterion; add explicit test coverage. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 504 | Medium — lowers platform quality | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 506 | Data breach of sensitive personal data | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 507 | Critical — regulatory and reputational | Treat as mandatory acceptance criterion; add explicit test coverage. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 508 | E2E encryption, regular pen testing, PDPA compliance, bug bounty | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 510 | High — users leave if AI is bad | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 513 | The following platforms were analyzed for this PRD: | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 514 | Shaadi.com — https://www.shaadi.com | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 515 | BharatMatrimony — https://www.bharatmatrimony.com | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 516 | Jeevansathi.com — https://www.jeevansathi.com | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 517 | Matrimony.com Group (Tamil, Marathi, Punjabi, Telugu sub-brands) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 519 | App Store & Google Play user reviews analysis (2024–2025) | Map requirement to module owner and define Definition of Done. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |
| 520 | This PRD is version 1.0 and should be treated as a living document. Update with user feedback from beta testing before finalizing. | Treat as mandatory acceptance criterion; add explicit test coverage. | PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure |

## Phase B — Research Synthesis

- **Auth & Identity**: 3 extracted lines mapped to implementation owners.
- **Onboarding & Profile**: 80 extracted lines mapped to implementation owners.
- **Search & Matching**: 72 extracted lines mapped to implementation owners.
- **Interaction & Realtime**: 37 extracted lines mapped to implementation owners.
- **Subscription & Billing**: 14 extracted lines mapped to implementation owners.
- **Trust, Safety & Privacy**: 12 extracted lines mapped to implementation owners.
- **Admin & Analytics**: 3 extracted lines mapped to implementation owners.
- **Architecture & Ops**: 4 extracted lines mapped to implementation owners.
- **General**: 295 extracted lines mapped to implementation owners.
- Highest signal areas are prioritized for implementation sequencing in Phase C.

## Phase C — Implementation Sequence (in order)

1. **Auth & Identity** — implement requirements against `backend/src/routes/auth.routes.js + backend/src/controllers/auth.controller.js + frontend/src/app/(auth)/*` and close via tests/checklist.
2. **Onboarding & Profile** — implement requirements against `backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/*` and close via tests/checklist.
3. **Search & Matching** — implement requirements against `backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js` and close via tests/checklist.
4. **Interaction & Realtime** — implement requirements against `backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js` and close via tests/checklist.
5. **Subscription & Billing** — implement requirements against `backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js` and close via tests/checklist.
6. **Trust, Safety & Privacy** — implement requirements against `backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js` and close via tests/checklist.
7. **Admin & Analytics** — implement requirements against `backend/src/routes/admin.routes.js + backend/src/routes/analytics.routes.js + frontend/src/app/(main)/admin/page.js` and close via tests/checklist.
8. **Architecture & Ops** — implement requirements against `README_PRODUCTION.md + UPTIME_DASHBOARD_RUNBOOK.md + docker-compose.yml` and close via tests/checklist.