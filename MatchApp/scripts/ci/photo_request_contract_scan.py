#!/usr/bin/env python3
"""Guard Matree's privacy-safe request-photo journey."""
from __future__ import annotations
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]

CONTRACTS = {
    "functions/src/photoRequests.ts": (
        "export const getPhotoRequestStatus",
        "export const requestProfilePhoto",
        "export const onPublicProfilePhotoAvailable",
        "export const cleanupPhotoRequestsOnUserDelete",
        "photoRequestDailyAllowed",
        "photoRequestCooldownRemaining",
        "persistAndSendNotification",
        '"PHOTO_REQUEST"',
    ),
    "functions/src/photoRequestPolicy.ts": (
        "PHOTO_REQUEST_COOLDOWN_MS",
        "PHOTO_REQUEST_DAILY_LIMIT",
        "publicPhotoAvailable",
    ),
    "app/src/main/java/com/match/app/data/repo/PhotoRequestRepository.kt": (
        'getHttpsCallable("getPhotoRequestStatus")',
        'getHttpsCallable("requestProfilePhoto")',
    ),
    "app/src/main/java/com/match/app/ui/detail/MatchDetailScreen.kt": (
        't("request_photo"',
        't("photo_requested"',
        'testTag("profile_request_photo")',
        "photoRequestRepository.request",
    ),
    "app/src/main/java/com/match/app/navigation/DeepLinkRouteResolver.kt": (
        '"photo_request"',
        '"notifications"',
    ),
    "firestore.rules": (
        "match /photoRequests/{requestId} { allow read, write: if false; }",
        "match /photoRequestRateLimits/{rateId} { allow read, write: if false; }",
    ),
}

def main() -> int:
    failures: list[str] = []
    for rel, phrases in CONTRACTS.items():
        path = ROOT / rel
        if not path.is_file():
            failures.append(f"missing photo-request contract source: {rel}")
            continue
        body = path.read_text(encoding="utf-8", errors="replace")
        for phrase in phrases:
            if phrase not in body:
                failures.append(f"{rel} missing photo-request contract: {phrase}")
    if failures:
        print("Photo request contract FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Photo request contract passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
