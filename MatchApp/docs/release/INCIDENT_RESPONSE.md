# Matree production incident response

## Severity
- P0: member safety/privacy/authorization/payment corruption, widespread outage or irreversible data risk.
- P1: major core-journey failure without immediate irreversible harm.
- P2: degraded non-critical capability or contained usability defect.

## P0 actions
1. assign incident commander and timestamp the incident;
2. protect members first: disable the narrow risky feature or halt rollout;
3. preserve exact Git SHA, AAB hash, Firebase deployment versions, Remote Config version and logs;
4. revoke compromised sessions/secrets when required;
5. use only last-known-good rules/functions compatible with current data;
6. communicate truthful service state; never fabricate provider success;
7. define recovery checks and monitor after mitigation.

## Recovery
Resume only after exact-head CI is green, the affected real-device/provider journey passes, required external evidence is attached, and monitoring/rollback ownership is explicit.

## Post-incident
Document root cause, impact window, affected data/users, corrective actions, regression tests and follow-up owner. Security/privacy incidents also follow applicable legal notification obligations.
