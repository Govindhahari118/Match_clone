#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCAN_ROOTS = [ROOT / "app" / "src" / "main", ROOT / "functions" / "src"]
ANDROID_TEST_ROOT = ROOT / "app" / "src" / "androidTest"
EXTENSIONS = {".kt", ".kts", ".java", ".ts", ".js", ".xml", ".json"}

STUB_RULES = [
    ("Kotlin TODO executable stub", re.compile(r"\bTODO\s*\(")),
    ("NotImplementedError executable stub", re.compile(r"\bNotImplementedError\b")),
    ("UnsupportedOperationException executable stub", re.compile(r"\bUnsupportedOperationException\b")),
    ("Firebase fake-key bypass", re.compile(r"fake API keys|Bypassing Firebase", re.IGNORECASE)),
]

STALE_RELEASE_TEST_RULES = [
    ("demo authentication in production-gate instrumentation", re.compile(r"signInAsDemo|demo[_ -]?user", re.IGNORECASE)),
    ("stale hidden route positively navigated by production-gate instrumentation", re.compile(
        r"(?:openDrawerAndNavigate|waitFor)\(\s*[\"'](?:regions|circles|stories|family)[\"']"
    )),
]

I18N_LEGACY_RULES = [
    ("stale prototype brand", re.compile(r"GreenKart|MobileMart|MobileVerify|\bMHub\b")),
    ("corrupted UTF-8/mojibake", re.compile(r"â‚¹|â€”|â†[‘’“”]|Â©|Ã°|Å¸|Â")),
]

# These surfaces are explicitly HIDDEN in the production route inventory. Their source files may
# remain for future audited promotion, but the production shell/deep-link resolver must not expose
# them accidentally. Secure Call is intentionally not listed here: its reachable screen is a
# server-backed capability/readiness surface only; release_candidate_scan.py separately enforces
# that live provider calling remains fail-closed.
PRODUCTION_ROUTE_SURFACES = {
    ROOT / "app" / "src" / "main" / "java" / "com" / "match" / "app" / "ui" / "main" / "MainShell.kt",
    ROOT / "app" / "src" / "main" / "java" / "com" / "match" / "app" / "navigation" / "DeepLinkRouteResolver.kt",
}
HIDDEN_ROUTE_IDENTIFIERS = [
    "BackgroundCheck", "BioGenerator", "BoostScreen", "Circles", "CommunityBrowse",
    "Counselling", "SwipeDiscovery", "EventsScreen", "FamilyPortal", "GuidesScreen",
    "AdvancedHoroscope", "LikesScreen", "VirtualMeet", "Muhurat", "PhotoEditor",
    "Referral", "Regions", "DailyRewards", "SecondMarriage",
    "SuccessStories", "Testimonials", "RelationshipTimeline", "WeddingPlanner",
]

# Reachable v1 surfaces must consume Material/Matree semantic colors. Literal UI colors would bypass
# religion-aware/Neutral families and can produce incorrect dark-mode contrast. Theme definitions and
# debug previews intentionally own raw palette values and are not included here.
_REACHABLE_UI_RELATIVE = [
    "ui/MatchRoot.kt",
    "ui/auth/SignInScreen.kt", "ui/auth/SignUpScreen.kt",
    "ui/onboarding/OnboardingScreen.kt", "ui/onboarding/ProfileWizardScreen.kt",
    "ui/main/MainShell.kt", "ui/main/HomeLauncherScreen.kt",
    "ui/main/ReligionHomeHero.kt",
    "ui/analytics/ProfileAnalyticsScreen.kt", "ui/aiinsights/AIMatchInsightsScreen.kt",
    "ui/assisted/AssistedServiceScreen.kt", "ui/deepcompat/CompatibilityDeepDiveScreen.kt",
    "ui/family/FamilyScreen.kt", "ui/family/FamilyAccessScreen.kt",
    "ui/phone/PhoneVerificationScreen.kt", "ui/recentlyjoined/RecentlyJoinedScreen.kt",
    "ui/safety/SafetyCenterScreen.kt", "ui/videoprofile/VideoProfileScreen.kt",
    "ui/components/MatreeComponents.kt", "ui/components/MatreeThemeDecor.kt",
    "ui/components/StateScreens.kt",
    "ui/common/ContactUnlockSheet.kt", "ui/common/PaywallSheet.kt",
    "ui/common/ProfileCompletenessBar.kt", "ui/common/Shimmer.kt", "ui/common/ShimmerCard.kt",
    "ui/matches/MatchesScreen.kt", "ui/nearby/NearbyMatchesScreen.kt",
    "ui/interests/InterestsScreen.kt", "ui/shortlist/ShortlistScreen.kt",
    "ui/chat/ChatListScreen.kt", "ui/chat/ChatScreen.kt",
    "ui/securecall/SecureCallScreen.kt",
    "ui/questionnaire/QuestionnaireScreen.kt",
    "ui/profile/ProfileScreen.kt", "ui/profile/ReligionExperienceCard.kt",
    "ui/detail/MatchDetailScreen.kt", "ui/settings/SettingsScreen.kt",
    "ui/language/LanguageSelectionScreen.kt", "ui/notifications/NotificationsScreen.kt",
    "ui/whoviewed/WhoViewedScreen.kt", "ui/kundli/KundliScreen.kt",
    "ui/pricing/PricingScreen.kt", "ui/verification/VerificationScreen.kt",
    "ui/privacy/PrivacyDashboardScreen.kt", "ui/help/HelpScreen.kt",
    "ui/legal/LegalScreen.kt", "ui/biodata/BiodataScreen.kt",
]
UI_JAVA_ROOT = ROOT / "app" / "src" / "main" / "java" / "com" / "match" / "app"
PRODUCTION_THEME_SURFACES = {UI_JAVA_ROOT / rel for rel in _REACHABLE_UI_RELATIVE}
THEME_LITERAL_COLOR = re.compile(
    r"(?<!android\.graphics\.)\bColor\s*\(\s*0x[0-9A-Fa-f]+|"
    r"\bColor\.(?:White|Black|Red|Green|Blue|Yellow|Gray|DarkGray|LightGray|Magenta|Cyan|Transparent)"
)
ANDROID_GRAPHICS_LITERAL_COLOR = re.compile(
    r"\bandroid\.graphics\.Color\.(?:rgb|argb)\s*\("
)

