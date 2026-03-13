const AUTH_CHANGE_EVENT = "auth:changed";
const USER_KEY = "user";
const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";
const LEGACY_ACCESS_TOKEN_KEY = "access_token";
const LEGACY_REFRESH_TOKEN_KEY = "refresh_token";

function emitAuthChange() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
  }
}

export function getAccessToken() {
  if (typeof window === "undefined") return null;
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (token) return token;

  const legacyToken = localStorage.getItem(LEGACY_ACCESS_TOKEN_KEY);
  if (legacyToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, legacyToken);
    localStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY);
    return legacyToken;
  }

  return null;
}

export function getStoredUser() {
  if (typeof window === "undefined") return null;

  try {
    const rawUser = localStorage.getItem(USER_KEY);
    return rawUser ? JSON.parse(rawUser) : null;
  } catch {
    return null;
  }
}

export function setAuthSession(userDetails, accessToken, refreshToken) {
  if (typeof window === "undefined") return;

  localStorage.setItem(USER_KEY, JSON.stringify(userDetails));
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY);

  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.removeItem(LEGACY_REFRESH_TOKEN_KEY);
  } else {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(LEGACY_REFRESH_TOKEN_KEY);
  }

  emitAuthChange();
}

export function clearAuthSession() {
  if (typeof window === "undefined") return;

  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(LEGACY_REFRESH_TOKEN_KEY);

  emitAuthChange();
}

export function onAuthStorageEvent(listener) {
  if (typeof window === "undefined") {
    return () => {};
  }

  const onStorage = (event) => {
    if (
      event.key === USER_KEY ||
      event.key === ACCESS_TOKEN_KEY ||
      event.key === LEGACY_ACCESS_TOKEN_KEY ||
      event.key === REFRESH_TOKEN_KEY ||
      event.key === LEGACY_REFRESH_TOKEN_KEY
    ) {
      listener();
    }
  };

  window.addEventListener("storage", onStorage);
  window.addEventListener(AUTH_CHANGE_EVENT, listener);

  return () => {
    window.removeEventListener("storage", onStorage);
    window.removeEventListener(AUTH_CHANGE_EVENT, listener);
  };
}
