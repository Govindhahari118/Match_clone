# Matree secrets and sensitive configuration specification

No production secret belongs in Git, Android resources, checked-in JSON, generated release evidence
or client-visible Remote Config.

## Secret/configuration classes

| Class | Storage / owner | Notes |
|---|---|---|
| Release signing keys/passwords | Play App Signing / protected CI secret store | Never commit keystore or passwords |
| Release Firebase client config | release pipeline input | `google-services.json` is configuration, but production copy stays outside this repository |
| Firebase deploy credentials | CI/workload identity | Least privilege; never packaged into Android |
| Google Play Android Publisher credentials | backend/deployment secret store | Used only by trusted purchase verification/reconciliation |
| KYC/Aadhaar/provider credentials | approved backend secret store | Provider activation remains external/legal gate |
| App Check/Play Integrity configuration | Firebase/Play consoles | Do not hardcode bypasses |
| Operator bootstrap authorization | controlled operational process | Bootstrap writes audited role claims; no static admin password |
| Production legal/support contact values | release configuration/content | Must be approved before publication |

## Repository-safe configuration

The following may be committed because they contain no production secret: Firebase Rules, indexes,
Remote Config key names/defaults, public package IDs, feature flag names, API contracts, and templates
whose production values are blank/false.

## Current Cloud Functions secret names

The Google Play verification runtime expects these deployment secrets:

- `GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL`
- `GOOGLE_PLAY_PRIVATE_KEY`

They are backend-only and must never be exposed to Android, Remote Config, logs or release artifacts.

## Cloud Functions runtime configuration

The code currently reads `security.enforce_app_check` as the staged callable-enforcement switch.
Production must only set this to `true` after valid release traffic is observed and the App Check
device matrix passes.

Any future provider credential must be read from the platform secret/config service and must not be
added as a literal to source.

## Logging redaction rules

Never log raw purchase tokens, FCM tokens, OTP codes, government-ID numbers, KYC files, exact location,
message bodies, signing material or provider secrets. Operations views return privacy-safe identifiers
and metadata only.
