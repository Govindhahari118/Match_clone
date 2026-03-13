import axios from "axios";
import { clearAuthSession, getAccessToken, getRefreshToken, setAuthSession, getStoredUser } from "./authStorage";
import { enqueueOfflineRequest } from "./offlineQueue";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:5000/api";
const RESPONSE_CACHE_PREFIX = "match_api_cache_v1:";
const RESPONSE_CACHE_TTL_MS = 5 * 60 * 1000;

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 8000,
  headers: {
    "Content-Type": "application/json",
  },
});

function canUseSessionStorage() {
  return typeof window !== "undefined" && typeof window.sessionStorage !== "undefined";
}

function buildCacheKey(config = {}) {
  const params = config.params ? JSON.stringify(config.params) : "";
  const url = String(config.url || "");
  return `${RESPONSE_CACHE_PREFIX}${url}?${params}`;
}

function saveCachedResponse(config, response) {
  if (!canUseSessionStorage()) return;
  if (String(config.method || "get").toLowerCase() !== "get") return;
  try {
    const key = buildCacheKey(config);
    const payload = {
      ts: Date.now(),
      data: response?.data ?? null,
      status: response?.status ?? 200,
      headers: response?.headers ?? {},
    };
    window.sessionStorage.setItem(key, JSON.stringify(payload));
  } catch {
    // Best-effort cache.
  }
}

function readCachedResponse(config) {
  if (!canUseSessionStorage()) return null;
  try {
    const key = buildCacheKey(config);
    const raw = window.sessionStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!parsed?.ts || Date.now() - parsed.ts > RESPONSE_CACHE_TTL_MS) {
      window.sessionStorage.removeItem(key);
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

api.interceptors.request.use(
  (config) => {
    const token = getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

api.interceptors.response.use(
  (response) => {
    saveCachedResponse(response.config, response);
    return response;
  },
  async (error) => {
    const config = error?.config || {};
    const method = String(config.method || "get").toLowerCase();

    if (error.response && error.response.status === 401) {
      const isRefreshCall = String(config.url || "").includes("/auth/refresh");
      const alreadyRetried = Boolean(config.__retriedWithRefresh);

      if (!isRefreshCall && !alreadyRetried) {
        const refreshToken = getRefreshToken();
        const user = getStoredUser();
        if (refreshToken && user) {
          try {
            const refreshResponse = await api.post("/auth/refresh", { refresh_token: refreshToken });
            const newAccessToken = refreshResponse?.data?.access_token || refreshResponse?.data?.accessToken;
            const newRefreshToken = refreshResponse?.data?.refresh_token || refreshResponse?.data?.refreshToken;
            if (newAccessToken) {
              setAuthSession(user, newAccessToken, newRefreshToken || refreshToken);
              const retryConfig = {
                ...config,
                __retriedWithRefresh: true,
                headers: {
                  ...(config.headers || {}),
                  Authorization: `Bearer ${newAccessToken}`,
                },
              };
              return api.request(retryConfig);
            }
          } catch (_refreshError) {
            // fall through to session clear
          }
        }
      }

      clearAuthSession();
      return Promise.reject(error);
    }

    const isNetworkError = !error.response;
    if (!isNetworkError) {
      return Promise.reject(error);
    }

    if (method === "get") {
      const cached = readCachedResponse(config);
      if (cached) {
        return {
          data: cached.data,
          status: cached.status || 200,
          statusText: "OK (cached)",
          headers: cached.headers || {},
          config,
          request: null,
          fromCache: true,
        };
      }
      return Promise.reject(error);
    }

    if (["post", "put", "patch", "delete"].includes(method)) {
      const replayMode =
        config?.headers?.["x-offline-replay"] === "1" ||
        config?.headers?.["X-Offline-Replay"] === "1";
      if (replayMode) {
        return Promise.reject(error);
      }
      enqueueOfflineRequest(config);
      return {
        data: {
          queued: true,
          offline: true,
          message: "Request queued and will retry when online.",
        },
        status: 202,
        statusText: "Accepted (queued)",
        headers: {},
        config,
        request: null,
      };
    }

    return Promise.reject(error);
  },
);

export default api;
