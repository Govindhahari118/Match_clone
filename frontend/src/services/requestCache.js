const requestCache = new Map();

export async function getCachedRequest(cacheKey, fetcher, ttlMs = 60_000) {
  const now = Date.now();
  const cached = requestCache.get(cacheKey);

  if (cached?.promise) {
    return cached.promise;
  }

  if (cached?.value && cached.expiresAt > now) {
    return cached.value;
  }

  const pending = Promise.resolve(fetcher())
    .then((value) => {
      requestCache.set(cacheKey, { value, expiresAt: Date.now() + ttlMs });
      return value;
    })
    .catch((error) => {
      requestCache.delete(cacheKey);
      throw error;
    });

  requestCache.set(cacheKey, { promise: pending, expiresAt: now + ttlMs });
  return pending;
}

export function clearCachedRequest(cacheKeyPrefix = "") {
  if (!cacheKeyPrefix) {
    requestCache.clear();
    return;
  }

  for (const key of requestCache.keys()) {
    if (key.startsWith(cacheKeyPrefix)) {
      requestCache.delete(key);
    }
  }
}
