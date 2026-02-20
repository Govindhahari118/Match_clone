"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import LoginPromptModal from "../../../components/LoginPromptModal";

const DEMO_CONVERSATIONS = [
  {
    userId: "u1",
    name: "Priya Sharma",
    photo: "https://randomuser.me/api/portraits/women/44.jpg",
    role: "Doctor",
    city: "Mumbai",
    lastMessage: "Would love to continue this conversation.",
    updatedAt: Date.now() - 12 * 60 * 1000,
    unread: 2,
  },
  {
    userId: "u2",
    name: "Ananya Rao",
    photo: "https://randomuser.me/api/portraits/women/45.jpg",
    role: "Engineer",
    city: "Bengaluru",
    lastMessage: "Thanks for sharing your profile details.",
    updatedAt: Date.now() - 72 * 60 * 1000,
    unread: 0,
  },
  {
    userId: "u3",
    name: "Kavya Menon",
    photo: "https://randomuser.me/api/portraits/women/46.jpg",
    role: "Lawyer",
    city: "Delhi",
    lastMessage: "Can we speak this weekend?",
    updatedAt: Date.now() - 3 * 60 * 60 * 1000,
    unread: 1,
  },
];

const DEMO_MESSAGES = {
  u1: [
    {
      id: "d1",
      senderId: "u1",
      content: "Hi. I liked your profile, especially your travel interests.",
      createdAt: new Date(Date.now() - 28 * 60 * 1000).toISOString(),
    },
    {
      id: "d2",
      senderId: "me",
      content: "Thank you. I noticed we both value family traditions too.",
      createdAt: new Date(Date.now() - 21 * 60 * 1000).toISOString(),
    },
    {
      id: "d3",
      senderId: "u1",
      content: "Exactly. Happy to continue this conversation.",
      createdAt: new Date(Date.now() - 12 * 60 * 1000).toISOString(),
    },
  ],
  u2: [
    {
      id: "d4",
      senderId: "me",
      content: "Hi Ananya, good to connect.",
      createdAt: new Date(Date.now() - 90 * 60 * 1000).toISOString(),
    },
    {
      id: "d5",
      senderId: "u2",
      content: "Likewise. Looking forward to knowing more.",
      createdAt: new Date(Date.now() - 75 * 60 * 1000).toISOString(),
    },
  ],
  u3: [
    {
      id: "d6",
      senderId: "u3",
      content: "Can we speak this weekend?",
      createdAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
    },
  ],
};

