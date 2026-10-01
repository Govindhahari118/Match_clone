# Matree performance release gate

Performance is a **measured release gate**, not a source-code claim.

## Backend discovery targets

The frozen production target is:

| Metric | Required |
|---|---:|
| P50 | < 200 ms |
| P95 | < 500 ms |
| P99 | < 1000 ms |
| Request success | >= 95% during the gate run |

Run against the production-like staging deployment with realistic profile volume, Firestore indexes,
App Check and authentication enabled:

```bash
MATREE_DISCOVERY_URL="https://<region>-<project>.cloudfunctions.net/discoverProfiles" \
MATREE_ID_TOKEN="<short-lived test-user Firebase ID token>" \
MATREE_APP_CHECK_TOKEN="<short-lived App Check token>" \
MATREE_PERF_REQUESTS=100 \
MATREE_PERF_CONCURRENCY=10 \
MATREE_DISCOVERY_BODY='{"ageMin":24,"ageMax":34,"state":"Telangana","withPhotoOnly":true}' \
node MatchApp/scripts/perf/discovery-load.mjs
```

The script exits non-zero when the latency or success thresholds fail. Tokens must be injected by the
test environment and must never be committed or attached to release artifacts.

## Required scenarios

Measure combinations, not single empty queries:

1. Default discovery with bilateral partner preferences.
2. State + age + photo + verified filters.
3. Religion/community + mother tongue + marital-status filters.
4. NRI/residence + education/occupation filters.
5. Keyword lookup by username or Matrimony ID.
6. Strict partner-preference exclusion with a realistically sized candidate set.
7. Cold Functions instance and warmed steady state separately.

## Android/device evidence

On at least one lower-end supported physical device and one current Android device, record:

- cold start to interactive Home,
- Discover initial content and pagination,
- profile photo scrolling/memory behavior,
- chat list/open/send,
- process-death restoration,
- ANR/crash evidence from the staged cohort.

Repository CI can validate code, lint, Rules and migrations. It **cannot** certify live latency,
device rendering, production network behavior or provider latency. Attach the measured output to the
release evidence pack for the exact release SHA.
