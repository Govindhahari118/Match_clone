#!/usr/bin/env python3
"""Inspect a generated release AAB for obvious debug/test leakage."""
from __future__ import annotations
import glob
import pathlib
import sys
import zipfile

FORBIDDEN_ENTRY_PARTS = (
    "matreethemepreviews",
    "debugappcheckprovider",
    "androidx.compose.ui.test",
)
FORBIDDEN_TEXT = (
    b"http://10.0.2.2",
    b"http://localhost",
    b"http://127.0.0.1",
)

def main() -> int:
    patterns = sys.argv[1:] or ["app/build/outputs/bundle/release/*.aab"]
    files = [pathlib.Path(p) for pattern in patterns for p in glob.glob(pattern)]
    if not files:
        print("No AAB available for artifact audit (external production Firebase/signing may still be required).")
        return 0
    failures: list[str] = []
    for aab in files:
        with zipfile.ZipFile(aab) as zf:
            for info in zf.infolist():
                lower = info.filename.lower()
                if any(part in lower for part in FORBIDDEN_ENTRY_PARTS):
                    failures.append(f"{aab.name}: forbidden release entry {info.filename}")
                if info.file_size <= 2_000_000 and info.filename.endswith((".xml", ".json", ".txt", ".properties")):
                    payload = zf.read(info)
                    for token in FORBIDDEN_TEXT:
                        if token in payload:
                            failures.append(f"{aab.name}: forbidden endpoint in {info.filename}: {token.decode()}")
    if failures:
        print("Release artifact audit FAILED")
        print("\n".join(f"- {x}" for x in failures))
        return 1
    print("Release artifact audit passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
