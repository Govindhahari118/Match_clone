# IMPL_06 — Match Score Display + Compatibility UI
## Pin-to-Pin Implementation Plan

> **Gap:** MatchScoreEngine.kt computes 9-dimension scores. TenPorutham.kt computes 10 poruthams. But NO screen shows these scores to users. Profile cards don't display compatibility %. Match detail doesn't explain "Why this match?".  
> **Impact:** The core USP (AI-powered compatibility) is invisible. Users can't see why a profile was recommended.  
> **Source Docs:** PLAN_01 ME-01→09, PLAN_03 Match-05, PLAN_05 EX-01 (Match Score™)

---

## DELIVERABLES

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `ui/match/CompatibilityScoreBadge.kt` | Circular % badge shown on profile cards |
| 2 | `ui/match/CompatibilityBreakdownSheet.kt` | Bottom sheet with 9-dimension radar chart + per-dimension score |
| 3 | `ui/match/PoruthamReportScreen.kt` | Full 10-Porutham compatibility report (paid feature) |
| 4 | `ui/match/WhyThisMatchChip.kt` | "Recommended because: Same city + Education" chip on cards |
| 5 | `ui/match/CompatibilityViewModel.kt` | Computes scores on-demand for a pair |
| 6 | `ui/match/RadarChartComposable.kt` | Custom Compose Canvas radar chart for 9 dimensions |

### Files to Modify

| File | Change |
|------|--------|
| `ui/discovery/ProfileCard.kt` | Add CompatibilityScoreBadge overlay (top-right corner) |
| `ui/discovery/DiscoveryScreen.kt` / `SwipeDiscoveryScreen.kt` | Compute match score before displaying cards |
| `ui/profile/ProfileDetailScreen.kt` | Add "Compatibility" section → tap opens BreakdownSheet |
| `MatchScoreEngine.kt` | Add `topReasons(user, candidate): List<String>` for "Why this match" |
| `TenPorutham.kt` | Already complete — just wire to PoruthamReportScreen |
| `SubscriptionPlans.kt` | Gate: CompatibilityBreakdown = STANDARD+, PoruthamReport = PREMIUM+ |

### Match Score Badge Design

```
┌───────────────────────────────────┐
│ [Profile Card]                    │
│                          ┌─────┐ │
│   Photo                  │ 82% │ │  ← CompatibilityScoreBadge
│                          │  ●  │ │     (circular, green/yellow/red)
│                          └─────┘ │
│ Name, 28, Hyderabad              │
│ [Same city] [Both IT] [Kamma]    │  ← WhyThisMatchChips (top 3 reasons)
└───────────────────────────────────┘
```

### Compatibility Breakdown Sheet

```
┌──────────────────────────────────────┐
│ Compatibility with Priya      82%   │
├──────────────────────────────────────┤
│                                      │
│        [Radar Chart Canvas]          │
│     Astro ──── Religion              │
│    /                    \            │
│  Personality      Education          │
│    \                    /            │
│     Physical ── Location             │
│        Lifestyle ── Age              │
│            Family                    │
│                                      │
├──────────────────────────────────────┤
│ Astrology        █████████░  90%    │
│ Religion & Caste █████████░  85%    │
│ Education        ████████░░  80%    │
│ Location         ██████████  100%   │
│ Age              ████████░░  78%    │
│ Family Values    ███████░░░  70%    │
│ Lifestyle        █████████░  85%    │
│ Physical         ████████░░  80%    │
│ Personality      █████░░░░░  50%    │
├──────────────────────────────────────┤
│ [View Full Porutham Report →] 🔒    │
└──────────────────────────────────────┘
```

### Porutham Report Screen (Premium Feature — ₹299 per pair OR included in PREMIUM+)

```
┌──────────────────────────────────────┐
│ 10-Porutham Report                  │
│ You & Priya                         │
├──────────────────────────────────────┤
│ Overall: 8/10 Poruthams ✓          │
│                                      │
│ 1. దినం (Dina)     ✓ Compatible    │
│    Count: 7, Position: 3            │
│                                      │
│ 2. గణం (Gana)      ✓ Compatible    │
│    Both Deva gana                    │
│                                      │
│ 3. మహేంద్ర (Mahendra) ✓ Compatible │
│    Star count divisible by 3         │
│                                      │
│ 4. స్త్రీ దీర్ఘ (Stree) ✓          │
│    Count: 15 (> 13 required)         │
│                                      │
│ 5. యోని (Yoni)     ✗ Not Compatible│
│    Animals are enemies               │
│                                      │
│ ... (all 10)                         │
│                                      │
│ [Share Report PDF] [Download]        │
└──────────────────────────────────────┘
```

### Score Color Coding

```kotlin
fun scoreColor(score: Float): Color = when {
    score >= 0.8f -> Color(0xFF4CAF50) // Green — Excellent
    score >= 0.6f -> Color(0xFFFFC107) // Amber — Good
    score >= 0.4f -> Color(0xFFFF9800) // Orange — Average
    else -> Color(0xFFF44336) // Red — Low
}
```

### "Why This Match?" Logic

```kotlin
fun topReasons(user: UserEntity, candidate: UserEntity): List<String> {
    val reasons = mutableListOf<String>()
    if (user.city == candidate.city) reasons.add("Same city")
    if (user.religion == candidate.religion) reasons.add("Same religion")
    if (user.caste == candidate.caste) reasons.add("Same community")
    if (user.education == candidate.education) reasons.add("Similar education")
    if (user.occupationCategory == candidate.occupationCategory) reasons.add("Same profession")
    if (user.diet == candidate.diet) reasons.add("Same diet preference")
    if (user.motherTongue == candidate.motherTongue) reasons.add("Same mother tongue")
    // ... more
    return reasons.take(3) // Show max 3 on card
}
```

### Performance Considerations

- Score computation is O(1) per pair (no DB calls, just field comparison)
- Pre-compute and cache scores in `dailyMatches/{uid}.profiles[].score`
- For discovery feed: compute on-device as cards load (lazy, fast)
- For Porutham report: compute on first view, cache in `poruthamReports/{uid}_{targetUid}`

---

## DEFINITION OF DONE

- [ ] CompatibilityScoreBadge shows on every profile card in discovery
- [ ] Badge color reflects score (green/amber/orange/red)
- [ ] Tapping badge or "View Compatibility" opens BreakdownSheet
- [ ] Radar chart renders all 9 dimensions
- [ ] Per-dimension progress bars show individual scores
- [ ] "Why this match?" shows top 3 reasons on card
- [ ] Porutham Report screen shows all 10 poruthams with Telugu names
- [ ] Porutham Report gated behind PREMIUM+ (or ₹299 one-time)
- [ ] BUILD SUCCESSFUL
