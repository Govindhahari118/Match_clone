# MatrimonyConnect — Production Deployment Checklist

## Pre-Deployment (Do these BEFORE going live)

### Firebase Console Setup
- [ ] Create production Firebase project (separate from dev)
- [ ] Enable Firebase Authentication → Email/Password provider
- [ ] Enable Firestore Database → Start in production mode
- [ ] Deploy `firestore.rules` from this repo
- [ ] Deploy `firestore.indexes.json` composite indexes
- [ ] Enable Firebase Storage → Deploy `storage.rules`
- [ ] Enable Firebase Cloud Messaging
- [ ] Enable Firebase Crashlytics
- [ ] Enable Firebase Analytics
- [ ] Enable Firebase Remote Config → Set initial values
- [ ] Set Razorpay secret in Firebase Functions config:
  ```
  firebase functions:config:set razorpay.key_secret="YOUR_LIVE_SECRET"
  ```

### Cloud Functions Deployment
- [ ] `cd functions && npm install`
- [ ] `npm run build` (TypeScript compilation)
- [ ] `firebase deploy --only functions`
- [ ] Verify all 4 triggers deployed: onInterestCreated, onMatchCreated, onNewMessage, verifyRazorpayPayment

### Android App Configuration
- [ ] Replace `google-services.json` with production project's version
- [ ] Update `RAZORPAY_KEY_ID` in `build.gradle.kts` release block with live key
- [ ] Create release keystore: `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000`
- [ ] Fill `keystore.properties` with release keystore credentials
- [ ] Update `versionCode` and `versionName` in `build.gradle.kts`
- [ ] Build release APK: `./gradlew assembleRelease`
- [ ] Build release AAB: `./gradlew bundleRelease`

### Play Store Listing
- [ ] Create Google Play Developer account ($25 one-time)
- [ ] Create app listing with:
  - App name: "MatrimonyConnect"
  - Short description (80 chars)
  - Full description (4000 chars)
  - Screenshots: Phone (min 2), Tablet (optional), Feature graphic (1024x500)
  - App icon (512x512 PNG)
  - Category: Social → Dating
  - Content rating questionnaire
  - Privacy policy URL (host `privacy-policy.html`)
  - Target audience: 18+
- [ ] Upload signed AAB
- [ ] Set pricing: Free (with in-app purchases)
- [ ] Complete Data Safety section matching privacy policy
- [ ] Submit for review

### Security Checklist
- [ ] Firestore rules reviewed (no public write access)
- [ ] Storage rules reviewed (size limits, auth required)
- [ ] Razorpay payment verification is SERVER-SIDE only (Cloud Function)
- [ ] No API keys in source code (use BuildConfig or Remote Config)
- [ ] ProGuard/R8 enabled for release builds
- [ ] `FLAG_SECURE` on sensitive screens (already done)
- [ ] Biometric lock available (already done)
- [ ] Chat encryption with AES-256-GCM (already done)
- [ ] BCrypt password hashing (already done)

### Testing
- [ ] Test sign-up flow end-to-end (Firebase Auth + Firestore profile creation)
- [ ] Test sign-in on a different device (profile loads from Firestore)
- [ ] Test like/unlike (syncs to Firestore interests collection)
- [ ] Test mutual match detection (match document created)
- [ ] Test FCM push notifications (interest, match, message)
- [ ] Test Razorpay payment flow (test mode first, then live)
- [ ] Test premium status activation after payment
- [ ] Test account deletion (removes from Auth + Firestore + Room)
- [ ] Test offline mode (Room cache should work without network)
- [ ] Test chat messaging between two real devices
- [ ] Test photo upload and privacy controls

## Post-Launch

### Week 1
- [ ] Monitor Crashlytics for any crashes
- [ ] Check Firebase Analytics for user behavior
- [ ] Monitor Firestore usage and billing
- [ ] Respond to initial Play Store reviews

### Month 1
- [ ] Set up Firebase App Distribution for beta testers
- [ ] Enable A/B testing via Remote Config
- [ ] Add Firebase Dynamic Links for referral program
- [ ] Monitor subscription conversion rates

### Ongoing
- [ ] Regular security rule audits
- [ ] Database index optimization based on query patterns
- [ ] Remote Config for feature flags (no app update needed)
- [ ] Crashlytics monitoring and bug fixing
