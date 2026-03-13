# Matrimony UI/UX Benchmark Report (2026-03-10)

## Method and limitations
- Sources are the public homepages for Shaadi.com, BharatMatrimony.com, TamilMatrimony.com, and EliteMatrimony.com.
- Jeevansathi.com homepage could not be retrieved in this environment (internal error), so its UX and visual system are not captured here.
- The crawler returns text-only content and strips CSS and computed styles. Exact fonts, colors, spacing, and imagery styles are therefore not verifiable here. This report focuses on structure, content hierarchy, flow, and interaction patterns that are visible in text.
- A fill-in checklist for exact visual tokens is included below; populate it using local browser devtools.

## Executive synthesis: what the best implementations converge on
- A single hero CTA backed by immediate trust signals and numeric proof.
- A short registration form in the hero that captures intent and phone/OTP verification.
- Clear, early safety and verification language.
- Assisted or concierge service upsells for high-intent or premium users.
- Success stories and FAQs included on the homepage to reduce doubt.
- Family-oriented options (profile created for) and offline support options.

## Site-by-site analysis

### 1) Shaadi.com
Observed structure and UX
- Hero headline with a primary CTA: "Find your forever" and "Find Your Match".
- Large social proof block near the hero: #1 matchmaking service, app ratings, success stories count.
- Feature triptych emphasizing money-back guarantee, verified profiles (blue-tick), and AI matchmaking.
- Premium upsell section (VIP Shaadi) with a "Free Consultation" CTA.
- Founder quote for trust and brand credibility.
- Success stories section and a visible FAQ block on the homepage.
- Extremely deep footer segmentation (community, country, religion, city, language, etc.) to reduce navigation friction.

Why it works
- Hero and proof points reduce uncertainty fast.
- Guarantees and verification remove risk for high-consideration users.
- Premium pathway is visible but not blocking the main funnel.

Implementation takeaways for Match
- Put a single strong CTA in the hero and pair it with 2 to 3 proof metrics.
- Add a verification badge system and a money-back or satisfaction guarantee if supported by business policy.
- Provide a premium path (concierge or assisted service) with a consultation CTA.
- Include FAQs directly on the homepage.

### 2) BharatMatrimony.com
Observed structure and UX
- Hero headline: "The biggest and most trusted matrimony service for Indians!".
- Prominent hero form titled "Create a Matrimony Profile".
- Form captures profile created for, gender, name, phone with country code, and OTP verification note.
- Trust badges: 100% mobile-verified profiles, 4 Crore+ customers, 26 years of matchmaking.
- Assisted service section with guaranteed matches, better response, time savings, and relationship manager support.
- EliteMatrimony upsell with confidentiality and relationship manager messaging.
- Retail outlet support section that lists in-person help for profile setup, usage, and payments.
- App download block with ratings and downloads.
- Success stories section with multiple testimonials.
- Persistent 24x7 help and contact options.

Why it works
- The form removes guesswork and gets users to submit quickly.
- OTP and verified messaging shifts trust earlier.
- Offline support supports family-led and older users.

Implementation takeaways for Match
- Make the hero form the primary conversion element.
- Add a family toggle (profile created for) and phone verification.
- Provide assisted service and offline support for high-consideration users.
- Keep an app-download CTA for mobile-first conversion.

### 3) TamilMatrimony.com
Observed structure and UX
- Same core structure as BharatMatrimony but localized for the Tamil community.
- Hero form with profile created for, gender, name, phone + OTP.
- Trust badges: 100% mobile-verified profiles, 4 Crore+ customers served, 26 years.
- Assisted service and EliteMatrimony blocks with confidentiality and relationship manager messaging.
- Retail outlets and app download section.
- Success stories and testimonials.
- 24x7 help options at the top.

Why it works
- Community localization increases relevance without changing the core flow.
- Consistency across regional variants reduces friction.

Implementation takeaways for Match
- Keep core UX identical across regional variants, but localize headline and trust proof.

