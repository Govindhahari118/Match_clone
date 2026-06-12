# IMPL_05 — Subscription & Monetization (End-to-End Payment Flow)
## Pin-to-Pin Implementation Plan

> **Gap:** SubscriptionPlans.kt defines 4 tiers + feature gating, but NO UI displays plans to users, NO purchase flow is wired, NO feature gating is enforced in screens. Razorpay payment verification Cloud Function exists but plan types don't match new 4-tier system.  
> **Impact:** Zero revenue. The entire business model is unimplemented in the UI layer.  
> **Source Docs:** PLAN_01 MON-01→06, PLAN_03 Section 9, PLAN_05 Section 6

---

## DELIVERABLES

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `ui/subscription/SubscriptionPlansScreen.kt` | Full-screen plan comparison with pricing + feature matrix |
| 2 | `ui/subscription/PlanCard.kt` | Individual plan card (price, features, CTA button) |
| 3 | `ui/subscription/PaywallSheet.kt` | Bottom sheet triggered when user hits feature gate |
| 4 | `ui/subscription/SubscriptionViewModel.kt` | Manages plan selection, Razorpay checkout, verification |
| 5 | `ui/subscription/SubscriptionSuccessScreen.kt` | Post-purchase confirmation with plan details |
| 6 | `domain/usecase/CheckFeatureAccessUseCase.kt` | Central gate: checks user plan vs feature requirement |
| 7 | `ui/boost/BoostPurchaseSheet.kt` | Spotlight boost purchase (₹149 one-time) |
| 8 | `ui/boost/BoostActiveOverlay.kt` | "Boost Active!" indicator during boost period |
| 9 | `data/repository/BoostRepository.kt` | Manages boost activation/expiry via Firestore |

### Files to Modify

| File | Change |
|------|--------|
| `SubscriptionPlans.kt` | Already exists — use as-is |
| `SubscriptionRepository.kt` | Update to use new 4-tier model (FREE/STANDARD/PREMIUM/PLATINUM) |
| `functions/src/index.ts` (`verifyRazorpayPayment`) | Update plan types: standard_1m/premium_1m/platinum_1m + annual variants |
| `HomeScreen.kt` | Add "Upgrade" banner for FREE users after 3 days |
| `DiscoveryScreen.kt` / `SwipeDiscoveryScreen.kt` | Gate daily interest limit via `CheckFeatureAccessUseCase` |
| `ChatScreen.kt` | Gate read receipts behind STANDARD+ |
| `ProfileDetailScreen.kt` | Gate "See who viewed" behind STANDARD+ |
| `SearchFilters.kt` | Gate advanced filters behind plan (already in IMPL_02) |
| `SettingsScreen.kt` | Show current plan + "Manage Subscription" button |
| `AppModule.kt` | Provide `CheckFeatureAccessUseCase` |

### Subscription Plans Screen Layout

```
┌──────────────────────────────────────────┐
│ [X]            Choose Your Plan          │
├──────────────────────────────────────────┤
│  [Monthly ●]  [Annual - Save 20%]       │
├──────────────────────────────────────────┤
│ ┌────────┐ ┌────────┐ ┌────────┐       │
│ │STANDARD│ │PREMIUM │ │PLATINUM│       │
│ │ ₹399/m │ │ ₹799/m │ │₹1,499/m│       │
│ │        │ │ BEST   │ │        │       │
│ │ • 15   │ │ • ∞    │ │ • ∞    │       │
│ │   ints │ │   ints │ │   ints │       │
│ │ • See  │ │ • All  │ │ • All  │       │
│ │   views│ │  Std + │ │  Prem +│       │
│ │ • Adv  │ │ • Video│ │ • RM   │       │
│ │  filter│ │ • Boost│ │ • VIP  │       │
│ │        │ │ • Stlth│ │        │       │
│ │[Choose]│ │[Choose]│ │[Choose]│       │
│ └────────┘ └────────┘ └────────┘       │
├──────────────────────────────────────────┤
│ Feature Comparison (expandable)          │
├──────────────────────────────────────────┤
│ ✓ 30-day money-back guarantee           │
│ ✓ Cancel anytime                         │
└──────────────────────────────────────────┘
```

