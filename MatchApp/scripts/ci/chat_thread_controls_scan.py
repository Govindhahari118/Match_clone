#!/usr/bin/env python3
"""Guard per-conversation mute/archive behavior across client, server, rules and notifications."""
from __future__ import annotations
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8", errors="replace")

def main() -> int:
    failures: list[str] = []
    server = read("functions/src/chatPreferences.ts")
    notifications = read("functions/src/notifications.ts")
    index = read("functions/src/index.ts")
    service = read("app/src/main/java/com/match/app/data/remote/FirestoreChatService.kt")
    repo = read("app/src/main/java/com/match/app/data/repo/ChatRepository.kt")
    screen = read("app/src/main/java/com/match/app/ui/chat/ChatScreen.kt")
    inbox = read("app/src/main/java/com/match/app/ui/chat/ChatListScreen.kt")
    rules = read("firestore.rules")

    required_server = (
        "export const setChatThreadPreferences",
        'db.collection("chatPreferences").doc(uid).collection("threads").doc(threadId)',
        "participantUids",
        "muted",
        "archived",
        "serverTimestamp()",
    )
    for needle in required_server:
        if needle not in server:
            failures.append(f"server chat preference contract missing: {needle}")

    if 'export * from "./chatPreferences";' not in index:
        failures.append("setChatThreadPreferences must stay exported")
    if 'recipientChatPreference.data()?.muted !== true' not in notifications:
        failures.append("muted threads must suppress message notification delivery")
    if "restoreArchivedConversationOnIncoming" not in notifications or "archived: false" not in notifications:
        failures.append("new incoming messages must restore archived conversations to Active")

    for needle in (
        "observeThreadPreferences",
        "observeThreadPreference",
        'getHttpsCallable("setChatThreadPreferences")',
    ):
        if needle not in service:
            failures.append(f"Android Firestore service missing chat preference contract: {needle}")
    if "setThreadPreferences" not in repo:
        failures.append("ChatRepository must expose thread preference updates")

    for needle in (
        "toggleThreadMuted",
        "toggleThreadArchived",
        't("mute_chat"',
        't("archive_chat"',
        't("unmute_chat"',
        't("unarchive_chat"',
    ):
        if needle not in screen:
            failures.append(f"active chat controls missing: {needle}")

    for needle in (
        "showArchived",
        "conversation.archived == showArchived",
        't("active_chats"',
        't("archived_chats"',
        "conv.muted",
    ):
        if needle not in inbox:
            failures.append(f"chat inbox mute/archive behavior missing: {needle}")

    if "match /chatPreferences/{ownerUid}/threads/{threadId}" not in rules:
        failures.append("Firestore rules missing chatPreferences owner-read boundary")
    if "allow create, update, delete: if false;" not in rules:
        failures.append("chatPreferences must remain server-write-only")

    if failures:
        print("Chat thread controls contract FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Chat thread controls contract passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
