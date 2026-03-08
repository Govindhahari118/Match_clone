"use client";

const STORAGE_KEY = "match_offline_queue_v1";
const MAX_QUEUE_SIZE = 100;
const MAX_AGE_MS = 24 * 60 * 60 * 1000;

function canUseStorage() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function readQueue() {
  if (!canUseStorage()) return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    const now = Date.now();
    return parsed.filter((item) => item && item.enqueuedAt && now - item.enqueuedAt < MAX_AGE_MS);
  } catch {
    return [];
  }
}

function writeQueue(queue) {
  if (!canUseStorage()) return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(queue.slice(-MAX_QUEUE_SIZE)));
  } catch {
    // Ignore storage failures to avoid blocking request flow.
  }
}

function normalizeHeaders(headers = {}) {
  const entries = Object.entries(headers || {});
  const out = {};
  for (const [key, value] of entries) {
    if (!key || value === undefined || value === null) continue;
    out[String(key)] = String(value);
  }
  return out;
}

export function enqueueOfflineRequest(config = {}) {
  const method = String(config.method || "get").toLowerCase();
  if (!["post", "put", "patch", "delete"].includes(method)) {
    return;
  }

  const entry = {
    id: `${Date.now()}_${Math.floor(Math.random() * 100000)}`,
    url: config.url,
    method,
    data: config.data ?? null,
    params: config.params ?? null,
    headers: normalizeHeaders(config.headers || {}),
    enqueuedAt: Date.now(),
  };

  const queue = readQueue();
  queue.push(entry);
  writeQueue(queue);
}

export function getOfflineQueueLength() {
  return readQueue().length;
}

export async function flushOfflineQueue(executor) {
  if (typeof executor !== "function") return { flushed: 0, failed: 0 };

  const queue = readQueue();
  if (queue.length === 0) return { flushed: 0, failed: 0 };

  const remaining = [];
  let flushed = 0;
  let failed = 0;

  for (const item of queue) {
    try {
      await executor({
        url: item.url,
        method: item.method,
        data: item.data,
        params: item.params,
        headers: item.headers,
      });
      flushed += 1;
    } catch {
      remaining.push(item);
      failed += 1;
    }
  }

  writeQueue(remaining);
  return { flushed, failed };
}
