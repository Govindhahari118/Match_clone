#!/usr/bin/env python3
"""Guard localized notification delivery and durable UI copy."""
from __future__ import annotations
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
ANDROID = ROOT / "app/src/main/java/com/match/app"
FUNCTIONS = ROOT / "functions/src"
I18N = ROOT / "app/src/main/assets/i18n"

def text(path: pathlib.Path) -> str:
    return path.read_text(encoding="utf-8")

def main() -> int:
    failures: list[str] = []
    registry = text(ANDROID / "data/remote/FcmDeviceRegistry.kt")
    language = text(ANDROID / "ui/language/LanguageSelectionScreen.kt")
    cards = text(ANDROID / "ui/notifications/NotificationsScreen.kt")
    notifications = text(FUNCTIONS / "notifications.ts")
    shared = text(FUNCTIONS / "shared.ts")

    if '"locale" to normalizedLocale' not in registry:
        failures.append("FCM registration must send the normalized device UI locale")
    if "fcmDeviceRegistry.register(token, code)" not in language:
        failures.append("changing app language must refresh the registered notification locale")
    if "locale: SupportedNotificationLocale" not in shared or "groups.set(record.locale" not in shared:
        failures.append("FCM delivery must group registered installations by device locale")
    if "localizedCopy?: NotificationCopyByLocale" not in shared:
        failures.append("persisted notification delivery must support localized per-device copy")
    if "const locale = normalizeNotificationLocale(data?.locale);" not in notifications:
        failures.append("registerFcmDevice must normalize and persist the device locale")
    if notifications.count("localizedNotificationCopy(") < 4:
        failures.append("interest, match and message notifications must provide localized push copy")
    for key in (
        "new_interest_title", "new_interest_body",
        "new_mutual_match_title", "new_mutual_match_body",
        "new_message_title", "new_message_body",
    ):
        if f'"{key}"' not in cards:
            failures.append(f"Notifications screen must localize durable card copy: {key}")

    catalogs = {}
    for code in ("en", "te", "hi"):
        catalogs[code] = json.loads((I18N / f"{code}.json").read_text(encoding="utf-8"))
    for key in (
        "new_interest_title", "new_interest_body",
        "new_mutual_match_title", "new_mutual_match_body",
        "new_message_title", "new_message_body",
    ):
        for code in ("en", "te", "hi"):
            if not str(catalogs[code].get(key, "")).strip():
                failures.append(f"{code}: missing localized notification key {key}")
        if catalogs["te"].get(key) == catalogs["en"].get(key):
            failures.append(f"Telugu notification key still equals English: {key}")

    if failures:
        print("Notification localization scan FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Notification localization scan passed")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