### 4) EliteMatrimony.com
Observed structure and UX
- Premium positioning headline: "The largest and most successful matchmaking service for Elite".
- Concierge-first CTA: phone number and "Call Us" are highly visible.
- Intake form fields designed for concierge onboarding (name, alliance for, mother tongue).
- Strong emphasis on personalized and confidential service.
- Extensive premium packages with pricing and durations (3, 6 months, Till You Marry).
- Detailed package benefit lists with relationship managers and privacy assurances.
- Long success stories with narrative detail.

Why it works
- Concierge-first conversion suits the premium segment.
- Transparent pricing and benefits reduce high-ticket friction.

Implementation takeaways for Match
- Create a premium tier landing that shifts from self-serve to concierge contact.
- Show package benefits and explicit pricing.
- Emphasize confidentiality and relationship-manager support.

### 5) Jeevansathi.com
Status
- Homepage blocked in this environment; no reliable UX capture here.
- Requires manual browser inspection to document structure and visual system.

## Best-practice blueprint for Match (synthesis)

### Hero and onboarding
- Primary CTA + short form in hero.
- Required fields: profile created for, gender, name, phone, OTP.
- Add a second CTA for browsing without registration.

### Trust and safety
- Verification badges (mobile verified, ID verified).
- Money-back or satisfaction pledge (if policy allows).
- Highlight privacy and confidentiality.

### Assisted / Premium service
- Separate section for assisted matchmaking.
- Concierge pathway with phone or consult CTA.
- Clear benefits list and pricing.

### Social proof
- Show success stories early.
- Use a rotating carousel + a "View all" link.

### Support and escalation
- 24x7 support channel list.
- Optional retail or in-person support listing.

### Segmentation
- Community, city, and language shortcuts in the footer.
- Regional landing variants with localized headlines.

## Visual system capture checklist (populate via browser devtools)

Fill this for each site (Shaadi, BharatMatrimony, TamilMatrimony, EliteMatrimony, Jeevansathi):
- Primary font family (body):
- Display font family (headings):
- Base font size:
- Primary CTA color:
- Secondary CTA color:
- Background color:
- Card background color:
- Border color:
- Link color:
- Hero background image or gradient:
- Button radius:
- Card radius:
- Shadow style:
- Icon style (outline vs filled):
- Form field style (bordered vs filled):

## Implementation notes for Match
- Start by rebuilding the hero form and trust band.
- Add assisted and premium sections as distinct modules, not mixed with the primary CTA.
- Build success story and FAQ modules that can be reused in different regional variants.

---

## Page-by-page UX audit + phased implementation (2026-03-10)

### Phase definitions used in this pass
- P1 Structure & navigation baseline (hero hierarchy, CTA placement, cross-links).
- P2 Trust & verification clarity (effective dates, safety positioning, disclosure routes).
- P3 Discovery & matching depth (filters, sorting, saved views, empty states).
- P4 Conversion & monetization (pricing clarity, assisted upsell surfaces).
- P5 Support & compliance (help, legal, refunds, safety).
- P6 Ops & reliability (admin, errors, loading, stability).

