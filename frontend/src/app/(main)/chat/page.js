"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import SafeProfileImage from "../../../components/SafeProfileImage";

function formatTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function makeClientMessageId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID();
  return `msg-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export default function ChatPage() {
  const { user, socket } = useAuth();
  const searchParams = useSearchParams();
  const requestedUser = searchParams.get("user");
  const myUserId = user?.id || user?.sub || null;

  const [conversations, setConversations] = useState([]);
  const [selectedId, setSelectedId] = useState(requestedUser || null);
  const [messages, setMessages] = useState([]);
  const [conversationState, setConversationState] = useState("loading");
  const [messageState, setMessageState] = useState("idle");
  const [error, setError] = useState("");
  const [newMessage, setNewMessage] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const scrollAnchorRef = useRef(null);

  const selectedConversation = useMemo(
    () => conversations.find((item) => item.userId === selectedId) || null,
    [conversations, selectedId]
  );

  const loadConversations = useCallback(async () => {
    if (!user) {
      setConversations([]);
      setConversationState("signed_out");
      return;
    }
    setConversationState("loading");
    setError("");
    try {
      const response = await api.get("/chat/conversations");
      const list = Array.isArray(response.data) ? response.data : [];
      setConversations(list);
      setSelectedId((current) => current || requestedUser || list[0]?.userId || null);
      setConversationState("content");
    } catch (err) {
      setConversations([]);
      setError(err.response?.data?.error || "Couldn’t load conversations.");
      setConversationState("error");
    }
  }, [user, requestedUser]);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  const loadMessages = useCallback(async (peerId) => {
    if (!peerId || !user) {
      setMessages([]);
      return;
    }
    setMessageState("loading");
    try {
      const response = await api.get(`/chat/${peerId}`);
      const list = Array.isArray(response.data) ? response.data : [];
      setMessages(list);
      setMessageState("content");

      if (socket?.connected) {
        list
          .filter((message) => message.receiverId === myUserId && message.status !== "read")
          .forEach((message) => socket.emit("message_read", { messageId: message.id }));
      }
    } catch (err) {
      setMessages([]);
      setError(err.response?.data?.error || "Couldn’t load messages.");
      setMessageState("error");
    }
  }, [user, socket, myUserId]);

  useEffect(() => {
    loadMessages(selectedId);
  }, [selectedId, loadMessages]);

  useEffect(() => {
    scrollAnchorRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    if (!socket || !myUserId) return undefined;

    const onReceiveMessage = (message) => {
      const peerId = message.senderId === myUserId ? message.receiverId : message.senderId;
      if (message.receiverId === myUserId) {
        socket.emit("message_delivered", { messageId: message.id });
      }

      setConversations((current) => {
        const exists = current.some((item) => item.userId === peerId);
        if (!exists) return current;
        return current.map((item) => item.userId === peerId ? { ...item, lastMessage: message.content, updatedAt: message.createdAt } : item);
      });

      if (peerId === selectedId) {
        setMessages((current) => {
          if (current.some((item) => item.id === message.id)) return current;
          return [...current, message];
        });
        if (message.receiverId === myUserId) socket.emit("message_read", { messageId: message.id });
      }
    };

    const onMessageStatus = (update) => {
      setMessages((current) => current.map((message) => message.id === update.messageId ? { ...message, ...update } : message));
    };

    socket.on("receive_message", onReceiveMessage);
    socket.on("message_status", onMessageStatus);
    return () => {
      socket.off("receive_message", onReceiveMessage);
      socket.off("message_status", onMessageStatus);
    };
  }, [socket, myUserId, selectedId]);

  const filteredConversations = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return conversations;
    return conversations.filter((item) => [item.name, item.lastMessage].filter(Boolean).join(" ").toLowerCase().includes(query));
  }, [conversations, searchQuery]);

  const sendMessage = async (event) => {
    event.preventDefault();
    const content = newMessage.trim();
    if (!content || !selectedConversation || !user) return;

    const clientMessageId = makeClientMessageId();
    const optimistic = {
      id: `local-${clientMessageId}`,
      clientMessageId,
      senderId: myUserId,
      receiverId: selectedConversation.userId,
      content,
      createdAt: new Date().toISOString(),
      status: "local_pending",
    };

    setNewMessage("");
    setMessages((current) => [...current, optimistic]);

    const finalize = (serverMessage) => {
      setMessages((current) => current.map((message) =>
        message.clientMessageId === clientMessageId
          ? { ...serverMessage, clientMessageId, status: serverMessage.status || "sent" }
          : message
      ));
    };

    const fail = (message) => {
      setMessages((current) => current.map((item) =>
        item.clientMessageId === clientMessageId ? { ...item, status: "failed", error: message } : item
      ));
    };

    try {
      if (socket?.connected) {
        socket.timeout(8000).emit(
          "send_message",
          { receiverId: selectedConversation.userId, content, clientMessageId },
          (timeoutError, response) => {
            if (timeoutError || !response?.ok) {
              fail(response?.error || "Message failed to send.");
              return;
            }
            finalize(response.message);
          }
        );
      } else {
        const response = await api.post("/chat/send", {
          receiverId: selectedConversation.userId,
          content,
          clientMessageId,
        });
        finalize(response.data);
      }
    } catch (err) {
      fail(err.response?.data?.error || "Message failed to send.");
    }
  };

  const retryMessage = async (message) => {
    if (!message?.clientMessageId || !selectedConversation) return;
    setMessages((current) => current.map((item) => item.clientMessageId === message.clientMessageId ? { ...item, status: "local_pending", error: null } : item));
    try {
      const response = await api.post("/chat/send", {
        receiverId: selectedConversation.userId,
        content: message.content,
        clientMessageId: message.clientMessageId,
      });
      setMessages((current) => current.map((item) => item.clientMessageId === message.clientMessageId ? response.data : item));
    } catch (err) {
      setMessages((current) => current.map((item) => item.clientMessageId === message.clientMessageId ? { ...item, status: "failed", error: err.response?.data?.error || "Retry failed." } : item));
    }
  };

  if (!user) {
    return (
      <section className="panel" style={{ padding: "2rem", textAlign: "center" }}>
        <h1 style={{ marginTop: 0 }}>Secure Messaging</h1>
        <p style={{ color: "var(--ink-muted)" }}>Sign in to view your real conversations. Demo conversations are not shown.</p>
        <Link href="/login" className="button button-primary">Sign In</Link>
      </section>
    );
  }

  return (
    <div className="panel" style={{ padding: "0.7rem", minHeight: "min(76vh, 760px)" }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: "0.8rem", alignItems: "center", marginBottom: "0.65rem" }}>
        <div>
          <p className="section-label">Conversations</p>
          <h1 style={{ margin: 0, fontSize: "1.6rem" }}>Secure Messaging</h1>
        </div>
        {selectedConversation && <Link href={`/profile/${selectedConversation.userId}`} className="button button-secondary">View Profile</Link>}
      </div>

      {conversationState === "error" && (
        <section className="panel" role="alert" style={{ padding: "1rem", marginBottom: "0.7rem" }}>
          <p style={{ marginTop: 0 }}>{error}</p>
          <button type="button" className="button button-primary" onClick={loadConversations}>Retry</button>
        </section>
      )}

      <div className="chat-shell-grid" style={{ display: "grid", gridTemplateColumns: "320px minmax(0,1fr)", gap: "0.65rem" }}>
        <aside className="panel" style={{ padding: "0.65rem" }}>
          <input
            className="form-input"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Search conversations"
            aria-label="Search conversations"
          />
          <div style={{ display: "grid", gap: "0.4rem", marginTop: "0.55rem", maxHeight: "64vh", overflowY: "auto" }}>
            {conversationState === "loading" && <p style={{ color: "var(--ink-muted)" }}>Loading conversations…</p>}
            {conversationState === "content" && filteredConversations.length === 0 && (
              <p style={{ color: "var(--ink-muted)" }}>No active conversations.</p>
            )}
            {filteredConversations.map((conversation) => (
              <button
                key={conversation.userId}
                type="button"
                onClick={() => setSelectedId(conversation.userId)}
                style={{
                  width: "100%",
                  textAlign: "left",
                  borderRadius: 14,
                  border: conversation.userId === selectedId ? "1px solid var(--brand)" : "1px solid var(--line)",
                  background: conversation.userId === selectedId ? "rgba(240,107,78,0.08)" : "white",
                  padding: "0.55rem",
                  cursor: "pointer",
                  display: "grid",
                  gridTemplateColumns: "48px minmax(0,1fr)",
                  gap: "0.52rem",
                }}
              >
                <SafeProfileImage
                  src={conversation.photo}
                  alt={conversation.name ? `${conversation.name} profile` : "Profile photo"}
                  width={48}
                  height={48}
                  sizes="48px"
                  style={{ width: 48, height: 48, borderRadius: 12, objectFit: "cover" }}
                />
                <div style={{ minWidth: 0 }}>
                  <strong style={{ fontSize: "0.88rem" }}>{conversation.name || "Member"}</strong>
                  {conversation.lastMessage && (
                    <p style={{ margin: "0.2rem 0 0", color: "var(--ink-muted)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", fontSize: "0.76rem" }}>
                      {conversation.lastMessage}
                    </p>
                  )}
                </div>
              </button>
            ))}
          </div>
        </aside>

        <section className="panel" style={{ padding: "0.75rem", minHeight: "66vh", display: "grid", gridTemplateRows: "auto minmax(0,1fr) auto" }}>
          {!selectedConversation ? (
            <div style={{ display: "grid", placeItems: "center", color: "var(--ink-muted)" }}>Select a conversation</div>
          ) : (
            <>
              <header style={{ display: "flex", gap: "0.6rem", alignItems: "center", paddingBottom: "0.6rem", borderBottom: "1px solid var(--line)" }}>
                <SafeProfileImage
                  src={selectedConversation.photo}
                  alt={selectedConversation.name ? `${selectedConversation.name} profile` : "Profile photo"}
                  width={46}
                  height={46}
                  sizes="46px"
                  style={{ width: 46, height: 46, borderRadius: 12, objectFit: "cover" }}
                />
                <strong>{selectedConversation.name || "Member"}</strong>
              </header>

              <div style={{ overflowY: "auto", padding: "0.72rem 0", display: "grid", gap: "0.48rem" }}>
                {messageState === "loading" && <p style={{ color: "var(--ink-muted)", textAlign: "center" }}>Loading messages…</p>}
                {messageState === "error" && (
                  <div role="alert" style={{ textAlign: "center" }}>
                    <p style={{ color: "var(--ink-muted)" }}>{error}</p>
                    <button type="button" className="button button-secondary" onClick={() => loadMessages(selectedId)}>Retry</button>
                  </div>
                )}
                {messageState === "content" && messages.length === 0 && (
                  <p style={{ color: "var(--ink-muted)", textAlign: "center" }}>No messages yet.</p>
                )}
                {messages.map((message) => {
                  const mine = message.senderId === myUserId;
                  return (
                    <div key={message.id || message.clientMessageId} style={{ display: "flex", justifyContent: mine ? "flex-end" : "flex-start" }}>
                      <div style={{
                        maxWidth: "72%",
                        borderRadius: mine ? "16px 16px 6px 16px" : "16px 16px 16px 6px",
                        background: mine ? "linear-gradient(135deg, var(--brand), var(--brand-deep))" : "#fff",
                        border: mine ? "none" : "1px solid var(--line)",
                        color: mine ? "#fff" : "var(--ink)",
                        padding: "0.6rem 0.72rem",
                      }}>
                        <p style={{ margin: 0, fontSize: "0.88rem" }}>{message.content}</p>
                        <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.45rem", marginTop: "0.2rem", fontSize: "0.68rem", opacity: 0.78 }}>
                          <span>{formatTime(message.createdAt)}</span>
                          {mine && <span>{String(message.status || "sent").replace("_", " ")}</span>}
                        </div>
                        {message.status === "failed" && mine && (
                          <button type="button" onClick={() => retryMessage(message)} style={{ marginTop: 6, border: 0, borderRadius: 8, padding: "4px 8px", cursor: "pointer" }}>
                            Retry
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
                <div ref={scrollAnchorRef} />
              </div>

              <form onSubmit={sendMessage} style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.42rem", borderTop: "1px solid var(--line)", paddingTop: "0.62rem" }}>
                <input
                  className="form-input"
                  value={newMessage}
                  onChange={(event) => setNewMessage(event.target.value)}
                  placeholder={`Message ${selectedConversation.name || "member"}…`}
                  maxLength={4000}
                />
                <button type="submit" className="button button-primary" disabled={!newMessage.trim()}>Send</button>
              </form>
            </>
          )}
        </section>
      </div>
    </div>
  );
}
