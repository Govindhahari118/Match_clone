"use client";

import { useState, useEffect, useRef } from "react";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import Link from "next/link";

const MOCK_CONVERSATIONS = [
    { userId: "u1", name: "Priya Sharma", photo: "https://randomuser.me/api/portraits/women/44.jpg", lastMessage: "Hi! Would love to get to know you 😊", time: "2m ago", unread: 2 },
    { userId: "u2", name: "Ananya Singh", photo: "https://randomuser.me/api/portraits/women/45.jpg", lastMessage: "Thank you for the interest!", time: "1h ago", unread: 0 },
    { userId: "u3", name: "Kavya Reddy", photo: "https://randomuser.me/api/portraits/women/46.jpg", lastMessage: "When are you free to talk?", time: "3h ago", unread: 1 },
    { userId: "u4", name: "Riya Patel", photo: "https://randomuser.me/api/portraits/women/47.jpg", lastMessage: "Sounds great, let's connect!", time: "1d ago", unread: 0 },
    { userId: "u5", name: "Simran Kaur", photo: "https://randomuser.me/api/portraits/women/48.jpg", lastMessage: "I love travelling too! 🌏", time: "2d ago", unread: 0 },
];

const MOCK_MESSAGES = {
    u1: [
        { id: "m1", senderId: "u1", content: "Hi! I saw your profile and I think we have a lot in common 😊", createdAt: new Date(Date.now() - 600000).toISOString() },
        { id: "m2", senderId: "me", content: "Hello Priya! Thanks for reaching out. I noticed that too 😄", createdAt: new Date(Date.now() - 500000).toISOString() },
        { id: "m3", senderId: "u1", content: "I see you're from Mumbai too! Which area?", createdAt: new Date(Date.now() - 400000).toISOString() },
        { id: "m4", senderId: "me", content: "Yes! I'm from Bandra. What about you?", createdAt: new Date(Date.now() - 300000).toISOString() },
        { id: "m5", senderId: "u1", content: "Andheri! We're practically neighbours 😄 Would love to connect more!", createdAt: new Date(Date.now() - 100000).toISOString() },
        { id: "m6", senderId: "u1", content: "Hi! Would love to get to know you 😊", createdAt: new Date(Date.now() - 120000).toISOString() },
    ],
    u2: [
        { id: "m1", senderId: "me", content: "Hi Ananya! I really liked your profile 💌", createdAt: new Date(Date.now() - 3600000).toISOString() },
        { id: "m2", senderId: "u2", content: "Thank you for the interest!", createdAt: new Date(Date.now() - 3500000).toISOString() },
    ],
};