### Page inventory audit (all routes in `frontend/src/app`)
| Route | Primary intent | Current UX pattern | Gap or opportunity | Phase action | Status |
| --- | --- | --- | --- | --- | --- |
| `/` | Convert new users or serve dashboard | Landing hero + trust + dashboard | Already aligned to benchmark hero, trust, assisted | P1, P4 maintained | Done |
| `/login` | Secure sign-in | Auth card + OTP + Google | Clear; keep trust copy | P1, P2 maintained | Done |
| `/otp` | Phone verification | OTP entry card | Clear; keep errors + resend | P1, P2 maintained | Done |
| `/step-1` | Onboarding basics | Form wizard + progress | Solid; ensure clarity | P1, P2 maintained | Done |
| `/step-2` | Location/community | Form wizard + progress | Solid | P1 maintained | Done |
| `/step-3` | Education/career | Form wizard + progress | Solid | P1 maintained | Done |
| `/step-4` | Lifestyle/bio | Form wizard + progress | Solid | P1 maintained | Done |
| `/step-5` | Partner preferences | Form wizard + progress | Solid | P1, P3 maintained | Done |
| `/matches` | Discovery + filters | Listing hero + advanced filters | Strong parity; keep server-side filtering | P3 maintained | Done |
| `/search` | Query routing | Redirect to matches | No UX surface needed | P3 maintained | Done |
| `/profile` | Manage own profile | Profile hero + edit panels | Strong; keep completeness cues | P1 maintained | Done |
| `/profile/[id]` | View others | Profile detail layout | Strong; keep safety controls | P2, P3 maintained | Done |
| `/chat` | Conversations | Split pane chat | Solid; empty states present | P3 maintained | Done |
| `/interests` | Manage requests | Listing hero + cards | Solid | P3 maintained | Done |
| `/shortlists` | Saved profiles | Listing hero + cards | Solid | P3 maintained | Done |
| `/who-viewed` | Visitor visibility | Listing hero + upsell | Solid; upsell present | P4 maintained | Done |
| `/notifications` | Updates feed | Listing hero + list | Solid | P3 maintained | Done |
| `/settings` | Privacy + account | Settings hero + sections | Solid; privacy blocks present | P2, P5 maintained | Done |
| `/help` | Support & FAQs | Support hero + FAQ grid | Solid; search + categories | P5 maintained | Done |
| `/pricing` | Monetization | Pricing hero + comparisons | Strong; assisted upsell present | P4 maintained | Done |
| `/success-stories` | Social proof | Success hero + stories | Strong | P4 maintained | Done |
| `/biodata` | Printable profile | Print-ready layout | Solid | P1 maintained | Done |
| `/kundli` | Horoscope matching | Page hero + form | Solid | P3 maintained | Done |
| `/quiz` | Compatibility input | Quiz cards + progress | Solid | P3 maintained | Done |
| `/admin` | Ops & moderation | Admin dashboard | Solid | P6 maintained | Done |
| `/verification` | ID verification | Trust hero + form | Solid | P2 maintained | Done |
| `/privacy` | Privacy policy | Legal sections | Add hero, effective date, cross-links | P1, P2, P5 executed | Done |
| `/terms` | Terms | Legal sections | Add hero, effective date, cross-links | P1, P2, P5 executed | Done |
| `/refunds` | Refund policy | Legal sections | Add hero, effective date, cross-links | P1, P4, P5 executed | Done |
| `/security` | Security policy | Legal sections | Add hero, effective date, cross-links | P1, P2, P5 executed | Done |
| `/community-guidelines` | Safety norms | Legal sections | Add hero, effective date, cross-links | P1, P2, P5 executed | Done |
| `/not-found` | 404 handling | State page | Solid | P6 maintained | Done |
| `/error` | Error boundary | State page | Solid | P6 maintained | Done |
| `/global-error` | App crash handling | State page | Solid | P6 maintained | Done |
| `/loading` | Global loading | State page | Solid | P6 maintained | Done |
| `/robots` | SEO control | Static file | Solid | P6 maintained | Done |
| `/sitemap` | SEO discovery | Static file | Solid | P6 maintained | Done |

### Phase execution summary for this pass
- P1: Added a shared `PageHero` pattern and aligned all legal/policy pages to the same hero hierarchy and action links.
- P2: Added explicit effective dates and safety-linked cross-navigation on policies.
- P3: Verified discovery flows already include empty states, advanced filters, and server-side filtering.
- P4: Ensured refund policy now links to pricing and support entry points.
- P5: Legal + support surfaces now cross-link for faster help routing.
- P6: No additional changes required; existing error/loading/admin surfaces are consistent.

## 2026-03-11 Implementation Update
- Rolled `PageHero` into listing pages (matches, interests, shortlists, notifications, who-viewed), pricing, and settings for consistent header hierarchy.
- Added a global `SiteFooter` across public and app layouts with standard Product/Company/Legal/Support/Social sections.
- Updated the public navbar to show the main links directly and group remaining pages under a `More` menu.
