#!/usr/bin/env python3
"""Validate external production evidence for an exact Matree release SHA.

Repository CI proves source-controlled properties. This gate proves the things CI cannot honestly
manufacture: deployed production configuration, provider integration, physical-device results,
independent security evidence, store/legal handoff, rollback and controlled rollout.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import pathlib
import sys
from typing import Any

REQUIRED_GATES = (
    "production_firebase_configuration",
    "production_signing",
    "app_check_play_integrity_enforcement",
    "aadhaar_registered_provider_flow",
    "selfie_liveness_face_provider",
    "voice_video_number_masking_provider",
    "google_play_licensed_billing",
    "physical_device_e2e",
    "performance_slo",
    "accessibility_physical_device",
    "penetration_test",
    "kundali_reference_validation",
    "localization_native_qa",
    "legal_data_safety_store",
    "rollback_drill",
    "controlled_rollout",
)


def non_empty(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def valid_timestamp(value: Any) -> bool:
    if not non_empty(value):
        return False
    raw = value.strip().replace("Z", "+00:00")
    try:
        parsed = dt.datetime.fromisoformat(raw)
    except ValueError:
        return False
    return parsed.tzinfo is not None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", required=True, type=pathlib.Path)
    parser.add_argument("--sha", default="")
    parser.add_argument(
        "--allow-pending",
        action="store_true",
        help="Validate template/schema only; PENDING gates are allowed and no release is certified.",
    )
    args = parser.parse_args()

    failures: list[str] = []
    try:
        payload = json.loads(args.evidence.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"External release evidence could not be read: {exc}", file=sys.stderr)
        return 2

    release_sha = payload.get("releaseSha")
    if args.allow_pending:
        if release_sha not in ("EXACT_RELEASE_SHA", "") and not non_empty(release_sha):
            failures.append("releaseSha must be a placeholder or non-empty string")
    else:
        if not non_empty(args.sha):
            failures.append("--sha is required for a certifying gate run")
        if release_sha != args.sha:
            failures.append(
                f"releaseSha mismatch: evidence={release_sha!r}, expected={args.sha!r}"
            )

    gates = payload.get("gates")
    if not isinstance(gates, list):
        failures.append("gates must be a list")
        gates = []

    by_id: dict[str, dict[str, Any]] = {}
    for row in gates:
        if not isinstance(row, dict) or not non_empty(row.get("id")):
            failures.append("every gate entry must be an object with a non-empty id")
            continue
        gate_id = row["id"].strip()
        if gate_id in by_id:
            failures.append(f"duplicate gate id: {gate_id}")
            continue
        by_id[gate_id] = row

    for gate_id in REQUIRED_GATES:
        row = by_id.get(gate_id)
        if row is None:
            failures.append(f"missing required gate: {gate_id}")
            continue
        status = str(row.get("status", "")).strip().upper()
        if args.allow_pending:
            if status not in {"PENDING", "PASS"}:
                failures.append(f"{gate_id}: status must be PENDING or PASS in the template")
        elif status != "PASS":
            failures.append(f"{gate_id}: status must be PASS, got {status or 'missing'}")

        if status == "PASS":
            if not non_empty(row.get("evidence")):
                failures.append(f"{gate_id}: PASS requires a non-empty evidence reference")
            if not valid_timestamp(row.get("completedAt")):
                failures.append(f"{gate_id}: PASS requires an offset-aware completedAt timestamp")
            owner = row.get("owner")
            if not non_empty(owner):
                failures.append(f"{gate_id}: PASS requires an evidence owner")

    unknown = sorted(set(by_id) - set(REQUIRED_GATES))
    if unknown:
        failures.append("unknown gate ids: " + ", ".join(unknown))

    if failures:
        print("External production release gate FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    if args.allow_pending:
        print("External release evidence template/schema is valid; no production certification was issued.")
    else:
        print(f"External production release gate PASSED for exact SHA {args.sha}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
