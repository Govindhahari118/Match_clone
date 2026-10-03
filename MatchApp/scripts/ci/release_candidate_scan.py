#!/usr/bin/env python3
"""Fail closed on repository-controlled Android release configuration regressions."""
from __future__ import annotations
import argparse
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[2]
APP = ROOT / "app"
ANDROID = "{http://schemas.android.com/apk/res/android}"

# Dependency-owned exported services that Android/Google libraries intentionally expose only
# behind signature/system permissions. Keep this allowlist exact and fail closed on any drift.
ALLOWED_EXPORTED_COMPONENTS = {
    "com.google.android.gms.auth.api.signin.RevocationBoundService":
        "com.google.android.gms.auth.api.signin.permission.REVOCATION_NOTIFICATION",
    "androidx.work.impl.background.systemjob.SystemJobService":
        "android.permission.BIND_JOB_SERVICE",
}

def require(ok: bool, message: str, failures: list[str]) -> None:
    if not ok:
        failures.append(message)

def text(path: pathlib.Path) -> str:
    return path.read_text(encoding="utf-8")

def manifest_checks(path: pathlib.Path, failures: list[str]) -> None:
    root = ET.parse(path).getroot()
    perms = {x.get(ANDROID + "name") for x in root.findall("uses-permission")}
    require("android.permission.ACCESS_BACKGROUND_LOCATION" not in perms,
            "release must not request background location", failures)
    app = root.find("application")
    require(app is not None, "application element missing", failures)
    if app is None:
        return
    require(app.get(ANDROID + "allowBackup") == "false",
            "android:allowBackup must be false", failures)
    require(app.get(ANDROID + "usesCleartextTraffic") == "false",
            "android:usesCleartextTraffic must be false", failures)
    for node in list(app.findall("service")) + list(app.findall("provider")):
        name = node.get(ANDROID + "name", "<unnamed>")
        exported = node.get(ANDROID + "exported") == "true"
        if not exported:
            continue
        required_permission = ALLOWED_EXPORTED_COMPONENTS.get(name)
        if required_permission is not None:
            require(
                node.get(ANDROID + "permission") == required_permission,
                f"allowed exported component permission changed: {name}",
                failures,
            )
            continue
        failures.append(f"service/provider unexpectedly exported: {name}")

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--merged-manifest", type=pathlib.Path)
    args = parser.parse_args()
    failures: list[str] = []

    gradle = text(APP / "build.gradle.kts")
    require(re.search(r'compileSdk\s*=\s*36\b', gradle) is not None, "compileSdk must be 36", failures)
    require(re.search(r'targetSdk\s*=\s*36\b', gradle) is not None, "targetSdk must be 36", failures)
    require(re.search(r'minSdk\s*=\s*24\b', gradle) is not None, "minimum supported API contract changed", failures)
    release = gradle.split("release {", 1)[1].split("\n        }", 1)[0] if "release {" in gradle else ""
    require("isMinifyEnabled = true" in release, "release minification must remain enabled", failures)
    require("isShrinkResources = true" in release, "release resource shrinking must remain enabled", failures)

    manifest_checks(APP / "src/main/AndroidManifest.xml", failures)
    if args.merged_manifest:
        require(args.merged_manifest.exists(), f"merged release manifest missing: {args.merged_manifest}", failures)
        if args.merged_manifest.exists():
            manifest_checks(args.merged_manifest, failures)

    network = text(APP / "src/main/res/xml/network_security_config.xml")
    require('cleartextTrafficPermitted="false"' in network, "production network config must deny cleartext", failures)

    release_appcheck = text(APP / "src/release/java/com/match/app/AppCheckProviderInstaller.kt")
    require("PlayIntegrityAppCheckProviderFactory" in release_appcheck,
            "release App Check must use Play Integrity", failures)
    require("DebugAppCheckProviderFactory" not in release_appcheck,
            "debug App Check provider leaked into release source set", failures)

    appearance_model = text(APP / "src/main/java/com/match/app/domain/model/AppearancePreference.kt")
    appearance_resolver = text(APP / "src/main/java/com/match/app/ui/theme/AppearanceThemeResolver.kt")
    settings_screen = text(APP / "src/main/java/com/match/app/ui/settings/SettingsScreen.kt")
    require("ThemePreference.NEUTRAL" in appearance_model and
            'val themePreference: ThemePreference = ThemePreference.NEUTRAL' in appearance_model,
            "appearance must remain Neutral-first for new accounts", failures)
    require('"AUTOMATIC" -> NEUTRAL' in appearance_model,
            "legacy automatic appearance must migrate to Neutral instead of inferring religion consent", failures)
    require("ThemePreference.AUTOMATIC" in appearance_resolver and
            "profileReligion" in appearance_resolver,
            "religion-following appearance must remain an explicit resolver mode", failures)
    require("Appearance changes presentation only; it never changes identity, matching, trust or authorization." in settings_screen,
            "settings must explain that visual theme is independent from matching and identity", failures)

    remote_config = text(APP / "src/main/java/com/match/app/core/config/RemoteConfigManager.kt")
    for flag in [
        "KEY_SHOW_VIDEO_PROFILES",
        "KEY_ENABLE_VOICE_CALLS",
        "KEY_ENABLE_NRI_FEATURES",
        "KEY_ENABLE_AI_ICEBREAKERS",
        "KEY_ENABLE_NEARBY",
        "KEY_ENABLE_KUNDALI",
    ]:
        require(
            re.search(rf"{flag}\s+to\s+false\b", remote_config) is not None,
            f"{flag} must remain fail-closed by default until its production gate passes",
            failures,
        )

    # Abuse/risk counters must have one canonical writer per authoritative action.
    trust_functions = text(ROOT / "functions/src/trust.ts")
    require("onInterestBehaviorRiskSignal" not in trust_functions,
            "legacy duplicate interest risk trigger must not return; interests.ts owns it", failures)
    require("onMessageBehaviorRiskSignal" not in trust_functions,
            "legacy duplicate message risk trigger must not return; messageSafety.ts owns it", failures)

    # Consent must be an explicit user choice, never an implicit repository side effect.
    photo_repo = text(APP / "src/main/java/com/match/app/data/repo/PhotoRepository.kt")
    location_repo = text(APP / "src/main/java/com/match/app/data/repo/LocationRepository.kt")
    partner_repo = text(APP / "src/main/java/com/match/app/data/repo/PartnerPreferenceRepository.kt")
    profile_screen = text(APP / "src/main/java/com/match/app/ui/profile/ProfileScreen.kt")
    nearby_screen = text(APP / "src/main/java/com/match/app/ui/nearby/NearbyMatchesScreen.kt")
    preference_screen = text(APP / "src/main/java/com/match/app/ui/preferences/PartnerPreferencesScreen.kt")
    require('consentRepository.set("media_processing", true)' not in photo_repo,
            "profile photo repository must not auto-grant media-processing consent", failures)
    require('consentRepository.set("location", true)' not in location_repo,
            "location repository must not auto-grant location consent", failures)
    require('consentRepository.set("sensitive_preferences", true)' not in partner_repo,
            "partner preference repository must not auto-grant sensitive-preference consent", failures)
    require('"profile_media_consent"' in profile_screen,
            "profile photo flow must expose an explicit media-processing consent control", failures)
    require('"nearby_location_consent"' in nearby_screen,
            "Nearby must expose an explicit location-processing consent control", failures)
    require('"Sensitive preference processing"' in preference_screen,
            "partner preferences must expose explicit sensitive-processing consent", failures)

    preference_policy = text(ROOT / "functions/src/partnerPreferencesPolicy.ts")
    preference_callable = text(ROOT / "functions/src/partnerPreferences.ts")
    require("schemaVersion: 5" in preference_callable,
            "partner-preference schema must remain v5 with opt-in public-summary privacy", failures)
    for preference_field in [
        "gothraMode",
        "faithTraditionMode",
        "faithSubTraditionMode",
        "faithInstitutionMode",
        "nativeStateMode",
        "educationFieldMode",
        "employerTypeMode",
        "familyStatusMode",
        "visaStatusMode",
        "weightMode",
        "incomeBandMode",
        "complexionMode",
        "sharePublicSummary",
    ]:
        require(preference_field in preference_policy,
                f"backend partner-preference contract missing {preference_field}", failures)
        require(preference_field in partner_repo,
                f"Android partner-preference contract missing {preference_field}", failures)
        require(preference_field in preference_screen,
                f"partner-preference UI missing {preference_field}", failures)
    require("partner-preferences-v4-reciprocal-min-evidence" in preference_policy,
            "reciprocal preference scoring must remain versioned and weaker-side bounded", failures)
    require("Math.min(forward, reverse)" in preference_policy,
            "mutual preference score must be limited by the weaker direction", failures)
    require("STRONG_MUTUAL_MIN_CRITERIA = 5" in preference_policy,
            "strong mutual-match claims must require a minimum evidence count", failures)

    discovery_functions = text(ROOT / "functions/src/discovery.ts")
    require('"minMutualMatchPercent"' in discovery_functions and
            "mutualPreferenceFit" in discovery_functions,
            "discovery must support server-authoritative strong mutual-match thresholds", failures)

    location_functions = text(ROOT / "functions/src/location.ts")
    horoscope_functions = text(ROOT / "functions/src/horoscope.ts")
    require(location_functions.count('requireProductionFeature("nearby")') >= 2,
            "Nearby backend update/discovery callables must enforce the rollout gate", failures)
    require('requireProductionFeature("kundali")' in horoscope_functions,
            "Kundali backend callable must enforce the rollout gate", failures)

    media_functions = text(ROOT / "functions/src/media.ts")
    video_submit = media_functions.split("export const submitProfileVideo", 1)[1].split(
        "export const listPendingVideoModeration", 1
    )[0]
    video_review = media_functions.split("export const reviewProfileVideo", 1)[1].split(
        "export const removeProfileVideo", 1
    )[0]
    photo_review = media_functions.split("export const reviewProfilePhoto", 1)[1].split(
        "export const setPrimaryApprovedPhoto", 1
    )[0]
    require('requireProductionFeature("video_profiles")' in video_submit,
            "Video profile submission must enforce the backend rollout gate", failures)
    require('requireProductionFeature("video_profiles")' in video_review,
            "Video profile approval must enforce the backend rollout gate", failures)
    require('requireProductionFeature("video_profiles")' not in photo_review,
            "Video rollout gating must never block ordinary profile-photo moderation", failures)
    require("onProfileVideoUploaded" in media_functions and
            "productionFeatureEnabled" in media_functions,
            "Video profile Storage uploads must be purged while the backend feature is disabled", failures)
    require("cleanupAbandonedProfileVideos" in media_functions and
            "profileVideoOrphans" in media_functions,
            "Unsubmitted profile-video uploads must have bounded orphan cleanup", failures)
    photo_submit = media_functions.split("export const submitProfilePhoto", 1)[1].split(
        "export const listPendingPhotoModeration", 1
    )[0]
    require("profileVideoOrphans" not in photo_submit,
            "Video orphan cleanup must not be wired into profile-photo submission", failures)
    require("profileVideoOrphans" in video_submit,
            "Video submission must clear its unregistered-upload orphan record", failures)


    play_billing = text(ROOT / "functions/src/playBilling.ts")
    interests_functions = text(ROOT / "functions/src/interests.ts")
    privacy_functions = text(ROOT / "functions/src/privacy.ts")
    discovery_functions = text(ROOT / "functions/src/discovery.ts")
    users_functions = text(ROOT / "functions/src/users.ts")
    require('membershipActive:' in play_billing and 'getMyMembershipStatus' in play_billing,
            "Play billing must persist and expose private membership authority", failures)
    require('resolveMembershipState' in interests_functions,
            "interest quota authorization must resolve private membership state", failures)
    require('resolveMembershipState' in privacy_functions,
            "contact reveal authorization must resolve private membership state", failures)
    require('resolveMembershipState' in discovery_functions,
            "discovery premium badge/filter must resolve private membership state", failures)
    require('subscriptionPlan: "FREE"' not in users_functions,
            "new user creation must not seed public billing metadata", failures)

    require("profileVideoOrphans" in users_functions,
            "account deletion must remove profile-video orphan records", failures)
    require('chatMediaOrphans").where("senderUid", "==", uid)' in users_functions and
            'chatMediaOrphans").where("recipientUid", "==", uid)' in users_functions,
            "account deletion must erase chat-media orphan ownership in both directions", failures)
    require("removeUidFromBioFingerprints(uid)" in users_functions,
            "account deletion must remove copied-bio fingerprint ownership deterministically", failures)


    verification_functions = text(ROOT / "functions/src/verification.ts")
    verification_screen = text(
        APP / "src/main/java/com/match/app/ui/verification/VerificationScreen.kt"
    )
    storage_rules = text(ROOT / "storage.rules")
    require("const AADHAAR_OFFLINE_PROVIDER_IMPLEMENTED = false" in verification_functions,
            "Aadhaar must remain unavailable until a registered offline-verification adapter exists", failures)
    require("const SELFIE_PROVIDER_IMPLEMENTED = false" in verification_functions and
            "const LIVENESS_PROVIDER_IMPLEMENTED = false" in verification_functions and
            "const FACE_SIMILARITY_PROVIDER_IMPLEMENTED = false" in verification_functions,
            "biometric verification claims must remain unavailable until validated provider adapters exist", failures)
    require('docType.toLowerCase().includes("aadhaar")' in verification_functions,
            "generic government-ID submission must explicitly reject Aadhaar", failures)
    require("Aadhaar is not accepted as a generic card/image upload" in verification_screen,
            "verification UI must truthfully explain that Aadhaar is unavailable", failures)
    require("'Aadhaar" not in storage_rules and '"Aadhaar' not in storage_rules,
            "Storage rules must not accept Aadhaar as a generic KYC document type", failures)

    gitignore = text(ROOT.parent / ".gitignore") if (ROOT.parent / ".gitignore").exists() else ""
    local_gitignore = text(ROOT / ".gitignore") if (ROOT / ".gitignore").exists() else ""
    ignore = gitignore + "\n" + local_gitignore
    require("keystore.properties" in ignore, "keystore.properties must be ignored", failures)

    # Production builds must not regain demo-account/password authority. Firebase Auth is the
    # only password authority and synthetic users/interactions belong in test/debug fixtures only.
    require(not (APP / "src/main/java/com/match/app/data/seed").exists(),
            "demo/seed account source must not ship in the production source set", failures)
    require(not (APP / "src/main/java/com/match/app/core/security/Passwords.kt").exists(),
            "device-local password hashing authority must not ship in production", failures)
    require("libs.jbcrypt" not in gradle,
            "obsolete BCrypt dependency must not ship in production", failures)

    # The production app is Firebase/Firestore + Google Play Billing authoritative. The retired
    # WEB_MATCH/Razorpay REST transport and its HTTP client stack must not silently return.
    require(not (APP / "src/main/java/com/match/app/data/remote/Dtos.kt").exists(),
            "retired WEB_MATCH/Razorpay DTO surface must not ship in production", failures)
    for retired_dependency in [
        "libs.retrofit",
        "libs.okhttp",
        "libs.okhttp.logging",
        "libs.retrofit.kotlinx.serialization.converter",
    ]:
        require(retired_dependency not in gradle,
                f"retired REST dependency must not return: {retired_dependency}", failures)

    retired_transport_patterns = [
        ("Razorpay transport", re.compile(r"\\brazorpay(?:OrderId|PaymentId|Signature)?\\b", re.I)),
        ("WEB_MATCH backend", re.compile(r"\\bWEB_MATCH\\b")),
        ("legacy REST regions API", re.compile(r"/api/meta/regions", re.I)),
        ("legacy REST matches API", re.compile(r"/api/matches", re.I)),
    ]
    for base in [APP / "src/main", ROOT / "functions/src"]:
        if not base.exists():
            continue
        for source in base.rglob("*"):
            if not source.is_file() or source.suffix.lower() not in {
                ".kt", ".java", ".ts", ".js", ".xml", ".json", ".kts"
            }:
                continue
            source_text = text(source)
            for label, pattern in retired_transport_patterns:
                require(pattern.search(source_text) is None,
                        f"{source.relative_to(ROOT)}: retired {label} reference", failures)

    # Family-assisted profiles are a first-class matrimony contract, not a UI-only label.
    user_entity = text(APP / "src/main/java/com/match/app/data/local/entity/UserEntity.kt")
    profile_service = text(APP / "src/main/java/com/match/app/data/remote/FirestoreProfileService.kt")
    profile_wizard = text(APP / "src/main/java/com/match/app/ui/onboarding/ProfileWizardViewModel.kt")
    profile_wizard_screen = text(APP / "src/main/java/com/match/app/ui/onboarding/ProfileWizardScreen.kt")
    firestore_rules = text(ROOT / "firestore.rules")
    require('val profileCreatedFor: String = "SELF"' in user_entity,
            "Room profile-created-for contract missing", failures)
    require('"profileCreatedFor" to e.profileCreatedFor' in profile_service and
            'data["profileCreatedFor"] as? String ?: "SELF"' in profile_service,
            "Firestore profile-created-for persistence/hydration missing", failures)
    require("PROFILE_CREATED_FOR_VALUES" in profile_wizard and
            '"Profile created for"' in profile_wizard_screen,
            "profile-created-for onboarding contract missing", failures)
    profile_domain = text(APP / "src/main/java/com/match/app/domain/model/Models.kt")
    match_detail = text(APP / "src/main/java/com/match/app/ui/detail/MatchDetailScreen.kt")
    require("enum class ProfileCreatedFor" in profile_domain and
            "ProfileCreatedFor.fromWire(profileCreatedFor)" in
                text(APP / "src/main/java/com/match/app/data/repo/AuthRepository.kt"),
            "profile-created-for typed domain mapping missing", failures)
    require('Fact("Profile managed as", p.profileCreatedFor.displayLabel)' in match_detail,
            "family-assisted profile status must remain visible to prospective matches", failures)
    require("validProfileCreatedFor" in firestore_rules,
            "Firestore must validate profile-created-for canonical values", failures)

    # Analytics must never regain member identifiers or sensitive payload fields. Typed telemetry
    # has its own unit contract; this static gate protects retained legacy compatibility facades.
    analytics_sources = [
        APP / "src/main/java/com/match/app/core/analytics/AnalyticsManager.kt",
        APP / "src/main/java/com/match/app/util/AnalyticsTracker.kt",
        APP / "src/main/java/com/match/app/core/telemetry/MatreeTelemetry.kt",
    ]
    forbidden_analytics_keys = [
        "viewed_uid", "target_uid", "user_id", "firebase_uid", "phone", "email",
        "message_body", "purchase_token", "latitude", "longitude", "exact_location",
        "document_number", "aadhaar_number", "date_of_birth",
    ]
    for analytics_source in analytics_sources:
        if not analytics_source.exists():
            continue
        analytics_text = text(analytics_source).lower()
        for forbidden_key in forbidden_analytics_keys:
            require(
                f'"{forbidden_key}"' not in analytics_text,
                f"{analytics_source.relative_to(ROOT)}: analytics privacy boundary exposes {forbidden_key}",
                failures,
            )

    for relative in [
        "src/main/java/com/match/app/core/trust/TrustScoreEngine.kt",
        "src/main/java/com/match/app/core/security/FakeProfileDetector.kt",
    ]:
        candidate = APP / relative
        if candidate.exists():
            require("isPremium" not in text(candidate),
                    f"{relative}: payment/premium state must not affect trust or fraud heuristics",
                    failures)

    legacy_local_trust_files = {
        APP / "src/main/java/com/match/app/core/trust/TrustScoreEngine.kt",
        APP / "src/main/java/com/match/app/core/security/FakeProfileDetector.kt",
    }
    for source in (APP / "src/main/java").rglob("*.kt"):
        if source in legacy_local_trust_files:
            continue
        source_text = text(source)
        require("TrustScoreEngine" not in source_text,
                f"{source.relative_to(ROOT)}: production code must use server TrustRepository, not TrustScoreEngine",
                failures)
        require("FakeProfileDetector" not in source_text,
                f"{source.relative_to(ROOT)}: production code must not use device-local fake-profile authority",
                failures)

    privacy_policy = ROOT / "privacy-policy.html"
    legal_screen = APP / "src/main/java/com/match/app/ui/legal/LegalScreen.kt"
    legal_surfaces = [p for p in [privacy_policy, legal_screen] if p.exists()]
    stale_legal_claims = [
        ("end-to-end encryption claim", re.compile(r"end[- ]to[- ]end encrypted|\\bE2EE\\b", re.I)),
        ("retired Razorpay claim", re.compile(r"\\bRazorpay\\b", re.I)),
        ("retired BCrypt password claim", re.compile(r"\\bBCrypt\\b", re.I)),
        ("invented fixed deletion period", re.compile(r"deleted within 30 days", re.I)),
        ("invented fixed payment retention", re.compile(r"payment records are retained for 7 years", re.I)),
        ("invented session expiry", re.compile(r"sessions expire automatically after 30 days", re.I)),
        ("false no-GPS claim", re.compile(r"not GPS[- ]tracked", re.I)),
    ]
    for legal_surface in legal_surfaces:
        data = text(legal_surface)
        for label, pattern in stale_legal_claims:
            require(
                pattern.search(data) is None,
                f"{legal_surface.relative_to(ROOT)}: {label}",
                failures,
            )

    deprecated_integration_patterns = [
        ("Firebase Dynamic Links", re.compile(r"FirebaseDynamicLinks|firebase-dynamic-links|page\\.link", re.I)),
    ]
    deprecated_roots = [APP / "src/main", ROOT / "functions/src"]
    for base in deprecated_roots:
        if not base.exists():
            continue
        for p in base.rglob("*"):
            if not p.is_file() or p.suffix.lower() not in {".kt", ".java", ".ts", ".js", ".xml", ".json", ".kts"}:
                continue
            data = text(p)
            for label, pattern in deprecated_integration_patterns:
                require(pattern.search(data) is None, f"{p.relative_to(ROOT)}: retired {label} reference", failures)

    app_main_source = APP / "src/main"
    if app_main_source.exists():
        for source in app_main_source.rglob("*.kt"):
            data = text(source)
            require("passwordHash" not in data,
                    f"{source.relative_to(ROOT)}: retired local password field reference returned", failures)
            require("isSeed" not in data,
                    f"{source.relative_to(ROOT)}: retired seed-authority field reference returned", failures)

    prod_roots = [APP / "src/main", ROOT / "functions/src"]
    forbidden = [
        ("emulator endpoint", re.compile(r"\b10\.0\.2\.2\b")),
        ("localhost endpoint", re.compile(r"https?://(?:localhost|127\.0\.0\.1)\b", re.I)),
        ("hard-coded OTP bypass", re.compile(r"(?:otp|verification).{0,40}(?:bypass|hardcoded)", re.I)),
        ("fake payment success", re.compile(r"fake.{0,20}(?:payment|purchase).{0,20}success", re.I)),
        ("unverified Firebase residency claim", re.compile(r"Firebase servers \(US/Mumbai\)", re.I)),
        ("unverified fixed grievance SLA", re.compile(r"Response within 72 hours", re.I)),
        ("hard-coded grievance mailbox", re.compile(r"grievance@matrimonyconnect\.app", re.I)),
    ]
    for base in prod_roots:
        if not base.exists():
            continue
        for p in base.rglob("*"):
            if not p.is_file() or p.suffix.lower() not in {".kt", ".java", ".ts", ".js", ".xml", ".json"}:
                continue
            data = text(p)
            for label, pattern in forbidden:
                if pattern.search(data):
                    failures.append(f"{p.relative_to(ROOT)}: {label}")

    # Hidden/prototype source must also remain truthful. A future navigation change must not
    # resurrect phantom provider callables or bundled fake member/event inventory.
    app_source = APP / "src/main/java"
    obsolete_callable_names = [
        "requestBackgroundCheckByProfileId",
        "requestBackgroundCheck",
        "requestSecureCall",
        "respondToSecureCall",
        "bookCounselling",
        "getRewardsState",
        "claimDailyReward",
        "redeemReward",
        "registerForEvent",
        "joinCommunity",
    ]
    if app_source.exists():
        for source in app_source.rglob("*.kt"):
            data = text(source)
            for callable_name in obsolete_callable_names:
                require(
                    f'getHttpsCallable("{callable_name}")' not in data,
                    f"{source.relative_to(ROOT)}: phantom callable {callable_name} must not ship",
                    failures,
                )

    prototype_markers = [
        (
            APP / "src/main/java/com/match/app/ui/events/LiveEventsScreen.kt",
            ["UPCOMING_EVENTS = listOf(", "attendees:", "maxAttendees:"],
        ),
        (
            APP / "src/main/java/com/match/app/ui/circles/CirclesScreen.kt",
            ["BUNDLED_CIRCLES", "100% phone-verified", "Premium Members"],
        ),
        (
            APP / "src/main/java/com/match/app/ui/regions/RegionsScreen.kt",
            ["FALLBACK_PRESETS"],
        ),
        (
            APP / "src/main/java/com/match/app/ui/timeline/RelationshipTimelineScreen.kt",
            ["First Call", "First In-Person Meet", "15 Mar 2026"],
        ),
        (
            APP / "src/main/java/com/match/app/ui/biogen/BioGeneratorScreen.kt",
            ['mutableStateOf("Priya")', "AI Bio Generator"],
        ),
    ]
    for source, markers in prototype_markers:
        if not source.exists():
            continue
        data = text(source)
        for marker in markers:
            require(
                marker not in data,
                f"{source.relative_to(ROOT)}: fabricated/prototype marker must not return: {marker}",
                failures,
            )

    nri_screen = APP / "src/main/java/com/match/app/ui/nri/NRIMatchScreen.kt"
    if nri_screen.exists():
        nri = text(nri_screen)
        for marker in ["NRIProfile(", "NRICountry(", '"41K+"', '"92%"']:
            require(marker not in nri,
                    f"NRI screen must not contain synthetic member inventory marker: {marker}",
                    failures)

    swipe_screen = APP / "src/main/java/com/match/app/ui/discovery/SwipeDiscoveryScreen.kt"
    if swipe_screen.exists():
        swipe = text(swipe_screen)
        require("social.sendInterest(" in swipe,
                "Swipe Discovery must persist interests through server-authoritative social flow", failures)
        require("userDao.allExcluding" not in swipe,
                "Swipe Discovery must not use local Room inventory as discovery authority", failures)
        require("matchOverlayProfile = profile" not in swipe,
                "Swipe Discovery must not fabricate a mutual-match celebration on local like", failures)

    deep_compat = APP / "src/main/java/com/match/app/ui/deepcompat/CompatibilityDeepDiveViewModel.kt"
    if deep_compat.exists():
        compat = text(deep_compat)
        require("MatchingRepository" in compat,
                "compatibility deep dive must use the production matching repository", failures)
        require("MatchScoreEngine" not in compat,
                "compatibility deep dive must not restore the legacy split-brain scorer", failures)

    recently_joined = APP / "src/main/java/com/match/app/ui/recentlyjoined/RecentlyJoinedScreen.kt"
    if recently_joined.exists():
        recent = text(recently_joined)
        require("recentlyJoinedDays = 30" in recent,
                "Recently Joined must use server-authoritative creation-time filtering", failures)
        require("userDao.allExcluding" not in recent,
                "Recently Joined must not derive chronology from local cache ordering", failures)

    verification_backend = ROOT / "functions/src/verification.ts"
    verification_policy = ROOT / "functions/src/verificationLevelPolicy.ts"
    ops_console = ROOT / "ops-console/app.js"
    if verification_backend.exists() and verification_policy.exists():
        verification = text(verification_backend)
        policy = text(verification_policy)
        require("GENERIC_GOVERNMENT_ID_LEVEL = 2" in policy,
                "generic government-ID evidence must remain Level 2 only", failures)
        require("verificationLevel: evidenceLevel" in verification,
                "verification approval must derive level from backend evidence", failures)
        require("newLevel >= 2" not in verification and "newLevel > 5" not in verification,
                "generic ID review must not restore reviewer-selected higher verification levels",
                failures)
    if ops_console.exists():
        ops = text(ops_console)
        require('select(["2", "3", "4", "5"]' not in ops,
                "ops console must not offer manual verification-level escalation",
                failures)

    calls_backend = ROOT / "functions/src/calls.ts"
    if calls_backend.exists():
        calls = text(calls_backend)
        provider_contract = (ROOT / "functions/src/communicationProvider.ts").read_text(encoding="utf-8")
        require("class DisabledCommunicationProvider" in provider_contract and
                "new DisabledCommunicationProvider()" in provider_contract and
                "readonly ready = false" in provider_contract,
                "secure calls must remain fail-closed until an audited provider adapter replaces the disabled default",
                failures)
        require("productionFeatureEnabled(functions.config().features?.secure_calls)" in calls and
                "communicationProvider.ready" in calls,
                "secure calls must require both provider readiness and an independent backend rollout flag",
                failures)

    family_screen = APP / "src/main/java/com/match/app/ui/family/FamilyScreen.kt"
    if family_screen.exists():
        family = text(family_screen)
        for marker in [
            'familyType: String = "Nuclear"',
            'familyStatus: String = "Middle Class"',
            'familyValues: String = "Moderate"',
            'ifBlank { "Nuclear" }',
            'ifBlank { "Middle Class" }',
            'ifBlank { "Moderate" }',
        ]:
            require(marker not in family,
                    f"family profile must not fabricate an unspecified attribute: {marker}",
                    failures)

    photo_editor = APP / "src/main/java/com/match/app/ui/photoeditor/PhotoEditorScreen.kt"
    if photo_editor.exists():
        photo = text(photo_editor)
        for marker in ["FilterPreset", "AdjustmentSlider(", "CropAspect"]:
            require(marker not in photo,
                    f"profile photo UI must not advertise unapplied edit control: {marker}",
                    failures)
        require("submitted for moderation" in photo.lower(),
                "profile photo upload must disclose moderation-before-publication", failures)

    if failures:
        print("Release candidate repository scan FAILED")
        for item in failures:
            print(f"- {item}")
        return 1
    print("Release candidate repository scan passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
