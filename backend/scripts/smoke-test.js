/* eslint-disable no-console */

const DEFAULT_BASE_URL = "http://localhost:5000";
const DEFAULT_OTP = "123456";

function stripTrailingSlash(value) {
  return String(value || "").replace(/\/+$/, "");
}

function withTimestampSuffix(prefix) {
  return `${prefix}${Date.now().toString().slice(-8)}`;
}

async function parseResponse(response) {
  const raw = await response.text();
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const body = await parseResponse(response);
  return { status: response.status, ok: response.ok, body };
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

async function runSmokeTest() {
  const baseUrl = stripTrailingSlash(process.env.BACKEND_URL || process.env.SMOKE_BASE_URL || DEFAULT_BASE_URL);
  const apiBase = `${baseUrl}/api`;
  const phone = process.env.SMOKE_PHONE || withTimestampSuffix("+199999");
  const otp = process.env.SMOKE_OTP || DEFAULT_OTP;

  console.log(`Running smoke test against ${baseUrl}`);
  console.log(`Using phone ${phone}`);

  const health = await requestJson(`${baseUrl}/health`);
  assert(health.ok, `Health check failed (${health.status}): ${JSON.stringify(health.body)}`);
  console.log("Health check passed.");

  const otpRequest = await requestJson(`${apiBase}/auth/request-otp`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phone, type: "signup" }),
  });
  assert(otpRequest.ok, `OTP request failed (${otpRequest.status}): ${JSON.stringify(otpRequest.body)}`);
  const otpId = otpRequest.body?.otp_id;
  console.log("OTP request passed.");

  const otpVerify = await requestJson(`${apiBase}/auth/verify-otp`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phone, otp, otp_id: otpId }),
  });
  assert(otpVerify.ok, `OTP verify failed (${otpVerify.status}): ${JSON.stringify(otpVerify.body)}`);
  const accessToken = otpVerify.body?.access_token;
  assert(accessToken, "OTP verify succeeded but no access_token returned.");
  console.log("OTP verification passed.");

  const matches = await requestJson(`${apiBase}/matches?limit=5`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
  assert(matches.ok, `Matches request failed (${matches.status}): ${JSON.stringify(matches.body)}`);

  const body = matches.body;
  const hasValidShape =
    Array.isArray(body) ||
    (body && Array.isArray(body.items)) ||
    (body && Array.isArray(body.matches));
  assert(hasValidShape, `Matches response has unexpected shape: ${JSON.stringify(body)}`);
  console.log("Matches listing passed.");

  console.log("Smoke test completed successfully.");
}

runSmokeTest().catch((error) => {
  console.error("Smoke test failed:", error.message);
  process.exit(1);
});
