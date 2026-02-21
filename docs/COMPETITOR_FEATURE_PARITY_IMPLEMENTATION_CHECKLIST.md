# Competitor Feature Parity Implementation Checklist

Reference planning doc: `docs/COMPETITOR_FEATURE_ADAPTATION_PLAN.md`

## Goal
Build a structured implementation plan so this project can support the combined feature/filter set observed across:
- Shaadi
- BharatMatrimony
- Jeevansathi
- Elite Matrimony
- Vivaah

This file is execution-focused: what to build, in what order, and how to track progress.

## Scope and Assumptions
- Source input is your compiled competitor analysis (feature discovery baseline).
- Focus is on product capability parity, not exact UI cloning or brand-specific offerings.
- Priority is: stable core platform first, then full filter parity, then premium/assisted services.

## Current Baseline (Repo Snapshot)
Status derived from current codebase (`backend/`, `frontend/`, `docs/`) as of this planning pass.

### Already Present (full or near-full)
- [x] Auth flows (OTP/email/social entry points)
- [x] Profile creation and update
- [x] Core matchmaking + interests + mutual matches
- [x] Real-time chat baseline (Socket.IO)
- [x] Photo upload and primary photo handling
- [x] Verification model + submit status flows
- [x] Pricing/subscription foundation (mock payment flow)
- [x] Shortlist basics
- [x] Horoscope compatibility baseline
- [x] Admin dashboard basics

### Partial (exists but needs parity upgrade)
- [ ] Search filters in UI are ahead of backend query support
- [ ] Filter value normalization (UI labels vs DB enums/bands)
- [ ] Saved search model exists but no complete CRUD + UX flow
- [ ] Premium plans exist but not aligned to multi-tier market packages
- [ ] Verification exists but no full trust badges policy + enforcement rules
- [ ] Privacy controls exist partially, not a full privacy matrix
- [ ] Astrology is demo-level, not production-grade multi-language kundli flow
- [ ] Review/shortlist/verification route consistency needs cleanup

### Missing (major parity gaps)
- [ ] Unified search modes: Regular / Advanced / Keyword / ID
- [ ] Full “with photo / with horoscope / online now / premium only” filters
- [ ] Children filter and explicit residential status filter
- [ ] Search by profile ID
- [ ] Geo hierarchy parity (country-state-district-city) with master data
- [ ] Dedicated assisted matchmaking workflows (relationship manager CRM)
- [ ] Spotlight/Bold listing and profile boost inventory
- [ ] Voice/video calls with access control and audit
- [ ] Money-back/refund policy automation
- [ ] Offline centers/events workflow (if in scope)
- [ ] Regional/community portal architecture (SEO + taxonomy)

---

## Phase Plan

## Phase 0 - Foundation and Data Contracts (P0)
Objective: make filter/search architecture reliable before adding new features.

### Backend and Data
- [ ] Create canonical dictionaries (religion, caste, mother tongue, education, profession, marital status, income bands, diets, countries/states/cities, horoscope enums).
- [ ] Decide dictionary source strategy:
  - [ ] Static seed tables + admin-managed overrides
  - [ ] or curated JSON snapshots + migration scripts
- [ ] Add normalized lookup tables and IDs (avoid free-text drift for filterable fields).
- [ ] Add DB indexes for heavy filters:
  - [ ] gender, DOB/age range, height, marital status
  - [ ] religion/caste/sub-caste/mother tongue
  - [ ] country/state/city
  - [ ] education/profession/income
  - [ ] verification flags + subscription tier
- [ ] Standardize auth identity key usage (`req.user.sub` consistently).
- [ ] Fix route/controller mismatches affecting shortlist/reviews/verification.

### API
- [ ] Introduce filter metadata endpoints:
  - [x] `GET /api/meta/filters`
  - [x] `GET /api/meta/locations`
  - [x] `GET /api/meta/communities`
- [ ] Version and freeze query contract for search/matches endpoint.
- [ ] Add request validation for all filter payloads.

### Frontend
- [x] Replace hardcoded filter options with metadata fetch.
- [ ] Add centralized filter schema/type mapping.
- [x] Add graceful fallback when metadata fetch fails.

### Exit Criteria
- [ ] Same filter labels/values used across DB, API, frontend.
- [ ] No local-only filtering that bypasses backend truth.

---

## Phase 1 - Core Search and Filter Parity (P1)
Objective: implement complete profile discovery parity from competitor baseline.

### Search Modes
- [x] Regular Search
- [x] Advanced Search
- [x] Keyword Search
- [x] Search by Profile ID

