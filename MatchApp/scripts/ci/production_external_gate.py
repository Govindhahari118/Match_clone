#!/usr/bin/env python3
"""Fail closed unless external production evidence exists for the exact release SHA."""
from __future__ import annotations

import argparse
import json
import pathlib
import sys

PRELAUNCH_GATES = (
    "productionFirebaseConfigured",
    "productionSigningVerified",
    "releaseCredentialsVerified",
    "appCheckPlayIntegrityVerified",
    "authReleaseDeviceMatrixPassed",
    "billingLicensedMatrixPassed",
    "physicalDeviceE2EPassed",
    "accessibilityDeviceMatrixPassed",
    "performanceSloPassed",
    "penetrationTestPassed",
    "legalAndDataSafetyApproved",
    "playPreLaunchReportPassed",
    "rollbackDrillPassed",
)

FULL_RELEASE_GATES = PRELAUNCH_GATES + (
    "controlledRolloutCompleted",
    "postRolloutHealthPassed",
)

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", required=True, type=pathlib.Path)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--mode", choices=("prelaunch", "full"), default="prelaunch")
    parser.add_argument(
        "--allow-ci-synthetic",
        action="store_true",
        help="CI self-test only: permit evidenceType=ci-synthetic-validator",
    )
    args = parser.parse_args()

    if not args.evidence.exists():
        print(f"External production evidence missing: {args.evidence}")
        return 1

    try:
        data = json.loads(args.evidence.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"External evidence is unreadable: {exc}")
        return 1

    failures: list[str] = []
    evidence_type = str(data.get("evidenceType", "")).strip()
    if args.allow_ci_synthetic:
        if evidence_type != "ci-synthetic-validator":
            failures.append("CI synthetic validation requires evidenceType=ci-synthetic-validator")
    elif evidence_type != "operator-controlled-production-evidence":
        failures.append(
            "evidenceType must be operator-controlled-production-evidence; "
            "CI/template/synthetic evidence is not release evidence"
        )

    recorded_sha = str(data.get("gitSha", "")).strip()
    if recorded_sha != args.sha:
        failures.append(f"gitSha mismatch: expected {args.sha}, found {recorded_sha or '<missing>'}")

    gates = PRELAUNCH_GATES if args.mode == "prelaunch" else FULL_RELEASE_GATES
    for gate in gates:
        value = data.get(gate)
        if value is not True:
            failures.append(f"{gate} must be true with real external evidence")

    evidence_links = data.get("evidence", {})
    if not isinstance(evidence_links, dict):
        failures.append("evidence must be an object of gate -> artifact/reference")
    else:
        for gate in gates:
            reference = str(evidence_links.get(gate, "")).strip()
            if len(reference) < 8:
                failures.append(f"evidence reference missing/too short for {gate}")
                continue
            lowered = reference.lower()
            if not args.allow_ci_synthetic and any(
                marker in lowered
                for marker in ("synthetic", "template", "placeholder", "example", "todo", "tbd")
            ):
                failures.append(f"non-production evidence reference rejected for {gate}")

    if failures:
        print(f"Production external gate FAILED ({args.mode})")
        for item in failures:
            print(f"- {item}")
        return 1

    print(f"Production external gate passed ({args.mode}) for {args.sha}.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
