# Multi-Doc Line-by-Line Analysis + Implementation Audit

Documents covered:
- `Matrimony_App_PRD.docx`
- `VivahApp_GapAnalysis_100_100.docx`
- `VivahApp_Ultimate_Blueprint.docx`
- `Matrimony_App_Complete_Documentation.docx`

## Implemented Features (from current codebase)

| Feature | Status | Evidence targets present | Requirement lines matched | Code-pattern hits |
|---|---|---:|---:|---:|
| OTP authentication | **implemented** | 2/2 | 50 | 3 |
| Onboarding/profile wizard | **implemented** | 2/2 | 21 | 4 |
| Search and filter matching | **implemented** | 2/2 | 182 | 3 |
| Interests/shortlist flow | **implemented** | 2/2 | 70 | 3 |
| Realtime chat | **implemented** | 3/3 | 138 | 3 |
| Voice/video calls | **implemented** | 2/2 | 36 | 3 |
| Verification/safety/reporting | **implemented** | 2/2 | 178 | 5 |
| Subscriptions and payments | **implemented** | 2/2 | 138 | 4 |
| Notifications | **implemented** | 2/2 | 51 | 2 |
| Admin and analytics | **implemented** | 3/3 | 85 | 4 |

### Implemented feature list

- OTP authentication
- Onboarding/profile wizard
- Search and filter matching
- Interests/shortlist flow
- Realtime chat
- Voice/video calls
- Verification/safety/reporting
- Subscriptions and payments
- Notifications
- Admin and analytics

### Partially implemented / follow-up


### Missing


## Line-by-line extraction and mapping

### Matrimony_App_PRD.docx

| # | Extracted line | Classified feature | Current status |
|---:|---|---|---|
| 1 | MATRIMONY APP | General / strategy / context | n/a |
| 2 | Product Requirements Document | General / strategy / context | n/a |
| 3 | Competitive Analysis & Full Feature Specification | General / strategy / context | n/a |
| 4 | Version | General / strategy / context | n/a |
| 5 | v1.0 | General / strategy / context | n/a |
| 6 | Status | General / strategy / context | n/a |
| 7 | Draft | General / strategy / context | n/a |
| 8 | Market | General / strategy / context | n/a |
| 9 | India + NRI | General / strategy / context | n/a |
| 10 | Date | General / strategy / context | n/a |
| 11 | Mar 2026 | General / strategy / context | n/a |
| 12 | Based on Competitive Research: Shaadi.com · BharatMatrimony · Jeevansathi · Matrimony.com | Search and filter matching | implemented |
| 13 | 1. Executive Summary | General / strategy / context | n/a |
| 14 | 1.1 Product Vision | General / strategy / context | n/a |
| 15 | Build India's most trusted, AI-powered matrimony platform that combines deep cultural understanding with modern technology to help users find a verified, compatible life partner — faster, safer, and with greater privacy than any existing solution. | Verification/safety/reporting | implemented |
| 16 | 1.2 Market Opportunity | General / strategy / context | n/a |
| 17 | Market Metric | General / strategy / context | n/a |
| 18 | 2024 Value | General / strategy / context | n/a |
| 19 | 2033 Projection | General / strategy / context | n/a |
| 20 | Global Matrimony Market | General / strategy / context | n/a |
| 21 | USD 5.51 Billion | General / strategy / context | n/a |
| 22 | USD 13.22 Billion | General / strategy / context | n/a |
| 23 | CAGR Growth Rate | General / strategy / context | n/a |
| 24 | Active Indian Users (Shaadi) | General / strategy / context | n/a |
| 25 | 35 Million+ | General / strategy / context | n/a |
| 26 | Daily New Registrations (BharatMatrimony) | General / strategy / context | n/a |
| 27 | 1000+ / day | General / strategy / context | n/a |
| 28 | 1.3 Core Problem Statements (from User Reviews & Research) | Search and filter matching | implemented |
| 29 | Too many features locked behind expensive paywalls — users feel tricked after downloading | General / strategy / context | n/a |
| 30 | Fake and unverified profiles reduce trust and waste time | General / strategy / context | n/a |
| 31 | UI is cluttered and not intuitive compared to modern dating apps | General / strategy / context | n/a |
| 32 | No way to communicate without revealing personal phone number | General / strategy / context | n/a |
| 33 | Assisted matchmaking services are overpriced and under-deliver | Search and filter matching | implemented |
| 34 | Spam messages and irrelevant matches frustrate users | Realtime chat | implemented |
| 35 | No meaningful AI intelligence — filters are just manual search | Search and filter matching | implemented |
| 36 | 2. Competitive Analysis | General / strategy / context | n/a |
| 37 | 2.1 Platform-by-Platform Feature Comparison | General / strategy / context | n/a |
| 38 | SHAADI.COM — Pioneer Since 1996 · 35M+ Members · 8M+ Success Stories | General / strategy / context | n/a |
| 39 | Feature | General / strategy / context | n/a |
| 40 | Free Tier | General / strategy / context | n/a |
| 41 | Premium Tier | Subscriptions and payments | implemented |
| 42 | Profile Creation | Onboarding/profile wizard | implemented |
| 43 | ✅ Full profile | General / strategy / context | n/a |
| 44 | Browse Matches | General / strategy / context | n/a |
| 45 | ✅ Limited | General / strategy / context | n/a |
| 46 | ✅ Unlimited | General / strategy / context | n/a |
| 47 | Send Interest / Like | Interests/shortlist flow | implemented |
| 48 | Read & Send Messages | Realtime chat | implemented |
| 49 | View Contact Details | General / strategy / context | n/a |
| 50 | ✅ Phone + Email | General / strategy / context | n/a |
| 51 | Video Call (ShaadiMeet) | Voice/video calls | implemented |
| 52 | ShaadiLive Events | General / strategy / context | n/a |
| 53 | ✅ Meet 10 in 1 hr | General / strategy / context | n/a |
| 54 | Advanced Filters (24+ categories) | Search and filter matching | implemented |
| 55 | Basic only | General / strategy / context | n/a |
| 56 | ✅ Full | General / strategy / context | n/a |
| 57 | Profile Boost / Visibility | General / strategy / context | n/a |
| 58 | AI-Powered Matchmaking | Search and filter matching | implemented |
| 59 | Basic | General / strategy / context | n/a |
| 60 | ✅ Advanced | General / strategy / context | n/a |
| 61 | Photo Privacy Control | Verification/safety/reporting | implemented |
| 62 | 30-Day Money Back Guarantee | General / strategy / context | n/a |
| 63 | ✅ Eligible plans | Subscriptions and payments | implemented |
| 64 | BHARATMATRIMONY — No.1 in India · Since 2000 · Community-First | General / strategy / context | n/a |
| 65 | AI Matching (MIMA™ Algorithm) | General / strategy / context | n/a |
| 66 | SecureConnect® (masked calling) | Voice/video calls | implemented |
| 67 | ✅ No number reveal | General / strategy / context | n/a |
| 68 | BharatMatrimony Prime (Verified) | General / strategy / context | n/a |
| 69 | ✅ Govt. ID verified | General / strategy / context | n/a |
| 70 | Featured Listing | General / strategy / context | n/a |
| 71 | Horoscope / Kundli Matching | General / strategy / context | n/a |
| 72 | View only | General / strategy / context | n/a |
| 73 | ✅ Full match report | Verification/safety/reporting | implemented |
| 74 | Interest-Based Matching (hobbies) | Interests/shortlist flow | implemented |
| 75 | ✅ Enhanced | General / strategy / context | n/a |
| 76 | Community-Specific Sub-Apps | General / strategy / context | n/a |
| 77 | ✅ 50+ communities | General / strategy / context | n/a |
| 78 | Monthly Advisor Consultation | General / strategy / context | n/a |
| 79 | ✅ 1-on-1 session | General / strategy / context | n/a |
| 80 | Multi-language App Support | General / strategy / context | n/a |
| 81 | ✅ Hindi + Regional | General / strategy / context | n/a |
| 82 | Photo Privacy (blur/lock) | Verification/safety/reporting | implemented |
| 83 | JEEVANSATHI — 25+ Years · Privacy-First · Nayatel Group | Verification/safety/reporting | implemented |
| 84 | Voice & Video Call (Number Hidden) | Voice/video calls | implemented |
| 85 | ✅ UNIQUE Feature | General / strategy / context | n/a |
| 86 | Chat & Hangouts | Realtime chat | implemented |
| 87 | Online Matchmaking Events | Search and filter matching | implemented |
| 88 | ✅ Hangouts feature | General / strategy / context | n/a |
| 89 | 3-Level Profile Verification | Verification/safety/reporting | implemented |
| 90 | Privacy Controls (photos, last seen) | Verification/safety/reporting | implemented |
| 91 | Album Privacy (show to accepted only) | Verification/safety/reporting | implemented |
| 92 | Caste/Community Search | Search and filter matching | implemented |
| 93 | ✅ Priority | General / strategy / context | n/a |
| 94 | 10 Lakh+ Profiles | General / strategy / context | n/a |
| 95 | Browse | General / strategy / context | n/a |
| 96 | ✅ Full access | General / strategy / context | n/a |
| 97 | 2.2 Feature Gap Analysis — What's Missing Across ALL Platforms | General / strategy / context | n/a |
| 98 | Current Market Gap | General / strategy / context | n/a |
| 99 | Our Opportunity | General / strategy / context | n/a |
| 100 | Poor UI compared to Tinder/Hinge | General / strategy / context | n/a |
| 101 | Modern swipe + list hybrid UI | General / strategy / context | n/a |
| 102 | Aggressive paywalls after sign-up | General / strategy / context | n/a |
| 103 | Transparent freemium with real free value | General / strategy / context | n/a |
| 104 | No real AI — just manual filters | Search and filter matching | implemented |
| 105 | Behavioral AI that learns preferences | General / strategy / context | n/a |
| 106 | Fake profiles despite "verification" | Verification/safety/reporting | implemented |
| 107 | Multi-layer verification + AI fraud detection | Verification/safety/reporting | implemented |
| 108 | Family/parent profile management | General / strategy / context | n/a |
| 109 | Family portal with controlled access | General / strategy / context | n/a |
| 110 | No video/audio before messaging | General / strategy / context | n/a |
| 111 | Verified video intro on profiles | General / strategy / context | n/a |
| 112 | Poor NRI/global experience | General / strategy / context | n/a |
| 113 | Time-zone aware scheduling, global search | Search and filter matching | implemented |
| 114 | No personality/compatibility quiz | Search and filter matching | implemented |
| 115 | AI compatibility scoring with test | Search and filter matching | implemented |
| 116 | 3. User Personas | General / strategy / context | n/a |
| 117 | Persona | General / strategy / context | n/a |
| 118 | Arjun, 28 | General / strategy / context | n/a |
| 119 | Priya, 26 | General / strategy / context | n/a |
| 120 | Sanjay & Meera (Parents) | General / strategy / context | n/a |
| 121 | Profile | General / strategy / context | n/a |
| 122 | IT Engineer, Bangalore. Busy, wants efficient modern UX | General / strategy / context | n/a |
| 123 | MBA graduate, Mumbai. Career-focused. Values privacy | Verification/safety/reporting | implemented |
| 124 | 50s, Hyderabad. Managing son's profile. Not tech-savvy | General / strategy / context | n/a |
| 125 | Goal | General / strategy / context | n/a |
| 126 | Find compatible match without wasting time | General / strategy / context | n/a |
| 127 | Find educated partner who respects independence | General / strategy / context | n/a |
| 128 | Find reputable match from same community | General / strategy / context | n/a |
| 129 | Pain Point | General / strategy / context | n/a |
| 130 | Too many fake profiles, premium too expensive | Subscriptions and payments | implemented |
| 131 | Scared to share contact info, too many spam messages | Realtime chat | implemented |
| 132 | App is too complex, unsure if profiles are genuine | General / strategy / context | n/a |
| 133 | Must-Have Features | General / strategy / context | n/a |
| 134 | AI match score, video call, clean UI | Voice/video calls | implemented |
| 135 | Photo privacy, masked calling, block/report | Voice/video calls | implemented |
| 136 | Simple UI, family portal, horoscope match | General / strategy / context | n/a |
| 137 | 4. Feature Specifications | General / strategy / context | n/a |
| 138 | 4.1 Onboarding & Registration | Onboarding/profile wizard | implemented |
| 139 | Description | General / strategy / context | n/a |
| 140 | F-01 | General / strategy / context | n/a |
| 141 | Quick Registration | General / strategy / context | n/a |
| 142 | Sign up via mobile OTP, email, or Google/Apple SSO. Minimal fields: Name, DOB, Gender, Religion, Location. | OTP authentication | implemented |
| 143 | F-02 | General / strategy / context | n/a |
| 144 | Profile Wizard | Onboarding/profile wizard | implemented |
| 145 | Step-by-step guided profile builder: Personal → Education → Career → Family → Partner Preferences. Progress bar shown. | Onboarding/profile wizard | implemented |
| 146 | F-03 | General / strategy / context | n/a |
| 147 | Family / Parent Profile | General / strategy / context | n/a |
| 148 | Option to register as 'creating profile for son/daughter/sibling' with consent confirmation checkbox. | General / strategy / context | n/a |
| 149 | F-04 | General / strategy / context | n/a |
| 150 | Photo Upload | General / strategy / context | n/a |
| 151 | Minimum 1 photo required (mandatory). AI auto-check: no sunglasses, no group photos, face clearly visible. | General / strategy / context | n/a |
| 152 | F-05 | General / strategy / context | n/a |
| 153 | Personality Quiz (Onboarding) | Onboarding/profile wizard | implemented |
| 154 | Optional 5-question quiz on lifestyle, values, family expectations. Used to seed AI compatibility scoring. | Search and filter matching | implemented |
| 155 | 4.2 Profile Management | General / strategy / context | n/a |
| 156 | Personal Details: Full name, DOB, height, weight, complexion, mother tongue, marital status (Single / Divorced / Widowed) | General / strategy / context | n/a |
| 157 | Education: Degree, field, institute | General / strategy / context | n/a |
| 158 | Career: Profession, employer, annual income range | General / strategy / context | n/a |
| 159 | Family: Father's occupation, mother's occupation, number of siblings, family type (nuclear/joint), family values (traditional/moderate/liberal) | General / strategy / context | n/a |
| 160 | Religious: Religion, caste, sub-caste (optional), gotra, manglik status | General / strategy / context | n/a |
| 161 | Lifestyle: Diet (veg/non-veg), smoking, drinking, hobbies & interests, pets | Interests/shortlist flow | implemented |
| 162 | Horoscope: Star/Nakshatra, rashi, time of birth for kundli matching | General / strategy / context | n/a |
| 163 | Partner Preferences: Mirror of profile fields with min/max ranges (age, height, income, education level) | Onboarding/profile wizard | implemented |
| 164 | Video Introduction: 30-second self-recorded intro video visible to verified matches only | General / strategy / context | n/a |
| 165 | Biodata PDF: Auto-generate printable biodata in traditional format with one click | General / strategy / context | n/a |
| 166 | Profile Completeness Score: Gamified meter (0–100%) with nudges to complete missing fields | General / strategy / context | n/a |
| 167 | 4.3 Verification & Trust System | Verification/safety/reporting | implemented |
| 168 | Verification Level | Verification/safety/reporting | implemented |
| 169 | Method | General / strategy / context | n/a |
| 170 | Badge Awarded | General / strategy / context | n/a |
| 171 | Level 1 — Phone Verified | General / strategy / context | n/a |
| 172 | Mobile OTP | OTP authentication | implemented |
| 173 | 📱 Phone Verified | General / strategy / context | n/a |
| 174 | Level 2 — ID Verified | General / strategy / context | n/a |
| 175 | Aadhaar, PAN, Passport, or Driving Licence upload | General / strategy / context | n/a |
| 176 | 🛡️ ID Verified | General / strategy / context | n/a |
| 177 | Level 3 — Employment Verified | General / strategy / context | n/a |
| 178 | LinkedIn link or salary slip upload | General / strategy / context | n/a |
| 179 | 💼 Employment Verified | General / strategy / context | n/a |
| 180 | Level 4 — Photo Verified | General / strategy / context | n/a |
| 181 | Selfie liveness check matched against ID photo | General / strategy / context | n/a |
| 182 | ✅ Photo Verified | General / strategy / context | n/a |
| 183 | Level 5 — PRIME Verified | General / strategy / context | n/a |
| 184 | All 4 above + manual review by team | General / strategy / context | n/a |
| 185 | ⭐ PRIME Member | General / strategy / context | n/a |
| 186 | AI Fraud Detection: Automated scanning for duplicate photos (reverse image search), suspicious activity patterns, bot-like behavior, and Malaysia-style scam accounts (flagged based on research of user complaints). | Search and filter matching | implemented |
| 187 | 4.4 Matchmaking Engine (AI) | Search and filter matching | implemented |
| 188 | Compatibility Score (0–100%): Calculated across 8 dimensions: | Search and filter matching | implemented |
| 189 | Community & Religion match weight: 25% | General / strategy / context | n/a |
| 190 | Education & Career compatibility: 20% | Search and filter matching | implemented |
| 191 | Location & Lifestyle: 15% | General / strategy / context | n/a |
| 192 | Partner Preference overlap: 20% | General / strategy / context | n/a |
| 193 | Horoscope match: 10% (optional, can be disabled) | General / strategy / context | n/a |
| 194 | Behavioral affinity (who viewed/liked similar profiles): 10% | General / strategy / context | n/a |
| 195 | Daily Matches: Send 5–10 curated matches every morning as push notifications | Notifications | implemented |
| 196 | MIMA-style Algorithm: Learn from user actions — skips, views, interests, ignores — and adapt recommendations | Interests/shortlist flow | implemented |
| 197 | Mutual Match Alert: Notify both users when they like each other simultaneously | Interests/shortlist flow | implemented |
| 198 | Recommended Because: Explain to user why a match is suggested (e.g. 'Both from Pune, same education level, overlapping hobbies') | General / strategy / context | n/a |
| 199 | 4.5 Search & Filters | Search and filter matching | implemented |
| 200 | Basic Filters (Free): | Search and filter matching | implemented |
| 201 | Age range, height range | General / strategy / context | n/a |
| 202 | Religion, caste, mother tongue | General / strategy / context | n/a |
| 203 | City / State / Country | General / strategy / context | n/a |
| 204 | Marital status, education level | General / strategy / context | n/a |
| 205 | Advanced Filters (Premium): | Search and filter matching | implemented |
| 206 | Annual income range | General / strategy / context | n/a |
| 207 | Profession / employer type | General / strategy / context | n/a |
| 208 | Family type, family values | General / strategy / context | n/a |
| 209 | Complexion, diet preference | General / strategy / context | n/a |
| 210 | Manglik / non-Manglik | General / strategy / context | n/a |
| 211 | NRI filter (by country of residence) | Search and filter matching | implemented |
| 212 | Verified profiles only toggle | General / strategy / context | n/a |
| 213 | Online in last 7 / 30 days | General / strategy / context | n/a |
| 214 | Profile photo mandatory toggle | General / strategy / context | n/a |
| 215 | Save & name custom filter sets | Search and filter matching | implemented |
| 216 | 4.6 Communication Features | General / strategy / context | n/a |
| 217 | How It Works | General / strategy / context | n/a |
| 218 | Tier | General / strategy / context | n/a |
| 219 | Express Interest | Interests/shortlist flow | implemented |
| 220 | Send a 'Like' / Interest with a preset message | Interests/shortlist flow | implemented |
| 221 | Free | General / strategy / context | n/a |
| 222 | In-App Messenger | General / strategy / context | n/a |
| 223 | Real-time chat with read receipts, typing indicator, emoji | Realtime chat | implemented |
| 224 | Premium | Subscriptions and payments | implemented |
| 225 | SecureConnect Voice Call | Voice/video calls | implemented |
| 226 | VoIP call through app — neither party's phone number is revealed | General / strategy / context | n/a |
| 227 | Video Call | Voice/video calls | implemented |
| 228 | 1-on-1 HD video call within app (no external app needed) | Voice/video calls | implemented |
| 229 | LiveMatch Events | General / strategy / context | n/a |
| 230 | Group video speed-matchmaking sessions — meet 8–10 matches in 60 minutes | Search and filter matching | implemented |
| 231 | Contact Unlock | General / strategy / context | n/a |
| 232 | View phone number + email of a match (requires their consent setting) | General / strategy / context | n/a |
| 233 | Smart Reply Suggestions | General / strategy / context | n/a |
| 234 | AI suggests 3 conversation starter options based on shared profile details | General / strategy / context | n/a |
| 235 | Message Request Filter | Search and filter matching | implemented |
| 236 | Filter messages: Accepted matches only / All verified / All | Search and filter matching | implemented |
| 237 | 4.7 Privacy & Safety | Verification/safety/reporting | implemented |
| 238 | Photo Album Privacy: Show photos to — Everyone / Connected members only / No one (blur) | Verification/safety/reporting | implemented |
| 239 | Profile Visibility: Public / Members only / Hidden from search (stealth mode) | Search and filter matching | implemented |
| 240 | Last Seen & Online Status: Show or hide from non-connected users | General / strategy / context | n/a |
| 241 | Block & Report: Block any user permanently; report with reason categories (fake/scam/harassment/obscene) | Verification/safety/reporting | implemented |
| 242 | Incognito Browse: View profiles without appearing in their 'Who Viewed Me' list (Premium) | Subscriptions and payments | implemented |
| 243 | Contact Detail Control: Choose who can unlock your contact info | General / strategy / context | n/a |
| 244 | Anti-Screenshot Protection: Screenshots of profile photos disabled in app | General / strategy / context | n/a |
| 245 | Profile Watermarking: All photos watermarked with platform name to deter misuse | General / strategy / context | n/a |
| 246 | 4.8 Horoscope & Kundli Features | General / strategy / context | n/a |
| 247 | Auto Kundli Generation: Generate birth chart from DOB, time, and place | General / strategy / context | n/a |
| 248 | Kundli Match Report: Full 36-point Guna matching with detailed explanation per category | Verification/safety/reporting | implemented |
| 249 | Manglik Compatibility: Flag and explain Manglik status clearly | Search and filter matching | implemented |
| 250 | Nadi Dosha alert with recommended remedies (informational only) | General / strategy / context | n/a |
| 251 | Toggle Off: Users can opt out of horoscope-based matching entirely | General / strategy / context | n/a |
| 252 | 4.9 Family Portal | General / strategy / context | n/a |
| 253 | Parent/Family Access: Parents create a sub-account linked to the main profile | General / strategy / context | n/a |
| 254 | Controlled Sharing: User chooses what family can see (matches list, messages, contact details) | Realtime chat | implemented |
| 255 | Family Shortlist: Family can mark profiles as 'Family Approved' for user to review | Interests/shortlist flow | implemented |
| 256 | Co-Browse Mode: User and parent browse together in a shared session | General / strategy / context | n/a |
| 257 | 4.10 Notifications & Engagement | Notifications | implemented |
| 258 | Daily Curated Matches push notification (morning, configurable time) | Notifications | implemented |
| 259 | 'Someone liked your profile' — real-time notification | Notifications | implemented |
| 260 | 'Your profile was viewed X times today' — weekly digest | General / strategy / context | n/a |
| 261 | Profile completeness nudges with specific tips | General / strategy / context | n/a |
| 262 | Subscription expiry reminders (7 days, 3 days, 1 day before) | Subscriptions and payments | implemented |
| 263 | Inactivity re-engagement: 'You haven't logged in — here are 3 new matches' | General / strategy / context | n/a |
| 264 | 4.11 Premium Subscription Tiers | Subscriptions and payments | implemented |
| 265 | Gold | General / strategy / context | n/a |
| 266 | Platinum | General / strategy / context | n/a |
| 267 | Create Profile | General / strategy / context | n/a |
| 268 | Browse Profiles | General / strategy / context | n/a |
| 269 | Limited 20/day | General / strategy / context | n/a |
| 270 | Unlimited | General / strategy / context | n/a |
| 271 | Send Interests | Interests/shortlist flow | implemented |
| 272 | 5/day | General / strategy / context | n/a |
| 273 | Read Messages | Realtime chat | implemented |
| 274 | Send Messages | Realtime chat | implemented |
| 275 | Voice & Video Calls | Voice/video calls | implemented |
| 276 | 20 unlocks/month | General / strategy / context | n/a |
| 277 | Advanced Search Filters | Search and filter matching | implemented |
| 278 | Profile Boost (2x visibility) | General / strategy / context | n/a |
| 279 | 1/month | General / strategy / context | n/a |
| 280 | 4/month | General / strategy / context | n/a |
| 281 | AI Compatibility Score | Search and filter matching | implemented |
| 282 | Incognito Browse | General / strategy / context | n/a |
| 283 | 2/month | General / strategy / context | n/a |
| 284 | Dedicated Relationship Advisor | General / strategy / context | n/a |
| 285 | 1 session/month | General / strategy / context | n/a |
| 286 | Suggested Price | General / strategy / context | n/a |
| 287 | ₹1,499/mo | General / strategy / context | n/a |
| 288 | ₹2,999/mo | General / strategy / context | n/a |
| 289 | 5. UI/UX Design Specifications | General / strategy / context | n/a |
| 290 | 5.1 Design Principles | General / strategy / context | n/a |
| 291 | Emotionally Warm: Use warm tones (reds, creams, golds) that feel culturally familiar and celebratory | General / strategy / context | n/a |
| 292 | Trust-First: Verification badges visible on every profile card; never hide trust signals | Verification/safety/reporting | implemented |
| 293 | Progressive Disclosure: Don't overwhelm — show free features fully, then reveal premium features contextually | Subscriptions and payments | implemented |
| 294 | Family-Friendly: UI must be usable by users aged 22–65, including non-tech-savvy parents | General / strategy / context | n/a |
| 295 | Speed: Every key action (loading match list, opening chat) must complete in under 1.5 seconds | Realtime chat | implemented |
| 296 | 5.2 App Navigation Structure | General / strategy / context | n/a |
| 297 | Tab / Section | General / strategy / context | n/a |
| 298 | Primary Content | General / strategy / context | n/a |
| 299 | Sub-Screens | General / strategy / context | n/a |
| 300 | 🏠 Home / Discover | General / strategy / context | n/a |
| 301 | Daily match cards (swipe or list view) | General / strategy / context | n/a |
| 302 | Match detail, Compatibility report | Search and filter matching | implemented |
| 303 | 🔍 Search | Search and filter matching | implemented |
| 304 | Full filter-based search | Search and filter matching | implemented |
| 305 | Advanced filters, Saved searches | Search and filter matching | implemented |
| 306 | 💬 Messages | Realtime chat | implemented |
| 307 | Inbox: Interests received, Chats | Interests/shortlist flow | implemented |
| 308 | Chat window, Voice/Video call | Realtime chat | implemented |
| 309 | ❤️ Activity | General / strategy / context | n/a |
| 310 | Who viewed me, Who liked me, Mutual interests | Interests/shortlist flow | implemented |
| 311 | Profile view analytics | Admin and analytics | implemented |
| 312 | 👤 Profile | General / strategy / context | n/a |
| 313 | My profile, Edit, Preview as match sees it | General / strategy / context | n/a |
| 314 | Verification center, Subscription, Settings | Verification/safety/reporting | implemented |
| 315 | 5.3 Key Screens | General / strategy / context | n/a |
| 316 | Home / Discover Screen | General / strategy / context | n/a |
| 317 | Toggle between Card View (swipe-style) and List View (BharatMatrimony-style grid) | General / strategy / context | n/a |
| 318 | Each match card shows: Photo, Name, Age, City, Profession, Compatibility %, Verification badges | Search and filter matching | implemented |
| 319 | Action buttons: ❤️ Like / ✖ Skip / ⭐ Super Interest / 📋 View Full Profile | Interests/shortlist flow | implemented |
| 320 | 'Why this match?' tooltip explaining AI reasoning | General / strategy / context | n/a |
| 321 | Profile Detail Screen | General / strategy / context | n/a |
| 322 | Full-width photo carousel (up to 10 photos) | General / strategy / context | n/a |
| 323 | Video intro player (30 sec, prominent placement) | General / strategy / context | n/a |
| 324 | Sections: About Me → Education & Career → Family Background → Partner Preferences → Horoscope | Onboarding/profile wizard | implemented |
| 325 | Sticky bottom bar: Connect / Message / Voice Call / Video Call | Realtime chat | implemented |
| 326 | Compatibility breakdown chart (radar/spider chart across 6 dimensions) | Search and filter matching | implemented |
| 327 | Similar profiles suggestion at bottom | General / strategy / context | n/a |
| 328 | Chat Screen | Realtime chat | implemented |
| 329 | WhatsApp-inspired bubble layout | General / strategy / context | n/a |
| 330 | In-chat voice note recording | Realtime chat | implemented |
| 331 | In-chat photo sharing (with watermark protection) | Realtime chat | implemented |
| 332 | Smart reply chip suggestions (AI-generated from profile context) | General / strategy / context | n/a |
| 333 | Call button to initiate SecureConnect call without leaving chat | Realtime chat | implemented |
| 334 | 5.4 Color & Typography System | General / strategy / context | n/a |
| 335 | Element | General / strategy / context | n/a |
| 336 | Color | General / strategy / context | n/a |
| 337 | Usage | General / strategy / context | n/a |
| 338 | Primary Brand | General / strategy / context | n/a |
| 339 | #C0392B (Deep Red) | General / strategy / context | n/a |
| 340 | CTA buttons, key icons, headings | General / strategy / context | n/a |
| 341 | Secondary | General / strategy / context | n/a |
| 342 | #E74C3C (Coral Red) | General / strategy / context | n/a |
| 343 | Hover states, secondary actions | General / strategy / context | n/a |
| 344 | Background | General / strategy / context | n/a |
| 345 | #FFF8F8 (Warm White) | General / strategy / context | n/a |
| 346 | App background, cards | General / strategy / context | n/a |
| 347 | Text Primary | General / strategy / context | n/a |
| 348 | #1A1A2E (Dark Navy) | General / strategy / context | n/a |
| 349 | Headings, primary text | General / strategy / context | n/a |
| 350 | Text Secondary | General / strategy / context | n/a |
| 351 | #7F8C8D (Gray) | General / strategy / context | n/a |
| 352 | Subtitles, metadata | General / strategy / context | n/a |
| 353 | Verified Badge | General / strategy / context | n/a |
| 354 | #27AE60 (Green) | General / strategy / context | n/a |
| 355 | Verification checkmarks | Verification/safety/reporting | implemented |
| 356 | Premium Badge | Subscriptions and payments | implemented |
| 357 | #F39C12 (Gold) | General / strategy / context | n/a |
| 358 | Premium member indicators | Subscriptions and payments | implemented |
| 359 | Font — Headings | General / strategy / context | n/a |
| 360 | Poppins Bold | General / strategy / context | n/a |
| 361 | All section titles and profile names | General / strategy / context | n/a |
| 362 | Font — Body | General / strategy / context | n/a |
| 363 | Inter Regular | General / strategy / context | n/a |
| 364 | All body copy, metadata | General / strategy / context | n/a |
| 365 | 5.5 UX Pain Points from Competitors to Avoid | General / strategy / context | n/a |
| 366 | DO NOT gate messaging immediately after sign-up — allow 3 free messages per match before paywall | Realtime chat | implemented |
| 367 | DO NOT show 'My Matches' tab that contains un-matched profiles — causes confusion (Shaadi.com complaint) | General / strategy / context | n/a |
| 368 | DO NOT allow profiles without photos — mandatory photo to complete profile | General / strategy / context | n/a |
| 369 | DO NOT use aggressive upsell popups on every interaction — use contextual, non-blocking upgrade nudges | Verification/safety/reporting | implemented |
| 370 | DO NOT hide verification status — show clearly on every profile card and in search results | Search and filter matching | implemented |
| 371 | 6. Technical Architecture | General / strategy / context | n/a |
| 372 | 6.1 Recommended Tech Stack | General / strategy / context | n/a |
| 373 | Layer | General / strategy / context | n/a |
| 374 | Technology | General / strategy / context | n/a |
| 375 | Reason | General / strategy / context | n/a |
| 376 | Mobile Frontend | General / strategy / context | n/a |
| 377 | Flutter (iOS + Android) | General / strategy / context | n/a |
| 378 | Single codebase, native performance, fast UI | General / strategy / context | n/a |
| 379 | Web Frontend | General / strategy / context | n/a |
| 380 | React.js + Next.js | General / strategy / context | n/a |
| 381 | SEO-friendly, server-side rendering | General / strategy / context | n/a |
| 382 | Backend API | General / strategy / context | n/a |
| 383 | Node.js + Express / Go | General / strategy / context | n/a |
| 384 | High concurrency for real-time features | General / strategy / context | n/a |
| 385 | Real-Time Messaging | General / strategy / context | n/a |
| 386 | WebSockets + Redis Pub/Sub | General / strategy / context | n/a |
| 387 | Low-latency chat and presence | Realtime chat | implemented |
| 388 | Video/Voice Calls | Voice/video calls | implemented |
| 389 | Agora.io or Daily.co SDK | General / strategy / context | n/a |
| 390 | Proven WebRTC abstraction, masked calls | Voice/video calls | implemented |
| 391 | Database | General / strategy / context | n/a |
| 392 | PostgreSQL (primary) + Redis (cache) | General / strategy / context | n/a |
| 393 | Relational for profiles, fast reads | General / strategy / context | n/a |
| 394 | Search Engine | Search and filter matching | implemented |
| 395 | Elasticsearch | Search and filter matching | implemented |
| 396 | Full-text + filter-based profile search | Search and filter matching | implemented |
| 397 | AI/ML | General / strategy / context | n/a |
| 398 | Python (FastAPI) + TensorFlow / PyTorch | General / strategy / context | n/a |
| 399 | Compatibility scoring, fraud detection | Search and filter matching | implemented |
| 400 | Cloud Infrastructure | General / strategy / context | n/a |
| 401 | AWS (India region ap-south-1) | General / strategy / context | n/a |
| 402 | Low latency for Indian users, PDPA compliance | General / strategy / context | n/a |
| 403 | Storage | General / strategy / context | n/a |
| 404 | AWS S3 + CloudFront CDN | General / strategy / context | n/a |
| 405 | Fast photo/video delivery | General / strategy / context | n/a |
| 406 | Notifications | Notifications | implemented |
| 407 | Firebase Cloud Messaging (FCM) | General / strategy / context | n/a |
| 408 | Push notifications iOS + Android | Notifications | implemented |
| 409 | Payments | Subscriptions and payments | implemented |
| 410 | Razorpay + Apple Pay / Google Pay | Subscriptions and payments | implemented |
| 411 | India + international payments | Subscriptions and payments | implemented |
| 412 | 6.2 Security & Compliance | General / strategy / context | n/a |
| 413 | End-to-end encryption for all messages (Signal Protocol or AES-256) | Realtime chat | implemented |
| 414 | All photos stored encrypted at rest in S3 | General / strategy / context | n/a |
| 415 | HTTPS/TLS 1.3 for all API calls | General / strategy / context | n/a |
| 416 | PDPA / IT Act 2000 compliance for Indian user data | General / strategy / context | n/a |
| 417 | GDPR compliance for EU/UK NRI users | General / strategy / context | n/a |
| 418 | Rate limiting on all APIs to prevent scraping | General / strategy / context | n/a |
| 419 | Automatic PII redaction in logs | General / strategy / context | n/a |
| 420 | OTP-based 2FA available for account access | OTP authentication | implemented |
| 421 | 6.3 Non-Functional Requirements | General / strategy / context | n/a |
| 422 | Metric | General / strategy / context | n/a |
| 423 | Target | General / strategy / context | n/a |
| 424 | How Measured | General / strategy / context | n/a |
| 425 | App Load Time (cold start) | General / strategy / context | n/a |
| 426 | < 2.5 seconds | General / strategy / context | n/a |
| 427 | Firebase Performance | General / strategy / context | n/a |
| 428 | Match List Load | General / strategy / context | n/a |
| 429 | < 1.5 seconds | General / strategy / context | n/a |
| 430 | API response time | General / strategy / context | n/a |
| 431 | Message Delivery Latency | Realtime chat | implemented |
| 432 | < 300ms | General / strategy / context | n/a |
| 433 | WebSocket ping | General / strategy / context | n/a |
| 434 | API Uptime | General / strategy / context | n/a |
| 435 | 99.9% SLA | General / strategy / context | n/a |
| 436 | AWS CloudWatch | General / strategy / context | n/a |
| 437 | Concurrent Users (launch) | General / strategy / context | n/a |
| 438 | Load testing | General / strategy / context | n/a |
| 439 | Profile Photo CDN Cache | General / strategy / context | n/a |
| 440 | < 200ms globally | General / strategy / context | n/a |
| 441 | CloudFront metrics | General / strategy / context | n/a |
| 442 | 7. Launch Roadmap | General / strategy / context | n/a |
| 443 | Phase | General / strategy / context | n/a |
| 444 | Timeline | General / strategy / context | n/a |
| 445 | Deliverables | General / strategy / context | n/a |
| 446 | Phase 1 — MVP | General / strategy / context | n/a |
| 447 | Months 1–3 | General / strategy / context | n/a |
| 448 | Registration, Profile creation, Basic search & filters, Interest / Like system, Simple inbox, Phone & ID verification (2 levels), iOS + Android apps | Onboarding/profile wizard | implemented |
| 449 | Phase 2 — Core | General / strategy / context | n/a |
| 450 | Months 4–5 | General / strategy / context | n/a |
| 451 | In-app messaging, SecureConnect voice calls, AI compatibility scoring (v1), Advanced search filters, Premium subscription (Gold tier), Profile boost, Horoscope matching | Search and filter matching | implemented |
| 452 | Phase 3 — Growth | General / strategy / context | n/a |
| 453 | Months 6–7 | General / strategy / context | n/a |
| 454 | Video calls, LiveMatch virtual events, Photo album privacy controls, Family portal, Incognito browse, Platinum tier, Biodata PDF export, Push notification engine | Voice/video calls | implemented |
| 455 | Phase 4 — AI+ | General / strategy / context | n/a |
| 456 | Months 8–10 | General / strategy / context | n/a |
| 457 | Behavioral AI (v2 — learns from skips/likes), Smart reply AI, Fraud detection ML model, NRI time-zone aware features, Video introduction on profiles, Personality quiz integration | General / strategy / context | n/a |
| 458 | Phase 5 — Scale | General / strategy / context | n/a |
| 459 | Months 11–12 | General / strategy / context | n/a |
| 460 | Web platform launch, Advisor marketplace, Community-specific sub-brands, API partnerships, Analytics dashboard for admins, A/B testing framework | Admin and analytics | implemented |
| 461 | 8. Success Metrics & KPIs | Admin and analytics | implemented |
| 462 | Month 3 Target | General / strategy / context | n/a |
| 463 | Month 12 Target | General / strategy / context | n/a |
| 464 | Priority | General / strategy / context | n/a |
| 465 | Daily Active Users (DAU) | General / strategy / context | n/a |
| 466 | Profile Completion Rate | General / strategy / context | n/a |
| 467 | Premium Conversion Rate | Subscriptions and payments | implemented |
| 468 | Message Response Rate | Realtime chat | implemented |
| 469 | Verified Profile % | General / strategy / context | n/a |
| 470 | Day-7 Retention | General / strategy / context | n/a |
| 471 | App Store Rating | General / strategy / context | n/a |
| 472 | Success Stories Reported | Verification/safety/reporting | implemented |
| 473 | Fake Profile Takedown Time | General / strategy / context | n/a |
| 474 | < 24 hours | General / strategy / context | n/a |
| 475 | < 4 hours | General / strategy / context | n/a |
| 476 | 9. Risks & Mitigations | General / strategy / context | n/a |
| 477 | Risk | General / strategy / context | n/a |
| 478 | Impact | General / strategy / context | n/a |
| 479 | Mitigation | General / strategy / context | n/a |
| 480 | Fake profile proliferation (common in Jeevansathi reviews) | General / strategy / context | n/a |
| 481 | High — destroys user trust | General / strategy / context | n/a |
| 482 | ML fraud detection + human review queue + scam pattern database | General / strategy / context | n/a |
| 483 | Paywall frustration causing churn (Shaadi.com major complaint) | General / strategy / context | n/a |
| 484 | High — early drop-off | General / strategy / context | n/a |
| 485 | Free tier must deliver genuine value; delay paywalls to Day 3+ | General / strategy / context | n/a |
| 486 | Low female user ratio (skews match quality for male users) | General / strategy / context | n/a |
| 487 | Medium — lowers platform quality | General / strategy / context | n/a |
| 488 | Free premium for women in first 6 months; women-first UI design | Subscriptions and payments | implemented |
| 489 | Data breach of sensitive personal data | General / strategy / context | n/a |
| 490 | Critical — regulatory and reputational | General / strategy / context | n/a |
| 491 | E2E encryption, regular pen testing, PDPA compliance, bug bounty | General / strategy / context | n/a |
| 492 | Poor matchmaking quality hurts retention | Search and filter matching | implemented |
| 493 | High — users leave if AI is bad | General / strategy / context | n/a |
| 494 | Start with rule-based matching, layer AI after data accumulates (3+ months) | General / strategy / context | n/a |
| 495 | 10. Appendix — Competitor URLs Researched | Search and filter matching | implemented |
| 496 | The following platforms were analyzed for this PRD: | General / strategy / context | n/a |
| 497 | Shaadi.com — https://www.shaadi.com | General / strategy / context | n/a |
| 498 | BharatMatrimony — https://www.bharatmatrimony.com | General / strategy / context | n/a |
| 499 | Jeevansathi.com — https://www.jeevansathi.com | General / strategy / context | n/a |
| 500 | Matrimony.com Group (Tamil, Marathi, Punjabi, Telugu sub-brands) | General / strategy / context | n/a |
| 501 | Apptunix Matrimonial App Market Research 2025 | Search and filter matching | implemented |
| 502 | App Store & Google Play user reviews analysis (2024–2025) | General / strategy / context | n/a |
| 503 | This PRD is version 1.0 and should be treated as a living document. Update with user feedback from beta testing before finalizing. | General / strategy / context | n/a |

