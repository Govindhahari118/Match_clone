#!/usr/bin/env python3
"""Fail when Android calls a Firebase callable that does not exist in Functions source."""
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
ANDROID_ROOT = ROOT / "app" / "src" / "main"
FUNCTIONS_ROOT = ROOT / "functions" / "src"

CALL_RE = re.compile(r'getHttpsCallable\(\s*["\']([A-Za-z0-9_]+)["\']\s*\)')
EXPORT_RE = re.compile(r'\bexport\s+const\s+([A-Za-z_][A-Za-z0-9_]*)\s*=')


def main() -> int:
    android_calls: dict[str, list[str]] = {}
    for path in ANDROID_ROOT.rglob("*"):
        if not path.is_file() or path.suffix not in {".kt", ".java"}:
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for match in CALL_RE.finditer(text):
            android_calls.setdefault(match.group(1), []).append(
                path.relative_to(ROOT).as_posix()
            )

    exports: set[str] = set()
    for path in FUNCTIONS_ROOT.rglob("*.ts"):
        text = path.read_text(encoding="utf-8", errors="replace")
        exports.update(EXPORT_RE.findall(text))

    missing = sorted(name for name in android_calls if name not in exports)
    if missing:
        print("Callable contract scan FAILED")
        for name in missing:
            locations = ", ".join(sorted(set(android_calls[name])))
            print(f"- Android callable {name!r} has no exported Functions implementation: {locations}")
        return 1

    print(
        f"Callable contract scan passed: {len(android_calls)} Android callable names "
        f"resolve to Functions exports."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
