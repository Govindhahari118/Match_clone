"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import io from "socket.io-client";

const AuthContext = createContext();
const SOCKET_URL = process.env.NEXT_PUBLIC_SOCKET_URL || "http://localhost:4000";

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

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => readStoredUser());
  const [loading] = useState(false);
  const [socket, setSocket] = useState(null);

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
    };
  }, [user]);

  const login = async (userDetails, accessToken, refreshToken) => {
    setUser(userDetails);
    localStorage.setItem("user", JSON.stringify(userDetails));
    localStorage.setItem("accessToken", accessToken);
    if (refreshToken) {
      localStorage.setItem("refreshToken", refreshToken);
    }
  };

  const logout = () => {
    if (socket) {
      socket.disconnect();
      setSocket(null);
    }

    setUser(null);
    localStorage.removeItem("user");
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    window.location.href = "/login";
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, socket }}>
      {!loading && children}
      <ToastContainer position="top-right" autoClose={3000} />
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};
