#!/usr/bin/env bash
set -euo pipefail

# Production Firebase deploy. Run from MatchApp/.
: "${MATREE_RELEASE_SHA:?MATREE_RELEASE_SHA is required}"
python3 scripts/ci/release_source_guard.py --sha "$MATREE_RELEASE_SHA"

: "${MATREE_FIREBASE_PROJECT_ID:?MATREE_FIREBASE_PROJECT_ID is required}"

project="$MATREE_FIREBASE_PROJECT_ID"
echo "Target Firebase project: $project"

firebase deploy --non-interactive --project "$project" --only firestore:rules
firebase deploy --non-interactive --project "$project" --only firestore:indexes
firebase deploy --non-interactive --project "$project" --only storage
firebase deploy --non-interactive --project "$project" --only functions

if [[ "${MATREE_DEPLOY_HOSTING:-0}" == "1" ]]; then
  firebase deploy --non-interactive --project "$project" --only hosting
fi

echo "Firebase deploy complete for $project."
