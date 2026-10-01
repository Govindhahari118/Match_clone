#!/usr/bin/env python3
"""Fail closed unless external production evidence exists for the exact release SHA."""
from __future__ import annotations

import argparse
import json
import pathlib
import re

SHA_RE = re.compile(r"^[0-9a-fA-F]{40}$")

# Concrete launch blockers. Provider-dependent features may remain OFF; the release record must prove
# that their fail-closed state was verified rather than silently treating missing providers as ready.
PRELAUNCH_GATES = (
    "sourceMergedAndFrozen",
    "productionFirebaseConfigured",
    "firebaseRulesIndexesFunctionsDeployed",
    "firebaseSecretsConfigured",
    "productionSigningVerified",
    "releaseCredentialsVerified",
    "appCheckPlayIntegrityVerified",
    "fcmProductionVerified",
    "crashlyticsAlertsVerified",
    "analyticsProductionVerified",
    "operatorRbacBootstrapVerified",
    "masterDataSeedVerified",
    "playAppSigningVerified",
    "signedReleaseAabVerified",
    "playCatalogConfigured",
    "playServiceAccountApiVerified",
    "playRtdnVerified",
    "authReleaseDeviceMatrixPassed",
    "billingLicensedMatrixPassed",
    "freshInstallPassed",
    "upgradeInstallPassed",
    "multiDeviceJourneyPassed",
    "poorNetworkRecoveryPassed",
    "physicalDeviceE2EPassed",
    "accessibilityDeviceMatrixPassed",
    "performanceSloPassed",
    "penetrationTestPassed",
    "legalAndDataSafetyApproved",
    "storeListingAndContentRatingApproved",
    "supportEscalationReady",
    "playPreLaunchReportPassed",
    "closedTestingPassed",
    "rollbackDrillPassed",
    "disabledProviderSurfacesVerified",
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

    expected_sha = args.sha.strip().lower()
    if not SHA_RE.fullmatch(expected_sha):
        print("Production external gate FAILED")
        print("- --sha must be an exact 40-character hexadecimal commit SHA")
        return 1

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

    release_owner = str(data.get("releaseOwner", "")).strip()
    captured_at = str(data.get("capturedAt", "")).strip()
    if len(release_owner) < 3:
        failures.append("releaseOwner is required")
    if len(captured_at) < 10:
        failures.append("capturedAt is required and must identify when evidence was frozen")

    recorded_sha = str(data.get("gitSha", "")).strip().lower()
    if not SHA_RE.fullmatch(recorded_sha):
        failures.append("gitSha must be an exact 40-character hexadecimal commit SHA")
    elif recorded_sha != expected_sha:
        failures.append(f"gitSha mismatch: expected {expected_sha}, found {recorded_sha}")

    gates = PRELAUNCH_GATES if args.mode == "prelaunch" else FULL_RELEASE_GATES
    for gate_name in gates:
        if data.get(gate_name) is not True:
            failures.append(f"{gate_name} must be true with real external evidence")

    evidence_links = data.get("evidence", {})
    if not isinstance(evidence_links, dict):
        failures.append("evidence must be an object of gate -> artifact/reference")
    else:
        for gate_name in gates:
            reference = str(evidence_links.get(gate_name, "")).strip()
            if len(reference) < 8:
                failures.append(f"evidence reference missing/too short for {gate_name}")
                continue
            lowered = reference.lower()
            if not args.allow_ci_synthetic and any(
                marker in lowered
                for marker in ("synthetic", "template", "placeholder", "example", "todo", "tbd")
            ):
                failures.append(f"non-production evidence reference rejected for {gate_name}")

    if failures:
        print(f"Production external gate FAILED ({args.mode})")
        for item in failures:
            print(f"- {item}")
        return 1

    print(f"Production external gate passed ({args.mode}) for {expected_sha}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
