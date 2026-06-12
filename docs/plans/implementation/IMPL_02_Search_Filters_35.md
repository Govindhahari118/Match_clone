# IMPL_02 — Search & Filter System (35 Filters)
## Pin-to-Pin Implementation Plan

> **Gap:** Plans require 35+ advanced filters across 8 sections (PLAN_03 Section 4.2). Currently SearchFilters.kt has ~12 basic filters. DB columns exist but UI doesn't expose them.  
> **Impact:** Users cannot find specific matches. Premium upsell (filter gating) not possible.  
> **Source Docs:** PLAN_01 SF-01→SF-15, PLAN_03 Section 4 (35 filters), PLAN_05 Section 1.2

---

## DELIVERABLES

### Files to Modify

| # | File | Changes |
|---|------|---------|
| 1 | `ui/search/SearchFilters.kt` | Add all 35 filter UI components |
| 2 | `ui/search/SearchFiltersViewModel.kt` | Add filter state for all new fields |
| 3 | `data/remote/FirestorePagingSource.kt` | Apply new filters to Firestore queries |
| 4 | `firestore.indexes.json` | Add composite indexes for new filter combos |

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `ui/search/FilterSectionPersonal.kt` | Age, Height, Marital status, Children, Physical status, Relocate |
| 2 | `ui/search/FilterSectionReligion.kt` | Religion, Mother tongue, Caste, Sub-caste, Gothra, Nakshatra, Rasi, Manglik |
| 3 | `ui/search/FilterSectionLocation.kt` | Country, State, City, Citizenship, NRI status |
| 4 | `ui/search/FilterSectionEducation.kt` | Education level, Field, Specific degree, Occupation category, Employer type, Income range |
| 5 | `ui/search/FilterSectionLifestyle.kt` | Diet, Smoking, Drinking, Family type, Family status, Hobbies |
| 6 | `ui/search/FilterSectionAstrology.kt` | Min Porutham score, Horoscope available, Rasi, Nakshatra |
| 7 | `ui/search/FilterSectionActivity.kt` | Profile posted in last X days, Last active, With photo only, Verified only, Premium only |
| 8 | `ui/search/SavedSearchRepository.kt` | Save/load named filter sets to Firestore `savedSearches/{uid}` |
| 9 | `ui/search/SavedSearchesSheet.kt` | Bottom sheet to manage up to 10 saved searches |
| 10 | `domain/model/SearchFilterState.kt` | Data class holding all 35 filter values |
| 11 | `domain/model/SubscriptionGate.kt` | Maps filter to required plan (FREE: 8 basic, STANDARD+: advanced) |

### All 35 Filters (from PLAN_03)

#### Section A — Personal Basics (FREE)
| # | Filter | UI Component | Field |
|---|--------|-------------|-------|
| 1 | Age range | RangeSlider (18-70) | `age` |
| 2 | Height range | RangeSlider (120-210 cm) + cm/ft toggle | `heightCm` |
| 3 | Marital status | MultiSelect chips | `maritalStatus` |
| 4 | Have children | Chips: No/Yes-together/Yes-apart/Don't mind | `hasChildren` |
| 5 | Physical status | Chips: Normal/Differently Abled (compassionate labels) | `physicalStatus` |
| 6 | Willing to relocate | MultiSelect: Anywhere/India/USA/UAE/No | `willingToRelocate` |

#### Section B — Religion & Community (FREE for religion, STANDARD+ for sub-caste/gothra)
| # | Filter | UI Component | Field |
|---|--------|-------------|-------|
| 7 | Religion | Multi-select chips | `religion` |
| 8 | Mother Tongue | Multi-select (Telugu pre-selected for AP/TS users) | `motherTongue` |
| 9 | Caste | Searchable multi-select (Telugu castes from JSON first) | `caste` |
| 10 | Sub-caste | Dynamic dropdown (driven by caste selection) | `subCaste` |
| 11 | Gothra | Searchable text + common Telugu gothras from JSON | `gothra` |
| 12 | Nakshatra | 27-item dropdown | `nakshatra` |
| 13 | Rasi | 12-item dropdown | `rasi` |
| 14 | Manglik | Chips: Manglik only / Non-Manglik / Don't mind | `manglik` |

#### Section C — Location & Citizenship (STANDARD+)
| # | Filter | UI Component | Field |
|---|--------|-------------|-------|
| 15 | Country | Multi-select (India, USA, UAE, AUS, UK, Singapore, Canada first) | `countryOfResidence` |
| 16 | State | Multi-select (AP + Telangana listed first) | `state` |
| 17 | City/District | Searchable multi-select (Telugu cities first from JSON) | `city` |
| 18 | Citizenship | Chips: Indian/NRI/British Indian/AUS PR/Any | `citizenship` |
| 19 | NRI Status | Toggle: NRI only / Include / Exclude | `isNRI` |

