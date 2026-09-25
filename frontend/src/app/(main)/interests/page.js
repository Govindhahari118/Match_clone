"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import SafeProfileImage from "../../../components/SafeProfileImage";

const TABS = [
  { key: "received", label: "Received" },
  { key: "sent", label: "Sent" },
  { key: "mutual", label: "Mutual" },
];

const STATUS_LABEL = {
  sent: "Pending",
  accepted: "Accepted",
  rejected: "Declined",
  withdrawn: "Withdrawn",
  blocked: "Unavailable",
};

export default function InterestsPage() {
  const [activeTab, setActiveTab] = useState("received");
  const [data, setData] = useState({ received: [], sent: [], mutual: [] });
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");
  const [busyUserId, setBusyUserId] = useState(null);

  const load = useCallback(async (tab) => {
    setState("loading");
    setError("");
    try {
      const response = await api.get("/interactions/interests", { params: { type: tab } });
      const list = Array.isArray(response.data) ? response.data : [];
      setData((previous) => ({ ...previous, [tab]: list }));
      setState("content");
    } catch (err) {
      setData((previous) => ({ ...previous, [tab]: [] }));
      setError(err.response?.data?.error || "Couldn’t load interests.");
      setState("error");
    }
  }, []);

  useEffect(() => {
    load(activeTab);
  }, [activeTab, load]);

  const currentList = data[activeTab] || [];
  const counts = useMemo(() => ({
    received: data.received.length,
    sent: data.sent.length,
    mutual: data.mutual.length,
  }), [data]);

  const handleAccept = async (userId) => {
    setBusyUserId(userId);
    try {
      await api.post(`/interactions/interests/${userId}/accept`);
      toast.success("Interest accepted. You are now connected.");
      await load("received");
      const mutual = await api.get("/interactions/interests", { params: { type: "mutual" } });
      setData((previous) => ({ ...previous, mutual: Array.isArray(mutual.data) ? mutual.data : [] }));
    } catch (err) {
      toast.error(err.response?.data?.error || "Couldn’t accept this interest.");
    } finally {
      setBusyUserId(null);
    }
  };

  const handleDecline = async (userId) => {
    setBusyUserId(userId);
    try {
      await api.post("/interactions/decline", { userId });
      toast.info("Interest declined.");
      await load("received");
    } catch (err) {
      toast.error(err.response?.data?.error || "Couldn’t decline this interest.");
    } finally {
      setBusyUserId(null);
    }
  };

  const handleWithdraw = async (userId) => {
    setBusyUserId(userId);
    try {
      await api.post(`/interactions/interests/${userId}/withdraw`);
      toast.info("Pending interest withdrawn.");
      await load("sent");
    } catch (err) {
      toast.error(err.response?.data?.error || "Couldn’t withdraw this interest.");
    } finally {
      setBusyUserId(null);
    }
  };

  return (
    <div style={{ display: "grid", gap: "0.95rem" }}>
      <section className="listing-hero">
        <div>
          <p className="section-label">Relationship Activity</p>
          <h1 className="section-title" style={{ margin: 0 }}>Interest Center</h1>
          <p className="section-copy">Every state below comes from the server. Accept, decline, and withdraw complete only after persistence succeeds.</p>
        </div>
      </section>

      <section className="panel" style={{ padding: "0.6rem" }}>
        <div className="interests-tab-row">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              className={`interests-tab-btn ${activeTab === tab.key ? "active" : ""}`}
              onClick={() => setActiveTab(tab.key)}
            >
              <span>{tab.label}</span>
              <span className="tab-badge">{counts[tab.key]}</span>
            </button>
          ))}
        </div>
      </section>

      {state === "loading" && <section className="panel" style={{ padding: "1.4rem" }}>Loading interests…</section>}

      {state === "error" && (
        <section className="panel" role="alert" style={{ padding: "1.4rem" }}>
          <h2 style={{ marginTop: 0 }}>Couldn’t load interests</h2>
          <p style={{ color: "var(--ink-muted)" }}>{error}</p>
          <button type="button" className="button button-primary" onClick={() => load(activeTab)}>Retry</button>
        </section>
      )}

      {state === "content" && currentList.length === 0 && (
        <section className="panel" style={{ textAlign: "center", padding: "2rem" }}>
          <h2 style={{ marginTop: 0 }}>No {TABS.find((tab) => tab.key === activeTab)?.label.toLowerCase()} interests</h2>
          <p style={{ color: "var(--ink-muted)" }}>Nothing is fabricated to fill an empty state.</p>
          <Link href="/matches" className="button button-primary">Browse Matches</Link>
        </section>
      )}

      {state === "content" && currentList.length > 0 && (
        <div style={{ display: "grid", gap: "0.72rem" }}>
          {currentList.map((item) => {
            const status = item.status || (activeTab === "mutual" ? "accepted" : "sent");
            const timeLabel = activeTab === "received" ? item.receivedAt : activeTab === "sent" ? item.sentAt : item.matchedAt;
            return (
              <article key={item.userId || item.id} className="panel listing-stage" style={{ padding: "0.8rem" }}>
                <div style={{ display: "flex", gap: "0.75rem", alignItems: "center", flexWrap: "wrap" }}>
                  <SafeProfileImage
                    src={item.photo}
                    alt={item.firstName ? `${item.firstName} profile` : "Profile photo"}
                    width={74}
                    height={74}
                    sizes="74px"
                    style={{ width: 74, height: 74, borderRadius: 16, objectFit: "cover", flexShrink: 0 }}
                  />
                  <div style={{ minWidth: 180, flex: 1 }}>
                    <div style={{ display: "flex", gap: "0.4rem", alignItems: "center", flexWrap: "wrap" }}>
                      <h3 style={{ margin: 0, fontSize: "1rem" }}>
                        {item.firstName || "Member"}{item.age ? `, ${item.age}` : ""}
                      </h3>
                      {item.isVerified && <span className="chip chip-support">Verified</span>}
                      <span className="chip chip-brand">{STATUS_LABEL[status] || status}</span>
                    </div>
                    <p style={{ margin: "0.25rem 0 0", color: "var(--ink-muted)", fontSize: "0.84rem" }}>
                      {[item.profession, item.city].filter(Boolean).join(" · ")}
                    </p>
                    {timeLabel && <p style={{ margin: "0.22rem 0 0", color: "var(--ink-muted)", fontSize: "0.74rem" }}>{timeLabel}</p>}
                  </div>
                  <div style={{ display: "flex", gap: "0.45rem", flexWrap: "wrap" }}>
                    {activeTab === "received" && (
                      <>
                        <button type="button" className="button button-primary" disabled={busyUserId === item.userId} onClick={() => handleAccept(item.userId)}>Accept</button>
                        <button type="button" className="button button-secondary" disabled={busyUserId === item.userId} onClick={() => handleDecline(item.userId)}>Decline</button>
                      </>
                    )}
                    {activeTab === "sent" && status === "sent" && (
                      <button type="button" className="button button-secondary" disabled={busyUserId === item.userId} onClick={() => handleWithdraw(item.userId)}>Withdraw</button>
                    )}
                    {(activeTab === "mutual" || status === "accepted") && (
                      <Link href={`/chat?user=${item.userId}`} className="button button-primary">Chat</Link>
                    )}
                    <Link href={`/profile/${item.userId}`} className="button button-secondary">View</Link>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
