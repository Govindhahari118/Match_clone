const AUTH_CHANGE_EVENT = "auth:changed";

function emitAuthChange() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
  }
}

export function getAccessToken() {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("accessToken") || localStorage.getItem("access_token");
}

export function getStoredUser() {
  if (typeof window === "undefined") return null;

  try {
    const rawUser = localStorage.getItem("user");
    return rawUser ? JSON.parse(rawUser) : null;
  } catch {
    return null;
  }
}

export function setAuthSession(userDetails, accessToken, refreshToken) {
  if (typeof window === "undefined") return;

  localStorage.setItem("user", JSON.stringify(userDetails));
  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("access_token", accessToken);

  if (refreshToken) {
    localStorage.setItem("refreshToken", refreshToken);
    localStorage.setItem("refresh_token", refreshToken);
  }

  emitAuthChange();
}

export function clearAuthSession() {
  if (typeof window === "undefined") return;

  localStorage.removeItem("user");
  localStorage.removeItem("accessToken");
  localStorage.removeItem("access_token");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("refresh_token");

  emitAuthChange();
}

export function onAuthStorageEvent(listener) {
  if (typeof window === "undefined") {
    return () => {};
  }

  const onStorage = (event) => {
    if (
      event.key === "user" ||
      event.key === "accessToken" ||
      event.key === "access_token" ||
      event.key === "refreshToken" ||
      event.key === "refresh_token"
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