export default function ChatPage() {
    const { user, socket } = useAuth();
    const [conversations, setConversations] = useState([]);
    const [selectedChat, setSelectedChat] = useState(null);
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState("");
    const [loadingChats, setLoadingChats] = useState(true);
    const [loadingMsgs, setLoadingMsgs] = useState(false);
    const [search, setSearch] = useState("");
    const [error, setError] = useState(null);
    const messagesEndRef = useRef(null);

    // Socket: incoming message listener
    useEffect(() => {
        if (!socket) return;
        const handler = (msg) => {
            if (selectedChat && (msg.senderId === selectedChat.userId || msg.receiverId === selectedChat.userId)) {
                setMessages(prev => [...prev, msg]);
            }
            // Update last message in sidebar
            setConversations(prev => prev.map(c =>
                c.userId === msg.senderId ? { ...c, lastMessage: msg.content, time: "Just now", unread: (c.unread || 0) + 1 } : c
            ));
        };
        socket.on("receive_message", handler);
        return () => socket.off("receive_message", handler);
    }, [socket, selectedChat]);

    // Load conversations
    useEffect(() => {
        const fetch = async () => {
            setLoadingChats(true);
            try {
                const res = await api.get("/chat/conversations");
                setConversations(res.data?.length ? res.data : MOCK_CONVERSATIONS);
            } catch {
                // Network error or not logged in — show demo data
                setConversations(MOCK_CONVERSATIONS);
                setError("Demo mode — showing sample conversations");
            } finally {
                setLoadingChats(false);
            }
        };
        fetch();
    }, []);

    // Load messages when chat selected
    useEffect(() => {
        if (!selectedChat) return;
        const fetch = async () => {
            setLoadingMsgs(true);
            // Clear unread
            setConversations(prev => prev.map(c => c.userId === selectedChat.userId ? { ...c, unread: 0 } : c));
            try {
                const res = await api.get(`/chat/${selectedChat.userId}`);
                setMessages(res.data?.length ? res.data : (MOCK_MESSAGES[selectedChat.userId] || []));
            } catch {
                setMessages(MOCK_MESSAGES[selectedChat.userId] || []);
            } finally {
                setLoadingMsgs(false);
            }
        };
        fetch();
    }, [selectedChat]);

    // Auto-scroll
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    const sendMessage = async (e) => {
        e.preventDefault();
        if (!newMessage.trim() || !selectedChat) return;

        const myId = user?.id || user?.sub || "me";
        const payload = {
            senderId: myId,
            receiverId: selectedChat.userId,
            content: newMessage.trim(),
            createdAt: new Date().toISOString(),
        };

        // Optimistic update
        setMessages(prev => [...prev, payload]);
        setConversations(prev => prev.map(c =>
            c.userId === selectedChat.userId ? { ...c, lastMessage: payload.content, time: "Just now" } : c
        ));
        setNewMessage("");

        // Prefer socket; REST fallback if unavailable
        if (socket?.connected) {
            socket.emit("send_message", payload);
        } else {
            try {
                await api.post("/chat/send", { receiverId: selectedChat.userId, content: payload.content });
            } catch {
                // Message already shown optimistically — user sees it
            }
        }
    };

    const myId = user?.id || user?.sub || "me";
    const filtered = conversations.filter(c => c.name?.toLowerCase().includes(search.toLowerCase()));

    const sidebarW = 320;

    return (
        <div>
            <div style={{ marginBottom: "1.25rem" }}>
                <h1 style={{ fontSize: 22, fontWeight: 800, color: "#111827" }}>Messages 💬</h1>
                {error && (
                    <div style={{ marginTop: 8, padding: "8px 14px", background: "#fffbeb", border: "1px solid #fde68a", borderRadius: 10, fontSize: 12, color: "#92400e", fontWeight: 600 }}>
                        ⚠️ {error}
                    </div>
                )}
            </div>

            <div style={{ display: "flex", height: "calc(100vh - 170px)", minHeight: 500, background: "white", borderRadius: 24, border: "1.5px solid #f1f5f9", overflow: "hidden", boxShadow: "0 4px 24px rgba(0,0,0,0.06)" }}>

                {/* ── SIDEBAR ── */}
                <div style={{ width: sidebarW, flexShrink: 0, borderRight: "1.5px solid #f1f5f9", display: "flex", flexDirection: "column" }}>
                    {/* Search */}
                    <div style={{ padding: "14px 14px 10px" }}>
                        <div style={{ position: "relative" }}>
                            <span style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", fontSize: 14, color: "#94a3b8" }}>🔍</span>
                            <input
                                value={search}
                                onChange={e => setSearch(e.target.value)}
                                placeholder="Search conversations..."
                                style={{ width: "100%", padding: "9px 12px 9px 34px", background: "#f8fafc", border: "1.5px solid #f1f5f9", borderRadius: 12, fontSize: 13, outline: "none", fontFamily: "inherit" }}
                            />
                        </div>
                    </div>

                    {/* List */}
                    <div style={{ flex: 1, overflowY: "auto" }}>
                        {loadingChats ? (
                            Array(4).fill(0).map((_, i) => (
                                <div key={i} style={{ display: "flex", gap: 10, padding: "12px 14px", borderBottom: "1px solid #f8fafc" }}>
                                    <div className="skeleton" style={{ width: 46, height: 46, borderRadius: "50%", flexShrink: 0 }}></div>
                                    <div style={{ flex: 1 }}>
                                        <div className="skeleton" style={{ height: 12, width: "60%", marginBottom: 7 }}></div>
                                        <div className="skeleton" style={{ height: 11, width: "80%" }}></div>
                                    </div>
                                </div>
                            ))
                        ) : filtered.length === 0 ? (
                            <div style={{ textAlign: "center", padding: "3rem 1rem" }}>
                                <div style={{ fontSize: 40, marginBottom: 8 }}>💬</div>
                                <p style={{ fontSize: 13, color: "#94a3b8" }}>No conversations yet.<br />Match with someone to start chatting!</p>
                                <Link href="/matches" style={{ display: "inline-block", marginTop: 12, padding: "8px 18px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", borderRadius: 999, fontSize: 12, fontWeight: 700, textDecoration: "none" }}>Browse Matches</Link>
                            </div>
                        ) : (
                            filtered.map(chat => {
                                const active = selectedChat?.userId === chat.userId;
                                return (
                                    <div
                                        key={chat.userId}
                                        onClick={() => setSelectedChat(chat)}
                                        style={{
                                            display: "flex", alignItems: "center", gap: 10,
                                            padding: "12px 14px", cursor: "pointer",
                                            background: active ? "#fff1f2" : "transparent",
                                            borderLeft: active ? "3px solid #e11d48" : "3px solid transparent",
                                            borderBottom: "1px solid #f8fafc",
                                            transition: "background 0.15s",
                                        }}
                                        onMouseEnter={e => { if (!active) e.currentTarget.style.background = "#fafafa"; }}
                                        onMouseLeave={e => { if (!active) e.currentTarget.style.background = "transparent"; }}
                                    >
                                        <div style={{ position: "relative", flexShrink: 0 }}>
                                            <img src={chat.photo} alt={chat.name} style={{ width: 46, height: 46, borderRadius: "50%", objectFit: "cover", border: active ? "2px solid #fda4af" : "2px solid #f1f5f9" }} />
                                            {/* Online dot */}
                                            <span style={{ position: "absolute", bottom: 1, right: 1, width: 10, height: 10, background: "#10b981", borderRadius: "50%", border: "2px solid white" }}></span>
                                        </div>
                                        <div style={{ flex: 1, minWidth: 0 }}>
                                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                                <p style={{ fontWeight: 700, fontSize: 13, color: active ? "#e11d48" : "#111827", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{chat.name}</p>
                                                <p style={{ fontSize: 10, color: "#94a3b8", flexShrink: 0, marginLeft: 4 }}>{chat.time}</p>
                                            </div>
                                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 2 }}>
                                                <p style={{ fontSize: 12, color: "#94a3b8", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", maxWidth: 160 }}>{chat.lastMessage}</p>
                                                {chat.unread > 0 && (
                                                    <span style={{ background: "#e11d48", color: "white", fontSize: 10, fontWeight: 800, padding: "1px 6px", borderRadius: 99, flexShrink: 0 }}>{chat.unread}</span>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>

                {/* ── CHAT WINDOW ── */}
                <div style={{ flex: 1, display: "flex", flexDirection: "column", background: "#f8fafc", minWidth: 0 }}>
                    {!selectedChat ? (
                        /* Empty state */
                        <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", color: "#94a3b8" }}>
                            <div style={{ fontSize: 72, marginBottom: "1rem" }}>💬</div>
                            <h3 style={{ fontWeight: 800, fontSize: 18, color: "#374151", marginBottom: 8 }}>Select a Conversation</h3>
                            <p style={{ fontSize: 14 }}>Choose from your matches to start chatting</p>
                        </div>
                    ) : (
                        <>
                            {/* Chat header */}
                            <div style={{ padding: "12px 20px", background: "white", borderBottom: "1.5px solid #f1f5f9", display: "flex", alignItems: "center", gap: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.04)" }}>
                                <div style={{ position: "relative" }}>
                                    <img src={selectedChat.photo} alt={selectedChat.name} style={{ width: 42, height: 42, borderRadius: "50%", objectFit: "cover", border: "2px solid #fce7f3" }} />
                                    <span style={{ position: "absolute", bottom: 1, right: 1, width: 10, height: 10, background: "#10b981", borderRadius: "50%", border: "2px solid white" }}></span>
                                </div>
                                <div>
                                    <p style={{ fontWeight: 800, fontSize: 14, color: "#111827" }}>{selectedChat.name}</p>
                                    <p style={{ fontSize: 11, color: "#10b981", fontWeight: 600 }}>● Online</p>
                                </div>
                                <div style={{ marginLeft: "auto", display: "flex", gap: 8 }}>
                                    <Link href={`/profile/${selectedChat.userId}`} style={{ padding: "7px 14px", background: "#f8fafc", border: "1.5px solid #e2e8f0", borderRadius: 10, fontSize: 12, fontWeight: 700, color: "#374151", textDecoration: "none" }}>
                                        👁 View Profile
                                    </Link>
                                </div>
                            </div>

                            {/* Messages area */}
                            <div style={{ flex: 1, overflowY: "auto", padding: "1.25rem", display: "flex", flexDirection: "column", gap: "0.625rem" }}>
                                {loadingMsgs ? (
                                    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", padding: "3rem" }}>
                                        <div style={{ width: 32, height: 32, border: "3px solid #fce7f3", borderTopColor: "#e11d48", borderRadius: "50%", animation: "spin-slow 0.8s linear infinite" }}></div>
                                        <style>{`@keyframes spin-slow { to { transform: rotate(360deg); } }`}</style>
                                    </div>
                                ) : messages.length === 0 ? (
                                    <div style={{ textAlign: "center", padding: "3rem", color: "#94a3b8" }}>
                                        <div style={{ fontSize: 40, marginBottom: 8 }}>👋</div>
                                        <p style={{ fontSize: 14 }}>No messages yet. Say hello!</p>
                                    </div>
                                ) : (
                                    messages.map((msg, idx) => {
                                        const isMe = msg.senderId === myId || msg.senderId === "me";
                                        const showTimestamp = idx === 0 ||
                                            new Date(messages[idx - 1].createdAt).getMinutes() !== new Date(msg.createdAt).getMinutes();
                                        return (
                                            <div key={msg.id || idx}>
                                                {showTimestamp && (
                                                    <div style={{ textAlign: "center", margin: "8px 0" }}>
                                                        <span style={{ fontSize: 10, color: "#94a3b8", background: "#f1f5f9", padding: "2px 10px", borderRadius: 99 }}>
                                                            {new Date(msg.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                                                        </span>
                                                    </div>
                                                )}
                                                <div style={{ display: "flex", justifyContent: isMe ? "flex-end" : "flex-start" }}>
                                                    {!isMe && (
                                                        <img src={selectedChat.photo} alt="" style={{ width: 28, height: 28, borderRadius: "50%", objectFit: "cover", marginRight: 8, flexShrink: 0, alignSelf: "flex-end" }} />
                                                    )}
                                                    <div style={{
                                                        maxWidth: "68%", padding: "10px 14px",
                                                        background: isMe ? "linear-gradient(135deg, #e11d48, #c2185b)" : "white",
                                                        color: isMe ? "white" : "#111827",
                                                        borderRadius: isMe ? "18px 18px 4px 18px" : "18px 18px 18px 4px",
                                                        fontSize: 13, lineHeight: 1.5, fontWeight: 500,
                                                        boxShadow: isMe ? "0 4px 12px rgba(225,29,72,0.25)" : "0 2px 8px rgba(0,0,0,0.06)",
                                                        border: isMe ? "none" : "1px solid #f1f5f9",
                                                    }}>
                                                        {msg.content}
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })
                                )}
                                <div ref={messagesEndRef} />
                            </div>

                            {/* Input bar */}
                            <div style={{ padding: "12px 16px", background: "white", borderTop: "1.5px solid #f1f5f9" }}>
                                <form onSubmit={sendMessage} style={{ display: "flex", gap: 8, alignItems: "center" }}>
                                    <input
                                        type="text"
                                        value={newMessage}
                                        onChange={e => setNewMessage(e.target.value)}
                                        placeholder={`Message ${selectedChat.name}...`}
                                        style={{
                                            flex: 1, padding: "11px 16px",
                                            background: "#f8fafc", border: "1.5px solid #f1f5f9",
                                            borderRadius: 14, fontSize: 13, outline: "none",
                                            fontFamily: "inherit", transition: "border-color 0.2s",
                                        }}
                                        onFocus={e => e.target.style.borderColor = "#fda4af"}
                                        onBlur={e => e.target.style.borderColor = "#f1f5f9"}
                                    />
                                    <button
                                        type="submit"
                                        disabled={!newMessage.trim()}
                                        style={{
                                            width: 44, height: 44, borderRadius: 14, border: "none",
                                            background: newMessage.trim() ? "linear-gradient(135deg, #e11d48, #c2185b)" : "#f1f5f9",
                                            color: newMessage.trim() ? "white" : "#94a3b8",
                                            cursor: newMessage.trim() ? "pointer" : "default",
                                            fontSize: 18, display: "flex", alignItems: "center", justifyContent: "center",
                                            transition: "all 0.2s",
                                            boxShadow: newMessage.trim() ? "0 4px 12px rgba(225,29,72,0.3)" : "none",
                                        }}
                                    >
                                        ➤
                                    </button>
                                </form>
                            </div>
                        </>
                    )}
                </div>
            </div>

            <style>{`
        @media (max-width: 640px) {
          /* On mobile hide sidebar when a chat is selected */
        }
      `}</style>
        </div>
    );
}
