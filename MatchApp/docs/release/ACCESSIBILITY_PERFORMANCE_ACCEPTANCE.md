# Matree accessibility and performance acceptance

## Accessibility
Release acceptance must cover TalkBack labels/semantics, dynamic font scaling, touch targets, contrast, focus order, dialogs/bottom sheets, meaningful image descriptions and reduced-motion behavior where applicable.

## Performance
Measure a release build for:
- cold/warm startup;
- discovery first-content and pagination latency;
- profile rendering and image loading;
- large-chat history and reconnect;
- memory growth during long sessions;
- ANR/main-thread blocking;
- background battery/network activity;
- AAB size.

## Reliability
Review crash-free users/sessions, non-fatal backend failures and Functions error rate during closed testing. Configure alerts before staged production rollout.

## Evidence
Store the exact Git SHA, release version, device/API level, methodology, measurement window and raw report reference. Debug-build measurements cannot certify release performance.
