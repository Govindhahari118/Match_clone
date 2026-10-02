#!/usr/bin/env python3
"""Validate and stage the generated Room schema for the current database version."""
from __future__ import annotations

import json
import pathlib
import re
import shutil
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
MIGRATIONS = ROOT / "app/src/main/java/com/match/app/data/local/Migrations.kt"
SCHEMA_ROOT = ROOT / "app/schemas"
OUT = ROOT / "app/build/outputs/room-schema"


def main() -> int:
    text = MIGRATIONS.read_text(encoding="utf-8")
    match = re.search(r"const val CURRENT_VERSION\s*=\s*(\d+)", text)
    if not match:
        print("Room schema evidence FAILED: CURRENT_VERSION not found")
        return 1
    version = int(match.group(1))
    candidates = sorted(SCHEMA_ROOT.glob(f"**/{version}.json"))
    if len(candidates) != 1:
        print(
            "Room schema evidence FAILED: expected exactly one generated "
            f"schema for v{version}, found {len(candidates)}"
        )
        for item in candidates:
            print(f"- {item.relative_to(ROOT)}")
        return 1

    schema_path = candidates[0]
    try:
        payload = json.loads(schema_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        print(f"Room schema evidence FAILED: invalid JSON: {exc}")
        return 1

    if int(payload.get("database", {}).get("version", -1)) != version:
        print("Room schema evidence FAILED: generated schema version mismatch")
        return 1

    entities = payload.get("database", {}).get("entities", [])
    tables = {entity.get("tableName") for entity in entities}
    if "users" not in tables:
        print("Room schema evidence FAILED: users table missing")
        return 1

    users = next(entity for entity in entities if entity.get("tableName") == "users")
    user_columns = {field.get("columnName") for field in users.get("fields", [])}
    retired = {"passwordHash", "isSeed"} & user_columns
    if retired:
        print("Room schema evidence FAILED: retired users columns remain: " + ", ".join(sorted(retired)))
        return 1

    OUT.mkdir(parents=True, exist_ok=True)
    staged = OUT / f"MatchDatabase-v{version}.json"
    shutil.copyfile(schema_path, staged)
    print(f"Room schema evidence passed: {schema_path.relative_to(ROOT)}")
    print(f"Staged: {staged.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
