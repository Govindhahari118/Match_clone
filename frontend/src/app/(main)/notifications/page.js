"use client";

import { useState, useEffect } from "react";
import { useAuth } from "../../../context/AuthContext";
import { useRouter } from "next/navigation";
import Link from "next/link";

export default function NotificationsPage() {
    const { user, loading } = useAuth();
    const router = useRouter();
    const [notifications, setNotifications] = useState([]);

    useEffect(() => {
        if (!loading && !user) {
            router.push("/login");
        }
    }, [user, loading, router]);

    useEffect(() => {
        // Mock data fetch
        setTimeout(() => {
            setNotifications([
                { id: 1, type: "match", title: "New Match!", message: "You matched with Priya Sharma.", time: "2m ago", read: false },
                { id: 2, type: "visitor", title: "Profile View", message: "Someone viewed your profile.", time: "1h ago", read: true },
                { id: 3, type: "system", title: "Welcome!", message: "Thanks for joining MatrimonyConnect.", time: "1d ago", read: true },
            ]);
        }, 1000);
    }, []);

    if (loading || !user) return null;

    return (
        <div style={{ maxWidth: 640, margin: "0 auto" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "1.5rem" }}>
                <h1 style={{ fontSize: 24, fontWeight: 800, color: "#111827" }}>Notifications</h1>
                <button style={{ fontSize: 13, color: "#e11d48", background: "none", border: "none", fontWeight: 600, cursor: "pointer" }}>
                    Mark all as read
                </button>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                {notifications.length === 0 ? (
                    <div style={{ textAlign: "center", padding: "4rem 1rem", background: "white", borderRadius: 16 }}>
                        <div style={{ fontSize: 48, marginBottom: 16 }}>🔕</div>
                        <h3 style={{ fontSize: 18, fontWeight: 700, color: "#374151" }}>No notifications yet</h3>
                        <p style={{ color: "#94a3b8", fontSize: 14 }}>We'll notify you when you get updates.</p>
                    </div>
                ) : (
                    notifications.map(n => (
                        <div key={n.id} style={{
                            display: "flex", gap: 16, padding: 16,
                            background: n.read ? "white" : "#fff1f2",
                            borderRadius: 16, border: "1px solid",
                            borderColor: n.read ? "#f1f5f9" : "#fecdd3",
                            transition: "transform 0.2s",
                            cursor: "pointer"
                        }} className="hover-lift">
                            <div style={{
                                width: 40, height: 40, borderRadius: "50%",
                                background: n.type === "match" ? "#fce7f3" : n.type === "visitor" ? "#e0f2fe" : "#f1f5f9",
                                display: "flex", alignItems: "center", justifyContent: "center",
                                fontSize: 20
                            }}>
                                {n.type === "match" ? "💖" : n.type === "visitor" ? "👁️" : "🔔"}
                            </div>
                            <div style={{ flex: 1 }}>
                                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                                    <h4 style={{ fontSize: 15, fontWeight: 700, color: "#111827" }}>{n.title}</h4>
                                    <span style={{ fontSize: 12, color: "#94a3b8" }}>{n.time}</span>
                                </div>
                                <p style={{ fontSize: 14, color: "#64748b", lineHeight: 1.5 }}>{n.message}</p>
                            </div>
                            {!n.read && <div style={{ width: 8, height: 8, borderRadius: "50%", background: "#e11d48", alignSelf: "center" }}></div>}
                        </div>
                    ))
                )}
            </div>
            <style>{`
                .hover-lift:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
}
