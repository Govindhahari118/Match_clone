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
    "docs/release/SECURITY_ACCEPTANCE.md",
    "docs/release/REAL_DEVICE_ACCEPTANCE.md",
    "docs/release/ACCESSIBILITY_PERFORMANCE_ACCEPTANCE.md",
    "docs/release/INCIDENT_RESPONSE.md",
    "docs/release/DATA_SAFETY_WORKSHEET.md",
    "docs/release/PLAY_STORE_HANDOFF.md",
    "docs/release/PRODUCTION_EXTERNAL_EVIDENCE.template.json",
    "scripts/ci/production_external_gate.py",
    "scripts/ci/release_source_guard.py",
    "scripts/ci/release_evidence.py",
    "scripts/ci/room_schema_evidence.py",
    "scripts/ci/truthfulness_scan.py",
    "scripts/ci/screen_classification_scan.py",
    "scripts/deploy/build-release.sh",
    "scripts/deploy/build-release.bat",
    "scripts/deploy/firebase-full.sh",
    "scripts/deploy/firebase-full.bat",
    "scripts/ci/callable_contract_scan.py",
    "scripts/perf/discovery-load.mjs",
    "functions/src/health.ts",
    "functions/src/chatMediaOrphanPolicy.ts",
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

    gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    for release_input in (
        "MATREE_APPLICATION_ID",
        "MATREE_VERSION_CODE",
        "MATREE_VERSION_NAME",
        "MATREE_KEYSTORE_PATH",
        "MATREE_KEYSTORE_PASSWORD",
        "MATREE_KEY_ALIAS",
        "MATREE_KEY_PASSWORD",
    ):
        if release_input not in gradle:
            fail(f"Android release configuration omits {release_input}", failures)

    for release_script in ("scripts/deploy/build-release.sh", "scripts/deploy/build-release.bat"):
        release_text = (ROOT / release_script).read_text(encoding="utf-8")
        if "MATREE_APPLICATION_ID" not in release_text or "com.match.app" not in release_text:
            fail(f"{release_script} must reject the generic production application id", failures)

    for rel in (
        "scripts/deploy/build-release.sh",
        "scripts/deploy/build-release.bat",
        "scripts/deploy/firebase-full.sh",
        "scripts/deploy/firebase-full.bat",
    ):
        deploy_text = (ROOT / rel).read_text(encoding="utf-8")
        if "MATREE_RELEASE_SHA" not in deploy_text or "release_source_guard.py" not in deploy_text:
            fail(f"{rel} must enforce the exact frozen MATREE_RELEASE_SHA", failures)

    firebase_deploy = (ROOT / "scripts/deploy/firebase-full.sh").read_text(encoding="utf-8")
    if "MATREE_FIREBASE_PROJECT_ID" not in firebase_deploy or "--project" not in firebase_deploy:
        fail("Firebase production deploy must require an explicit project id", failures)

    local_ignore = (ROOT / ".gitignore").read_text(encoding="utf-8")
    for release_secret_path in (
        "/app/src/release/google-services.json",
        "/app/google-services.json",
    ):
        if release_secret_path not in local_ignore:
            fail(f"release-only Firebase config must stay outside source control: {release_secret_path}", failures)

    external_gate = (ROOT / "scripts/ci/production_external_gate.py").read_text(encoding="utf-8")
    for external_contract in (
        "mainBranchProtectionVerified",
        "releaseIdentityVerified",
        "httpsAppLinksVerified",
        "firebaseRulesIndexesFunctionsDeployed",
        "firestoreAuthorizationAdversarialPassed",
        "storageAbuseMatrixPassed",
        "firebaseSecretsConfigured",
        "playServiceAccountApiVerified",
        "playRtdnVerified",
        "signedReleaseAabVerified",
        "supportedLocaleQaPassed",
        "screenCapturePhysicalMatrixPassed",
        "notificationQuietHoursDeviceMatrixPassed",
        "accountDeletionE2EPassed",
        "productionMonitoringAlertsVerified",
        "costBudgetAlertsVerified",
        "incidentResponseOwnerVerified",
        "closedTestingPassed",
        "disabledProviderSurfacesVerified",
    ):
        if external_contract not in external_gate:
            fail(f"external evidence contract omits launch blocker: {external_contract}", failures)

    media = (ROOT / "functions/src/media.ts").read_text(encoding="utf-8")
    if "cleanupAbandonedChatMedia" not in media or "chatMediaOrphans" not in media:
        fail("chat-media orphan cleanup contract is missing", failures)

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


    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    if not readme.startswith("# Matree"):
        fail("README must use the canonical Matree product identity", failures)
    if "# MatrimonyConnect" in readme:
        fail("README must not restore retired MatrimonyConnect branding", failures)

    for acceptance_doc, required_phrases in {
        "docs/release/SECURITY_ACCEPTANCE.md": ("App Check", "Firestore", "Storage", "OWASP MASVS"),
        "docs/release/REAL_DEVICE_ACCEPTANCE.md": ("two independent physical Android devices", "account deletion"),
        "docs/release/ACCESSIBILITY_PERFORMANCE_ACCEPTANCE.md": ("TalkBack", "release build"),
        "docs/release/INCIDENT_RESPONSE.md": ("P0", "exact Git SHA"),
    }.items():
        body = (ROOT / acceptance_doc).read_text(encoding="utf-8")
        for phrase in required_phrases:
            if phrase not in body:
                fail(f"{acceptance_doc} missing required production contract phrase: {phrase}", failures)

    if failures:
        print("Production handoff package scan FAILED")
        for item in failures:
            print(f"- {item}")
        return 1

    print("Production handoff package scan passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
