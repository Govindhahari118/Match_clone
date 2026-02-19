"use client";

import { createContext, useContext, useState, useEffect } from "react";
import api from "../services/api";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import io from "socket.io-client";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [socket, setSocket] = useState(null);

    useEffect(() => {
        // Rehydrate User
        const token = localStorage.getItem("accessToken");
        if (token) {
            try {
                const storedUser = localStorage.getItem("user");
                if (storedUser) {
                    setUser(JSON.parse(storedUser));
                }
            } catch (error) {
                console.error("Failed to parse user from local storage:", error);
            }
        }
        setLoading(false);
    }, []);

    // Socket Connection Effect
    useEffect(() => {
        if (user && !socket) {
            // Initiate socket connection
            const newSocket = io("http://localhost:4000"); // Use env var in prod
            newSocket.on("connect", () => {
                newSocket.emit("join_room", user.id);
            });

            newSocket.on("new_match", (data) => {
                toast.success("💕 You have a new Match! Start chatting now.");
            });

            newSocket.on("new_like", (data) => {
                toast.info("Someone is interested in you! Check matches.");
            });

            setSocket(newSocket);

            return () => {
                newSocket.disconnect();
                setSocket(null);
            };
        } else if (!user && socket) {
            // Disconnect if logged out
            socket.disconnect();
            setSocket(null);
        }
    }, [user]); // Re-run when user changes (login/logout)

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
