# Matree production security acceptance

This document is an executable release contract, not a claim that external testing has already passed.

## Mandatory repository evidence
- exact-head Production CI is green;
- Firestore and Storage emulator authorization suites pass;
- Functions production dependency audit has no high-severity production finding;
- release candidate scan and truthfulness scan pass;
- no release secrets are committed.

## Mandatory external security evidence
Before Production GO, attach exact-SHA evidence for:
- App Check / Play Integrity enforcement against the production Firebase project;
- unauthenticated, unrelated-user, blocked-user, deleted-user and owner authorization attempts;
- Firestore write-forgery attempts for profiles, interests, matches, chat, moderation and server-only fields;
- Storage ownership, content-type, size and overwrite abuse tests;
- deep-link/intent abuse tests;
- modified-client/replay/stale-token tests;
- production log review proving tokens, passwords, purchase tokens, private message bodies and exact location are not logged;
- dependency/secret scan and an OWASP MASVS-oriented mobile security review.

## Fail-closed rules
A provider outage, missing App Check token, invalid entitlement, failed verification provider or unavailable moderation dependency must never be translated into success. Security rules must not be weakened to restore availability.

## Evidence format
Every evidence item must identify the candidate Git SHA, app version, environment/project, capture time, operator/owner, test steps and result. Screenshots alone are insufficient when machine-readable logs or provider export data are available.
