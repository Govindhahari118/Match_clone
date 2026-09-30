#!/usr/bin/env python3
"""Validate the repository-controlled production handoff package.

This gate intentionally does not certify external/provider/device evidence. It only ensures that the
engineering artifacts required by the frozen Matree master plan exist and stay wired to source truth.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

REQUIRED = (
    "docs/release/ENVIRONMENT_SPEC.md",
    "docs/release/SECRETS_SPEC.md",
    "docs/release/API_SPEC.md",
    "docs/release/DATABASE_SCHEMA.md",
    "docs/release/MASTER_DATA.md",
    "docs/release/KNOWN_ISSUES.md",
    "docs/release/GO_NO_GO.md",
    "docs/release/PRODUCTION_CONFIG.template.md",
    "docs/release/FEATURE_FLAGS.md",
    "docs/release/ROLLBACK_RUNBOOK.md",
    "docs/release/PLAY_STORE_HANDOFF.md",
    "docs/release/PRODUCTION_EXTERNAL_EVIDENCE.template.json",
    "scripts/ci/production_external_gate.py",
    "scripts/ci/release_evidence.py",
    "scripts/ci/truthfulness_scan.py",
    "scripts/ci/screen_classification_scan.py",
    "scripts/ci/callable_contract_scan.py",
    "scripts/perf/discovery-load.mjs",
    "functions/src/health.ts",
    "firestore.rules",
    "firestore.indexes.json",
    "storage.rules",
)

FORBIDDEN_MARKERS = re.compile(r"\b(?:TODO|TBD|FIXME|REPLACE_ME)\b", re.IGNORECASE)


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)


def main() -> int:
    failures: list[str] = []

    for rel in REQUIRED:
        target = ROOT / rel
        if not target.is_file() or target.stat().st_size == 0:
            fail(f"missing/empty required handoff artifact: {rel}", failures)

    handoff_docs = [
        ROOT / "docs/release/ENVIRONMENT_SPEC.md",
        ROOT / "docs/release/SECRETS_SPEC.md",
        ROOT / "docs/release/API_SPEC.md",
        ROOT / "docs/release/DATABASE_SCHEMA.md",
        ROOT / "docs/release/MASTER_DATA.md",
        ROOT / "docs/release/KNOWN_ISSUES.md",
        ROOT / "docs/release/GO_NO_GO.md",
    ]
    for target in handoff_docs:
        if not target.exists():
            continue
        text = target.read_text(encoding="utf-8")
        marker = FORBIDDEN_MARKERS.search(text)
        if marker:
            fail(f"{target.relative_to(ROOT)} contains unresolved marker: {marker.group(0)}", failures)

    health = (ROOT / "functions/src/health.ts").read_text(encoding="utf-8")
    for exported in ("healthLive", "healthReady"):
        if not re.search(rf"export\s+const\s+{exported}\b", health):
            fail(f"health contract missing export {exported}", failures)

    migrations = (ROOT / "app/src/main/java/com/match/app/data/local/Migrations.kt").read_text(
        encoding="utf-8"
    )
    if "OLDEST_SUPPORTED_VERSION" not in migrations or "CURRENT_VERSION" not in migrations:
        fail("migration boundary constants are missing", failures)
    if "val ALL: List<Migration>" not in migrations:
        fail("registered migration chain is missing", failures)

    api = (ROOT / "docs/release/API_SPEC.md").read_text(encoding="utf-8")
    for critical in (
        "discoverProfiles",
        "sendInterest",
        "sendChatMessage",
        "verifyGooglePlayPurchase",
        "submitVerificationRequest",
        "getTrustSummary",
        "deleteUserAccount",
        "healthReady",
    ):
        if f"`{critical}`" not in api:
            fail(f"API handoff omits critical endpoint: {critical}", failures)

    go_no_go = (ROOT / "docs/release/GO_NO_GO.md").read_text(encoding="utf-8")
    if "Production GO" not in go_no_go or "external" not in go_no_go.lower():
        fail("GO/NO-GO report must preserve the external-evidence boundary", failures)

    if failures:
        print("Production handoff package scan FAILED")
        for item in failures:
            print(f"- {item}")
        return 1

    print("Production handoff package scan passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
