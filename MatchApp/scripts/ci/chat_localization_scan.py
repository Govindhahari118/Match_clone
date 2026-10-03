#!/usr/bin/env python3
"""Require critical production chat surfaces to resolve visible copy through i18n."""
from __future__ import annotations
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
CHAT = ROOT / "app/src/main/java/com/match/app/ui/chat/ChatScreen.kt"
LIST = ROOT / "app/src/main/java/com/match/app/ui/chat/ChatListScreen.kt"

REQUIRED = {
    CHAT: {
        "mic_permission_voice", "voice_recording_start_failed", "recording_time",
        "replying_to", "message_placeholder", "loading_conversation",
        "conversation_unavailable", "private_chat_safety_notice", "block_member_title",
        "block_member_body", "image_message", "chat_blocked_notice",
        "chat_mutual_required_notice", "start_your_conversation",
        "mutual_interest_required", "chat_respect_notice",
        "both_accept_before_messages", "tap_to_retry", "voice_message",
        "online_now", "last_active", "typing", "report_profile",
    },
    LIST: {
        "messages", "unread_messages", "search_chat_placeholder",
        "messaging_mutual_hint", "chat_safety_full", "no_matching_conversations",
        "no_conversations_search", "conversation_started",
    },
}

def main() -> int:
    failures: list[str] = []
    for path, keys in REQUIRED.items():
        text = path.read_text(encoding="utf-8")
        for key in sorted(keys):
            if f't("{key}"' not in text:
                failures.append(f"{path.relative_to(ROOT)} missing i18n usage for {key}")
    if failures:
        print("Chat localization scan FAILED")
        for item in failures:
            print(f"- {item}")
        return 1
    print("Chat localization scan passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
