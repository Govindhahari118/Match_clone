# Matree Play Store release handoff

This document separates repository-controlled release gates from external production evidence. A green repository build is a **release candidate**, not proof that the app has been published.

## Exact candidate evidence

For every candidate, use the Production CI run attached to the exact Git SHA. The Android job generates RELEASE_EVIDENCE.md and audits the merged release manifest and generated AAB. Do not reuse evidence from an older SHA.

## Required external actions

- Configure Play App Signing and keep signing material outside Git.
- Supply the production Firebase Android configuration for com.match.app through the protected release environment.
- Register release SHA-1/SHA-256 fingerprints and production authentication providers.
- Deploy the exact candidate's Firestore rules, Storage rules, indexes and Functions to the production Firebase project.
- Configure Play Integrity/App Check, observe valid release traffic, then enforce according to the rollout plan.
- Configure Google Play Billing products and licensed testers; verify purchase, pending, restore, renewal, expiry, refund/revocation and account binding.
- Verify production HTTPS App Links and publish the matching Digital Asset Links file before claiming verified links.
- Publish stable HTTPS Privacy Policy, Terms, support and account-deletion information.
- Complete Play Data Safety, app access, content rating, target audience, ads, sensitive-permission and account-deletion declarations from the shipped behavior.
- Upload the exact AAB, run Play pre-launch reports, fix P0/P1 defects, then use a staged production rollout.

## Physical-device acceptance

At minimum record fresh install, upgrade, process death, offline/reconnect, large fonts/TalkBack, foreground location permission variants, two-account/two-device matchmaking, chat/media, block revocation, FCM cold/warm routing, account switch cleanup and deletion retry. Record device/API versions and candidate SHA.

## Rollout observability

Monitor Crashlytics/ANR, authentication, onboarding/profile writes, discovery, interest/match transitions, chat/media, FCM, purchase reconciliation, deletion and Nearby failures. Halt rollout or disable an already feature-flagged optional feature when a material safety/privacy/reliability regression appears.


## External evidence gate

Copy `docs/release/PRODUCTION_EXTERNAL_EVIDENCE.template.json` to an operator-controlled
release-evidence location, set `gitSha` to the exact candidate, keep
`evidenceType=operator-controlled-production-evidence`, and attach a concrete artifact or
record reference for every gate. Do not commit credentials or sensitive test artifacts. Template,
placeholder and CI-synthetic provenance is rejected by the production gate unless the explicit
CI-only self-test flag is supplied.

Before Play production promotion:

```bash
python3 scripts/ci/production_external_gate.py \
  --evidence /secure/release-evidence/<sha>.json \
  --sha <exact-git-sha> \
  --mode prelaunch
```

After the staged rollout and post-rollout health review, the same exact SHA must pass `--mode full`.
A repository CI pass or a synthetically generated CI validator file is never external production
evidence.


## Operations readiness

Before production moderation/support/KYC/payment operations begin, provision role-scoped operator
accounts using `docs/release/OPS_ROLE_BOOTSTRAP.md`. The bootstrap tool is dry-run-first and writes a
server-only audit record; executing it against production remains an external evidence gate.

Use `docs/release/ROLLBACK_RUNBOOK.md` for incident rollback and feature-disable decisions. A real
production-like rollback drill must be attached to the exact release SHA before full rollout.