### VivahApp_GapAnalysis_100_100.docx

| # | Extracted line | Classified feature | Current status |
|---:|---|---|---|
| 1 | VIVAH.APP | General / strategy / context | n/a |
| 2 | Gap-Analysis & Completion Blueprint | General / strategy / context | n/a |
| 3 | 17 Critical Gaps Identified & Resolved — 100 / 100 Readiness | General / strategy / context | n/a |
| 4 | Addendum to: Ultimate Technical Blueprint v1.0 \| March 2026 \| Founder Only | General / strategy / context | n/a |
| 5 | READ THIS FIRST: | General / strategy / context | n/a |
| 6 | Both previous documents are architecturally excellent but contain 17 critical omissions — any one of which can block launch, delay revenue, or get your app removed from app stores. This document resolves every gap with concrete, actionable answers. | Verification/safety/reporting | implemented |
| 7 | A. LAUNCH BLOCKERS — Fix These First | Verification/safety/reporting | implemented |
| 8 | These three gaps are not features — they are pre-conditions. The entire platform cannot legally collect money or ship to app stores without them. | General / strategy / context | n/a |
| 9 | A1. Legal Entity Setup — Day Zero, Before Any Code | General / strategy / context | n/a |
| 10 | Blocker: | Verification/safety/reporting | implemented |
| 11 | Razorpay, PayU, and Stripe India require a registered Indian business entity + GST number. You CANNOT accept payments as an individual. No entity = zero revenue. | Subscriptions and payments | implemented |
| 12 | Step | General / strategy / context | n/a |
| 13 | Action Required | General / strategy / context | n/a |
| 14 | 1. Company Registration | General / strategy / context | n/a |
| 15 | Register a Private Limited Company (preferred) or LLP via MCA21 portal. Cost: ~₹7,000–₹12,000 via CA or IndiaFilings/Razorpay Rize. Timeline: 10–15 working days. | Subscriptions and payments | implemented |
| 16 | 2. GST Registration | General / strategy / context | n/a |
| 17 | Apply for GST after company registration. Required for digital services. Threshold: ₹20L revenue, but Razorpay mandate is immediate. Timeline: 7–10 days. | Subscriptions and payments | implemented |
| 18 | 3. Business Bank Account | General / strategy / context | n/a |
| 19 | Open current account (HDFC/ICICI/RBL recommended for Razorpay compatibility). Required for payment settlement. | Search and filter matching | implemented |
| 20 | 4. Razorpay Account | Subscriptions and payments | implemented |
| 21 | Apply at razorpay.com/businesses. Submit: COI, GST certificate, cancelled cheque, PAN. Approval: 2–5 days. Activate Test mode immediately. | Subscriptions and payments | implemented |
| 22 | 5. Trademark (Optional) | General / strategy / context | n/a |
| 23 | File for 'VIVAH' or 'VIVAH.APP' trademark under Class 45 (matrimony/dating services). Cost: ₹4,500 per class. Protects brand before you go viral. | General / strategy / context | n/a |
| 24 | Shortcut: | General / strategy / context | n/a |
| 25 | Use Razorpay Rize (rize.razorpay.com) — they handle company registration + bank account + Razorpay activation as a bundle. Estimated total timeline: 3 weeks. | Subscriptions and payments | implemented |
| 26 | A2. Authentication — Concrete Implementation Plan | Subscriptions and payments | implemented |
| 27 | Blueprint v1 said 'auth decided later.' This is wrong. Every other feature (messaging, matching, payments) depends on identity. Auth must be Sprint 0, not an afterthought. | Subscriptions and payments | implemented |
| 28 | Chosen Stack: Better Auth + Cloudflare Workers | General / strategy / context | n/a |
| 29 | Better Auth (betterauth.dev) is the only open-source auth library with native Cloudflare Workers + D1 support. It handles OTP, JWT, sessions, social login, and device management out of the box. | OTP authentication | implemented |
| 30 | Auth Flow | General / strategy / context | n/a |
| 31 | Implementation Detail | General / strategy / context | n/a |
| 32 | Mobile OTP (Primary) | OTP authentication | implemented |
| 33 | User enters mobile → Worker calls Fast2SMS API → 6-digit OTP stored as Argon2id hash in D1 otp_log table (TTL: 10 min, 3 attempts max) → User submits OTP → hash compared → Better Auth issues session. | OTP authentication | implemented |
| 34 | JWT Structure | General / strategy / context | n/a |
| 35 | Access Token: RS256, 15-min TTL. Payload: { sub: userId, plan: 'free', deviceId, iat, exp }. Refresh Token: 30-day TTL, stored in HttpOnly cookie + D1 sessions table. | Subscriptions and payments | implemented |
| 36 | Social Login | General / strategy / context | n/a |
| 37 | Better Auth Google + Apple providers. One config block. Apple Sign-In is MANDATORY on iOS — Apple rejects apps with social login if Apple option is absent. | Verification/safety/reporting | implemented |
| 38 | Session Storage | General / strategy / context | n/a |
| 39 | Sessions stored in D1 sessions table (persistent) + KV SESSIONS namespace (hot cache, 30-day TTL). KV lookup = ~1ms. D1 fallback on KV miss. | General / strategy / context | n/a |
| 40 | Device Binding | General / strategy / context | n/a |
| 41 | Each session tagged with device fingerprint (canvas + WebGL hash). Token from Device A rejected on Device B. Prevents session token sharing. | General / strategy / context | n/a |
| 42 | Admin Auth | Admin and analytics | implemented |
| 43 | Separate Better Auth tenant for admin panel. TOTP (Google Authenticator) mandatory for all admin roles. Admin sessions stored in separate D1 DB. | Admin and analytics | implemented |
| 44 | Resolution: | General / strategy / context | n/a |
| 45 | Install Better Auth in vivah-auth Worker. Configure D1 adapter. All auth flows operational in < 1 sprint week. Zero custom crypto code needed. | General / strategy / context | n/a |
| 46 | A3. App Store Compliance — Get This Wrong = Weeks of Delay | General / strategy / context | n/a |
| 47 | Google Play and Apple App Store have specific policies for matrimony/dating apps. Both require explicit declarations before submission. Missing any = rejection. | General / strategy / context | n/a |
| 48 | Google Play Requirements (Policy: Apps for Dating) | General / strategy / context | n/a |
| 49 | Must declare: app is for adult users (18+) seeking romantic/marriage connections | General / strategy / context | n/a |
| 50 | Mandatory: in-app safety page with block, report, and emergency contact features | Verification/safety/reporting | implemented |
| 51 | Mandatory: human moderation process described in app listing | Admin and analytics | implemented |
| 52 | Profile photos: must state AI + human review process in store description | General / strategy / context | n/a |
| 53 | Age verification: declare mechanism (DOB + Aadhaar eKYC) | Verification/safety/reporting | implemented |
| 54 | Contact us email: policy-level support email must be live before submission | General / strategy / context | n/a |
| 55 | Privacy policy URL: must be live, cover all data collected, linked in app AND store listing | Verification/safety/reporting | implemented |
| 56 | Apple App Store Requirements (Guideline 1.3 — Kids / Guideline 5.6 — Developer Code of Conduct) | General / strategy / context | n/a |
| 57 | Must implement Apple Sign-In if any other social login is present (Google, Facebook) — no exceptions | General / strategy / context | n/a |
| 58 | In-app reporting mechanism must be present before submission | Verification/safety/reporting | implemented |
| 59 | Account deletion must be available in-app (not just via email) — Apple mandates this since 2022 | General / strategy / context | n/a |
| 60 | No misleading screenshots — don't show verified badge on unverified test profiles | General / strategy / context | n/a |
| 61 | Subscription pricing must be displayed clearly in-app before purchase (StoreKit required) | Subscriptions and payments | implemented |
| 62 | Timeline Risk: | General / strategy / context | n/a |
| 63 | First submission always takes 1–3 days for review. Rejection adds another cycle. Submit TestFlight build 4 weeks before intended launch date. | General / strategy / context | n/a |
| 64 | B. Cloudflare Hard Limits — What the Blueprint Got Wrong | General / strategy / context | n/a |
| 65 | Both previous documents presented Cloudflare as limitless. It is not. These limits are real, will bite you in production, and need architectural decisions now. | General / strategy / context | n/a |
| 66 | B1. Exact Free Tier Capacity Math | General / strategy / context | n/a |
| 67 | Service | General / strategy / context | n/a |
| 68 | Free Limit | General / strategy / context | n/a |
| 69 | When You Hit It | General / strategy / context | n/a |
| 70 | Workers | General / strategy / context | n/a |
| 71 | 100,000 req/day | General / strategy / context | n/a |
| 72 | ~1,000 DAU doing 100 actions/day each | General / strategy / context | n/a |
| 73 | D1 Reads | General / strategy / context | n/a |
| 74 | 5M reads/day | General / strategy / context | n/a |
| 75 | ~5,000 DAU (each session = ~1,000 D1 reads) | General / strategy / context | n/a |
| 76 | D1 Writes | General / strategy / context | n/a |
| 77 | 100K writes/day | General / strategy / context | n/a |
| 78 | ~5,000 DAU (profile updates, messages, etc.) | Realtime chat | implemented |
| 79 | KV Reads | General / strategy / context | n/a |
| 80 | 100K reads/day | General / strategy / context | n/a |
| 81 | ~1,000 DAU (every auth check hits KV) | General / strategy / context | n/a |
| 82 | KV Writes | General / strategy / context | n/a |
| 83 | 1,000 writes/day | General / strategy / context | n/a |
| 84 | ~500 logins/day (each login writes session to KV) | General / strategy / context | n/a |
| 85 | Workers AI | General / strategy / context | n/a |
| 86 | 10,000 neurons/day | General / strategy / context | n/a |
| 87 | ~100 Kundali computations OR ~500 profile embeddings | General / strategy / context | n/a |
| 88 | Durable Objects | General / strategy / context | n/a |
| 89 | 1M req/month | General / strategy / context | n/a |
| 90 | ~33,000 WebSocket messages/day | Realtime chat | implemented |
| 91 | R2 Storage | General / strategy / context | n/a |
| 92 | 10 GB free | General / strategy / context | n/a |
| 93 | ~10,000 profiles with 3 photos each at 300KB avg | General / strategy / context | n/a |
| 94 | KV Writes Is Your First Bottleneck: | General / strategy / context | n/a |
| 95 | At 500 logins/day you exhaust KV writes on the free tier. Fix: Store sessions primarily in D1. Use KV only for hot-read cache (not write path). Upgrade to Workers Paid ($5/month) solves all limits simultaneously. | General / strategy / context | n/a |
| 96 | B2. Kundali Engine — Cannot Run Inline in Workers | General / strategy / context | n/a |
| 97 | Architecture Error in Blueprint v1: | General / strategy / context | n/a |
| 98 | Swiss Ephemeris WASM is 2–4MB. Workers free tier limit is 1MB. Paid is 10MB. Even on paid, WASM load + ephemeris computation + Workers AI LLM inference exceeds the 30ms CPU limit per request. | General / strategy / context | n/a |
| 99 | Correct Architecture — Async Queue Pattern: | General / strategy / context | n/a |
| 100 | User submits DOB + TOB + POB on profile | General / strategy / context | n/a |
| 101 | Worker receives request, immediately returns 202 Accepted with job_id | General / strategy / context | n/a |
| 102 | Worker enqueues job to Cloudflare Queue (vivah-kundali-queue) | General / strategy / context | n/a |
| 103 | Queue consumer Worker picks up job (separate Worker with higher CPU allowance) | General / strategy / context | n/a |
| 104 | WASM ephemeris calculates positions (15–25ms CPU) | General / strategy / context | n/a |
| 105 | Workers AI LLM generates interpretation (50–200ms wall time, non-CPU-billed) | General / strategy / context | n/a |
| 106 | Result written to D1 horoscopes table + KV for instant retrieval | General / strategy / context | n/a |
| 107 | Push notification sent to user: 'Your Kundali is ready' | Notifications | implemented |
| 108 | All subsequent requests for this Kundali hit KV cache — zero compute cost | General / strategy / context | n/a |
| 109 | WASM Bundling Solution: | General / strategy / context | n/a |
| 110 | Store Swiss Ephemeris WASM file in R2. Queue consumer Worker fetches from R2 on cold start and caches in module-level variable for warm requests. WASM never bundled inside Worker script — bypasses size limit entirely. | General / strategy / context | n/a |
| 111 | B3. D1 Limitations You Must Design Around | General / strategy / context | n/a |
| 112 | D1 Limitation | General / strategy / context | n/a |
| 113 | Design Workaround | General / strategy / context | n/a |
| 114 | No full-text search by default | Search and filter matching | implemented |
| 115 | Enable D1 FTS5 extension in schema: CREATE VIRTUAL TABLE profiles_fts USING fts5(name, bio, occupation). Run on D1 paid tier only. | General / strategy / context | n/a |
| 116 | 500 row result limit per query | General / strategy / context | n/a |
| 117 | Always paginate with LIMIT 50. Never SELECT * without pagination. Cursor-based: WHERE id > ? ORDER BY id LIMIT 50. | General / strategy / context | n/a |
| 118 | No stored procedures | General / strategy / context | n/a |
| 119 | All business logic in Worker. D1 = dumb storage only. Complex queries = multiple round trips composed in Worker. | General / strategy / context | n/a |
| 120 | No real-time subscriptions | Subscriptions and payments | implemented |
| 121 | D1 is not Postgres. No LISTEN/NOTIFY. Use Durable Objects + KV for all real-time state. D1 = source of truth, never event source. | General / strategy / context | n/a |
| 122 | Single-region write primary | General / strategy / context | n/a |
| 123 | D1 writes go to one region. Reads replicate globally. For India users: bind D1 to nearest region in wrangler.toml: [[d1_databases]] ... location_hint = 'apac' | General / strategy / context | n/a |
| 124 | Connection limit | General / strategy / context | n/a |
| 125 | Workers share D1 connection pool. Under heavy load, use Hyperdrive (available on Workers Paid) for connection pooling and query caching. | General / strategy / context | n/a |
| 126 | C. Missing Database Tables — Complete D1 Schema | General / strategy / context | n/a |
| 127 | Both documents defined 12 tables. The complete schema requires 20. These 8 missing tables are essential for notifications, safety, feature control, and audit compliance. | Verification/safety/reporting | implemented |
| 128 | Missing Table | General / strategy / context | n/a |
| 129 | Full Schema + Purpose | General / strategy / context | n/a |
| 130 | otp_log | General / strategy / context | n/a |
| 131 | id TEXT PK, mobile_hash TEXT, code_hash TEXT, attempts INT DEFAULT 0, expires_at INT, used INT DEFAULT 0, ip_subnet TEXT, created_at INT. PURPOSE: OTP audit trail + brute force detection. | OTP authentication | implemented |
| 132 | sessions | General / strategy / context | n/a |
| 133 | id TEXT PK, user_id TEXT FK, device_fingerprint TEXT, refresh_token_hash TEXT, last_active INT, ip_subnet TEXT, ua_hash TEXT, revoked INT DEFAULT 0, created_at INT. PURPOSE: Server-side session management + revocation. | General / strategy / context | n/a |
| 134 | verification_requests | Verification/safety/reporting | implemented |
| 135 | id TEXT PK, user_id TEXT FK, type TEXT (aadhaar/employment/education/photo), document_r2_key TEXT, submitted_at INT, reviewed_by TEXT, reviewed_at INT, status TEXT (pending/approved/rejected), rejection_reason TEXT. PURPOSE: ID verification queue for admin review. | Verification/safety/reporting | implemented |
| 136 | notifications | Notifications | implemented |
| 137 | id TEXT PK, user_id TEXT FK, type TEXT, title TEXT, body TEXT, data_json TEXT, channel TEXT (push/email/sms), sent_at INT, read_at INT, fcm_msg_id TEXT. PURPOSE: Full notification log + read tracking. | Notifications | implemented |
| 138 | notification_prefs | Notifications | implemented |
| 139 | user_id TEXT PK, new_match BOOL DEFAULT 1, interest_received BOOL DEFAULT 1, message BOOL DEFAULT 1, profile_view BOOL DEFAULT 1, daily_digest BOOL DEFAULT 1, promo BOOL DEFAULT 0, channel_push BOOL DEFAULT 1, channel_email BOOL DEFAULT 1, channel_sms BOOL DEFAULT 0. PURPOSE: Granular user notification preferences. | Interests/shortlist flow | implemented |
| 140 | feature_flags | General / strategy / context | n/a |
| 141 | id TEXT PK, flag_name TEXT UNIQUE, enabled INT DEFAULT 0, rollout_pct INT DEFAULT 0, allowed_plans JSON, description TEXT, updated_at INT. PURPOSE: Kill-switch for any feature. Ship code dark, enable via flag. | Subscriptions and payments | implemented |
| 142 | audit_events | General / strategy / context | n/a |
| 143 | id TEXT PK, actor_id TEXT, actor_type TEXT (user/admin/system), action TEXT, target_type TEXT, target_id TEXT, metadata_json TEXT, ip_subnet TEXT, ts INT. PURPOSE: Immutable audit trail for DPDP Act compliance + legal proceedings. | Admin and analytics | implemented |
| 144 | community_groups | General / strategy / context | n/a |
| 145 | id TEXT PK, name TEXT, religion TEXT, caste TEXT, city TEXT, admin_id TEXT FK, member_count INT DEFAULT 0, created_at INT, status TEXT. PURPOSE: Vivah Circles™ feature — private community matching groups. | Admin and analytics | implemented |
| 146 | Index These Immediately: | General / strategy / context | n/a |
| 147 | Every FK column and every column used in WHERE clauses must have an index in D1. Missing indexes = full table scans = 10x slower queries at 100K+ profiles. Add: CREATE INDEX idx_notifications_user ON notifications(user_id, read_at); | Notifications | implemented |
| 148 | D. Notification System Design — The #1 Retention Lever | Notifications | implemented |
| 149 | Missed Entirely in Both Docs: | General / strategy / context | n/a |
| 150 | Notification strategy is the single highest-ROI feature for retention. Too many notifications = 34% of users uninstall within 7 days. Too few = 60% churn by Day 30. The science of when and what to send is a product decision, not just an engineering one. | Notifications | implemented |
| 151 | D1. Notification Type Matrix | Notifications | implemented |
| 152 | Trigger | General / strategy / context | n/a |
| 153 | Message & Timing | Realtime chat | implemented |
| 154 | Channel | General / strategy / context | n/a |
| 155 | New Interest Received | Interests/shortlist flow | implemented |
| 156 | Immediate: '[Name] is interested in your profile ❤️' | Interests/shortlist flow | implemented |
| 157 | Push + In-app | Notifications | implemented |
| 158 | Interest Accepted | Interests/shortlist flow | implemented |
| 159 | Immediate: '[Name] accepted your interest! Say hello 👋' | Interests/shortlist flow | implemented |
| 160 | New Message Received | Realtime chat | implemented |
| 161 | Immediate if app closed, 30-sec delay if app open | General / strategy / context | n/a |
| 162 | Profile Viewed | General / strategy / context | n/a |
| 163 | Batched: 'Your profile was viewed 5 times today' — sent at 7 PM | General / strategy / context | n/a |
| 164 | Push only | Notifications | implemented |
| 165 | New Match Available | General / strategy / context | n/a |
| 166 | Daily at 9 AM: 'You have 12 new matches today ✨' | General / strategy / context | n/a |
| 167 | Push + Email | Notifications | implemented |
| 168 | Profile Incomplete | General / strategy / context | n/a |
| 169 | Day 2, Day 7 if < 70% complete: 'Add your photo to get 3x more matches' | General / strategy / context | n/a |
| 170 | Inactive User (Day 7) | General / strategy / context | n/a |
| 171 | '[Name] from [City] just joined — sounds like a great match!' | General / strategy / context | n/a |
| 172 | Inactive User (Day 14) | General / strategy / context | n/a |
| 173 | Personalised email: Weekly digest of top 5 matches | General / strategy / context | n/a |
| 174 | Email only | General / strategy / context | n/a |
| 175 | Inactive User (Day 30) | General / strategy / context | n/a |
| 176 | Final nudge: 'Your profile is hidden — reactivate to appear in matches' | General / strategy / context | n/a |
| 177 | Email + SMS | General / strategy / context | n/a |
| 178 | Subscription Expiring | Subscriptions and payments | implemented |
| 179 | 7 days, 3 days, day-of: 'Your plan expires on [date]' | Subscriptions and payments | implemented |
| 180 | Verification Approved | Verification/safety/reporting | implemented |
| 181 | Immediate: 'Vivah Verified™ badge added to your profile!' | General / strategy / context | n/a |
| 182 | Boost Active | General / strategy / context | n/a |
| 183 | Start: 'Your boost is live — watch the views roll in!' End: Summary | General / strategy / context | n/a |
| 184 | D2. Technical Notification Architecture | Notifications | implemented |
| 185 | FCM (Android) + APNS (iOS): Device tokens stored in D1 users table (fcm_token, apns_token columns — add to schema) | General / strategy / context | n/a |
| 186 | Cloudflare Queue (vivah-notify-queue): All notification dispatch goes through queue. Worker fires and forgets. Queue consumer handles retries. | Notifications | implemented |
| 187 | Email via Resend.com: React Email templates. Transactional only on free tier. Add Loops.so for drip sequences (re-engagement flows). | General / strategy / context | n/a |
| 188 | SMS via Fast2SMS: ONLY for OTP and subscription expiry. NOT for marketing. SMS = highest cost, reserve for critical only. | OTP authentication | implemented |
| 189 | Frequency cap: Maximum 3 push notifications per user per day. Enforced via KV counter (key: notif_cap:{userId}:{date}, TTL: 24h). | Notifications | implemented |
| 190 | Quiet hours: No push between 10 PM – 8 AM. Store user timezone in profile. Worker checks before dispatch. | Notifications | implemented |
| 191 | E. Onboarding Funnel + Cold-Start Strategy | Onboarding/profile wizard | implemented |
| 192 | E1. Onboarding Funnel — Industry Drop-Off Data | Onboarding/profile wizard | implemented |
| 193 | Industry benchmarks for matrimony apps show severe drop-off before first match interaction. Vivah.App must engineer against each drop-off point explicitly. | General / strategy / context | n/a |
| 194 | Funnel Stage | General / strategy / context | n/a |
| 195 | Industry Drop-Off | General / strategy / context | n/a |
| 196 | Vivah.App Intervention | General / strategy / context | n/a |
| 197 | App Install → Registration Start | General / strategy / context | n/a |
| 198 | 30% abandon | General / strategy / context | n/a |
| 199 | Landing page with 'Start in 2 minutes' promise. No email required at step 1. | General / strategy / context | n/a |
| 200 | Registration → Mobile Verify | General / strategy / context | n/a |
| 201 | 15% abandon | General / strategy / context | n/a |
| 202 | WhatsApp OTP fallback eliminates SMS non-delivery failures. | OTP authentication | implemented |
| 203 | Mobile Verify → Basic Profile | General / strategy / context | n/a |
| 204 | 25% abandon | General / strategy / context | n/a |
| 205 | 3-field MVP: Name + DOB + Gender. Everything else optional at this stage. | General / strategy / context | n/a |
| 206 | Basic Profile → Photo Upload | General / strategy / context | n/a |
| 207 | 40% abandon | General / strategy / context | n/a |
| 208 | Show blurred placeholder matches BEFORE photo upload. 'Upload photo to reveal matches.' Curiosity gap. | General / strategy / context | n/a |
| 209 | Photo Upload → Partner Prefs | General / strategy / context | n/a |
| 210 | 20% abandon | General / strategy / context | n/a |
| 211 | AI pre-fills partner preferences from their own profile data. One-click confirm. | Onboarding/profile wizard | implemented |
| 212 | Partner Prefs → First Match View | General / strategy / context | n/a |
| 213 | 10% abandon | General / strategy / context | n/a |
| 214 | Show match queue IMMEDIATELY after prefs — no 'processing' delay. Pre-compute for common community/city combos. | General / strategy / context | n/a |
| 215 | Match View → First Interest Sent | Interests/shortlist flow | implemented |
| 216 | 50% abandon | General / strategy / context | n/a |
| 217 | AI Icebreaker Engine generates first message suggestion on match card. One-tap send. | Realtime chat | implemented |
| 218 | The Curiosity Gap: | General / strategy / context | n/a |
| 219 | Never show an empty state. If user has no matches yet (new community), show 'preview profiles' that are blurred. This converts 2x better than empty state + waiting message. | Realtime chat | implemented |
| 220 | E2. Cold-Start Problem — The Existential Risk | General / strategy / context | n/a |
| 221 | This Kills Matrimony Startups: | General / strategy / context | n/a |
| 222 | Matrimony requires simultaneous critical mass of BOTH genders in the SAME community + city. Without this, early users see zero matches and churn instantly. This is the hardest problem in matrimony. | General / strategy / context | n/a |
| 223 | The 3-Phase Cold-Start Playbook | General / strategy / context | n/a |
| 224 | Phase | General / strategy / context | n/a |
| 225 | Strategy | General / strategy / context | n/a |
| 226 | Phase 0: Seed Community (Month 1–2) | General / strategy / context | n/a |
| 227 | Pick ONE community + ONE city. Example: Tamil Brahmin, Chennai. Personally reach out to 500 profiles. Matrimony FB groups, WhatsApp groups, local temple networks. Offer 6 months free. Goal: 250 male + 250 female verified profiles in this exact community before launch. | General / strategy / context | n/a |
| 228 | Phase 1: Controlled Expansion (Month 3–4) | General / strategy / context | n/a |
| 229 | Open 3 more communities in same city. Then expand to 3 cities in same community. Never expand both dimensions simultaneously — dilutes density. | General / strategy / context | n/a |
| 230 | Phase 2: Viral Loop Activation (Month 5+) | General / strategy / context | n/a |
| 231 | Shagun Board™ (success stories) creates social proof. Each wedding story shared = 50–200 organic registrations. Incentivise couples to share: 6-month premium extension on wedding story submission. | Subscriptions and payments | implemented |
| 232 | Phase 3: Network Effect Lock-In | General / strategy / context | n/a |
| 233 | Community density > 1,000 profiles per community creates self-sustaining matching. Once achieved, switching cost (Vivah Score™ history, connections, messages) makes users sticky. | Realtime chat | implemented |
| 234 | Counter-Intuitive Truth: | General / strategy / context | n/a |
| 235 | Do NOT try to serve all communities at launch. BharatMatrimony launched with Tamil community only. Shaadi.com launched with Pakistani community only. Depth in one community > shallow presence in all communities. | Realtime chat | implemented |
| 236 | E3. Competitive Switch Strategy — How to Pull Users from Shaadi/BM | General / strategy / context | n/a |
| 237 | Features alone do not move users. Network effects lock people in. These are the only proven mechanisms to break incumbents: | General / strategy / context | n/a |
| 238 | Pain-Point Targeting: Survey churned BharatMatrimony / Shaadi users. Top 3 complaints in 2024: 'Fake profiles everywhere', 'Aggressive sales calls', 'Paid features locked behind 5,000/month wall'. Vivah.App solves all three — market that specific pain. | General / strategy / context | n/a |
| 239 | Free Profile Import: Let users paste their Shaadi.com/BharatMatrimony profile URL. AI scrapes public data, pre-fills Vivah profile. Reduces registration friction to < 60 seconds. | General / strategy / context | n/a |
| 240 | 'Bring Your Match' Referral: If a matched pair on another platform both sign up on Vivah within 7 days using referral link, both get 3 months premium free. Incentivises couples to migrate together. | Subscriptions and payments | implemented |
| 241 | Influencer Strategy: Partner with marriage counsellors, astrologers, community leaders on YouTube/Instagram. They have trusted audiences already seeking matches. Revenue share on subscriptions they drive. | Subscriptions and payments | implemented |
| 242 | WhatsApp Community Strategy: Create 'Vivah Verified Matches — [Community] [City]' WhatsApp communities. Admin-posted weekly match highlights (anonymised). Drives word-of-mouth. | Admin and analytics | implemented |
| 243 | F. SEO Architecture — Organic Traffic Engine | General / strategy / context | n/a |
| 244 | Entirely Missed in Both Documents: | General / strategy / context | n/a |
| 245 | SEO is the single highest-ROI acquisition channel for matrimony platforms. BharatMatrimony gets 15M+ monthly organic visits. Every profile page and search result page is a Google-indexable asset generating free traffic for years. | Search and filter matching | implemented |
| 246 | F1. SEO URL Architecture | General / strategy / context | n/a |
| 247 | Page Type | General / strategy / context | n/a |
| 248 | URL Pattern + SEO Strategy | General / strategy / context | n/a |
| 249 | Community Listing | General / strategy / context | n/a |
| 250 | /matrimony/tamil-brahmin-brides — H1: 'Tamil Brahmin Brides on Vivah.App \| Verified Profiles'. Static-generated on Cloudflare Pages. Updates daily. | General / strategy / context | n/a |
| 251 | City + Community | General / strategy / context | n/a |
| 252 | /matrimony/tamil-brahmin-chennai — High intent long-tail. 'Tamil Brahmin Brides in Chennai \| 1,200+ Verified Profiles' | General / strategy / context | n/a |
| 253 | Individual Profile | General / strategy / context | n/a |
| 254 | /profile/{slug} — Example: /profile/software-engineer-chennai-1992 — Server-rendered. Schema.org Person markup. Open Graph for WhatsApp share preview. | General / strategy / context | n/a |
| 255 | Search Results | Search and filter matching | implemented |
| 256 | /search?religion=hindu&caste=brahmin&city=chennai — Canonical URL. Robots: index only popular filter combos. Noindex rare combos (thin content penalty). | Search and filter matching | implemented |
| 257 | Success Stories | General / strategy / context | n/a |
| 258 | /weddings — User-generated content goldmine. Each story = long-form SEO content. 'How Priya and Karthik Found Each Other on Vivah.App in Chennai' | General / strategy / context | n/a |
| 259 | Blog / Advice | General / strategy / context | n/a |
| 260 | /blog — Matrimony tips, community wedding traditions, Kundali guides. Targets 'How to write matrimony bio', 'What is Manglik dosha' queries. | General / strategy / context | n/a |
| 261 | F2. Technical SEO Implementation | General / strategy / context | n/a |
| 262 | Next.js App Router with generateMetadata() per route — dynamic title + description from profile data | General / strategy / context | n/a |
| 263 | Schema.org: Person markup on profile pages (name, jobTitle, location, image). Eligible for Google Rich Results. | General / strategy / context | n/a |
| 264 | Sitemap.xml: Auto-generated by Cloudflare Pages build. Includes all public profiles + community pages. Submitted to GSC. | General / strategy / context | n/a |
| 265 | robots.txt: Crawl public profiles, community pages, blog. Noindex: chat, settings, admin, private profiles. | Realtime chat | implemented |
| 266 | Core Web Vitals: LCP < 2.5s (Cloudflare CDN + WebP images). CLS = 0 (reserve image dimensions in HTML). FID < 100ms (minimal JS on server-rendered pages). | General / strategy / context | n/a |
| 267 | Internal linking: Each profile page links to its community listing page. Each community page links to top 10 profiles. PageRank flows down the pyramid. | General / strategy / context | n/a |
| 268 | Hreflang: /hi/, /ta/, /te/ subpaths for Hindi, Tamil, Telugu — separate sitemaps per language. Captures vernacular search traffic. | Search and filter matching | implemented |
| 269 | G. Payment Security + Content Moderation Gaps | Subscriptions and payments | implemented |
| 270 | G1. Razorpay Webhook — Mandatory Security (Missed in Blueprint) | Subscriptions and payments | implemented |
| 271 | Security Vulnerability: | General / strategy / context | n/a |
| 272 | Blueprint v1 integrated Razorpay but omitted webhook signature verification. Without this, any attacker can POST a fake payment.captured event to your endpoint and receive a free subscription. This is not a theoretical risk — it is actively exploited. | Verification/safety/reporting | implemented |
| 273 | Correct Webhook Verification in Hono.js Worker: | Verification/safety/reporting | implemented |
| 274 | // In vivah-pay Worker | General / strategy / context | n/a |
| 275 | import { createHmac } from 'node:crypto'; | General / strategy / context | n/a |
| 276 | const WEBHOOK_SECRET = env.RAZORPAY_WEBHOOK_SECRET; | Subscriptions and payments | implemented |
| 277 | const signature = req.headers.get('x-razorpay-signature'); | Subscriptions and payments | implemented |
| 278 | const body = await req.text(); | General / strategy / context | n/a |
| 279 | const expected = createHmac('sha256', WEBHOOK_SECRET) | General / strategy / context | n/a |
| 280 | .update(body).digest('hex'); | General / strategy / context | n/a |
| 281 | if (signature !== expected) return c.json({}, 401); | General / strategy / context | n/a |
| 282 | // Only now process the payment event | Subscriptions and payments | implemented |
| 283 | G2. OTP Cost Reality — Budget This Now | OTP authentication | implemented |
| 284 | Provider | General / strategy / context | n/a |
| 285 | Cost Per OTP (India) | OTP authentication | implemented |
| 286 | Monthly Cost at 1K Registrations | General / strategy / context | n/a |
| 287 | Fast2SMS (Promotional) | General / strategy / context | n/a |
| 288 | Fast2SMS (Transactional) | General / strategy / context | n/a |
| 289 | MSG91 | General / strategy / context | n/a |
| 290 | Twilio (India) | General / strategy / context | n/a |
| 291 | WhatsApp Business API | General / strategy / context | n/a |
| 292 | ₹0.58 (Utility category) | General / strategy / context | n/a |
| 293 | ₹580 (hybrid fallback) | General / strategy / context | n/a |
| 294 | Strategy: | General / strategy / context | n/a |
| 295 | Primary: Fast2SMS Transactional (cheapest, good delivery). Fallback: WhatsApp OTP via Interakt/Wati if SMS fails. Dual-channel eliminates OTP delivery failures that cause registration drop-off. Total budget: ₹200–₹400/month per 1,000 new users. | OTP authentication | implemented |
| 296 | G3. Content Moderation — Solo Founder Workflow | Admin and analytics | implemented |
| 297 | Reality Check: | General / strategy / context | n/a |
| 298 | At launch you are the only moderator. Design the system so 95% of decisions are automated and only the hardest 5% require your eyes. Target: < 30 minutes/day on moderation at 10,000 users. | Admin and analytics | implemented |
| 299 | Content Type | General / strategy / context | n/a |
| 300 | Auto-Decision Rules (No Human Needed) | General / strategy / context | n/a |
| 301 | Profile Photos | General / strategy / context | n/a |
| 302 | Workers AI NSFW score > 0.85 → AUTO-REJECT + notify user. Score 0.60–0.85 → human queue. Score < 0.60 → AUTO-APPROVE. | General / strategy / context | n/a |
| 303 | Face Verification | Verification/safety/reporting | implemented |
| 304 | Cosine similarity vs existing profiles > 0.92 → AUTO-HOLD for duplicate review. < 0.92 → AUTO-PASS. | General / strategy / context | n/a |
| 305 | Bio Text | General / strategy / context | n/a |
| 306 | Profanity filter match (list in KV) → AUTO-REJECT field. Contact info pattern (phone regex) → AUTO-STRIP. Clean → AUTO-APPROVE. | Search and filter matching | implemented |
| 307 | Chat Messages | Realtime chat | implemented |
| 308 | Phone number regex match → MESSAGE BLOCKED + warning. Scam AI score > 0.80 → MESSAGE HELD + account flagged. < 0.80 → DELIVERED. | Realtime chat | implemented |
| 309 | Reports Filed | Verification/safety/reporting | implemented |
| 310 | Same account reported by 3+ unique users in 7 days → AUTO-SUSPEND pending review. Single report → queue for review. | Verification/safety/reporting | implemented |
| 311 | New Accounts | General / strategy / context | n/a |
| 312 | Device fingerprint seen before (new account) → soft-flag, monitor for 48 hours. Clean device → AUTO-ACTIVATE. | General / strategy / context | n/a |
| 313 | Human Review Queue (Admin Panel — Minimum Viable): | Admin and analytics | implemented |
| 314 | Daily: Check suspension queue (auto-suspended accounts). Takes 5–10 min. | General / strategy / context | n/a |
| 315 | Daily: Review photo moderation borderline cases (0.60–0.85 NSFW score). | Admin and analytics | implemented |
| 316 | Weekly: Audit report queue. Investigate patterns. | Verification/safety/reporting | implemented |
| 317 | On-alert: Sentry error spike, unusual transaction volume, >5 reports on one account in 1 hour. | Verification/safety/reporting | implemented |
| 318 | H. Observability, Testing & Error Handling | General / strategy / context | n/a |
| 319 | H1. Minimum Viable Observability Stack | General / strategy / context | n/a |
| 320 | Tool | General / strategy / context | n/a |
| 321 | Setup + What to Track | General / strategy / context | n/a |
| 322 | Sentry (Free) | General / strategy / context | n/a |
| 323 | Install @sentry/cloudflare SDK in every Worker. Track: unhandled exceptions, Worker CPU time > 25ms, D1 query errors. Alert via Telegram bot (free) for P0 errors. | General / strategy / context | n/a |
| 324 | Cloudflare Analytics Engine | Admin and analytics | implemented |
| 325 | Log custom events from Workers. Key events: user_registered, interest_sent, match_viewed, payment_initiated, payment_failed, report_filed, fake_detected. Query via CF GraphQL API. | Interests/shortlist flow | implemented |
| 326 | PostHog (1M events/month free) | General / strategy / context | n/a |
| 327 | Frontend analytics: funnel analysis (registration to first interest), session recordings on key flows, feature flag control, A/B test framework. | Interests/shortlist flow | implemented |
| 328 | Cloudflare Dashboard | Admin and analytics | implemented |
| 329 | Built-in: Worker request volume, error rate, CPU time percentiles, D1 query count. Set alerts on: error rate > 1%, CPU P99 > 25ms. | General / strategy / context | n/a |
| 330 | Uptime monitoring | General / strategy / context | n/a |
| 331 | Better Uptime (free tier) or UptimeRobot: ping /api/health every 60 seconds. Alert via WhatsApp/Telegram on downtime. | General / strategy / context | n/a |
| 332 | H2. Analytics Engine Event Schema — Log These from Day 1 | Admin and analytics | implemented |
| 333 | Define your event schema before writing any Worker code. Events you don't log at launch are data you'll never recover. | General / strategy / context | n/a |
| 334 | Event Name | General / strategy / context | n/a |
| 335 | Properties | General / strategy / context | n/a |
| 336 | Why Critical | General / strategy / context | n/a |
| 337 | user_registered | General / strategy / context | n/a |
| 338 | { source, community, gender, city } | General / strategy / context | n/a |
| 339 | Understand acquisition by source and community density | General / strategy / context | n/a |
| 340 | profile_completed | General / strategy / context | n/a |
| 341 | { completion_pct, missing_fields[] } | General / strategy / context | n/a |
| 342 | Identify which fields cause drop-off | General / strategy / context | n/a |
| 343 | match_viewed | General / strategy / context | n/a |
| 344 | { viewer_id, target_community, duration_ms } | General / strategy / context | n/a |
| 345 | Measure match quality by dwell time | General / strategy / context | n/a |
| 346 | interest_sent | Interests/shortlist flow | implemented |
| 347 | { sender_plan, match_score_band } | Subscriptions and payments | implemented |
| 348 | Correlation: plan type vs interest activity | Interests/shortlist flow | implemented |
| 349 | interest_accepted | Interests/shortlist flow | implemented |
| 350 | { time_to_accept_hrs, compatibility_score } | Search and filter matching | implemented |
| 351 | Measure match algorithm quality | General / strategy / context | n/a |
| 352 | message_sent | Realtime chat | implemented |
| 353 | { conn_id, type, is_first_message } | Realtime chat | implemented |
| 354 | Track conversation depth | General / strategy / context | n/a |
| 355 | payment_initiated | Subscriptions and payments | implemented |
| 356 | { plan, amount, source_screen } | Subscriptions and payments | implemented |
| 357 | Attribution: which screen drives conversions | General / strategy / context | n/a |
| 358 | payment_completed | Subscriptions and payments | implemented |
| 359 | { plan, gateway, method } | Subscriptions and payments | implemented |
| 360 | Revenue tracking | General / strategy / context | n/a |
| 361 | fake_detected | General / strategy / context | n/a |
| 362 | { signal_type, action_taken } | General / strategy / context | n/a |
| 363 | Trust & Safety KPI | Verification/safety/reporting | implemented |
| 364 | feature_flag_exposure | General / strategy / context | n/a |
| 365 | { flag_name, variant, user_plan } | Subscriptions and payments | implemented |
| 366 | A/B test tracking | General / strategy / context | n/a |
| 367 | H3. Testing Strategy — What to Test Before Launch | General / strategy / context | n/a |
| 368 | Test Type | General / strategy / context | n/a |
| 369 | Scope + Tool | General / strategy / context | n/a |
| 370 | Unit Tests (Vitest) | General / strategy / context | n/a |
| 371 | Kundali engine (deterministic: assert specific Gun scores for known birth data). OTP hash verification. JWT mint/verify/revoke cycle. Matching algorithm score computation. | OTP authentication | implemented |
| 372 | Integration Tests | General / strategy / context | n/a |
| 373 | D1 CRUD for all 20 tables. KV read/write/TTL expiry. R2 upload + presigned URL generation. Razorpay webhook signature verification. | Verification/safety/reporting | implemented |
| 374 | E2E Tests (Playwright) | General / strategy / context | n/a |
| 375 | Critical paths only: Register → Verify OTP → Create Profile → Upload Photo → View Match → Send Interest → Accept Interest → Send Message. Run on every PR merge. | OTP authentication | implemented |
| 376 | Load Test (k6 free) | General / strategy / context | n/a |
| 377 | Before launch: simulate 500 concurrent users on /matches/daily endpoint. Assert P99 < 500ms. Reveals D1 query bottlenecks before users hit them. | General / strategy / context | n/a |
| 378 | Security Test (OWASP ZAP) | General / strategy / context | n/a |
| 379 | Run automated scan against staging URL. Fix all High severity findings before launch. Medium: fix within 2 weeks post-launch. | General / strategy / context | n/a |
| 380 | I. Pricing Decision Framework — When and What | General / strategy / context | n/a |
| 381 | Both documents deferred pricing. 'Decide later' is not a strategy — it is a risk. Here is the exact data-driven framework for when to turn on pricing and what to charge. | General / strategy / context | n/a |
| 382 | I1. The Pricing Decision Trigger | General / strategy / context | n/a |
| 383 | Metric | General / strategy / context | n/a |
| 384 | Threshold That Triggers Pricing Switch-On | General / strategy / context | n/a |
| 385 | Profile Volume | General / strategy / context | n/a |
| 386 | Minimum 500 verified profiles per community before charging. Below this, users legitimately cannot get value — charging is unethical and will kill word-of-mouth. | General / strategy / context | n/a |
| 387 | Match Quality | General / strategy / context | n/a |
| 388 | Average compatibility score of shown matches > 65% for > 60% of active users. Measure via Analytics Engine. | Search and filter matching | implemented |
| 389 | Interest Acceptance Rate | Interests/shortlist flow | implemented |
| 390 | Platform-wide interest acceptance rate > 15%. Signals that matches are relevant and users see value. | Interests/shortlist flow | implemented |
| 391 | 7-Day Retention | General / strategy / context | n/a |
| 392 | > 35% of registered users return after 7 days. Below this, you have a product problem, not a pricing problem. | General / strategy / context | n/a |
| 393 | Organic Demand Signal | General / strategy / context | n/a |
| 394 | Users asking 'how do I get more matches?' or 'how do I see who viewed me?' in support — these are requests to be upsold, not frustrated users. | General / strategy / context | n/a |
| 395 | Rule of Thumb: | General / strategy / context | n/a |
| 396 | Run completely free for the first 3 months minimum. Use this time to achieve community density, fix product bugs, and collect conversion intent data. Flip pricing switch when all 5 triggers above are met simultaneously. | General / strategy / context | n/a |
| 397 | I2. Recommended Pricing Model | General / strategy / context | n/a |
| 398 | Plan | Subscriptions and payments | implemented |
| 399 | Price (INR) | General / strategy / context | n/a |
| 400 | What Unlocks | General / strategy / context | n/a |
| 401 | Spark (Free Forever) | General / strategy / context | n/a |
| 402 | 5 interests/day, 10 daily matches, text chat only, blurred photos | Interests/shortlist flow | implemented |
| 403 | Connect (Monthly) | General / strategy / context | n/a |
| 404 | ₹599/month | General / strategy / context | n/a |
| 405 | Unlimited interests, 50 daily matches, full photos, voice messages, see who viewed | Interests/shortlist flow | implemented |
| 406 | Vivah Gold (Quarterly) | General / strategy / context | n/a |
| 407 | ₹1,299/quarter (₹433/mo) | General / strategy / context | n/a |
| 408 | All Connect + video calling, advanced filters, Kundali Pro, priority in search | Search and filter matching | implemented |
| 409 | Vivah Platinum (Annual) | General / strategy / context | n/a |
| 410 | ₹3,599/year (₹300/mo) | General / strategy / context | n/a |
| 411 | All Gold + anonymous browse, profile boost weekly, AI compatibility report monthly | Search and filter matching | implemented |
| 412 | À La Carte: Profile Boost ₹99 (2 hours). Extra Kundali report ₹149. Vivah Verified™ badge ₹299 one-time. | Verification/safety/reporting | implemented |
| 413 | Pricing Psychology: Quarterly plan anchors value. Annual plan is the hero (shown first, best per-month). Monthly is the tryout. | Subscriptions and payments | implemented |
| 414 | A/B Test: Launch with two price points simultaneously via PostHog feature flags. ₹599 vs ₹799 for Connect. Run for 30 days. Pick winner. | General / strategy / context | n/a |
| 415 | I3. Revenue Projection — Conservative Case | General / strategy / context | n/a |
| 416 | Month | General / strategy / context | n/a |
| 417 | Active Users | General / strategy / context | n/a |
| 418 | Revenue | General / strategy / context | n/a |
| 419 | Month 1–3 (Free) | General / strategy / context | n/a |
| 420 | 0 → 5,000 registered | General / strategy / context | n/a |
| 421 | ₹0 (intentional — building density) | General / strategy / context | n/a |
| 422 | Month 4 (Soft Launch Pricing) | General / strategy / context | n/a |
| 423 | 5,000 registered, 500 DAU | General / strategy / context | n/a |
| 424 | ₹29,950 (5% of DAU × ₹599 avg) | General / strategy / context | n/a |
| 425 | Month 6 | General / strategy / context | n/a |
| 426 | 15,000 registered, 1,500 DAU | General / strategy / context | n/a |
| 427 | ₹89,850/month | General / strategy / context | n/a |
| 428 | Month 12 | General / strategy / context | n/a |
| 429 | 50,000 registered, 5,000 DAU | General / strategy / context | n/a |
| 430 | ₹2.99L/month (~₹36L/year) | General / strategy / context | n/a |
| 431 | Month 18 (Scale) | General / strategy / context | n/a |
| 432 | 2L registered, 20,000 DAU | General / strategy / context | n/a |
| 433 | ₹11.9L/month — Series A ready | General / strategy / context | n/a |
| 434 | J. 100 / 100 Pre-Launch Readiness Checklist | General / strategy / context | n/a |
| 435 | This checklist aggregates every critical item across all documents. Nothing ships until every box is checked. | General / strategy / context | n/a |
| 436 | Legal & Business | General / strategy / context | n/a |
| 437 | Private Limited Company registered (MCA21) | General / strategy / context | n/a |
| 438 | GST number obtained | General / strategy / context | n/a |
| 439 | Business bank account opened | General / strategy / context | n/a |
| 440 | Razorpay account activated (test mode) | Subscriptions and payments | implemented |
| 441 | 'VIVAH' trademark application filed | General / strategy / context | n/a |
| 442 | Privacy Policy live at vivah.app/privacy (covers DPDP Act + GDPR) | Verification/safety/reporting | implemented |
| 443 | Terms of Service live at vivah.app/terms | General / strategy / context | n/a |
| 444 | Support email active: support@vivah.app | General / strategy / context | n/a |
| 445 | Infrastructure & Auth | General / strategy / context | n/a |
| 446 | Cloudflare account created, vivah.app domain added | General / strategy / context | n/a |
| 447 | Workers Paid plan activated ($5/month) | Subscriptions and payments | implemented |
| 448 | D1 databases created with location_hint = 'apac' | General / strategy / context | n/a |
| 449 | All 20 D1 tables created with indexes | General / strategy / context | n/a |
| 450 | KV namespaces created and bound in wrangler.toml | General / strategy / context | n/a |
| 451 | R2 bucket created with private access only | General / strategy / context | n/a |
| 452 | Better Auth configured with D1 adapter, OTP + social providers | OTP authentication | implemented |
| 453 | All secrets in Cloudflare Secrets (never in wrangler.toml) | General / strategy / context | n/a |
| 454 | Core Features (MVP) | General / strategy / context | n/a |
| 455 | OTP registration flow (SMS primary + WhatsApp fallback) | OTP authentication | implemented |
| 456 | Profile creation with photo upload to R2 | Onboarding/profile wizard | implemented |
| 457 | Workers AI NSFW moderation on photo upload | Admin and analytics | implemented |
| 458 | Basic matching algorithm (rule-based v1) running | General / strategy / context | n/a |
| 459 | Interest send / accept / decline flow | Interests/shortlist flow | implemented |
| 460 | In-app messaging via Durable Objects WebSocket | General / strategy / context | n/a |
| 461 | Push notifications via FCM + APNS | Notifications | implemented |
| 462 | Block and report user | Verification/safety/reporting | implemented |
| 463 | Account deletion flow (in-app, not just email) | General / strategy / context | n/a |
| 464 | Security Checklist | General / strategy / context | n/a |
| 465 | Razorpay webhook HMAC-SHA256 verification implemented | Verification/safety/reporting | implemented |
| 466 | All API endpoints have Zod input validation | General / strategy / context | n/a |
| 467 | Rate limiting active on all auth endpoints (5/15min) | General / strategy / context | n/a |
| 468 | CORS allowlist configured (no wildcard) | General / strategy / context | n/a |
| 469 | CSP headers set on all Pages responses | General / strategy / context | n/a |
| 470 | OTPs stored as hashed values only (never plaintext) | General / strategy / context | n/a |
| 471 | Device fingerprint bound to JWT sessions | General / strategy / context | n/a |
| 472 | R2 photos served via presigned URLs only (15-min TTL) | General / strategy / context | n/a |
| 473 | App Store | General / strategy / context | n/a |
| 474 | Google Play: Safety section declared (report, block, human review, 18+ policy) | Verification/safety/reporting | implemented |
| 475 | Apple App Store: Apple Sign-In implemented | General / strategy / context | n/a |
| 476 | In-app account deletion feature live | General / strategy / context | n/a |
| 477 | In-app report mechanism live | Verification/safety/reporting | implemented |
| 478 | TestFlight build submitted 4 weeks before launch | General / strategy / context | n/a |
| 479 | App content rating declared correctly (17+ / Adult) | General / strategy / context | n/a |
| 480 | Observability | General / strategy / context | n/a |
| 481 | Sentry SDK installed in all Workers | General / strategy / context | n/a |
| 482 | Analytics Engine events logging all 11 defined event types | Admin and analytics | implemented |
| 483 | PostHog frontend analytics active | Admin and analytics | implemented |
| 484 | Uptime monitor pinging /api/health every 60 seconds | General / strategy / context | n/a |
| 485 | Cloudflare error rate alert set (> 1% triggers Telegram alert) | General / strategy / context | n/a |
| 486 | Pre-Launch Community | General / strategy / context | n/a |
| 487 | 250 male + 250 female seed profiles in target community (Tamil Brahmin, Chennai OR chosen community) | General / strategy / context | n/a |
| 488 | All seed profiles verified (mobile OTP minimum) | OTP authentication | implemented |
| 489 | Matching algorithm tested — seed users receive relevant matches | General / strategy / context | n/a |
| 490 | Vivah Circles™ WhatsApp group created for seed community | General / strategy / context | n/a |
| 491 | One partner influencer committed for launch week post | General / strategy / context | n/a |
| 492 | 17 gaps identified. 17 gaps closed. You are now 100 / 100 ready. | General / strategy / context | n/a |
| 493 | © 2026 Vivah.App — Founder Confidential — Addendum v1.0 | General / strategy / context | n/a |

