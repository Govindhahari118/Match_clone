# IMPL_10 — Family Portal & Biodata Generator
## Pin-to-Pin Implementation Plan

> **Gap:** Plans require parent/family sub-accounts with co-browsing, family approval workflow, and a Biodata PDF generator (free feature for lead generation). None exist.  
> **Impact:** 40-60% of Indian matrimony registrations are done by parents. Without Family Portal, we lose the largest user segment. Biodata PDF is the #1 free engagement hook.  
> **Source Docs:** PLAN_01 FP-01→04, PLAN_03 Section 3.3 (Suresh persona), PLAN_05 EX-05

---

## DELIVERABLES

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `ui/family/FamilyPortalScreen.kt` | Family dashboard: linked profiles + approval queue |
| 2 | `ui/family/FamilyInviteSheet.kt` | Invite parent/sibling via phone/WhatsApp link |
| 3 | `ui/family/FamilyApprovalCard.kt` | Match card with Approve/Reject/Notes for family |
| 4 | `ui/family/FamilyMemberList.kt` | Show linked family members with roles |
| 5 | `data/repository/FamilyRepository.kt` | CRUD for family links + approval status |
| 6 | `domain/model/FamilyMember.kt` | Data class: uid, relation, role, permissions |
| 7 | `domain/model/FamilyApproval.kt` | Data class: matchUid, status, notes, approvedBy |
| 8 | `ui/biodata/BiodataPreviewScreen.kt` | Preview biodata before PDF generation |
| 9 | `ui/biodata/BiodataGeneratorViewModel.kt` | Orchestrates PDF generation |
| 10 | `util/BiodataPdfGenerator.kt` | Generates branded PDF biodata with QR code |
| 11 | `ui/biodata/BiodataTemplateSelector.kt` | Choose template: Traditional/Modern/Minimal |

### Files to Modify

| File | Change |
|------|--------|
| `SettingsScreen.kt` | Add "Family Portal" and "Download Biodata" nav items |
| `ProfileDetailScreen.kt` | Add "Share with Family" and "Family Approved ✓" badge |
| `UserEntity.kt` | Add `profileFor` field (Migration 17→18) |
| `firestore.rules` | Add `families/{uid}/members` and `families/{uid}/approvals` rules |
| `functions/src/index.ts` | Add `onFamilyInviteAccepted` Cloud Function |

### Family Portal Architecture

```
Firestore Schema:
families/{primaryUid}/
  members/{memberUid}: { relation, role, permissions[], linkedAt }
  approvals/{matchUid}: { status, notes, approvedBy, decidedAt }

Roles:
  "owner" — The actual marriage candidate
  "parent" — Can view matches, approve/reject, cannot chat
  "sibling" — Can view matches, shortlist, cannot approve
  "elder" — Can view only family-approved matches

Permissions:
  "view_matches" — See discovery feed
  "approve_reject" — Approve or reject matches
  "view_chat" — Read (but not send) messages
  "shortlist" — Add to family shortlist
```

### Family Invite Flow

```
1. Owner goes to Family Portal → "Invite Family Member"
2. FamilyInviteSheet: Enter phone number + select relation (Father/Mother/Sibling/Elder)
3. Generates a unique invite link (Firebase Dynamic Link):
   https://match.page.link/family?uid=XXX&invite=YYY
4. Send via WhatsApp / SMS / copy link
5. Family member opens link → app opens → FamilyJoinScreen
6. Family member creates account (simplified: phone OTP only)
7. Cloud Function `onFamilyInviteAccepted` links accounts
8. Family member sees FamilyPortalScreen with owner's matches
```

### Family Approval Workflow

```
┌─────────────────────────────────┐
│ Family Portal                   │
├─────────────────────────────────┤
│ Pending Approval (3)            │
│ ┌─────────────────────────────┐ │
│ │ Priya, 26, Hyderabad        │ │
│ │ Kamma • MBA • ₹12L         │ │
│ │ Match Score: 82%            │ │
│ │                             │ │
│ │ [✓ Approve] [✗ Reject]     │ │
│ │ [Add Note...]               │ │
│ └─────────────────────────────┘ │
│                                 │
│ Family Approved (7)             │
│ • Ravi, 28, Vizag ✓ Dad        │
│ • Anil, 30, Hyderabad ✓ Mom    │
│                                 │
│ Family Rejected (2)             │
│ • Suresh — "Not our community" │
└─────────────────────────────────┘
```

### Biodata PDF Generator

**Template Structure (Traditional):**
```
┌─────────────────────────────────┐
│ 🕉️  BIODATA                     │
│ [Photo]                          │
│                                  │
│ PERSONAL DETAILS                 │
│ Name: Priya Reddy               │
│ DOB: 15-Mar-1998 (26 years)     │
│ Height: 5'4" | Complexion: Fair │
│ Mother Tongue: Telugu            │
│                                  │
│ EDUCATION & CAREER               │
│ MBA (Finance) — ISB Hyderabad   │
│ Senior Analyst — Deloitte       │
│ Income: ₹12-15 LPA             │
│                                  │
│ FAMILY DETAILS                   │
│ Father: Retired Bank Manager    │
│ Mother: Homemaker               │
│ Siblings: 1 Brother (married)  │
│ Family Type: Nuclear            │
│                                  │
│ RELIGION & COMMUNITY            │
│ Hindu • Kamma • Gothra: Kasyapa│
│ Manglik: No                     │
│                                  │
│ PARTNER PREFERENCES              │
│ Age: 28-33 | Height: 5'8"+     │
│ Education: Graduate+            │
│ Religion: Hindu (Kamma/Kapu)    │
│                                  │
│ CONTACT                          │
│ Matrimony ID: TLG-ABC12         │
│ [QR Code → Profile Link]       │
│                                  │
│ Generated by Match App          │
└─────────────────────────────────┘
```

**PDF Generation (using Android Canvas → PDF):**
```kotlin
class BiodataPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generate(user: UserEntity, template: BiodataTemplate): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        
        // Draw template sections using Canvas API
        drawHeader(canvas, user, template)
        drawPersonalDetails(canvas, user)
        drawEducation(canvas, user)
        drawFamily(canvas, user)
        drawReligion(canvas, user)
        drawPartnerPreferences(canvas, user)
        drawQrCode(canvas, user.matrimonyId)
        drawFooter(canvas)
        
        document.finishPage(page)
        val file = File(context.cacheDir, "biodata_${user.matrimonyId}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }
}
```

### QR Code (link to profile)

```kotlin
// Generate QR code pointing to: https://match.page.link/profile/{matrimonyId}
// Use ZXing library: com.google.zxing:core:3.5.2
```

### Dependencies

| Library | Purpose | Version |
|---------|---------|---------|
| ZXing | QR code generation for biodata | `com.google.zxing:core:3.5.2` |
| None (Android PdfDocument) | PDF generation | Built-in Android API |

---

## DEFINITION OF DONE

- [ ] Family invite generates shareable link (Dynamic Link)
- [ ] Family member can join with simplified signup
- [ ] Family Portal shows matches pending approval
- [ ] Parent can approve/reject with notes
- [ ] "Family Approved" badge shows on ProfileCard
- [ ] Biodata PDF generates correctly with all user fields
- [ ] 3 templates available (Traditional/Modern/Minimal)
- [ ] QR code in biodata links to profile
- [ ] PDF can be shared via Android share sheet
- [ ] BUILD SUCCESSFUL
