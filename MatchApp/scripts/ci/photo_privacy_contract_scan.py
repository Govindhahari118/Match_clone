#!/usr/bin/env python3
"""Guard the server-authoritative profile photo privacy and request workflow."""
from __future__ import annotations
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")

def require(condition: bool, message: str, failures: list[str]) -> None:
    if not condition:
        failures.append(message)

def main() -> int:
    failures: list[str] = []
    policy = read("functions/src/photoPrivacyPolicy.ts")
    privacy = read("functions/src/privacy.ts")
    notifications = read("functions/src/notifications.ts")
    safety = read("functions/src/safety.ts")
    users = read("functions/src/users.ts")
    index = read("functions/src/index.ts")
    storage = read("storage.rules")
    firestore = read("firestore.rules")
    repo = read("app/src/main/java/com/match/app/data/repo/PhotoRepository.kt")
    detail = read("app/src/main/java/com/match/app/ui/detail/MatchDetailScreen.kt")
    dashboard = read("app/src/main/java/com/match/app/ui/privacy/PrivacyDashboardScreen.kt")

    for mode in ("PUBLIC", "ACCEPTED_ONLY", "HIDDEN"):
        require(mode in policy, f"photo privacy policy missing {mode}", failures)
    for callable_name in (
        "getProfilePhotoAccess",
        "setProfilePhotoVisibility",
        "requestProfilePhotoAccess",
        "respondProfilePhotoAccess",
    ):
        require(f"export const {callable_name}" in privacy,
                f"privacy backend missing {callable_name}", failures)
        require(callable_name in index,
                f"Functions export surface missing {callable_name}", failures)

    require("profilePhotoAllowed" in storage and "photoVisibility" in storage,
            "Storage Rules must re-authorize published photo visibility", failures)
    require("explicitPhotoGrant" in storage and "mutualInterest" in storage,
            "Storage Rules must honor explicit grants and mutual interest", failures)
    require("allow write: if false" in firestore and "match /photoRequests/" in firestore,
            "photo requests must remain server-owned in Firestore Rules", failures)
    require("match /photoGrants/" in firestore,
            "photo grants must have an explicit Firestore Rules boundary", failures)
    require("allow delete: if false;" in firestore,
            "privacy settings deletion must remain fail-closed", failures)

    require("photoGrants" in safety and "photoRequests" in safety,
            "blocking must revoke photo grants and requests", failures)
    require("photoGrants" in users and "photoRequests" in users,
            "account deletion must remove photo grants and requests", failures)
    require("onPhotoRequestPending" in notifications and "PHOTO_REQUEST" in notifications,
            "photo request notification trigger is missing", failures)

    for callable_name in (
        "getProfilePhotoAccess",
        "setProfilePhotoVisibility",
        "requestProfilePhotoAccess",
        "respondProfilePhotoAccess",
    ):
        require(callable_name in repo,
                f"Android PhotoRepository missing trusted callable {callable_name}", failures)
    require("request_photo_access" in detail and "photoAccess.canView" in detail,
            "profile detail must render protected-photo request state", failures)
    require("profile_photo_visibility" in dashboard and "pendingPhotoRequests" in dashboard,
            "Privacy dashboard must expose owner photo controls and pending requests", failures)

    if failures:
        print("Photo privacy contract scan FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Photo privacy contract scan passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
