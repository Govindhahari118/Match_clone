# Matree production configuration template

Copy this document into the operator-controlled release record and replace only the values that are
owned by deployment/release operations. Do not commit the completed production copy.

## Release identity

- Git SHA: `<exact release SHA>`
- Android application ID: `com.match.app`
- Version code: `<release-owner decision>`
- Version name: `<release-owner decision>`
- Firebase production project: `<external>`
- Google Play application: `com.match.app`

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
