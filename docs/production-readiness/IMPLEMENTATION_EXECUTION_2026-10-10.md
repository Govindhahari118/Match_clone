# Matree implementation and launch execution

Status: implementation in draft PR #60; **production NO-GO**.

Repository: Govindhahari118/Match_clone. Baseline: main
`09e1019769cfd839740737a06ad41aaec4f4be9e`. Release issue: #26.
This plan supersedes stale PR integration instructions; it does not certify deployment or release.

## Implemented in the integration candidate

- Combined the useful changes from #50, #56 and #43 on the current baseline, preserving
  Storage upload metadata authorization from #59 and the audited Functions dependency lockfile.
- Preserved conversation starters, chat mute/archive, profile video gating and missing-photo requests.
- Added PUBLIC / ACCEPTED_ONLY / HIDDEN published-photo controls, trusted request/approval
  callables, Storage authorization, and Android privacy/detail interfaces.
- Separated `photoAccessRequests` from `photoRequests`: asking someone to upload a photo must
  never overwrite a request to view an existing protected photo. This new collection has no
  prior production migration assumption; confirm whether any old PR was deployed before rollout.
- Prevented clients from deleting/removing/changing the server-owned photo visibility field.
  Unrecognized explicit visibility fails closed; the legacy missing value remains PUBLIC.
- Photo access defaults to denied when the authorization lookup fails. Storage remains the
  byte-level authority; downloaded media cannot be recalled from a recipient's device.
- Added matched call scheduling, accept/decline/cancel, participant checks, bounded proposed times,
  replacement cooldown, request revision IDs and Android state/error handling.
- Rechecked call relationship eligibility within mutation transactions so concurrent block,
  account or privacy changes invalidate and retry authorization reads.
- Moved call notification delivery to a retryable Firestore trigger. A committed mutation no
  longer reports failure solely because push failed; stale revisions are suppressed.
- Ported missing Android call notification routing and its tests from #51; inspected #53 overlap.
- Denied call-request reads for blocked/inactive participants, removed request state on blocking,
  and added cleanup for actual requester/target fields during account deletion.
- Added per-installation notification locale, language-change registration and localized durable
  notification cards. Call, missing-photo and photo-access events also receive EN/HI/TE copy.
- Retained every existing release gate. No provider readiness or external evidence was fabricated.

## Review and validation contract

Local checks cover Functions lint/build/tests, existing high/critical dependency-audit threshold,
and all source contract scans. Local Node is 24; authoritative Functions CI uses Node 22.
Android SDK and Java 21 are unavailable locally, so the full Production CI is required.
Nine inherited moderate production dependency findings remain for triage; high/critical audit
PASS is not a zero-vulnerability claim.

The first PR #60 CI run (`38053227961`) passed the Functions job and found two Rules test failures.
The contact rule regression was reverted; the new photo-access fixture was corrected to seed
active users. A fresh full run on the final head is mandatory. Do not certify the first run.

| Stage | Owner | Required result | Current state |
| --- | --- | --- | --- |
| Source integration | Engineering | Current main plus consolidated changes; no lost security fixes | Implemented in draft |
| Repository checks | CI / engineering | All four Production CI jobs PASS on exact final head | PASS, run `38053689535`, exact SHA `2aea8f4` |
| Independent review | Security / Android | Photo grants, transaction races, notifications and Kotlin review | Pending |
| PR consolidation | Engineering | Feature parity documented before older PRs are superseded | Candidate #60; originals open |
| Main governance | Repository admin | PRs, exact required checks, approval and no unchecked bypass | BLOCKED / unverified; GitHub integration returned 403 for branch-protection endpoint; prior issue #26 handoff says disabled |
| Production release | Release owner | All actual external evidence accepted on frozen SHA | Blocked |

## Step-by-step remaining execution

1. **Validate integration.** COMPLETE for PR #60 head `2aea8f4`: all four exact-head CI jobs
   passed; Android unit/lint/R8, Room/privacy instrumentation, Functions tests/audit and
   Firestore/Storage emulator tests are green. Re-run after any source change. Real-device checks
   for cached-media behavior, account/locale/token switching and full user journey remain part of stages 8–9.
2. **Review and consolidate.** PENDING independent security/Android review on #60. Feature changes
   from #50/#56/#43 and missing routes/tests from #51/#53 are integrated, but originals remain
   open until an independent parity review. Repository administrator must verify/protect main,
   then merge only the qualified reviewed candidate and verify all four jobs on new main.
3. **Confirm launch scope.** Live voice/video is a launch-parity requirement in KNOWN_ISSUES.md.
   Current external gates require real provider, webhook and device evidence. Keep transport
   disabled until a vetted provider is selected, server session allocation and authenticated,
   replay-safe webhooks are implemented, and two-device testing passes. Scheduling is not media.
4. **Freeze release identity.** Set final package, domain, version and signing identity. Freeze
   the reviewed commit only after integration. Every later source change creates a new candidate.
5. **Provision Firebase.** Configure a dedicated production project and actual exposed Auth
   providers; register signing fingerprints. Keep config/secrets outside git. Deploy rules,
   indexes and Functions from the frozen commit with explicit project selection. Verify health,
   authorization, rollback IDs, least-privilege admin roles and approved master data.
6. **Verify App Check and links.** Validate signed device Play Integrity tokens before enforcing
   App Check. Publish real Digital Asset Links with the Play signing certificate; test cold and
   warm links, wrong host, wrong account and revoked sessions.
7. **Verify Play Billing.** Configure mapped products/pricing, Developer API and RTDN. Licensed
   signed-build tests must cover success, pending, cancel, decline, restore, renewal, expiry,
   grace, refund/revoke, wrong user, duplicate/reordered events and device/account switches.
   Server entitlements must match Play authority.
8. **Build and test the signed artifact.** Create AAB from the frozen SHA with protected signing
   inputs and production Firebase config. Record signature/hash. Run two real devices/accounts:
   fresh/upgrade install, authentication, profile, discovery, interests, match, shortlist, chat,
   media, notifications, photo grant/revoke, block, export, deletion and account switching.
9. **Qualify resilience and security.** Exercise slow/offline/reconnect, token expiry, interrupted
   upload, duplicate tap, process death, reinstall, permission denial, quiet hours, screenshot and
   recents privacy. Run independent authorization/penetration review, EN/HI/TE accessibility,
   large text and release performance on a lower-tier device. File specific reproducible P0/P1
   defects; fix and requalify the new SHA.
10. **Finish legal and closed testing.** Finalize actual privacy/terms/deletion/support disclosures,
    Data Safety, content rating and store listing. Upload the same signed candidate to internal/
    closed testing; clear Play prelaunch findings and record approvals.
11. **Pass prelaunch.** Configure monitoring/alerts/budgets and incident ownership, then execute
    rollback/kill-switch drill. Supply genuine operator evidence for all 49 prelaunch fields.
    Run `production_external_gate.py --mode prelaunch` on the frozen SHA. Missing stays blocked.
12. **Roll out and close.** Use a controlled staged rollout with stop thresholds for auth,
    crash/ANR, privacy, billing, Functions and support incidents. Capture rollout/health evidence
    and pass the two additional full-release fields. Close #26 only after `--mode full` PASS.

## Safe resumption

Start from PR #60's latest remote head and its exact-SHA CI. Read this plan, KNOWN_ISSUES.md,
GO_NO_GO.md and the external-evidence template. Do not restart already integrated features.
Do not merge/deploy a failed candidate, silently close overlapping PRs, enable unvalidated
optional surfaces, fabricate external approvals or describe green code tests as production GO.
