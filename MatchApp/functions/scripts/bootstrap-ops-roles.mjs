#!/usr/bin/env node
import process from "node:process";
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";

const ALLOWED_ROLES = new Set([
  "support",
  "moderator",
  "kyc_reviewer",
  "payment_ops",
  "ops_admin",
]);

function usage() {
  console.log(`Usage:
  npm run ops:bootstrap -- --project <firebase-project-id> --uid <firebase-uid> \\
    --roles support,moderator --approval <change-ticket> [--apply]

Dry-run is the default. Set MATREE_BOOTSTRAP_ACTOR to the human/operator identity initiating
the approved change. Use --roles none to remove all Matree operations roles.

ops_admin requires --allow-ops-admin in addition to --apply and an approval reference.`);
}

function parseArgs(argv) {
  const out = { apply: false, allowOpsAdmin: false };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--apply") out.apply = true;
    else if (arg === "--allow-ops-admin") out.allowOpsAdmin = true;
    else if (arg.startsWith("--")) {
      const value = argv[i + 1];
      if (!value || value.startsWith("--")) throw new Error(`Missing value for ${arg}`);
      out[arg.slice(2)] = value;
      i += 1;
    } else {
      throw new Error(`Unexpected argument: ${arg}`);
    }
  }
  return out;
}

function requireSafeId(value, label) {
  const text = typeof value === "string" ? value.trim() : "";
  if (!text || text.length > 128 || text.includes("/")) {
    throw new Error(`Invalid ${label}`);
  }
  return text;
}

function parseRoles(value) {
  const text = typeof value === "string" ? value.trim() : "";
  if (!text) throw new Error("--roles is required");
  if (text.toLowerCase() === "none") return [];
  const roles = [...new Set(text.split(",").map((item) => item.trim()).filter(Boolean))];
  for (const role of roles) {
    if (!ALLOWED_ROLES.has(role)) throw new Error(`Unsupported role: ${role}`);
  }
  return roles.sort();
}

async function main() {
  let args;
  try {
    args = parseArgs(process.argv.slice(2));
  } catch (error) {
    console.error(error.message);
    usage();
    process.exitCode = 2;
    return;
  }

  const projectId = requireSafeId(args.project, "project id");
  const uid = requireSafeId(args.uid, "Firebase UID");
  const approval = typeof args.approval === "string" ? args.approval.trim() : "";
  const actor = (process.env.MATREE_BOOTSTRAP_ACTOR || "").trim();
  const roles = parseRoles(args.roles);

  if (args.apply) {
    if (approval.length < 6 || approval.length > 200) {
      throw new Error("--approval must identify the approved change record");
    }
    if (actor.length < 3 || actor.length > 200) {
      throw new Error("MATREE_BOOTSTRAP_ACTOR must identify the human/operator applying the change");
    }
    if (roles.includes("ops_admin") && !args.allowOpsAdmin) {
      throw new Error("Granting ops_admin additionally requires --allow-ops-admin");
    }
  }

  initializeApp({ credential: applicationDefault(), projectId });
  const auth = getAuth();
  const db = getFirestore();
  const user = await auth.getUser(uid);
  const previousClaims = user.customClaims || {};
  const previousRoles = Array.isArray(previousClaims.roles)
    ? previousClaims.roles.filter((item) => typeof item === "string")
    : [];

  const nextClaims = { ...previousClaims, roles };
  const summary = {
    projectId,
    uid,
    email: user.email || null,
    previousRoles,
    requestedRoles: roles,
    apply: args.apply,
    approval: approval || null,
    actor: actor || null,
  };

  if (!args.apply) {
    console.log(JSON.stringify(summary, null, 2));
    console.log("DRY RUN ONLY. Re-run with --apply after approved review.");
    return;
  }

  await auth.setCustomUserClaims(uid, nextClaims);
  await auth.revokeRefreshTokens(uid);
  await db.collection("opsBootstrapAudit").add({
    uid,
    previousRoles,
    nextRoles: roles,
    approval,
    actor,
    projectId,
    appliedAt: FieldValue.serverTimestamp(),
  });

  console.log(JSON.stringify({ ...summary, applied: true, refreshTokensRevoked: true }, null, 2));
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
