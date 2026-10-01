#!/usr/bin/env python3
"""Verify that a production build/deploy is executed from the exact frozen source SHA."""
from __future__ import annotations

import argparse
import pathlib
import re
import subprocess

SHA_RE = re.compile(r"^[0-9a-fA-F]{40}$")


def git(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return result.stdout.strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--sha", required=True, help="Exact 40-character release commit SHA")
    parser.add_argument(
        "--allow-dirty",
        action="store_true",
        help="Local diagnostics only; production build/deploy scripts never use this.",
    )
    args = parser.parse_args()

    expected = args.sha.strip().lower()
    if not SHA_RE.fullmatch(expected):
        print("Release source guard FAILED")
        print("- --sha must be an exact 40-character hexadecimal commit SHA")
        return 1

    try:
        root = pathlib.Path(git("rev-parse", "--show-toplevel")).resolve()
        head = git("rev-parse", "HEAD").lower()
        status = git("status", "--porcelain")
    except (subprocess.CalledProcessError, OSError) as exc:
        print(f"Release source guard FAILED: unable to inspect git source: {exc}")
        return 1

    if head != expected:
        print("Release source guard FAILED")
        print(f"- expected HEAD {expected}")
        print(f"- actual HEAD   {head}")
        return 1

    if status and not args.allow_dirty:
        print("Release source guard FAILED")
        print("- working tree is not clean; production actions require frozen source")
        for line in status.splitlines()[:20]:
            print(f"  {line}")
        return 1

    print(f"Release source guard passed for {expected} at {root}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
