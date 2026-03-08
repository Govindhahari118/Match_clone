const STATIC_CACHE = "match-static-v2";
const RUNTIME_CACHE = "match-runtime-v2";
const OFFLINE_FALLBACKS = ["/manifest.json"];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => cache.addAll(OFFLINE_FALLBACKS)).catch(() => undefined),
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key !== STATIC_CACHE && key !== RUNTIME_CACHE)
          .map((key) => caches.delete(key)),
      ),
    ),
  );
  self.clients.claim();
});

function isGetRequest(request) {
  return request && request.method === "GET";
}

function isNavigationRequest(request) {
  return request && request.mode === "navigate";
}

function isAssetRequest(url) {
  return (
    url.pathname.startsWith("/_next/") ||
    url.pathname.startsWith("/icons/") ||
    url.pathname.startsWith("/images/") ||
    /\.(?:css|js|mjs|png|jpg|jpeg|gif|svg|webp|ico|woff|woff2|ttf|otf)$/i.test(url.pathname)
  );
}

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (!isGetRequest(request)) return;

  const url = new URL(request.url);
  const isSameOrigin = url.origin === self.location.origin;
  const isApiRequest = url.pathname.startsWith("/api/") || url.origin.includes("localhost:4000");

  if (isApiRequest) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const clone = response.clone();
          caches.open(RUNTIME_CACHE).then((cache) => cache.put(request, clone)).catch(() => undefined);
          return response;
        })
        .catch(() => caches.match(request).then((cached) => cached || Response.error())),
    );
    return;
  }

  if (isNavigationRequest(request) && isSameOrigin) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const clone = response.clone();
          caches.open(RUNTIME_CACHE).then((cache) => cache.put(request, clone)).catch(() => undefined);
          return response;
        })
        .catch(() => caches.match(request).then((cached) => cached || Response.error())),
    );
    return;
  }

  if (isSameOrigin) {
    if (!isAssetRequest(url)) {
      event.respondWith(fetch(request).catch(() => caches.match(request).then((cached) => cached || Response.error())));
      return;
    }

    event.respondWith(
      caches.match(request).then((cached) => {
        const networkFetch = fetch(request)
          .then((response) => {
            const clone = response.clone();
            caches.open(RUNTIME_CACHE).then((cache) => cache.put(request, clone)).catch(() => undefined);
            return response;
          })
          .catch(() => cached || Response.error());
        return cached || networkFetch;
      }),
    );
  }
});
