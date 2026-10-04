#!/usr/bin/env python3
"""Guard stable-code profile/message reporting and server-owned moderation evidence."""
from __future__ import annotations
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]

def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8", errors="replace")

def main() -> int:
    failures: list[str] = []

    reasons = text("app/src/main/java/com/match/app/ui/common/ReportReasonOptions.kt")
    detail = text("app/src/main/java/com/match/app/ui/detail/MatchDetailScreen.kt")
    chat = text("app/src/main/java/com/match/app/ui/chat/ChatScreen.kt")
    support = text("app/src/main/java/com/match/app/data/repo/SupportRepository.kt")
    safety = text("functions/src/safety.ts")
    policy = text("functions/src/reportReasonPolicy.ts")
    rules = text("firestore.rules")

    required_codes = [
        "FAKE_PROFILE",
        "SCAM_OR_MONEY_REQUEST",
        "HARASSMENT",
        "INAPPROPRIATE_CONTENT",
        "UNDERAGE_CONCERN",
        "OTHER",
    ]
    for code in required_codes:
        if code not in reasons:
            failures.append(f"missing stable Android report reason code: {code}")

    if "selected?.code?.let(vm::submitReport)" not in detail:
        failures.append("profile reporting must submit the stable reason code, not localized label")
    if "vm.reportMember(reason.code)" not in chat:
        failures.append("chat profile reporting must submit the stable reason code")
    if "submitChatMessageReport" not in support or "vm.reportMessage(message, reason.code)" not in chat:
        failures.append("specific-message reporting must remain wired through SupportRepository")
    if "normalizeReportReason" not in safety or "REPORT_REASON_CODES" not in policy:
        failures.append("backend must normalize stable report reason codes")
    if "evidence:" not in safety or "messageSnap.data()" not in safety:
        failures.append("message reports must snapshot server-owned message evidence")
    if "message.fromFirebaseUid !== targetUid" not in safety:
        failures.append("members must not report their own message through the received-message flow")
    if "match /chatMessageReports/{reportId} { allow read, write: if false; }" not in rules:
        failures.append("chat message report evidence must stay client-inaccessible")

    if failures:
        print("Reporting contract scan FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("Reporting contract scan passed")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
