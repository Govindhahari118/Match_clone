"use client";

import { createContext, useContext, useEffect, useState, useSyncExternalStore } from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import io from "socket.io-client";
import api from "@/services/api";
import {
  clearAuthSession,
  getAccessToken,
  getStoredUser,
  onAuthStorageEvent,
  setAuthSession,
} from "@/services/authStorage";

const AuthContext = createContext();
const SOCKET_URL = process.env.NEXT_PUBLIC_SOCKET_URL || "http://localhost:4000";

function readStoredUser() {
  if (typeof window === "undefined") return null;

  const token = getAccessToken();
  const user = getStoredUser();
  if (!token || !user) return null;
  return user;
}

function subscribeAuth(listener) {
  return onAuthStorageEvent(listener);
}

function getAuthSnapshot() {
  return readStoredUser();
}

function getAuthServerSnapshot() {
  return null;
}

export const AuthProvider = ({ children }) => {
  const user = useSyncExternalStore(subscribeAuth, getAuthSnapshot, getAuthServerSnapshot);
  const loading = false;
  const [socket, setSocket] = useState(null);

  useEffect(() => {
    if (!user?.id && !user?.sub) return undefined;
    const userId = user.id || user.sub;
    const guardKey = `flag_exposure_sent:${userId}`;
    if (typeof window !== "undefined" && window.sessionStorage.getItem(guardKey)) {
      return undefined;
    }

    let cancelled = false;
    const syncFlags = async () => {
      try {
        const response = await api.get("/analytics/flags");
        const flags = Array.isArray(response?.data?.flags) ? response.data.flags : [];
        if (cancelled || flags.length === 0) return;
        await Promise.allSettled(
          flags.map((flag) =>
            api.post("/analytics/flags/exposure", {
              flagKey: flag.key,
              enabled: Boolean(flag.enabled),
              experimentId: `auto-${flag.key}`,
            })
          )
        );
        if (typeof window !== "undefined") {
          window.sessionStorage.setItem(guardKey, "1");
        }
      } catch {
        // Silent telemetry; do not block auth flow.
      }
    };

    syncFlags();
    return () => {
      cancelled = true;
    };
  }, [user]);

  useEffect(() => {
    if (!user) return undefined;

    const newSocket = io(SOCKET_URL);

    newSocket.on("connect", () => {
      newSocket.emit("join_room", user.id);
      setSocket(newSocket);
    });

    newSocket.on("new_match", () => {
      toast.success("You have a new match. Start chatting now.");
    });

    newSocket.on("new_like", () => {
      toast.info("Someone is interested in you. Check matches.");
    });

    return () => {
      newSocket.disconnect();
      setSocket(null);
    };
  }, [user]);

  const login = async (userDetails, accessToken, refreshToken) => {
    setAuthSession(userDetails, accessToken, refreshToken);
  };

  const logout = () => {
    if (socket) {
      socket.disconnect();
      setSocket(null);
    }

    clearAuthSession();
    window.location.href = "/login";
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, socket }}>
      {children}
      <ToastContainer position="top-right" autoClose={3000} />
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};
