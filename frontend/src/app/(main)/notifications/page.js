"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import api from "../../../services/api";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";
import PageHero from "../../../components/PageHero";

const MOCK_NOTIFICATIONS = [
  { id: 1, type: "match", title: "New Match", message: "You matched with Priya Sharma.", time: "2m ago", read: false },
  { id: 2, type: "visitor", title: "Profile View", message: "Someone viewed your profile.", time: "1h ago", read: true },
  { id: 3, type: "system", title: "Welcome", message: "Thanks for joining MatrimonyConnect.", time: "1d ago", read: true },
];

const TYPE_META = {
  match: { label: "Match", tone: "type-match" },
  interest: { label: "Interest", tone: "type-interest" },
  message: { label: "Message", tone: "type-message" },
  visitor: { label: "Viewer", tone: "type-visitor" },
  system: { label: "System", tone: "type-system" },
};

export default function NotificationsPage() {
  const { user, loading } = useAuth();
  const router = useRouter();
  const [notifications, setNotifications] = useState([]);
  const [loadingFeed, setLoadingFeed] = useState(true);
  const [isPreview, setIsPreview] = useState(false);

  const dataUpdatedLabel = useMemo(
    () => new Date().toLocaleDateString(undefined, { month: "short", day: "numeric" }),
    []
  );

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    setLoadingFeed(true);

    const loadNotifications = async () => {
      try {
        const response = await api.get("/notifications");
        const items = response?.data?.items;
        if (!cancelled) {
          if (Array.isArray(items)) {
            setNotifications(items);
            setIsPreview(false);
          } else {
            setNotifications(MOCK_NOTIFICATIONS);
            setIsPreview(true);
          }
        }
      } catch {
        if (!cancelled) {
          setNotifications(MOCK_NOTIFICATIONS);
          setIsPreview(true);
        }
      } finally {
        if (!cancelled) {
          setLoadingFeed(false);
        }
      }
    };

    loadNotifications();
    return () => {
      cancelled = true;
    };
  }, [user]);

  const notificationStats = useMemo(() => {
    const total = notifications.length;
    const unread = notifications.filter((item) => !item.read).length;
    const matches = notifications.filter((item) => item.type === "match").length;
    return { total, unread, matches };
  }, [notifications]);

  const markAllRead = async () => {
    setNotifications((previous) => previous.map((item) => ({ ...item, read: true })));
    if (isPreview) return;
    try {
      await api.post("/notifications/read");
    } catch {
      // Best effort. UI already updated.
    }
  };

  if (loading || !user) return null;

  return (
    <div className="notifications-shell">
      <PageHero
        eyebrow="Account Activity"
        title="Notifications"
        copy="Stay on top of matches, profile views, and important account updates."
        className="listing-hero"
        actions={(
          <>
            <button type="button" className="button button-secondary" onClick={markAllRead} disabled={notificationStats.unread === 0}>
              Mark All Read
            </button>
            <Link href="/matches" className="button button-primary">
              View Matches
            </Link>
          </>
        )}
      >
        <div className="result-metrics">
          <span className="metric-chip metric-chip-highlight">{notificationStats.unread} unread</span>
          <span className="metric-chip">{notificationStats.matches} match alerts</span>
          <span className="metric-chip">{notificationStats.total} total</span>
          {isPreview && <span className="metric-chip">Preview mode</span>}
        </div>
        <p className="data-freshness">Updated {dataUpdatedLabel}</p>
      </PageHero>

      {loadingFeed ? (
        <PageLoadingState
          title="Loading notifications..."
          description="Fetching your latest match, visitor, and system updates."
          compact
        />
      ) : notifications.length === 0 ? (
        <PageEmptyState
          title="No notifications yet"
          description="Updates will appear here as soon as you receive new activity."
          primaryActionLabel="View Matches"
          primaryActionHref="/matches"
        />
      ) : (
        <div className="notification-list">
          {notifications.map((item) => (
            <article key={item.id} className={`panel listing-stage notification-card ${item.read ? "read" : "unread"}`}>
              <div className="notification-inner">
                <div className="notification-head">
                  <h3 className="notification-title">{item.title}</h3>
                  <span className={`type-pill ${TYPE_META[item.type]?.tone || "type-system"}`}>{TYPE_META[item.type]?.label || "Update"}</span>
                </div>
                <p className="notification-message">{item.message}</p>
                <div className="notification-meta-row">
                  <p className="notification-time">{item.time}</p>
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
