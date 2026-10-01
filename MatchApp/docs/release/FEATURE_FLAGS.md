# Matree feature flags and operational defaults

Source of truth: `app/src/main/java/com/match/app/core/config/RemoteConfigManager.kt`.

Remote Config failures must preserve the last activated values or source defaults; they must never
silently enable an optional feature.

| Key | Source default | Release classification |
|---|---:|---|
| `free_message_limit` | 5 | operational limit; trusted server safety ceilings still apply |
| `boost_duration_hours` | 24 | UI/config value; paid entitlement duration remains backend-owned |
| `max_daily_likes_free` | 20 | operational UI/config value |
| `show_video_profiles` | false | BETA, fail closed |
| `enable_voice_calls` | false | unsafe/post-launch, fail closed |
| `enable_community` | false | optional, fail closed unless promoted |
| `min_photos_for_boost` | 2 | operational |
| `maintenance_mode` | false | incident control |
| `maintenance_message` | maintenance-safe generic copy | incident control |
| `force_update_version_code` | 0 | release-owner controlled |
| `recommended_update_version_code` | 0 | release-owner controlled |
| `referral_bonus_premium_days` | 0 | retired/unlaunched rewards remain non-authoritative |
| `enable_nri_features` | false | BETA, fail closed |
| `max_profile_photos` | 6 | operational |
| `enable_ai_icebreakers` | false | post-launch, fail closed |
| `daily_reward_enabled` | false | post-launch, fail closed |
| `enable_nearby` | false | BETA, fail closed until location evidence passes |
| `enable_kundali` | false | BETA, fail closed until calculation/provider evidence passes |

## Authority rule

Remote Config can hide/enable user-facing optional capability. It does **not** grant premium,
verification, Trust, moderation, account status, payment, protected-media, location or messaging
authority. Those decisions remain on trusted backend/provider state.

## Promotion checklist

Before turning any BETA flag ON:

1. exact-head repository CI is green;
2. route-specific security/privacy tests pass;
3. physical-device/provider/load evidence exists where applicable;
4. telemetry/rollback is ready;
5. operator owner records the change and exit criteria;
6. the external release evidence references the validation.

If a capability cannot satisfy those conditions, keep it OFF rather than presenting a partial or
simulated implementation.
