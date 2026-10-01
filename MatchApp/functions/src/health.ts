import * as crypto from "crypto";
import * as functions from "firebase-functions/v1";
import { db } from "./shared";

function requestId(req: functions.https.Request): string {
  const incoming = req.header("x-request-id")?.trim();
  if (incoming && incoming.length <= 128) return incoming;
  return crypto.randomUUID();
}

type HeaderResponse = {
  set(field: string, value: string): unknown;
};

function baseHeaders(res: HeaderResponse, id: string): void {
  res.set("Cache-Control", "no-store");
  res.set("X-Request-Id", id);
}

/**
 * Liveness is intentionally dependency-free: a running Functions runtime should answer even when
 * Firestore is degraded, allowing operators to distinguish process health from dependency health.
 */
export const healthLive = functions
  .runWith({ timeoutSeconds: 5, memory: "128MB" })
  .https.onRequest((req, res) => {
    const id = requestId(req);
    baseHeaders(res, id);
    res.status(200).json({
      status: "ok",
      check: "live",
      requestId: id,
      service: "matree-functions",
    });
  });

/**
 * Readiness proves that the trusted backend can currently reach Firestore. The probe intentionally
 * reads no user collection and returns no environment, secret, project or account identifiers.
 */
export const healthReady = functions
  .runWith({ timeoutSeconds: 5, memory: "128MB" })
  .https.onRequest(async (req, res) => {
    const id = requestId(req);
    baseHeaders(res, id);
    const startedAt = Date.now();

    try {
      await db.collection("_readiness").limit(1).get();
      res.status(200).json({
        status: "ok",
        check: "ready",
        requestId: id,
        dependencies: { firestore: "ok" },
        latencyMs: Date.now() - startedAt,
      });
    } catch (error) {
      functions.logger.error("Readiness probe failed", {
        requestId: id,
        dependency: "firestore",
        error: error instanceof Error ? error.message : String(error),
      });
      res.status(503).json({
        status: "unavailable",
        check: "ready",
        requestId: id,
        dependencies: { firestore: "unavailable" },
      });
    }
  });
