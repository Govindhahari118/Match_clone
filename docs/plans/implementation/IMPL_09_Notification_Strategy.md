# IMPL_09 — Notification Strategy (13 Types + Timing)
## Pin-to-Pin Implementation Plan

> **Gap:** Plan 04 Section D defines 13 notification types with specific timing rules. Currently only 3 push triggers exist (interest/match/message). Daily match + expiry + inactivity Cloud Functions exist but 7 types are missing entirely.  
> **Impact:** Notifications are the #1 retention lever for matrimony apps. Without proper nudges, 7-day retention drops below 10%.  
> **Source Docs:** PLAN_04 Section D (13 types), PLAN_08 Section 2.2 (funnel analytics)

---

## DELIVERABLES

### New Cloud Functions to Create (in `functions/src/index.ts`)

| # | Function | Trigger | Timing |
|---|----------|---------|--------|
| 1 | `sendProfileViewDigest` | Scheduled | Daily 7 PM IST — batch "X people viewed you today" |
| 2 | `sendProfileIncompleteD2` | Scheduled | Daily — find users D+2 with completeness < 70% |
| 3 | `sendProfileIncompleteD7` | Scheduled | Daily — find users D+7 with completeness < 70% |
| 4 | `sendInactiveD14` | Scheduled | Daily — weekly top 5 matches email for 14-day inactive |
| 5 | `sendInactiveD30` | Scheduled | Daily — "Profile auto-hidden" for 30-day inactive |
| 6 | `sendVerificationApproved` | Firestore trigger | On verification status → "approved" |
| 7 | `sendBoostSummary` | Scheduled (hourly check) | When boost expires → "Boost ended: X views gained" |

### Existing Functions to Enhance

| Function | Enhancement Needed |
|----------|-------------------|
| `onInterestCreated` | Already sends push — add in-app notification doc |
| `onMatchCreated` | Already sends push — add in-app notification doc |
| `onNewMessage` | Add 30-sec delay logic if app is in foreground |
| `sendDailyMatchNotification` | Already exists — add email variant |
| `sendSubscriptionExpiry` | Already exists — add 1-day urgency variant |
| `sendInactivityNudge` | Already exists (7-day) — but missing 14-day + 30-day |

### New Files to Create (Android)

| # | File | Purpose |
|---|------|---------|
| 1 | `ui/notifications/NotificationCenterScreen.kt` | In-app notification inbox (all types) |
| 2 | `ui/notifications/NotificationPrefsScreen.kt` | Per-type toggle settings |
| 3 | `data/repository/NotificationRepository.kt` | Read/mark-read in-app notifications from Firestore |
| 4 | `domain/model/AppNotification.kt` | Data class for in-app notification |

### Files to Modify

| File | Change |
|------|--------|
| `MatchFcmService.kt` | Handle all 13 notification types with proper channel routing |
| `HomeScreen.kt` | Show notification bell with unread count badge |
| `functions/src/index.ts` | Add 7 new functions + enhance 5 existing |

### Complete Notification Type Matrix

