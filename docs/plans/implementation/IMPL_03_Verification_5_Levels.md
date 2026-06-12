# IMPL_03 — Verification System (5 Levels)
## Pin-to-Pin Implementation Plan

> **Gap:** Plans require 5-level verification (Phone→ID→Employment→Liveness→PRIME). Currently only phone OTP exists (Level 1). The `verificationLevel` field exists in DB but no UI or backend logic to verify.  
> **Impact:** Trust is THE differentiator for matrimony. Without visible verification badges, users don't trust profiles.  
> **Source Docs:** PLAN_01 L1-L5, PLAN_03 Section 8, PLAN_04 AUTH-07→10, PLAN_05 Section 1.4

---

## DELIVERABLES

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `ui/verification/VerificationHubScreen.kt` | Main screen showing all 5 levels with status + actions |
| 2 | `ui/verification/IdVerificationScreen.kt` | Level 2: Upload Aadhaar/PAN/Passport/DL + selfie |
| 3 | `ui/verification/EmploymentVerificationScreen.kt` | Level 3: LinkedIn link OR salary slip upload |
| 4 | `ui/verification/LivenessCheckScreen.kt` | Level 4: Real-time selfie vs ID face comparison (ML Kit) |
| 5 | `ui/verification/VerificationBadge.kt` | Composable badge component (Grey/Blue/Gold/Premium/PRIME) |
| 6 | `data/repository/VerificationRepository.kt` | Firestore operations for verification requests/status |
| 7 | `domain/model/VerificationRequest.kt` | Data class: type, documentUrl, selfieUrl, status, reviewedAt |
| 8 | `domain/model/VerificationLevel.kt` | Enum: PHONE(1), ID_VERIFIED(2), EMPLOYMENT(3), PHOTO_VERIFIED(4), PRIME(5) |
| 9 | Cloud Function: `onVerificationSubmitted` | Trigger on `verifications/{uid}` write → auto-check or queue for review |
| 10 | Cloud Function: `approveVerification` | Admin callable → update user's verificationLevel + send push |

### Files to Modify

| File | Change |
|------|--------|
| `ProfileCard.kt` | Show VerificationBadge next to name |
| `ProfileDetailScreen.kt` | Show verification section with level + "Get Verified" CTA |
| `SettingsScreen.kt` | Add "Verification" nav item |
| `functions/src/index.ts` | Add 2 new Cloud Functions |
| `firestore.rules` | Already has `verifications` rules (from Sprint 10) |
| `storage.rules` | Already has `verifications/` path (from Sprint 15) |

### Verification Levels Detail

| Level | Method | Badge | UI Flow |
|-------|--------|-------|---------|
| 1 | Phone OTP | 📱 Grey "Phone Verified" | Already done via Firebase Auth |
| 2 | Aadhaar/PAN/Passport upload | 🛡️ Blue "ID Verified" | Upload doc photo → Cloud Function validates format → Manual review |
| 3 | LinkedIn link OR salary slip | 💼 Gold "Employment Verified" | Enter LinkedIn URL or upload document |
| 4 | Live selfie vs ID photo | ✅ Green "Photo Verified" | ML Kit face detection: capture live → compare vs Level 2 doc |
| 5 | All 4 + manual team review | ⭐ PRIME badge | Auto-triggered when L1-L4 complete → admin queue |

### Level 2 Implementation (ID Verification)

```
User Flow:
1. User taps "Verify Identity" on VerificationHubScreen
2. IdVerificationScreen shows document type picker (Aadhaar/PAN/Passport/DL)
3. Camera opens → capture front of document
4. Upload to Firebase Storage: `verifications/{uid}/id_document_{timestamp}.jpg`
5. Create Firestore doc: `verifications/{uid}` with status: "pending"
6. Cloud Function sends "Verification submitted" push
7. Admin reviews (manual for MVP) → updates status to "approved"/"rejected"
8. Cloud Function on status change → update user.verificationLevel = 2 + send push

Technical:
- Use CameraX for capture (already in project for photos)
- No OCR needed for MVP (manual review)
- Future: ML Kit Text Recognition for auto-extracting name + DOB from Aadhaar
```

### Level 4 Implementation (Liveness Check)

```
User Flow:
1. User taps "Photo Verification" on VerificationHubScreen
2. LivenessCheckScreen shows camera preview
3. Instructions: "Turn head left" → "Turn head right" → "Smile"
4. ML Kit FaceDetection detects each pose
5. Final frame captured as selfie
6. Compare face embedding vs Level 2 document photo (ML Kit Face Contours)
7. If match > 0.85 → auto-approve Level 4
8. If match < 0.85 → queue for manual review

Technical:
- ML Kit FaceDetection (on-device, no network)
- FaceDetectorOptions: PERFORMANCE_MODE_ACCURATE, LANDMARK_MODE_ALL, CONTOUR_MODE_ALL
- Liveness: detect head rotation (euler angles), smile probability
- Comparison: use face contour similarity (no cloud API needed for MVP)
```

### Firestore Schema

```
verifications/{uid}:
  type: "aadhaar" | "pan" | "passport" | "driving_licence" | "employment" | "liveness"
  documentUrl: string (Storage path)
  selfieUrl: string (Storage path, for liveness)
  status: "pending" | "approved" | "rejected"
  submittedAt: Timestamp
  reviewedAt: Timestamp | null
  reviewerNote: string | null
  level: 2 | 3 | 4 | 5
```

### Badge Display Rules

```kotlin
@Composable
fun VerificationBadge(level: Int) {
    when (level) {
        0 -> {} // No badge
        1 -> Badge(color = Grey, text = "Phone Verified", icon = Phone)
        2 -> Badge(color = Blue, text = "ID Verified", icon = Shield)
        3 -> Badge(color = Gold, text = "Employment Verified", icon = Briefcase)
        4 -> Badge(color = Green, text = "Photo Verified", icon = CheckCircle)
        5 -> Badge(color = Purple, text = "PRIME", icon = Star)
    }
}
```

### Dependencies

| Library | Purpose | Already in project? |
|---------|---------|---------------------|
| ML Kit Face Detection | Liveness + face comparison | NO — add `com.google.mlkit:face-detection:16.1.6` |
| CameraX | Document/selfie capture | YES (PhotoEditorScreen uses it) |
| Firebase Storage | Document upload | YES |
| Firestore | Verification docs | YES |

---

## DEFINITION OF DONE

- [ ] VerificationHubScreen shows all 5 levels with current status
- [ ] Level 2: Document upload → pending → approved flow works E2E
- [ ] Level 4: Liveness check with 3 poses detected
- [ ] Badge shows on ProfileCard for all verified users
- [ ] Cloud Function updates verificationLevel on approval
- [ ] Push notification sent on verification approval
- [ ] Storage rules restrict verification docs to owner-write only (already done)
- [ ] BUILD SUCCESSFUL
