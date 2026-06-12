# IMPL_11 — Photo Pipeline (Multi-Photo + Moderation + Privacy)
## Pin-to-Pin Implementation Plan

> **Gap:** Plans require up to 25 photos, AI face detection (reject non-face), NSFW moderation, WebP compression, watermarking, photo blurring until consent. Currently only single photo upload exists.  
> **Impact:** Photos are the #1 factor in profile engagement. Single photo = low engagement. No moderation = fake/NSFW profiles = app store removal.  
> **Source Docs:** PLAN_02 PH-01→09, PLAN_05 Section 1.4, PLAN_06 Section 4

---

## DELIVERABLES

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `ui/photo/PhotoGalleryManager.kt` | Multi-photo upload/reorder/delete (up to 10 photos) |
| 2 | `ui/photo/PhotoUploadSheet.kt` | Bottom sheet: Camera / Gallery picker |
| 3 | `ui/photo/PhotoCropScreen.kt` | Crop + basic adjustments before upload |
| 4 | `util/ImageCompressor.kt` | WebP conversion + resize to max 1080px |
| 5 | `util/FaceValidator.kt` | ML Kit face detection: ensure primary has exactly 1 face |
| 6 | `util/PhotoWatermark.kt` | Add subtle "Match" watermark on display |
| 7 | Cloud Function: Enhanced `onPhotoUpload` | Vision API NSFW + duplicate pHash check |

### Files to Modify

| File | Change |
|------|--------|
| `PhotoEditorScreen.kt` | Replace with multi-photo gallery support |
| `ProfileDetailScreen.kt` | Photo carousel (swipeable gallery) |
| `ProfileCard.kt` | Show photo count indicator (e.g., "1/5") |
| `FirebaseStorageService.kt` | Add WebP compression before upload + multi-photo paths |
| `functions/src/index.ts` (`onPhotoUpload`) | Add Vision API SafeSearch call |
| `UserEntity.kt` | Change `photoUrl` to `photoUrls` (JSON array) — or keep primary + add `galleryPhotos` |
| `app/build.gradle.kts` | Add ML Kit Face Detection dependency |

### Multi-Photo Architecture

```
Firebase Storage paths:
  photos/{uid}/primary.webp     → Main profile photo (always shown)
  photos/{uid}/gallery_1.webp   → Gallery photo 1
  photos/{uid}/gallery_2.webp   → Gallery photo 2
  ...
  photos/{uid}/gallery_9.webp   → Gallery photo 9 (max 10 total)

Firestore user doc:
  photoUrl: string (primary photo URL)
  galleryPhotos: string[] (up to 9 gallery URLs)
  photoCount: number
```

### Upload Pipeline (per photo)

```
1. User selects photo (camera or gallery)
2. PhotoCropScreen → user crops to 4:5 ratio
3. ImageCompressor:
   a. Resize to max 1080x1350 pixels
   b. Convert to WebP (quality 85%)
   c. Result should be < 500KB typically
4. FaceValidator (primary photo only):
   a. ML Kit FaceDetection on bitmap
   b. If 0 faces → reject: "Please upload a photo showing your face"
   c. If 2+ faces → reject: "Primary photo must show only you"
   d. If 1 face → proceed
5. Upload to Firebase Storage (photos/{uid}/{filename}.webp)
6. Cloud Function `onPhotoUpload` triggers:
   a. Vision API SafeSearch: adult/violence/racy scores
   b. If any score > LIKELY → flag + hide + notify user
   c. If all UNLIKELY/VERY_UNLIKELY → approve
   d. Store moderation result in `photoModerations` collection
7. On success → update user doc with new URL
```

### NSFW Moderation (Cloud Function Enhancement)

```typescript
// Enhanced onPhotoUpload
import vision from "@google-cloud/vision";

export const onPhotoUpload = functions.storage.object().onFinalize(async (object) => {
  const filePath = object.name;
  if (!filePath?.startsWith("photos/")) return;
  
  const uid = filePath.split("/")[1];
  const bucket = admin.storage().bucket(object.bucket);
  const file = bucket.file(filePath);
  
  // Run Vision API SafeSearch
  const client = new vision.ImageAnnotatorClient();
  const [result] = await client.safeSearchDetection(
    `gs://${object.bucket}/${filePath}`
  );
  const safe = result.safeSearchAnnotation;
  
  const isNsfw = ["LIKELY", "VERY_LIKELY"].includes(safe?.adult || "") ||
                 ["LIKELY", "VERY_LIKELY"].includes(safe?.violence || "");
  
  await db.collection("photoModerations").add({
    uid, filePath,
    adult: safe?.adult, violence: safe?.violence, racy: safe?.racy,
    status: isNsfw ? "rejected" : "approved",
    moderatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  
  if (isNsfw) {
    // Delete the file
    await file.delete();
    // Notify user
    const userDoc = await db.collection("users").doc(uid).get();
    const fcmToken = userDoc.data()?.fcmToken;
    if (fcmToken) {
      await messaging.send({
        token: fcmToken,
        data: { type: "photo_rejected", title: "Photo Rejected", body: "Your photo didn't meet guidelines." },
      });
    }
  }
});
```

### ML Kit Face Detection (Client-Side)

```kotlin
@Singleton
class FaceValidator @Inject constructor() {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(0.15f)
            .build()
    )
    
    suspend fun validatePrimaryPhoto(bitmap: Bitmap): FaceValidationResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(image).await()
        
        return when {
            faces.isEmpty() -> FaceValidationResult.NoFace
            faces.size > 1 -> FaceValidationResult.MultipleFaces
            else -> {
                val face = faces[0]
                if (face.boundingBox.width() < bitmap.width * 0.1f) {
                    FaceValidationResult.FaceTooSmall
                } else {
                    FaceValidationResult.Valid
                }
            }
        }
    }
}

sealed class FaceValidationResult {
    object Valid : FaceValidationResult()
    object NoFace : FaceValidationResult()
    object MultipleFaces : FaceValidationResult()
    object FaceTooSmall : FaceValidationResult()
}
```

### WebP Compression

```kotlin
@Singleton
class ImageCompressor @Inject constructor() {
    fun compress(bitmap: Bitmap, maxWidth: Int = 1080, quality: Int = 85): ByteArray {
        val scaled = if (bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width
            Bitmap.createScaledBitmap(bitmap, maxWidth, (bitmap.height * ratio).toInt(), true)
        } else bitmap
        
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
        return outputStream.toByteArray()
    }
}
```

### Dependencies

```kotlin
// app/build.gradle.kts
implementation("com.google.mlkit:face-detection:16.1.6")
// Cloud Functions package.json
"@google-cloud/vision": "^4.0.0"
```

---

## DEFINITION OF DONE

- [ ] User can upload up to 10 photos (1 primary + 9 gallery)
- [ ] Primary photo requires face detection pass (1 face, visible)
- [ ] All photos auto-compressed to WebP < 500KB
- [ ] Photo carousel on profile detail (swipeable)
- [ ] Cloud Function rejects NSFW photos via Vision API
- [ ] Rejected photo → user gets push notification
- [ ] Photo count badge on profile card
- [ ] Photo reordering (drag to rearrange)
- [ ] Photo delete (swipe or long-press)
- [ ] BUILD SUCCESSFUL