APPEARANCE_SETTINGS_SURFACE = (
    UI_JAVA_ROOT / "ui" / "settings" / "SettingsScreen.kt"
)
THEME_IDENTITY_SURFACES = {
    APPEARANCE_SETTINGS_SURFACE,
    UI_JAVA_ROOT / "ui" / "theme" / "Palettes.kt",
    UI_JAVA_ROOT / "ui" / "theme" / "MatreeVisualFamily.kt",
}
UNSUPPORTED_APPEARANCE_PALETTE = re.compile(
    r"AppPalette\.(?:ROSE|LAVENDER|SOLAR|OCEAN|MONO|GLACIER|TELUGU|COMMUNITY)\b"
)
STALE_UNIVERSAL_THEME_NAME = re.compile(r"Matree Signature")


def android_test_files():
    if ANDROID_TEST_ROOT.exists():
        for path in ANDROID_TEST_ROOT.rglob("*"):
            if path.is_file() and path.suffix.lower() in {".kt", ".java"}:
                yield path


def source_files():
    for root in SCAN_ROOTS:
        if root.exists():
            for path in root.rglob("*"):
                if path.is_file() and path.suffix.lower() in EXTENSIONS:
                    yield path


def main() -> int:
    findings = []
    for path in source_files():
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
        for line_no, line in enumerate(lines, 1):
            for label, pattern in STUB_RULES:
                if pattern.search(line):
                    findings.append(f"{path.relative_to(ROOT)}:{line_no}: {label}: {line.strip()}")

            if "/assets/i18n/" in path.as_posix():
                for label, pattern in I18N_LEGACY_RULES:
                    if pattern.search(line):
                        findings.append(
                            f"{path.relative_to(ROOT)}:{line_no}: {label}: {line.strip()}"
                        )

            if re.search(r"onClick\s*=\s*\{\s*\}", line):
                nearby = "\n".join(lines[line_no - 1:min(len(lines), line_no + 3)])
                disabled = re.search(r"enabled\s*=\s*false", nearby) is not None
                if not disabled:
                    findings.append(
                        f"{path.relative_to(ROOT)}:{line_no}: interactive empty onClick: {line.strip()}"
                    )

            if path in PRODUCTION_ROUTE_SURFACES:
                for identifier in HIDDEN_ROUTE_IDENTIFIERS:
                    if identifier in line:
                        findings.append(
                            f"{path.relative_to(ROOT)}:{line_no}: hidden production route exposure "
                            f"({identifier}): {line.strip()}"
                        )

            if path in PRODUCTION_THEME_SURFACES and (
                THEME_LITERAL_COLOR.search(line) or ANDROID_GRAPHICS_LITERAL_COLOR.search(line)
            ):
                findings.append(
                    f"{path.relative_to(ROOT)}:{line_no}: hard-coded production UI color "
                    f"bypasses Matree theme: {line.strip()}"
                )

            if path == APPEARANCE_SETTINGS_SURFACE and UNSUPPORTED_APPEARANCE_PALETTE.search(line):
                findings.append(
                    f"{path.relative_to(ROOT)}:{line_no}: unsupported appearance palette exposed "
                    f"to production settings: {line.strip()}"
                )

            if path in THEME_IDENTITY_SURFACES and STALE_UNIVERSAL_THEME_NAME.search(line):
                findings.append(
                    f"{path.relative_to(ROOT)}:{line_no}: stale universal theme name; "
                    f"use Matree Neutral: {line.strip()}"
                )

    for path in android_test_files():
        for line_no, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
            for label, pattern in STALE_RELEASE_TEST_RULES:
                if pattern.search(line):
                    findings.append(
                        f"{path.relative_to(ROOT)}:{line_no}: stale release-test contract ({label}): {line.strip()}"
                    )

    if findings:
        print("Production integrity scan FAILED")
        for finding in findings:
            print(finding)
        return 1

    print("Production integrity scan passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