### VivahApp_Ultimate_Blueprint.docx

| # | Extracted line | Classified feature | Current status |
|---:|---|---|---|
| 1 | VIVAH.APP | General / strategy / context | n/a |
| 2 | India's First Edge-Native, Zero-Cost, Billion-Dollar Matrimony Platform | General / strategy / context | n/a |
| 3 | ULTIMATE TECHNICAL BLUEPRINT | General / strategy / context | n/a |
| 4 | God-Tier Architecture • Zero ₹ Infrastructure • Billion-Dollar Execution | General / strategy / context | n/a |
| 5 | Cloudflare Edge Stack • World-Class Security • Competitor-Crushing Features | General / strategy / context | n/a |
| 6 | Version 1.0 — March 2026 \| CONFIDENTIAL — FOUNDER ONLY | General / strategy / context | n/a |
| 7 | Table of Contents | General / strategy / context | n/a |
| 8 | 01. Vision, Mission & Billion-Dollar Thesis | General / strategy / context | n/a |
| 9 | 02. God-Tier Feature Matrix — Beating Every Competitor | General / strategy / context | n/a |
| 10 | 03. Zero-Cost Cloudflare Infrastructure Blueprint | General / strategy / context | n/a |
| 11 | 04. Fire & Forget Deployment Architecture | General / strategy / context | n/a |
| 12 | 05. Edge-Native Backend Design | General / strategy / context | n/a |
| 13 | 06. Database Architecture on D1 + KV + R2 | General / strategy / context | n/a |
| 14 | 07. Real-Time Engine — Durable Objects | General / strategy / context | n/a |
| 15 | 08. Security Architecture — Unbreakable | General / strategy / context | n/a |
| 16 | 09. AI & Smart Matchmaking Engine | Search and filter matching | implemented |
| 17 | 10. Performance: Thousands of Users, Zero Lag | General / strategy / context | n/a |
| 18 | 11. Scale Path: ₹0 → $5 → $500 → IPO | General / strategy / context | n/a |
| 19 | 01 Vision, Mission & Billion-Dollar Thesis | General / strategy / context | n/a |
| 20 | Mission: | General / strategy / context | n/a |
| 21 | Build the most technologically advanced, trust-first, community-inclusive Indian matrimony platform — accessible to every Indian, anywhere on Earth — at zero infrastructure cost to the founder until profit is proven. | General / strategy / context | n/a |
| 22 | 1.1 The Opportunity Gap | General / strategy / context | n/a |
| 23 | India conducts 10+ million marriages annually. The online matrimony market is projected to reach ₹7,000 Cr by 2027, yet the top platforms — BharatMatrimony, Shaadi.com, Jeevansathi — all suffer from the same five fatal flaws: | General / strategy / context | n/a |
| 24 | Bloated, legacy Java/.NET backends with slow, server-rendered UIs | General / strategy / context | n/a |
| 25 | Aggressive paywalls that block basic features and destroy user trust | Verification/safety/reporting | implemented |
| 26 | Poor mobile UX — apps that feel like 2015 | General / strategy / context | n/a |
| 27 | Zero AI-native intelligence — keyword search, not smart matching | Search and filter matching | implemented |
| 28 | Privacy theatre — profiles exposed without real consent architecture | Verification/safety/reporting | implemented |
| 29 | The Opening: | General / strategy / context | n/a |
| 30 | A single founder with a $0 infra budget, leveraging Cloudflare's global edge network, can outperform all of them on every technical dimension from Day 1. | General / strategy / context | n/a |
| 31 | 1.2 The Billion-Dollar Thesis | General / strategy / context | n/a |
| 32 | Dimension | General / strategy / context | n/a |
| 33 | Why Vivah.App Wins | General / strategy / context | n/a |
| 34 | Distribution | General / strategy / context | n/a |
| 35 | Cloudflare's 300+ PoPs = sub-20ms globally. No competitor has edge-native latency. | General / strategy / context | n/a |
| 36 | Cost Structure | General / strategy / context | n/a |
| 37 | ₹0 infra cost until 10M+ requests/month. Competitors spend ₹50L+/month on AWS. | General / strategy / context | n/a |
| 38 | Feature Depth | General / strategy / context | n/a |
| 39 | Every feature from every competitor, unified, plus 15 never-seen-before features. | General / strategy / context | n/a |
| 40 | Trust Engine | General / strategy / context | n/a |
| 41 | Multi-layer AI verification that makes fake profiles structurally impossible at scale. | Verification/safety/reporting | implemented |
| 42 | AI-Native | General / strategy / context | n/a |
| 43 | Workers AI + fine-tuned models running at the edge — no model API latency. | General / strategy / context | n/a |
| 44 | Moat | General / strategy / context | n/a |
| 45 | Network effects + community data + Kundali AI = impossible to replicate in < 3 years. | General / strategy / context | n/a |
| 46 | 02 God-Tier Feature Matrix — Beating Every Competitor | General / strategy / context | n/a |
| 47 | Principle: | General / strategy / context | n/a |
| 48 | Every feature listed below is either FREE on Cloudflare or deployable at zero marginal cost. Pricing and auth are decided post-launch. Features ship first. | General / strategy / context | n/a |
| 49 | 2.1 Profile & Registration Features | General / strategy / context | n/a |
| 50 | Feature | General / strategy / context | n/a |
| 51 | Shaadi / BM | General / strategy / context | n/a |
| 52 | Jeevansathi | General / strategy / context | n/a |
| 53 | Vivah.App ✦ | General / strategy / context | n/a |
| 54 | Mobile OTP Registration | OTP authentication | implemented |
| 55 | ✓ + WhatsApp OTP | OTP authentication | implemented |
| 56 | Social Sign-In | General / strategy / context | n/a |
| 57 | Google only | General / strategy / context | n/a |
| 58 | Google + FB | General / strategy / context | n/a |
| 59 | Google + FB + Apple + LinkedIn | General / strategy / context | n/a |
| 60 | Profile Completeness AI | General / strategy / context | n/a |
| 61 | Score only | General / strategy / context | n/a |
| 62 | AI Coach with voice nudges | General / strategy / context | n/a |
| 63 | Photo Gallery | General / strategy / context | n/a |
| 64 | 10 photos | General / strategy / context | n/a |
| 65 | 12 photos | General / strategy / context | n/a |
| 66 | 25 photos + AI beauty retouch toggle | General / strategy / context | n/a |
| 67 | Bio-Data PDF | General / strategy / context | n/a |
| 68 | Basic | General / strategy / context | n/a |
| 69 | Branded PDF with QR code + share link | General / strategy / context | n/a |
| 70 | AI Profile Writer | General / strategy / context | n/a |
| 71 | ✓ — 1-click AI About Me generation | General / strategy / context | n/a |
| 72 | Video Introduction | General / strategy / context | n/a |
| 73 | ✓ — 60-sec in-app video profile | General / strategy / context | n/a |
| 74 | Voice Bio | General / strategy / context | n/a |
| 75 | ✓ — 30-sec voice introduction | General / strategy / context | n/a |
| 76 | Multilingual Profile | General / strategy / context | n/a |
| 77 | Hindi + English | General / strategy / context | n/a |
| 78 | 22 Indian languages supported | General / strategy / context | n/a |
| 79 | Family Co-Access Login | General / strategy / context | n/a |
| 80 | Granular read/approve roles per family member | General / strategy / context | n/a |
| 81 | Dark Mode App | General / strategy / context | n/a |
| 82 | Partial | General / strategy / context | n/a |
| 83 | ✓ Full adaptive dark/light | General / strategy / context | n/a |
| 84 | 2.2 Matchmaking & Discovery Features | Search and filter matching | implemented |
| 85 | Daily Recommendations | General / strategy / context | n/a |
| 86 | Rule-based | General / strategy / context | n/a |
| 87 | Hybrid ML + Collaborative Filter | Search and filter matching | implemented |
| 88 | Filter Depth | Search and filter matching | implemented |
| 89 | 12 filters | Search and filter matching | implemented |
| 90 | 15 filters | Search and filter matching | implemented |
| 91 | 40+ filters including personality type | Search and filter matching | implemented |
| 92 | Kundali Matching | General / strategy / context | n/a |
| 93 | Manual upload | General / strategy / context | n/a |
| 94 | AI Kundali — auto-generated + 36 Gun | General / strategy / context | n/a |
| 95 | NRI / Diaspora Search | Search and filter matching | implemented |
| 96 | ✓ + Timezone-aware scheduling | General / strategy / context | n/a |
| 97 | 'Near Me' GPS Search | Search and filter matching | implemented |
| 98 | ✓ Real-time GPS radius matching | General / strategy / context | n/a |
| 99 | Personality Match | General / strategy / context | n/a |
| 100 | ✓ 16-personality type compatibility | Search and filter matching | implemented |
| 101 | Lifestyle Score | General / strategy / context | n/a |
| 102 | ✓ Diet + fitness + values alignment | General / strategy / context | n/a |
| 103 | Reverse Compatibility | Search and filter matching | implemented |
| 104 | ✓ See who YOU match for them | General / strategy / context | n/a |
| 105 | AI Conversation Starter | General / strategy / context | n/a |
| 106 | ✓ Context-aware first message AI | Realtime chat | implemented |
| 107 | Mutual Friends Match | General / strategy / context | n/a |
| 108 | ✓ LinkedIn graph overlap detection | General / strategy / context | n/a |
| 109 | Compatibility Report | Search and filter matching | implemented |
| 110 | 12-page AI-generated PDF report | Verification/safety/reporting | implemented |
| 111 | 2.3 Communication Features | General / strategy / context | n/a |
| 112 | In-App Messaging | General / strategy / context | n/a |
| 113 | ✓ (paywalled) | General / strategy / context | n/a |
| 114 | ✓ Free for all matches | General / strategy / context | n/a |
| 115 | Voice Messages | Realtime chat | implemented |
| 116 | ✓ Up to 10 min, edge-stored | General / strategy / context | n/a |
| 117 | Video Calling | Voice/video calls | implemented |
| 118 | ✓ WebRTC via Durable Objects | Voice/video calls | implemented |
| 119 | Group Family Call | General / strategy / context | n/a |
| 120 | ✓ Up to 10 participants | General / strategy / context | n/a |
| 121 | Message Translation | Realtime chat | implemented |
| 122 | ✓ Real-time 22-language translation | General / strategy / context | n/a |
| 123 | Typing Indicator | General / strategy / context | n/a |
| 124 | ✓ + read receipts + delivery status | General / strategy / context | n/a |
| 125 | Message Reactions | Realtime chat | implemented |
| 126 | ✓ Full emoji reaction layer | General / strategy / context | n/a |
| 127 | Voice-to-Text Chat | Realtime chat | implemented |
| 128 | ✓ Whisper AI transcription at edge | General / strategy / context | n/a |
| 129 | Schedule Video Date | General / strategy / context | n/a |
| 130 | ✓ Calendar invite + reminders | General / strategy / context | n/a |
| 131 | E2E Encryption | General / strategy / context | n/a |
| 132 | ✓ AES-256-GCM, keys never on server | General / strategy / context | n/a |
| 133 | Anti-Contact-Share AI | General / strategy / context | n/a |
| 134 | ✓ Real-time number masking in chat | Realtime chat | implemented |
| 135 | 2.4 Safety, Trust & Verification Features | Verification/safety/reporting | implemented |
| 136 | Aadhaar eKYC via UIDAI sandbox API — government-level identity proof | General / strategy / context | n/a |
| 137 | DigiLocker integration — degree, employment certificates pulled directly from government vault | General / strategy / context | n/a |
| 138 | Live selfie verification — AI face match against Aadhaar photo (Rekognition-equivalent using Workers AI) | Verification/safety/reporting | implemented |
| 139 | Duplicate face detection across entire database — structurally prevents multi-account fraud | General / strategy / context | n/a |
| 140 | Photo reverse-search — detects stock images, celebrity photos, stolen pictures | Search and filter matching | implemented |
| 141 | Device fingerprinting — canvas + WebGL + audio fingerprint prevents multi-device abuse | General / strategy / context | n/a |
| 142 | Criminal background check API (AuthBridge) — Premium tier opt-in | Subscriptions and payments | implemented |
| 143 | Employment verification — company email ping or Juspay/Springverify API | Verification/safety/reporting | implemented |
| 144 | Emergency SOS — shares GPS location with trusted contact during a date | General / strategy / context | n/a |
| 145 | Scam sentence detector — AI flags scripts commonly used by romance scammers | General / strategy / context | n/a |
| 146 | Photo watermarking — invisible steganographic watermark to trace screenshot leaks | General / strategy / context | n/a |
| 147 | Zero-knowledge contact reveal — phone number shown only after both parties explicitly agree | General / strategy / context | n/a |
| 148 | 2.5 World-Exclusive Features — Never Seen Before | General / strategy / context | n/a |
| 149 | Competitive Moat: | General / strategy / context | n/a |
| 150 | These 10 features do not exist on any Indian matrimony platform today. Shipping even 3 of them creates an immediate viral moment. | General / strategy / context | n/a |
| 151 | Exclusive Feature | General / strategy / context | n/a |
| 152 | Description | General / strategy / context | n/a |
| 153 | Vivah Score™ | General / strategy / context | n/a |
| 154 | Proprietary 100-point trust + compatibility score combining verification level, profile completeness, match history quality, and AI personality analysis. Shown prominently on every profile card. | Search and filter matching | implemented |
| 155 | Kundali AI Pro™ | General / strategy / context | n/a |
| 156 | Real-time Vedic astrology engine — auto-generate chart from DOB + time + place. 36-gun Milan, Manglik analysis, muhurat suggestions, remedial stones — all in natural language via Workers AI LLM. | General / strategy / context | n/a |
| 157 | Voice Personality Match | General / strategy / context | n/a |
| 158 | 30-second voice recording analysed by AI for speech patterns, language, tone, energy — match users by voice personality compatibility (groundbreaking feature, patentable). | Search and filter matching | implemented |
| 159 | Shadow Profile Mode | General / strategy / context | n/a |
| 160 | Ultra-privacy: user exists in matches algorithm but is invisible to direct search. Like a VIP lounge — they approach you, you never appear in anyone's feed uninvited. | Search and filter matching | implemented |
| 161 | Family Approval Workflow | General / strategy / context | n/a |
| 162 | Structured approval chain: User → Parent → Elder family member. Each stage has its own dashboard, can add notes, approve/reject. Tracks full family decision journey. | Admin and analytics | implemented |
| 163 | Shagun Board™ | General / strategy / context | n/a |
| 164 | Community celebration wall — couples who matched on Vivah share wedding photos. Massive social proof engine. Drives organic viral loops. | General / strategy / context | n/a |
| 165 | Vivah Circles™ | General / strategy / context | n/a |
| 166 | Private community groups by city + community — users find matches through trusted circles. Think WhatsApp groups but structured, moderated, and match-powered. | General / strategy / context | n/a |
| 167 | Match Timeline | General / strategy / context | n/a |
| 168 | Visual relationship timeline — interest sent → accepted → first call → family intro → engagement → wedding. Users track journey milestones. Creates emotional investment. | Interests/shortlist flow | implemented |
| 169 | AI Icebreaker Engine | General / strategy / context | n/a |
| 170 | After a match, AI analyses both profiles and generates 5 personalised conversation starters specific to shared interests, education, location overlap. Eliminates the blank-message problem. | Interests/shortlist flow | implemented |
| 171 | Vivah Verified™ Badge | General / strategy / context | n/a |
| 172 | Gold-tier badge earned only through Aadhaar + employment + education + face verification. Market this as 'The only matrimony badge that actually means something.' | Verification/safety/reporting | implemented |
| 173 | 03 Zero-Cost Cloudflare Infrastructure Blueprint | General / strategy / context | n/a |
| 174 | God-Tier Insight: | General / strategy / context | n/a |
| 175 | Cloudflare's free tier is not a toy — it is a production-grade, globally distributed infrastructure stack used by companies worth billions. The entire Vivah.App backend runs natively on it from Day 1. | General / strategy / context | n/a |
| 176 | 3.1 The Complete ₹0 Stack | General / strategy / context | n/a |
| 177 | Cloudflare Service | General / strategy / context | n/a |
| 178 | What It Gives You | General / strategy / context | n/a |
| 179 | Free Tier Limit | General / strategy / context | n/a |
| 180 | Workers | General / strategy / context | n/a |
| 181 | Serverless edge functions (backend API) | General / strategy / context | n/a |
| 182 | 100,000 req/day free | General / strategy / context | n/a |
| 183 | Pages | General / strategy / context | n/a |
| 184 | Full-stack app hosting (frontend + SSR) | General / strategy / context | n/a |
| 185 | Unlimited sites, unlimited bandwidth | General / strategy / context | n/a |
| 186 | SQLite database at the edge | General / strategy / context | n/a |
| 187 | 5M reads/day, 100K writes/day | General / strategy / context | n/a |
| 188 | Global key-value store (cache, sessions, OTPs) | General / strategy / context | n/a |
| 189 | 100K reads/day, 1K writes/day | General / strategy / context | n/a |
| 190 | Object storage (photos, docs, audio) | General / strategy / context | n/a |
| 191 | 10 GB storage, zero egress fees | General / strategy / context | n/a |
| 192 | Durable Objects | General / strategy / context | n/a |
| 193 | Stateful edge actors (WebSocket, real-time) | General / strategy / context | n/a |
| 194 | 1M requests/month | General / strategy / context | n/a |
| 195 | Workers AI | General / strategy / context | n/a |
| 196 | On-device LLM + vision + audio models | General / strategy / context | n/a |
| 197 | 10,000 neurons/day free | General / strategy / context | n/a |
| 198 | Vectorize | General / strategy / context | n/a |
| 199 | Vector database (semantic search, embeddings) | Search and filter matching | implemented |
| 200 | 30M vector dimensions free | General / strategy / context | n/a |
| 201 | Analytics Engine | Admin and analytics | implemented |
| 202 | Real-time event analytics, zero sampling | Admin and analytics | implemented |
| 203 | 1M events/day free | General / strategy / context | n/a |
| 204 | Turnstile | General / strategy / context | n/a |
| 205 | Bot protection (better than reCAPTCHA) | General / strategy / context | n/a |
| 206 | Unlimited, always free | General / strategy / context | n/a |
| 207 | Email Routing | General / strategy / context | n/a |
| 208 | Transactional email handling | General / strategy / context | n/a |
| 209 | 10,000 messages/day free | Realtime chat | implemented |
| 210 | Queues | General / strategy / context | n/a |
| 211 | Message queues for background jobs | Realtime chat | implemented |
| 212 | 1M messages/month free | Realtime chat | implemented |
| 213 | Hyperdrive | General / strategy / context | n/a |
| 214 | Database connection pooling accelerator | General / strategy / context | n/a |
| 215 | Free on paid Workers plan | Subscriptions and payments | implemented |
| 216 | Zero Trust | General / strategy / context | n/a |
| 217 | Access control, tunnels, identity proxy | General / strategy / context | n/a |
| 218 | 50 users free | General / strategy / context | n/a |
| 219 | Total Monthly Cost: | General / strategy / context | n/a |
| 220 | ₹0 (Zero Rupees). Zero. No credit card needed for free tier. The platform serves thousands of users per day before hitting any paid tier limit. | General / strategy / context | n/a |
| 221 | 3.2 Third-Party Services — Also Free Tier | General / strategy / context | n/a |
| 222 | Service | General / strategy / context | n/a |
| 223 | Purpose | General / strategy / context | n/a |
| 224 | Free Allowance | General / strategy / context | n/a |
| 225 | Resend.com | General / strategy / context | n/a |
| 226 | Transactional email (OTP, alerts, welcome) | OTP authentication | implemented |
| 227 | 3,000 emails/month free | General / strategy / context | n/a |
| 228 | Fast2SMS | General / strategy / context | n/a |
| 229 | Indian OTP SMS delivery | OTP authentication | implemented |
| 230 | ₹0 trial credits, cheapest INR rates | General / strategy / context | n/a |
| 231 | Turso (LibSQL) | General / strategy / context | n/a |
| 232 | Edge SQLite replica for global read speed | General / strategy / context | n/a |
| 233 | 500 DBs, 9GB free | General / strategy / context | n/a |
| 234 | Upstash Redis | General / strategy / context | n/a |
| 235 | Serverless Redis (rate limiting, pub/sub) | General / strategy / context | n/a |
| 236 | 10,000 commands/day free | General / strategy / context | n/a |
| 237 | Cloudinary | General / strategy / context | n/a |
| 238 | Image AI (crop, resize, NSFW detect) | General / strategy / context | n/a |
| 239 | 25 credits/month free | General / strategy / context | n/a |
| 240 | Sentry.io | General / strategy / context | n/a |
| 241 | Error tracking, performance monitoring | General / strategy / context | n/a |
| 242 | 5,000 errors/month free | General / strategy / context | n/a |
| 243 | PostHog | General / strategy / context | n/a |
| 244 | Product analytics, feature flags, A/B test | Admin and analytics | implemented |
| 245 | 1M events/month free | General / strategy / context | n/a |
| 246 | Loops.so | General / strategy / context | n/a |
| 247 | Email marketing automation | General / strategy / context | n/a |
| 248 | 2,000 contacts free | General / strategy / context | n/a |
| 249 | Cal.com (self-host) | General / strategy / context | n/a |
| 250 | Video date scheduling | General / strategy / context | n/a |
| 251 | Self-hosted, free forever | General / strategy / context | n/a |
| 252 | Livekit (self-host) | General / strategy / context | n/a |
| 253 | WebRTC video calling server | Voice/video calls | implemented |
| 254 | Self-hosted on CF Workers | General / strategy / context | n/a |
| 255 | 3.3 Architecture Decision: Why Cloudflare Over AWS | General / strategy / context | n/a |
| 256 | Cloudflare Edge vs. AWS/GCP | General / strategy / context | n/a |
| 257 | Cold Start | General / strategy / context | n/a |
| 258 | ~0ms (Workers run in V8 isolates, no container spin-up). AWS Lambda: 100–800ms cold start. | General / strategy / context | n/a |
| 259 | Global Latency | General / strategy / context | n/a |
| 260 | 300+ edge PoPs. User in Jaipur hits Cloudflare node 8ms away. AWS Mumbai only: 40ms+ for distant users. | General / strategy / context | n/a |
| 261 | Operational Overhead | General / strategy / context | n/a |
| 262 | Zero — no EC2, no ECS, no EKS, no VPCs, no security groups. Pure code deploy. | General / strategy / context | n/a |
| 263 | DDoS Protection | General / strategy / context | n/a |
| 264 | Cloudflare handles 76 million DDoS attacks/day as core infra. AWS Shield costs $3,000/month. | General / strategy / context | n/a |
| 265 | Vendor Lock-In | General / strategy / context | n/a |
| 266 | Workers use Web Standards API (fetch, Web Crypto, Cache). Code is portable. | General / strategy / context | n/a |
| 267 | Cost at Scale | General / strategy / context | n/a |
| 268 | Workers Paid = $5/month for 10M requests. Equivalent AWS infra = $2,000+/month. | General / strategy / context | n/a |
| 269 | 04 Fire & Forget Deployment Architecture | General / strategy / context | n/a |
| 270 | Every commit to the main branch auto-deploys to production in under 60 seconds with zero human intervention, zero downtime, zero rollback anxiety. This is the deployment model: | General / strategy / context | n/a |
| 271 | 4.1 Full Deployment Pipeline | General / strategy / context | n/a |
| 272 | Stage | General / strategy / context | n/a |
| 273 | What Happens | General / strategy / context | n/a |
| 274 | 1. Code Push | Notifications | implemented |
| 275 | Developer pushes to GitHub main branch | Notifications | implemented |
| 276 | 2. GitHub Actions CI | General / strategy / context | n/a |
| 277 | Runs: TypeScript compile → unit tests → integration tests → edge function bundle → Wrangler lint | General / strategy / context | n/a |
| 278 | 3. Cloudflare Pages Build | General / strategy / context | n/a |
| 279 | Auto-triggered by GitHub push. Builds Next.js / Remix app. Deploys to 300+ PoPs simultaneously. | Notifications | implemented |
| 280 | 4. Workers Deploy | General / strategy / context | n/a |
| 281 | Wrangler CLI deploys all Worker scripts. Each Worker version deployed atomically. | General / strategy / context | n/a |
| 282 | 5. D1 Migrations | General / strategy / context | n/a |
| 283 | Drizzle ORM migration runs automatically against D1 database. Schema versioned in Git. | General / strategy / context | n/a |
| 284 | 6. KV Sync | General / strategy / context | n/a |
| 285 | Static reference data (city lists, religion lists) synced to KV namespaces globally. | General / strategy / context | n/a |
| 286 | 7. Smoke Tests | General / strategy / context | n/a |
| 287 | Automated Playwright test suite hits 10 critical user journeys on production edge. | General / strategy / context | n/a |
| 288 | 8. Done ✓ | General / strategy / context | n/a |
| 289 | Full global deployment complete in < 60 seconds. Zero downtime. Instant rollback available. | General / strategy / context | n/a |
| 290 | 4.2 Project Structure | General / strategy / context | n/a |
| 291 | Directory / File | General / strategy / context | n/a |
| 292 | apps/web/ | General / strategy / context | n/a |
| 293 | Next.js 14 App Router frontend — deployed to Cloudflare Pages | General / strategy / context | n/a |
| 294 | apps/worker/ | General / strategy / context | n/a |
| 295 | Hono.js API server — deployed as Cloudflare Worker | General / strategy / context | n/a |
| 296 | apps/realtime/ | Realtime chat | implemented |
| 297 | Durable Objects — WebSocket, video signalling, presence | Realtime chat | implemented |
| 298 | packages/db/ | General / strategy / context | n/a |
| 299 | Drizzle ORM schema + migrations for D1 | General / strategy / context | n/a |
| 300 | packages/ai/ | General / strategy / context | n/a |
| 301 | Workers AI wrapper — Kundali engine, matching ML, moderation | Admin and analytics | implemented |
| 302 | packages/email/ | General / strategy / context | n/a |
| 303 | Resend email templates (React Email components) | General / strategy / context | n/a |
| 304 | wrangler.toml | General / strategy / context | n/a |
| 305 | Single config file declaring all Workers, D1 bindings, KV namespaces, R2 buckets | General / strategy / context | n/a |
| 306 | .github/workflows/ | General / strategy / context | n/a |
| 307 | CI/CD pipeline — single YAML file runs entire deployment | General / strategy / context | n/a |
| 308 | 4.3 Technology Choices — The God-Tier Stack | General / strategy / context | n/a |
| 309 | Layer | General / strategy / context | n/a |
| 310 | Technology | General / strategy / context | n/a |
| 311 | Why This Choice | General / strategy / context | n/a |
| 312 | Frontend | General / strategy / context | n/a |
| 313 | Next.js 14 + React 18 | General / strategy / context | n/a |
| 314 | App Router, Server Components, streaming SSR for SEO (matrimony profile pages must rank on Google) | General / strategy / context | n/a |
| 315 | Styling | General / strategy / context | n/a |
| 316 | Tailwind CSS + shadcn/ui | General / strategy / context | n/a |
| 317 | Ship pixel-perfect UI in hours, not weeks | General / strategy / context | n/a |
| 318 | API Framework | General / strategy / context | n/a |
| 319 | Hono.js on Workers | General / strategy / context | n/a |
| 320 | Fastest Web-standards router. 0ms overhead. TypeScript-first. | General / strategy / context | n/a |
| 321 | Type Safety | Verification/safety/reporting | implemented |
| 322 | TypeScript + Zod | General / strategy / context | n/a |
| 323 | End-to-end type safety from DB schema to API to UI. Zero runtime type errors. | Verification/safety/reporting | implemented |
| 324 | DB ORM | General / strategy / context | n/a |
| 325 | Drizzle ORM | General / strategy / context | n/a |
| 326 | Lightweight, D1-compatible, generates type-safe queries, instant migrations | General / strategy / context | n/a |
| 327 | Auth (Future) | General / strategy / context | n/a |
| 328 | Better Auth / Lucia | General / strategy / context | n/a |
| 329 | Open-source, Workers-compatible, supports OTP + social + sessions | OTP authentication | implemented |
| 330 | Real-Time | General / strategy / context | n/a |
| 331 | Durable Objects + WebSocket | General / strategy / context | n/a |
| 332 | Stateful actors at the edge — WebSocket sessions with zero infrastructure | General / strategy / context | n/a |
| 333 | Search | Search and filter matching | implemented |
| 334 | Cloudflare Vectorize + D1 FTS | General / strategy / context | n/a |
| 335 | Semantic profile search using embeddings + SQLite full-text search | Search and filter matching | implemented |
| 336 | State (Client) | General / strategy / context | n/a |
| 337 | Zustand + React Query | General / strategy / context | n/a |
| 338 | Minimal bundle, powerful caching, optimistic UI updates | General / strategy / context | n/a |
| 339 | Testing | General / strategy / context | n/a |
| 340 | Vitest + Playwright | General / strategy / context | n/a |
| 341 | Fast unit tests + E2E browser automation | General / strategy / context | n/a |
| 342 | Monorepo | General / strategy / context | n/a |
| 343 | Turborepo + pnpm | General / strategy / context | n/a |
| 344 | Cached builds — CI runs in < 2 min even with 100+ packages | General / strategy / context | n/a |
| 345 | 05 Edge-Native Backend Design | General / strategy / context | n/a |
| 346 | 5.1 Worker Architecture — Domain-Driven Routing | General / strategy / context | n/a |
| 347 | Each domain is a separate Cloudflare Worker with its own D1 binding, KV namespace, and R2 bucket. Workers communicate via Service Bindings (internal, zero-latency RPC — no HTTP overhead). | General / strategy / context | n/a |
| 348 | Worker Name | General / strategy / context | n/a |
| 349 | Responsibilities | General / strategy / context | n/a |
| 350 | vivah-auth | General / strategy / context | n/a |
| 351 | OTP generation/validation, JWT mint/refresh/revoke, session management | OTP authentication | implemented |
| 352 | vivah-profile | General / strategy / context | n/a |
| 353 | Profile CRUD, photo upload, bio-data PDF, completeness scoring | General / strategy / context | n/a |
| 354 | vivah-match | General / strategy / context | n/a |
| 355 | Algorithm execution, daily queue generation, interest send/accept/decline | Interests/shortlist flow | implemented |
| 356 | vivah-search | Search and filter matching | implemented |
| 357 | Full-text + semantic + geo search, filter processing, Vectorize queries | Search and filter matching | implemented |
| 358 | vivah-chat | Realtime chat | implemented |
| 359 | Message send/receive, E2E key exchange, media message handling | Realtime chat | implemented |
| 360 | vivah-realtime | Realtime chat | implemented |
| 361 | Durable Objects coordinator — WebSocket lifecycle, presence, typing | Realtime chat | implemented |
| 362 | vivah-notify | General / strategy / context | n/a |
| 363 | Push notification dispatch (FCM/APNS), email triggers, SMS OTP | OTP authentication | implemented |
| 364 | vivah-media | General / strategy / context | n/a |
| 365 | R2 upload orchestration, image compression, AI content moderation | Admin and analytics | implemented |
| 366 | vivah-ai | General / strategy / context | n/a |
| 367 | Kundali engine, recommendation ML, profile AI writer, icebreakers | General / strategy / context | n/a |
| 368 | vivah-pay | General / strategy / context | n/a |
| 369 | Payment webhook handler, subscription state machine (Razorpay/Stripe) | Subscriptions and payments | implemented |
| 370 | vivah-admin | Admin and analytics | implemented |
| 371 | Moderation queue, user management, analytics aggregation, CMS | Admin and analytics | implemented |
| 372 | vivah-safety | Verification/safety/reporting | implemented |
| 373 | Fake detection pipeline, report processing, block/ban enforcement | Verification/safety/reporting | implemented |
| 374 | 5.2 API Design — RESTful + Type-Safe | General / strategy / context | n/a |
| 375 | All endpoints versioned at /api/v1/ | General / strategy / context | n/a |
| 376 | JSON:API-compatible response envelope: { data, meta, errors, links } | General / strategy / context | n/a |
| 377 | Cursor-based pagination on all list endpoints — O(1) performance regardless of offset | General / strategy / context | n/a |
| 378 | Request validation: Zod schema on every endpoint — returns typed 422 on invalid input | General / strategy / context | n/a |
| 379 | Response caching: GET endpoints carry Cache-Control headers for Cloudflare edge caching | General / strategy / context | n/a |
| 380 | Idempotency keys on all mutation endpoints — safe to retry without duplicate effects | General / strategy / context | n/a |
| 381 | 5.3 Critical API Endpoints | General / strategy / context | n/a |
| 382 | Endpoint | General / strategy / context | n/a |
| 383 | POST /auth/otp/send | OTP authentication | implemented |
| 384 | Dispatch OTP via SMS + WhatsApp simultaneously | OTP authentication | implemented |
| 385 | POST /auth/otp/verify | OTP authentication | implemented |
| 386 | Validate OTP, mint JWT, store refresh token in KV with device fingerprint | OTP authentication | implemented |
| 387 | GET /profiles/:id | General / strategy / context | n/a |
| 388 | Fetch profile — respects privacy level, increments view counter | Verification/safety/reporting | implemented |
| 389 | PUT /profiles/:id | General / strategy / context | n/a |
| 390 | Full profile update with Zod validation + re-score completeness | General / strategy / context | n/a |
| 391 | GET /matches/daily | General / strategy / context | n/a |
| 392 | Return personalised queue: ML score × community weight × activity recency | General / strategy / context | n/a |
| 393 | POST /matches/interest | Interests/shortlist flow | implemented |
| 394 | Send interest — checks daily limit, fires notification, logs to Analytics Engine | Interests/shortlist flow | implemented |
| 395 | PATCH /matches/interest/:id | Interests/shortlist flow | implemented |
| 396 | Accept/decline — on accept, creates Connection, unlocks chat | Realtime chat | implemented |
| 397 | GET /search | Search and filter matching | implemented |
| 398 | Full-text + filter search backed by Vectorize semantic embeddings | Search and filter matching | implemented |
| 399 | POST /media/upload | General / strategy / context | n/a |
| 400 | Presigned R2 URL generation — client uploads directly to R2, zero Worker throughput | General / strategy / context | n/a |
| 401 | GET /chat/:connId | Realtime chat | implemented |
| 402 | Return paginated message history from D1 (Cassandra pattern with D1) | Realtime chat | implemented |
| 403 | POST /chat/:connId/message | Realtime chat | implemented |
| 404 | Persist message + fan out to Durable Object for WebSocket delivery | Realtime chat | implemented |
| 405 | POST /safety/report | Verification/safety/reporting | implemented |
| 406 | File report — auto-triage by AI severity score → moderation queue | Verification/safety/reporting | implemented |
| 407 | GET /kundali/match | General / strategy / context | n/a |
| 408 | Compute 36-gun Milan between two profiles via Workers AI Kundali engine | General / strategy / context | n/a |
| 409 | 06 Database Architecture — D1 + KV + R2 + Vectorize | General / strategy / context | n/a |
| 410 | Design Principle: | General / strategy / context | n/a |
| 411 | Polyglot persistence — each data type lives in the storage engine optimised for its access pattern. D1 for relational, KV for hot lookups, R2 for blobs, Vectorize for semantic search. | Search and filter matching | implemented |
| 412 | 6.1 D1 (SQLite at Edge) — Core Schema | General / strategy / context | n/a |
| 413 | Table | General / strategy / context | n/a |
| 414 | Schema Overview | General / strategy / context | n/a |
| 415 | users | General / strategy / context | n/a |
| 416 | id TEXT PK, mobile_hash TEXT UNIQUE, email_hash TEXT, pwd_hash TEXT, status TEXT, plan TEXT, verification_level INTEGER, created_at INTEGER, last_active INTEGER | Verification/safety/reporting | implemented |
| 417 | profiles | General / strategy / context | n/a |
| 418 | id TEXT PK, user_id TEXT FK, name TEXT, dob INTEGER, gender TEXT, height_cm INTEGER, religion TEXT, caste TEXT, education TEXT, occupation TEXT, income_band TEXT, city TEXT, state TEXT, country TEXT, about_me TEXT, profile_score INTEGER, vivah_score INTEGER, updated_at INTEGER | General / strategy / context | n/a |
| 419 | preferences | General / strategy / context | n/a |
| 420 | id TEXT PK, profile_id TEXT FK, age_min INTEGER, age_max INTEGER, religions JSON, castes JSON, income_min TEXT, locations JSON, education_levels JSON, lifestyle_prefs JSON | General / strategy / context | n/a |
| 421 | horoscopes | General / strategy / context | n/a |
| 422 | id TEXT PK, profile_id TEXT FK, dob INTEGER, tob TEXT, pob TEXT, rashi TEXT, nakshatra TEXT, manglik INTEGER, gun_points INTEGER, chart_json TEXT | General / strategy / context | n/a |
| 423 | profile_media | General / strategy / context | n/a |
| 424 | id TEXT PK, profile_id TEXT FK, r2_key TEXT, type TEXT, sort_order INTEGER, visibility TEXT, is_primary INTEGER, mod_status TEXT, uploaded_at INTEGER | General / strategy / context | n/a |
| 425 | interests | Interests/shortlist flow | implemented |
| 426 | id TEXT PK, sender_id TEXT, receiver_id TEXT, status TEXT, message TEXT, sent_at INTEGER, responded_at INTEGER | Realtime chat | implemented |
| 427 | connections | General / strategy / context | n/a |
| 428 | id TEXT PK, profile_a TEXT, profile_b TEXT, connected_at INTEGER, status TEXT | General / strategy / context | n/a |
| 429 | messages | Realtime chat | implemented |
| 430 | id TEXT PK, conn_id TEXT FK, sender_id TEXT, type TEXT, content_enc TEXT, nonce TEXT, sent_at INTEGER, delivered_at INTEGER, read_at INTEGER, deleted_for JSON | General / strategy / context | n/a |
| 431 | subscriptions | Subscriptions and payments | implemented |
| 432 | id TEXT PK, user_id TEXT, plan TEXT, start_ts INTEGER, end_ts INTEGER, status TEXT, auto_renew INTEGER | Subscriptions and payments | implemented |
| 433 | transactions | General / strategy / context | n/a |
| 434 | id TEXT PK, user_id TEXT, amount INTEGER, currency TEXT, gateway TEXT, gw_id TEXT, status TEXT, created_at INTEGER | General / strategy / context | n/a |
| 435 | reports | Verification/safety/reporting | implemented |
| 436 | id TEXT PK, reporter_id TEXT, target_id TEXT, category TEXT, evidence_json TEXT, ai_severity INTEGER, status TEXT, created_at INTEGER | Verification/safety/reporting | implemented |
| 437 | moderation_log | Admin and analytics | implemented |
| 438 | id TEXT PK, admin_id TEXT, action TEXT, target_type TEXT, target_id TEXT, reason TEXT, ts INTEGER | Admin and analytics | implemented |
| 439 | 6.2 KV Namespace Strategy | General / strategy / context | n/a |
| 440 | KV Namespace | General / strategy / context | n/a |
| 441 | Keys Stored & TTL | General / strategy / context | n/a |
| 442 | SESSIONS | General / strategy / context | n/a |
| 443 | session:{token_hash} → {user_id, device, plan} — TTL 30 days | Subscriptions and payments | implemented |
| 444 | OTP_STORE | General / strategy / context | n/a |
| 445 | otp:{mobile_hash} → {code_hash, attempts} — TTL 10 minutes | OTP authentication | implemented |
| 446 | RATE_LIMITS | General / strategy / context | n/a |
| 447 | rl:{user_id}:{action} → count — TTL 1 hour | General / strategy / context | n/a |
| 448 | PROFILE_CACHE | General / strategy / context | n/a |
| 449 | profile:{id} → compressed JSON — TTL 5 minutes, invalidated on update | General / strategy / context | n/a |
| 450 | MATCH_QUEUE | General / strategy / context | n/a |
| 451 | queue:{user_id} → {profile_ids[], generated_at} — TTL 24 hours | General / strategy / context | n/a |
| 452 | DEVICE_FP | General / strategy / context | n/a |
| 453 | fp:{fingerprint_hash} → {user_id, first_seen} — TTL 90 days | General / strategy / context | n/a |
| 454 | REF_DATA | General / strategy / context | n/a |
| 455 | cities, religions, castes, education lists — TTL 24 hours | General / strategy / context | n/a |
| 456 | BOOST_ACTIVE | General / strategy / context | n/a |
| 457 | boost:{profile_id} → {end_time} — TTL = boost duration | General / strategy / context | n/a |
| 458 | 6.3 R2 Bucket Layout | General / strategy / context | n/a |
| 459 | R2 Path Pattern | General / strategy / context | n/a |
| 460 | Content | General / strategy / context | n/a |
| 461 | photos/{profile_id}/{media_id}.webp | General / strategy / context | n/a |
| 462 | Profile photos — served via Cloudflare Images transform | General / strategy / context | n/a |
| 463 | photos/{profile_id}/thumb_{media_id}.webp | General / strategy / context | n/a |
| 464 | Thumbnails auto-generated on upload | General / strategy / context | n/a |
| 465 | docs/{user_id}/aadhaar_{ts}.enc | General / strategy / context | n/a |
| 466 | Encrypted Aadhaar uploads — server-side encrypted before storage | General / strategy / context | n/a |
| 467 | biodata/{profile_id}/biodata.pdf | General / strategy / context | n/a |
| 468 | Generated bio-data PDFs — cached 1 hour | General / strategy / context | n/a |
| 469 | audio/{conn_id}/{msg_id}.ogg | General / strategy / context | n/a |
| 470 | Voice messages — E2E encrypted | Realtime chat | implemented |
| 471 | kundali/{profile_id}/chart.png | General / strategy / context | n/a |
| 472 | Rendered Kundali chart images | General / strategy / context | n/a |
| 473 | exports/{user_id}/data_export.zip | General / strategy / context | n/a |
| 474 | GDPR/DPDP data export archives | General / strategy / context | n/a |
| 475 | 6.4 Vectorize — Semantic Search Index | Search and filter matching | implemented |
| 476 | Every profile generates a 768-dimension embedding vector using the Workers AI 'bge-large-en-v1.5' model, computed from a concatenation of bio text, occupation, hobbies, and lifestyle fields. These vectors power semantic 'people like you also liked' recommendations and natural language search. | Search and filter matching | implemented |
| 477 | Index: vivah-profiles — 768-dim, HNSW algorithm, cosine similarity | General / strategy / context | n/a |
| 478 | Query: 'Software engineer, vegetarian, Bangalore, loves travel' → top-50 semantically similar profiles | General / strategy / context | n/a |
| 479 | Metadata filtering: Vectorize metadata fields store religion, caste, age_band for pre-filter before ANN search | Search and filter matching | implemented |
| 480 | Re-embedding: Profile vector regenerated automatically on any significant profile update | General / strategy / context | n/a |
| 481 | 07 Real-Time Engine — Durable Objects | General / strategy / context | n/a |
| 482 | 7.1 Durable Objects Architecture | General / strategy / context | n/a |
| 483 | Cloudflare Durable Objects are stateful V8 isolates — each one is a single-instance, globally unique actor with its own persistent storage and WebSocket lifecycle management. They are the key to zero-infra real-time at edge. | General / strategy / context | n/a |
| 484 | Durable Object Class | General / strategy / context | n/a |
| 485 | Role | General / strategy / context | n/a |
| 486 | ConversationDO | General / strategy / context | n/a |
| 487 | One instance per active conversation. Manages all WebSocket connections for that chat. Fan-out messages to both participants in < 5ms. Stores last 100 messages in DO storage for instant history. | Realtime chat | implemented |
| 488 | PresenceDO | Realtime chat | implemented |
| 489 | Tracks online/offline/typing status per user. Single source of truth. Subscribers get push updates. | Notifications | implemented |
| 490 | VideoSignalDO | General / strategy / context | n/a |
| 491 | WebRTC signalling server — exchanges SDP offers/answers, ICE candidates between peers. Replaces Twilio for free. | Voice/video calls | implemented |
| 492 | MatchQueueDO | General / strategy / context | n/a |
| 493 | Coordinates real-time match notifications. When User A's profile enters User B's algorithm queue, this DO fires the push notification within 200ms. | Notifications | implemented |
| 494 | RateLimitDO | General / strategy / context | n/a |
| 495 | Per-user rate limiter using DO atomic counters. Distributed, consistent, zero Redis needed. | General / strategy / context | n/a |
| 496 | 7.2 WebSocket Message Flow | Realtime chat | implemented |
| 497 | User opens chat screen — client connects to /ws/conv/{convId} on vivah-realtime Worker | Realtime chat | implemented |
| 498 | Worker routes to ConversationDO using conn_id as DO name (globally unique) | General / strategy / context | n/a |
| 499 | DO upgrades connection to WebSocket, registers participant in its local state | General / strategy / context | n/a |
| 500 | User types — client sends {type:'typing'} over WebSocket | General / strategy / context | n/a |
| 501 | DO receives event, fans out to other participant's WebSocket within the same DO | General / strategy / context | n/a |
| 502 | User sends message — DO persists to D1 via binding, fans out to recipient's WebSocket | Realtime chat | implemented |
| 503 | If recipient is offline — DO triggers vivah-notify via Service Binding for push notification | Notifications | implemented |
| 504 | Message delivered, delivered_at timestamp updated via KV write | Realtime chat | implemented |
| 505 | Latency: | General / strategy / context | n/a |
| 506 | End-to-end message delivery for users in same region: < 20ms. Globally: < 80ms. No competitor achieves this. | Realtime chat | implemented |
| 507 | 08 Security Architecture — Unbreakable | General / strategy / context | n/a |
| 508 | Threat Model: | General / strategy / context | n/a |
| 509 | Vivah.App is a high-value target: personal identity data, relationship intentions, government IDs, financial transactions, and sensitive family information. The security architecture is designed to make a data breach structurally impossible — not just improbable. | General / strategy / context | n/a |
| 510 | 8.1 Authentication Security | General / strategy / context | n/a |
| 511 | Control | General / strategy / context | n/a |
| 512 | Implementation | General / strategy / context | n/a |
| 513 | OTP Security | OTP authentication | implemented |
| 514 | 6-digit OTP hashed with bcrypt before KV storage. Server compares hashes — never plaintext. Rate-limited to 3 attempts. 10-minute TTL. Brute-force structurally impossible. | OTP authentication | implemented |
| 515 | JWT Architecture | General / strategy / context | n/a |
| 516 | Access Token: RS256, 15-min TTL, signed with edge-stored private key via Web Crypto API. Refresh Token: stored in HttpOnly, Secure, SameSite=Strict cookie. Server-side revocation list in KV. | General / strategy / context | n/a |
| 517 | Token Binding | General / strategy / context | n/a |
| 518 | Each JWT carries device fingerprint claim. Token from Device A cannot be replayed on Device B — server validates fingerprint match on every request. | General / strategy / context | n/a |
| 519 | Refresh Rotation | General / strategy / context | n/a |
| 520 | Every refresh issues a new refresh token and invalidates the old one (single-use). Prevents token theft replay attacks. | General / strategy / context | n/a |
| 521 | Session Anomaly Detection | General / strategy / context | n/a |
| 522 | Login from new device/country triggers silent re-verification OTP. No friction for normal users, immediate block for account takeover attempts. | OTP authentication | implemented |
| 523 | Biometric Pin (App) | General / strategy / context | n/a |
| 524 | App-level PIN / FaceID / TouchID using platform secure enclave. Access token decrypted only after biometric success — never exposed to device storage plaintext. | General / strategy / context | n/a |
| 525 | 8.2 End-to-End Message Encryption | Realtime chat | implemented |
| 526 | Messages are encrypted client-side before leaving the device. The server never holds message plaintext. Even a full database breach exposes zero message content. | Realtime chat | implemented |
| 527 | Step | General / strategy / context | n/a |
| 528 | Cryptographic Operation | General / strategy / context | n/a |
| 529 | Key Generation | General / strategy / context | n/a |
| 530 | On connection established, each client generates an ephemeral X25519 key pair using Web Crypto API | General / strategy / context | n/a |
| 531 | Key Exchange | General / strategy / context | n/a |
| 532 | ECDH key agreement: each party computes shared secret from their private key + other's public key | General / strategy / context | n/a |
| 533 | Key Derivation | General / strategy / context | n/a |
| 534 | HKDF-SHA256 derives two 256-bit AES-GCM keys (one per direction) from the shared secret | General / strategy / context | n/a |
| 535 | Message Encryption | Realtime chat | implemented |
| 536 | AES-256-GCM with 96-bit random nonce per message. Nonce stored alongside ciphertext in D1 | Realtime chat | implemented |
| 537 | Server Role | General / strategy / context | n/a |
| 538 | Server stores only: sender_id, ciphertext (base64), nonce, timestamps. Zero knowledge of content. | General / strategy / context | n/a |
| 539 | Forward Secrecy | General / strategy / context | n/a |
| 540 | New ephemeral keys generated per conversation session. Past messages safe even if future keys compromised. | Realtime chat | implemented |
| 541 | Key Rotation | General / strategy / context | n/a |
| 542 | Keys rotated every 7 days or on device change — transparent to users | General / strategy / context | n/a |
| 543 | 8.3 Data Protection Architecture | General / strategy / context | n/a |
| 544 | Data Category | General / strategy / context | n/a |
| 545 | Protection Method | General / strategy / context | n/a |
| 546 | Mobile Numbers | General / strategy / context | n/a |
| 547 | Stored as Argon2id hash (not bcrypt — Argon2id is memory-hard, defeats GPU cracking). Never stored in plaintext anywhere. | General / strategy / context | n/a |
| 548 | Email Addresses | General / strategy / context | n/a |
| 549 | SHA-256 hash with site-wide pepper. Used only for login matching. Raw email never persisted. | General / strategy / context | n/a |
| 550 | Government IDs (Aadhaar) | General / strategy / context | n/a |
| 551 | AES-256-GCM encrypted before R2 storage using KMS-derived key. Decrypted only during one-time verification flow. Auto-deleted after verification. | Verification/safety/reporting | implemented |
| 552 | Profile Photos | General / strategy / context | n/a |
| 553 | Stored in private R2 bucket. Served via time-limited presigned URLs (15-min TTL) — direct URL sharing is impossible. | General / strategy / context | n/a |
| 554 | Payment Data | Subscriptions and payments | implemented |
| 555 | Zero card data on Vivah servers. Razorpay tokenisation — only Razorpay token stored. PCI-DSS compliance inherited from gateway. | Subscriptions and payments | implemented |
| 556 | Database | General / strategy / context | n/a |
| 557 | D1 encryption at rest (AES-256, Cloudflare-managed). Backups encrypted independently. | General / strategy / context | n/a |
| 558 | Logs | General / strategy / context | n/a |
| 559 | All logs in Analytics Engine stripped of PII before storage. IP addresses truncated to /24 subnet. | Admin and analytics | implemented |
| 560 | 8.4 API & Transport Security | General / strategy / context | n/a |
| 561 | Specification | General / strategy / context | n/a |
| 562 | TLS 1.3 minimum enforced by Cloudflare. TLS 1.0/1.1 permanently disabled. HSTS max-age=31536000; includeSubDomains; preload | General / strategy / context | n/a |
| 563 | Certificate Pinning | General / strategy / context | n/a |
| 564 | Mobile apps pin to Cloudflare's root CA via Network Security Config (Android) and ATS (iOS) | General / strategy / context | n/a |
| 565 | Content Security Policy | General / strategy / context | n/a |
| 566 | Strict CSP: default-src 'self'. No inline scripts. No eval. Blocks XSS at browser level. | Verification/safety/reporting | implemented |
| 567 | CORS | General / strategy / context | n/a |
| 568 | Allowlist only: vivah.app, app.vivah.app, local dev. Wildcard (*) permanently blocked at Worker level. | Verification/safety/reporting | implemented |
| 569 | Rate Limiting | General / strategy / context | n/a |
| 570 | Global: 200 req/min per IP. Per user: 60 req/min. Auth endpoints: 5 req/15 min. Enforced by RateLimitDO. | General / strategy / context | n/a |
| 571 | Input Sanitisation | General / strategy / context | n/a |
| 572 | All inputs validated by Zod schema. SQL injection structurally impossible — Drizzle uses parameterised queries exclusively. XSS sanitised by DOMPurify client-side + CSP server-side. | General / strategy / context | n/a |
| 573 | Bot Protection | General / strategy / context | n/a |
| 574 | Cloudflare Turnstile on all public forms. Workers AI bot score (0–99) on all API requests. Score < 30 = auto-block. | Verification/safety/reporting | implemented |
| 575 | WAF Rules | General / strategy / context | n/a |
| 576 | Cloudflare WAF with OWASP Core Rule Set. Custom rules block matrimony-specific attack patterns (mass interest sending, profile scraping). | Interests/shortlist flow | implemented |
| 577 | DDoS | General / strategy / context | n/a |
| 578 | Cloudflare handles up to 71 Tbps of DDoS absorption. Zero-cost, always-on. No additional configuration. | General / strategy / context | n/a |
| 579 | 8.5 Privacy Engineering | Verification/safety/reporting | implemented |
| 580 | Privacy Feature | Verification/safety/reporting | implemented |
| 581 | Technical Implementation | General / strategy / context | n/a |
| 582 | Data Minimisation | General / strategy / context | n/a |
| 583 | Zod schemas enforce that only declared fields are accepted. Unknown fields stripped server-side. | General / strategy / context | n/a |
| 584 | Purpose Limitation | General / strategy / context | n/a |
| 585 | Separate D1 databases per data purpose — profile DB, moderation DB, analytics DB. No cross-join possible at DB level. | Admin and analytics | implemented |
| 586 | Right to Erasure | General / strategy / context | n/a |
| 587 | DELETE /users/{id} cascades hard-deletes across all tables within 30 seconds. R2 lifecycle rule purges media within 24 hours. KV TTLs auto-expire session data. | General / strategy / context | n/a |
| 588 | Data Portability | General / strategy / context | n/a |
| 589 | Automated export pipeline generates ZIP of all user data in JSON within 10 minutes of request. | General / strategy / context | n/a |
| 590 | Consent Audit Trail | General / strategy / context | n/a |
| 591 | Every consent grant/revoke stored as immutable event in Analytics Engine with timestamp, IP subnet, and user agent. | Admin and analytics | implemented |
| 592 | Photo Privacy | Verification/safety/reporting | implemented |
| 593 | All photos served as presigned R2 URLs with 15-min TTL. Screenshot prevention via CSS pointer-events block + mobile native screenshot listener. | Verification/safety/reporting | implemented |
| 594 | Anonymisation | General / strategy / context | n/a |
| 595 | Deleted account data anonymised: name → NULL, mobile → kept as hash (for future re-registration block), all profile fields → NULL. | Verification/safety/reporting | implemented |
| 596 | 8.6 Fake Profile & Fraud Prevention System | General / strategy / context | n/a |
| 597 | The Vivah Trust Engine runs a continuous multi-signal pipeline that makes fake profiles structurally unsustainable: | General / strategy / context | n/a |
| 598 | Signal | General / strategy / context | n/a |
| 599 | Detection Method | General / strategy / context | n/a |
| 600 | Duplicate Face Detection | General / strategy / context | n/a |
| 601 | Workers AI vision model generates face embedding on every photo upload. Cosine similarity check against all existing face embeddings in Vectorize. > 0.92 similarity = same person flag. | General / strategy / context | n/a |
| 602 | Stock Photo Detection | General / strategy / context | n/a |
| 603 | Perceptual hash (pHash) compared against database of 1M+ known stock photos. Match = instant rejection. | General / strategy / context | n/a |
| 604 | Device Fingerprinting | General / strategy / context | n/a |
| 605 | Canvas + WebGL + AudioContext fingerprint generated client-side. New account from same fingerprint within 30 days = manual review. | General / strategy / context | n/a |
| 606 | Velocity Checks | General / strategy / context | n/a |
| 607 | > 3 accounts from same IP/24 in 24 hours = auto-hold for review. > 5 reports filed against one profile in 7 days = auto-suspend. | Verification/safety/reporting | implemented |
| 608 | Behavioural Analysis | General / strategy / context | n/a |
| 609 | ML model scores interaction patterns. Mass-copying profile text, identical interest messages, unusually high daily interest count = fraud score escalation. | Interests/shortlist flow | implemented |
| 610 | Scam Script Detection | General / strategy / context | n/a |
| 611 | NLP classifier (fine-tuned on romance scam datasets) scores all outgoing messages. Score > 0.8 = message blocked + account flagged. | Realtime chat | implemented |
| 612 | Phone Intelligence | General / strategy / context | n/a |
| 613 | Mobile number cross-checked against disposable number databases. VoIP numbers flagged. Ported numbers scored. | General / strategy / context | n/a |
| 614 | 09 AI & Smart Matchmaking Engine | Search and filter matching | implemented |
| 615 | Workers AI Advantage: | General / strategy / context | n/a |
| 616 | All AI inference runs at the Cloudflare edge — zero external API calls, zero API key costs (free tier), zero data leaving Cloudflare's network. Latency = 10–50ms for inference. No competitor has this architecture. | General / strategy / context | n/a |
| 617 | 9.1 AI Models Running at Edge (Workers AI) | General / strategy / context | n/a |
| 618 | Model | General / strategy / context | n/a |
| 619 | Use Case in Vivah.App | General / strategy / context | n/a |
| 620 | @cf/meta/llama-3.1-8b-instruct | General / strategy / context | n/a |
| 621 | Kundali AI interpretation, icebreaker generation, AI profile writer, scam detection reasoning | General / strategy / context | n/a |
| 622 | @cf/baai/bge-large-en-v1.5 | General / strategy / context | n/a |
| 623 | Profile text → 768-dim embeddings for Vectorize semantic search | Search and filter matching | implemented |
| 624 | @cf/microsoft/phi-2 | General / strategy / context | n/a |
| 625 | Lightweight model for completeness scoring nudges, quick classification tasks | General / strategy / context | n/a |
| 626 | @cf/unum/uform-gen2-qwen-500m | General / strategy / context | n/a |
| 627 | Vision-language: profile photo description for blind users and moderation context | Admin and analytics | implemented |
| 628 | @cf/llava-hf/llava-1.5-7b-hf | General / strategy / context | n/a |
| 629 | NSFW detection + face quality scoring on profile photo uploads | General / strategy / context | n/a |
| 630 | @cf/openai/whisper | General / strategy / context | n/a |
| 631 | Voice message transcription for search indexing and safety scanning | Search and filter matching | implemented |
| 632 | @cf/huggingface/distilbert-sst-2-int8 | General / strategy / context | n/a |
| 633 | Sentiment analysis on incoming messages for harassment detection | Realtime chat | implemented |
| 634 | 9.2 Kundali AI Engine — Technical Design | General / strategy / context | n/a |
| 635 | The Kundali AI engine is a competitive moat. No competitor has an AI-native Vedic astrology engine. The system combines a deterministic calculation layer with an LLM interpretation layer: | General / strategy / context | n/a |
| 636 | INPUT: Date of birth, time of birth (HH:MM), place of birth (lat/lon) | General / strategy / context | n/a |
| 637 | EPHEMERIS LAYER: Swiss Ephemeris WASM module (runs in Worker) calculates planetary positions at birth moment | Subscriptions and payments | implemented |
| 638 | RASHI ENGINE: Assigns Moon sign (Rashi) and birth star (Nakshatra) from planetary data | Subscriptions and payments | implemented |
| 639 | GUN MILAN ENGINE: Deterministic 36-point Ashtakoota calculation — Varna, Vashya, Tara, Yoni, Graha Maitri, Gana, Bhakoot, Nadi | General / strategy / context | n/a |
| 640 | MANGLIK DETECTION: Mars position check in 1st, 2nd, 4th, 7th, 8th, 12th house | General / strategy / context | n/a |
| 641 | LLM INTERPRETATION: Llama-3.1 generates a 300-word natural language compatibility explanation from the Gun score breakdown | Search and filter matching | implemented |
| 642 | OUTPUT: { rashi, nakshatra, manglik: bool, gun_score: int, koota_breakdown: [], interpretation: string, muhurat_dates: [] } | General / strategy / context | n/a |
| 643 | 9.3 Matching Algorithm — Multi-Stage Pipeline | General / strategy / context | n/a |
| 644 | Algorithm Detail | General / strategy / context | n/a |
| 645 | Stage 1: Hard Filter | Search and filter matching | implemented |
| 646 | SQL WHERE clause on D1 — religion, caste, age range, gender, marital status, country. Runs in < 5ms. | General / strategy / context | n/a |
| 647 | Stage 2: Soft Score | General / strategy / context | n/a |
| 648 | Weighted attribute scoring on filtered set — education, income, location, lifestyle, horoscope compatibility. O(n) over filtered set. | Search and filter matching | implemented |
| 649 | Stage 3: Semantic Boost | General / strategy / context | n/a |
| 650 | Vectorize ANN query: profiles whose embedding is closest to current user's embedding. Boosts candidates with similar personality/career/lifestyle language. | General / strategy / context | n/a |
| 651 | Stage 4: Social Graph | General / strategy / context | n/a |
| 652 | Boost profiles with overlapping LinkedIn education or employer (extracted from verified employment data). | General / strategy / context | n/a |
| 653 | Stage 5: Behavioural ML | General / strategy / context | n/a |
| 654 | XGBoost model trained on historical interest-accept patterns. Features: view duration, message reply rate, profile photo count, recency. Predicts P(accept) for each candidate. | Interests/shortlist flow | implemented |
| 655 | Stage 6: Diversity Injection | General / strategy / context | n/a |
| 656 | Ensure queue isn't homogeneous — inject 10% 'discovery' profiles slightly outside comfort zone to combat filter bubbles. | Search and filter matching | implemented |
| 657 | Stage 7: Vivah Score™ Filter | Search and filter matching | implemented |
| 658 | Profiles with Vivah Score < 40 deprioritised. Protects users from unverified/incomplete profiles dominating their queue. | General / strategy / context | n/a |
| 659 | Output | General / strategy / context | n/a |
| 660 | Ranked list of 50 profiles per user, stored in KV Match Queue, refreshed every 24 hours. | General / strategy / context | n/a |
| 661 | 10 Performance — Thousands of Users, Zero Lag | General / strategy / context | n/a |
| 662 | 10.1 Performance Targets & Benchmarks | General / strategy / context | n/a |
| 663 | Metric | General / strategy / context | n/a |
| 664 | Target (Non-Negotiable) | General / strategy / context | n/a |
| 665 | API Response P50 | General / strategy / context | n/a |
| 666 | < 50ms globally (Workers execute in < 2ms CPU; D1 adds 10–30ms) | General / strategy / context | n/a |
| 667 | API Response P99 | General / strategy / context | n/a |
| 668 | < 200ms globally | General / strategy / context | n/a |
| 669 | Time to First Byte (Web) | General / strategy / context | n/a |
| 670 | < 200ms — Cloudflare Pages edge SSR | General / strategy / context | n/a |
| 671 | App Cold Launch (Android mid-range) | General / strategy / context | n/a |
| 672 | < 2 seconds | General / strategy / context | n/a |
| 673 | Profile Image Load | General / strategy / context | n/a |
| 674 | < 300ms — R2 + Cloudflare Images CDN, WebP, responsive sizes | General / strategy / context | n/a |
| 675 | Real-Time Message Delivery | Realtime chat | implemented |
| 676 | < 20ms same-region, < 80ms cross-region | General / strategy / context | n/a |
| 677 | Search Query (40 filters) | Search and filter matching | implemented |
| 678 | < 150ms — D1 FTS + pre-filtered Vectorize ANN | Search and filter matching | implemented |
| 679 | Kundali Computation | General / strategy / context | n/a |
| 680 | < 800ms — WASM ephemeris + Workers AI LLM inference | General / strategy / context | n/a |
| 681 | Match Queue Generation | General / strategy / context | n/a |
| 682 | < 2 seconds for full 50-profile queue via Cloudflare Queue | General / strategy / context | n/a |
| 683 | Concurrent WebSocket Sessions | General / strategy / context | n/a |
| 684 | 10,000+ per Durable Object region (Cloudflare managed) | General / strategy / context | n/a |
| 685 | Platform Uptime | General / strategy / context | n/a |
| 686 | 99.99% — Cloudflare's own SLA | General / strategy / context | n/a |
| 687 | 10.2 Caching Architecture | General / strategy / context | n/a |
| 688 | Cache Layer | General / strategy / context | n/a |
| 689 | Strategy | General / strategy / context | n/a |
| 690 | Cloudflare CDN | General / strategy / context | n/a |
| 691 | Profile cards, static assets, biodata PDFs — Cache-Control: public, max-age=300. Purged on profile update via CF API. | General / strategy / context | n/a |
| 692 | KV Profile Cache | General / strategy / context | n/a |
| 693 | Full profile JSON — 5-min TTL. Hit rate > 95% for popular profiles. KV reads: ~1ms globally. | General / strategy / context | n/a |
| 694 | KV Match Queue | General / strategy / context | n/a |
| 695 | 24-hour TTL. Invalidated only when user significantly updates preferences. Prevents redundant ML pipeline re-runs. | General / strategy / context | n/a |
| 696 | D1 Query Cache | General / strategy / context | n/a |
| 697 | SQLite page cache — frequently accessed index pages kept in Workers memory for duration of isolate lifetime. | General / strategy / context | n/a |
| 698 | Workers Memory Cache | General / strategy / context | n/a |
| 699 | Reference data (city lists, religion enums) cached in Worker module-level Map — zero-latency lookups. | General / strategy / context | n/a |
| 700 | R2 + Transform | General / strategy / context | n/a |
| 701 | Images transformed once, cached at CF edge in 50+ formats (size × quality). Never re-processed. | General / strategy / context | n/a |
| 702 | 10.3 Handling Concurrent Users — The Math | General / strategy / context | n/a |
| 703 | Cloudflare Workers scale infinitely and automatically. There is no 'scaling' action required by the operator. Here is what the free tier genuinely supports: | General / strategy / context | n/a |
| 704 | Scenario | General / strategy / context | n/a |
| 705 | Capacity Analysis | General / strategy / context | n/a |
| 706 | 100K daily active users | General / strategy / context | n/a |
| 707 | Avg 10 API calls/user/day = 1M req/day. Free tier: 100K/day. Upgrade to $5 Workers Paid = 10M req/month = comfortably handles 100K DAU. | General / strategy / context | n/a |
| 708 | 1,000 concurrent chatters | Realtime chat | implemented |
| 709 | 1,000 WebSocket connections across Durable Objects. Each DO handles 32,000 connections. Total: trivial. | General / strategy / context | n/a |
| 710 | Viral Spike (10x traffic) | General / strategy / context | n/a |
| 711 | Workers scale to millions of requests in milliseconds. No pre-warming, no scale-out time. Spike absorbed transparently. | General / strategy / context | n/a |
| 712 | Photo Upload Storm | General / strategy / context | n/a |
| 713 | R2 handles uploads via presigned URLs — Workers never touch photo bytes. 10,000 simultaneous uploads: zero Worker load. | General / strategy / context | n/a |
| 714 | Search Load | Search and filter matching | implemented |
| 715 | Vectorize handles 1,000 ANN queries/second at free tier. D1 FTS: 100+ concurrent queries/second. | General / strategy / context | n/a |
| 716 | Match Queue Regeneration | General / strategy / context | n/a |
| 717 | Cloudflare Queues process match jobs in background. 10,000 users' queues regenerated overnight: 2–3 hours, zero user-facing impact. | General / strategy / context | n/a |
| 718 | 10.4 Mobile App Performance | General / strategy / context | n/a |
| 719 | Bundle splitting: Code split by route — chat bundle never loaded on search screen | Search and filter matching | implemented |
| 720 | Optimistic UI: Sending a message updates UI immediately before server confirmation | Realtime chat | implemented |
| 721 | Image lazy loading: Profile photos load only when scrolled into viewport | General / strategy / context | n/a |
| 722 | Infinite scroll: Cursor-based pagination loads 20 profiles at a time, prefetch next page | General / strategy / context | n/a |
| 723 | Offline mode: Service Worker caches last 50 viewed profiles for offline browsing | General / strategy / context | n/a |
| 724 | WebP + AVIF: All images served in modern formats — 40–60% smaller than JPEG | General / strategy / context | n/a |
| 725 | HTTP/3 (QUIC): Cloudflare serves all traffic over HTTP/3 — 30% faster on mobile networks | General / strategy / context | n/a |
| 726 | Brotli compression: All text responses Brotli-compressed — 20–25% smaller than gzip | General / strategy / context | n/a |
| 727 | 11 Scale Path — ₹0 → $5 → $500 → IPO | General / strategy / context | n/a |
| 728 | 11.1 Phase 0 — Pre-Launch (₹0 / Month) | General / strategy / context | n/a |
| 729 | Goal: | General / strategy / context | n/a |
| 730 | Complete platform built and deployed on Cloudflare free tier. Fully functional for 0–5,000 daily active users. | General / strategy / context | n/a |
| 731 | Milestone | General / strategy / context | n/a |
| 732 | Detail | General / strategy / context | n/a |
| 733 | Infrastructure | General / strategy / context | n/a |
| 734 | 100% free tier — Workers, Pages, D1, KV, R2, Durable Objects, Workers AI | General / strategy / context | n/a |
| 735 | Features Shipped | General / strategy / context | n/a |
| 736 | Registration, profiles, matching algorithm, messaging, Kundali AI, basic safety | Verification/safety/reporting | implemented |
| 737 | Auth | General / strategy / context | n/a |
| 738 | OTP-based (Fast2SMS), JWT sessions in KV | OTP authentication | implemented |
| 739 | Pricing | General / strategy / context | n/a |
| 740 | Decided post-launch based on conversion data | General / strategy / context | n/a |
| 741 | Target Capacity | General / strategy / context | n/a |
| 742 | 5,000 DAU comfortably, 50,000 registered profiles | General / strategy / context | n/a |
| 743 | Monthly Cost | General / strategy / context | n/a |
| 744 | 11.2 Phase 1 — Growth ($5 / Month) | General / strategy / context | n/a |
| 745 | Trigger: | General / strategy / context | n/a |
| 746 | Upgrade to Workers Paid ($5/month) when approaching 100K req/day free tier limit. This single upgrade 100x-es capacity. | General / strategy / context | n/a |
| 747 | Upgrade | General / strategy / context | n/a |
| 748 | New Capability | General / strategy / context | n/a |
| 749 | Workers Paid ($5/month) | General / strategy / context | n/a |
| 750 | 10M requests/month. Durable Objects available at scale. Hyperdrive for D1 acceleration. | General / strategy / context | n/a |
| 751 | R2 Paid (usage-based) | General / strategy / context | n/a |
| 752 | 10GB free → unlimited. ₹1.5/GB/month beyond free tier. | General / strategy / context | n/a |
| 753 | Turso Paid ($29/month) | General / strategy / context | n/a |
| 754 | If needed: 100GB SQLite, multi-region read replicas for < 5ms global DB reads. | General / strategy / context | n/a |
| 755 | Resend Pro ($20/month) | General / strategy / context | n/a |
| 756 | 50,000 emails/month. Custom domain. Higher deliverability. | General / strategy / context | n/a |
| 757 | Total | General / strategy / context | n/a |
| 758 | ~$54/month (~₹4,500) supporting 50,000+ DAU | General / strategy / context | n/a |
| 759 | 11.3 Phase 2 — Monetisation & Scale ($500 / Month) | General / strategy / context | n/a |
| 760 | Revenue Stream | General / strategy / context | n/a |
| 761 | Premium Subscriptions | Subscriptions and payments | implemented |
| 762 | Silver/Gold/Platinum tiers. Pricing set based on conversion data from Phase 0. | General / strategy / context | n/a |
| 763 | Premium identity verification — ₹299–₹999 one-time | Verification/safety/reporting | implemented |
| 764 | Profile Boost | General / strategy / context | n/a |
| 765 | ₹99–₹299 per 2-hour boost. High-margin, impulse purchase. | General / strategy / context | n/a |
| 766 | Kundali Pro Report | Verification/safety/reporting | implemented |
| 767 | ₹199 per full AI report — incremental upsell | Verification/safety/reporting | implemented |
| 768 | Wedding Marketplace | General / strategy / context | n/a |
| 769 | Affiliate revenue from venue, photographer, travel bookings | General / strategy / context | n/a |
| 770 | Turso Scale | General / strategy / context | n/a |
| 771 | Upgrade to handle 500K DAU global reads | General / strategy / context | n/a |
| 772 | Workers AI Usage | General / strategy / context | n/a |
| 773 | Workers AI usage scales with request volume — budget accordingly | General / strategy / context | n/a |
| 774 | Target | General / strategy / context | n/a |
| 775 | ₹50L+ MRR at 50,000 paying users × avg ₹1,000 ARPU | General / strategy / context | n/a |
| 776 | 11.4 Phase 3 — Series A / Billion-Dollar Path | General / strategy / context | n/a |
| 777 | Vector | General / strategy / context | n/a |
| 778 | Community Expansion | General / strategy / context | n/a |
| 779 | Launch community-specific sub-brands: VivahTamil, VivahMuslim, VivahSikh — each powered by same infra | General / strategy / context | n/a |
| 780 | Diaspora Markets | General / strategy / context | n/a |
| 781 | NRI communities in UAE, UK, USA, Canada, Australia — English-first UI, same backend | General / strategy / context | n/a |
| 782 | B2B API | General / strategy / context | n/a |
| 783 | Matrimony API for community organisations, temples, mosques, gurudwaras to power their own platforms | General / strategy / context | n/a |
| 784 | IPO Narrative | General / strategy / context | n/a |
| 785 | 'The only matrimony platform born at the edge, with zero legacy infra debt, serving 10M+ users at $500/month operational cost' — unmatched unit economics for public markets | General / strategy / context | n/a |
| 786 | Defensibility | General / strategy / context | n/a |
| 787 | Network effects (Vivah Circles) + community data flywheel + Kundali AI moat + Vivah Score brand | General / strategy / context | n/a |
| 788 | 11.5 Compliance Roadmap — DPDP Act & Legal | General / strategy / context | n/a |
| 789 | Requirement | General / strategy / context | n/a |
| 790 | Implementation Plan | Subscriptions and payments | implemented |
| 791 | DPDP Act 2023 (India) | General / strategy / context | n/a |
| 792 | Privacy policy drafted by launch. Consent management implemented in onboarding flow. Data Fiduciary registration: Phase 1. | Onboarding/profile wizard | implemented |
| 793 | Age Verification | Verification/safety/reporting | implemented |
| 794 | 18+ enforced at registration: DOB collected + Aadhaar eKYC confirms age. Under-18 DOB rejected server-side. | General / strategy / context | n/a |
| 795 | Data Localisation | General / strategy / context | n/a |
| 796 | D1 databases deployed in Cloudflare's India region (Mumbai PoP). R2 bucket: ap-south-1 binding. | General / strategy / context | n/a |
| 797 | Automated deletion pipeline on account delete request. Completion within 30 minutes. | General / strategy / context | n/a |
| 798 | Breach Notification | Notifications | implemented |
| 799 | Sentry + PagerDuty alert on anomalous query patterns. 72-hour DPDP notification SLA tracked. | Notifications | implemented |
| 800 | Payment Compliance | Subscriptions and payments | implemented |
| 801 | Razorpay handles PCI-DSS. GST 18% collected and remitted via Razorpay Tax. | Subscriptions and payments | implemented |
| 802 | Terms of Service | General / strategy / context | n/a |
| 803 | No dowry references policy. No marital status misrepresentation tolerance. Community Guidelines enforced. | General / strategy / context | n/a |
| 804 | Appendix — Master Feature Launch Checklist | General / strategy / context | n/a |
| 805 | Every feature below is implementable on the zero-cost Cloudflare stack. Use this as your sprint backlog. | General / strategy / context | n/a |
| 806 | MVP Sprint 1 (Week 1–4) | General / strategy / context | n/a |
| 807 | Mobile OTP registration + WhatsApp OTP fallback | OTP authentication | implemented |
| 808 | Basic profile creation — name, DOB, religion, caste, education, occupation | Onboarding/profile wizard | implemented |
| 809 | Photo upload to R2 with Workers AI NSFW moderation | Admin and analytics | implemented |
| 810 | Basic partner preferences | Onboarding/profile wizard | implemented |
| 811 | Daily match queue (rule-based v1) | General / strategy / context | n/a |
| 812 | Send / accept / decline interest | Interests/shortlist flow | implemented |
| 813 | In-app text messaging via Durable Objects WebSocket | General / strategy / context | n/a |
| 814 | Push notifications via FCM (Android) + APNS (iOS) | Notifications | implemented |
| 815 | Profile privacy controls (visibility toggles) | Verification/safety/reporting | implemented |
| 816 | Block and report user | Verification/safety/reporting | implemented |
| 817 | Sprint 2 (Week 5–8) | General / strategy / context | n/a |
| 818 | Semantic search with Vectorize embeddings | Search and filter matching | implemented |
| 819 | 40-filter advanced search | Search and filter matching | implemented |
| 820 | Kundali AI engine (WASM ephemeris + LLM interpretation) | General / strategy / context | n/a |
| 821 | Voice messages (R2 storage, Whisper transcription) | Realtime chat | implemented |
| 822 | Video calling via Durable Objects WebRTC signalling | Voice/video calls | implemented |
| 823 | AI profile writer (Llama-3.1 at edge) | General / strategy / context | n/a |
| 824 | Vivah Score™ computation | General / strategy / context | n/a |
| 825 | Bio-data PDF generation | General / strategy / context | n/a |
| 826 | Photo verification (face embedding via Workers AI) | Verification/safety/reporting | implemented |
| 827 | Sprint 3 (Week 9–12) | General / strategy / context | n/a |
| 828 | Aadhaar eKYC integration (UIDAI sandbox → production) | General / strategy / context | n/a |
| 829 | Family access login with approval workflow | General / strategy / context | n/a |
| 830 | Vivah Circles™ community groups | General / strategy / context | n/a |
| 831 | Shagun Board™ success stories wall | General / strategy / context | n/a |
| 832 | Match Timeline feature | General / strategy / context | n/a |
| 833 | Profile Boost (à la carte purchase) | General / strategy / context | n/a |
| 834 | Admin moderation panel | Admin and analytics | implemented |
| 835 | Analytics dashboard (PostHog + Analytics Engine) | Admin and analytics | implemented |
| 836 | Subscription plan infrastructure (Razorpay integration) | Subscriptions and payments | implemented |
| 837 | DPDP Act compliance — consent flows, data export, erasure | General / strategy / context | n/a |
| 838 | Sprint 4 (Week 13–16) — Launch & Beyond | General / strategy / context | n/a |
| 839 | Wedding Marketplace (venue, photographer affiliate links) | General / strategy / context | n/a |
| 840 | Voice Personality Match (experimental) | General / strategy / context | n/a |
| 841 | Real-time message translation (22 languages) | Realtime chat | implemented |
| 842 | Multilingual UI rollout (Hindi, Tamil, Telugu, Bengali priority) | General / strategy / context | n/a |
| 843 | A/B testing framework (PostHog feature flags) | General / strategy / context | n/a |
| 844 | SEO: Server-rendered profile pages on Cloudflare Pages for Google indexing | General / strategy / context | n/a |
| 845 | App Store Launch (iOS + Android) | General / strategy / context | n/a |
| 846 | PR: 'India's first edge-native matrimony platform' launch campaign | General / strategy / context | n/a |
| 847 | Built at the Edge. Designed for Bharat. Ready for the World. | General / strategy / context | n/a |
| 848 | © 2026 Vivah.App — Founder Confidential — All Rights Reserved | General / strategy / context | n/a |

