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

    </div>
  );
}
