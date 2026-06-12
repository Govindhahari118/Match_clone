# IMPL_01 — Profile Wizard & Onboarding Flow
## Pin-to-Pin Implementation Plan

> **Gap:** Plans require a 9-step guided Profile Wizard (PLAN_03 Section 5.1). Currently users fill profiles in a flat ProfileEditScreen with no guidance.  
> **Impact:** Without guided wizard, profile completeness stays <30%, which kills matchmaking quality.  
> **Source Docs:** PLAN_01 F-02, PLAN_02 PC-04, PLAN_03 Section 5.1

---

## DELIVERABLES

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `ui/wizard/ProfileWizardScreen.kt` | Main NavHost for 9-step wizard flow |
| 2 | `ui/wizard/WizardStepBasicInfo.kt` | Step 1: Profile For, Name, DOB, Gender, Height |
| 3 | `ui/wizard/WizardStepLocation.kt` | Step 2: City, State, Country, Relocate toggle |
| 4 | `ui/wizard/WizardStepReligion.kt` | Step 3: Religion, Caste, Sub-caste, Gothra, Nakshatra, Rasi |
| 5 | `ui/wizard/WizardStepEducation.kt` | Step 4: Education Level, Field, Institution, Occupation, Employer, Income |
| 6 | `ui/wizard/WizardStepFamily.kt` | Step 5: Family Type, Father/Mother Occupation, Siblings, Family Values |
| 7 | `ui/wizard/WizardStepLifestyle.kt` | Step 6: Diet, Smoking, Drinking, Fitness, Hobbies |
| 8 | `ui/wizard/WizardStepPartnerPrefs.kt` | Step 7: All partner preference ranges with "Open to any" toggles |
| 9 | `ui/wizard/WizardStepAstrology.kt` | Step 8: Nakshatra, Rasi, Manglik, Birth Time, Birth Place (optional, skippable) |
| 10 | `ui/wizard/WizardStepPhotos.kt` | Step 9: Primary photo (mandatory) + up to 5 gallery photos |
| 11 | `ui/wizard/WizardProgressBar.kt` | Top progress indicator (step X/9 + completion %) |
| 12 | `ui/wizard/ProfileWizardViewModel.kt` | State management: draft profile, validation, step navigation |
| 13 | `ui/wizard/WizardNavGraph.kt` | Nested navigation for wizard screens |

### Files to Modify

| File | Change |
|------|--------|
| `navigation/AppNavGraph.kt` | Add `profileWizard` route, trigger after first sign-up if completeness < 50% |
| `HomeScreen.kt` | Show banner "Complete your profile" if `profileCompleteness < 0.7` |
| `AuthRepository.kt` or `SignUpScreen.kt` | After signup success → navigate to wizard |
| `ProfileCompletenessUtil.kt` | Already exists — wire into wizard progress bar |
| `UserEntity.kt` | Add `profileFor` field ("self", "son", "daughter", "sibling") via Migration 17→18 |
| `FirestoreProfileService.kt` | Map `profileFor` field |

### Data Requirements (from assets)

| Data | Source | Use |
|------|--------|-----|
| Castes + sub-castes | `castes_telugu.json` | Step 3 dropdown (already exists) |
| Cities | `cities_telugu.json` | Step 2 suggestions (already exists) |
| Nakshatras/Rashis | `astrology_telugu.json` | Step 8 dropdowns (already exists) |
| Occupations | **NEW:** `assets/reference/occupations_telugu.json` | Step 4 dropdown — must create |
| Education fields | **NEW:** `assets/reference/education_fields.json` | Step 4 dropdown — must create |

### UX Rules (from source docs)

1. Steps 1-6 are mandatory. Steps 7-9 can be "Add Later" skipped.
2. Minimum completeness to appear in search: **50%** (step 6 done)
3. Minimum completeness for daily recommendations: **75%** (step 8 done)
4. Photo upload (step 9) is strongly encouraged but not blocking.
5. After all 9 steps → show confetti + "Profile Published!" + redirect to Discovery
6. Progress bar shows weighted % (using `ProfileCompletenessUtil.calculate()`)
7. User can return to any step from Profile → Edit
8. Wizard state survives app kill (persist draft to Room or DataStore)

### Validation Rules Per Step

| Step | Required Fields | Validation |
|------|----------------|------------|
| 1 | displayName, DOB, gender, heightCm | Age must be 18-99 (DOB checked), height 100-250cm |
| 2 | city, state, country | Non-empty |
| 3 | religion | Caste/sub-caste optional but encouraged |
| 4 | education level, profession | Field/employer optional |
| 5 | familyType | Parent occupations optional |
| 6 | diet | Smoking/drinking optional |
| 7 | At least 3 preference fields set | OR "Open to any" checked |
| 8 | Optional (skip allowed) | If filled: nakshatra must be from valid list |
| 9 | Optional | If photo: must pass face detection (ML Kit) |

### Navigation Flow

```
SignUp Complete → ProfileWizardScreen (if completeness < 50%)
    Step 1 → Step 2 → ... → Step 9 → Discovery
    Back: Goes to previous step (never exits wizard until Step 6 done)
    Skip: Available on Steps 7, 8, 9

Profile Edit → Any Step (deep link via step parameter)
Home Banner "Complete Profile" → Wizard at first incomplete step
```

### Sprint Estimate: 1 full sprint (creates 13 new files, modifies 4)

---

## DEFINITION OF DONE

- [ ] All 9 wizard steps render and accept input
- [ ] Progress bar shows real-time % from ProfileCompletenessUtil
- [ ] Data persists to Room + Firestore on each step completion
- [ ] Castes/cities/nakshatras load from asset JSONs
- [ ] Wizard auto-triggers after new signup
- [ ] "Complete Profile" banner shows on Home if < 70%
- [ ] Step navigation (next/back/skip) works correctly
- [ ] BUILD SUCCESSFUL
