#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCAN_ROOTS = [ROOT / "app" / "src" / "main", ROOT / "functions" / "src"]
EXTENSIONS = {".kt", ".kts", ".java", ".ts", ".js", ".xml"}

STUB_RULES = [
    ("Kotlin TODO executable stub", re.compile(r"\\bTODO\\s*\\(")),
    ("NotImplementedError executable stub", re.compile(r"\\bNotImplementedError\\b")),
    ("UnsupportedOperationException executable stub", re.compile(r"\\bUnsupportedOperationException\\b")),
]

def source_files():
    for root in SCAN_ROOTS:
        if root.exists():
            for path in root.rglob("*"):
                if path.is_file() and path.suffix.lower() in EXTENSIONS:
                    yield path

def main() -> int:
    findings = []
    for path in source_files():
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
        for line_no, line in enumerate(lines, 1):
            for label, pattern in STUB_RULES:
                if pattern.search(line):
                    findings.append(f"{path.relative_to(ROOT)}:{line_no}: {label}: {line.strip()}")

            if re.search(r"onClick\\s*=\\s*\\{\\s*\\}", line):
                nearby = "\\n".join(lines[line_no - 1:min(len(lines), line_no + 3)])
                disabled = re.search(r"enabled\\s*=\\s*false", nearby) is not None
                if not disabled:
                    findings.append(
                        f"{path.relative_to(ROOT)}:{line_no}: interactive empty onClick: {line.strip()}"
                    )

    if findings:
        print("Production integrity scan FAILED")
        for finding in findings:
            print(finding)
        return 1
    print("Production integrity scan passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
