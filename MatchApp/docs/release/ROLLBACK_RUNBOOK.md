# Matree production rollback and kill-switch runbook

This runbook is for the exact production release candidate. It does not replace operator access,
provider dashboards, incident command or legal/security escalation.

## Immediate decision order

1. **Protect members first.** If there is a privacy, authorization, fraud, KYC, payment or unsafe-media
   regression, stop the affected capability before investigating convenience or conversion impact.
2. **Use a narrow kill switch** when the affected capability already has a trusted Remote Config flag.
3. **Use maintenance mode** only for broad service degradation where normal member operations cannot
   remain safe.
4. **Halt Play rollout / roll back the release** for defects in always-on core paths that cannot be
   safely isolated by a feature flag.
5. Preserve logs, exact Git SHA, AAB hash, Remote Config version, Firebase deployment version and
   incident timestamps.

## Repository-defined kill switches

All optional switches below default fail-closed. A failed/missing fetch must not enable them.

| Capability | Remote Config key | Emergency value |
|---|---|---:|
| Nearby | `enable_nearby` | `false` |
| Kundali | `enable_kundali` | `false` |
| NRI discovery | `enable_nri_features` | `false` |
| Video profiles | `show_video_profiles` | `false` |
| Voice calls | `enable_voice_calls` | `false` |
| Community | `enable_community` | `false` |
| AI icebreakers | `enable_ai_icebreakers` | `false` |
| Daily reward | `daily_reward_enabled` | `false` |

For a broad service incident, set `maintenance_mode=true` with a truthful, non-sensitive maintenance
message. Do not use maintenance mode to conceal a single-feature failure that can be safely isolated.

## Always-on core incident actions

For authentication, discovery, interests, chat, account lifecycle, moderation, billing, verification,
or security-rule regressions that cannot be isolated safely:

- stop or pause the Play staged rollout;
- restore the last known-good application release when appropriate;
- restore/deploy the last known-good Firebase Rules/Functions/indexes only after checking schema and
  data compatibility;
- revoke compromised operator/member sessions when required;
- suspend risky external provider processing rather than fabricating success;
- do not downgrade authorization rules merely to restore availability.

## Payment incident

Do not grant local premium as a workaround. Keep Google Play + trusted backend verification as the
only entitlement authority. Pause promotional rollout, preserve purchase/reconciliation evidence and
repair/replay the idempotent ledger after the provider path is healthy.

## Verification/KYC incident

Stop new verification intake or keep the route unavailable if provider/reviewer safety cannot be
maintained. Raw KYC artifacts must remain client-unreadable. Never convert a provider outage into an
automatic verified result.

## Exit criteria

Resume rollout or re-enable a switch only when:

- the corrective commit has all mandatory Production CI jobs green on its exact SHA;
- the affected real-device/provider journey has been rerun;
- required external evidence is attached to that SHA;
- no open P0/P1 remains for the re-enabled capability;
- monitoring has a named owner and rollback threshold.

Record the rollback drill/evidence reference in the external production evidence file.
