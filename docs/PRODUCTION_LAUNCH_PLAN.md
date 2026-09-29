# Matree Production Launch Plan — Superseded Historical Record

> **Status: SUPERSEDED. Do not use this file as the current release plan.**
>
> The original May 7, 2026 plan described an earlier prototype architecture and included assumptions
> that are no longer true (including retired payment/provider paths, obsolete route exposure and
> historical security/verification gaps). Keeping those statements as if they were current would
> conflict with the hardened production contract.

The authoritative launch sources are now:

- `MatchApp/DEPLOYMENT_CHECKLIST.md` — executable production release gates and external/operator work.
- `docs/production-readiness/current-head.md` — exact-head repository evidence and remaining gates.
- `docs/production-readiness/route-inventory.md` — reachable, flagged and hidden production routes.
- `docs/production-readiness/screen-classification.json` — exhaustive Android screen classification,
  enforced by CI.
- `docs/production-readiness/architecture-authority-environments.md` — authority boundaries and
  environment ownership.
- `docs/production-readiness/performance-gate.md` — measured backend/device performance requirements.
- `MatchApp/docs/release/PRODUCTION_EXTERNAL_EVIDENCE.template.json` — machine-verified external
  evidence contract.
- `MatchApp/docs/release/ROLLBACK_RUNBOOK.md` — rollback and kill-switch procedure.

## Current release principle

Repository source is considered complete only when every repository-controlled P0/P1 item is either
implemented or intentionally fail-closed, and Production CI is green on the exact candidate SHA.

Production launch additionally requires the external evidence gate: real Firebase/Play/App Check
configuration, production signing, provider credentials where applicable, physical-device and
two-user/two-device journeys, accessibility and performance evidence, legal/Data Safety approval,
Play pre-launch evidence, penetration testing and rollback validation.

A historical green commit, source file, rendered screen or old checklist entry is never sufficient
release evidence for a later SHA.

## Historical content

The previous May 7, 2026 plan remains available in Git history for audit/reference. It must not be
copied forward as current architecture or launch evidence.