### Paywall Trigger Points (from source docs)

| Trigger | When | What Shows |
|---------|------|------------|
| Daily interest limit reached | FREE user sends 5th interest | PaywallSheet: "Upgrade for unlimited interests" |
| Advanced filter tapped | FREE user taps locked filter | PaywallSheet: "Upgrade to use advanced filters" |
| "See who viewed" tapped | FREE user taps viewers tab | PaywallSheet: "See who's interested in you" |
| Read receipts | FREE user wants to see read status | PaywallSheet: "Know when they read your message" |
| Video call | STANDARD user tries video | PaywallSheet: "Upgrade to Premium for video calls" |
| Boost | FREE user taps boost | PaywallSheet: "Boost your profile to the top" |
| After 3rd search | FREE user after 3 searches in session | Subtle banner (not paywall) |

### Razorpay Integration Flow

```
1. User selects plan on SubscriptionPlansScreen
2. SubscriptionViewModel calls Razorpay.checkout()
   - amount: plan.priceInPaise
   - currency: "INR" (or "USD" for NRI detected via location)
   - description: "Match ${plan.name} - ${billingPeriod}"
   - prefill: { email, phone }
3. Razorpay returns: orderId, paymentId, signature
4. ViewModel calls Cloud Function `verifyRazorpayPayment`
5. Cloud Function verifies HMAC → updates user doc:
   - subscriptionPlan = plan.id
   - subscriptionExpiry = now + duration
   - isPremium = true
6. UI observes user doc change → SubscriptionSuccessScreen
7. All feature gates immediately unlock (reactive Flow)
```

### Feature Gating Implementation

```kotlin
@Singleton
class CheckFeatureAccessUseCase @Inject constructor(
    private val sessionStore: SessionStore
) {
    suspend fun canAccess(feature: SubscriptionFeature): Boolean {
        val plan = sessionStore.currentPlan.first()
        return SubscriptionPlans.canAccess(plan, feature)
    }
    
    suspend fun requireAccess(feature: SubscriptionFeature): AccessResult {
        return if (canAccess(feature)) AccessResult.Granted
        else AccessResult.Blocked(minimumPlan = SubscriptionPlans.minimumPlan(feature))
    }
}

sealed class AccessResult {
    object Granted : AccessResult()
    data class Blocked(val minimumPlan: Plan) : AccessResult()
}
```

### Profile Boost Feature

```
- One-time purchase: ₹149 via Razorpay
- Effect: Profile appears in top 10 of all matching searches for 24 hours
- Firestore: user.boostActiveUntil = now + 24h
- Cloud Function `onBoostActivated` already exists (logs boost)
- UI: Golden border on profile card + "⚡ Boosted" chip during active period
- Limit: 1 boost at a time (can't stack)
- Included free with: PREMIUM (1/month), PLATINUM (unlimited)
```

### Updated verifyRazorpayPayment Plan Types

```typescript
// New plan mapping
const planDurations: Record<string, number> = {
  "standard_1m": 30,
  "standard_12m": 365,
  "premium_1m": 30,
  "premium_12m": 365,
  "platinum_1m": 30,
  "platinum_12m": 365,
  "boost_24h": 1, // Special: only sets boostActiveUntil
};

const planNames: Record<string, string> = {
  "standard_1m": "STANDARD",
  "standard_12m": "STANDARD",
  "premium_1m": "PREMIUM",
  "premium_12m": "PREMIUM",
  "platinum_1m": "PLATINUM",
  "platinum_12m": "PLATINUM",
};
```

---

## DEFINITION OF DONE

- [ ] SubscriptionPlansScreen displays all 4 tiers with pricing
- [ ] Monthly/Annual toggle with 20% discount visible
- [ ] Razorpay checkout launches and completes purchase
- [ ] Cloud Function verifies payment and updates user plan
- [ ] Feature gating enforced: locked features show PaywallSheet
- [ ] Daily interest limit enforced (5/15/unlimited per plan)
- [ ] Boost purchase works (₹149 → 24h boost active)
- [ ] "Active Plan" shown in Settings with expiry date
- [ ] NRI users see USD pricing (detected via countryOfResidence)
- [ ] BUILD SUCCESSFUL
