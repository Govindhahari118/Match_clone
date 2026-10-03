#!/usr/bin/env python3
"""Require critical production discovery and partner-preference surfaces to use i18n."""
from __future__ import annotations
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
DISCOVERY = ROOT / "app/src/main/java/com/match/app/ui/matches/MatchesScreen.kt"
PREFERENCES = ROOT / "app/src/main/java/com/match/app/ui/preferences/PartnerPreferencesScreen.kt"

REQUIRED = {
    DISCOVERY: {
        "discover", "search_profiles_hint", "all_filters", "search_preferences",
        "strong_mutual_match", "mutual_preferences", "reciprocal_fit",
        "send_interest", "interested", "saved_searches", "apply_filters",
        "no_mutual_threshold_profiles", "no_filter_profiles",
    },
    PREFERENCES: {
        "partner_preferences", "strict_preference_explanation",
        "sensitive_preference_processing", "core_preferences",
        "community_faith", "location_residence", "language_family_career",
        "lifestyle_wellbeing", "strict", "preferred", "no_preference",
        "save_partner_preferences", "show_partner_summary_title",
    },
}

def main() -> int:
    failures: list[str] = []
    for path, keys in REQUIRED.items():
        text = path.read_text(encoding="utf-8")
        compact = "".join(text.split())
        for key in sorted(keys):
            if f't("{key}"' not in compact:
                failures.append(f"{path.relative_to(ROOT)} missing i18n usage for {key}")
    if failures:
        print("Discovery/preferences localization scan FAILED")
        for item in failures:
            print(f"- {item}")
        return 1
    print("Discovery/preferences localization scan passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
