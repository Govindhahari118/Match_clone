#!/usr/bin/env bash
set -euo pipefail

# Production release build. Run from MatchApp/.
: "${MATREE_VERSION_CODE:?MATREE_VERSION_CODE is required}"
: "${MATREE_VERSION_NAME:?MATREE_VERSION_NAME is required}"

if [[ ! -f app/src/release/google-services.json && ! -f app/google-services.json ]]; then
  echo "[ERROR] Production google-services.json is required; the debug placeholder is not allowed." >&2
  exit 1
fi

if [[ ! -f keystore.properties ]]; then
  : "${MATREE_KEYSTORE_PATH:?MATREE_KEYSTORE_PATH is required when keystore.properties is absent}"
  : "${MATREE_KEYSTORE_PASSWORD:?MATREE_KEYSTORE_PASSWORD is required}"
  : "${MATREE_KEY_ALIAS:?MATREE_KEY_ALIAS is required}"
  : "${MATREE_KEY_PASSWORD:?MATREE_KEY_PASSWORD is required}"
  [[ -f "$MATREE_KEYSTORE_PATH" ]] || {
    echo "[ERROR] MATREE_KEYSTORE_PATH does not exist." >&2
    exit 1
  }
fi

./gradlew --no-daemon testReleaseUnitTest lintRelease bundleRelease

python3 scripts/ci/release_candidate_scan.py \
  --merged-manifest app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml
python3 scripts/ci/release_artifact_audit.py app/build/outputs/bundle/release/*.aab

echo "Production release bundle validated."
echo "Version: $MATREE_VERSION_NAME ($MATREE_VERSION_CODE)"
sha256sum app/build/outputs/bundle/release/*.aab
