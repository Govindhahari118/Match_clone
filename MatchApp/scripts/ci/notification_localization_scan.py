#!/usr/bin/env python3
"""Guard per-device localized notification delivery without leaking private chat text."""
from __future__ import annotations
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ANDROID = ROOT / "app/src/main/java/com/match/app"
FUNCTIONS = ROOT / "functions/src"
I18N = ROOT / "app/src/main/assets/i18n"

def text(path: pathlib.Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")

def main() -> int:
    failures: list[str] = []
    registry = text(ANDROID / "data/remote/FcmDeviceRegistry.kt")
    language = text(ANDROID / "ui/language/LanguageSelectionScreen.kt")
    cards = text(ANDROID / "ui/notifications/NotificationsScreen.kt")
    shared = text(FUNCTIONS / "shared.ts")
    notifications = text(FUNCTIONS / "notifications.ts")

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
    if notifications.count("localizedNotificationCopy(") < 6:
        failures.append("interest, match, message and contact-request notifications must provide localized push copy")
    if 'notificationDeepLink("messages"' not in notifications:
        failures.append("new-message notifications must preserve the dedicated Messages deep link")
    if 'body: "Open the app to view your message."' not in notifications:
        failures.append("new-message push must keep generic body copy instead of private chat content")

    required_keys = (
        "new_interest_title", "new_interest_body",
        "new_mutual_match_title", "new_mutual_match_body",
        "new_message_title", "new_message_body",
        "contact_request_title", "contact_request_body",
    )
    for key in required_keys:
        if f't("{key}"' not in cards:
            failures.append(f"Notifications screen must localize durable card copy: {key}")

    catalogs: dict[str, dict[str, str]] = {}
    for code in ("en", "te", "hi"):
        catalogs[code] = json.loads((I18N / f"{code}.json").read_text(encoding="utf-8"))
        for key in required_keys:
            if not str(catalogs[code].get(key, "")).strip():
                failures.append(f"{code}: missing localized notification key {key}")

    for key in required_keys:
        if catalogs.get("te", {}).get(key) == catalogs.get("en", {}).get(key):
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