function formatTime(value) {
  const date = new Date(value);
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
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
          setInfoMessage("Preview mode: login to access live conversations.");
          return;
        }

        const response = await api.get("/chat/conversations");
        const list = Array.isArray(response.data) && response.data.length > 0 ? response.data : DEMO_CONVERSATIONS;
        setConversations(list);
        setSelectedId((current) => current || list[0]?.userId || null);
      } catch {
        setConversations(DEMO_CONVERSATIONS);
        setSelectedId(DEMO_CONVERSATIONS[0]?.userId || null);
        setInfoMessage("Live chat unavailable right now. Showing demo conversations.");
      } finally {
        setLoadingConversations(false);
      }
    };

    loadConversations();
  }, [user]);

  useEffect(() => {
    const loadMessages = async () => {
      if (!selectedId) return;

      setLoadingMessages(true);
      try {
        if (!user) {
          setMessages(DEMO_MESSAGES[selectedId] || []);
          return;
        }

        const response = await api.get(`/chat/${selectedId}`);
        const list = Array.isArray(response.data) ? response.data : [];
        setMessages(list.length ? list : DEMO_MESSAGES[selectedId] || []);

        setConversations((prev) =>
          prev.map((item) =>
            item.userId === selectedId
              ? {
                  ...item,
                  unread: 0,
                }
              : item
          )
        );
      } catch {
        setMessages(DEMO_MESSAGES[selectedId] || []);
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
      const peerId = message.senderId === myUserId ? message.receiverId : message.senderId;

      setConversations((prev) =>
        prev.map((item) => {
          if (item.userId !== peerId) return item;

          return {
            ...item,
            lastMessage: message.content,
            updatedAt: Date.now(),
            unread: peerId === selectedId ? 0 : (item.unread || 0) + 1,
          };
        })
      );

      if (peerId === selectedId) {
        setMessages((prev) => [...prev, message]);
      }
    };

    socket.on("receive_message", onReceiveMessage);
    return () => socket.off("receive_message", onReceiveMessage);
  }, [socket, myUserId, selectedId]);

  const filteredConversations = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return conversations;

    return conversations.filter((item) => {
      const text = [item.name, item.city, item.role, item.lastMessage].filter(Boolean).join(" ").toLowerCase();
      return text.includes(query);
    });
  }, [conversations, searchQuery]);

  const sendMessage = async (event) => {
    event.preventDefault();

    if (!newMessage.trim() || !selectedConversation) return;

    if (!user) {
      setShowLoginModal(true);
      return;
    }

    const payload = {
      senderId: myUserId,
      receiverId: selectedConversation.userId,
      content: newMessage.trim(),
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, payload]);
    setConversations((prev) =>
      prev.map((item) =>
        item.userId === selectedConversation.userId
          ? {
              ...item,
              lastMessage: payload.content,
              updatedAt: Date.now(),
            }
          : item
      )
    );
    setNewMessage("");

    try {
      if (socket?.connected) {
        socket.emit("send_message", payload);
      } else {
        await api.post("/chat/send", {
          receiverId: selectedConversation.userId,
          content: payload.content,
        });
      }
    } catch {
      setInfoMessage("Message queued in demo mode.");
    }
  };

  return (
    <div className="panel" style={{ padding: "0.7rem", minHeight: "min(76vh, 760px)" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "0.8rem", marginBottom: "0.55rem" }}>
        <div>
          <p className="section-label" style={{ marginBottom: "0.18rem" }}>
            Conversations
          </p>
          <h1 style={{ margin: 0, fontSize: "1.6rem", fontFamily: "var(--font-display)" }}>Secure Messaging</h1>
        </div>
        {selectedConversation && (
          <Link href={`/profile/${selectedConversation.userId}`} className="button button-secondary">
            View Profile
          </Link>
        )}
      </div>

      {infoMessage && (
        <div className="chip chip-brand" style={{ marginBottom: "0.55rem" }}>
          {infoMessage}
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "320px minmax(0,1fr)", gap: "0.65rem" }} className="chat-shell-grid">
        <aside className="panel" style={{ padding: "0.65rem", overflow: "hidden" }}>
          <input
            className="form-input"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Search conversations"
            style={{ marginBottom: "0.55rem" }}
          />

          <div style={{ display: "grid", gap: "0.4rem", maxHeight: "64vh", overflowY: "auto" }}>
            {loadingConversations ? (
              Array.from({ length: 5 }).map((_, index) => (
                <div key={`load-c-${index}`} className="panel" style={{ height: 72, background: "#f7f3ec" }} />
              ))
            ) : filteredConversations.length === 0 ? (
              <div className="panel" style={{ padding: "1rem", textAlign: "center" }}>
                <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.88rem" }}>
                  No conversations yet.
                </p>
              </div>
            ) : (
              filteredConversations.map((conversation) => {
                const active = conversation.userId === selectedId;
                return (
                  <button
                    key={conversation.userId}
                    type="button"
                    onClick={() => setSelectedId(conversation.userId)}
                    style={{
                      width: "100%",
                      textAlign: "left",
                      borderRadius: 14,
                      border: active ? "1px solid rgba(240,107,78,0.4)" : "1px solid rgba(23,33,59,0.08)",
                      background: active ? "rgba(240,107,78,0.12)" : "#fff",
                      padding: "0.55rem",
                      cursor: "pointer",
                      display: "grid",
                      gridTemplateColumns: "48px minmax(0,1fr)",
                      gap: "0.52rem",
                    }}
                  >
                    <Image
                      src={conversation.photo}
                      alt={`${conversation.name} photo`}
                      width={48}
                      height={48}
                      unoptimized
                      style={{ width: 48, height: 48, borderRadius: 12, objectFit: "cover" }}
                    />
                    <div style={{ minWidth: 0 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", gap: "0.4rem" }}>
                        <strong style={{ fontSize: "0.88rem", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                          {conversation.name}
                        </strong>
                        <span style={{ fontSize: "0.72rem", color: "var(--ink-muted)", flexShrink: 0 }}>
                          {formatTime(conversation.updatedAt)}
                        </span>
                      </div>
                      <p
                        style={{
                          margin: "0.2rem 0 0",
                          fontSize: "0.76rem",
                          color: "var(--ink-muted)",
                          whiteSpace: "nowrap",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                        }}
                      >
                        {conversation.lastMessage}
                      </p>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </aside>

        <section className="panel" style={{ padding: "0.75rem", display: "grid", gridTemplateRows: "auto minmax(0,1fr) auto", minHeight: "66vh" }}>
          {!selectedConversation ? (
            <div style={{ display: "grid", placeItems: "center", color: "var(--ink-muted)", minHeight: "62vh" }}>
              Select a conversation to begin
            </div>
          ) : (
            <>
              <header style={{ display: "flex", alignItems: "center", gap: "0.6rem", paddingBottom: "0.58rem", borderBottom: "1px solid var(--line)" }}>
                <Image
                  src={selectedConversation.photo}
                  alt={`${selectedConversation.name} photo`}
                  width={46}
                  height={46}
                  unoptimized
                  style={{ borderRadius: 12, objectFit: "cover" }}
                />
                <div>
                  <strong>{selectedConversation.name}</strong>
                  <p style={{ margin: "0.2rem 0 0", fontSize: "0.78rem", color: "var(--ink-muted)" }}>
                    {selectedConversation.role || "Professional"} · {selectedConversation.city || "India"}
                  </p>
                </div>
              </header>

              <div style={{ overflowY: "auto", padding: "0.72rem 0.2rem 0.72rem 0", display: "grid", gap: "0.48rem" }}>
                {loadingMessages ? (
                  Array.from({ length: 5 }).map((_, index) => (
                    <div key={`m-load-${index}`} className="panel" style={{ height: 56, background: "#f7f3ec" }} />
                  ))
                ) : messages.length === 0 ? (
                  <div style={{ color: "var(--ink-muted)", textAlign: "center", marginTop: "2rem" }}>
                    No messages yet. Start with a simple introduction.
                  </div>
                ) : (
                  messages.map((message, index) => {
                    const mine = message.senderId === myUserId || message.senderId === "me";
                    return (
                      <div key={`${message.id || "m"}-${index}`} style={{ display: "flex", justifyContent: mine ? "flex-end" : "flex-start" }}>
                        <div
                          style={{
                            maxWidth: "72%",
                            borderRadius: mine ? "16px 16px 6px 16px" : "16px 16px 16px 6px",
                            background: mine
                              ? "linear-gradient(135deg, var(--brand), var(--brand-deep))"
                              : "#fff",
                            border: mine ? "none" : "1px solid var(--line)",
                            color: mine ? "#fff" : "var(--ink)",
                            padding: "0.6rem 0.72rem",
                            boxShadow: mine ? "0 10px 20px rgba(215,81,54,0.25)" : "var(--shadow-sm)",
                          }}
                        >
                          <p style={{ margin: 0, fontSize: "0.88rem", lineHeight: 1.45 }}>{message.content}</p>
                          <p style={{ margin: "0.22rem 0 0", fontSize: "0.68rem", opacity: 0.75, textAlign: "right" }}>
                            {formatTime(message.createdAt || Date.now())}
                          </p>
                        </div>
                      </div>
                    );
                  })
                )}
                <div ref={scrollAnchorRef} />
              </div>

              <form onSubmit={sendMessage} style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.42rem", borderTop: "1px solid var(--line)", paddingTop: "0.62rem" }}>
                <input
                  className="form-input"
                  value={newMessage}
                  onChange={(event) => setNewMessage(event.target.value)}
                  placeholder={user ? `Message ${selectedConversation.name}...` : "Login to chat"}
                  disabled={!selectedConversation}
                />
                <button type="submit" className="button button-primary" disabled={!newMessage.trim() || !selectedConversation}>
                  Send
                </button>
              </form>
            </>
          )}
        </section>
      </div>

      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />

      <style jsx>{`
        @media (max-width: 980px) {
          .chat-shell-grid {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  );
}