#### Section D — Education & Career (STANDARD+)
| # | Filter | UI Component | Field |
|---|--------|-------------|-------|
| 20 | Education level | Multi-select chips | `education` |
| 21 | Education field | Multi-select from JSON | `educationField` |
| 22 | Occupation category | Multi-select: IT/Govt/Healthcare/Business/Finance/Defence | `occupationCategory` |
| 23 | Employer type | Chips: Private MNC/Government/PSU/Self-employed | `employerType` |
| 24 | Income range | RangeSlider (₹3L brackets) + NRI USD equivalent | `incomeBand` |

#### Section E — Lifestyle & Family (STANDARD+)
| # | Filter | UI Component | Field |
|---|--------|-------------|-------|
| 25 | Diet | Multi-select: Veg/Non-veg/Eggetarian/Vegan/Jain | `diet` |
| 26 | Smoking | Chips: Never/Occasionally/Regularly/Don't mind | `smoking` |
| 27 | Drinking | Chips: Never/Occasionally/Regularly/Don't mind | `drinking` |
| 28 | Family type | Chips: Joint/Nuclear/Either | `familyType` |
| 29 | Family status | Chips: Middle/Upper-middle/Affluent/Rich | `familyStatus` |
| 30 | Hobbies & Interests | Multi-select tags (Telugu-first: Cricket, Carnatic music, Temple) | `hobbies` |

#### Section F — Activity & Quality (PREMIUM+)
| # | Filter | UI Component | Field |
|---|--------|-------------|-------|
| 31 | Profile posted in last X days | Chips: 7/15/30/90 | `createdAt` |
| 32 | Last active | Chips: Today/7 days/30 days | `lastActiveAt` |
| 33 | With photo only | Toggle (default ON) | `photoUrl != null` |
| 34 | Verification status | Chips: Any/Blue/Gold | `verificationLevel` |
| 35 | Premium members only | Toggle | `subscriptionPlan` |

### Subscription Gating Logic

```kotlin
enum class FilterTier { FREE, STANDARD, PREMIUM }

val filterTierMap = mapOf(
    // FREE (always available)
    "age" to FREE, "height" to FREE, "maritalStatus" to FREE,
    "religion" to FREE, "caste" to FREE, "education" to FREE,
    "city" to FREE, "state" to FREE, "manglik" to FREE,
    
    // STANDARD (₹399/month)
    "motherTongue" to STANDARD, "subCaste" to STANDARD, "gothra" to STANDARD,
    "country" to STANDARD, "citizenship" to STANDARD, "isNRI" to STANDARD,
    "educationField" to STANDARD, "occupationCategory" to STANDARD, 
    "employerType" to STANDARD, "income" to STANDARD,
    "smoking" to STANDARD, "drinking" to STANDARD, "familyType" to STANDARD,
    "familyStatus" to STANDARD, "hobbies" to STANDARD,
    "hasChildren" to STANDARD, "physicalStatus" to STANDARD, "relocate" to STANDARD,
    "nakshatra" to STANDARD, "rasi" to STANDARD,
    
    // PREMIUM (₹799/month)
    "createdAt" to PREMIUM, "lastActive" to PREMIUM, "photoOnly" to PREMIUM,
    "verificationLevel" to PREMIUM, "premiumOnly" to PREMIUM,
)
```

### Firestore Query Strategy

**Problem:** Firestore allows max 1 inequality filter + 1 orderBy per query.  
**Solution:** 
1. Apply hard equality filters server-side (gender, religion, caste, country, NRI)
2. Pull wider result set (limit 100)
3. Apply remaining filters client-side in `FirestorePagingSource`
4. Add composite indexes for most common combos:
   - `gender + religion + ageBucket + lastActiveAt`
   - `gender + caste + ageBucket + lastActiveAt`
   - `gender + countryOfResidence + ageBucket + lastActiveAt`
   - `gender + isNRI + religion + lastActiveAt`
   - `gender + verificationLevel + religion + lastActiveAt`

### Saved Searches (Premium Feature)

- Up to 10 saved searches per user
- Stored in Firestore: `savedSearches/{uid}/items/{searchId}`
- Schema: `{ name, filters: {...}, createdAt, lastUsedAt }`
- Cloud Function trigger: when new profile matches a saved search → FCM push

---

## DEFINITION OF DONE

- [ ] All 35 filters render in expandable section UI
- [ ] Locked filters show lock icon + "Upgrade" tooltip
- [ ] Filters marked STANDARD+ show "Upgrade to Standard" sheet on tap (if FREE user)
- [ ] At least 8 filters work with Firestore queries (rest client-side)
- [ ] Saved searches save/load from Firestore
- [ ] Filter state persists across screen rotations
- [ ] Results update in real-time when filters change
- [ ] BUILD SUCCESSFUL
