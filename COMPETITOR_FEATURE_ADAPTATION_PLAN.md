# Competitor Feature Adaptation Plan

Date: 2026-02-20  
Status: Planning baseline (pre-implementation)  
Scope: Search and filtering, UI/UX system, verification/security, matching intelligence

## 0. Progress Update (2026-02-20)

Completed in this pass:
- Added metadata APIs:
  - `GET /api/meta/filters`
  - `GET /api/meta/locations`
  - `GET /api/meta/communities`
- Wired metadata route in backend bootstrap (`/api/meta`).
- Updated `search` and `matches` pages to consume backend metadata with fallback behavior.
- Extended match filtering normalization in backend for inconsistent label/value formats.
- Replaced random match score in `/matches` service with deterministic scoring baseline.
- Added multi-mode search execution API with pagination/sort:
  - `GET /api/search`
  - `GET /api/search/execute`
- Added saved search APIs:
  - `GET /api/search/saved`
  - `POST /api/search/saved`
  - `PATCH /api/search/saved/:id`
  - `DELETE /api/search/saved/:id`
- Added verification badge API:
  - `GET /api/verification/badges`
- Added privacy settings APIs:
  - `GET /api/users/privacy`
  - `POST /api/users/privacy`
- Added deep-linkable search URL sync and filter chips in search UI.

Next in queue:
- Add canonical metadata persistence strategy (DB-backed dictionaries instead of static service data).
- Implement missing advanced filters server-side (children, residential status, premium visibility flags).
- Add saved-search scheduling hooks (daily alerts) and share/export payload format.

## 1. Objective

Create an execution-ready plan to adapt high-impact features from BharatMatrimony, Shaadi, Jeevansathi, and Vivaah without copying weak UX patterns or adding unstable complexity.

This document is the implementation source of truth before feature build-out.

## 2. What We Should Learn and Adapt

### A. Core Search and Filtering (Critical)

Adopt:
- Multi-mode discovery: Regular, Advanced, Keyword, Profile ID search
- Strong filter depth: caste, religion, mother tongue, education, profession, income, marital status, children, residential status
- Quick filters + advanced filters + premium locked filters
- Searchable dropdowns and location hierarchy
- Save search and daily match alerts

Avoid:
- Dumping too many filters at once
- Hiding critical filters behind deep accordions
- Frontend-only filtering that diverges from backend truth

### B. UI and Design (High)

Adopt:
- Mobile-first, clean layout, strong hierarchy
- Consistent visual system (colors, type scale, spacing, states)
- Progressive disclosure without losing discoverability
- Fast perceived performance

Avoid:
- Dated, crowded form-heavy layouts
- Inconsistent styling patterns across pages
- Low-contrast components or inaccessible controls

### C. Verification and Security (Critical)

Adopt:
- Multi-layer verification with clear badge taxonomy
- Strong privacy controls (photo/contact/profile visibility)
- Report and abuse pipeline with moderation workflow
- Safety education surfaces and anti-scam monitoring

Avoid:
- Single-signal verification reliance
- Badge logic without enforcement in ranking/filtering
- Non-scalable manual-only processes

### D. Matching and Recommendations (High)

Adopt:
- Rule-based eligibility first, then scoring, then ranking
- Explainable match reasons
- Behavior feedback loop with measurable outcomes
- Verified profile boost with controlled weight

Avoid:
- Random score generation
- Black-box ranking without observable quality metrics
- Over-volume recommendations that reduce intent quality

## 3. Current Repo Gap Snapshot (As-Is)

### Search and Filter State
- `frontend/src/app/(main)/search/page.js` and `frontend/src/app/(main)/matches/page.js` contain rich filter UI but mostly hardcoded option sets.
- `backend/src/services/matching.service.js` supports only a subset of filters server-side (gender, age, religion, caste, maritalStatus, city/state, education, profession, income).
- Match score now uses a deterministic baseline in `matching.service.js`; explainability and behavior-weight learning are still pending.
- Filter value contract is inconsistent (example: income and marital labels differ across frontend/backend).
- `SavedSearch` model exists in Prisma, but no CRUD API/routes are exposed.

### Verification and Privacy State
- Verification flow exists: `backend/src/routes/verification.routes.js` + `frontend/src/components/IdentityVerification.jsx`.
- Current implementation is mostly ID submission/status; no full tiered badge policy yet.
- Privacy controls are present in UI (`frontend/src/app/(main)/settings/page.js`) but backend endpoint alignment is incomplete (`/user/privacy` call has no matching route).

### Matching Intelligence State
- Baseline compatibility exists in `backend/src/services/interaction.service.js`; `/matches` now uses deterministic baseline scoring but still needs explainability and learning loop depth.
- No explainability payload ("why matched") in match results.

## 4. Product Adaptation Decisions (Finalized for Build)

### Search IA (Information Architecture)

Implement this structure:
- Quick filters: Gender, Age range, Religion, Location smart search
- Advanced filters: Height, Marital status, Have children, Education, Profession, Income, Mother tongue, Caste, Residential status, Photo settings
- Premium locked filters: Online now, Horoscope compatibility, Verification level, Last active
- Save and share: Saved search, daily alerts, share criteria

### UX Rules
- Critical filters always visible; advanced is collapsible but discoverable
- No more than 6 controls visible in "quick" section
- All search requests use one backend contract (no client-side truth drift)
- Every locked filter remains visible with upgrade CTA (not hidden)

## 5. Execution Strategy (Efficient Delivery)

1. Build canonical metadata once, reuse everywhere.  
2. Stabilize backend search contract before UI expansion.  
3. Ship in vertical slices (schema -> API -> UI -> tests -> metrics).  
4. Use feature flags for premium filters and ranking logic rollout.  
5. Keep one source of truth for enums/dictionaries (not per-page arrays).

