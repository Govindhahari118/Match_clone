"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../../context/AuthContext";

const MOCK_NOTIFICATIONS = [
  { id: 1, type: "match", title: "New Match", message: "You matched with Priya Sharma.", time: "2m ago", read: false },
  { id: 2, type: "visitor", title: "Profile View", message: "Someone viewed your profile.", time: "1h ago", read: true },
  { id: 3, type: "system", title: "Welcome", message: "Thanks for joining MatrimonyConnect.", time: "1d ago", read: true },
];

const TYPE_META = {
  match: { label: "Match", tone: "type-match" },
  visitor: { label: "Viewer", tone: "type-visitor" },
  system: { label: "System", tone: "type-system" },
};

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
    const timer = setTimeout(() => {
      setNotifications(MOCK_NOTIFICATIONS);
    }, 450);
    return () => clearTimeout(timer);
  }, []);

  const notificationStats = useMemo(() => {
    const total = notifications.length;
    const unread = notifications.filter((item) => !item.read).length;
    const matches = notifications.filter((item) => item.type === "match").length;
    return { total, unread, matches };
  }, [notifications]);

  const markAllRead = () => {
    setNotifications((previous) => previous.map((item) => ({ ...item, read: true })));
  };

  if (loading || !user) return null;

  return (
    <div style={{ display: "grid", gap: "0.95rem", maxWidth: 820 }}>
      <section className="listing-hero">
        <div>
          <p className="section-label" style={{ marginBottom: "0.22rem" }}>
            Activity Feed
          </p>
          <h1 className="section-title" style={{ margin: 0, fontSize: "clamp(1.64rem, 3vw, 2.2rem)" }}>
            Notifications
          </h1>
          <p className="section-copy" style={{ marginTop: "0.38rem", fontSize: "0.92rem" }}>
            Keep track of matches, visitors, and account updates in one stream.
          </p>
          <div className="result-metrics">
            <span className="metric-chip metric-chip-highlight">{notificationStats.unread} unread</span>
            <span className="metric-chip">{notificationStats.matches} match alerts</span>
            <span className="metric-chip">{notificationStats.total} total</span>
          </div>
        </div>

        <div className="hero-actions" style={{ display: "flex", gap: "0.45rem", alignItems: "center" }}>
          <button type="button" className="button button-secondary" onClick={markAllRead} disabled={notificationStats.unread === 0}>
            Mark All Read
          </button>
          <Link href="/matches" className="button button-primary">
            View Matches
          </Link>
        </div>
      </section>

      {notifications.length === 0 ? (
        <section className="panel listing-stage" style={{ textAlign: "center", padding: "2.2rem" }}>
          <h2 style={{ marginTop: 0, marginBottom: "0.38rem" }}>No notifications yet</h2>
          <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.9rem" }}>
            Updates will appear here as soon as you receive new activity.
          </p>
        </section>
      ) : (
        <div style={{ display: "grid", gap: "0.68rem" }}>
          {notifications.map((item) => (
            <article key={item.id} className={`panel listing-stage notification-card ${item.read ? "read" : "unread"}`}>
              <div className="notification-inner">
                <div className="notification-head">
                  <h3 style={{ margin: 0, fontSize: "1rem", lineHeight: 1.18 }}>{item.title}</h3>
                  <span className={`type-pill ${TYPE_META[item.type]?.tone || "type-system"}`}>{TYPE_META[item.type]?.label || "Update"}</span>
                </div>
                <p style={{ margin: "0.34rem 0 0", color: "var(--ink-muted)", fontSize: "0.9rem" }}>{item.message}</p>
                <div className="notification-meta-row">
                  <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.76rem" }}>{item.time}</p>
                  {!item.read && <span className="unread-dot" aria-label="Unread" />}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      <style jsx>{`
        .listing-hero {
          padding: 1rem;
          border-radius: 22px;
          border: 1px solid rgba(29, 78, 216, 0.2);
          background: linear-gradient(142deg, rgba(255, 255, 255, 0.98), rgba(241, 247, 255, 0.93));
          box-shadow: 0 16px 34px rgba(15, 23, 42, 0.09);
        }

        .result-metrics {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 0.42rem;
          margin-top: 0.62rem;
        }

        .metric-chip {
          display: inline-flex;
          align-items: center;
          padding: 0.24rem 0.6rem;
          border-radius: 999px;
          border: 1px solid rgba(148, 163, 184, 0.4);
          background: rgba(255, 255, 255, 0.9);
          color: var(--ink-muted);
          font-size: 0.74rem;
          font-weight: 680;
        }

        .metric-chip-highlight {
          border-color: rgba(227, 68, 117, 0.3);
          background: rgba(227, 68, 117, 0.13);
          color: #b32458;
        }

        .hero-actions {
          margin-left: auto;
          flex-wrap: wrap;
          justify-content: flex-end;
        }

        .notification-card {
          border: 1px solid rgba(29, 78, 216, 0.14);
          transition: all 0.2s ease;
        }

        .notification-card.unread {
          border-color: rgba(227, 68, 117, 0.22);
          background: linear-gradient(145deg, rgba(255, 245, 250, 0.9), rgba(255, 255, 255, 0.95));
        }

        .notification-card.read {
          background: rgba(255, 255, 255, 0.9);
        }

        .notification-card:hover {
          transform: translateY(-2px);
          box-shadow: 0 12px 24px rgba(15, 23, 42, 0.09);
        }

        .notification-inner {
          padding: 0.82rem 0.9rem;
        }

        .notification-head {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 0.6rem;
        }

        .type-pill {
          display: inline-flex;
          align-items: center;
          border-radius: 999px;
          padding: 0.2rem 0.5rem;
          border: 1px solid transparent;
          font-size: 0.72rem;
          font-weight: 700;
          line-height: 1;
        }

        .type-match {
          color: #b32458;
          border-color: rgba(227, 68, 117, 0.3);
          background: rgba(254, 217, 231, 0.92);
        }

        .type-visitor {
          color: #1d4ed8;
          border-color: rgba(29, 78, 216, 0.24);
          background: rgba(224, 239, 255, 0.92);
        }

        .type-system {
          color: #475569;
          border-color: rgba(148, 163, 184, 0.28);
          background: rgba(241, 245, 249, 0.92);
        }

        .notification-meta-row {
          margin-top: 0.46rem;
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .unread-dot {
          width: 9px;
          height: 9px;
          border-radius: 50%;
          background: #e11d48;
          box-shadow: 0 0 0 4px rgba(225, 29, 72, 0.15);
        }
      `}</style>
    </div>
  );
}
