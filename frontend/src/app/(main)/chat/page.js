"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import LoginPromptModal from "../../../components/LoginPromptModal";

const DEMO_CONVERSATIONS = [
  {
    userId: "preview-profile",
    name: "Preview Profile",
    photo: "https://randomuser.me/api/portraits/women/44.jpg",
    role: "Professional",
    city: "India",
    lastMessage: "Guest preview only — login to access real conversations.",
    updatedAt: Date.now(),
    unread: 0,
  },
];
const DEMO_MESSAGES = {
  "preview-profile": [
    {
      id: "preview-message",
      senderId: "preview-profile",
      content: "This is a clearly labelled preview conversation. Live messages appear only after login.",
      createdAt: new Date().toISOString(),
      status: "delivered",
    },
  ],
};

function formatTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function createClientMessageId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID();
  return `msg_${Date.now()}_${Math.random().toString(16).slice(2)}`;
}

function upsertMessage(list, incoming) {
  const key = incoming.clientMessageId || incoming.id;
  const index = list.findIndex((item) => (item.clientMessageId || item.id) === key);
  if (index === -1) return [...list, incoming];
  const next = [...list];
  next[index] = { ...next[index], ...incoming };
  return next;
}

export default function ChatPage() {
  const { user, socket } = useAuth();
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [loadingConversations, setLoadingConversations] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [conversations, setConversations] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [newMessage, setNewMessage] = useState("");
  const [infoMessage, setInfoMessage] = useState("");
  const scrollAnchorRef = useRef(null);

  const selectedConversation = useMemo(
    () => conversations.find((item) => item.userId === selectedId) || null,
    [conversations, selectedId]
  );
  const myUserId = user?.id || user?.sub || "me";

  useEffect(() => {
    const loadConversations = async () => {
      setLoadingConversations(true);
      setInfoMessage("");
      try {
        if (!user) {
          setConversations(DEMO_CONVERSATIONS);
          setSelectedId(DEMO_CONVERSATIONS[0]?.userId || null);
          setInfoMessage("Guest preview only. Login to access live conversations.");
          return;
        }
        const response = await api.get("/chat/conversations");
        const list = Array.isArray(response.data) ? response.data : [];
        setConversations(list);
        setSelectedId((current) => (current && list.some((item) => item.userId === current) ? current : list[0]?.userId || null));
      } catch {
        setConversations([]);
        setSelectedId(null);
        setInfoMessage("Live conversations could not be loaded. Retry after checking your connection.");
      } finally {
        setLoadingConversations(false);
      }
    };
    loadConversations();
  }, [user]);

  useEffect(() => {
    const loadMessages = async () => {
      if (!selectedId) {
        setMessages([]);
        return;
      }
      setLoadingMessages(true);
      try {
        if (!user) {
          setMessages(DEMO_MESSAGES[selectedId] || []);
          return;
        }
        const response = await api.get(`/chat/${selectedId}`);
        setMessages(Array.isArray(response.data) ? response.data : []);
        await api.post(`/chat/${selectedId}/read`).catch(() => {});
        setConversations((prev) => prev.map((item) => item.userId === selectedId ? { ...item, unread: 0 } : item));
      } catch {
        setMessages([]);
        setInfoMessage("Messages could not be loaded. Nothing has been replaced with demo data.");
      } finally {
        setLoadingMessages(false);
      }
    };
    loadMessages();
  }, [selectedId, user]);

  useEffect(() => {
    scrollAnchorRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    if (!socket) return undefined;
    const onReceiveMessage = (message) => {
      const peerId = message.senderId === myUserId ? selectedId : message.senderId;
      if (!peerId) return;
      setConversations((prev) => prev.map((item) => item.userId === peerId ? {
        ...item,
        lastMessage: message.content,
        updatedAt: Date.now(),
        unread: peerId === selectedId ? 0 : (item.unread || 0) + 1,
      } : item));
      if (peerId === selectedId) {
        setMessages((prev) => upsertMessage(prev, message));
        api.post(`/chat/${selectedId}/read`).catch(() => {});
      }
    };
    const onMessageSent = (message) => setMessages((prev) => upsertMessage(prev, message));
    socket.on("receive_message", onReceiveMessage);
    socket.on("message_sent", onMessageSent);
    return () => {
      socket.off("receive_message", onReceiveMessage);
      socket.off("message_sent", onMessageSent);
    };
  }, [socket, myUserId, selectedId]);

  const filteredConversations = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return conversations;
    return conversations.filter((item) => [item.name, item.city, item.role, item.lastMessage].filter(Boolean).join(" ").toLowerCase().includes(query));
  }, [conversations, searchQuery]);

  const sendMessage = async (event) => {
    event.preventDefault();
    const content = newMessage.trim();
    if (!content || !selectedConversation) return;
    if (!user) {
      setShowLoginModal(true);
      return;
    }

    const clientMessageId = createClientMessageId();
    const optimistic = {
      clientMessageId,
      senderId: myUserId,
      content,
      createdAt: new Date().toISOString(),
      status: "queued",
    };
    setMessages((prev) => upsertMessage(prev, optimistic));
    setConversations((prev) => prev.map((item) => item.userId === selectedConversation.userId ? {
      ...item,
      lastMessage: content,
      updatedAt: Date.now(),
    } : item));
    setNewMessage("");

    if (/(upi|bank\s*account|send\s+money|transfer\s+money|wire\s+money)/i.test(content)) {
      setInfoMessage("Safety reminder: independently verify a person before sending money or sharing financial details.");
    }

    const markFailed = (errorText) => {
      setMessages((prev) => prev.map((item) => item.clientMessageId === clientMessageId ? { ...item, status: "failed" } : item));
      setInfoMessage(errorText || "Message failed. You can retry safely without creating duplicates.");
    };

    try {
      const payload = { receiverId: selectedConversation.userId, content, clientMessageId };
      if (socket?.connected) {
        socket.emit("send_message", payload, (ack) => {
          if (ack?.ok && ack.message) setMessages((prev) => upsertMessage(prev, ack.message));
          else markFailed(ack?.error);
        });
      } else {
        const response = await api.post("/chat/send", payload);
        setMessages((prev) => upsertMessage(prev, response.data));
      }
    } catch {
      markFailed();
    }
  };

  return (
    <div className="panel" style={{ padding: "0.7rem", minHeight: "min(76vh, 760px)" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "0.8rem", marginBottom: "0.55rem" }}>
        <div>
          <p className="section-label" style={{ marginBottom: "0.18rem" }}>Conversations</p>
          <h1 style={{ margin: 0, fontSize: "1.6rem", fontFamily: "var(--font-display)" }}>Secure Messaging</h1>
        </div>
        {selectedConversation && user && <Link href={`/profile/${selectedConversation.userId}`} className="button button-secondary">View Profile</Link>}
      </div>

      {infoMessage && <div className="chip chip-brand" style={{ marginBottom: "0.55rem" }}>{infoMessage}</div>}

      <div style={{ display: "grid", gridTemplateColumns: "320px minmax(0,1fr)", gap: "0.65rem" }} className="chat-shell-grid">
        <aside className="panel" style={{ padding: "0.65rem", overflow: "hidden" }}>
          <input className="form-input" value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="Search conversations" style={{ marginBottom: "0.55rem" }} />
          <div style={{ display: "grid", gap: "0.4rem", maxHeight: "64vh", overflowY: "auto" }}>
            {loadingConversations ? (
              Array.from({ length: 5 }).map((_, index) => <div key={`load-c-${index}`} className="panel" style={{ height: 72, background: "#f7f3ec" }} />)
            ) : filteredConversations.length === 0 ? (
              <div className="panel" style={{ padding: "1rem", textAlign: "center" }}><p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.88rem" }}>No conversations yet.</p></div>
            ) : filteredConversations.map((conversation) => {
              const active = conversation.userId === selectedId;
              return (
                <button key={conversation.userId} type="button" onClick={() => setSelectedId(conversation.userId)} style={{ width: "100%", textAlign: "left", borderRadius: 14, border: active ? "1px solid rgba(240,107,78,0.4)" : "1px solid rgba(23,33,59,0.08)", background: active ? "rgba(240,107,78,0.12)" : "#fff", padding: "0.55rem", cursor: "pointer", display: "grid", gridTemplateColumns: "48px minmax(0,1fr)", gap: "0.52rem" }}>
                  <Image src={conversation.photo || "https://randomuser.me/api/portraits/lego/1.jpg"} alt={`${conversation.name} photo`} width={48} height={48} sizes="48px" style={{ width: 48, height: 48, borderRadius: 12, objectFit: "cover" }} />
                  <div style={{ minWidth: 0 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", gap: "0.4rem" }}>
                      <strong style={{ fontSize: "0.88rem", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{conversation.name}</strong>
                      <span style={{ fontSize: "0.72rem", color: "var(--ink-muted)", flexShrink: 0 }}>{formatTime(conversation.updatedAt || conversation.lastMessageAt)}</span>
                    </div>
                    <p style={{ margin: "0.2rem 0 0", fontSize: "0.76rem", color: "var(--ink-muted)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{conversation.lastMessage || conversation.activity?.label || "Connected"}</p>
                  </div>
                </button>
              );
            })}
          </div>
        </aside>

        <section className="panel" style={{ padding: "0.75rem", display: "grid", gridTemplateRows: "auto minmax(0,1fr) auto", minHeight: "66vh" }}>
          {!selectedConversation ? (
            <div style={{ display: "grid", placeItems: "center", color: "var(--ink-muted)", minHeight: "62vh" }}>Select a conversation to begin</div>
          ) : (
            <>
              <header style={{ display: "flex", alignItems: "center", gap: "0.6rem", paddingBottom: "0.58rem", borderBottom: "1px solid var(--line)" }}>
                <Image src={selectedConversation.photo || "https://randomuser.me/api/portraits/lego/1.jpg"} alt={`${selectedConversation.name} photo`} width={46} height={46} sizes="46px" style={{ borderRadius: 12, objectFit: "cover" }} />
                <div>
                  <strong>{selectedConversation.name}</strong>
                  <p style={{ margin: "0.2rem 0 0", fontSize: "0.78rem", color: "var(--ink-muted)" }}>{selectedConversation.activity?.label || selectedConversation.city || "Connected"}{selectedConversation.managedBy && ` · Managed by ${selectedConversation.managedBy}`}</p>
                </div>
              </header>

              <div style={{ overflowY: "auto", padding: "0.72rem 0.2rem 0.72rem 0", display: "grid", gap: "0.48rem" }}>
                {loadingMessages ? (
                  Array.from({ length: 5 }).map((_, index) => <div key={`m-load-${index}`} className="panel" style={{ height: 56, background: "#f7f3ec" }} />)
                ) : messages.length === 0 ? (
                  <div style={{ color: "var(--ink-muted)", textAlign: "center", marginTop: "2rem" }}>No messages yet. Start with a simple introduction.</div>
                ) : messages.map((message, index) => {
                  const mine = message.senderId === myUserId || message.senderId === "me";
                  return (
                    <div key={message.clientMessageId || message.id || `m-${index}`} style={{ display: "flex", justifyContent: mine ? "flex-end" : "flex-start" }}>
                      <div style={{ maxWidth: "72%", borderRadius: mine ? "16px 16px 6px 16px" : "16px 16px 16px 6px", background: mine ? "linear-gradient(135deg, var(--brand), var(--brand-deep))" : "#fff", border: mine ? "none" : "1px solid var(--line)", color: mine ? "#fff" : "var(--ink)", padding: "0.6rem 0.72rem", boxShadow: mine ? "0 10px 20px rgba(215,81,54,0.25)" : "var(--shadow-sm)" }}>
                        <p style={{ margin: 0, fontSize: "0.88rem", lineHeight: 1.45 }}>{message.content}</p>
                        <p style={{ margin: "0.22rem 0 0", fontSize: "0.68rem", opacity: 0.75, textAlign: "right" }}>{formatTime(message.createdAt || Date.now())}{mine && message.status ? ` · ${message.status}` : ""}</p>
                      </div>
                    </div>
                  );
                })}
                <div ref={scrollAnchorRef} />
              </div>

              <form onSubmit={sendMessage} style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.42rem", borderTop: "1px solid var(--line)", paddingTop: "0.62rem" }}>
                <input className="form-input" value={newMessage} onChange={(event) => setNewMessage(event.target.value)} placeholder={user ? `Message ${selectedConversation.name}...` : "Login to chat"} disabled={!selectedConversation} maxLength={5000} />
                <button type="submit" className="button button-primary" disabled={!newMessage.trim() || !selectedConversation}>Send</button>
              </form>
            </>
          )}
        </section>
      </div>

      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />
    </div>
  );
}
