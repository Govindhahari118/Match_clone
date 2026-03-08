"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import LoginPromptModal from "../../../components/LoginPromptModal";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";

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

function makeClientMessageId() {
  return `cm_${Date.now()}_${Math.floor(Math.random() * 100000)}`;
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
  const [typingPeer, setTypingPeer] = useState(false);

  const scrollAnchorRef = useRef(null);
  const typingTimeoutRef = useRef(null);

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
        await api.post(`/chat/${selectedId}/read`);
        if (socket?.connected) {
          socket.emit("conversation_seen", { viewerId: myUserId, peerId: selectedId });
        }

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
  }, [selectedId, user, socket, myUserId]);

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
        socket.emit("conversation_seen", { viewerId: myUserId, peerId });
      }
    };

    const onMessageAck = (ack = {}) => {
      setMessages((prev) =>
        prev.map((item) => {
          const clientMatched = ack.clientMessageId && item.clientMessageId === ack.clientMessageId;
          const idMatched = ack.messageId && item.id === ack.messageId;
          if (!clientMatched && !idMatched) return item;
          return {
            ...item,
            id: ack.messageId || item.id,
            status: ack.status || item.status || "sent",
          };
        })
      );
    };

    const onMessageStatus = (update = {}) => {
      if (!update.messageId) return;
      setMessages((prev) =>
        prev.map((item) => (item.id === update.messageId ? { ...item, status: update.status || item.status } : item))
      );
    };

    const onTyping = (payload = {}) => {
      const isPeerTyping = payload.senderId === selectedId && payload.receiverId === myUserId && payload.isTyping;
      if (!isPeerTyping) return;
      setTypingPeer(true);
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = setTimeout(() => setTypingPeer(false), 1400);
    };

    const onConversationSeen = (payload = {}) => {
      if (payload.viewerId !== selectedId) return;
      setMessages((prev) =>
        prev.map((item) => (item.senderId === myUserId ? { ...item, status: "seen" } : item))
      );
    };

    socket.on("receive_message", onReceiveMessage);
    socket.on("message_ack", onMessageAck);
    socket.on("message_status", onMessageStatus);
    socket.on("typing", onTyping);
    socket.on("conversation_seen", onConversationSeen);

    return () => {
      socket.off("receive_message", onReceiveMessage);
      socket.off("message_ack", onMessageAck);
      socket.off("message_status", onMessageStatus);
      socket.off("typing", onTyping);
      socket.off("conversation_seen", onConversationSeen);
    };
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

    const clientMessageId = makeClientMessageId();
    const payload = {
      id: clientMessageId,
      clientMessageId,
      senderId: myUserId,
      receiverId: selectedConversation.userId,
      content: newMessage.trim(),
      createdAt: new Date().toISOString(),
      status: "sent",
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
        const response = await api.post("/chat/send", {
          receiverId: selectedConversation.userId,
          content: payload.content,
          clientMessageId,
        });
        const serverMessage = response?.data;
        if (serverMessage?.id) {
          setMessages((prev) =>
            prev.map((item) =>
              item.clientMessageId === clientMessageId
                ? { ...item, id: serverMessage.id, status: serverMessage.status || "sent" }
                : item
            )
          );
        }
      }
    } catch {
      setInfoMessage("Message queued in demo mode.");
      setMessages((prev) =>
        prev.map((item) => (item.clientMessageId === clientMessageId ? { ...item, status: "failed" } : item))
      );
    }
  };

  return (
    <div className="panel chat-page-v2">
      <header className="chat-page-head-v2">
        <div>
          <p className="section-label">Conversations</p>
          <h1 className="chat-title-v2">Secure Messaging</h1>
        </div>
        {selectedConversation && (
          <Link href={`/profile/${selectedConversation.userId}`} className="button button-secondary">
            View Profile
          </Link>
        )}
      </header>

      {infoMessage && <div className="chip chip-brand chat-info-v2">{infoMessage}</div>}

      <div className="chat-shell-grid chat-grid-v2">
        <aside className="panel chat-sidebar-v2">
          <input
            className="form-input chat-search-v2"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Search conversations"
          />

          <div className="chat-thread-list-v2">
            {loadingConversations ? (
              <PageLoadingState title="Loading conversations..." description="Syncing your latest chat threads." compact />
            ) : filteredConversations.length === 0 ? (
              <PageEmptyState
                title="No conversations yet"
                description="Your active chats will appear here after a mutual match."
                primaryActionLabel="Explore Matches"
                primaryActionHref="/matches"
              />
            ) : (
              filteredConversations.map((conversation) => {
                const active = conversation.userId === selectedId;
                return (
                  <button
                    key={conversation.userId}
                    type="button"
                    onClick={() => setSelectedId(conversation.userId)}
                    className={`chat-thread-v2 ${active ? "active" : ""}`}
                  >
                    <Image
                      src={conversation.photo}
                      alt={`${conversation.name} photo`}
                      width={48}
                      height={48}
                      sizes="48px"
                      className="chat-thread-avatar-v2"
                    />
                    <div className="chat-thread-copy-v2">
                      <div className="chat-thread-row-v2">
                        <strong>{conversation.name}</strong>
                        <span>{formatTime(conversation.updatedAt)}</span>
                      </div>
                      <p>{conversation.lastMessage}</p>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </aside>

        <section className="panel chat-main-v2">
          {!selectedConversation ? (
            <PageEmptyState title="Select a conversation" description="Pick a thread from the left panel to start messaging." />
          ) : (
            <>
              <header className="chat-main-head-v2">
                <Image
                  src={selectedConversation.photo}
                  alt={`${selectedConversation.name} photo`}
                  width={46}
                  height={46}
                  sizes="46px"
                  className="chat-main-avatar-v2"
                />
                <div>
                  <strong>{selectedConversation.name}</strong>
                  <p>
                    {selectedConversation.role || "Professional"} | {selectedConversation.city || "India"}
                  </p>
                  {typingPeer && <span className="chat-typing-v2">Typing...</span>}
                </div>
              </header>

              <div className="chat-message-list-v2">
                {loadingMessages ? (
                  <PageLoadingState title="Loading messages..." description="Opening this conversation." compact />
                ) : messages.length === 0 ? (
                  <PageEmptyState title="No messages yet" description="Start with a simple introduction to break the ice." />
                ) : (
                  messages.map((message, index) => {
                    const mine = message.senderId === myUserId || message.senderId === "me";
                    return (
                      <div key={`${message.id || "m"}-${index}`} className={`chat-message-row-v2 ${mine ? "mine" : "peer"}`}>
                        <div className={`chat-bubble-v2 ${mine ? "mine" : "peer"} ${message.status === "failed" ? "failed" : ""}`}>
                          <p>{message.content}</p>
                          <small>
                            {formatTime(message.createdAt || Date.now())}
                            {mine && message.status ? ` | ${message.status}` : ""}
                          </small>
                        </div>
                      </div>
                    );
                  })
                )}
                <div ref={scrollAnchorRef} />
              </div>

              <form onSubmit={sendMessage} className="chat-input-row-v2">
                <input
                  className="form-input"
                  value={newMessage}
                  onChange={(event) => {
                    const value = event.target.value;
                    setNewMessage(value);
                    if (socket?.connected && selectedConversation && user) {
                      socket.emit("typing", {
                        senderId: myUserId,
                        receiverId: selectedConversation.userId,
                        isTyping: value.trim().length > 0,
                      });
                    }
                  }}
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
    </div>
  );
}
