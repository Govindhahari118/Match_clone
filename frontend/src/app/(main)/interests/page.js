"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import LoginPromptModal from "../../../components/LoginPromptModal";

const PROFILE_PLACEHOLDER = "/profile-placeholder.svg";
const TABS = [
  { key: "received", label: "Received" },
  { key: "sent", label: "Sent" },
  { key: "mutual", label: "Mutual" },
];
const STATUS_META = {
  sent: { label: "Pending", tone: "status-pending" },
  pending: { label: "Pending", tone: "status-pending" },
  accepted: { label: "Accepted", tone: "status-accepted" },
  rejected: { label: "Declined", tone: "status-declined" },
  declined: { label: "Declined", tone: "status-declined" },
  withdrawn: { label: "Withdrawn", tone: "status-declined" },
};

export default function InterestsPage() {
  const { user } = useAuth();
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [activeTab, setActiveTab] = useState("received");
  const [data, setData] = useState({ received: [], sent: [], mutual: [] });
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [busyId, setBusyId] = useState(null);

  const loadTab = async (tab = activeTab) => {
    if (!user) {
      setData((previous) => ({ ...previous, [tab]: [] }));
      return;
    }
    setLoading(true);
    setErrorMessage("");
    try {
      const res = await api.get(`/interactions/interests?type=${tab}`);
      setData((previous) => ({ ...previous, [tab]: Array.isArray(res.data) ? res.data : [] }));
    } catch (error) {
      setData((previous) => ({ ...previous, [tab]: [] }));
      setErrorMessage(error.response?.data?.error || "Interests could not be loaded. No demo profiles were substituted.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTab(activeTab);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, user]);

  const currentList = data[activeTab] || [];
  const tabCounts = useMemo(
    () => ({
      received: data.received?.length || 0,
      sent: data.sent?.length || 0,
      mutual: data.mutual?.length || 0,
    }),
    [data]
  );
  const pendingSent = useMemo(
    () => (data.sent || []).filter((item) => ["sent", "pending"].includes(item.status)).length,
    [data.sent]
  );

  const handleAccept = async (userId) => {
    if (!user) return setShowLoginModal(true);
    setBusyId(userId);
    try {
      const response = await api.post("/interactions/like", { receiverId: userId });
      toast.success(response.data?.isMatch ? "Interest accepted. You are now connected." : "Interest accepted.");
      await Promise.all([loadTab("received"), loadTab("mutual")]);
    } catch (error) {
      toast.error(error.response?.data?.error || "Unable to accept this interest.");
    } finally {
      setBusyId(null);
    }
  };

  const handleDecline = async (userId) => {
    if (!user) return setShowLoginModal(true);
    setBusyId(userId);
    try {
      await api.post("/interactions/decline", { userId });
      toast.info("Interest declined.");
      await loadTab("received");
    } catch (error) {
      toast.error(error.response?.data?.error || "Unable to decline this interest.");
    } finally {
      setBusyId(null);
    }
  };

  const handleWithdraw = async (userId) => {
    if (!user) return setShowLoginModal(true);
    setBusyId(userId);
    try {
      await api.post("/interactions/withdraw", { receiverId: userId });
      toast.info("Interest withdrawn. You can send a new interest later if the profile is still available.");
      await loadTab("sent");
    } catch (error) {
      toast.error(error.response?.data?.error || "Unable to withdraw this interest.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div style={{ display: "grid", gap: "0.95rem" }}>
      <section className="listing-hero">
        <div>
          <p className="section-label" style={{ marginBottom: "0.22rem" }}>Relationship Pipeline</p>
          <h1 className="section-title" style={{ margin: 0, fontSize: "clamp(1.64rem, 3vw, 2.2rem)" }}>Interest Center</h1>
          <p className="section-copy" style={{ marginTop: "0.38rem", fontSize: "0.92rem" }}>
            Every item here is backed by a real account interaction. Pending requests can be withdrawn, and declined or blocked interactions are not presented as active engagement.
          </p>
          <div className="result-metrics">
            <span className="metric-chip metric-chip-highlight">{tabCounts.received} received</span>
            <span className="metric-chip">{pendingSent} pending sent</span>
            <span className="metric-chip">{tabCounts.mutual} mutual</span>
          </div>
        </div>
      </section>

      {!user && (
        <section className="panel" style={{ padding: "1rem", textAlign: "center" }}>
          <p style={{ margin: "0 0 0.7rem", color: "var(--ink-muted)" }}>Sign in to view real interests. This screen does not fabricate preview requests.</p>
          <button type="button" className="button button-primary" onClick={() => setShowLoginModal(true)}>Sign in</button>
        </section>
      )}

      <section className="panel interests-tabs-shell" style={{ padding: "0.6rem" }}>
        <div className="interests-tab-row">
          {TABS.map((tab) => (
            <button key={tab.key} type="button" className={`interests-tab-btn ${activeTab === tab.key ? "active" : ""}`} onClick={() => setActiveTab(tab.key)}>
              <span>{tab.label}</span>
              <span className="tab-badge">{tabCounts[tab.key]}</span>
            </button>
          ))}
        </div>
      </section>

      {errorMessage && (
        <section className="panel" style={{ padding: "0.9rem", borderColor: "rgba(185, 28, 28, 0.2)" }}>
          <p style={{ margin: 0, color: "#991b1b" }}>{errorMessage}</p>
          <button type="button" className="button button-secondary" style={{ marginTop: "0.65rem" }} onClick={() => loadTab(activeTab)}>Retry</button>
        </section>
      )}

      {loading ? (
        <div style={{ display: "grid", gap: "0.75rem" }}>
          {Array.from({ length: 4 }).map((_, index) => <div key={`interest-loading-${index}`} className="panel listing-stage skeleton-tile" style={{ height: 110 }} />)}
        </div>
      ) : currentList.length === 0 ? (
        <section className="panel listing-stage" style={{ textAlign: "center", padding: "2.2rem" }}>
          <h2 style={{ marginTop: 0, marginBottom: "0.38rem" }}>No {TABS.find((item) => item.key === activeTab)?.label.toLowerCase()} interests right now</h2>
          <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.9rem" }}>
            {user ? "This is the real current state of your account." : "Sign in to load your account interactions."}
          </p>
          {user && <Link href="/matches" className="button button-primary" style={{ marginTop: "0.9rem" }}>Browse Matches</Link>}
        </section>
      ) : (
        <div style={{ display: "grid", gap: "0.72rem" }}>
          {currentList.map((item) => {
            const statusMeta = STATUS_META[item.status];
            const timeLabel = activeTab === "received" ? `Received ${item.receivedAt || "recently"}` : activeTab === "sent" ? `Sent ${item.sentAt || item.receivedAt || "recently"}` : `Matched ${item.matchedAt || "recently"}`;
            return (
              <article key={`${activeTab}-${item.id || item.userId}`} className="panel listing-stage interest-card" style={{ padding: "0.8rem" }}>
                <div className="interest-main-row">
                  <div className="interest-avatar-wrap">
                    <Image src={item.photo || PROFILE_PLACEHOLDER} alt={item.photo ? `${item.firstName} profile` : "Profile photo unavailable"} width={74} height={74} sizes="74px" className="interest-avatar" />
                  </div>

                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.45rem", flexWrap: "wrap" }}>
                      <h3 style={{ margin: 0, fontSize: "1rem", lineHeight: 1.15 }}>{item.firstName || "Member"}{item.age ? `, ${item.age}` : ""}</h3>
                      {item.isVerified && <span className="chip chip-support">Account verified</span>}
                      {item.activity?.label && <span className="chip chip-brand">{item.activity.label}</span>}
                    </div>
                    <p className="profile-meta" style={{ margin: "0.22rem 0 0", color: "var(--ink-muted)", fontSize: "0.86rem" }}>
                      {[item.profession, item.city].filter(Boolean).join(" · ") || "Profile details available on view"}
                    </p>
                    <p style={{ margin: "0.28rem 0 0", color: "var(--ink-muted)", fontSize: "0.76rem" }}>{timeLabel}{item.managedBy ? ` · Managed by ${item.managedBy}` : ""}</p>
                    {activeTab === "sent" && statusMeta && <span className={`status-pill ${statusMeta.tone}`}>{statusMeta.label}</span>}
                  </div>

                  <div className="interest-actions-wrap">
                    {activeTab === "received" && (
                      <>
                        <button type="button" className="button button-primary" disabled={busyId === item.userId} onClick={() => handleAccept(item.userId)}>Accept</button>
                        <button type="button" className="button button-secondary" disabled={busyId === item.userId} onClick={() => handleDecline(item.userId)}>Decline</button>
                      </>
                    )}
                    {activeTab === "sent" && ["sent", "pending"].includes(item.status) && (
                      <button type="button" className="button button-secondary" disabled={busyId === item.userId} onClick={() => handleWithdraw(item.userId)}>Withdraw</button>
                    )}
                    {(activeTab === "mutual" || (activeTab === "sent" && item.status === "accepted")) && <Link href="/chat" className="button button-primary">Chat</Link>}
                    <Link href={`/profile/${item.userId}`} className="button button-secondary">View</Link>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />
    </div>
  );
}
