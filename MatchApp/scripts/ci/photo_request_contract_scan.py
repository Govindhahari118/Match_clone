#!/usr/bin/env python3
"""Guard Matree's canonical profile photo-request journey."""
from __future__ import annotations
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

REQUIRED = {
    "functions/src/photoRequests.ts": (
        "export const getProfilePhotoRequestStatus",
        "export const requestProfilePhoto",
        "photoRequestCoolingDown",
    ),
    "functions/src/notifications.ts": (
        "export const onProfilePhotoRequested",
        'pushType: "photo_request"',
        'type: "PHOTO_REQUEST"',
    ),
    "firestore.rules": (
        "match /photoRequests/{requestId} { allow read, write: if false; }",
    ),
    "app/src/main/java/com/match/app/data/repo/PhotoRequestRepository.kt": (
        'getHttpsCallable("getProfilePhotoRequestStatus")',
        'getHttpsCallable("requestProfilePhoto")',
    ),
    "app/src/main/java/com/match/app/ui/detail/MatchDetailScreen.kt": (
        '"profile_request_photo"',
        't("request_photo"',
        't("photo_requested"',
    ),
}

def main() -> int:
    failures: list[str] = []
    for relative, needles in REQUIRED.items():
        path = ROOT / relative
        if not path.exists():
            failures.append(f"missing file: {relative}")
            continue
        text = path.read_text(encoding="utf-8")
        for needle in needles:
            if needle not in text:
                failures.append(f"{relative}: missing {needle}")

    if failures:
        print("Photo request journey contract FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("Photo request journey contract passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
