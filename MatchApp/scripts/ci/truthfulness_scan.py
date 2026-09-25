#!/usr/bin/env python3
"""Fail CI on production-only patterns that violate Matree truthfulness invariants.

This intentionally scans executable production source only. Debug/test fixtures and documentation
are excluded because the source plan allows deterministic test/demo data outside production paths.
Every newly added rule should target a high-confidence production defect, not a vague keyword.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCAN_ROOTS = [
    ROOT / "app" / "src" / "main",
    ROOT / "functions" / "src",
]

RULES: list[tuple[str, re.Pattern[str]]] = [
    ("fixed OTP 123456", re.compile(r"(?<!\d)123456(?!\d)")),
    ("randomuser production human", re.compile(r"randomuser\.me", re.I)),
    ("Kotlin TODO() executable stub", re.compile(r"\bTODO\s*\(")),
    ("NotImplementedError executable stub", re.compile(r"\bNotImplementedError\b")),
    ("UnsupportedOperationException executable stub", re.compile(r"\bUnsupportedOperationException\b")),
    ("empty Compose onClick callback", re.compile(r"onClick\s*=\s*\{\s*\}")),
]

EXTENSIONS = {".kt", ".kts", ".java", ".ts", ".js", ".xml"}

def iter_source_files():
    for root in SCAN_ROOTS:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if path.is_file() and path.suffix.lower() in EXTENSIONS:
                yield path

def main() -> int:
    findings: list[str] = []
    for path in iter_source_files():
        text = path.read_text(encoding="utf-8", errors="replace")
        lines = text.splitlines()
        for line_no, line in enumerate(lines, start=1):
            for label, pattern in RULES:
                if pattern.search(line):
                    rel = path.relative_to(ROOT)
                    findings.append(f"{rel}:{line_no}: {label}: {line.strip()}")

            # An empty onClick is a defect only when the component is actually interactive.
            # Material chips used purely as status/metadata are allowed when explicitly disabled.
            if re.search(r"onClick\\s*=\\s*\\{\\s*\\}", line):
                nearby = "\n".join(lines[line_no - 1:min(len(lines), line_no + 3)])
                explicitly_disabled = re.search(r"enabled\\s*=\\s*false", nearby) is not None
                if not explicitly_disabled:
                    rel = path.relative_to(ROOT)
                    findings.append(
                        f"{rel}:{line_no}: interactive empty Compose onClick callback: {line.strip()}"
                    )

    if findings:
        print("Production truthfulness scan FAILED")
        for finding in findings:
            print(finding)
        print(
            "\nDo not suppress findings globally. Remove the production defect or narrow a rule "
            "only when the occurrence is demonstrably safe."
        )
        return 1

    print("Production truthfulness scan passed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
