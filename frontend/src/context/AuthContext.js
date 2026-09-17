"use client";

import { createContext, useContext, useEffect, useState, useSyncExternalStore } from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import io from "socket.io-client";

const AuthContext = createContext();
const SOCKET_URL = process.env.NEXT_PUBLIC_SOCKET_URL || "http://localhost:4000";
const authListeners = new Set();

function notifyAuthChange() {
  authListeners.forEach((listener) => listener());
}

function readStoredUser() {
  if (typeof window === "undefined") return null;
  try {
    const token = localStorage.getItem("accessToken");
    const storedUser = localStorage.getItem("user");
    if (!token || !storedUser) return null;
    return JSON.parse(storedUser);
  } catch {
    return null;
  }
}

function subscribeAuth(listener) {
  authListeners.add(listener);
  const onStorage = (event) => {
    if (event.key === "user" || event.key === "accessToken" || event.key === "refreshToken") listener();
  };
  if (typeof window !== "undefined") window.addEventListener("storage", onStorage);
  return () => {
    authListeners.delete(listener);
    if (typeof window !== "undefined") window.removeEventListener("storage", onStorage);
  };
}

function getAuthSnapshot() { return readStoredUser(); }
function getAuthServerSnapshot() { return null; }

export const AuthProvider = ({ children }) => {
  const user = useSyncExternalStore(subscribeAuth, getAuthSnapshot, getAuthServerSnapshot);
  const loading = false;
  const [socket, setSocket] = useState(null);

  useEffect(() => {
    if (!user || typeof window === "undefined") return undefined;
    const token = localStorage.getItem("accessToken");
    if (!token) return undefined;

    const newSocket = io(SOCKET_URL, {
      auth: { token },
      transports: ["websocket", "polling"],
      reconnection: true,
    });

    newSocket.on("connect", () => {
      // Server derives the room from the verified JWT. The id argument is kept only for legacy compatibility.
      newSocket.emit("join_room", user.id);
      setSocket(newSocket);
    });
    newSocket.on("connect_error", () => setSocket(null));
    newSocket.on("new_match", () => toast.success("You have a new match. Start chatting now."));
    newSocket.on("new_like", () => toast.info("Someone is interested in you. Check matches."));

    return () => {
      newSocket.disconnect();
      setSocket(null);
    };
  }, [user]);

  const login = async (userDetails, accessToken, refreshToken) => {
    localStorage.setItem("user", JSON.stringify(userDetails));
    localStorage.setItem("accessToken", accessToken);
    if (refreshToken) localStorage.setItem("refreshToken", refreshToken);
    notifyAuthChange();
  };

  const logout = () => {
    if (socket) socket.disconnect();
    setSocket(null);
    localStorage.removeItem("user");
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    notifyAuthChange();
    window.location.href = "/login";
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, socket }}>
      {children}
      <ToastContainer position="top-right" autoClose={3000} />
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
