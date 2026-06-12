# IMPL_08 — Privacy & Security Hardening
## Pin-to-Pin Implementation Plan

> **Gap:** Plans require FLAG_SECURE, root detection, certificate pinning, photo blurring, stealth mode enforcement, biometric auth. Fields exist in DB (`stealthMode`, `showLastActive`, `showHoroscope`) but none are enforced in the UI or network layer.  
> **Impact:** Google Play requires dating apps to demonstrate safety measures. Without these, the app can be rejected or face negative reviews. Privacy-conscious users (primary Telugu women persona) won't use it.  
> **Source Docs:** PLAN_01 PS-01→08, PLAN_04 SEC-01→08, PLAN_05 Section 3, PLAN_08 SF-01→10

---

## DELIVERABLES

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `security/RootDetector.kt` | Root/jailbreak detection using RootBeer library |
| 2 | `security/SecurityGate.kt` | Central security check on app launch (root, emulator, debugger) |
| 3 | `security/BiometricAuthManager.kt` | Biometric prompt for sensitive screens (contact reveal, chat) |
| 4 | `ui/privacy/PhotoPrivacyEnforcer.kt` | Apply blur/hide logic based on photo privacy settings |
| 5 | `ui/privacy/StealthModeManager.kt` | Suppress profile from browse when stealth active |
| 6 | `res/xml/network_security_config.xml` | Certificate pinning for Firebase + Razorpay domains |

### Files to Modify

| File | Change |
|------|--------|
| `MainActivity.kt` | Add FLAG_SECURE for release builds, root detection check on launch |
| `AndroidManifest.xml` | Reference `network_security_config.xml`, add `USE_BIOMETRIC` permission |
| `app/build.gradle.kts` | Add RootBeer dependency, ML Kit dependency |
| `ui/profile/ProfileDetailScreen.kt` | Enforce photo privacy (blur if user not connected) |
| `data/remote/FirestorePagingSource.kt` | Exclude stealth mode users from browse queries |
| `ui/chat/ChatScreen.kt` | Optional biometric unlock before entering chat |
| `MatchDetailScreen.kt` | Biometric required before contact reveal |
| `PrivacyDashboardScreen.kt` | Wire toggles to actual enforcement logic |

### FLAG_SECURE Implementation

```kotlin
// In MainActivity.kt onCreate()
if (!BuildConfig.DEBUG) {
    window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    )
}
```

**Scope:** Applied globally in release builds. This prevents:
- Screenshots of the entire app
- Screen recording
- Recent apps thumbnail showing profile data

**Alternative (per-screen):** If full-app FLAG_SECURE is too aggressive:
```kotlin
// Apply only on sensitive screens
DisposableEffect(Unit) {
    val window = (context as Activity).window
    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
}
```

Screens requiring FLAG_SECURE:
- ProfileDetailScreen (shows photos/contact)
- ChatScreen (private messages)
- KundliScreen (horoscope data)
- VerificationHubScreen (document images)

### Root Detection

```kotlin
@Singleton
class RootDetector @Inject constructor(@ApplicationContext private val context: Context) {
    
    fun isDeviceRooted(): Boolean {
        val rootBeer = RootBeer(context)
        return rootBeer.isRooted
    }
    
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MANUFACTURER.contains("Genymotion"))
    }
}
```

**Behavior on root detected (release only):**
- Show warning dialog: "Device security compromised"
- Allow continue but disable: contact reveal, payment, document upload
- Log event to Firebase Analytics: `security_root_detected`

### Certificate Pinning

```xml
<!-- res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">firebaseio.com</domain>
        <domain includeSubdomains="true">googleapis.com</domain>
        <domain includeSubdomains="true">firestore.googleapis.com</domain>
        <domain includeSubdomains="true">api.razorpay.com</domain>
        <pin-set expiration="2026-12-31">
            <!-- Firebase/Google pins (update annually) -->
            <pin digest="SHA-256">...</pin>
            <pin digest="SHA-256">...</pin>
        </pin-set>
    </domain-config>
    <debug-overrides>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

### Photo Privacy Enforcement

```kotlin
enum class PhotoVisibility { EVERYONE, CONNECTED, PREMIUM_ONLY, NONE }

@Composable
fun ProtectedProfilePhoto(
    photoUrl: String?,
    currentUser: UserEntity,
    profileOwner: UserEntity,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val visibility = PhotoVisibility.valueOf(profileOwner.photoPrivacy ?: "EVERYONE")
    
    val shouldBlur = when (visibility) {
        PhotoVisibility.EVERYONE -> false
        PhotoVisibility.CONNECTED -> !isConnected
        PhotoVisibility.PREMIUM_ONLY -> !currentUser.isPremium && !isConnected
        PhotoVisibility.NONE -> true
    }
    
    AsyncImage(
        model = photoUrl,
        modifier = modifier.then(
            if (shouldBlur) Modifier.blur(20.dp) else Modifier
        ),
        contentDescription = if (shouldBlur) "Photo hidden" else "Profile photo"
    )
    
    if (shouldBlur) {
        // Overlay with lock icon and explanation
        Box(modifier.background(Color.Black.copy(alpha = 0.3f))) {
            Icon(Icons.Default.Lock, "Photo locked")
            Text("Connect to view photos")
        }
    }
}
```

### Stealth Mode Enforcement

```kotlin
// In FirestorePagingSource — exclude stealth users from browse
fun discoverProfiles(...): Query {
    var query = usersRef
        .whereEqualTo("gender", targetGender)
        .whereEqualTo("stealthMode", false) // Only show non-stealth
        // ... other filters
    return query
}
```

**Stealth mode rules:**
- Profile NOT shown in browse/search results
- Profile CAN appear in daily matches (Cloud Function still includes them)
- Profile CAN be found via Profile ID search (TLG-XXXXX)
- User CAN still send interests to others
- Available to PREMIUM+ subscribers only

### Biometric Authentication

```kotlin
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun canAuthenticate(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    fun authenticate(
        activity: FragmentActivity,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify Identity")
            .setSubtitle(reason)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            })
        
        prompt.authenticate(promptInfo)
    }
}
```

**Screens requiring biometric (optional, user-configurable):**
- App open (if enabled in settings)
- Contact number reveal
- Payment/subscription purchase
- Account deletion

### Dependencies to Add

```kotlin
// app/build.gradle.kts
implementation("com.scottyab:rootbeer-lib:0.1.0")
implementation("androidx.biometric:biometric:1.2.0-alpha05")
```

---

## DEFINITION OF DONE

- [ ] FLAG_SECURE active in release builds (no screenshots possible)
- [ ] Root detection warns user and disables sensitive features
- [ ] Certificate pinning configured for Firebase + Razorpay
- [ ] Photos blur for non-connected users (based on owner's privacy setting)
- [ ] Stealth mode users excluded from browse queries
- [ ] Biometric prompt available for contact reveal
- [ ] Privacy Dashboard toggles actually enforce behavior
- [ ] `showLastActive` = false → hide last active timestamp from other users
- [ ] `showHoroscope` = false → hide kundali data from other users
- [ ] network_security_config.xml referenced in AndroidManifest
- [ ] BUILD SUCCESSFUL
