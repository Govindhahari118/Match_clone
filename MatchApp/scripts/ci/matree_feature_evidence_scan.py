#!/usr/bin/env python3
"""Fail-closed validation for Matree's 100-feature execution ledger.

No status is upgraded by this command. Evidence must be recorded by reviewers.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import sys

STATUSES = {
    "AUDIT_REQUIRED", "EXISTS_TEST_NEEDED", "PARTIAL", "MISSING",
    "DEFECTIVE", "BLOCKED_EXTERNAL", "CODE_COMPLETE", "CI_VERIFIED",
    "DEVICE_VERIFIED", "IMPLEMENTED_VERIFIED", "OUT_OF_SCOPE_APPROVED",
}
REQUIRED_EVIDENCE = (
    "owner", "sourcePaths", "testPaths", "mergedMainSha", "ciRunUrl",
    "deviceEvidenceUrl", "securityReviewUrl", "qaSignoff", "lastVerifiedAt",
)
COMMIT_RE = re.compile(r"^[0-9a-f]{40}$")
ROW_ID_RE = re.compile(r"^MAT-\d{3}$")


def parse_rows(tracker: str) -> list[tuple[str, str]]:
    rows = []
    for line in tracker.splitlines():
        columns = [column.strip() for column in line.strip().split("|")]
        if len(columns) >= 7 and ROW_ID_RE.fullmatch(columns[1]):
            rows.append((columns[1], columns[4]))
    return rows


def validate(tracker: str, evidence: object) -> list[str]:
    errors: list[str] = []
    records = parse_rows(tracker)
    counts: dict[str, int] = {}
    for feature_id, status in records:
        counts[feature_id] = counts.get(feature_id, 0) + 1
        if status not in STATUSES:
            errors.append(f"{feature_id}: unsupported tracker status {status}")
    expected = {f"MAT-{n:03d}" for n in range(1, 101)}
    actual = set(counts)
    errors.extend(f"missing tracker row {key}" for key in sorted(expected - actual))
    errors.extend(f"unexpected tracker row {key}" for key in sorted(actual - expected))
    errors.extend(f"duplicate tracker row {key}" for key, count in counts.items() if count != 1)

    if not isinstance(evidence, dict):
        return errors + ["evidence must be a JSON object keyed by feature ID"]
    for key in sorted(expected - set(evidence)):
        errors.append(f"missing evidence record {key}")

    for key, item in evidence.items():
        if key not in expected:
            errors.append(f"unknown evidence ID {key}")
            continue
        if not isinstance(item, dict):
            errors.append(f"{key}: evidence entry must be object")
            continue
        status = item.get("status")
        if status not in STATUSES:
            errors.append(f"{key}: invalid evidence status")
            continue
        tracker_status = dict(records).get(key)
        if tracker_status != status:
            errors.append(f"{key}: tracker/evidence status disagreement")
        if status == "OUT_OF_SCOPE_APPROVED":
            # Deferred scope must be an explicit, accountable decision; it is never
            # equivalent to an implemented feature or permission to hide a P0/P1.
            if item.get("priority") in {"P0", "P1"}:
                errors.append(f"{key}: launch-critical feature cannot be out of scope")
            if not isinstance(item.get("scopeReason"), str) or not item["scopeReason"].strip():
                errors.append(f"{key}: approved deferral needs a written scopeReason")
            if not isinstance(item.get("scopeApprovedBy"), str) or not item["scopeApprovedBy"].strip():
                errors.append(f"{key}: approved deferral needs a scopeApprovedBy")
            if not str(item.get("scopeDecisionUrl", "")).startswith("https://"):
                errors.append(f"{key}: approved deferral needs an HTTPS scopeDecisionUrl")
        if status != "IMPLEMENTED_VERIFIED":
            continue
        for field in REQUIRED_EVIDENCE:
            value = item.get(field)
            if value is None or value is False or value == "" or value == []:
                errors.append(f"{key}: missing {field} for IMPLEMENTED_VERIFIED")
        if not COMMIT_RE.fullmatch(str(item.get("mergedMainSha", ""))):
            errors.append(f"{key}: mergedMainSha must be a full commit SHA")
        if item.get("qaSignoff") is not True:
            errors.append(f"{key}: qaSignoff must be true")
        for field in ("ciRunUrl", "deviceEvidenceUrl", "securityReviewUrl"):
            if not str(item.get(field, "")).startswith("https://"):
                errors.append(f"{key}: {field} must be an HTTPS evidence link")
        for field in ("sourcePaths", "testPaths"):
            value = item.get(field)
            if not isinstance(value, list) or not value or not all(
                isinstance(path, str) and path.strip() for path in value
            ):
                errors.append(f"{key}: {field} must contain nonempty paths")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    project = Path(__file__).resolve().parents[3]
    parser.add_argument("--tracker", type=Path, default=project / "docs/production-readiness/MATREE_100_FEATURE_E2E_DELIVERY_2026-10-10.md")
    parser.add_argument("--evidence", type=Path, default=project / "docs/production-readiness/matree-feature-evidence.json")
    args = parser.parse_args()
    try:
        errors = validate(
            args.tracker.read_text(encoding="utf-8"),
            json.loads(args.evidence.read_text(encoding="utf-8")),
        )
    except (OSError, ValueError) as exc:
        print(f"Feature evidence scan FAILED: {exc}")
        return 1
    if errors:
        print("Feature evidence scan FAILED:")
        for error in errors:
            print(f" - {error}")
        return 1
    print("Feature evidence scan PASS (100 IDs; evidence/status consistency checked; no release sign-off inferred)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
