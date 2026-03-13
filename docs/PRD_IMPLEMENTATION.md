# PRD Implementation Tracker (2026-03-10)

This tracker maps the `Matrimony_App_PRD.docx` requirements to the current web app implementation. It focuses on the Next.js web experience in this repo. Items requiring AI or paid infrastructure are flagged as deferred per the “no AI / zero INR” constraint.

## Constraints Applied
1. No AI features (per explicit user instruction).
2. Zero-INR / zero-infra baseline (Cloudflare free tier compatible).

## 1. Vision & Market (Sections 1-3)
- Vision, market sizing, personas: Documented for product alignment. No code changes required.

## 2. Feature Specifications (Section 4)

### 4.1 Onboarding & Registration
- F-01 Quick registration (OTP + SSO): Implemented (phone OTP + Google).
- F-02 Profile wizard: Implemented (step-1 through step-5).
- F-03 Family/parent profile + consent: Implemented (step-1 “profile created for” + consent).
- F-04 Photo upload mandatory: Implemented (step-4 blocks completion without photo).
- F-05 Personality quiz: Implemented (quiz page + saved in profile extension).

### 4.2 Profile Management
- Personal, education, career, lifestyle, religious: Implemented (expanded profile edit + backend persistence).
- Family details (parents, siblings, family type/values): Implemented (step-4 + profile + biodata).
- Horoscope fields: Implemented (profile fields + kundli page).
- Partner preferences mirror: Implemented (step-5 + preferences API; partial).
- Video introduction: Implemented (profile edit + link).
- Biodata PDF: Implemented (printable biodata page).
- Profile completeness score: Implemented (onboarding service + profile UI).

### 4.3 Verification & Trust
- Level 1 Phone verified: Implemented (OTP).
- Level 2 ID verified: Implemented (verification page + submit).
- Level 3 Employment verified: Deferred (needs document flow + review).
- Level 4 Photo verified (liveness): Deferred (needs liveness infra).
- Level 5 PRIME verified: Deferred (manual ops + policy).
- Fraud detection: Deferred (AI constraint).

### 4.4 Matchmaking Engine
- Compatibility score: Implemented (rule-based placeholder).
- Behavioral learning + AI matching: Deferred (AI constraint).
- “Recommended because” reasons: Implemented (matches page).
- Daily match alerts, push: Deferred (needs notification infra).

### 4.5 Search & Filters
- Basic filters: Implemented (matches page).
- Advanced filters (income, diet, verified toggle): Implemented in UI.
- Saved filters: Implemented (saved searches in matches filter panel).

### 4.6 Communication Features
- Interests/likes: Implemented.
- In-app messenger: Implemented (chat UI + API).
- SecureConnect voice/video: Deferred (VoIP infra).
- LiveMatch events: Deferred (infra).

### 4.7 Privacy & Safety
- Photo privacy / profile visibility / last seen: Implemented in settings + backend privacy service.
- Block/report: Implemented (profile report).
- Incognito browse, contact control, watermarking: Deferred (infra + policy).

### 4.8 Horoscope & Kundli
- Kundli page + inputs: Implemented (kundli screen).
- Detailed guna scoring & dosha: Deferred (needs algorithm service).

### 4.9 Family Portal
- Sub-accounts, co-browse, family shortlist: Deferred (needs auth/roles + sharing model).

### 4.10 Notifications & Engagement
- Notifications UI + feed: Implemented (API-derived notifications, mark-all-read).
- Real-time + scheduled notifications: Deferred (infra).

### 4.11 Subscription Tiers
- Pricing page with tiers: Implemented (pricing UI + comparisons).
- Feature gating: Partial (no enforced gates in UI).

## 3. UI/UX Design (Section 5)
- Warm, trust-first palette: Implemented (brand tokens updated).
- Fonts: Implemented (Poppins headings, Inter body).
- Card/list hybrid discover: Implemented (matches grid/list toggle).
- Profile detail sections & sticky CTAs: Implemented.

## 4. Technical Architecture (Section 6)
- Web stack: Matches current repo (Next.js + Node/Express).
- Real-time, calls, AI services: Deferred (infra).
- Security controls: Partial (headers, privacy settings, verification).

## 5. Launch Roadmap (Section 7)
Web MVP features are aligned with Phase 1, with AI/real-time infra deferred per constraints.

## 6. Success Metrics / Risks (Sections 8-9)
Metrics captured in analytics stubs; full instrumentation deferred.

---

## Next Implementation Targets
1. Enforce feature gating for premium plans (UI + API).
2. Notification pipeline (email + web push) once infra allowed.
