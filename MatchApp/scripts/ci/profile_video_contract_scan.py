#!/usr/bin/env python3
"""Guard the approved profile-video playback privacy/no-autoplay contract."""
from __future__ import annotations
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

def main() -> int:
    failures: list[str] = []
    detail = read("app/src/main/java/com/match/app/ui/detail/MatchDetailScreen.kt")
    player = read("app/src/main/java/com/match/app/ui/common/ProtectedVideoPlayer.kt")
    owner = read("app/src/main/java/com/match/app/ui/videoprofile/VideoProfileScreen.kt")

    checks = [
        ('if (!ui.blocked && p.videoUrl.isNotBlank())' in detail,
         "match detail must hide profile video after a block"),
        ('rememberSecureMediaUri(videoSource)' in detail,
         "match detail must resolve profile video through authenticated secure media"),
        ('testTag("match_profile_video")' in detail,
         "match profile video must retain a stable acceptance-test tag"),
        ('playWhenReady = false' in player,
         "profile video must never autoplay"),
        ('DisposableEffect(player)' in player and 'player.release()' in player,
         "profile video player must release resources on disposal"),
        ('ProtectedVideoPlayer(playbackUri = playbackUri)' in owner,
         "owner preview and match detail must share the protected player contract"),
    ]

    for ok, message in checks:
        if not ok:
            failures.append(message)

    if failures:
        print("Profile video contract scan FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("Profile video contract scan passed")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
