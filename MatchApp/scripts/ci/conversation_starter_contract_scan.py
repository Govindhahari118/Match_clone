#!/usr/bin/env python3
"""Guard the first-conversation starter contract.

Starters are guidance only: they must be shown only to active mutual matches,
must populate the editable draft, and must never auto-send.
"""
from __future__ import annotations
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
CHAT = ROOT / "app/src/main/java/com/match/app/ui/chat/ChatScreen.kt"

def main() -> int:
    failures: list[str] = []
    text = CHAT.read_text(encoding="utf-8")
    compact = "".join(text.split())

    required = (
        'testTag("conversation_starters")',
        't("conversation_starters_title"',
        't("conversation_starters_body"',
        't("conversation_starter_1"',
        't("conversation_starter_2"',
        't("conversation_starter_3"',
        "if (state.isMutual && !state.isBlocked)",
        "onClick = { draft = starter }",
        'testTag("conversation_starter_$index")',
    )
    for needle in required:
        normalized = "".join(needle.split())
        if normalized not in compact:
            failures.append(f"missing conversation-starter contract: {needle}")

    if "vm.send(starter)" in text or "vm.send(draft)" in text:
        failures.append("conversation starters must never auto-send")

    if failures:
        print("Conversation starter contract FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("Conversation starter contract passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
