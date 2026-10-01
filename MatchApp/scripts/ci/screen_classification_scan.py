#!/usr/bin/env python3
"""Fail closed when Android screen inventory or production exposure drifts."""
from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
APP_MAIN = ROOT / "app" / "src" / "main" / "java" / "com" / "match" / "app"
REGISTRY = ROOT.parent / "docs" / "production-readiness" / "screen-classification.json"
NAV_FILES = [
    APP_MAIN / "ui" / "MatchRoot.kt",
    APP_MAIN / "ui" / "main" / "MainShell.kt",
    APP_MAIN / "navigation" / "DeepLinkRouteResolver.kt",
]
REMOTE_CONFIG = APP_MAIN / "core" / "config" / "RemoteConfigManager.kt"
ALLOWED = {"READY", "BETA", "STUB", "UNSAFE", "POST_LAUNCH"}
EXPOSURES = {"auth", "onboarding", "reachable", "flagged", "hidden"}


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)


def main() -> int:
    failures: list[str] = []
    data = json.loads(REGISTRY.read_text(encoding="utf-8"))
    rows = data.get("screens")
    if not isinstance(rows, list):
        print("Screen classification scan FAILED: screens must be a list")
        return 1

    discovered = {
        p.relative_to(APP_MAIN).as_posix()
        for p in (APP_MAIN / "ui").rglob("*Screen.kt")
    }
    registered: set[str] = set()
    symbols: set[str] = set()

    nav_text = "\n".join(
        p.read_text(encoding="utf-8") for p in NAV_FILES if p.exists()
    )
    remote = REMOTE_CONFIG.read_text(encoding="utf-8")

    for row in rows:
        if not isinstance(row, dict):
            fail("classification row is not an object", failures)
            continue
        path = str(row.get("path", ""))
        symbol = str(row.get("symbol", ""))
        status = str(row.get("status", ""))
        exposure = str(row.get("exposure", ""))
        flag = row.get("requiredFlag")

        if path in registered:
            fail(f"duplicate screen classification: {path}", failures)
        registered.add(path)
        if symbol in symbols:
            fail(f"duplicate screen symbol: {symbol}", failures)
        symbols.add(symbol)

        if path not in discovered:
            fail(f"classified screen does not exist: {path}", failures)
        if status not in ALLOWED:
            fail(f"{path}: invalid status {status}", failures)
        if exposure not in EXPOSURES:
            fail(f"{path}: invalid exposure {exposure}", failures)

        routed = re.search(rf"\b{re.escape(symbol)}\s*\(", nav_text) is not None

        if exposure == "hidden" and routed:
            fail(f"{path}: hidden {status} screen became production-routed", failures)
        if status in {"STUB", "UNSAFE", "POST_LAUNCH"} and exposure != "hidden":
            fail(f"{path}: {status} screens must remain hidden", failures)
        if exposure in {"auth", "onboarding", "reachable", "flagged"} and not routed:
            fail(f"{path}: declared {exposure} but no production route invokes {symbol}", failures)

        if exposure == "flagged":
            if not isinstance(flag, str) or not flag:
                fail(f"{path}: flagged screen missing requiredFlag", failures)
            else:
                if re.search(rf"{re.escape(flag)}\s+to\s+false\b", remote) is None:
                    fail(f"{path}: {flag} must default false in Remote Config", failures)

    missing = sorted(discovered - registered)
    extra = sorted(registered - discovered)
    for path in missing:
        fail(f"unclassified Android screen: {path}", failures)
    for path in extra:
        fail(f"classification points to missing Android screen: {path}", failures)

    if failures:
        print("Screen classification scan FAILED")
        for item in failures:
            print(f"- {item}")
        return 1

    print(f"Screen classification scan passed: {len(discovered)} screens explicitly classified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
