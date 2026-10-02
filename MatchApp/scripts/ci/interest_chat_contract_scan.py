#!/usr/bin/env python3
"""Guard Matree's canonical interest -> notification -> mutual match -> chat journey."""
from __future__ import annotations
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

CONTRACTS = {
    "functions/src/interests.ts": (
        "export const sendInterest",
        "tx.set(outgoingRef",
        "const mutual = reverseSnap.exists",
        "tx.set(matchRef",
    ),
    "functions/src/notifications.ts": (
        "export const onInterestCreated",
        '"interest_received"',
        "export const onMatchCreated",
        '"mutual_match"',
        "export const onNewMessage",
    ),
    "functions/src/chat.ts": (
        "export const prepareChatThread",
        "export const sendChatMessage",
        "Mutual match required",
    ),
    "app/src/main/java/com/match/app/data/remote/FirestoreInterestService.kt": (
        'getHttpsCallable("sendInterest")',
        'getHttpsCallable("declineInterest")',
        "observeIncomingInterests",
        "observeMatches",
    ),
    "app/src/main/java/com/match/app/ui/interests/InterestsScreen.kt": (
        't("accept_and_chat"',
        "SnackbarResult.ActionPerformed",
        "onOpenChat(matchedId)",
        't("start_chatting"',
    ),
    "app/src/main/java/com/match/app/ui/detail/MatchDetailScreen.kt": (
        '"Send interest"',
        '"Message after match"',
        "enabled = ui.isMutual",
    ),
}

def main() -> int:
    failures: list[str] = []
    for rel, phrases in CONTRACTS.items():
        path = ROOT / rel
        if not path.is_file():
            failures.append(f"missing contract source: {rel}")
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for phrase in phrases:
            if phrase not in text:
                failures.append(f"{rel} missing journey contract: {phrase}")

    if failures:
        print("Interest/chat journey contract FAILED")
        for item in failures:
            print(f"- {item}")
        return 1

    print("Interest/chat journey contract passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
