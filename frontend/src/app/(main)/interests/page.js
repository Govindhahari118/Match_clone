"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";

const MOCK_RECEIVED = [
  { id: "r1", userId: "u1", firstName: "Arjun", age: 29, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/men/11.jpg", isVerified: true, match: 92, receivedAt: "2h ago" },
  { id: "r2", userId: "u2", firstName: "Rohan", age: 27, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/men/12.jpg", isVerified: true, match: 86, receivedAt: "5h ago" },
  { id: "r3", userId: "u3", firstName: "Karan", age: 31, city: "Delhi", profession: "Lawyer", photo: "https://randomuser.me/api/portraits/men/13.jpg", isVerified: false, match: 80, receivedAt: "1d ago" },
  { id: "r4", userId: "u4", firstName: "Vivek", age: 28, city: "Pune", profession: "MBA Exec", photo: "https://randomuser.me/api/portraits/men/14.jpg", isVerified: true, match: 75, receivedAt: "2d ago" },
];

const MOCK_SENT = [
  { id: "s1", userId: "u5", firstName: "Priya", age: 26, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/women/44.jpg", isVerified: true, match: 94, status: "pending", sentAt: "1h ago" },
  { id: "s2", userId: "u6", firstName: "Ananya", age: 24, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/women/45.jpg", isVerified: true, match: 89, status: "accepted", sentAt: "3h ago" },
  { id: "s3", userId: "u7", firstName: "Kavya", age: 27, city: "Chennai", profession: "CA", photo: "https://randomuser.me/api/portraits/women/46.jpg", isVerified: false, match: 75, status: "declined", sentAt: "1d ago" },
];

const MOCK_MUTUAL = [
  { id: "m1", userId: "u8", firstName: "Nisha", age: 25, city: "Hyderabad", profession: "Designer", photo: "https://randomuser.me/api/portraits/women/49.jpg", isVerified: true, match: 88, matchedAt: "30m ago" },
  { id: "m2", userId: "u9", firstName: "Shreya", age: 26, city: "Jaipur", profession: "Teacher", photo: "https://randomuser.me/api/portraits/women/50.jpg", isVerified: false, match: 81, matchedAt: "2h ago" },
  { id: "m3", userId: "u10", firstName: "Tanvi", age: 28, city: "Kolkata", profession: "Architect", photo: "https://randomuser.me/api/portraits/women/51.jpg", isVerified: true, match: 77, matchedAt: "1d ago" },
];

const TABS = [
  { key: "received", label: "Received" },
  { key: "sent", label: "Sent" },
  { key: "mutual", label: "Mutual" },
];

const STATUS_META = {
  pending: { label: "Pending", tone: "status-pending" },
  accepted: { label: "Accepted", tone: "status-accepted" },
  declined: { label: "Declined", tone: "status-declined" },
};

function getMatchTier(matchScore) {
  if (matchScore >= 90) return "elite";
  if (matchScore >= 80) return "strong";
  return "rising";
}

export default function InterestsPage() {
  const [activeTab, setActiveTab] = useState("received");
  const [data, setData] = useState({ received: MOCK_RECEIVED, sent: MOCK_SENT, mutual: MOCK_MUTUAL });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchInterests = async () => {
      setLoading(true);
      try {
        const res = await api.get(`/interactions/interests?type=${activeTab}`);
        if (Array.isArray(res.data) && res.data.length > 0) {
          setData((previous) => ({ ...previous, [activeTab]: res.data }));
        }
      } catch {
        // Keep fallback mock data in demo mode.
      } finally {
        setLoading(false);
      }
    };

    fetchInterests();
  }, [activeTab]);

  const currentList = data[activeTab] || [];

  const tabCounts = useMemo(
    () => ({
      received: data.received?.length || 0,
      sent: data.sent?.length || 0,
      mutual: data.mutual?.length || 0,
    }),
    [data]
  );

  const headlineStats = useMemo(() => {
    const pendingSent = (data.sent || []).filter((item) => item.status === "pending").length;
    return {
      total: tabCounts.received + tabCounts.sent + tabCounts.mutual,
      pendingReceived: tabCounts.received,
      pendingSent,
      mutual: tabCounts.mutual,
    };
  }, [tabCounts, data.sent]);

  const handleAccept = async (userId) => {
    try {
      await api.post("/interactions/like", { receiverId: userId });
      toast.success("Interest accepted.");
    } catch {
      toast.info("Accepted in demo mode.");
    }

    setData((previous) => ({
      ...previous,
      received: previous.received.filter((item) => item.userId !== userId),
    }));
  };

  const handleDecline = async (userId) => {
    try {
      await api.post("/interactions/decline", { userId });
      toast.info("Interest declined.");
    } catch {
      toast.info("Declined in demo mode.");
    }

    setData((previous) => ({
      ...previous,
      received: previous.received.filter((item) => item.userId !== userId),
    }));
  };

  return (
    <div style={{ display: "grid", gap: "0.95rem" }}>
      <section className="listing-hero">
        <div>
          <p className="section-label" style={{ marginBottom: "0.22rem" }}>
            Relationship Pipeline
          </p>
          <h1 className="section-title" style={{ margin: 0, fontSize: "clamp(1.64rem, 3vw, 2.2rem)" }}>
            Interest Center
          </h1>
          <p className="section-copy" style={{ marginTop: "0.38rem", fontSize: "0.92rem" }}>
            Track incoming interests, outgoing requests, and mutual connections in one streamlined view.
          </p>

          <div className="result-metrics">
            <span className="metric-chip metric-chip-highlight">{headlineStats.pendingReceived} received</span>
            <span className="metric-chip">{headlineStats.pendingSent} pending sent</span>
            <span className="metric-chip">{headlineStats.mutual} mutual</span>
            <span className="metric-chip">{headlineStats.total} total</span>
          </div>
        </div>
      </section>

      <section className="panel interests-tabs-shell" style={{ padding: "0.6rem" }}>
        <div className="interests-tab-row">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              className={`interests-tab-btn ${activeTab === tab.key ? "active" : ""}`}
              onClick={() => setActiveTab(tab.key)}
            >
              <span>{tab.label}</span>
              <span className="tab-badge">{tabCounts[tab.key]}</span>
            </button>
          ))}
        </div>
      </section>

      {loading ? (
        <PageLoadingState
          title="Loading interests..."
          description="Fetching your latest incoming, sent, and mutual activity."
          compact
        />
      ) : currentList.length === 0 ? (
        <PageEmptyState
          title={`No ${TABS.find((item) => item.key === activeTab)?.label.toLowerCase()} interests right now`}
          description="Continue exploring profiles to keep your pipeline active and increase high-quality conversations."
          primaryActionLabel="Browse Matches"
          primaryActionHref="/matches"
        />
      ) : (
        <div style={{ display: "grid", gap: "0.72rem" }}>
          {currentList.map((item) => {
            const matchTier = getMatchTier(Number(item.match) || 0);
            const timeLabel =
              activeTab === "received"
                ? `Received ${item.receivedAt}`
                : activeTab === "sent"
                  ? `Sent ${item.sentAt}`
                  : `Matched ${item.matchedAt}`;

            return (
              <article key={item.id} className="panel listing-stage interest-card" style={{ padding: "0.8rem" }}>
                <div className="interest-main-row">
                  <div className="interest-avatar-wrap">
                    <Image
                      src={item.photo}
                      alt={item.firstName}
                      width={74}
                      height={74}
                      sizes="74px"
                      className="interest-avatar"
                    />
                    <span className={`match-badge match-badge-${matchTier}`}>{item.match}%</span>
                  </div>

                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.45rem", flexWrap: "wrap" }}>
                      <h3 style={{ margin: 0, fontSize: "1rem", lineHeight: 1.15 }}>
                        {item.firstName}, {item.age}
                      </h3>
                      {item.isVerified && <span className="chip chip-support">Verified</span>}
                    </div>

                    <p className="profile-meta" style={{ margin: "0.22rem 0 0", color: "var(--ink-muted)", fontSize: "0.86rem" }}>
                      {item.profession} | {item.city}
                    </p>
                    <p style={{ margin: "0.28rem 0 0", color: "var(--ink-muted)", fontSize: "0.76rem" }}>{timeLabel}</p>

                    {activeTab === "sent" && item.status && STATUS_META[item.status] && (
                      <span className={`status-pill ${STATUS_META[item.status].tone}`}>{STATUS_META[item.status].label}</span>
                    )}
                  </div>

                  <div className="interest-actions-wrap">
                    {activeTab === "received" && (
                      <>
                        <button type="button" className="button button-primary" onClick={() => handleAccept(item.userId)}>
                          Accept
                        </button>
                        <button type="button" className="button button-secondary" onClick={() => handleDecline(item.userId)}>
                          Decline
                        </button>
                      </>
                    )}

                    {activeTab === "mutual" && (
                      <Link href={`/chat/${item.userId}`} className="button button-primary">
                        Chat
                      </Link>
                    )}

                    {activeTab === "sent" && item.status === "accepted" && (
                      <Link href={`/chat/${item.userId}`} className="button button-primary">
                        Chat
                      </Link>
                    )}

                    <Link href={`/profile/${item.userId}`} className="button button-secondary">
                      View
                    </Link>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {activeTab === "received" && currentList.length > 0 && (
        <section className="panel" style={{ padding: "0.9rem", borderColor: "rgba(225, 29, 72, 0.24)", background: "linear-gradient(145deg, rgba(234, 250, 246, 0.9), rgba(248, 252, 255, 0.9))" }}>
          <p style={{ margin: 0, color: "#0f5f58", fontSize: "0.86rem", fontWeight: 650 }}>
            Tip: Accepting an interest unlocks faster conversation and helps momentum.
          </p>
        </section>
      )}

    </div>
  );
}