### Matrimony_App_Complete_Documentation.docx

| # | Extracted line | Classified feature | Current status |
|---:|---|---|---|
| 1 | MATRIMONY APP | General / strategy / context | n/a |
| 2 | Complete Product & Technical Reference | General / strategy / context | n/a |
| 3 | End-to-End Documentation | General / strategy / context | n/a |
| 4 | Features • Architecture • Backend • Security • Safety | Verification/safety/reporting | implemented |
| 5 | Version 1.0 \| March 2026 | General / strategy / context | n/a |
| 6 | CONFIDENTIAL — INTERNAL DOCUMENT | General / strategy / context | n/a |
| 7 | Table of Contents | General / strategy / context | n/a |
| 8 | 1. Executive Overview & Market Landscape | General / strategy / context | n/a |
| 9 | 2. Core Application Architecture | General / strategy / context | n/a |
| 10 | 3. User Registration & Profile Management | General / strategy / context | n/a |
| 11 | 4. Matchmaking & Search Features | Search and filter matching | implemented |
| 12 | 5. Communication Features | General / strategy / context | n/a |
| 13 | 6. Privacy, Safety & Trust Features | Verification/safety/reporting | implemented |
| 14 | 7. Advanced & Premium Features | Subscriptions and payments | implemented |
| 15 | 8. Admin Panel & CMS | Admin and analytics | implemented |
| 16 | 9. Backend Technology Stack | General / strategy / context | n/a |
| 17 | 10. Database Design & Data Management | General / strategy / context | n/a |
| 18 | 11. Security Architecture & Protocols | General / strategy / context | n/a |
| 19 | 12. API Design & Integration | General / strategy / context | n/a |
| 20 | 13. Monetisation & Revenue Models | General / strategy / context | n/a |
| 21 | 14. Performance, Scalability & DevOps | General / strategy / context | n/a |
| 22 | 15. Compliance, Legal & Future Roadmap | General / strategy / context | n/a |
| 23 | 1.1 What Is a Matrimony App? | General / strategy / context | n/a |
| 24 | A matrimony app is a specialised digital platform that facilitates the process of finding a suitable life partner for marriage. Unlike casual dating applications, matrimony platforms focus exclusively on long-term, committed relationships and legally recognised marriage. They cater primarily to users who are serious about settling down, as well as to their families who often participate in the match-making process. | General / strategy / context | n/a |
| 25 | Matrimony apps bridge the gap between traditional arranged-marriage systems and modern, technology-driven matchmaking. They provide structured profiles, compatibility algorithms, community-based filtering, and secure communication channels — all within a trust-focused environment. | Search and filter matching | implemented |
| 26 | 1.2 Market Size & Growth | General / strategy / context | n/a |
| 27 | Metric | General / strategy / context | n/a |
| 28 | Data | General / strategy / context | n/a |
| 29 | Global Online Matrimony Market (2024) | General / strategy / context | n/a |
| 30 | USD 3.9 Billion (projected) | General / strategy / context | n/a |
| 31 | India Online Matrimony Revenue | General / strategy / context | n/a |
| 32 | ~USD 0.26 Billion (Statista 2024) | General / strategy / context | n/a |
| 33 | Global Market CAGR | General / strategy / context | n/a |
| 34 | 8.48% annually | General / strategy / context | n/a |
| 35 | Projected Market by 2027 | General / strategy / context | n/a |
| 36 | USD 6.9 Billion | General / strategy / context | n/a |
| 37 | Online Dating Users (India, 2024) | General / strategy / context | n/a |
| 38 | 22.7 Million+ (Statista) | General / strategy / context | n/a |
| 39 | BharatMatrimony Active Users | General / strategy / context | n/a |
| 40 | 3.5 Million+ | General / strategy / context | n/a |
| 41 | Shaadi.com Total Profiles | General / strategy / context | n/a |
| 42 | 20 Million+ | General / strategy / context | n/a |
| 43 | Mobile Traffic Share (Matrimony.com) | General / strategy / context | n/a |
| 44 | 70%+ of all traffic | General / strategy / context | n/a |
| 45 | 1.3 Key Market Drivers | General / strategy / context | n/a |
| 46 | Increasing smartphone penetration in Tier 2 and Tier 3 cities across India and South Asia | General / strategy / context | n/a |
| 47 | Growing comfort with digital platforms among millennials and Gen-Z seeking partners | General / strategy / context | n/a |
| 48 | Rising demand for community-specific (caste, religion, profession) matchmaking | Search and filter matching | implemented |
| 49 | Decline of traditional matchmaking intermediaries (pandits, agents) | Search and filter matching | implemented |
| 50 | Pandemic-driven digital acceleration — virtual meetings replaced in-person introductions | General / strategy / context | n/a |
| 51 | AI and ML-driven match recommendations increasing user satisfaction rates | General / strategy / context | n/a |
| 52 | Diaspora communities worldwide seeking culturally compatible partners | General / strategy / context | n/a |
| 53 | 1.4 Leading Matrimony Platforms | General / strategy / context | n/a |
| 54 | Platform | General / strategy / context | n/a |
| 55 | User Base | General / strategy / context | n/a |
| 56 | BharatMatrimony | General / strategy / context | n/a |
| 57 | India's most profitable matrimony platform; strict profile verification | Verification/safety/reporting | implemented |
| 58 | 3.5M+ active | General / strategy / context | n/a |
| 59 | Shaadi.com | General / strategy / context | n/a |
| 60 | Largest global profile database; cross-platform accessibility | General / strategy / context | n/a |
| 61 | 20M+ profiles | General / strategy / context | n/a |
| 62 | Jeevansathi | General / strategy / context | n/a |
| 63 | Strong North India presence; advanced privacy controls | Realtime chat | implemented |
| 64 | 8M+ users | General / strategy / context | n/a |
| 65 | eHarmony (US) | General / strategy / context | n/a |
| 66 | Compatibility-first algorithm; personality-based matching | Search and filter matching | implemented |
| 67 | 15M+ users | General / strategy / context | n/a |
| 68 | EliteMatrimony | General / strategy / context | n/a |
| 69 | Premium tier for professionals; curated matches | Subscriptions and payments | implemented |
| 70 | Invite-only | General / strategy / context | n/a |
| 71 | NikkahExplorer | General / strategy / context | n/a |
| 72 | Islamic matrimony; Halal-certified; global Muslim diaspora | General / strategy / context | n/a |
| 73 | 2M+ users | General / strategy / context | n/a |
| 74 | TamilMatrimony | General / strategy / context | n/a |
| 75 | Community-specific; Tamil diaspora worldwide | General / strategy / context | n/a |
| 76 | 5M+ users | General / strategy / context | n/a |
| 77 | 2.1 High-Level System Architecture | General / strategy / context | n/a |
| 78 | A modern matrimony application follows a layered, microservices-based architecture designed for high availability, horizontal scalability, and strict data isolation. The system can be logically divided into five primary tiers. | General / strategy / context | n/a |
| 79 | 2.1.1 Presentation Tier (Client Layer) | General / strategy / context | n/a |
| 80 | The client layer encompasses all user-facing interfaces including the native iOS app, native Android app, Progressive Web App (PWA), and the responsive web portal. This tier is responsible for rendering the UI, managing local state, handling user inputs, and communicating with the backend via RESTful or GraphQL APIs. | General / strategy / context | n/a |
| 81 | iOS App: Swift / SwiftUI with UIKit for legacy support | General / strategy / context | n/a |
| 82 | Android App: Kotlin with Jetpack Compose | General / strategy / context | n/a |
| 83 | Cross-Platform: Flutter (Dart) for simultaneous iOS/Android deployment | General / strategy / context | n/a |
| 84 | Web Portal: React.js with Next.js (SSR) for SEO-optimised profile pages | General / strategy / context | n/a |
| 85 | PWA: Service workers + Web Push for offline capability and fast load | Notifications | implemented |
| 86 | 2.1.2 API Gateway Layer | General / strategy / context | n/a |
| 87 | All client requests pass through an API Gateway (AWS API Gateway, Kong, or NGINX) that handles routing, rate limiting, authentication token validation, SSL termination, request transformation, and observability logging. This layer is the single entry point for all client-server communication. | General / strategy / context | n/a |
| 88 | 2.1.3 Business Logic Layer (Microservices) | General / strategy / context | n/a |
| 89 | The application backend is decomposed into independent microservices, each owning a distinct domain. Services communicate asynchronously via a message broker (Apache Kafka / RabbitMQ) and synchronously via gRPC or REST. | Realtime chat | implemented |
| 90 | Microservice | General / strategy / context | n/a |
| 91 | Responsibility | General / strategy / context | n/a |
| 92 | User Service | General / strategy / context | n/a |
| 93 | Registration, authentication, profile CRUD, account management | General / strategy / context | n/a |
| 94 | Matching Service | General / strategy / context | n/a |
| 95 | Algorithm execution, compatibility scoring, match queue management | Search and filter matching | implemented |
| 96 | Search Service | Search and filter matching | implemented |
| 97 | Full-text search, filter processing, Elasticsearch indexing | Search and filter matching | implemented |
| 98 | Messaging Service | General / strategy / context | n/a |
| 99 | Real-time chat, WebSocket management, message encryption | Realtime chat | implemented |
| 100 | Notification Service | Notifications | implemented |
| 101 | Push notifications, email alerts, SMS dispatching | Notifications | implemented |
| 102 | Media Service | General / strategy / context | n/a |
| 103 | Photo upload, compression, CDN delivery, AI moderation | Admin and analytics | implemented |
| 104 | Payment Service | Subscriptions and payments | implemented |
| 105 | Subscription management, payment gateway integration, invoicing | Subscriptions and payments | implemented |
| 106 | Verification Service | Verification/safety/reporting | implemented |
| 107 | ID checks, photo verification, mobile OTP validation | OTP authentication | implemented |
| 108 | Analytics Service | Admin and analytics | implemented |
| 109 | User behaviour tracking, A/B testing, reporting dashboards | Verification/safety/reporting | implemented |
| 110 | Admin Service | Admin and analytics | implemented |
| 111 | CMS, moderation tools, user management, fraud detection | Admin and analytics | implemented |
| 112 | Recommendation Engine | General / strategy / context | n/a |
| 113 | ML-based match suggestions, collaborative filtering | Search and filter matching | implemented |
| 114 | 2.1.4 Data Layer | General / strategy / context | n/a |
| 115 | Data persistence is managed through a polyglot approach — different storage engines are selected based on the specific characteristics of each data type. This ensures optimal read/write performance and scalability for each use case. | General / strategy / context | n/a |
| 116 | 2.1.5 Infrastructure Layer | General / strategy / context | n/a |
| 117 | The infrastructure runs on a multi-region cloud deployment (AWS / Google Cloud / Azure) with auto-scaling groups, load balancers, and geo-distributed CDN nodes. Container orchestration is managed by Kubernetes (EKS / GKE) with Helm charts for deployment management. | General / strategy / context | n/a |
| 118 | 2.2 Microservices Communication | General / strategy / context | n/a |
| 119 | Services interact using a combination of synchronous and asynchronous patterns to ensure loose coupling and fault tolerance. | General / strategy / context | n/a |
| 120 | Communication Pattern | General / strategy / context | n/a |
| 121 | Use Case | General / strategy / context | n/a |
| 122 | REST over HTTPS | General / strategy / context | n/a |
| 123 | Standard CRUD operations, profile fetches, search queries | Search and filter matching | implemented |
| 124 | gRPC (Protocol Buffers) | General / strategy / context | n/a |
| 125 | Internal service-to-service calls requiring low latency | General / strategy / context | n/a |
| 126 | WebSockets | General / strategy / context | n/a |
| 127 | Real-time messaging, live notifications, typing indicators | Notifications | implemented |
| 128 | Apache Kafka (async) | General / strategy / context | n/a |
| 129 | Event streaming: new match events, profile updates, audit logs | General / strategy / context | n/a |
| 130 | RabbitMQ (async) | General / strategy / context | n/a |
| 131 | Task queues: email dispatch, background jobs, AI jobs | General / strategy / context | n/a |
| 132 | GraphQL | General / strategy / context | n/a |
| 133 | Flexible data fetching for mobile clients to minimize payload | General / strategy / context | n/a |
| 134 | 2.3 System Design Principles | General / strategy / context | n/a |
| 135 | Single Responsibility: Each microservice owns exactly one bounded context | General / strategy / context | n/a |
| 136 | High Cohesion / Loose Coupling: Services interact through well-defined APIs | General / strategy / context | n/a |
| 137 | Event Sourcing: Critical state changes captured as immutable event streams | General / strategy / context | n/a |
| 138 | CQRS (Command Query Responsibility Segregation): Separate read and write models for profiles | General / strategy / context | n/a |
| 139 | Circuit Breaker Pattern: Hystrix / Resilience4J prevent cascade failures | General / strategy / context | n/a |
| 140 | Saga Pattern: Distributed transactions managed via choreography-based sagas | General / strategy / context | n/a |
| 141 | 12-Factor App: Configuration in environment, stateless processes, port binding | General / strategy / context | n/a |
| 142 | 3.1 Registration Flow | General / strategy / context | n/a |
| 143 | The registration process is carefully designed to be frictionless for new users while capturing enough data to power accurate matchmaking from the very first session. Registration can be completed in 3 to 5 minutes for basic profiles, with enrichment encouraged over subsequent sessions. | Search and filter matching | implemented |
| 144 | 3.1.1 Registration Channels | General / strategy / context | n/a |
| 145 | Mobile Number Registration (Primary): OTP-based verification via SMS / WhatsApp | OTP authentication | implemented |
| 146 | Email Registration: Verification link sent to email inbox | Verification/safety/reporting | implemented |
| 147 | Social Sign-On: Google OAuth 2.0, Facebook Login, Apple Sign-In (iOS mandatory) | General / strategy / context | n/a |
| 148 | Self Registration: User creating their own profile | General / strategy / context | n/a |
| 149 | Third-Party Registration: Parent, sibling, or friend creating a profile on behalf of the candidate | General / strategy / context | n/a |
| 150 | 3.1.2 Identity Verification at Onboarding | Onboarding/profile wizard | implemented |
| 151 | All new accounts are required to complete at least mobile number verification before accessing any matchmaking features. Premium accounts require additional identity verification as described in Section 6. | Search and filter matching | implemented |
| 152 | 3.2 Profile Architecture | General / strategy / context | n/a |
| 153 | The profile is the fundamental data object of the matrimony platform. It is a rich, structured entity with dozens of attributes that power both matchmaking algorithms and search filters. | Search and filter matching | implemented |
| 154 | 3.2.1 Basic Profile Attributes | General / strategy / context | n/a |
| 155 | Attribute Category | General / strategy / context | n/a |
| 156 | Fields Captured | General / strategy / context | n/a |
| 157 | Personal Details | General / strategy / context | n/a |
| 158 | Full name, date of birth, height, weight, complexion, physical disability | General / strategy / context | n/a |
| 159 | Location | General / strategy / context | n/a |
| 160 | Current city, home state, country, willingness to relocate | General / strategy / context | n/a |
| 161 | Religious / Cultural | General / strategy / context | n/a |
| 162 | Religion, caste, sub-caste, mother tongue, gotra, nakshatra | General / strategy / context | n/a |
| 163 | Education | General / strategy / context | n/a |
| 164 | Highest qualification, field of study, institution name, graduation year | General / strategy / context | n/a |
| 165 | Profession | General / strategy / context | n/a |
| 166 | Occupation type, employer name, income range (annual), profession category | General / strategy / context | n/a |
| 167 | Family Background | General / strategy / context | n/a |
| 168 | Father's occupation, mother's occupation, siblings, family type, family values | General / strategy / context | n/a |
| 169 | Lifestyle | General / strategy / context | n/a |
| 170 | Diet (vegetarian/non-veg/vegan), smoking, alcohol, hobbies, fitness activities | General / strategy / context | n/a |
| 171 | Partner Preferences | Onboarding/profile wizard | implemented |
| 172 | Age range, height range, education, income, location, religion, caste | General / strategy / context | n/a |
| 173 | Horoscope / Kundali | General / strategy / context | n/a |
| 174 | Rashi, Nakshatra, time of birth, Manglik status, Kundali chart upload | General / strategy / context | n/a |
| 175 | About Me | General / strategy / context | n/a |
| 176 | Free-text personal description (up to 500 characters) | General / strategy / context | n/a |
| 177 | 3.2.2 Photo Management | General / strategy / context | n/a |
| 178 | Photos are the single most important factor in user engagement and response rates. A robust photo management system is essential. | General / strategy / context | n/a |
| 179 | Upload up to 20 photos (primary photo + gallery) | General / strategy / context | n/a |
| 180 | In-app camera capture with real-time preview | General / strategy / context | n/a |
| 181 | Image cropping, brightness, and contrast editing tools | General / strategy / context | n/a |
| 182 | AI-powered face detection to reject non-face uploads | General / strategy / context | n/a |
| 183 | AI content moderation to detect inappropriate images (NSFW filter) | Search and filter matching | implemented |
| 184 | Automatic compression and WebP conversion for optimised delivery | General / strategy / context | n/a |
| 185 | Watermarking option to prevent screenshot sharing | General / strategy / context | n/a |
| 186 | Blurred photo option (visible only to users with consent) | General / strategy / context | n/a |
| 187 | Photo privacy: visible to All / Matches Only / Premium Members Only | Verification/safety/reporting | implemented |
| 188 | 3.2.3 Bio-Data / Profile PDF Generation | General / strategy / context | n/a |
| 189 | A key differentiator of matrimony apps vs. dating apps is the ability to generate a structured bio-data PDF. Users can generate a professionally formatted bio-data sheet from their profile data, which can be downloaded, printed, or shared with family members. This integrates a third-party PDF generation API and pulls structured data directly from the profile service. | General / strategy / context | n/a |
| 190 | 3.3 Profile Completeness & Gamification | General / strategy / context | n/a |
| 191 | Profile completion is driven through a progress indicator showing a percentage score, with contextual nudges to fill incomplete fields. A minimum completeness threshold (typically 70%) is required before the profile becomes visible in search results. Gamification elements include: | Search and filter matching | implemented |
| 192 | Profile Score badge displayed prominently on the profile card | General / strategy / context | n/a |
| 193 | Step-by-step guided completion wizard for new users | General / strategy / context | n/a |
| 194 | Push notifications prompting incomplete sections | Notifications | implemented |
| 195 | Horoscope completion unlocks the Kundali match feature | General / strategy / context | n/a |
| 196 | Verified badge awarded upon ID and photo verification | Verification/safety/reporting | implemented |
| 197 | 3.4 Profile Visibility & Privacy Controls | Verification/safety/reporting | implemented |
| 198 | Users have granular control over what information is visible and to whom. The privacy model operates at the field level, allowing selective disclosure. | Verification/safety/reporting | implemented |
| 199 | Privacy Setting | Verification/safety/reporting | implemented |
| 200 | Options | General / strategy / context | n/a |
| 201 | Profile Visibility | General / strategy / context | n/a |
| 202 | Public / Matches Only / Hidden | General / strategy / context | n/a |
| 203 | Photo Visibility | General / strategy / context | n/a |
| 204 | All / Connected Profiles / Premium Members / None | Subscriptions and payments | implemented |
| 205 | Contact Visibility | General / strategy / context | n/a |
| 206 | Visible after mutual interest / Premium unlock / Masked | Interests/shortlist flow | implemented |
| 207 | Last Active Timestamp | General / strategy / context | n/a |
| 208 | Visible to All / Matches Only / Hidden | General / strategy / context | n/a |
| 209 | Profile Viewed By | General / strategy / context | n/a |
| 210 | User can see who viewed their profile (Premium feature) | Subscriptions and payments | implemented |
| 211 | Horoscope Visibility | General / strategy / context | n/a |
| 212 | Salary / Income | General / strategy / context | n/a |
| 213 | Optional disclosure; can show as range rather than exact | General / strategy / context | n/a |
| 214 | 4.1 Matchmaking Algorithms | Search and filter matching | implemented |
| 215 | The core value proposition of a matrimony platform is the quality of its match recommendations. Modern platforms employ a multi-layered algorithmic approach combining rule-based filtering with machine learning models. | Search and filter matching | implemented |
| 216 | 4.1.1 Rule-Based Filter Engine | Search and filter matching | implemented |
| 217 | The first pass uses hard filters derived directly from user-specified partner preferences. Only profiles satisfying all mandatory criteria proceed to the scoring phase. This ensures no obviously incompatible profiles are shown. | Onboarding/profile wizard | implemented |
| 218 | 4.1.2 Weighted Compatibility Scoring | Search and filter matching | implemented |
| 219 | Each shortlisted profile receives a compatibility score computed from a weighted combination of attributes. The weight configuration is tunable per platform and personalised per user over time. | Search and filter matching | implemented |
| 220 | Matching Factor | General / strategy / context | n/a |
| 221 | Default Weight | General / strategy / context | n/a |
| 222 | Religion & Caste | General / strategy / context | n/a |
| 223 | Age Range Match | General / strategy / context | n/a |
| 224 | Education Compatibility | Search and filter matching | implemented |
| 225 | Location / Relocation Willingness | General / strategy / context | n/a |
| 226 | Income Range Compatibility | Search and filter matching | implemented |
| 227 | Family Values Alignment | General / strategy / context | n/a |
| 228 | Horoscope Compatibility (Kundali) | Search and filter matching | implemented |
| 229 | Lifestyle Compatibility | Search and filter matching | implemented |
| 230 | Physical Preference Match | General / strategy / context | n/a |
| 231 | 4.1.3 Machine Learning-Based Recommendation | General / strategy / context | n/a |
| 232 | Beyond rule-based matching, ML models continuously learn from implicit and explicit user feedback signals to refine recommendations. The recommendation engine uses collaborative filtering and content-based filtering techniques. | Search and filter matching | implemented |
| 233 | Collaborative Filtering: Users similar to you expressed interest in profiles like X | Search and filter matching | implemented |
| 234 | Content-Based Filtering: You liked profiles with attribute Y, suggesting more like Y | Search and filter matching | implemented |
| 235 | Implicit Signals: Time spent on a profile, number of photos viewed, message sent | Realtime chat | implemented |
| 236 | Explicit Signals: Interest sent, favourite saved, interest declined | Interests/shortlist flow | implemented |
| 237 | Kundali AI: NLP-based horoscope compatibility scoring using Vedic astrology rules | Search and filter matching | implemented |
| 238 | Churn Prediction: Identifies users at risk of leaving and boosts their daily recommendations | General / strategy / context | n/a |
| 239 | 4.2 Search & Discovery Features | Search and filter matching | implemented |
| 240 | In addition to curated recommendations, users actively search using a rich filter set. The search layer is powered by Elasticsearch for full-text and geo-spatial querying at scale. | Search and filter matching | implemented |
| 241 | 4.2.1 Basic Search Filters | Search and filter matching | implemented |
| 242 | Age range (From – To) | General / strategy / context | n/a |
| 243 | Height range (From – To in feet/cm) | General / strategy / context | n/a |
| 244 | Religion and Caste / Community | General / strategy / context | n/a |
| 245 | Education level and field | General / strategy / context | n/a |
| 246 | Profession / Employment type | General / strategy / context | n/a |
| 247 | Income range (annual) | General / strategy / context | n/a |
| 248 | Marital status (Never Married / Divorced / Widowed) | General / strategy / context | n/a |
| 249 | 4.2.2 Advanced Search Filters | Search and filter matching | implemented |
| 250 | Language / Mother tongue | General / strategy / context | n/a |
| 251 | Country / State / City of residence | General / strategy / context | n/a |
| 252 | Willingness to relocate (Yes / No / Anywhere) | General / strategy / context | n/a |
| 253 | Diet preference (Vegetarian / Non-Vegetarian / Vegan / Jain) | General / strategy / context | n/a |
| 254 | Manglik / Non-Manglik (horoscope compatibility) | Search and filter matching | implemented |
| 255 | Physical appearance (skin tone, body type) — optional fields | General / strategy / context | n/a |
| 256 | Profile completeness threshold | General / strategy / context | n/a |
| 257 | Verified profile only filter | Search and filter matching | implemented |
| 258 | Recently joined (New profiles in last 7 / 30 days) | General / strategy / context | n/a |
| 259 | Members who are online now | General / strategy / context | n/a |
| 260 | 4.2.3 Location-Based Discovery | General / strategy / context | n/a |
| 261 | GPS / geolocation integration allows users to discover profiles near their current location or within a configured radius. This is particularly useful for diaspora communities or users looking for matches within the same city. | General / strategy / context | n/a |
| 262 | Radius-based search (10 km, 25 km, 50 km, 100 km, anywhere) | Search and filter matching | implemented |
| 263 | City-level and state-level search | Search and filter matching | implemented |
| 264 | Country-level filter for NRI / international profiles | Search and filter matching | implemented |
| 265 | 'Near Me' widget on the home screen showing proximity-sorted profiles | General / strategy / context | n/a |
| 266 | 4.3 Daily Recommendations & Match Queue | General / strategy / context | n/a |
| 267 | Every user receives a curated Daily Match queue — a personalised list of recommended profiles refreshed every 24 hours. The queue size varies by subscription tier (typically 5–10 for free, 20–50 for premium). Key sub-features include: | Subscriptions and payments | implemented |
| 268 | Top Picks: Highest-compatibility profiles of the day | Search and filter matching | implemented |
| 269 | Premium Matches: Elite/verified profiles surfaced for premium members | Subscriptions and payments | implemented |
| 270 | Community Matches: Same-community profiles prioritised by community algorithm | General / strategy / context | n/a |
| 271 | NRI Matches: International profiles filtered by country of residence | Search and filter matching | implemented |
| 272 | Recently Joined: Fresh profiles added in the last 7 days | General / strategy / context | n/a |
| 273 | Profile of the Day: Featured premium match highlighted at top of feed | Subscriptions and payments | implemented |
| 274 | 4.4 Horoscope / Kundali Matching | General / strategy / context | n/a |
| 275 | Kundali (horoscope) matching is a critical feature for South Asian users. The system implements the Vedic 36-Gun (points) system to calculate compatibility between two horoscopes. | Search and filter matching | implemented |
| 276 | Guna Milan: 36-point compatibility check across 8 kootas (Varna, Vashya, Tara, Yoni, Graha Maitri, Gana, Bhakoot, Nadi) | Search and filter matching | implemented |
| 277 | Manglik Dosha Detection: Automatic identification and matching suggestion | General / strategy / context | n/a |
| 278 | Kundali Chart Display: Visual chakra-style chart rendered from birth details | General / strategy / context | n/a |
| 279 | AI-Based Kundali Interpretation: Natural language summary of compatibility score | Search and filter matching | implemented |
| 280 | Third-Party Astrologer Integration: Users can book a live horoscope consultation | General / strategy / context | n/a |
| 281 | 5.1 Interest & Connection Workflow | Interests/shortlist flow | implemented |
| 282 | Communication in a matrimony app typically follows a structured, consent-driven workflow that protects user privacy and enforces mutual interest before direct contact is possible. | Interests/shortlist flow | implemented |
| 283 | User A views User B's profile and clicks 'Send Interest' | Interests/shortlist flow | implemented |
| 284 | User B receives a push notification and in-app alert | Notifications | implemented |
| 285 | User B accepts or declines the interest | Interests/shortlist flow | implemented |
| 286 | Upon acceptance, a connection is established and chat is unlocked | Realtime chat | implemented |
| 287 | Premium users may unlock direct messaging without waiting for acceptance | Subscriptions and payments | implemented |
| 288 | Contact details (mobile / email) are revealed only after mutual connection | General / strategy / context | n/a |
| 289 | 5.2 In-App Messaging | General / strategy / context | n/a |
| 290 | The messaging module is built on WebSocket technology (Socket.io / native WebSocket protocol) with a message broker backend for reliable delivery. Messages are end-to-end encrypted using AES-256 with per-conversation keys. | Realtime chat | implemented |
| 291 | 5.2.1 Text Messaging Features | General / strategy / context | n/a |
| 292 | Real-time message delivery with read receipts (single tick → double tick → blue tick) | Realtime chat | implemented |
| 293 | Typing indicator ('User is typing...') | General / strategy / context | n/a |
| 294 | Message reactions (emoji reactions to individual messages) | Realtime chat | implemented |
| 295 | Reply-to-message threading | Realtime chat | implemented |
| 296 | Message forwarding (within platform only, not to external apps) | Realtime chat | implemented |
| 297 | Message deletion (for self / for everyone within 24 hours) | Realtime chat | implemented |
| 298 | Message search within conversation history | Search and filter matching | implemented |
| 299 | Conversation archiving | General / strategy / context | n/a |
| 300 | Offline message queuing — messages delivered on reconnection | Realtime chat | implemented |
| 301 | 5.2.2 Rich Media Messaging | General / strategy / context | n/a |
| 302 | Photo sharing (from gallery or in-app camera) — subject to privacy settings | Verification/safety/reporting | implemented |
| 303 | Voice messages (up to 5 minutes) | Realtime chat | implemented |
| 304 | Document sharing (bio-data PDF, horoscope PDF) | General / strategy / context | n/a |
| 305 | Location sharing (optional, for suggesting meeting place) | General / strategy / context | n/a |
| 306 | 5.3 Video Calling | Voice/video calls | implemented |
| 307 | Video calling allows users to conduct virtual 'first meetings' within the app, avoiding the need to share personal phone numbers at an early stage. This is especially critical for long-distance or international matches. | Voice/video calls | implemented |
| 308 | In-app WebRTC-based video calling (no third-party app needed) | Voice/video calls | implemented |
| 309 | Audio-only call option | General / strategy / context | n/a |
| 310 | Call recording is strictly disabled for privacy protection | Verification/safety/reporting | implemented |
| 311 | Call quality adapts to network bandwidth (adaptive bitrate) | General / strategy / context | n/a |
| 312 | Virtual backgrounds to maintain privacy of home environment | Verification/safety/reporting | implemented |
| 313 | Screen sharing capability (for showing documents to family members) | General / strategy / context | n/a |
| 314 | Call scheduling — send a video meeting invite with date and time | General / strategy / context | n/a |
| 315 | Group video call (up to 5 participants — for family introductions) | Voice/video calls | implemented |
| 316 | 5.4 Chatbot & AI Conversation Assistant | Realtime chat | implemented |
| 317 | An AI-powered assistant (chatbot) guides new users through the onboarding process, helps them craft their first message to a potential match, and answers platform-related questions. The chatbot leverages a large language model fine-tuned on matrimony-specific contexts. | Onboarding/profile wizard | implemented |
| 318 | Onboarding wizard: Step-by-step profile completion guide | Onboarding/profile wizard | implemented |
| 319 | Ice-breaker suggestions: AI-suggested conversation starters for new matches | General / strategy / context | n/a |
| 320 | Response templates: Pre-written responses users can customise and send | General / strategy / context | n/a |
| 321 | FAQ assistant: Answers questions about features, subscriptions, privacy | Verification/safety/reporting | implemented |
| 322 | Sentiment analysis on incoming messages to flag potentially abusive content | Realtime chat | implemented |
| 323 | 5.5 Communication Restrictions & Anti-Spam | General / strategy / context | n/a |
| 324 | To prevent harassment and spam, communication is governed by platform rules enforced at the API level: | General / strategy / context | n/a |
| 325 | Free users can send a maximum of 5 interests per day | Interests/shortlist flow | implemented |
| 326 | Unmatched profiles cannot send direct messages | Realtime chat | implemented |
| 327 | Auto-detection of contact number sharing in chat (phone numbers masked or blocked) | Realtime chat | implemented |
| 328 | Profanity filter on all outgoing messages using NLP-based classifier | Search and filter matching | implemented |
| 329 | Spam detection: Unusual mass-messaging patterns trigger account review | General / strategy / context | n/a |
| 330 | Rate limiting: Maximum messages per minute enforced per user account | Realtime chat | implemented |
| 331 | 6.1 Identity Verification Framework | Verification/safety/reporting | implemented |
| 332 | Trust is the single most critical differentiator of a matrimony platform from a casual dating app. A multi-tier verification system ensures profile authenticity and reduces fake profiles. | Verification/safety/reporting | implemented |
| 333 | 6.1.1 Verification Tiers | Verification/safety/reporting | implemented |
| 334 | Tier | General / strategy / context | n/a |
| 335 | Verification Steps | Verification/safety/reporting | implemented |
| 336 | Basic (All Users) | General / strategy / context | n/a |
| 337 | Mobile OTP verification at registration | OTP authentication | implemented |
| 338 | Verified (Standard) | General / strategy / context | n/a |
| 339 | Email verification + profile review by moderation team | Verification/safety/reporting | implemented |
| 340 | ID Verified | General / strategy / context | n/a |
| 341 | Government-issued ID upload (Aadhaar, PAN, Passport, Driver's Licence) — AI OCR extraction + human review | General / strategy / context | n/a |
| 342 | Photo Verified | General / strategy / context | n/a |
| 343 | Live selfie compared to profile photo using facial recognition AI | General / strategy / context | n/a |
| 344 | Employment Verified | General / strategy / context | n/a |
| 345 | Employer verification email link / offer letter upload | Verification/safety/reporting | implemented |
| 346 | Education Verified | General / strategy / context | n/a |
| 347 | Degree certificate upload + DigiLocker API (India) integration | General / strategy / context | n/a |
| 348 | Elite Verified | General / strategy / context | n/a |
| 349 | All above verifications + background check via third-party service | Verification/safety/reporting | implemented |
| 350 | 6.1.2 AI-Powered Fake Profile Detection | General / strategy / context | n/a |
| 351 | The platform employs a multi-signal fraud detection system that runs continuously on all new and existing profiles. | General / strategy / context | n/a |
| 352 | Duplicate face detection: Same face appearing across multiple accounts | General / strategy / context | n/a |
| 353 | Photo reverse-search: Detects stock photos, celebrity images, or stolen photos | Search and filter matching | implemented |
| 354 | Device fingerprinting: Multiple accounts from same device flagged for review | General / strategy / context | n/a |
| 355 | Behaviour analysis: Unusual interaction patterns (mass copying of profile text) flagged | General / strategy / context | n/a |
| 356 | Language pattern analysis: Detection of scripted/template messages | Realtime chat | implemented |
| 357 | IP geolocation cross-check: Profile location vs. login location discrepancy alerts | General / strategy / context | n/a |
| 358 | 6.2 Privacy Controls | Verification/safety/reporting | implemented |
| 359 | Every user has comprehensive control over their data visibility and interaction permissions. | General / strategy / context | n/a |
| 360 | Privacy Control | Verification/safety/reporting | implemented |
| 361 | Description | General / strategy / context | n/a |
| 362 | Anonymous Browsing | General / strategy / context | n/a |
| 363 | Browse profiles without appearing in their 'Profile Viewed By' list (Premium) | Subscriptions and payments | implemented |
| 364 | Block User | Verification/safety/reporting | implemented |
| 365 | Blocked users cannot view your profile, send interests, or message you | Interests/shortlist flow | implemented |
| 366 | Report User | Verification/safety/reporting | implemented |
| 367 | Report inappropriate behaviour with category selection and evidence upload | Verification/safety/reporting | implemented |
| 368 | Hide Profile | General / strategy / context | n/a |
| 369 | Temporarily deactivate profile from search without deleting account | Search and filter matching | implemented |
| 370 | Delete Account | General / strategy / context | n/a |
| 371 | Permanent account deletion with data erasure per GDPR / PDPA rights | General / strategy / context | n/a |
| 372 | Contact Masking | General / strategy / context | n/a |
| 373 | Mobile numbers masked until mutual connection and explicit consent | General / strategy / context | n/a |
| 374 | Photo Blur | General / strategy / context | n/a |
| 375 | Profile photos blurred by default; revealed only on user approval | General / strategy / context | n/a |
| 376 | Incognito Mode | General / strategy / context | n/a |
| 377 | View profiles without leaving any trace (Ultra-Premium feature) | Subscriptions and payments | implemented |
| 378 | Shortlist Privacy | Interests/shortlist flow | implemented |
| 379 | Other users cannot see who shortlisted them (Premium) | Interests/shortlist flow | implemented |
| 380 | 6.3 Anti-Harassment & Safety Tools | Verification/safety/reporting | implemented |
| 381 | One-click block: Immediate blocking from any profile view or chat screen | Realtime chat | implemented |
| 382 | Report categories: Fake profile / Inappropriate content / Harassment / Spam / Suspicious behaviour | Verification/safety/reporting | implemented |
| 383 | Emergency contact: Option to share current location with a trusted contact while on a date arranged through the app | General / strategy / context | n/a |
| 384 | Safe meeting guide: In-app tips for first meetings; recommends public locations | General / strategy / context | n/a |
| 385 | Voice note safety: AI scans voice messages for explicit content before delivery | Realtime chat | implemented |
| 386 | Community moderation: Trusted members of large communities can flag suspicious profiles | Admin and analytics | implemented |
| 387 | Screenshot prevention: Platform attempts to disable screenshots on sensitive screens (message thread, contact reveal) | Realtime chat | implemented |
| 388 | 6.4 Content Moderation System | Admin and analytics | implemented |
| 389 | All user-generated content (profile photos, bio text, messages) passes through a moderation pipeline before reaching other users. | Realtime chat | implemented |
| 390 | Content Type | General / strategy / context | n/a |
| 391 | Moderation Method | Admin and analytics | implemented |
| 392 | Profile Photos | General / strategy / context | n/a |
| 393 | AI NSFW classification + human review queue for borderline cases | General / strategy / context | n/a |
| 394 | Profile Bio Text | General / strategy / context | n/a |
| 395 | NLP-based keyword flagging + profanity filter + human review | Search and filter matching | implemented |
| 396 | Chat Messages | Realtime chat | implemented |
| 397 | Real-time NLP classifier + contact-sharing detection | General / strategy / context | n/a |
| 398 | Uploaded Documents | General / strategy / context | n/a |
| 399 | File scanning for malware + AI OCR for ID validation | General / strategy / context | n/a |
| 400 | Voice Messages | Realtime chat | implemented |
| 401 | Speech-to-text transcription + NLP safety check before delivery | Verification/safety/reporting | implemented |
| 402 | Video Calls | Voice/video calls | implemented |
| 403 | No recording; real-time AI analysis flags suspicious calls for post-call review | General / strategy / context | n/a |
| 404 | 7.1 Premium Membership Tiers | Subscriptions and payments | implemented |
| 405 | Matrimony apps typically offer tiered subscription plans, each unlocking progressively more powerful features. The freemium model ensures wide top-of-funnel user acquisition while monetising engaged users through premium conversions. | Subscriptions and payments | implemented |
| 406 | Feature | General / strategy / context | n/a |
| 407 | Free | General / strategy / context | n/a |
| 408 | Premium / Gold / Platinum | Subscriptions and payments | implemented |
| 409 | Daily Match Recommendations | General / strategy / context | n/a |
| 410 | 5 profiles | General / strategy / context | n/a |
| 411 | 50–100 profiles | General / strategy / context | n/a |
| 412 | Send Interests per Day | Interests/shortlist flow | implemented |
| 413 | Unlimited | General / strategy / context | n/a |
| 414 | Initiate Chat | Realtime chat | implemented |
| 415 | No (must wait for match) | General / strategy / context | n/a |
| 416 | Yes (direct message) | Realtime chat | implemented |
| 417 | View Contact Numbers | General / strategy / context | n/a |
| 418 | Yes (post connection) | General / strategy / context | n/a |
| 419 | View Profile Visitors | General / strategy / context | n/a |
| 420 | Blurred | General / strategy / context | n/a |
| 421 | Full quality | General / strategy / context | n/a |
| 422 | Advanced Filters | Search and filter matching | implemented |
| 423 | Basic only | General / strategy / context | n/a |
| 424 | All filters including income, education | Search and filter matching | implemented |
| 425 | Kundali Matching | General / strategy / context | n/a |
| 426 | Basic score | General / strategy / context | n/a |
| 427 | Full 36-gun report + AI interpretation | Verification/safety/reporting | implemented |
| 428 | Priority in Search | Search and filter matching | implemented |
| 429 | Normal ranking | General / strategy / context | n/a |
| 430 | Boosted to top | General / strategy / context | n/a |
| 431 | Relationship Manager | General / strategy / context | n/a |
| 432 | Dedicated RM (Platinum only) | General / strategy / context | n/a |
| 433 | Video Date Feature | General / strategy / context | n/a |
| 434 | Profile Boost | General / strategy / context | n/a |
| 435 | Weekly boost included | General / strategy / context | n/a |
| 436 | WhatsApp Integration | General / strategy / context | n/a |
| 437 | Yes (share profile to WhatsApp) | General / strategy / context | n/a |
| 438 | 7.2 Profile Boost | General / strategy / context | n/a |
| 439 | Profile Boost temporarily promotes a user's profile to the top of search results and daily recommendation feeds of compatible users. Boost visibility windows are typically 30 minutes to 2 hours. Metrics visible to the user during a boost include profiles reached, views received, interests received, and contacts unlocked. This feature can be purchased à la carte or included in premium plans. | Search and filter matching | implemented |
| 440 | 7.3 Assisted Matchmaking (Relationship Manager) | Search and filter matching | implemented |
| 441 | Ultra-premium users (EliteMatrimony style) are assigned a dedicated human Relationship Manager who provides white-glove matchmaking services. | Search and filter matching | implemented |
| 442 | Personal profile consultation and profile enhancement guidance | General / strategy / context | n/a |
| 443 | Curated shortlist of handpicked profiles delivered weekly | Interests/shortlist flow | implemented |
| 444 | Introduction arranged with verified matches on behalf of the user | General / strategy / context | n/a |
| 445 | First meeting preparation tips and coaching | General / strategy / context | n/a |
| 446 | Ongoing support throughout the courtship and family introduction process | General / strategy / context | n/a |
| 447 | Dedicated support line (phone, WhatsApp, email) | General / strategy / context | n/a |
| 448 | 7.4 Astrology & Compatibility Tools | Search and filter matching | implemented |
| 449 | Full Kundali Chart generation from date, time, and place of birth | General / strategy / context | n/a |
| 450 | 36-Gun Milan compatibility report with detailed koota breakdown | Search and filter matching | implemented |
| 451 | Nakshatra and Rashi matching | General / strategy / context | n/a |
| 452 | Manglik dosha report and remedial suggestions | Verification/safety/reporting | implemented |
| 453 | Lucky colours, wedding date suggestions (muhurat recommendations) | General / strategy / context | n/a |
| 454 | Live astrologer consultation booking through in-app marketplace | General / strategy / context | n/a |
| 455 | 7.5 Social & Family Features | General / strategy / context | n/a |
| 456 | Unlike dating apps, matrimony decisions involve family participation. These features support a family-inclusive approach. | General / strategy / context | n/a |
| 457 | Family Access Login: Secondary login for parents with read-only profile visibility | General / strategy / context | n/a |
| 458 | Share Profile: Generate a shareable link or WhatsApp card for a shortlisted profile | Interests/shortlist flow | implemented |
| 459 | Family Video Call: Group video call with up to 10 participants for family introductions | Voice/video calls | implemented |
| 460 | Family Shortlist: Family members can co-shortlist profiles and share notes | Interests/shortlist flow | implemented |
| 461 | Connect Request: Formal request sent with a personal message to other family | Realtime chat | implemented |
| 462 | 7.6 AI-Powered Features | General / strategy / context | n/a |
| 463 | Smart Profile Writing: AI drafts the 'About Me' section based on filled attributes | General / strategy / context | n/a |
| 464 | Profile Score Optimiser: AI suggests specific improvements to boost visibility | General / strategy / context | n/a |
| 465 | Conversation Coach: AI analyses sent messages and suggests improvements | Realtime chat | implemented |
| 466 | Match Insight Report: Weekly AI report summarising your match activity and recommendations | Verification/safety/reporting | implemented |
| 467 | Churn Prevention Nudges: Personalised re-engagement messages when user goes inactive | Realtime chat | implemented |
| 468 | Dynamic Pricing: AI-optimised subscription offer timing based on user engagement score | Subscriptions and payments | implemented |
| 469 | 8. Admin Panel & Content Management System | Admin and analytics | implemented |
| 470 | 8.1 Admin Dashboard Overview | Admin and analytics | implemented |
| 471 | The admin panel is a secure web-based control centre accessible only to authorised internal team members. It provides comprehensive visibility and control over all aspects of the platform — user management, content moderation, analytics, financial management, and system health monitoring. | Admin and analytics | implemented |
| 472 | 8.2 User Management Module | General / strategy / context | n/a |
| 473 | Comprehensive user search with multi-parameter filters (name, mobile, ID, registration date, subscription status) | Search and filter matching | implemented |
| 474 | Profile view and edit: Admins can view all user fields including hidden data for moderation purposes | Admin and analytics | implemented |
| 475 | Account status management: Activate / Deactivate / Suspend / Permanently ban accounts | General / strategy / context | n/a |
| 476 | Verification management: Approve or reject uploaded identity documents | Verification/safety/reporting | implemented |
| 477 | Impersonation mode: Admins can view the app as a specific user for support debugging | Admin and analytics | implemented |
| 478 | Bulk actions: Bulk deactivate, bulk export user data for compliance reports | Verification/safety/reporting | implemented |
| 479 | Activity log: Full audit trail of all admin actions against user accounts | Admin and analytics | implemented |
| 480 | 8.3 Content Moderation Queue | Admin and analytics | implemented |
| 481 | The moderation queue is the primary workspace for the trust and safety team. It aggregates flagged content from automated systems and user reports for human review. | Verification/safety/reporting | implemented |
| 482 | Queue | General / strategy / context | n/a |
| 483 | Content | General / strategy / context | n/a |
| 484 | Photo Review Queue | General / strategy / context | n/a |
| 485 | New profile photos pending AI + human moderation | Admin and analytics | implemented |
| 486 | ID Verification Queue | Verification/safety/reporting | implemented |
| 487 | Uploaded government IDs pending review and approval | General / strategy / context | n/a |
| 488 | Report Review Queue | Verification/safety/reporting | implemented |
| 489 | User-submitted reports of fake profiles or harassment | Verification/safety/reporting | implemented |
| 490 | Bio Text Review Queue | General / strategy / context | n/a |
| 491 | Profile descriptions flagged by NLP filter | Search and filter matching | implemented |
| 492 | Appeal Queue | General / strategy / context | n/a |
| 493 | Suspended users appealing their account status | General / strategy / context | n/a |
| 494 | Chat Escalation Queue | Realtime chat | implemented |
| 495 | Conversations flagged by AI for potential policy violations | General / strategy / context | n/a |
| 496 | 8.4 Content Management System (CMS) | General / strategy / context | n/a |
| 497 | Homepage banner and featured profile management | General / strategy / context | n/a |
| 498 | Success stories content management (publish, edit, unpublish wedding stories) | General / strategy / context | n/a |
| 499 | Push notification campaign management: Schedule and send targeted push campaigns | Notifications | implemented |
| 500 | Email marketing integration: Create and dispatch email newsletters to segmented user lists | General / strategy / context | n/a |
| 501 | Blog and article management: SEO-optimised content for organic traffic | General / strategy / context | n/a |
| 502 | App copy management: Edit UI strings, error messages, and onboarding content without code deploy | Onboarding/profile wizard | implemented |
| 503 | Localisation management: Translate and manage content across 15+ supported languages | General / strategy / context | n/a |
| 504 | 8.5 Analytics & Reporting Dashboard | Verification/safety/reporting | implemented |
| 505 | The analytics module provides real-time and historical data across all key business metrics, powering data-driven decision making. | Admin and analytics | implemented |
| 506 | Metric Category | General / strategy / context | n/a |
| 507 | Key KPIs | Admin and analytics | implemented |
| 508 | Frequency | General / strategy / context | n/a |
| 509 | User Acquisition | General / strategy / context | n/a |
| 510 | New registrations, source attribution, cost per registration | General / strategy / context | n/a |
| 511 | Daily | General / strategy / context | n/a |
| 512 | Engagement | General / strategy / context | n/a |
| 513 | DAU/MAU, session duration, match interaction rate | General / strategy / context | n/a |
| 514 | Daily / Weekly | General / strategy / context | n/a |
| 515 | Conversion | General / strategy / context | n/a |
| 516 | Free-to-paid conversion rate, subscription revenue, ARPU | Subscriptions and payments | implemented |
| 517 | Retention | General / strategy / context | n/a |
| 518 | Day-1/7/30 retention, churn rate, resurrection rate | General / strategy / context | n/a |
| 519 | Weekly | General / strategy / context | n/a |
| 520 | Safety | Verification/safety/reporting | implemented |
| 521 | Reports filed, fake profiles detected, response time | Verification/safety/reporting | implemented |
| 522 | Match Quality | General / strategy / context | n/a |
| 523 | Mutual connections, video calls completed, success stories | Voice/video calls | implemented |
| 524 | Monthly | General / strategy / context | n/a |
| 525 | 8.6 Subscription & Revenue Management | Subscriptions and payments | implemented |
| 526 | Subscription plan creation and pricing management | Subscriptions and payments | implemented |
| 527 | Coupon and discount code generation and tracking | General / strategy / context | n/a |
| 528 | Refund and dispute management workflow | General / strategy / context | n/a |
| 529 | Revenue dashboards: MRR, ARR, churn rate, LTV per customer segment | Admin and analytics | implemented |
| 530 | Payment failure analysis and retry management | Subscriptions and payments | implemented |
| 531 | Tax and GST reporting (configurable by geography) | Verification/safety/reporting | implemented |
| 532 | 9.1 Recommended Technology Stack | General / strategy / context | n/a |
| 533 | The following technology stack represents the industry-standard choices used by large-scale matrimony platforms and is recommended for new platforms targeting 100,000+ concurrent users. | General / strategy / context | n/a |
| 534 | 9.1.1 Frontend & Mobile | General / strategy / context | n/a |
| 535 | Layer | General / strategy / context | n/a |
| 536 | Technology | General / strategy / context | n/a |
| 537 | iOS Native App | General / strategy / context | n/a |
| 538 | Swift 5.x + SwiftUI + Combine | General / strategy / context | n/a |
| 539 | Android Native App | General / strategy / context | n/a |
| 540 | Kotlin + Jetpack Compose + Coroutines | General / strategy / context | n/a |
| 541 | Cross-Platform (Startup) | General / strategy / context | n/a |
| 542 | Flutter 3.x (Dart) — single codebase for iOS + Android | General / strategy / context | n/a |
| 543 | Web Portal | General / strategy / context | n/a |
| 544 | React 18 + Next.js 14 (App Router, Server Components) | General / strategy / context | n/a |
| 545 | Progressive Web App | General / strategy / context | n/a |
| 546 | Next.js PWA + Workbox service workers | General / strategy / context | n/a |
| 547 | State Management | General / strategy / context | n/a |
| 548 | Redux Toolkit (React) / Riverpod (Flutter) / Combine (iOS) | General / strategy / context | n/a |
| 549 | UI Component Library | General / strategy / context | n/a |
| 550 | Tailwind CSS + shadcn/ui (Web) / Material3 (Android) | General / strategy / context | n/a |
| 551 | 9.1.2 Backend Services | General / strategy / context | n/a |
| 552 | Component | General / strategy / context | n/a |
| 553 | Primary API Framework | General / strategy / context | n/a |
| 554 | Node.js (Express / Fastify) — high I/O concurrency | General / strategy / context | n/a |
| 555 | Secondary Services | General / strategy / context | n/a |
| 556 | Python (FastAPI) — ML model serving, data processing | General / strategy / context | n/a |
| 557 | Real-Time Server | General / strategy / context | n/a |
| 558 | Socket.io on Node.js cluster + Redis adapter | General / strategy / context | n/a |
| 559 | API Gateway | General / strategy / context | n/a |
| 560 | AWS API Gateway / Kong / NGINX with Lua scripting | General / strategy / context | n/a |
| 561 | Authentication | General / strategy / context | n/a |
| 562 | OAuth 2.0 + OpenID Connect; JWT access tokens (15 min TTL) + Refresh tokens | General / strategy / context | n/a |
| 563 | Task Queue | General / strategy / context | n/a |
| 564 | Bull (Node.js) + Redis for background job processing | General / strategy / context | n/a |
| 565 | Message Broker | Realtime chat | implemented |
| 566 | Apache Kafka (event streaming) + RabbitMQ (task queues) | General / strategy / context | n/a |
| 567 | Search Engine | Search and filter matching | implemented |
| 568 | Elasticsearch 8.x for full-text + geo-spatial profile search | Search and filter matching | implemented |
| 569 | ML / AI Services | General / strategy / context | n/a |
| 570 | Python (scikit-learn, TensorFlow, Hugging Face Transformers) | General / strategy / context | n/a |
| 571 | PDF Generation | General / strategy / context | n/a |
| 572 | Puppeteer / Playwright headless Chrome for bio-data PDF | General / strategy / context | n/a |
| 573 | Email Service | General / strategy / context | n/a |
| 574 | AWS SES / SendGrid with template management | General / strategy / context | n/a |
| 575 | SMS / OTP | OTP authentication | implemented |
| 576 | Twilio / Exotel / MSG91 (India-specific for OTP delivery) | OTP authentication | implemented |
| 577 | Push Notifications | Notifications | implemented |
| 578 | Firebase Cloud Messaging (FCM) + Apple Push Notification Service (APNS) | Notifications | implemented |
| 579 | Video Calling | Voice/video calls | implemented |
| 580 | WebRTC with Twilio Video or Agora SDK | Voice/video calls | implemented |
| 581 | 9.1.3 Database Layer | General / strategy / context | n/a |
| 582 | Database | General / strategy / context | n/a |
| 583 | PostgreSQL 15 | General / strategy / context | n/a |
| 584 | Primary relational database — user accounts, subscriptions, transactions | Subscriptions and payments | implemented |
| 585 | MongoDB 7 | General / strategy / context | n/a |
| 586 | Semi-structured data — profiles, preferences, match history | General / strategy / context | n/a |
| 587 | Redis Enterprise | General / strategy / context | n/a |
| 588 | Caching, session storage, real-time match queues, rate limiting (Matrimony.com production use case) | General / strategy / context | n/a |
| 589 | Elasticsearch 8 | Search and filter matching | implemented |
| 590 | Profile full-text search, geo-queries, analytics aggregations | Search and filter matching | implemented |
| 591 | Apache Cassandra | General / strategy / context | n/a |
| 592 | High-volume time-series data — message history, activity logs | Realtime chat | implemented |
| 593 | AWS S3 / GCS | General / strategy / context | n/a |
| 594 | Object storage — profile photos, documents, audio, video | General / strategy / context | n/a |
| 595 | InfluxDB | General / strategy / context | n/a |
| 596 | Metrics and time-series monitoring data | General / strategy / context | n/a |
| 597 | 9.1.4 Cloud & DevOps Infrastructure | General / strategy / context | n/a |
| 598 | Cloud Provider | General / strategy / context | n/a |
| 599 | AWS (primary) / GCP (multi-cloud for ML workloads) | General / strategy / context | n/a |
| 600 | Container Orchestration | General / strategy / context | n/a |
| 601 | Kubernetes (EKS) with Helm charts | General / strategy / context | n/a |
| 602 | CI/CD Pipeline | General / strategy / context | n/a |
| 603 | GitHub Actions + ArgoCD (GitOps deployment) | General / strategy / context | n/a |
| 604 | Infrastructure as Code | General / strategy / context | n/a |
| 605 | Terraform + AWS CDK | General / strategy / context | n/a |
| 606 | Containerisation | General / strategy / context | n/a |
| 607 | Docker with multi-stage builds | General / strategy / context | n/a |
| 608 | Service Mesh | General / strategy / context | n/a |
| 609 | Istio for inter-service mTLS and observability | General / strategy / context | n/a |
| 610 | CloudFront (AWS) / Cloudflare for static assets and media | General / strategy / context | n/a |
| 611 | Load Balancing | General / strategy / context | n/a |
| 612 | AWS ALB with health check and weighted routing | General / strategy / context | n/a |
| 613 | Auto Scaling | General / strategy / context | n/a |
| 614 | Kubernetes HPA + Cluster Autoscaler | General / strategy / context | n/a |
| 615 | Observability | General / strategy / context | n/a |
| 616 | Prometheus + Grafana (metrics), Jaeger (tracing), ELK Stack (logging) | General / strategy / context | n/a |
| 617 | Secrets Management | General / strategy / context | n/a |
| 618 | AWS Secrets Manager + HashiCorp Vault | General / strategy / context | n/a |
| 619 | 10.1 Core Data Entities | General / strategy / context | n/a |
| 620 | The following entity-relationship summary describes the primary data objects and their relationships within the matrimony platform data model. | General / strategy / context | n/a |
| 621 | 10.1.1 Users & Profiles | General / strategy / context | n/a |
| 622 | Entity | General / strategy / context | n/a |
| 623 | Key Fields | General / strategy / context | n/a |
| 624 | User | General / strategy / context | n/a |
| 625 | user_id (UUID), mobile_hash, email_hash, password_hash (bcrypt), created_at, status, plan_id, verification_level | Verification/safety/reporting | implemented |
| 626 | Profile | General / strategy / context | n/a |
| 627 | profile_id, user_id (FK), name, dob, gender, height, religion, caste, education, occupation, income_range, location_id, about_me, profile_score | General / strategy / context | n/a |
| 628 | ProfileMedia | General / strategy / context | n/a |
| 629 | media_id, profile_id, url, type (photo/doc), sort_order, is_primary, visibility_level, moderation_status | Admin and analytics | implemented |
| 630 | PartnerPreferences | General / strategy / context | n/a |
| 631 | pref_id, profile_id, age_min, age_max, religion[], caste[], income_min, education_level[], location_prefs[], lifestyle[] | General / strategy / context | n/a |
| 632 | Horoscope | General / strategy / context | n/a |
| 633 | horo_id, profile_id, rashi, nakshatra, birth_time, manglik_status, kundali_json, last_updated | General / strategy / context | n/a |
| 634 | 10.1.2 Matchmaking & Interaction | Search and filter matching | implemented |
| 635 | Match | General / strategy / context | n/a |
| 636 | match_id, profile_a, profile_b, compatibility_score, algorithm_version, created_at | Search and filter matching | implemented |
| 637 | Interest | Interests/shortlist flow | implemented |
| 638 | interest_id, sender_id, receiver_id, status (pending/accepted/declined), sent_at, responded_at | Interests/shortlist flow | implemented |
| 639 | Connection | General / strategy / context | n/a |
| 640 | conn_id, profile_a, profile_b, connected_at, status (active/blocked) | Verification/safety/reporting | implemented |
| 641 | Shortlist | Interests/shortlist flow | implemented |
| 642 | shortlist_id, user_id, target_profile_id, added_at, note | Interests/shortlist flow | implemented |
| 643 | ProfileView | General / strategy / context | n/a |
| 644 | view_id, viewer_id, viewed_id, viewed_at, duration_seconds | General / strategy / context | n/a |
| 645 | 10.1.3 Messaging | General / strategy / context | n/a |
| 646 | Conversation | General / strategy / context | n/a |
| 647 | conv_id, participant_a, participant_b, created_at, last_message_at | Realtime chat | implemented |
| 648 | Message | Realtime chat | implemented |
| 649 | msg_id, conv_id, sender_id, type (text/audio/photo/doc), content_encrypted, sent_at, delivered_at, read_at, deleted_for[] | General / strategy / context | n/a |
| 650 | MessageReaction | Realtime chat | implemented |
| 651 | reaction_id, msg_id, user_id, emoji, created_at | General / strategy / context | n/a |
| 652 | 10.1.4 Subscriptions & Payments | Subscriptions and payments | implemented |
| 653 | Plan | Subscriptions and payments | implemented |
| 654 | plan_id, name, duration_days, price, currency, features_json | Subscriptions and payments | implemented |
| 655 | Subscription | Subscriptions and payments | implemented |
| 656 | sub_id, user_id, plan_id, start_date, end_date, status, auto_renew | Subscriptions and payments | implemented |
| 657 | Transaction | General / strategy / context | n/a |
| 658 | txn_id, user_id, amount, currency, gateway, gateway_txn_id, status, created_at, metadata_json | General / strategy / context | n/a |
| 659 | Invoice | General / strategy / context | n/a |
| 660 | invoice_id, txn_id, user_id, amount, tax, issued_at, pdf_url | General / strategy / context | n/a |
| 661 | 10.2 Data Indexing Strategy | General / strategy / context | n/a |
| 662 | Efficient query performance at scale requires a carefully designed indexing strategy across all primary entities. | General / strategy / context | n/a |
| 663 | Composite index on (religion, caste, age, location) for core search queries | Search and filter matching | implemented |
| 664 | GiST / PostGIS index on geographic coordinates for location-based search | Search and filter matching | implemented |
| 665 | Full-text search index on profile bio, name, and occupation fields | Search and filter matching | implemented |
| 666 | B-tree index on user_id, profile_id across all foreign-key relationships | General / strategy / context | n/a |
| 667 | Partial index on active subscriptions (status = 'active') for billing queries | Subscriptions and payments | implemented |
| 668 | TTL index on session tokens and OTP records (auto-expiry) | OTP authentication | implemented |
| 669 | 10.3 Data Retention & Purge Policies | General / strategy / context | n/a |
| 670 | Data Type | General / strategy / context | n/a |
| 671 | Retention Policy | General / strategy / context | n/a |
| 672 | Active User Data | General / strategy / context | n/a |
| 673 | Retained indefinitely while account is active | General / strategy / context | n/a |
| 674 | Deleted Account Data | General / strategy / context | n/a |
| 675 | Anonymised within 30 days, fully purged within 90 days | General / strategy / context | n/a |
| 676 | Retained for 2 years; user can delete conversation at any time | General / strategy / context | n/a |
| 677 | Transaction Records | General / strategy / context | n/a |
| 678 | Retained for 7 years for financial compliance (GST Act, IT Act) | General / strategy / context | n/a |
| 679 | Activity Logs | General / strategy / context | n/a |
| 680 | 90 days hot storage, 1 year cold archive | General / strategy / context | n/a |
| 681 | Moderation Logs | Admin and analytics | implemented |
| 682 | 3 years (required for potential legal proceedings) | General / strategy / context | n/a |
| 683 | OTP Tokens | OTP authentication | implemented |
| 684 | Auto-purged after 10 minutes | General / strategy / context | n/a |
| 685 | 11.1 Authentication & Authorisation | General / strategy / context | n/a |
| 686 | 11.1.1 Multi-Factor Authentication (MFA) | General / strategy / context | n/a |
| 687 | Every user account is protected by at least one second factor of authentication, with additional factors available for high-risk operations. | General / strategy / context | n/a |
| 688 | Factor | General / strategy / context | n/a |
| 689 | Implementation | General / strategy / context | n/a |
| 690 | Mobile OTP (Primary 2FA) | OTP authentication | implemented |
| 691 | 6-digit OTP sent via SMS or WhatsApp; 10-minute TTL; 3 attempts max | OTP authentication | implemented |
| 692 | Email OTP | OTP authentication | implemented |
| 693 | Verification link or 6-digit code via email | Verification/safety/reporting | implemented |
| 694 | Biometric (Mobile) | General / strategy / context | n/a |
| 695 | FaceID / TouchID via platform-native biometric API (device-level) | General / strategy / context | n/a |
| 696 | TOTP (Admin accounts) | Admin and analytics | implemented |
| 697 | Google Authenticator / Authy TOTP for admin panel access | Admin and analytics | implemented |
| 698 | Hardware Security Key | General / strategy / context | n/a |
| 699 | FIDO2 / WebAuthn support for enterprise admin accounts | Admin and analytics | implemented |
| 700 | 11.1.2 JWT Token Architecture | General / strategy / context | n/a |
| 701 | All API access is governed by JSON Web Tokens following a short-lived access token + long-lived refresh token pattern. | General / strategy / context | n/a |
| 702 | Access Token: Signed with RS256 (RSA 2048-bit), TTL = 15 minutes | General / strategy / context | n/a |
| 703 | Refresh Token: Stored as HttpOnly Secure cookie, TTL = 30 days | General / strategy / context | n/a |
| 704 | Token Rotation: New refresh token issued on every refresh request (prevents token theft replay) | General / strategy / context | n/a |
| 705 | Token Revocation: Refresh tokens stored in Redis with O(1) revocation capability | General / strategy / context | n/a |
| 706 | Audience Claim: Token bound to specific client type (iOS / Android / Web) to prevent cross-client replay | General / strategy / context | n/a |
| 707 | 11.1.3 Role-Based Access Control (RBAC) | General / strategy / context | n/a |
| 708 | Role | General / strategy / context | n/a |
| 709 | Permissions | General / strategy / context | n/a |
| 710 | Guest | General / strategy / context | n/a |
| 711 | View public landing pages, initiate registration | General / strategy / context | n/a |
| 712 | Registered User (Free) | General / strategy / context | n/a |
| 713 | Create profile, send limited interests, view limited profiles | Interests/shortlist flow | implemented |
| 714 | Premium User | Subscriptions and payments | implemented |
| 715 | Full match access, direct messaging, contact viewing | General / strategy / context | n/a |
| 716 | Moderator | General / strategy / context | n/a |
| 717 | Access moderation queue, approve/reject content, suspend users | Admin and analytics | implemented |
| 718 | Customer Support | General / strategy / context | n/a |
| 719 | View user accounts, manage subscriptions, process refunds | Subscriptions and payments | implemented |
| 720 | Data Analyst | General / strategy / context | n/a |
| 721 | Read-only analytics dashboard, anonymised data exports | Admin and analytics | implemented |
| 722 | Super Admin | Admin and analytics | implemented |
| 723 | Full system access including role management and configuration | General / strategy / context | n/a |
| 724 | 11.2 Data Encryption | General / strategy / context | n/a |
| 725 | 11.2.1 Encryption in Transit | General / strategy / context | n/a |
| 726 | All client-server communication over TLS 1.3 (minimum TLS 1.2) | General / strategy / context | n/a |
| 727 | HTTPS enforced via HSTS with a minimum max-age of 1 year | General / strategy / context | n/a |
| 728 | Perfect Forward Secrecy: ECDHE key exchange ensures session key uniqueness | General / strategy / context | n/a |
| 729 | Certificate Pinning on mobile apps to prevent MITM attacks | General / strategy / context | n/a |
| 730 | mTLS for internal service-to-service communication via Istio service mesh | General / strategy / context | n/a |
| 731 | 11.2.2 Encryption at Rest | General / strategy / context | n/a |
| 732 | Database encryption: AWS RDS with AES-256 KMS-managed keys | General / strategy / context | n/a |
| 733 | Object storage: S3 server-side encryption (SSE-S3 or SSE-KMS) for all photos and documents | General / strategy / context | n/a |
| 734 | Message content: End-to-end encrypted using AES-256-GCM with per-conversation keys | Realtime chat | implemented |
| 735 | E2E Key Management: Conversation keys generated client-side, server never holds plaintext keys | General / strategy / context | n/a |
| 736 | Backup encryption: All database backups encrypted with separate KMS key | General / strategy / context | n/a |
| 737 | Sensitive PII fields: Mobile numbers and email addresses stored as salted hashes (bcrypt + pepper) | General / strategy / context | n/a |
| 738 | 11.3 API Security | General / strategy / context | n/a |
| 739 | Security Control | General / strategy / context | n/a |
| 740 | Rate Limiting | General / strategy / context | n/a |
| 741 | Global: 1000 req/min per IP. Per user: 100 req/min. Login: 5 attempts/15 min. | General / strategy / context | n/a |
| 742 | Input Validation | General / strategy / context | n/a |
| 743 | JSON Schema validation on all API request bodies; Joi / Zod libraries | General / strategy / context | n/a |
| 744 | SQL Injection Prevention | General / strategy / context | n/a |
| 745 | Parameterised queries only; ORM (Sequelize / Prisma) enforced | General / strategy / context | n/a |
| 746 | XSS Prevention | General / strategy / context | n/a |
| 747 | Content Security Policy headers; output encoding; DOMPurify on client | General / strategy / context | n/a |
| 748 | CSRF Prevention | General / strategy / context | n/a |
| 749 | Double-submit cookie pattern; SameSite=Strict on session cookies | General / strategy / context | n/a |
| 750 | CORS Policy | General / strategy / context | n/a |
| 751 | Allowlist of trusted origins only; wildcard (*) never permitted | General / strategy / context | n/a |
| 752 | API Key Management | General / strategy / context | n/a |
| 753 | Third-party API keys stored in AWS Secrets Manager; rotated quarterly | General / strategy / context | n/a |
| 754 | Request Signing | General / strategy / context | n/a |
| 755 | HMAC-SHA256 signature on sensitive operations (payment, account deletion) | Subscriptions and payments | implemented |
| 756 | Bot Detection | General / strategy / context | n/a |
| 757 | Google reCAPTCHA v3 on registration and login endpoints | General / strategy / context | n/a |
| 758 | DDoS Protection | General / strategy / context | n/a |
| 759 | AWS Shield Standard + CloudFront WAF with managed rule groups | General / strategy / context | n/a |
| 760 | 11.4 Infrastructure Security | General / strategy / context | n/a |
| 761 | VPC isolation: All backend services in private subnets; no direct internet exposure | General / strategy / context | n/a |
| 762 | Security Groups: Least-privilege inbound/outbound rules per service | General / strategy / context | n/a |
| 763 | Bastion host: SSH access to infrastructure only through a hardened jump server with MFA | General / strategy / context | n/a |
| 764 | IAM Roles: EC2/EKS nodes use IAM roles (no static credentials stored on instances) | General / strategy / context | n/a |
| 765 | Image scanning: Container images scanned for CVEs via Trivy + AWS ECR scanning in CI/CD pipeline | General / strategy / context | n/a |
| 766 | Runtime security: Falco for runtime anomaly detection in Kubernetes pods | General / strategy / context | n/a |
| 767 | Network policies: Kubernetes NetworkPolicy restricts pod-to-pod communication | General / strategy / context | n/a |
| 768 | Secrets rotation: AWS Secrets Manager auto-rotates database credentials every 30 days | General / strategy / context | n/a |
| 769 | 11.5 Penetration Testing & Vulnerability Management | General / strategy / context | n/a |
| 770 | Quarterly third-party penetration testing (OWASP Top 10 + API security) | General / strategy / context | n/a |
| 771 | Bug bounty programme via HackerOne or Bugcrowd | General / strategy / context | n/a |
| 772 | Static Application Security Testing (SAST): SonarQube + Semgrep in CI pipeline | General / strategy / context | n/a |
| 773 | Dynamic Application Security Testing (DAST): OWASP ZAP in staging environment | General / strategy / context | n/a |
| 774 | Software Composition Analysis (SCA): Snyk for open-source dependency vulnerability scanning | General / strategy / context | n/a |
| 775 | Security patch cadence: Critical CVEs patched within 24 hours; High within 7 days; Medium within 30 days | General / strategy / context | n/a |
| 776 | 11.6 Incident Response Plan | Subscriptions and payments | implemented |
| 777 | Detection: Automated alert via Prometheus / CloudWatch triggers incident ticket | General / strategy / context | n/a |
| 778 | Triage: On-call engineer assesses severity (P0 / P1 / P2 / P3) | General / strategy / context | n/a |
| 779 | Containment: Isolate affected services; revoke compromised credentials | General / strategy / context | n/a |
| 780 | Eradication: Identify root cause; apply patch or configuration fix | General / strategy / context | n/a |
| 781 | Recovery: Restore from clean backup if needed; run regression tests | General / strategy / context | n/a |
| 782 | Post-Mortem: Blameless post-mortem within 5 days; update runbooks | General / strategy / context | n/a |
| 783 | User Notification: If user data affected, notify users and regulators per GDPR / PDPA within 72 hours | Notifications | implemented |
| 784 | 12. API Design & Third-Party Integrations | General / strategy / context | n/a |
| 785 | 12.1 REST API Design Principles | General / strategy / context | n/a |
| 786 | The matrimony platform exposes a well-structured RESTful API following industry best practices to ensure consistency, discoverability, and ease of integration across all client platforms. | General / strategy / context | n/a |
| 787 | 12.1.1 API Standards | General / strategy / context | n/a |
| 788 | Resource naming: Noun-based, plural, lowercase with hyphens (e.g., /api/v1/profiles, /api/v1/match-requests) | General / strategy / context | n/a |
| 789 | HTTP Methods: GET (read), POST (create), PUT (full update), PATCH (partial update), DELETE (remove) | General / strategy / context | n/a |
| 790 | Versioning: URL path versioning (/v1/, /v2/) for backward compatibility | Search and filter matching | implemented |
| 791 | Pagination: Cursor-based pagination for feeds and search results (performance at scale) | Search and filter matching | implemented |
| 792 | Response format: Consistent JSON envelope with { data, meta, error } structure | General / strategy / context | n/a |
| 793 | HTTP Status Codes: Strict adherence (200/201/204/400/401/403/404/409/422/429/500) | General / strategy / context | n/a |
| 794 | Hypermedia: HATEOAS links in responses for discoverability | General / strategy / context | n/a |
| 795 | 12.1.2 Core API Endpoints | General / strategy / context | n/a |
| 796 | Endpoint | General / strategy / context | n/a |
| 797 | POST /auth/register | General / strategy / context | n/a |
| 798 | New user registration with OTP dispatch | OTP authentication | implemented |
| 799 | POST /auth/verify-otp | OTP authentication | implemented |
| 800 | OTP verification and initial JWT issuance | OTP authentication | implemented |
| 801 | POST /auth/refresh | General / strategy / context | n/a |
| 802 | Access token refresh using refresh token cookie | General / strategy / context | n/a |
| 803 | POST /auth/logout | General / strategy / context | n/a |
| 804 | Revoke refresh token and clear session | General / strategy / context | n/a |
| 805 | GET /profiles/{id} | General / strategy / context | n/a |
| 806 | Fetch a specific profile (respects privacy settings) | Verification/safety/reporting | implemented |
| 807 | PATCH /profiles/{id} | General / strategy / context | n/a |
| 808 | Update profile fields (authenticated, self only) | General / strategy / context | n/a |
| 809 | GET /matches/daily | General / strategy / context | n/a |
| 810 | Fetch daily curated match recommendations | General / strategy / context | n/a |
| 811 | GET /search/profiles | Search and filter matching | implemented |
| 812 | Search profiles with filter parameters | Search and filter matching | implemented |
| 813 | POST /interests | Interests/shortlist flow | implemented |
| 814 | Send interest to a profile | Interests/shortlist flow | implemented |
| 815 | PATCH /interests/{id} | Interests/shortlist flow | implemented |
| 816 | Accept or decline a received interest | Interests/shortlist flow | implemented |
| 817 | GET /conversations | General / strategy / context | n/a |
| 818 | List all active conversations | General / strategy / context | n/a |
| 819 | GET /conversations/{id}/messages | Realtime chat | implemented |
| 820 | Paginated message history | Realtime chat | implemented |
| 821 | POST /conversations/{id}/messages | Realtime chat | implemented |
| 822 | Send a new message | Realtime chat | implemented |
| 823 | POST /media/upload | General / strategy / context | n/a |
| 824 | Upload profile photo or document | General / strategy / context | n/a |
| 825 | POST /payments/subscriptions | Subscriptions and payments | implemented |
| 826 | Purchase a subscription plan | Subscriptions and payments | implemented |
| 827 | POST /users/{id}/report | Verification/safety/reporting | implemented |
| 828 | File a report against a user | Verification/safety/reporting | implemented |
| 829 | DELETE /users/{id} | General / strategy / context | n/a |
| 830 | Initiate account deletion | General / strategy / context | n/a |
| 831 | 12.2 Key Third-Party Integrations | General / strategy / context | n/a |
| 832 | Integration | General / strategy / context | n/a |
| 833 | Provider & Purpose | General / strategy / context | n/a |
| 834 | OTP / SMS | OTP authentication | implemented |
| 835 | Twilio, MSG91, Exotel — One-time password delivery via SMS | OTP authentication | implemented |
| 836 | AWS SES, SendGrid — Transactional and marketing email | General / strategy / context | n/a |
| 837 | Firebase Cloud Messaging (Android), Apple Push Notification Service (iOS) | Notifications | implemented |
| 838 | Payment Gateway (India) | Subscriptions and payments | implemented |
| 839 | Razorpay, PayU, Cashfree — UPI, cards, net banking, EMI | Subscriptions and payments | implemented |
| 840 | Payment Gateway (Global) | Subscriptions and payments | implemented |
| 841 | Stripe, PayPal, Braintree — International card payments | Subscriptions and payments | implemented |
| 842 | Agora SDK / Twilio Video — WebRTC-based in-app video | Voice/video calls | implemented |
| 843 | Identity Verification | Verification/safety/reporting | implemented |
| 844 | Aadhaar eKYC (UIDAI API), DigiLocker API, Jumio (global KYC) | General / strategy / context | n/a |
| 845 | Maps & Geolocation | General / strategy / context | n/a |
| 846 | Google Maps Platform — Geocoding, places autocomplete, distance calculation | General / strategy / context | n/a |
| 847 | AI Content Moderation | Admin and analytics | implemented |
| 848 | AWS Rekognition / Google Vision API — NSFW photo detection | General / strategy / context | n/a |
| 849 | Background Check | General / strategy / context | n/a |
| 850 | AuthBridge, SpringVerify — Criminal background verification (Premium) | Verification/safety/reporting | implemented |
| 851 | Analytics | Admin and analytics | implemented |
| 852 | Firebase Analytics, Mixpanel, Amplitude — User behaviour and funnel analysis | Admin and analytics | implemented |
| 853 | Crash Reporting | Verification/safety/reporting | implemented |
| 854 | Sentry (backend & frontend), Crashlytics (mobile) | General / strategy / context | n/a |
| 855 | Freshdesk / Zendesk — Support ticket management and chat | Realtime chat | implemented |
| 856 | Social Login | General / strategy / context | n/a |
| 857 | Google OAuth 2.0, Facebook Login, Apple Sign-In | General / strategy / context | n/a |
| 858 | Deep Linking | General / strategy / context | n/a |
| 859 | Branch.io — Dynamic links for app install attribution and sharing | General / strategy / context | n/a |
| 860 | 13.1 Primary Revenue Streams | General / strategy / context | n/a |
| 861 | 13.1.1 Subscription Plans | Subscriptions and payments | implemented |
| 862 | Subscription revenue is the dominant revenue model for matrimony platforms, contributing 70–80% of total revenue. Plans are offered in monthly, quarterly, half-yearly, and annual durations with significant discounts for longer commitments. | Subscriptions and payments | implemented |
| 863 | Plan Tier | Subscriptions and payments | implemented |
| 864 | Duration Options | General / strategy / context | n/a |
| 865 | Price Range (INR) | General / strategy / context | n/a |
| 866 | Silver / Basic | General / strategy / context | n/a |
| 867 | 1 Month, 3 Months | General / strategy / context | n/a |
| 868 | Gold / Standard | General / strategy / context | n/a |
| 869 | 3 Months, 6 Months | General / strategy / context | n/a |
| 870 | Platinum / Premium | Subscriptions and payments | implemented |
| 871 | 6 Months, 12 Months | General / strategy / context | n/a |
| 872 | Diamond / Elite | General / strategy / context | n/a |
| 873 | Assisted Matchmaking | Search and filter matching | implemented |
| 874 | 12 Month minimum | General / strategy / context | n/a |
| 875 | 13.1.2 À La Carte Premium Add-Ons | Subscriptions and payments | implemented |
| 876 | Profile Boost (30-min top placement): ₹99 – ₹299 per boost | General / strategy / context | n/a |
| 877 | Extra Contact Views: ₹49 per contact unlock | General / strategy / context | n/a |
| 878 | Kundali Full Report: ₹199 per report | Verification/safety/reporting | implemented |
| 879 | Live Astrologer Consultation: ₹299 – ₹999 per session | General / strategy / context | n/a |
| 880 | Priority Customer Support: ₹499/month add-on | General / strategy / context | n/a |
| 881 | WhatsApp Profile Sharing Pack: ₹149/month | General / strategy / context | n/a |
| 882 | 13.1.3 Advertising Revenue | General / strategy / context | n/a |
| 883 | A small proportion of revenue comes from relevant, non-intrusive advertising placements for complementary services (wedding vendors, jewellery, honeymoon travel, home furnishings). Native-format ads are integrated within the feed for free users only. | General / strategy / context | n/a |
| 884 | 13.1.4 Wedding & Allied Services Marketplace | General / strategy / context | n/a |
| 885 | Premium platforms extend their monetisation beyond matchmaking by offering a curated marketplace of wedding-related services. | Search and filter matching | implemented |
| 886 | Wedding Venue Discovery and Booking | General / strategy / context | n/a |
| 887 | Photographer and Videographer Listings | General / strategy / context | n/a |
| 888 | Wedding Planner Directory | Subscriptions and payments | implemented |
| 889 | Bridal Makeup and Mehendi Artist Bookings | General / strategy / context | n/a |
| 890 | Caterer and Decorator Listings | General / strategy / context | n/a |
| 891 | Honeymoon Travel Package Partnerships (affiliate revenue) | General / strategy / context | n/a |
| 892 | Wedding Jewellery and Attire Discovery (affiliate / commission model) | General / strategy / context | n/a |
| 893 | 13.2 Pricing Strategy | General / strategy / context | n/a |
| 894 | Matrimony platforms use a combination of value-based pricing (premium tiers priced by exclusivity and results), time-based discounting (annual plans at 40–60% discount), and urgency-driven promotions (festival offers — Diwali, Navratri, Wedding season). Dynamic pricing models based on user engagement score are increasingly employed to personalise offer timing. | Subscriptions and payments | implemented |
| 895 | 13.3 Refund Policy Framework | General / strategy / context | n/a |
| 896 | Free trial period: 3–7 days with full refund on cancellation | General / strategy / context | n/a |
| 897 | No refund after 7 days of plan activation (standard policy) | Subscriptions and payments | implemented |
| 898 | Pro-rata refund if platform fails to deliver promised features (SLA breach) | General / strategy / context | n/a |
| 899 | Goodwill credits issued for verified technical issues or service outages | General / strategy / context | n/a |
| 900 | Payment disputes handled within 7 working days per consumer protection regulations | Subscriptions and payments | implemented |
| 901 | 14.1 Performance Benchmarks & SLAs | General / strategy / context | n/a |
| 902 | Target SLA | General / strategy / context | n/a |
| 903 | API Response Time (P95) | General / strategy / context | n/a |
| 904 | < 200 ms for read queries; < 500 ms for write operations | General / strategy / context | n/a |
| 905 | App Launch Time (Cold) | General / strategy / context | n/a |
| 906 | < 3 seconds on mid-range Android device | General / strategy / context | n/a |
| 907 | Profile Image Load Time | General / strategy / context | n/a |
| 908 | < 1 second (CDN-delivered, WebP format) | General / strategy / context | n/a |
| 909 | Real-Time Message Delivery | Realtime chat | implemented |
| 910 | < 100 ms within same region | General / strategy / context | n/a |
| 911 | Search Query Response | Search and filter matching | implemented |
| 912 | < 300 ms for standard filters; < 800 ms for complex geo+text | Search and filter matching | implemented |
| 913 | Uptime SLA | General / strategy / context | n/a |
| 914 | 99.95% monthly (< 22 minutes downtime/month) | General / strategy / context | n/a |
| 915 | Video Call Setup Time | Voice/video calls | implemented |
| 916 | < 5 seconds to establish WebRTC connection | Voice/video calls | implemented |
| 917 | OTP Delivery | OTP authentication | implemented |
| 918 | < 30 seconds via SMS; < 5 seconds via WhatsApp | General / strategy / context | n/a |
| 919 | 14.2 Caching Strategy | General / strategy / context | n/a |
| 920 | A multi-layer caching architecture is essential for serving millions of profile requests without overloading the primary database. | General / strategy / context | n/a |
| 921 | Cache Layer | General / strategy / context | n/a |
| 922 | Purpose & TTL | General / strategy / context | n/a |
| 923 | CDN (CloudFront) | General / strategy / context | n/a |
| 924 | Static assets, profile photos — TTL: 7 days with cache invalidation on update | General / strategy / context | n/a |
| 925 | Redis L1 Cache | General / strategy / context | n/a |
| 926 | Frequently accessed profiles, session data, match queues — TTL: 5–30 minutes | General / strategy / context | n/a |
| 927 | Redis L2 Cache | General / strategy / context | n/a |
| 928 | Search result pages, filter aggregations — TTL: 2 minutes | Search and filter matching | implemented |
| 929 | Elasticsearch Cache | Search and filter matching | implemented |
| 930 | Repeated search queries within short windows — query cache | Search and filter matching | implemented |
| 931 | Application-Level Cache | General / strategy / context | n/a |
| 932 | Reference data (religion lists, city lists) — in-memory, refreshed hourly | General / strategy / context | n/a |
| 933 | Database Query Cache | General / strategy / context | n/a |
| 934 | PostgreSQL shared_buffers tuning for hot data sets | General / strategy / context | n/a |
| 935 | 14.3 Scalability Architecture | General / strategy / context | n/a |
| 936 | The platform is designed to scale horizontally across all layers, supporting growth from 10,000 to 10 million active users without architectural rework. | General / strategy / context | n/a |
| 937 | 14.3.1 Horizontal Scaling Components | General / strategy / context | n/a |
| 938 | API Servers: Kubernetes HPA scales pods based on CPU utilisation (70% threshold) and custom RPS metrics | General / strategy / context | n/a |
| 939 | Database: PostgreSQL read replicas (up to 5) for read distribution; PgBouncer connection pooling | General / strategy / context | n/a |
| 940 | Search: Elasticsearch cluster scaled by shard distribution; dedicated data + master + coordinator nodes | Search and filter matching | implemented |
| 941 | WebSocket Servers: Socket.io with Redis adapter for sticky session distribution across nodes | General / strategy / context | n/a |
| 942 | Kafka: Partition-based horizontal scaling for event stream throughput | General / strategy / context | n/a |
| 943 | Object Storage: AWS S3 inherently scalable; lifecycle policies move old media to Glacier | General / strategy / context | n/a |
| 944 | 14.3.2 Database Sharding Strategy | General / strategy / context | n/a |
| 945 | At very large scale (50M+ profiles), the profile database is sharded by geographic region (India North, India South, India West, NRI). Each shard is independently replicated and backed up. Cross-shard queries (for NRI matches crossing regions) are handled by the Elasticsearch layer which maintains a global index. | Search and filter matching | implemented |
| 946 | 14.4 CI/CD Pipeline | General / strategy / context | n/a |
| 947 | Developer pushes code to feature branch | Notifications | implemented |
| 948 | GitHub Actions triggers: SAST scan (Semgrep), unit tests, integration tests | General / strategy / context | n/a |
| 949 | Pull request review by 2 engineers + automated code quality check (SonarQube) | General / strategy / context | n/a |
| 950 | Merge to main triggers Docker build and push to ECR | Notifications | implemented |
| 951 | ArgoCD detects new image; deploys to staging environment automatically | General / strategy / context | n/a |
| 952 | Automated E2E tests (Playwright / Cypress) run against staging | General / strategy / context | n/a |
| 953 | Manual QA sign-off for user-facing changes | General / strategy / context | n/a |
| 954 | Production deployment via blue-green strategy (zero-downtime) | General / strategy / context | n/a |
| 955 | Post-deploy synthetic monitoring confirms key user journeys pass | General / strategy / context | n/a |
| 956 | 14.5 Disaster Recovery | General / strategy / context | n/a |
| 957 | DR Strategy | General / strategy / context | n/a |
| 958 | Multi-AZ RDS with automated failover (RTO: < 2 min, RPO: < 1 min) | General / strategy / context | n/a |
| 959 | Application Layer | General / strategy / context | n/a |
| 960 | Multi-region Kubernetes deployments (Active-Passive) | General / strategy / context | n/a |
| 961 | Object Storage | General / strategy / context | n/a |
| 962 | S3 Cross-Region Replication to secondary region | General / strategy / context | n/a |
| 963 | Kafka | General / strategy / context | n/a |
| 964 | Multi-AZ MSK cluster with replication factor 3 | General / strategy / context | n/a |
| 965 | DNS Failover | General / strategy / context | n/a |
| 966 | Route53 health check with automatic failover to DR region | General / strategy / context | n/a |
| 967 | Backup Testing | General / strategy / context | n/a |
| 968 | Monthly DR drill — failover + failback simulation | General / strategy / context | n/a |
| 969 | RPO Target | General / strategy / context | n/a |
| 970 | < 15 minutes for transactional data; < 1 hour for analytics data | Admin and analytics | implemented |
| 971 | RTO Target | General / strategy / context | n/a |
| 972 | < 30 minutes for full platform restoration | General / strategy / context | n/a |
| 973 | 15.1 Regulatory Compliance Framework | General / strategy / context | n/a |
| 974 | 15.1.1 India — IT Act & DPDP Act | General / strategy / context | n/a |
| 975 | The Digital Personal Data Protection Act, 2023 (DPDP Act) is the primary data protection regulation governing Indian matrimony platforms. Key obligations include: | General / strategy / context | n/a |
| 976 | Explicit, informed consent required before collecting any personal data | General / strategy / context | n/a |
| 977 | Purpose limitation: Data collected only for the stated purpose (matchmaking) | Search and filter matching | implemented |
| 978 | Data minimisation: Only data necessary for the service can be collected | General / strategy / context | n/a |
| 979 | User rights: Right to access, correct, and erase personal data | General / strategy / context | n/a |
| 980 | Data localisation: Sensitive personal data of Indian users must be stored in India | General / strategy / context | n/a |
| 981 | Data Fiduciary registration with the Data Protection Board of India | General / strategy / context | n/a |
| 982 | Mandatory breach notification within 72 hours of discovery | Notifications | implemented |
| 983 | 15.1.2 GDPR (EU/UK Users) | General / strategy / context | n/a |
| 984 | Lawful basis for processing: Consent (for matchmaking) and Legitimate Interest (for safety) | Search and filter matching | implemented |
| 985 | Data Subject Rights: Access, Rectification, Erasure ('right to be forgotten'), Portability | General / strategy / context | n/a |
| 986 | Cookie consent management platform (CMP) on web portal | General / strategy / context | n/a |
| 987 | Data Processing Agreements (DPA) with all sub-processors | General / strategy / context | n/a |
| 988 | Standard Contractual Clauses (SCC) for data transfers outside EEA | General / strategy / context | n/a |
| 989 | Data Protection Impact Assessment (DPIA) for high-risk processing activities | General / strategy / context | n/a |
| 990 | 15.1.3 Payment Compliance | Subscriptions and payments | implemented |
| 991 | PCI-DSS Level 1 compliance through payment gateway partners (Razorpay, Stripe) | Subscriptions and payments | implemented |
| 992 | No raw card data stored on platform servers — tokenisation only | General / strategy / context | n/a |
| 993 | GST compliance for Indian transactions (18% GST on digital services) | General / strategy / context | n/a |
| 994 | TDS deduction and Form 16A issuance for Indian payouts to service providers | General / strategy / context | n/a |
| 995 | 15.1.4 Matrimony-Specific Legal Considerations | General / strategy / context | n/a |
| 996 | Age verification: Users must be 18+ (India legal marriage age); date of birth verified against ID | Verification/safety/reporting | implemented |
| 997 | Marital status disclosure: Platform is not liable for misrepresentation but takes reports seriously | Verification/safety/reporting | implemented |
| 998 | No guarantee of marriage: Terms of service explicitly state platform is a facilitation tool | General / strategy / context | n/a |
| 999 | Anti-dowry: Platform community guidelines prohibit any reference to dowry demands | General / strategy / context | n/a |
| 1000 | Fake profile liability: Platform maintains takedown procedures for impersonation complaints | General / strategy / context | n/a |
| 1001 | 15.2 Accessibility & Inclusion | General / strategy / context | n/a |
| 1002 | WCAG 2.1 AA compliance on web portal for users with disabilities | General / strategy / context | n/a |
| 1003 | Screen reader support (VoiceOver, TalkBack) on mobile apps | General / strategy / context | n/a |
| 1004 | High-contrast mode and font size adjustment settings | General / strategy / context | n/a |
| 1005 | Multi-language support: English, Hindi, Tamil, Telugu, Kannada, Malayalam, Marathi, Gujarati, Punjabi, Bengali, Urdu | General / strategy / context | n/a |
| 1006 | Right-to-Left (RTL) support for Urdu and Arabic interfaces | General / strategy / context | n/a |
| 1007 | 15.3 Future Roadmap — Emerging Technologies | General / strategy / context | n/a |
| 1008 | Feature / Technology | General / strategy / context | n/a |
| 1009 | Description & Timeline | General / strategy / context | n/a |
| 1010 | AI-Generated Profile Video | General / strategy / context | n/a |
| 1011 | 30-second auto-generated profile introduction video from photos and bio — 2026 | General / strategy / context | n/a |
| 1012 | AR Virtual Meeting Room | General / strategy / context | n/a |
| 1013 | Augmented Reality space for first virtual meetings — 2026 | General / strategy / context | n/a |
| 1014 | Blockchain Identity Layer | Verification/safety/reporting | implemented |
| 1015 | Immutable identity credentials using Polygon / Ethereum-based DID — 2026–27 | General / strategy / context | n/a |
| 1016 | Voice-Enabled Profile Search | Search and filter matching | implemented |
| 1017 | 'Show me profiles from Chennai aged 28–32 working in IT' — 2026 | General / strategy / context | n/a |
| 1018 | Wearable Integration | General / strategy / context | n/a |
| 1019 | Apple Watch / WearOS notification and quick-response actions — 2026 | Notifications | implemented |
| 1020 | GenAI Match Explanation | Subscriptions and payments | implemented |
| 1021 | Generative AI explains why two profiles are compatible in natural language — 2026 | General / strategy / context | n/a |
| 1022 | VR Family Introduction | General / strategy / context | n/a |
| 1023 | VR-based family introduction experience using Meta Quest integration — 2027 | General / strategy / context | n/a |
| 1024 | Federated Identity | General / strategy / context | n/a |
| 1025 | Single matrimony identity usable across community-specific platforms — 2027 | General / strategy / context | n/a |
| 1026 | Real-Time Translation | General / strategy / context | n/a |
| 1027 | AI-powered in-chat translation for cross-language couples — 2026 | Realtime chat | implemented |
| 1028 | Offline Mode | General / strategy / context | n/a |
| 1029 | Full profile browsing and response queuing while offline — 2026 | General / strategy / context | n/a |
| 1030 | 15.4 Success Metrics & KPIs | Admin and analytics | implemented |
| 1031 | The ultimate measure of a matrimony platform's success is not just engagement but real-world marriages facilitated. The following KPIs align business metrics with this human outcome. | Admin and analytics | implemented |
| 1032 | Target / Benchmark | General / strategy / context | n/a |
| 1033 | Monthly Active Users (MAU) | General / strategy / context | n/a |
| 1034 | 25%+ of registered user base | General / strategy / context | n/a |
| 1035 | Free-to-Paid Conversion Rate | General / strategy / context | n/a |
| 1036 | 8–12% (industry benchmark) | General / strategy / context | n/a |
| 1037 | Average Revenue Per User (ARPU) | General / strategy / context | n/a |
| 1038 | ₹3,500 – ₹8,000 per year | General / strategy / context | n/a |
| 1039 | Day-30 Retention Rate | General / strategy / context | n/a |
| 1040 | 40%+ for registered users | General / strategy / context | n/a |
| 1041 | Match Interaction Rate | General / strategy / context | n/a |
| 1042 | 60%+ of daily active users sending or receiving interest | Interests/shortlist flow | implemented |
| 1043 | Profile to Connection Rate | General / strategy / context | n/a |
| 1044 | 15–25% of sent interests result in mutual connection | Interests/shortlist flow | implemented |
| 1045 | Video Call Completion Rate | Voice/video calls | implemented |
| 1046 | 30%+ of connections lead to at least one video call | Voice/video calls | implemented |
| 1047 | Success Story Rate | General / strategy / context | n/a |
| 1048 | 2–5% of premium members report successful marriage | Verification/safety/reporting | implemented |
| 1049 | Verified Profile Ratio | General / strategy / context | n/a |
| 1050 | 70%+ of active profiles with at least mobile verification | Verification/safety/reporting | implemented |
| 1051 | Fake Profile Detection Rate | General / strategy / context | n/a |
| 1052 | < 0.5% of active profiles are fraudulent (post-moderation) | Admin and analytics | implemented |
| 1053 | Customer Satisfaction (CSAT) | General / strategy / context | n/a |
| 1054 | 4.2+ out of 5.0 on app store reviews | General / strategy / context | n/a |
| 1055 | Net Promoter Score (NPS) | General / strategy / context | n/a |
| 1056 | > 40 (good) / > 60 (excellent) for premium users | Subscriptions and payments | implemented |
| 1057 | Appendix A — Glossary of Terms | General / strategy / context | n/a |
| 1058 | Term | General / strategy / context | n/a |
| 1059 | Definition | General / strategy / context | n/a |
| 1060 | Kundali / Horoscope | General / strategy / context | n/a |
| 1061 | Vedic astrological chart generated from a person's birth details | General / strategy / context | n/a |
| 1062 | Manglik | General / strategy / context | n/a |
| 1063 | A person with Mars in specific astrological positions; traditionally matched with another Manglik | General / strategy / context | n/a |
| 1064 | Gun Milan | General / strategy / context | n/a |
| 1065 | 36-point horoscope compatibility scoring system in Vedic astrology | Search and filter matching | implemented |
| 1066 | Biodata | General / strategy / context | n/a |
| 1067 | A structured personal profile document used in arranged marriage introductions | General / strategy / context | n/a |
| 1068 | Non-Resident Indian — Indian national residing outside India | General / strategy / context | n/a |
| 1069 | eKYC | General / strategy / context | n/a |
| 1070 | Electronic Know Your Customer — digital identity verification using Aadhaar | Verification/safety/reporting | implemented |
| 1071 | Monthly Active Users | General / strategy / context | n/a |
| 1072 | ARPU | General / strategy / context | n/a |
| 1073 | Average Revenue Per User | General / strategy / context | n/a |
| 1074 | Monthly Recurring Revenue | General / strategy / context | n/a |
| 1075 | CSAT | General / strategy / context | n/a |
| 1076 | Customer Satisfaction Score | General / strategy / context | n/a |
| 1077 | Net Promoter Score | General / strategy / context | n/a |
| 1078 | JSON Web Token — compact, URL-safe token for authentication | General / strategy / context | n/a |
| 1079 | WebRTC | Voice/video calls | implemented |
| 1080 | Web Real-Time Communication — browser-native video/audio calling protocol | General / strategy / context | n/a |
| 1081 | Content Delivery Network — geographically distributed servers for fast content delivery | General / strategy / context | n/a |
| 1082 | DPDP Act | General / strategy / context | n/a |
| 1083 | Digital Personal Data Protection Act, 2023 — India's primary data protection law | General / strategy / context | n/a |
| 1084 | GDPR | General / strategy / context | n/a |
| 1085 | General Data Protection Regulation — EU data privacy law | Verification/safety/reporting | implemented |
| 1086 | Service Level Agreement — committed performance guarantees | General / strategy / context | n/a |
| 1087 | Recovery Time Objective — maximum time to restore a service after failure | General / strategy / context | n/a |
| 1088 | Recovery Point Objective — maximum acceptable data loss in time | General / strategy / context | n/a |
| 1089 | Appendix B — Document History | General / strategy / context | n/a |
| 1090 | Version | General / strategy / context | n/a |
| 1091 | Date | General / strategy / context | n/a |
| 1092 | Changes | General / strategy / context | n/a |
| 1093 | March 2026 | General / strategy / context | n/a |
| 1094 | Initial release — complete platform documentation | General / strategy / context | n/a |
| 1095 | © 2026 Matrimony Platform — All Rights Reserved. This document is confidential and intended for internal use only. | General / strategy / context | n/a |

## Implementation order

1. Search and filter matching — implemented (182 requirement lines)
2. Verification/safety/reporting — implemented (178 requirement lines)
3. Realtime chat — implemented (138 requirement lines)
4. Subscriptions and payments — implemented (138 requirement lines)
5. Admin and analytics — implemented (85 requirement lines)
6. Interests/shortlist flow — implemented (70 requirement lines)
7. Notifications — implemented (51 requirement lines)
8. OTP authentication — implemented (50 requirement lines)
9. Voice/video calls — implemented (36 requirement lines)
10. Onboarding/profile wizard — implemented (21 requirement lines)