### Must-Have Filters (Functional)
- [x] Gender (seeking bride/groom)
- [x] Age min/max
- [x] Height min/max (cm + ft/in display)
- [x] Marital status
- [ ] Have children
- [x] Religion
- [x] Caste/Community (multi-select + searchable)
- [x] Mother tongue
- [ ] Country
- [x] State
- [ ] District/City
- [x] Education (multi-select)
- [x] Profession
- [x] Income range
- [ ] Residential status

### Profile/Visibility Filters
- [x] With photo only
- [ ] Photo visibility type (public/protected/request access)
- [x] With horoscope only
- [x] Online now
- [x] Premium members only
- [x] Verified profiles only

### Astrology Filters (searchable)
- [ ] Rashi/Zodiac
- [ ] Nakshatra
- [ ] Gothra
- [ ] Dosha/Manglik preference

### UX + Query Behavior
- [x] Filter chips with remove/reset all
- [x] Preset save/load (saved searches)
- [x] Deep-linkable search URLs
- [x] Pagination + sort (relevance/newest/compatibility/activity)
- [ ] Empty-state and no-result recommendation handling

### Exit Criteria
- [ ] All above filters run server-side with tested query results.
- [ ] Search latency meets agreed SLA under realistic dataset.

---

## Phase 2 - Matching, Communication, and Trust Upgrades (P2)
Objective: strengthen engagement and trust features already present at baseline.

### Matching and Discovery
- [x] Replace demo/random match score with deterministic scoring model.
- [x] Add compatibility explanation card (why profile matched).
- [ ] Add profile spotlight ranking logic (paid feature-ready).
- [ ] Add “who viewed me” upsell gating for free/premium tiers.

### Communication
- [ ] In-app direct messaging policy by plan tier.
- [ ] Secure voice call (without phone number disclosure).
- [ ] Secure video call (WebRTC or provider integration).
- [ ] Chat presence/last seen/read receipts controls.

### Trust and Verification
- [ ] Badge taxonomy:
  - [ ] phone verified
  - [ ] email verified
  - [ ] ID verified
  - [ ] profile screened/manual verified
- [ ] Blue tick rules + renewal logic.
- [ ] Verification queue tooling in admin.
- [ ] Fraud/risk flags and escalation workflow.

### Exit Criteria
- [ ] Trust badges are enforceable and reflected in filters/search ranking.
- [ ] Communication features honor privacy and plan limits.

---

## Phase 3 - Premium, Monetization, and Services Parity (P3)
Objective: implement flexible packages similar to market leaders.

### Package System
- [ ] Plan catalog model with duration-based tiers (3/6/12 months + variants).
- [ ] Feature entitlements per tier:
  - [ ] contact access caps
  - [ ] direct messages
  - [ ] chat unlock policy
  - [ ] spotlight/bold listing credits
  - [ ] relationship manager access
- [ ] Coupon/offer engine (partner discounts, campaigns).
- [ ] Free trial / free credits support.

### Billing and Policies
- [ ] Production payment gateway integration with signature verification.
- [ ] Refund workflow (time-boxed guarantee rules).
- [ ] Subscription renewals/cancellation/proration rules.
- [ ] Payment and entitlement audit logs.

### Premium Discovery Features
- [ ] Spotlight listing inventory and scheduler
- [ ] Bold listing style flag
- [ ] Priority response/quick connect channels

### Exit Criteria
- [ ] Entitlements are enforced API-side and UI-side.
- [ ] Admin can manage plans without code changes.

---

## Phase 4 - Assisted Matchmaking and Elite Layer (P4)
Objective: support concierge/assisted services and high-intent users.

### Assisted Services
- [ ] Relationship manager CRM module:
  - [ ] assign manager to member
  - [ ] collect preference notes
  - [ ] manage curated recommendations
  - [ ] track calls/meetings/outcomes
- [ ] Match curation dashboard for RM team.
- [ ] Family interaction scheduling workflow.

### Elite Offering
- [ ] Elite package SKUs (service package vs success-fee package).
- [ ] Confidential profile handling mode.
- [ ] Limited visibility profiles (invite-only discoverability).

### Offline/Events (optional scope)
- [ ] Event listing and registration
- [ ] In-person support center operations model
- [ ] Lead routing to city/regional teams

### Exit Criteria
- [ ] Assisted workflows auditable end-to-end.
- [ ] Elite privacy mode technically isolated and policy-compliant.

---

