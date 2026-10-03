#!/usr/bin/env python3
"""Validate Matree production UI translation catalogs and static translation-key coverage."""
from __future__ import annotations

import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "i18n"
UI_ROOT = ROOT / "app" / "src" / "main" / "java" / "com" / "match" / "app" / "ui"
SUPPORTED = ("en", "te", "hi")
KEY_RE = re.compile(r"\b(?:t|tp)\(\s*[\"']([A-Za-z0-9_.-]+)[\"']")

def load(code: str) -> dict[str, str]:
    path = ASSETS / f"{code}.json"
    if not path.exists():
        raise ValueError(f"missing catalog: {path.relative_to(ROOT)}")
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"{path.relative_to(ROOT)} must contain a JSON object")
    return {str(k): str(v) for k, v in data.items()}

def main() -> int:
    failures: list[str] = []
    catalogs: dict[str, dict[str, str]] = {}
    for code in SUPPORTED:
        try:
            catalogs[code] = load(code)
        except (ValueError, json.JSONDecodeError) as exc:
            failures.append(str(exc))

    if len(catalogs) == len(SUPPORTED):
        english_keys = set(catalogs["en"])
        for code, catalog in catalogs.items():
            keys = set(catalog)
            missing = sorted(english_keys - keys)
            extra = sorted(keys - english_keys)
            if missing:
                failures.append(f"{code}.json missing keys: {', '.join(missing)}")
            if extra:
                failures.append(f"{code}.json has keys absent from en.json: {', '.join(extra)}")
            blanks = sorted(k for k, v in catalog.items() if not v.strip())
            if blanks:
                failures.append(f"{code}.json has blank translations: {', '.join(blanks)}")

        referenced: set[str] = set()
        for path in UI_ROOT.rglob("*.kt"):
            text = path.read_text(encoding="utf-8", errors="replace")
            referenced.update(KEY_RE.findall(text))
        missing_references = sorted(referenced - english_keys)
        if missing_references:
            failures.append(
                "production UI references missing i18n keys: " + ", ".join(missing_references)
            )

    if failures:
        print("I18n catalog scan FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print(
        f"I18n catalog scan passed: {len(catalogs['en'])} keys across "
        + ", ".join(SUPPORTED)
    )
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
