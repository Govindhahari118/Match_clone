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

    remote_config = text(APP / "src/main/java/com/match/app/core/config/RemoteConfigManager.kt")
    for safe_off in ["KEY_ENABLE_NEARBY", "KEY_ENABLE_KUNDALI", "KEY_ENABLE_NRI_FEATURES",
                     "KEY_SHOW_VIDEO_PROFILES", "KEY_ENABLE_VOICE_CALLS"]:
        require(re.search(rf"{safe_off}\\s+to\\s+false", remote_config) is not None,
                f"{safe_off} must remain false by default for release builds", failures)

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

    nri_screen = APP / "src/main/java/com/match/app/ui/nri/NRIMatchScreen.kt"
    if nri_screen.exists():
        nri = text(nri_screen)
        for marker in ["NRIProfile(", "NRICountry(", '"41K+"', '"92%"']:
            require(marker not in nri,
                    f"NRI screen must not contain synthetic member inventory marker: {marker}",
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
