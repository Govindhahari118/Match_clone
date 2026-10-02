# Matree production configuration template

Copy this document into the operator-controlled release record and replace only the values that are
owned by deployment/release operations. Do not commit the completed production copy.

## Release identity

- Git SHA: `<exact 40-character release SHA>`
- Export the same value as `MATREE_RELEASE_SHA`; production build/deploy scripts reject any HEAD mismatch or dirty source tree.
- Android application ID: set `MATREE_APPLICATION_ID=<final reverse-DNS package>`; production build scripts reject the generic `com.match.app`.
- Version code: set `MATREE_VERSION_CODE=<positive integer greater than every prior Play upload>`
- Version name: set `MATREE_VERSION_NAME=<release version>`
- Firebase production project: `<external>`
- Google Play application: must exactly match `MATREE_APPLICATION_ID` and the package registered in the production Firebase configuration.

## Android release signing inputs

The release build accepts a protected CI environment or local uncommitted `keystore.properties`.
For CI, supply all four together:

- `MATREE_KEYSTORE_PATH`
- `MATREE_KEYSTORE_PASSWORD`
- `MATREE_KEY_ALIAS`
- `MATREE_KEY_PASSWORD`

The Gradle configuration rejects partial signing input. The production build script also refuses to
build without a production `google-services.json` and explicit version inputs.

## Firebase deployment target

Set `MATREE_RELEASE_SHA` to the frozen release commit and `MATREE_FIREBASE_PROJECT_ID` to the exact production project before using
`scripts/deploy/firebase-full.sh` or `.bat`. The scripts always pass `--project` explicitly and
never trust an ambient Firebase CLI project selection. Set `MATREE_DEPLOY_HOSTING=1` only when the
operator console is approved for that deployment.

## Backend secrets

Secret **names** are part of the code contract; values must be supplied through the deployment secret
store and never written here.

- `GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL`
- `GOOGLE_PLAY_PRIVATE_KEY`
- any approved KYC/Aadhaar/biometric provider credentials introduced by a future validated adapter

## Callable security configuration

- `security.enforce_app_check=false` during validated pre-enforcement observation.
- Change to `true` only after the exact release's App Check / Play Integrity device matrix passes.
- `security.enforce_ops_mfa=false` until every production operator account is enrolled and the
  console MFA matrix passes; then set it to `true` before production moderation access.

## Remote Config safe baseline

Start from the source defaults in `RemoteConfigManager.kt`. Optional/provider-dependent capabilities
must remain OFF unless their exact-release evidence has passed.

| Key | Safe release baseline |
|---|---:|
| `show_video_profiles` | false |
| `enable_voice_calls` | false |
| `enable_community` | false |
| `enable_nri_features` | false |
| `enable_ai_icebreakers` | false |
| `daily_reward_enabled` | false |
| `enable_nearby` | false |
| `enable_kundali` | false |
| `maintenance_mode` | false |
| `force_update_version_code` | 0 until release-owner decision |
| `recommended_update_version_code` | 0 until release-owner decision |

Operational numeric defaults are documented in `FEATURE_FLAGS.md` and must not be changed merely to
make a release test pass.

## Google Play products expected by backend

The product IDs must exist in Play Console and match the backend mapping exactly:

- `match_silver_3m`
- `match_gold_6m`
- `match_platinum_12m`
- `match_boost_3h`
- `match_boost_24h`
- `match_boost_7d`

Pricing is a Play Console/operator decision; the client/backend do not accept price or duration from
the device as authority.

## Firebase deployment set

Deploy from the exact release SHA:

- Firestore Rules: `firestore.rules`
- Firestore indexes: `firestore.indexes.json`
- Storage Rules: `storage.rules`
- Functions: `functions/`
- operator Hosting: `ops-console/` when operations rollout is approved

## Production URLs / declarations

Operator-controlled values:

- Privacy Policy URL: `<external>`
- Terms URL: `<external>`
- Support URL/contact: `<external>`
- Account deletion information URL: `<external>`
- verified App Links domain / Digital Asset Links: `<external or disabled>`

## Promotion rule

This template is configuration guidance, not evidence. The completed production values and their
deployment/test references must be recorded in the external evidence file for the exact SHA and pass
`production_external_gate.py`.
