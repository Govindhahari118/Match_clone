# Matree release-candidate known issues and deferred surface

This file records release-relevant gaps without converting missing external evidence into a code defect.

## Repository-controlled P0/P1

No intentionally exposed repository-controlled P0/P1 is accepted. A failing mandatory CI gate or a newly discovered critical defect immediately makes the candidate NO-GO until fixed or the affected surface is fail-closed.

The legacy Room fields `passwordHash` and `isSeed` remain inert compatibility columns in database v24. They must not become authentication or seed-data authority. Removal is deferred until an authoritative generated Room schema is captured and a lossless v24 -> v25 table-rebuild migration is validated across every supported upgrade path.

## Intentionally disabled / deferred

- Nearby: BETA, OFF by default until real-device location/security/load evidence passes.
- Kundali: BETA, OFF by default until calculation/provider validation passes.
- NRI-specific promoted surface: BETA, OFF by default pending market/load/device evidence.
- Profile video: hardened backend/media moderation exists, but broad exposure remains flag-gated.
- Unsafe or post-launch source surfaces listed by `screen-classification.json` remain hidden.
- Production package/application identity and App Link hostname are explicit release inputs. HTTPS routing is implemented, while domain ownership, Digital Asset Links publication, Play-signing fingerprint binding and device verification remain external evidence. The legacy custom scheme is compatibility-only and must not be treated as verified App Links.

These are not permitted to be described as production-ready simply because source files exist.

## External release blockers

The release remains NO-GO until operator-controlled evidence exists for the exact candidate SHA covering at least:

- protected `main` and required CI;
- final package/product identity and verified HTTPS App Links;
- production Firebase configuration/deployment, secrets and App Check / Play Integrity;
- production signing, Play App Signing, catalog, API and RTDN;
- licensed billing matrix;
- physical-device core journeys, screenshot/privacy behavior, notification quiet hours and account deletion;
- Firestore/Storage adversarial authorization testing and penetration/security review;
- accessibility, supported-locale and release-build performance acceptance;
- Crashlytics/monitoring, cost budgets/alerts and named incident ownership;
- legal, privacy policy, Data Safety, store listing/content rating and support escalation;
- Play pre-launch report, closed testing, rollback drill and controlled rollout/post-rollout health.

The external gate must remain red until those references exist. Missing provider credentials or external evidence must never be converted into a synthetic pass.