## Phase 5 - Regionalization, Ecosystem, and Growth (P5)
Objective: support large catalog and regional/community-scale discoverability.

### Regional/Community Expansion
- [ ] Community-specific landing architecture (SEO-ready).
- [ ] Multi-language content and labels for major Indian languages.
- [ ] Mother tongue + caste catalogs at scale (governed updates).
- [ ] Country-specific onboarding variants for NRI audiences.

### Ecosystem Integrations (if desired)
- [ ] Astrology consultation integration
- [ ] Wedding service marketplace integrations
- [ ] Partner offers and affiliate tracking

### Growth and Operations
- [ ] Recommendation experiments (A/B infrastructure)
- [ ] Lifecycle messaging (interests, reminders, nudges)
- [ ] Analytics dashboards for funnel and conversion

### Exit Criteria
- [ ] Regional pages and filters are fully indexable and measurable.

---

## Cross-Cutting Technical Checklist

### Data Model Changes
- [ ] Add `hasChildren` and child-living arrangement fields.
- [ ] Add `residentialStatus` field.
- [ ] Add online presence fields (last seen / online now).
- [ ] Add profile visibility controls (photo/contact/privacy levels).
- [ ] Add plan entitlement mapping table(s).
- [ ] Add spotlight/bold credits ledger.

### API Surface
- [x] `GET /api/search` (multi-mode, paginated, fully filterable)
- [x] `POST /api/search/saved` and `GET /api/search/saved`
- [ ] `GET /api/profile/:id` with privacy-respecting projection
- [ ] `GET /api/subscription/entitlements`
- [ ] `POST /api/call/session` (voice/video token issuance)
- [x] `GET /api/verification/badges`

### Frontend Workstreams
- [x] Unified search page with mode switch + advanced drawer.
- [ ] Dynamic filter sections: Basic, Advanced, Astro, Lifestyle.
- [x] Saved search UX (create, rename, apply, delete).
- [x] Premium-only feature locks and upgrade prompts.
- [ ] Verification and trust surfaces (badges, explanation tooltips).

### Performance
- [ ] Query performance testing on 100k+ synthetic profiles.
- [ ] Add caching strategy for metadata and common filter combos.
- [ ] Add background job for ranking/materialized search views (if needed).

### Security/Privacy/Compliance
- [ ] Fine-grained privacy consent for profile/contact visibility.
- [ ] Rate limits on search/profile views/chat/call setup.
- [ ] Abuse reporting + moderation SLAs.
- [ ] PII retention/deletion workflows.

### QA and Release
- [ ] Unit tests for filter parser + scoring engine.
- [ ] Integration tests for every filter combination class.
- [ ] Contract tests for metadata endpoints.
- [ ] E2E tests for free vs premium entitlements.
- [ ] Release checklist by phase with rollback strategy.

---

## Competitor Parity Tracking Matrix (Working)
Use this table for sprint-level updates.

| Area | Target Parity | Current | Phase |
|---|---|---|---|
| Search Modes | Regular/Advanced/Keyword/ID | Partial | P1 |
| Core Filters | Demographic + religion + location + education + income | Partial | P1 |
| Profile Visibility Filters | photo/horoscope/online/premium/verified | Missing/Partial | P1 |
| Astrology | Search + compatibility + language variants | Partial | P1-P2 |
| Communication | Chat + voice/video + privacy-safe calling | Chat only baseline | P2 |
| Verification | Multi-badge trust framework | Partial | P2 |
| Premium Plans | Multi-tier duration + boosts + RM | Partial | P3 |
| Spotlight/Bold | Paid ranking boosts | Missing | P3 |
| Assisted Matchmaking | RM workflow + curated connections | Missing | P4 |
| Elite/Confidential | Confidential profile paths | Missing | P4 |
| Regional Portals | Community/language expansion | Missing | P5 |
| Refund/Guarantee Ops | Policy + automation + audit | Missing | P3 |

---

## Sprint Execution Template
Repeat per sprint:

- [ ] Finalize sprint scope from this checklist.
- [ ] Create technical design notes (schema + API + UI impacts).
- [ ] Implement backend contracts and migrations.
- [ ] Implement frontend flows with real API integration.
- [ ] Add automated tests (unit + integration + E2E as relevant).
- [ ] Run load/performance test for search-related changes.
- [ ] Release behind feature flags where needed.
- [ ] Capture analytics and QA sign-off.

---

## Immediate Next Step (Recommended)
- [ ] Execute Phase 0 first (canonical metadata + contract cleanup), then Phase 1 filter parity.
