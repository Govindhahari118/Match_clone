#!/usr/bin/env python3
"""Validate production UI translation catalog parity and critical Telugu coverage."""
from __future__ import annotations
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
I18N = ROOT / "app/src/main/assets/i18n"
CODES = ("en", "te", "hi")
CRITICAL_TELUGU_KEYS = {
    "interests", "received", "mutual", "sent", "send_interest", "accept_and_chat",
    "start_chatting", "chat", "pending_interests", "personal_note", "decline",
    "withdraw_interest", "message", "message_after_match", "contact",
    "secure_call", "shortlist", "report_profile", "profile_unavailable",
    "online_now", "last_active", "private_match_conversation", "typing",
    "conversation_starters_title", "conversation_starter_1", "conversation_starter_2", "conversation_starter_3",
    "new_interest_title", "new_mutual_match_title", "new_message_title", "contact_request_title",
    "private_chat_safety_notice", "chat_safety_full", "replying_to", "voice_message",
    "what_they_are_looking_for", "trust_and_verification", "compatibility",
    "profile_details", "marital_status", "education", "profession", "mother_tongue",
    "request_contact_access", "contact_requests", "contact_request_pending", "approve",
}

def load(code: str) -> dict[str, str]:
    path = I18N / f"{code}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"{path} must contain a JSON object")
    return {str(k): str(v).strip() for k, v in data.items()}

def main() -> int:
    failures: list[str] = []
    catalogs = {code: load(code) for code in CODES}
    english_keys = set(catalogs["en"])

    for code in CODES:
        keys = set(catalogs[code])
        missing = sorted(english_keys - keys)
        extra = sorted(keys - english_keys)
        empty = sorted(k for k, v in catalogs[code].items() if not v)
        if missing:
            failures.append(f"{code}: missing keys: {', '.join(missing)}")
        if extra:
            failures.append(f"{code}: extra keys not present in English: {', '.join(extra)}")
        if empty:
            failures.append(f"{code}: empty translations: {', '.join(empty)}")

    for key in sorted(CRITICAL_TELUGU_KEYS):
        en = catalogs["en"].get(key, "")
        te = catalogs["te"].get(key, "")
        if not en or not te:
            failures.append(f"critical Telugu key missing: {key}")
        elif te == en:
            failures.append(f"critical Telugu key still falls back to English: {key}")

    if failures:
        print("I18n catalog scan FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print(
        f"I18n catalog scan passed: {len(english_keys)} keys across "
        f"{', '.join(CODES)} with critical Telugu journey coverage."
    )
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