| # | Trigger | Message | Timing | Channel | Priority | In-App Doc |
|---|---------|---------|--------|---------|----------|-----------|
| 1 | New Interest | "[Name] is interested ❤️" | Immediate | interests | HIGH | Yes |
| 2 | Interest Accepted | "[Name] accepted! Say hello 👋" | Immediate | matches | HIGH | Yes |
| 3 | New Message | "[Name]: message preview..." | Immediate (30s delay if app open) | messages | HIGH | No (chat has its own) |
| 4 | Profile Viewed | "5 people viewed your profile today 👀" | Batched at 7 PM | system | NORMAL | Yes |
| 5 | Daily Matches | "12 new profiles match you ✨" | 9 AM IST | matches | HIGH | Yes |
| 6 | Profile Incomplete (D+2) | "Add a photo — get 3x more matches 📸" | 48h after signup | system | NORMAL | Yes |
| 7 | Profile Incomplete (D+7) | "75% done! Finish to unlock matches" | 7d after signup | system | NORMAL | Yes |
| 8 | Inactive (D+7) | "We miss you! [Name] from [City] just joined" | 7d inactive | system | NORMAL | Yes |
| 9 | Inactive (D+14) | "Top 5 matches this week" (email only) | 14d inactive | — | EMAIL | No |
| 10 | Inactive (D+30) | "Profile hidden — reactivate to be found" | 30d inactive | system | HIGH | Yes |
| 11 | Subscription Expiry | "Plan expires in X days — renew now" | 7d, 3d, 1d before | system | HIGH (3d/1d) | Yes |
| 12 | Verification Approved | "Verified! Your [level] badge is live ✓" | Immediate | system | HIGH | Yes |
| 13 | Boost Summary | "Boost ended: 47 extra views gained!" | On boost expiry | system | NORMAL | Yes |

### In-App Notification Storage

```
Firestore: notifications/{uid}/items/{notifId}
Schema:
{
  type: "interest" | "match" | "daily_match" | "profile_view" | "boost_summary" | ...
  title: string
  body: string
  data: { userId?: string, matchId?: string }
  readAt: Timestamp | null
  createdAt: Timestamp
}
```

### Notification Preferences

```
Firestore: notificationPrefs/{uid}
Schema:
{
  interests: { push: true, email: false, inApp: true }
  matches: { push: true, email: true, inApp: true }
  messages: { push: true, email: false, inApp: false }
  dailyMatches: { push: true, email: true, inApp: true }
  profileViews: { push: true, email: false, inApp: true }
  subscriptionAlerts: { push: true, email: true, inApp: true }
  profileNudges: { push: true, email: true, inApp: true }
  inactivityNudges: { push: true, email: true, inApp: true }
}
```

### NotificationCenterScreen Layout

```
┌──────────────────────────────────────┐
│ 🔔 Notifications              [⚙️]  │
├──────────────────────────────────────┤
│ Today                                │
│ ┌────────────────────────────────┐  │
│ │ ❤️ Priya is interested         │  │
│ │    2 hours ago                  │  │
│ └────────────────────────────────┘  │
│ ┌────────────────────────────────┐  │
│ │ ✨ 8 new matches for you       │  │
│ │    9:00 AM                      │  │
│ └────────────────────────────────┘  │
│                                      │
│ Yesterday                            │
│ ┌────────────────────────────────┐  │
│ │ 👀 5 people viewed your profile │  │
│ │    7:00 PM                      │  │
│ └────────────────────────────────┘  │
│ ...                                  │
└──────────────────────────────────────┘
```

### FCM Channel Routing (update MatchFcmService.kt)

```kotlin
private fun channelFor(type: String): String = when (type) {
    "interest", "super_interest" -> CH_INTERESTS
    "match_accepted", "daily_match" -> CH_MATCHES
    "message" -> CH_MESSAGES
    "safety_alert", "sos" -> CH_SAFETY
    "profile_viewed", "subscription_expiry", "verification_approved",
    "boost_summary", "profile_incomplete", "inactivity_nudge" -> CH_SYSTEM
    else -> CH_SYSTEM
}
```

---

## DEFINITION OF DONE

- [ ] All 13 notification types trigger at correct timing
- [ ] NotificationCenterScreen shows in-app notification inbox
- [ ] Unread badge on notification bell in HomeScreen
- [ ] NotificationPrefsScreen allows per-type push/email/inApp toggles
- [ ] Profile view batching sends at 7 PM (not real-time)
- [ ] D+2 and D+7 incomplete profile nudges work
- [ ] D+14 and D+30 inactivity nudges work
- [ ] Verification approval triggers immediate push
- [ ] Boost summary sent when boost expires
- [ ] User can disable any notification type
- [ ] BUILD SUCCESSFUL
