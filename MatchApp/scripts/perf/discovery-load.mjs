#!/usr/bin/env node

const endpoint = process.env.MATREE_DISCOVERY_URL;
const idToken = process.env.MATREE_ID_TOKEN;
const appCheckToken = process.env.MATREE_APP_CHECK_TOKEN || "";
const requests = Math.max(5, Math.min(500, Number(process.env.MATREE_PERF_REQUESTS || 50)));
const concurrency = Math.max(1, Math.min(20, Number(process.env.MATREE_PERF_CONCURRENCY || 5)));
const body = JSON.parse(process.env.MATREE_DISCOVERY_BODY || "{}");

if (!endpoint || !idToken) {
  console.error("MATREE_DISCOVERY_URL and MATREE_ID_TOKEN are required.");
  process.exit(2);
}

function percentile(sorted, fraction) {
  if (sorted.length === 0) return 0;
  const index = Math.min(sorted.length - 1, Math.ceil(sorted.length * fraction) - 1);
  return sorted[index];
}

async function once(index) {
  const started = performance.now();
  const headers = {
    "content-type": "application/json",
    "authorization": `Bearer ${idToken}`,
    "x-request-id": `perf-${Date.now()}-${index}`,
  };
  if (appCheckToken) headers["x-firebase-appcheck"] = appCheckToken;

  const response = await fetch(endpoint, {
    method: "POST",
    headers,
    body: JSON.stringify({ data: body }),
  });
  const elapsed = performance.now() - started;
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${text.slice(0, 300)}`);
  }
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch {
    throw new Error("Discovery response was not JSON");
  }
  if (parsed.error) {
    throw new Error(`Callable error: ${JSON.stringify(parsed.error).slice(0, 300)}`);
  }
  return elapsed;
}

const latencies = [];
let next = 0;
let failed = 0;

async function worker() {
  while (true) {
    const index = next++;
    if (index >= requests) return;
    try {
      latencies.push(await once(index));
    } catch (error) {
      failed += 1;
      console.error(`request ${index} failed: ${error.message}`);
    }
  }
}

await Promise.all(Array.from({ length: concurrency }, worker));

latencies.sort((a, b) => a - b);
const result = {
  requested: requests,
  successful: latencies.length,
  failed,
  concurrency,
  p50Ms: Math.round(percentile(latencies, 0.50)),
  p95Ms: Math.round(percentile(latencies, 0.95)),
  p99Ms: Math.round(percentile(latencies, 0.99)),
  maxMs: Math.round(latencies.at(-1) || 0),
};
console.log(JSON.stringify(result, null, 2));

const enough = latencies.length >= Math.ceil(requests * 0.95);
const meetsTargets =
  result.p50Ms < 200 &&
  result.p95Ms < 500 &&
  result.p99Ms < 1000;

if (!enough || !meetsTargets) {
  console.error(
    "PERFORMANCE GATE FAILED: require >=95% success, P50<200ms, P95<500ms, P99<1000ms."
  );
  process.exit(1);
}
console.log("PERFORMANCE GATE PASSED");
