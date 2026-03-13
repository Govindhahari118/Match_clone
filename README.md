# MatrimonyConnect

![Zero-INR](https://img.shields.io/badge/Zero--INR-Yes-0b6e4f)
![Zero-Infra](https://img.shields.io/badge/Zero--Infra-Yes-0b6e4f)
![No-AI](https://img.shields.io/badge/AI-Not%20Used-1f2a44)
![Cloudflare-Free](https://img.shields.io/badge/Cloudflare-Free%20Tier-1f6feb)
![Fire-and-Forget](https://img.shields.io/badge/Fire--and--Forget-Yes-0b6e4f)

A privacy-first, self-serve matrimony platform engineered for zero-INR feature development, zero external services, and a fire-and-forget operating model. No AI features. No paid APIs. Cloudflare free tier only, with a planned upgrade to the USD 5 tier after profits are proven.

---

## Quick start

```bash
npm run dev:up
```

- On Windows: uses PowerShell bootstrap.
- On Linux/macOS: launches frontend dev server at `http://localhost:8000`.

Without Docker:

```bash
npm run dev:up:nodocker
```

Frontend:

- http://localhost:8000

Backend:

- http://localhost:5000/health

---

## At a glance

| Topic | Summary |
| --- | --- |
| Mission | Build a trusted matrimony platform with near-zero operating cost and no AI features. |
| Core constraints | Zero INR for features, zero paid services, zero manual ops dependency. |
| Trust model | Self-attested profiles, completion checklists, and privacy-first controls. |
| Matching | Rule-based filters and transparent ranking only. |
| Ops model | Self-serve, static guidance, and edge protections. |
| Edge | Cloudflare free tier (DNS, CDN, TLS, basic security). |

---

## Table of contents

1. Executive summary
2. Non-negotiables
3. Definitions (Zero-INR, Zero-Infra, Fire-and-Forget)
4. Product philosophy
5. Trust model (self-serve)
6. No-AI policy
7. Architecture overview
8. Tech stack
9. Repo layout
10. Local development
11. Configuration
12. Deployment model
13. Security baseline
14. Performance baseline
15. Privacy baseline
16. Scalability baseline
17. Product journeys
18. Marketplace mechanics
19. Monetization model
20. Metrics and guardrails
21. Feature catalog (Top 50)
22. Feature catalog (Top 100)
23. UX consistency rules
24. Release checklist
25. QA and test strategy
26. Risk register
27. Incident response
28. Reference repositories and docs
29. FAQ
30. License
31. Appendix A: Decision gate (50-round filter)
32. Appendix B: API map (compressed)
33. Appendix C: Data dictionary (compressed)
34. Appendix D: Security headers
35. Appendix E: Performance budgets
36. Appendix F: SEO and discoverability
37. Appendix G: Copy bank
38. Appendix H: Accessibility checklist
39. Appendix I: Operational runbook
40. Appendix J: Style tokens
41. Appendix K: Route map (frontend)
42. Appendix L: Component inventory
43. Appendix M: Onboarding field checklist
44. Appendix N: Matching rules (deterministic)
45. Appendix O: Privacy controls matrix
46. Appendix P: Copy and tone guidelines
47. Appendix Q: Security testing checklist
48. Appendix R: Performance testing checklist
49. Appendix S: Data deletion and export
50. Appendix T: Logging and telemetry
51. Appendix U: Accessibility QA
52. Appendix V: Deployment checklist
53. Appendix W: Feature flags registry
54. Appendix X: Experiment playbook
55. Appendix Y: Glossary
56. Appendix Z: API reference (detailed)
57. Appendix AA: Socket events
58. Appendix AB: Environment variables
59. Appendix AC: Rate limits and throttling
60. Appendix AD: Data model (detailed)
61. Appendix AE: Filters and query params
62. Appendix AF: Privacy settings schema
63. Appendix AG: Error codes
64. Appendix AH: Pagination conventions
65. Appendix AI: Scripts and commands
66. Appendix AJ: Build artifacts and caching
67. Appendix AK: Database migrations
68. Appendix AL: Security and compliance mapping
69. Appendix AM: Contribution guide
70. Appendix AN: Roadmap (zero-INR)
71. Interactive reference

---

## Executive summary

MatrimonyConnect is a strict-constraint product. The goal is not to win with expensive services or AI automation. The goal is to win through clarity, trust, and disciplined UX that compounds conversion, retention, and referrals while operating at near-zero cost. The entire stack is designed to run without paid services, without AI, and without ongoing manual operations.

Key outcomes targeted:

- Increase visitor-to-profile conversion through frictionless onboarding.
- Increase trust with self-attested, completion-based signals.
- Improve match quality via transparent, rule-based filtering and ranking.
- Minimize operational burden by designing for self-serve flows.
- Scale safely on Cloudflare free tier.

---

## Non-negotiables

These are hard constraints. No feature is accepted if any of these are violated.

- Zero INR for new features.
- Zero paid APIs or SaaS.
- Zero manual ops dependency.
- No AI features. Ever.
- Cloudflare free tier only until profits are proven.
- Privacy-first by default.
- Self-serve trust mechanisms only.

---

## Definitions

### Zero-INR

Zero-INR means the feature requires no paid services, no paid APIs, and no ongoing spend to function. It can be shipped with code and content only.

### Zero-Infra

Zero-Infra means no new infrastructure services are introduced beyond Cloudflare free tier. No paid queues, analytics, or messaging services. The system must remain deployable on a minimal single-node setup without third-party dependencies.

### Fire-and-Forget

Fire-and-forget means after launch:

- The feature runs without continuous human moderation.
- The feature does not rely on manual verification or support.
- The feature is robust to low-touch operations.

---

## Product philosophy

MatrimonyConnect is built on three loops that reinforce each other without AI:

1. Trust loop: Better data quality increases user confidence.
2. Engagement loop: Better confidence leads to more interactions.
3. Growth loop: More interactions produce stories and referrals.

Every feature must directly strengthen at least one loop without adding operational complexity.

---

## Trust model (self-serve)

Trust is earned through transparency, not automation. The platform focuses on self-attested and completion-based signals.

Primary trust mechanisms:

- Profile completeness checklist with visible score.
- Self-attestation checkbox confirming accuracy.
- Privacy-first visibility controls.
- Contact details hidden until mutual interest.
- Reporting and blocking tools to preserve quality.

---

## No-AI policy

This repository explicitly rejects AI features. That includes:

- No ML-based scoring or recommendations.
- No AI-powered verification or moderation.
- No AI chat, bots, or automated messaging.
- No external AI APIs or SDKs.

All matching and ranking logic is transparent, rule-based, and deterministic.

---

## Architecture overview

High-level flow:

```
Browser (Next.js)
  -> API (Express)
     -> PostgreSQL
     -> Redis (optional caching or rate-limit)
  -> Socket.IO (real-time chat)

Cloudflare (Free Tier)
  -> DNS + CDN + TLS + basic security
```

Design characteristics:

- Stateless services.
- No paid queues or third-party platforms.
- No external identity verification services.
- Cache and performance optimizations that are free-tier friendly.

---

## Tech stack

| Layer | Choice | Notes |
| --- | --- | --- |
| Frontend | Next.js (App Router) | Static-first where possible. |
| Backend | Node.js + Express | API-first, JSON contracts. |
| Database | PostgreSQL | Relational store for profiles and interactions. |
| Cache | Redis (optional) | Used only if available locally. |
| Realtime | Socket.IO | Real-time chat only. |
| Edge | Cloudflare Free | DNS, CDN, TLS, DDoS. |

---

## Repo layout

```
match/
|- backend/              Express API + Prisma
|- frontend/             Next.js web app (App Router)
|- mobile/               React Native/Expo placeholder
|- docs/                 Product + API docs
|- scripts/              dev-up / dev-down / dev-status
|- runtime/              generated logs + PID files
|- docker-compose.yml    postgres + redis (+ optional api)
\- package.json          workspace helper scripts
```

---

## Local development

With Docker:

```bash
npm run dev:up
```

- On Windows: uses PowerShell bootstrap.
- On Linux/macOS: launches frontend dev server at `http://localhost:8000`.

Without Docker:

```bash
npm run dev:up:nodocker
```

Frontend:

- http://localhost:8000

Backend:

- http://localhost:5000/health

---

## Configuration

Environment variables are stored in `frontend/.env.local` and `backend/.env`. Refer to `.env.local.example` for required fields. Keep secrets out of source control.

Recommended defaults:

- Use local DB for development.
- Use local Redis only if enabled.
- Keep feature flags disabled unless explicitly needed.

---

## Deployment model

The minimal production model:

- One application server hosting API + SSR.
- One database instance for PostgreSQL.
- Cloudflare free tier for DNS, TLS, CDN, and basic protection.

No paid services are required for baseline operation. Cloudflare can be upgraded to a USD 5 tier only after profits are proven.

---

## Security baseline

Minimum security posture required for every release:

- Input validation and output encoding.
- Strict authentication and authorization checks on every route.
- CSRF protection for state-changing requests.
- Secure session handling and rotation on login.
- Rate limits on auth and messaging routes.
- Security headers (CSP, HSTS, X-Frame-Options, Referrer-Policy).
- Dependency scanning and lockfile hygiene.

---

## Performance baseline

Performance targets:

- Fast LCP on landing and search pages.
- Strict image sizing and lazy loading.
- Paginated data loads everywhere.
- Minimal third-party scripts (default: none).

Performance guardrails:

- No heavy animation libraries.
- No unbounded list rendering.
- No large client-side bundles on entry pages.

---

## Privacy baseline

Privacy is default, not optional:

- Contact details hidden until mutual interest.
- Profile visibility controls per user.
- Minimal data stored in public search listings.
- Profile data export and deletion available.

---

## Scalability baseline

Scale without new infra:

- Stateless API servers.
- Deterministic query contracts.
- Caching only on safe, public endpoints.
- Pagination for all collections.
- Feature flags are client-only and safe.

---

## Product journeys

Primary journey:

1. Landing page leads to onboarding.
2. Essential profile data captured.
3. Trust checklist nudges completion.
4. Discovery via filters and saved searches.
5. Shortlist and send interest.
6. Mutual interest reveals contact.

Secondary journey:

- View pricing.
- Review community guidelines.
- Manage privacy and visibility settings.

---

## Marketplace mechanics

Two-sided dynamics require balance without AI:

- Exposure balancing in feeds (rule-based rotation).
- Clear incentives for profile completion.
- Mutual interest gating for contact access.
- Transparent filters to reduce mismatch.

---

## Monetization model

Revenue is optional but supported in the design:

- Free plan with essential flows.
- Paid plans that unlock visibility and advanced filters.
- Pricing pages and upgrade prompts are static until payments are integrated.

No payment service is required in the zero-INR baseline.

---

## Metrics and guardrails

Track only essential metrics without paid analytics:

- Visitor to signup conversion.
- Profile completion rate.
- Interest send rate.
- Mutual interest rate.
- Shortlist retention.

Guardrails:

- Do not ship features that reduce privacy or increase ops.
- Do not ship features that require AI or external verification.

---

## Feature catalog (Top 50)

The highest ROI, zero-INR features. These are P0 and should ship first.

| # | Feature | Why it matters |
| --- | --- | --- |
| 1 | Conversion-first hero form | Increases signup without new services. |
| 2 | Trust proof band | Reduces anxiety and increases completion. |
| 3 | Profile created for toggle | Expands family users. |
| 4 | Guest browsing | Lowers entry friction. |
| 5 | Quick filters | Faster time-to-first match. |
| 6 | Advanced filters | Power users retained. |
| 7 | Saved search presets | Increases return visits. |
| 8 | Deep-linkable search URLs | Shareable and SEO friendly. |
| 9 | Profile completeness meter | Better data quality. |
| 10 | Onboarding stepper | Reduces drop-off. |
| 11 | Profile strength checklist | Increases completion. |
| 12 | Minimal required fields | Baseline data quality. |
| 13 | Visibility defaults | Trust by default. |
| 14 | Self-attestation badge | Trust signaling without ops. |
| 15 | Safety microcopy | Lowers abuse risk. |
| 16 | Block/mute controls | Retention protection. |
| 17 | Report abuse UI | Capture issues early. |
| 18 | Last active status | Reduces wasted outreach. |
| 19 | Photo visibility controls | Protects privacy. |
| 20 | Profile preview | Reduces mistakes. |
| 21 | Shortlist | Engagement and intent capture. |
| 22 | Send interest CTA | Clear next action. |
| 23 | Chat starters | Lower first-message friction. |
| 24 | Viewed-by feed | Reciprocity effect. |
| 25 | Session resume banner | Reduces drop-off. |
| 26 | Success stories section | Social proof loop. |
| 27 | Pricing preview | Early revenue clarity. |
| 28 | Pricing comparison | Upgrade clarity. |
| 29 | Premium badge UI | Aspirational value. |
| 30 | Feature gating prompts | Upsell pressure without ops. |
| 31 | Soft free quota | Upgrade pressure without coercion. |
| 32 | Concierge landing page | Monetizable later. |
| 33 | Sitemap + robots | Discovery and SEO. |
| 34 | Structured data | Search visibility. |
| 35 | Strict image sizing | Faster load. |
| 36 | Lazy loaded images | Lower bandwidth. |
| 37 | Route-level skeletons | Better perceived speed. |
| 38 | Cache headers | Free speed boost. |
| 39 | Minimize third-party scripts | Faster and safer. |
| 40 | Font loading strategy | Better paint timing. |
| 41 | Pagination everywhere | Cost control. |
| 42 | API request limits | Abuse prevention. |
| 43 | Audit logs | Investigate issues quickly. |
| 44 | Strong password policy | Reduce takeovers. |
| 45 | Session expiry | Reduce abuse risk. |
| 46 | Data minimization | Lower legal risk. |
| 47 | Clear legal pages | Compliance and trust. |
| 48 | Help center pages | Reduce support burden. |
| 49 | FAQ on key pages | Reduce drop-off. |
| 50 | Release checklist | Repeatable stability. |

---

## Feature catalog (Top 100)

The full 100-feature list is included here for complete coverage. These are grouped by objective.

### Acquisition and activation (1-10)

| # | Feature | ROI impact |
| --- | --- | --- |
| 1 | Conversion-first hero form | Immediate signup lift. |
| 2 | Trust proof band | Reduces anxiety. |
| 3 | Pricing preview | Shows value early. |
| 4 | Regional landing pages | SEO + relevance. |
| 5 | Community landing pages | Discovery boosts. |
| 6 | Guest browsing | Lower barrier. |
| 7 | Profile created for toggle | Expands audience. |
| 8 | FAQ on landing | Objection handling. |
| 9 | Success stories carousel | Social proof. |
| 10 | CTA repetition | Conversion lift. |

### Trust and safety (11-20)

| # | Feature | ROI impact |
| --- | --- | --- |
| 11 | Privacy defaults conservative | Safer by default. |
| 12 | Photo visibility controls | Protects sensitive data. |
| 13 | Profile visibility tiers | Trust + upsell. |
| 14 | Self-attestation checklist | Signals seriousness. |
| 15 | Safety tips microcopy | Lower support load. |
| 16 | Block and mute controls | Retention protection. |
| 17 | Report abuse UI | Captures issues. |
| 18 | Last active visibility | Reduces wasted outreach. |
| 19 | Profile completeness badge | Increases quality. |
| 20 | Terms and guidelines pages | Clear expectations. |

### Profile quality and onboarding (21-30)

| # | Feature | ROI impact |
| --- | --- | --- |
| 21 | Onboarding stepper | Clear completion path. |
| 22 | Required fields gating | Baseline quality. |
| 23 | Example bios and prompts | Higher quality profiles. |
| 24 | Inline validation | Prevents bad data. |
| 25 | Progressive disclosure | Less intimidation. |
| 26 | Profile preview before publish | Fewer mistakes. |
| 27 | Profile strength meter | Completion motivation. |
| 28 | Missing info reminder | Completion nudges. |
| 29 | Structured dropdowns | Cleaner data. |
| 30 | Draft/resume state | Less abandonment. |

### Discovery and search (31-40)

| # | Feature | ROI impact |
| --- | --- | --- |
| 31 | Quick filters | Faster matches. |
| 32 | Advanced filters | Power users retained. |
| 33 | Saved search presets | Repeat engagement. |
| 34 | Deep-linkable filters | Shareable search. |
| 35 | Filter chips | Faster refinement. |
| 36 | Empty-state suggestions | Prevents abandonment. |
| 37 | Profile comparison view | Decision confidence. |
| 38 | Sorting modes | Perceived control. |
| 39 | With-photo filter | Better quality leads. |
| 40 | Recently active filter | Higher response rates. |

### Engagement and retention (41-50)

| # | Feature | ROI impact |
| --- | --- | --- |
| 41 | Shortlist/save profile | Intent capture. |
| 42 | Send interest CTA | Clear next step. |
| 43 | Chat starter templates | Low friction. |
| 44 | Viewed-by feed | Reciprocity effect. |
| 45 | Session resume banner | Return visits. |
| 46 | New matches banner | Recency effect. |
| 47 | Success story spotlight | Hope and trust. |
| 48 | Profile share link | Viral loop. |
| 49 | Local saved filters | Repeat usage. |
| 50 | Profile insights summary | Perceived value. |

### Monetization (51-60)

| # | Feature | ROI impact |
| --- | --- | --- |
| 51 | Pricing page with comparison | Clear upgrade path. |
| 52 | Premium badges | Aspirational value. |
| 53 | Upgrade prompts in filters | Timely upsell. |
| 54 | Limited free quota (soft) | Conversion pressure. |
| 55 | Concierge landing page | Premium lead capture. |
| 56 | Premium success stories | Upgrade proof. |
| 57 | Feature gating labels | Make value visible. |
| 58 | Coupon entry (client) | Later-ready revenue. |
| 59 | Refund policy page | Reduces risk hesitation. |
| 60 | Plan FAQ | Fewer drop-offs. |

### Performance (61-70)

| # | Feature | ROI impact |
| --- | --- | --- |
| 61 | Static rendering for public pages | Fast load. |
| 62 | Lazy load images | Bandwidth savings. |
| 63 | Skeleton loaders | Faster perceived speed. |
| 64 | Image size discipline | Consistent performance. |
| 65 | Cache headers | Free speed boost. |
| 66 | Minimize third-party scripts | Lower blocking. |
| 67 | Font loading strategy | Better paint timing. |
| 68 | Page-level code splitting | Smaller bundles. |
| 69 | Memoized lists | Less re-render cost. |
| 70 | Compression enabled | Lower payload size. |

### Security and privacy (71-80)

| # | Feature | ROI impact |
| --- | --- | --- |
| 71 | Security headers | Blocks common attacks. |
| 72 | Rate limiting | Abuse prevention. |
| 73 | Input validation | Data integrity. |
| 74 | Session expiry | Reduced hijacking. |
| 75 | Password strength warnings | Fewer takeovers. |
| 76 | Minimal data display | Lower exposure. |
| 77 | Hide contact by default | Trust and safety. |
| 78 | Audit logs for key events | Debug and compliance. |
| 79 | File type and size rules | Malware risk reduction. |
| 80 | Security contact page | Responsible disclosure. |

### Operations and scalability (81-90)

| # | Feature | ROI impact |
| --- | --- | --- |
| 81 | Pagination everywhere | Controlled query cost. |
| 82 | Read-only caching | Lower load. |
| 83 | Consistent query contracts | Stability. |
| 84 | Feature flags (client-only) | Safe rollouts. |
| 85 | Graceful error states | Lower churn. |
| 86 | Request IDs in logs | Faster debugging. |
| 87 | Health endpoint | Ops visibility. |
| 88 | Static legal pages | Low ops. |
| 89 | Dependency hygiene | Lower breakage. |
| 90 | Release checklist | Repeatable quality. |

### Ecosystem and growth (91-100)

| # | Feature | ROI impact |
| --- | --- | --- |
| 91 | Sitemap + robots | Better crawl and indexing. |
| 92 | Open Graph tags | Better sharing. |
| 93 | Structured data (schema.org) | Search enhancement. |
| 94 | Media kit page | Press readiness. |
| 95 | Community guidelines | Trust and clarity. |
| 96 | Privacy policy | Compliance and trust. |
| 97 | Terms | Legal clarity. |
| 98 | Support center docs | Lower support cost. |
| 99 | Feedback page | Product learning. |
| 100 | Referral copy kit | Organic growth. |

---

## UX consistency rules

A small set of UI rules keeps the product coherent:

- Typography uses a fixed scale defined in `globals.css`.
- Button hierarchy is consistent (primary, secondary, ghost).
- All CTAs are outcome-based, not generic.
- All pages follow the same hero layout template.
- Spacing uses an 8px-based scale.

---

## Release checklist

Before every release:

- Run local dev status checks.
- Validate env templates.
- Confirm Prisma schema is aligned with API.
- Smoke test the main journey.
- Validate security headers on key routes.

---

## QA and test strategy

Testing is kept minimal but focused:

- Unit tests for core utilities and validation rules.
- Contract tests for key API routes.
- Manual smoke test for onboarding, search, and profile.

No paid testing tools are required.

---

## Risk register

Key risks and mitigation:

- Low trust due to no manual verification.
  Mitigation: checklist, privacy defaults, reporting.

- Low engagement due to sparse profiles.
  Mitigation: completion nudges and prompts.

- Performance regressions on search pages.
  Mitigation: pagination and lazy load.

---

## Incident response

If a major issue is detected:

1. Disable the affected feature with client-side gating.
2. Roll back to last known stable deployment.
3. Document the issue and add a regression check.

---

## Reference repositories and docs

This project is inspired by world-class open-source repos and documentation patterns. The following are useful references for structure and quality:

- Next.js (official repo)
- Prisma (official repo)
- Supabase (official repo)
- Stripe (docs and developer portal)
- Cloudflare (docs and security guides)
- OWASP (security checklists)
- Linux Foundation best practices

---

## FAQ

Q: Is AI used anywhere?
A: No. AI features are explicitly excluded.

Q: Do we use paid services?
A: No. Only Cloudflare free tier is assumed until profits justify upgrades.

Q: Can this scale?
A: Yes. The architecture is stateless and optimized for zero-infra scaling.

---

## License

TBD

---

# Appendix A: Decision gate (50-round filter)

Use this as a strict rejection filter. If any answer is no, the feature is rejected.

1. Costs zero INR to build.
2. Costs zero INR to run.
3. No external SaaS dependency.
4. No paid API dependency.
5. No manual moderation dependency.
6. No manual verification dependency.
7. No AI or ML dependency.
8. Improves conversion or trust.
9. Improves retention or revenue.
10. Does not reduce privacy.
11. Does not add legal risk.
12. No extra ops burden.
13. Works on Cloudflare free tier.
14. Works with current stack.
15. No new infrastructure required.
16. Does not add customer support load.
17. Does not require 24x7 monitoring.
18. No new data collection beyond minimum.
19. No new data retention obligations.
20. No new compliance obligations.
21. Works with self-serve onboarding.
22. Safe for families and guardians.
23. Does not increase spam risk.
24. Does not reduce transparency.
25. Does not add opaque ranking.
26. Improves profile quality.
27. Improves search efficiency.
28. Improves trust signaling.
29. Supports privacy-first defaults.
30. Can be documented with static pages.
31. No reliance on paid email/SMS.
32. No reliance on paid identity checks.
33. No reliance on paid analytics.
34. Does not require data labeling.
35. Does not require human review.
36. Does not require paid storage.
37. Does not require paid CDN features.
38. Works with a single-node deployment.
39. Does not degrade performance budgets.
40. Has a clear rollback path.
41. Adds measurable improvement.
42. Has clear UI copy.
43. Does not increase accessibility debt.
44. Does not introduce dark patterns.
45. Maintains ethical standards.
46. Works on mobile and desktop.
47. Is resilient to abuse.
48. Does not require continuous tuning.
49. Has clear ownership in code.
50. Can be shipped in one sprint.

---

# Appendix B: API map (compressed)

Core route groups (illustrative):

- `/api/auth` for login and session management.
- `/api/users` for profile CRUD.
- `/api/profiles` for public-facing profile access.
- `/api/matches` for discovery and filters.
- `/api/interactions` for interests and shortlists.
- `/api/chat` for real-time messaging.
- `/api/settings` for privacy preferences.
- `/api/analytics` for local event capture.

---

# Appendix C: Data dictionary (compressed)

Profile fields (examples):

- `firstName`, `lastName`, `dateOfBirth`, `gender`.
- `city`, `state`, `country`, `district`.
- `religion`, `caste`, `subCaste`, `motherTongue`.
- `profession`, `educationLevel`, `educationField`.
- `familyType`, `siblingsCount`, `fatherOccupation`, `motherOccupation`.
- `bio`, `hobbies`, `photos`, `videoUrl`.

Interaction fields (examples):

- `senderId`, `receiverId`, `status`, `createdAt`.
- `shortlistedAt`, `mutualAt`, `lastViewedAt`.

Chat fields (examples):

- `messageId`, `threadId`, `senderId`, `body`, `createdAt`.

---

# Appendix D: Security headers

Baseline headers:

- `Content-Security-Policy`
- `Strict-Transport-Security`
- `X-Content-Type-Options`
- `X-Frame-Options`
- `Referrer-Policy`
- `Permissions-Policy`

---

# Appendix E: Performance budgets

Minimum budgets:

- LCP under 2.5s on key pages.
- CLS under 0.1.
- TBT under 200ms.
- First input under 200ms.

---

# Appendix F: SEO and discoverability

Baseline SEO tasks:

- Sitemap and robots file.
- Structured data for profile pages.
- Clean, shareable search URLs.
- Metadata for all primary pages.

---

# Appendix G: Copy bank

Sample copy blocks:

- Hero: "Find the right match with privacy-first controls."
- Trust: "Complete your profile for better visibility."
- Safety: "Contact details are shared only after mutual interest."

---

# Appendix H: Accessibility checklist

Minimum requirements:

- Visible labels on all inputs.
- Focus states on all interactive controls.
- Keyboard navigability for all forms.
- Color contrast meets WCAG AA.

---

# Appendix I: Operational runbook

Minimal ops playbook:

- Use health endpoint for uptime checks.
- Keep backups of Postgres snapshots.
- Rotate secrets on schedule.

---

# Appendix J: Style tokens

Core CSS tokens:

- `--bg`, `--ink`, `--brand`, `--line`.
- `--radius-sm`, `--radius-md`, `--radius-lg`.
- `--space-1` to `--space-8`.

# Appendix K: Route map (frontend)

Main routes and intent (compressed):

| Route | Purpose |
| --- | --- |
| `/` | Landing page and primary conversion surface. |
| `/login` | Authentication entry. |
| `/otp` | OTP confirmation flow. |
| `/step-1` | Onboarding step 1 (basics). |
| `/step-2` | Onboarding step 2 (location, community). |
| `/step-3` | Onboarding step 3 (education, profession). |
| `/step-4` | Onboarding step 4 (family, lifestyle). |
| `/step-5` | Onboarding step 5 (bio, interests). |
| `/matches` | Discovery and search. |
| `/shortlists` | Saved profiles and comparisons. |
| `/interests` | Incoming and outgoing interests. |
| `/chat` | Real-time messaging. |
| `/profile` | Self profile dashboard. |
| `/profile/[id]` | Public profile view. |
| `/who-viewed` | Profile visitors feed. |
| `/notifications` | Activity feed. |
| `/pricing` | Plans and upgrades. |
| `/help` | Help center. |
| `/success-stories` | Social proof and outcomes. |
| `/community-guidelines` | Community rules. |
| `/security` | Security posture page. |
| `/privacy` | Privacy policy page. |
| `/terms` | Terms of service. |
| `/refunds` | Refund policy page. |
| `/verification` | Trust checklist page (self-serve). |

---

# Appendix L: Component inventory

Core components and purpose:

| Component | Purpose |
| --- | --- |
| `PublicTopNav` | Public navigation with CTA. |
| `SiteFooter` | Global footer with links. |
| `PageHero` | Consistent page header layout. |
| `PageEmptyState` | Consistent empty states. |
| `PageLoadingState` | Consistent loading states. |
| `PhotoUpload` | Self-serve photo management. |
| `GuestBannerGate` | Guest mode banner and CTA. |
| `RuntimeClientBootstrap` | Client-only runtime setup. |

---

# Appendix M: Onboarding field checklist

Step 1 (Basics):

- Profile created for
- Consent confirmation (if not self)
- First name
- Last name
- Date of birth
- Gender
- Marital status

Step 2 (Location and community):

- City
- State
- Country
- District
- Religion
- Caste and sub-caste
- Mother tongue

Step 3 (Education and work):

- Profession
- Education level
- Education field
- Company
- Income band

Step 4 (Family and lifestyle):

- Family type
- Siblings count
- Father occupation
- Mother occupation
- Food habit
- Drinking
- Smoking

Step 5 (Profile story):

- Bio
- Interests
- Photos
- Optional video intro URL

---

# Appendix N: Matching rules (deterministic)

Matching is rule-based with transparent signals. Example scoring model:

- Base score starts at 50.
- Add 10 if city matches.
- Add 8 if mother tongue matches.
- Add 7 if education level matches.
- Add 5 if profession alignment.
- Add 5 if lifestyle preferences align.
- Subtract 10 if deal-breaker mismatch.

No AI or probabilistic ranking is used.

---

# Appendix O: Privacy controls matrix

| Data | Default visibility | After mutual interest |
| --- | --- | --- |
| Name | Partial | Full |
| Contact | Hidden | Visible |
| Photos | Visible (primary) | Visible (all) |
| Location | City only | City + district |
| Family details | Hidden | Visible |

---

# Appendix P: Copy and tone guidelines

Copy style:

- Short, decisive sentences.
- Use outcome-based CTAs.
- Avoid jargon and technical language.
- Always clarify privacy controls.

Do not use:

- Aggressive urgency.
- Deceptive scarcity.
- Forced opt-ins.

---

# Appendix Q: Security testing checklist

Minimum security QA:

- Validate authentication on every private route.
- Verify access control for profile visibility.
- Validate rate limiting on login and messaging.
- Confirm CSP header is active.
- Confirm no inline scripts on sensitive pages.

---

# Appendix R: Performance testing checklist

Minimum performance QA:

- Check LCP on landing, matches, profile.
- Validate lazy loading on images.
- Confirm pagination is enforced.
- Confirm bundle size thresholds.

---

# Appendix S: Data deletion and export

Deletion flow:

- User requests account deletion.
- System schedules deletion for grace period.
- Data is removed from live tables.
- Backups expire on retention policy.

Export flow:

- User requests data export.
- System prepares a JSON bundle.
- User downloads securely from profile.

---

# Appendix T: Logging and telemetry

Logging policy:

- Log only minimal, anonymized events.
- Store logs locally or in file system.
- Avoid personal data in logs.
- Retain logs for short, defined duration.

---

# Appendix U: Accessibility QA

Accessibility checks:

- All buttons have clear labels.
- All form fields have visible labels.
- All modals are keyboard accessible.
- All images have alt text.

---

# Appendix V: Deployment checklist

- Environment variables set.
- Database migrations applied.
- Static assets built.
- Health endpoint verified.
- Cloudflare DNS configured.

---

# Appendix W: Feature flags registry

Suggested flags:

- `flags.trust_checklist`
- `flags.saved_searches`
- `flags.profile_comparison`
- `flags.privacy_controls`

---

# Appendix X: Experiment playbook

No paid A/B tools are used. Use manual toggles and compare outcomes in logs.

- Define hypothesis.
- Add client-side toggle.
- Monitor conversion changes.
- Roll out or revert.

---

# Appendix Y: Glossary

- Trust checklist: A self-serve set of profile completion checks.
- Mutual interest: Both parties express interest.
- Shortlist: A saved list of candidates.
- Fire-and-forget: No manual operations required after launch.

# Appendix Z: API reference (detailed)

All endpoints are prefixed with `/api` unless noted. Authentication uses `Authorization: Bearer <JWT>`. Admin endpoints require admin role.

Response conventions:

- Success: 2xx with JSON payload.
- Error: non-2xx with `{ "error": "message" }`.

<details>
<summary>Auth endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | /api/auth/request-otp | No | Request OTP for phone login. |
| POST | /api/auth/verify-otp | No | Verify OTP and issue tokens. |
| POST | /api/auth/login-email | No | Login via email/password. |
| POST | /api/auth/login-firebase | No | Login with Firebase ID token. |

Example request:
```json
{ "phone": "+91xxxxxxxxxx", "type": "login" }
```

Example response:
```json
{ "success": true, "otp_id": "otp_xxx", "expires_in": 300, "message": "OTP sent" }
```

Token response:
```json
{
  "success": true,
  "user": { "id": "uuid", "phone": "+91...", "email": null },
  "access_token": "jwt",
  "refresh_token": "jwt",
  "expires_in": 3600
}
```

</details>

<details>
<summary>User and profile endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/users/profile | Yes | Get current user profile and extensions. |
| PUT | /api/users/profile | Yes | Update profile fields (partial update). |
| PUT | /api/users/profile/preferences | Yes | Save partner preferences. |
| POST | /api/users/profile/preferences | Yes | Legacy fallback for preferences. |
| PUT | /api/users/password | Yes | Update password (demo). |
| GET | /api/users/privacy | Yes | Get privacy settings. |
| POST | /api/users/privacy | Yes | Update privacy settings. |
| GET | /api/users/onboarding/progress | Yes | Get onboarding resume state. |
| POST | /api/users/onboarding/progress | Yes | Save onboarding resume state. |
| POST | /api/users/verification | Yes | Legacy identity submission (not required for zero-ops). |

Profile update fields (partial):

- firstName, lastName, dateOfBirth, gender, maritalStatus
- bio, city, state, country, religion, caste, subCaste, motherTongue
- heightCm, educationLevel, educationField, profession, company, incomeBand
- foodHabit, drinks, smokes, hobbies, religiousness, role, intent
- videoUrl, videoThumbnail, birthTime, birthPlace, gothra, zodiacSign, nakshatra, dosha
- hasChildren, residentialStatus, district, fatherOccupation, motherOccupation, siblingsCount, familyType
- personalityQuiz (object)

Example update request:
```json
{ "firstName": "Aanya", "city": "Mumbai", "profession": "Engineer" }
```

Example profile response (compressed):
```json
{
  "id": "uuid",
  "phone": "+91...",
  "identityStatus": "unverified",
  "firstName": "Aanya",
  "city": "Mumbai",
  "photos": [],
  "partnerPreference": { "minAge": 24, "maxAge": 30 },
  "onboarding": { "step": 3, "draft": {} }
}
```

Profile lookup:
- GET /api/profiles/:id returns public profile with privacy gating on photos and last seen.

</details>

<details>
<summary>Matches endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/matches | Yes | Get match feed with filters. |
| POST | /api/matches/feedback | Yes | Record match feedback (like, skip, hide, report, open). |

Example match response:
```json
[
  {
    "userId": "uuid",
    "firstName": "Priya",
    "age": 26,
    "city": "Mumbai",
    "isVerified": true,
    "isPremium": false,
    "match": 86,
    "photo": "https://...",
    "reasons": ["Matches preferred city", "Recently active"]
  }
]
```

Feedback request:
```json
{ "targetUserId": "uuid", "action": "like", "source": "matches_feed" }
```

</details>

<details>
<summary>Interaction endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/interactions/interests | Yes | List interests: ?type=received|sent|mutual. |
| POST | /api/interactions/like | Yes | Send interest to a user. |
| POST | /api/interactions/reject | Yes | Reject a user (outgoing). |
| POST | /api/interactions/decline | Yes | Decline incoming interest. |
| POST | /api/interactions/report | Yes | Report a user. |
| POST | /api/interactions/safety-action | Yes | Block or mute a user. |
| GET | /api/interactions/safety-status | Yes | Check safety relationship. |
| GET | /api/interactions/profile-viewers | Yes | Who viewed my profile. |
| GET | /api/interactions/horoscope/:targetUserId | Yes | Horoscope compatibility. |

Example like request:
```json
{ "receiverId": "uuid" }
```

Example like response:
```json
{ "status": "liked", "isMatch": false, "matchId": null }
```

</details>

<details>
<summary>Search endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/search | Yes | Search with query filters (mode, sort, page, limit). |
| GET | /api/search/execute | Yes | Alias for /api/search. |
| GET | /api/search/rails | Yes | Curated discovery rails. |
| GET | /api/search/suggestions | Yes | Search suggestions. |
| GET | /api/search/compare | Yes | Compare multiple profiles. |
| GET | /api/search/saved | Yes | List saved searches. |
| POST | /api/search/saved | Yes | Create saved search. |
| PATCH | /api/search/saved/:id | Yes | Update saved search. |
| DELETE | /api/search/saved/:id | Yes | Delete saved search. |

Supported modes: regular, advanced, keyword, profile_id.

</details>

<details>
<summary>Shortlist endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | /api/shortlist/add | Yes | Add to shortlist. |
| POST | /api/shortlist/remove | Yes | Remove from shortlist. |
| GET | /api/shortlist | Yes | List shortlisted profiles. |

Example request:
```json
{ "shortlistedUserId": "uuid" }
```

</details>

<details>
<summary>Chat endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/chat/conversations | Yes | List conversations. |
| GET | /api/chat/:userId | Yes | List messages with a user. |
| POST | /api/chat/send | Yes | Send a message. |
| POST | /api/chat/status | Yes | Update message status. |
| POST | /api/chat/:userId/read | Yes | Mark conversation read. |

Example send request:
```json
{ "receiverId": "uuid", "content": "Hello", "clientMessageId": "local-1" }
```

</details>

<details>
<summary>Notification endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/notifications | Yes | List notifications. |
| POST | /api/notifications/read | Yes | Mark all notifications read. |

</details>

<details>
<summary>Media and photo endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | /api/photos/upload | Yes | Upload a profile photo (multipart). |
| DELETE | /api/photos/:id | Yes | Delete a profile photo. |
| POST | /api/media/presigned-url | Yes | Get S3 presigned URL (optional). |

Note: Cloudinary and S3 integrations are optional and should be disabled in strict zero-INR mode.

</details>

<details>
<summary>Payments and subscriptions</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/subscription/entitlements | Yes | Current plan entitlements. |
| GET | /api/payment/plans | No | List pricing plans. |
| POST | /api/payment/create-order | Yes | Create payment order (optional). |
| POST | /api/payment/verify | Yes | Verify payment (optional). |

Payments are optional and should remain disabled until revenue justifies paid services.

</details>

<details>
<summary>Meta and content endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/meta/filters | No | Filter metadata. |
| GET | /api/meta/locations | No | Location metadata. |
| GET | /api/meta/communities | No | Community metadata. |
| GET | /api/posts/feed | No | Content feed. |

</details>

<details>
<summary>Analytics endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/analytics/taxonomy | Yes | Event taxonomy. |
| GET | /api/analytics/flags | Yes | Feature flags. |
| POST | /api/analytics/flags/exposure | Yes | Log flag exposure. |
| POST | /api/analytics/events | Yes | Track event. |
| POST | /api/analytics/decisions | Admin | Log admin decision. |

</details>

<details>
<summary>Admin endpoints</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | /api/admin/dashboard | Admin | Dashboard stats. |
| GET | /api/admin/users | Admin | List users. |
| POST | /api/admin/users/ban | Admin | Ban a user. |
| GET | /api/admin/moderation/queue | Admin | Moderation queue. |
| PATCH | /api/admin/moderation/:reportId | Admin | Update moderation case. |
| PATCH | /api/admin/moderation/:reportId/assign | Admin | Assign case. |
| POST | /api/admin/moderation/bulk | Admin | Bulk moderation action. |
| POST | /api/admin/moderation/bulk/:operationId/rollback | Admin | Rollback bulk action. |
| GET | /api/admin/moderation/qa-sample | Admin | QA sample. |
| GET | /api/admin/users/:userId/risk | Admin | Risk snapshot. |
| GET | /api/admin/ops/metrics | Admin | Ops metrics. |
| GET | /api/admin/ops/runbooks | Admin | Ops runbooks. |

</details>

<details>
<summary>Verification endpoints (legacy)</summary>

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | /api/verification/submit-id | Yes | Submit identity doc. |
| GET | /api/verification/status | Yes | Get verification status. |
| GET | /api/verification/badges | Yes | Get badge status. |

These endpoints are legacy and should be disabled in strict zero-ops mode.

</details>

<details>
<summary>Health endpoint</summary>

- GET /health (no auth)
- Returns `{ status, db, socket, uptimeSeconds, timestamp }`.

</details>

---

# Appendix AA: Socket events

Socket events used by the real-time layer.

| Event | Direction | Payload |
| --- | --- | --- |
| join_room | client -> server | `{ userId }` |
| presence_update | server -> clients | `{ userId, isOnline }` |
| send_message | client -> server | `{ senderId, receiverId, content, clientMessageId }` |
| receive_message | server -> receiver | `{ id, matchId, senderId, receiverId, content, status }` |
| message_ack | server -> sender | `{ clientMessageId, messageId, status }` |
| message_status | server -> sender | `{ messageId, status, receiverId }` |
| new_match | server -> both | `{ matchId, userId }` |
| new_like | server -> receiver | `{ userId }` |

---

# Appendix AB: Environment variables

Backend (`backend/.env`):

| Variable | Required | Purpose |
| --- | --- | --- |
| DATABASE_URL | Yes | PostgreSQL connection string. |
| PORT | Yes | API port (default 4000). |
| NODE_ENV | Yes | Environment: development or production. |
| JWT_SECRET | Yes | JWT signing secret. |
| FRONTEND_URL | Yes | Base URL for CORS fallback. |
| CORS_ORIGINS | Optional | Comma-delimited allowlist. |
| BODY_LIMIT | Optional | JSON body size limit. |
| CLOUDINARY_CLOUD_NAME | Optional | Photo upload provider (avoid in zero-INR). |
| CLOUDINARY_API_KEY | Optional | Cloudinary key. |
| CLOUDINARY_API_SECRET | Optional | Cloudinary secret. |
| AWS_REGION | Optional | S3 region. |
| AWS_ACCESS_KEY_ID | Optional | S3 access key. |
| AWS_SECRET_ACCESS_KEY | Optional | S3 secret. |
| AWS_BUCKET_NAME | Optional | S3 bucket. |

Frontend (`frontend/.env.local`):

| Variable | Required | Purpose |
| --- | --- | --- |
| NEXT_PUBLIC_API_URL | Yes | API base URL. |
| NEXT_PUBLIC_SOCKET_URL | Yes | Socket base URL. |
| NEXT_PUBLIC_FIREBASE_API_KEY | Optional | Firebase (avoid in zero-INR). |
| NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN | Optional | Firebase auth domain. |
| NEXT_PUBLIC_FIREBASE_PROJECT_ID | Optional | Firebase project id. |
| NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET | Optional | Firebase storage bucket. |
| NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID | Optional | Firebase sender id. |
| NEXT_PUBLIC_FIREBASE_APP_ID | Optional | Firebase app id. |

---

# Appendix AC: Rate limits and throttling

| Scope | Window | Limit (prod) | Limit (dev) |
| --- | --- | --- | --- |
| Global | 15 minutes | 100 | 1000 |
| Search | 60 seconds | 80 | 240 |
| Profile | 60 seconds | 60 | 180 |
| Chat | 60 seconds | 100 | 300 |
| Call | 5 minutes | 10 | 30 |

---

# Appendix AD: Data model (detailed)

<details>
<summary>User</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| phone | String? | Unique. |
| email | String? | Unique. |
| passwordHash | String? | Password hash. |
| isVerified | Boolean | Basic verification flag. |
| verificationCode | String? | OTP/verification code. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |
| lastLogin | DateTime? | Last login timestamp. |
| isActive | Boolean | Active flag. |
| isBanned | Boolean | Banned flag. |
| banReason | String? | Ban reason. |
| banExpiresAt | DateTime? | Ban expiry. |
| role | String | user or admin. |
| identityStatus | String | unverified, pending, verified, rejected. |
| identityDocUrl | String? | ID document URL. |
| socialProvider | String? | google, apple, facebook. |
| socialId | String? | Provider user id. |

Indexes: email, phone, createdAt, isActive, isBanned, socialId, lastLogin.

</details>

<details>
<summary>Profile</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID | Unique FK to User. |
| firstName | String | Required. |
| lastName | String? | Optional. |
| dateOfBirth | Date | Required. |
| gender | String | male, female, other. |
| heightCm | Int? | Optional. |
| maritalStatus | String | never_married, divorced, widowed, annulled. |
| bio | Text? | Optional. |
| religion | String? | Optional. |
| caste | String? | Optional. |
| subCaste | String? | Optional. |
| motherTongue | String? | Optional. |
| country | String | Default India. |
| state | String? | Optional. |
| city | String? | Optional. |
| educationLevel | String? | high_school, bachelors, masters, phd. |
| educationField | String? | Optional. |
| profession | String? | Optional. |
| company | String? | Optional. |
| incomeBand | String? | below_5L, 5-10L, 10-25L, 25-50L, 50L+. |
| foodHabit | String? | vegetarian, non_vegetarian, vegan. |
| drinks | String? | Optional. |
| smokes | String? | Optional. |
| hobbies | String[] | Default empty. |
| religiousness | String? | orthodox, moderate, liberal. |
| completionPercentage | Int | Default 0. |
| role | String | self, parent, relative. |
| intent | String | within_1yr, flexible, exploring. |
| videoUrl | String? | Optional. |
| videoThumbnail | String? | Optional. |
| birthTime | String? | HH:MM. |
| birthPlace | String? | Optional. |
| gothra | String? | Optional. |
| zodiacSign | String? | Optional. |
| nakshatra | String? | Optional. |
| dosha | String? | no, manglik, sarpa_dosha, dont_know. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

Indexes: userId, religion/caste, gender/dateOfBirth, country/state/city, educationLevel/profession, incomeBand.

</details>

<details>
<summary>PartnerPreference</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID | Unique FK to User. |
| minAge | Int | Default 18. |
| maxAge | Int | Default 60. |
| preferredLocations | String[] | Optional. |
| religionOpen | Boolean | Default true. |
| preferredReligions | String[] | Optional. |
| casteOpen | Boolean | Default true. |
| preferredCastes | String[] | Optional. |
| minEducation | String? | Optional. |
| minIncomeBand | String? | Optional. |
| foodHabitPreferences | String[] | Optional. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>Photo</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID | FK to User. |
| photoUrl | String | Required. |
| thumbnailUrl | String? | Optional. |
| isPrimary | Boolean | Default false. |
| uploadedAt | DateTime | Created timestamp. |
| width | Int? | Optional. |
| height | Int? | Optional. |
| fileSizeKb | Int? | Optional. |

</details>

<details>
<summary>Verification</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID | FK to User. |
| type | String | phone, email, id, linkedin. |
| status | String | not_started, pending, verified, rejected. |
| documentUrl | String? | Optional. |
| rejectionReason | String? | Optional. |
| verifiedAt | DateTime? | Optional. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>Like</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| senderId | UUID | FK to User. |
| receiverId | UUID | FK to User. |
| status | String | sent, accepted, rejected, blocked. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>Match</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userAId | UUID | FK to User. |
| userBId | UUID | FK to User. |
| createdAt | DateTime | Created timestamp. |
| lastMessageAt | DateTime? | Optional. |
| isActive | Boolean | Default true. |

</details>

<details>
<summary>Message</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| matchId | UUID | FK to Match. |
| senderId | UUID | FK to User. |
| content | Text | Message body. |
| isRead | Boolean | Default false. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>Subscription</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID | FK to User. |
| plan | String | free, standard, till_marriage. |
| status | String | active, inactive, cancelled. |
| startedAt | DateTime | Started timestamp. |
| expiresAt | DateTime? | Optional. |
| autoRenew | Boolean | Default false. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>Payment</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID | FK to User. |
| subscriptionId | UUID? | Optional FK to Subscription. |
| amountInr | Decimal | Payment amount. |
| plan | String | Plan id. |
| status | String | pending, completed, failed, refunded. |
| razorpayOrderId | String? | Optional. |
| razorpayPaymentId | String? | Optional. |
| razorpaySignature | String? | Optional. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>SavedSearch</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID | FK to User. |
| name | String | Saved search name. |
| filters | Json | Filter object. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>Report</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| reporterId | UUID | FK to User. |
| reportedUserId | UUID | FK to User. |
| reportType | String | inappropriate, fake_profile, harassment, scam, other. |
| description | Text? | Optional. |
| status | String | open, reviewing, resolved, dismissed. |
| adminNotes | Text? | Optional. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>Review</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| reviewerId | UUID | FK to User. |
| reviewedUserId | UUID | FK to User. |
| rating | Int | Numeric rating. |
| comment | Text? | Optional. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>AdminUser</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID | FK to User. |
| email | String | Unique. |
| passwordHash | String | Admin password hash. |
| role | String | admin, moderator, support. |
| createdAt | DateTime | Created timestamp. |
| updatedAt | DateTime | Updated timestamp. |

</details>

<details>
<summary>AuditLog</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID? | Optional FK to User. |
| action | String | Action label. |
| resourceType | String? | Optional. |
| resourceId | String? | Optional. |
| changes | Json? | Optional. |
| ipAddress | String? | Optional. |
| userAgent | String? | Optional. |
| createdAt | DateTime | Created timestamp. |

</details>

<details>
<summary>Shortlist</summary>

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key. |
| userId | UUID | FK to User. |
| shortlistedUserId | UUID | FK to User. |
| createdAt | DateTime | Created timestamp. |

</details>

---

# Appendix AE: Filters and query params

Match filter keys (GET /api/matches and /api/search):

- gender, minAge, maxAge, maritalStatus
- religion, caste, subCaste, motherTongue
- city, state, country, district
- education, profession, income
- minHeight, maxHeight, diet
- gothra, nakshatra, rashi, dosha
- minMatch, verifiedOnly, withPhotoOnly, withHoroscopeOnly
- photoVisibility, profileVisibility, verificationLevel
- onlineNow, premiumOnly, lastActiveDays, shortlistedOnly
- hasChildren, residentialStatus
- keyword, query
- diversityCap, sessionPreferredProfession, sessionPreferredCity, sessionSearchEdits, sessionProfileOpens
- sort, page, limit

Sort options used by the match engine:

- relevance (default)
- compatibility
- activity
- newest

Search modes (GET /api/search):

- regular
- advanced
- keyword (uses query or keyword)
- profile_id (uses profileId or query)

---

# Appendix AF: Privacy settings schema

Default privacy settings:

| Key | Default | Notes |
| --- | --- | --- |
| showPhone | false | Hide contact by default. |
| showPhoto | true | Photos visible based on photoVisibility. |
| showProfile | true | Profile visible by default. |
| allowSearch | true | Allow search inclusion. |
| showLastSeen | false | Hide last seen by default. |
| allowMessages | true | Allow messages by default. |
| photoVisibility | public | public, protected, request_access. |
| profileVisibility | public | public, premium_only, verified_only, hidden. |

---

# Appendix AG: Error codes

Common errors returned by the API:

| Status | Meaning | Example |
| --- | --- | --- |
| 400 | Bad request | Invalid input or missing fields. |
| 401 | Unauthorized | Access denied or missing token. |
| 403 | Forbidden | Access blocked by privacy or role. |
| 404 | Not found | Missing resource. |
| 429 | Too many requests | Rate limit exceeded. |
| 500 | Server error | Unexpected failure. |

---

# Appendix AH: Pagination conventions

Pagination uses `page` and `limit` query parameters.

- `page` defaults to 1.
- `limit` defaults to 20.
- `limit` is capped at 50 on most endpoints.

Responses either return a list or include pagination metadata:

```json
{
  "items": [],
  "page": 1,
  "limit": 20,
  "total": 0,
  "hasNextPage": false
}
```

---

# Appendix AI: Scripts and commands

Root scripts (`package.json`):

- `npm run dev:up` start full stack (docker).
- `npm run dev:up:nodocker` start without docker.
- `npm run dev:down` stop services.
- `npm run dev:status` check running services.
- `npm run infra:up` start postgres + redis.
- `npm run infra:down` stop postgres + redis.

Backend scripts (`backend/package.json`):

- `npm run dev` start API with nodemon.
- `npm run start` start API.
- `npm run db:generate` generate Prisma client.
- `npm run db:push` push schema to DB.
- `npm run db:migrate` run Prisma migrations.
- `npm run seed` seed database.
- `npm run test:smoke` run smoke test.

Frontend scripts (`frontend/package.json`):

- `npm run dev` start Next.js on port 8000.
- `npm run build` build production assets.
- `npm run start` run production server.
- `npm run lint` run ESLint.

---

# Appendix AJ: Build artifacts and caching

Key generated folders:

- `frontend/.next` Next.js build output.
- `backend/node_modules` backend dependencies.
- `frontend/node_modules` frontend dependencies.
- `runtime/` local runtime files and logs.

Caching guidelines:

- Cache static assets at the edge with long TTL.
- Avoid caching authenticated API responses.
- Cache public metadata endpoints when safe.

---

# Appendix AK: Database migrations

Prisma workflow:

- `npm run db:generate` to refresh the Prisma client.
- `npm run db:push` to sync schema without migrations.
- `npm run db:migrate` to create and apply migrations.
- `npm run seed` to load sample data.

---

# Appendix AL: Security and compliance mapping

Security controls aligned with common web risks:

- Access control: JWT auth + privacy gating on profiles.
- Injection defense: Prisma queries + input validation.
- XSS/CSRF: security headers + CSRF checks on state-changing requests.
- Data exposure: privacy defaults and contact gating.
- Abuse prevention: rate limits, reporting, and safety actions.

---

# Appendix AM: Contribution guide

Contribution rules:

- Keep features zero-INR and no-AI.
- Avoid new paid services.
- Keep privacy defaults conservative.
- Prefer small, reversible changes.

Suggested workflow:

1. Create a short design note.
2. Implement minimal viable scope.
3. Add tests or update smoke checks.
4. Update docs if behavior changed.

---

# Appendix AN: Roadmap (zero-INR)

Phase 1: Trust and onboarding

- Trust checklist refinements.
- Profile completion nudges.
- Privacy-first visibility defaults.

Phase 2: Discovery and retention

- Saved searches.
- Profile comparisons.
- Session resume banners.

Phase 3: Monetization (optional)

- Pricing clarity.
- Plan gating UX.
- Payment integration only after revenue.

---
# Interactive reference

<details>
<summary>Open zero-INR constraint card</summary>

- No paid APIs.
- No paid SaaS.
- No paid identity checks.
- No AI features.
- Cloudflare free tier only.

</details>

<details>
<summary>Open trust checklist card</summary>

- Photo added.
- Bio completed.
- Basics complete.
- Interests added.
- Family details added.
- Quiz completed.
- Self-attestation confirmed.

</details>

<details>
<summary>Open security baseline card</summary>

- Access control on every route.
- Input validation on every write.
- Rate limits on auth and chat.
- Security headers enabled.
- Session rotation on login.

</details>

<details>
<summary>Open performance baseline card</summary>

- Lazy load images.
- Pagination on all lists.
- Static render public pages.
- No third-party scripts.
- Enforce bundle budgets.

</details>