## 6. Phased Implementation Roadmap

## Phase 0 - Data and Contract Foundation (Sprint 1)

Goal: remove data mismatch and make search extensible.

Backend/Data:
- Add canonical dictionaries for religion/caste/motherTongue/education/profession/income/maritalStatus/residentialStatus.
- Introduce hierarchical location master (country/state/city/district).
- Add profile fields: `hasChildren`, `residentialStatus`, `lastActiveAt`, `profileVisibility`, `photoVisibility`.
- Add search-friendly indexes.

API:
- Add metadata endpoints:
  - `GET /api/meta/filters`
  - `GET /api/meta/locations`
- Define unified search contract DTO.

Frontend:
- Replace hardcoded filter constants with metadata API.
- Add typed filter mapper with normalization.

Exit criteria:
- UI labels/values map 1:1 with backend accepted values.
- No filter option is hardcoded in search pages except fallback.

## Phase 1 - Core Search and Filtering Parity (Sprints 2-3)

Goal: complete critical filtering and search modes.

Deliverables:
- Search modes: Regular, Advanced, Keyword, Profile ID.
- Full advanced filter support server-side:
  - children, residential status, mother tongue, height range, photo settings, verified/premium, horoscope flags.
- Pagination, sorting, filter chips, reset, deep-linking.
- Saved search CRUD:
  - `POST /api/search/saved`
  - `GET /api/search/saved`
  - `PATCH /api/search/saved/:id`
  - `DELETE /api/search/saved/:id`

Exit criteria:
- Filtered result set is backend-driven.
- All selected filters persist in URL and reload correctly.
- Saved search works end-to-end.

## Phase 2 - UI System and Accessibility Upgrade (Sprint 4)

Goal: modern, consistent, scalable UX.

Deliverables:
- Search page redesign using Quick + Advanced + Premium sections.
- Design tokens (color/type/spacing/radius/shadow/motion) in shared styles.
- Responsive behavior validation (mobile/tablet/desktop).
- Accessibility baseline: keyboard nav, focus states, contrast, semantic labeling.

Exit criteria:
- Consistent visual patterns across search/matches/settings/pricing.
- No critical contrast or keyboard trap issues.

## Phase 3 - Verification, Privacy, and Safety Layers (Sprints 5-6)

Goal: improve trust and reduce abuse risk.

Deliverables:
- Tiered verification model:
  - Tier 1: mobile/email/basic checks
  - Tier 2: ID + liveness
  - Tier 3: enhanced premium verification
- Badge taxonomy and enforcement in ranking/filtering.
- Privacy matrix:
  - hide phone/email/photo/profile scope
  - activity/last seen controls
  - blocklist enforcement
- Report workflow upgrades with moderation statuses and audit trails.

Exit criteria:
- Verification badges are policy-backed and queryable.
- Privacy controls are enforced at API projection level.

## Phase 4 - Matching Engine and Recommendation Quality (Sprints 7-8)

Goal: replace demo ranking with explainable personalization.

Deliverables:
- Deterministic scoring pipeline:
  - eligibility filter -> compatibility score -> ranking boost rules
- Feature set for score:
  - profile compatibility + preference alignment + behavior signals
- Explainability in API response:
  - e.g., "same city", "aligned education preference", "verified profile"
- Daily recommendation batch and feedback capture.

Exit criteria:
- Random score logic removed.
- Match quality metrics are observable and improving.

## 7. Priority Matrix (Build Order)

Must have first:
- Contract normalization, metadata APIs, filter parity, saved search
- Verification tier 1 + badge enforcement
- Deterministic matching baseline

Should have next:
- Premium filters, profile visibility controls, daily recommendation pipeline

Could have later:
- RM-assisted concierge workflows
- Offline events and regional portal SEO expansion

## 8. Engineering Workstreams

Backend workstream:
- Schema migrations + metadata services + unified search engine + verification policy engine

Frontend workstream:
- Search IA refactor + reusable filter components + premium lock UX + saved search UX

Trust/Safety workstream:
- Privacy controls + reporting pipeline + moderation tooling

Data/ML workstream:
- Scoring model v1 + explainability + metrics instrumentation

## 9. Key Risks and Mitigations

Risk: filter sprawl causes slow queries.  
Mitigation: index strategy + query profiling + capped option cardinality.

Risk: UI/backend value mismatch reappears.  
Mitigation: metadata-only filter options + contract tests.

Risk: feature overload hurts discoverability.  
Mitigation: quick filter cap + progressive disclosure + UX testing.

Risk: trust badges become cosmetic.  
Mitigation: enforce badges in ranking/filter logic and admin workflows.

## 10. Sprint 1 Kickoff Checklist

- [ ] Finalize canonical filter dictionary schema.
- [ ] Add Prisma migration for missing profile/search fields.
- [x] Implement `/api/meta/filters` and `/api/meta/locations`.
- [x] Refactor search UI to consume metadata endpoints.
- [ ] Add integration tests for filter request validation.
- [ ] Create feature flags for premium filters and ranking strategy.
- [ ] Define baseline metrics: search latency, search-to-interest, match acceptance.

## 11. Definition of Done (Per Feature)

- Backend contract implemented and validated
- Frontend integrated without hardcoded fallback (except network fail-safe)
- Unit + integration tests pass
- Tracking events added
- Documentation updated in `docs/`
- Feature behind flag if high risk

## 12. Immediate Next Step

Start Phase 0 implementation from this plan, beginning with metadata and contract normalization.  
No major feature expansion should begin until Phase 0 exit criteria is met.
