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

    gitignore = text(ROOT.parent / ".gitignore") if (ROOT.parent / ".gitignore").exists() else ""
    local_gitignore = text(ROOT / ".gitignore") if (ROOT / ".gitignore").exists() else ""
    ignore = gitignore + "\n" + local_gitignore
    require("keystore.properties" in ignore, "keystore.properties must be ignored", failures)

    # Legacy one-shot legal copy must not return. Consent is purpose-specific, versioned and
    # backend-owned; hard-coded infrastructure/legal claims in an Android dialog drift silently.
    legacy_consent = APP / "src/main/java/com/match/app/ui/consent/DpdpConsentDialog.kt"
    require(not legacy_consent.exists(),
            "legacy hard-coded DPDP consent dialog must not ship", failures)

    nri_screen = APP / "src/main/java/com/match/app/ui/nri/NRIMatchScreen.kt"
    if nri_screen.exists():
        nri = text(nri_screen)
        for forbidden_copy in [
            "NRIProfile(",
            "profiles: Int",
            "41K+",
            "verified NRI profiles across",
            "Featured NRI Profiles",
        ]:
            require(forbidden_copy not in nri,
                    f"NRI screen contains synthetic/demo inventory copy: {forbidden_copy}", failures)

    remote_config = APP / "src/main/java/com/match/app/core/config/RemoteConfigManager.kt"
    if remote_config.exists():
        rc = text(remote_config)
        require("KEY_ENABLE_NRI_FEATURES to false" in rc,
                "NRI route must remain fail-closed by default", failures)

    family_screen = APP / "src/main/java/com/match/app/ui/family/FamilyScreen.kt"
    if family_screen.exists():
        family = text(family_screen)
        for unsaved_field in ["familyIncome", "propertyDetails", "brothersMarried", "sistersMarried"]:
            require(unsaved_field not in family,
                    f"family UI must not show an unpersisted field: {unsaved_field}", failures)

    consent_backend = ROOT / "functions/src/consent.ts"
    recommendation_backend = ROOT / "functions/src/recommendationFeedback.ts"
    if consent_backend.exists() and recommendation_backend.exists():
        require("onPersonalizationConsentChanged" in text(consent_backend),
                "personalization withdrawal must purge optional recommendation telemetry", failures)
        require('hasActiveConsent(viewerUid, "personalization")' in text(recommendation_backend),
                "recommendation telemetry must honor current personalization consent", failures)

    storage_rules = ROOT / "storage.rules"
    if storage_rules.exists():
        sr = text(storage_rules)
        require("peerCanReadPublishedMedia" in sr,
                "profile photo/video peers must be limited to backend-published media", failures)
        require("request.resource.metadata.keys().hasOnly(['ownerUid'])" in sr,
                "member profile-media uploads must not self-assign publication metadata", failures)

    media_backend = ROOT / "functions/src/media.ts"
    if media_backend.exists():
        media = text(media_backend)
        require('requireActiveConsent(uid, "media_processing")' in media,
                "profile-media moderation must require current media-processing consent", failures)
        require("submitProfileVideo" in media and "reviewProfileVideo" in media,
                "video profiles must use trusted submit/review backend authority", failures)
        require("setPublishedMetadata" in media,
                "profile media must receive trusted publication metadata", failures)

    profile_service = APP / "src/main/java/com/match/app/data/remote/FirestoreProfileService.kt"
    if profile_service.exists():
        ps = text(profile_service)
        require('remove("photoUrl")' in ps and 'remove("videoUrl")' in ps,
                "generic client profile sync must exclude moderated media pointers", failures)

    photo_editor = APP / "src/main/java/com/match/app/ui/photoeditor/PhotoEditorScreen.kt"
    if photo_editor.exists():
        editor = text(photo_editor)
        for fake_control in ["FilterPreset", "AdjustmentSlider(", "watermark by remember"]:
            require(fake_control not in editor,
                    f"photo UI contains a preview-only editing control: {fake_control}", failures)

    video_screen = APP / "src/main/java/com/match/app/ui/videoprofile/VideoProfileScreen.kt"
    if video_screen.exists():
        video = text(video_screen)
        require('getHttpsCallable("submitProfileVideo")' in video,
                "video profile upload must submit through trusted moderation", failures)
        require('mapOf("videoUrl" to' not in video,
                "video profile must not publish videoUrl from Android", failures)

    # Production builds must not regain demo-account/password authority. Firebase Auth is the
    # only password authority and synthetic users/interactions belong in test/debug fixtures only.
    require(not (APP / "src/main/java/com/match/app/data/seed").exists(),
            "demo/seed account source must not ship in the production source set", failures)
    require(not (APP / "src/main/java/com/match/app/core/security/Passwords.kt").exists(),
            "device-local password hashing authority must not ship in production", failures)
    require("libs.jbcrypt" not in gradle,
            "obsolete BCrypt dependency must not ship in production", failures)

    for relative in [
        "src/main/java/com/match/app/core/trust/TrustScoreEngine.kt",
        "src/main/java/com/match/app/core/security/FakeProfileDetector.kt",
    ]:
        candidate = APP / relative
        if candidate.exists():
            require("isPremium" not in text(candidate),
                    f"{relative}: payment/premium state must not affect trust or fraud heuristics",
                    failures)

    prod_roots = [APP / "src/main", ROOT / "functions/src"]
    forbidden = [
        ("emulator endpoint", re.compile(r"\b10\.0\.2\.2\b")),
        ("localhost endpoint", re.compile(r"https?://(?:localhost|127\.0\.0\.1)\b", re.I)),
        ("hard-coded OTP bypass", re.compile(r"(?:otp|verification).{0,40}(?:bypass|hardcoded)", re.I)),
        ("fake payment success", re.compile(r"fake.{0,20}(?:payment|purchase).{0,20}success", re.I)),
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

    if failures:
        print("Release candidate repository scan FAILED")
        for item in failures:
            print(f"- {item}")
        return 1
    print("Release candidate repository scan passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
