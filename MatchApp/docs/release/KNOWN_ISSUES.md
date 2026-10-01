# Matree release-candidate known issues and deferred surface

This file records release-relevant gaps without converting missing external evidence into a code defect.

## Repository-controlled P0/P1

At the time this document was added, no intentionally exposed repository-controlled P0/P1 is accepted.
A failing mandatory CI gate or a newly discovered critical defect changes that statement immediately
and blocks release until fixed or the affected feature is fail-closed.

## Intentionally disabled / deferred

- Nearby: BETA, OFF by default until real-device location/security/load evidence passes.
- Kundali: BETA, OFF by default until calculation/provider validation passes.
- NRI-specific promoted surface: BETA, OFF by default pending market/load/device evidence.
- Profile video: hardened backend/media moderation exists, but broad exposure remains flag-gated.
- Unsafe or post-launch source surfaces listed by `screen-classification.json` remain hidden.

These are not permitted to be described as production-ready simply because source files exist.

## External release blockers

Production Firebase/config/deployment, release signing, App Check/Play Integrity enforcement, licensed
billing tests, KYC/provider credentials, operator provisioning/Hosting verification, physical-device E2E,
accessibility, performance SLO, penetration/security review, legal/Data Safety approval, Play pre-launch
report and rollback drill require operator evidence for the exact release SHA.

The external gate must remain red until those references exist.
