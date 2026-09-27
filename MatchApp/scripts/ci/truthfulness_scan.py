#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCAN_ROOTS = [ROOT / "app" / "src" / "main", ROOT / "functions" / "src"]
EXTENSIONS = {".kt", ".kts", ".java", ".ts", ".js", ".xml", ".json"}

STUB_RULES = [
    ("Kotlin TODO executable stub", re.compile(r"\bTODO\s*\(")),
    ("NotImplementedError executable stub", re.compile(r"\bNotImplementedError\b")),
    ("UnsupportedOperationException executable stub", re.compile(r"\bUnsupportedOperationException\b")),
    ("Firebase fake-key bypass", re.compile(r"fake API keys|Bypassing Firebase", re.IGNORECASE)),
]

I18N_LEGACY_RULES = [
    ("stale prototype brand", re.compile(r"GreenKart|MobileMart|MobileVerify|\bMHub\b")),
    ("corrupted UTF-8/mojibake", re.compile(r"â‚¹|â€”|â†[‘’“”]|Â©|Ã°|Å¸|Â")),
]

# These surfaces are explicitly HIDDEN/UNAVAILABLE in the production route inventory. Their source
# files may remain for future audited promotion, but the production shell/deep-link resolver must not
# expose them accidentally.
PRODUCTION_ROUTE_SURFACES = {
    ROOT / "app" / "src" / "main" / "java" / "com" / "match" / "app" / "ui" / "main" / "MainShell.kt",
    ROOT / "app" / "src" / "main" / "java" / "com" / "match" / "app" / "navigation" / "DeepLinkRouteResolver.kt",
}
HIDDEN_ROUTE_IDENTIFIERS = [
    "AIInsights", "ProfileAnalytics", "AssistedMatchmaking", "BackgroundCheck",
    "BioGenerator", "BoostScreen", "Circles", "CommunityBrowse", "Counselling",
    "CompatibilityDeepDive", "SwipeDiscovery", "EventsScreen", "FamilyPortal",
    "GuidesScreen", "AdvancedHoroscope", "LikesScreen", "VirtualMeet", "Muhurat",
    "NriDiscovery", "PhotoEditor", "Referral", "Regions", "DailyRewards",
    "SafetyCenter", "SecondMarriage", "SecureCall", "SuccessStories", "Testimonials",
    "RelationshipTimeline", "VideoProfile", "WeddingPlanner",
]


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

    if findings:
        print("Production integrity scan FAILED")
        for finding in findings:
            print(finding)
        return 1

    print("Production integrity scan passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
