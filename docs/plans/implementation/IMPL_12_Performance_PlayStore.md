# IMPL_12 — Performance, App Check & Play Store Readiness
## Pin-to-Pin Implementation Plan

> **Gap:** Plans require <2s cold start, Baseline Profiles, Firebase App Check, root detection, and full Play Store compliance (18+ rating, safety policy, moderation description). None implemented.  
> **Impact:** Without Baseline Profiles, cold start is 3-5s. Without App Check, backend is vulnerable to abuse. Without Play Store compliance, submission will be rejected.  
> **Source Docs:** PLAN_04 PS-01→07, PLAN_05 Section 5, PLAN_06 Section 5, PLAN_09 Section 1

---

## DELIVERABLES

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `baselineprofile/BaselineProfileGenerator.kt` | Macrobenchmark test generating Baseline Profile |
| 2 | `security/AppCheckInitializer.kt` | Firebase App Check with Play Integrity provider |
| 3 | `res/xml/network_security_config.xml` | Certificate pinning (from IMPL_08, listed here too) |
| 4 | `store/STORE_LISTING_COPY.md` | Play Store description, title, short description |
| 5 | `store/DATA_SAFETY_FORM.md` | Answers for Play Store Data Safety form |
| 6 | `store/CONTENT_RATING_ANSWERS.md` | IARC questionnaire answers (dating = mature) |

### Files to Modify

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Add Baseline Profile + Firebase App Check + Performance Monitoring dependencies |
| `build.gradle.kts` (project) | Add macrobenchmark module configuration |
| `MainActivity.kt` | Initialize App Check, add performance tracing |
| `AppModule.kt` | Provide performance trace helpers |
| `functions/src/index.ts` | Enable App Check verification on callable functions |
| `privacy-policy.html` | Update for DPDP Act 2023 + Play Store compliance |
| `AndroidManifest.xml` | Add `tools:targetApi="34"`, metadata for App Check |

### Firebase App Check Setup

```kotlin
// AppCheckInitializer.kt
@Singleton
class AppCheckInitializer @Inject constructor() {
    fun initialize() {
        FirebaseApp.getInstance()
        val factory = PlayIntegrityAppCheckProviderFactory.getInstance()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }
}

// In MainActivity.onCreate()
appCheckInitializer.initialize()
```

**Cloud Functions enforcement:**
```typescript
// Add to each callable function:
export const someFunction = functions.https.onCall(async (data, context) => {
    // App Check verification
    if (!context.app) {
        throw new functions.https.HttpsError(
            "failed-precondition",
            "App Check token missing"
        );
    }
    // ... rest of function
});
```

### Baseline Profile Generation

```kotlin
// In a new :baselineprofile module
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()
    
    @Test
    fun generateBaselineProfile() {
        rule.collect(packageName = "com.match.app") {
            // Cold start
            pressHome()
            startActivityAndWait()
            
            // Navigate key journeys
            device.findObject(By.text("Discover")).click()
            device.waitForIdle()
            
            device.findObject(By.text("Profile")).click()
            device.waitForIdle()
            
            // Scroll discovery feed
            device.findObject(By.scrollable(true)).scroll(Direction.DOWN, 2.0f)
        }
    }
}
```

**build.gradle.kts addition:**
```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        release {
            // Use generated Baseline Profile
            baselineProfile.automaticGenerationDuringBuild = true
        }
    }
}

dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    baselineProfile(project(":baselineprofile"))
}
```

### Performance Monitoring

```kotlin
// Custom traces for critical paths
class PerformanceTracer @Inject constructor() {
    fun traceProfileLoad(uid: String): Trace {
        val trace = Firebase.performance.newTrace("profile_load")
        trace.putAttribute("uid", uid)
        trace.start()
        return trace
    }
    
    fun traceDiscoveryFeed(): Trace {
        val trace = Firebase.performance.newTrace("discovery_feed_load")
        trace.start()
        return trace
    }
    
    fun traceSearchQuery(filterCount: Int): Trace {
        val trace = Firebase.performance.newTrace("search_query")
        trace.putMetric("filter_count", filterCount.toLong())
        trace.start()
        return trace
    }
}
```

### Target Metrics

| Metric | Current (est.) | Target | How |
|--------|---------------|--------|-----|
| Cold start | ~4s | < 2s | Baseline Profile + lazy init |
| Profile card render | ~200ms | < 100ms | Coil preload + WebP |
| Search results | ~2s | < 1s | Composite indexes (already added) |
| APK size | ~15MB | < 10MB | R8 shrink + remove unused deps |
| First frame | ~1.5s | < 1s | SplashScreen API + async init |

### Play Store Compliance Checklist

| Requirement | Status | Action |
|-------------|--------|--------|
| Content rating: Dating | ❌ | Set in Play Console → IARC questionnaire |
| 18+ age gate in app | ✅ | DOB check in SignUpScreen (age >= 18) |
| Safety page URL in listing | ❌ | Link to SafetyCenterScreen or web page |
| Human moderation described | ❌ | Add to store description |
| Privacy policy URL | 🟡 | Update and deploy to Firebase Hosting |
| Contact email | ❌ | Create support@matchapp.in |
| Data Safety form filled | ❌ | Answer all questions per STORE_DATA_SAFETY.md |
| Screenshots without fake badges | ❌ | Create honest screenshots |
| Short description (80 chars) | ❌ | "Telugu Matrimony — AI-powered matches for AP, Telangana & NRI community" |
| Full description (4000 chars) | ❌ | Write compelling store listing |
| Feature graphic (1024x500) | ❌ | Design in Canva with Telugu branding |
| App icon (512x512) | ❌ | Create with maroon/gold branding |
| targetSdk = 34 | ✅ | Already set in build.gradle.kts |

### Data Safety Form Answers

| Question | Answer |
|----------|--------|
| Does app collect user data? | Yes |
| Location | Approximate location (city-level, not GPS) |
| Personal info | Name, email, phone, DOB, photos |
| Financial info | Purchase history (Razorpay) |
| Contacts | Emergency contact (optional) |
| Photos/videos | Profile photos, video intro |
| Health info | No |
| Messages | In-app messaging between matched users |
| Is data encrypted in transit? | Yes (TLS 1.3 via Firebase) |
| Can users request data deletion? | Yes (in-app account deletion) |
| Is data shared with third parties? | Firebase (processor), Razorpay (payment), Agora (calls) |

### Dependencies to Add

```kotlin
// app/build.gradle.kts
implementation("com.google.firebase:firebase-appcheck-playintegrity:17.1.2")
implementation("com.google.firebase:firebase-perf:20.5.2")
implementation("androidx.profileinstaller:profileinstaller:1.3.1")
```

---

## DEFINITION OF DONE

- [ ] Firebase App Check enabled (Play Integrity provider)
- [ ] All callable Cloud Functions verify App Check token
- [ ] Baseline Profile generated and included in release build
- [ ] Cold start < 2s on mid-range device (measured via Firebase Performance)
- [ ] Performance traces for profile load, search, discovery
- [ ] Privacy policy deployed to Firebase Hosting (live URL)
- [ ] Store listing copy complete (title, short desc, full desc)
- [ ] Data Safety form answers documented
- [ ] Content rating questionnaire answers documented
- [ ] APK size < 15MB after R8 optimization
- [ ] BUILD SUCCESSFUL
