"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import api from "../../../services/api";
import SafeProfileImage from "../../../components/SafeProfileImage";

export default function WhoViewedPage() {
  const [viewers, setViewers] = useState([]);
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setState("loading");
    setError("");
    try {
      const response = await api.get("/interactions/profile-viewers");
      setViewers(Array.isArray(response.data) ? response.data : []);
      setState("content");
    } catch (err) {
      setViewers([]);
      setError(err.response?.data?.error || "Couldn’t load profile viewers.");
      setState("error");
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const verified = useMemo(() => viewers.filter((viewer) => viewer.isVerified).length, [viewers]);

  return (
    <div style={{ display: "grid", gap: "0.95rem" }}>
      <section className="listing-hero">
        <div>
          <p className="section-label">Profile Activity</p>
          <h1 className="section-title" style={{ margin: 0 }}>Who Viewed Your Profile</h1>
          <p className="section-copy">Only real recorded profile-view events are shown.</p>
          {state === "content" && (
            <div className="result-metrics">
              <span className="metric-chip metric-chip-highlight">{viewers.length} visitors</span>
              <span className="metric-chip">{verified} verified</span>
            </div>
          )}
        </div>
      </section>

      {state === "loading" && <section className="panel" style={{ padding: "1.4rem" }}>Loading profile viewers…</section>}

      {state === "error" && (
        <section className="panel" role="alert" style={{ padding: "1.4rem" }}>
          <h2 style={{ marginTop: 0 }}>Couldn’t load viewers</h2>
          <p style={{ color: "var(--ink-muted)" }}>{error}</p>
          <button type="button" className="button button-primary" onClick={load}>Retry</button>
        </section>
      )}

      {state === "content" && viewers.length === 0 && (
        <section className="panel" style={{ padding: "2rem", textAlign: "center" }}>
          <h2 style={{ marginTop: 0 }}>No profile views yet</h2>
          <p style={{ color: "var(--ink-muted)" }}>When an eligible member genuinely views your profile, the activity will appear here.</p>
          <Link href="/profile" className="button button-primary">Review My Profile</Link>
        </section>
      )}

      {state === "content" && viewers.length > 0 && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(270px, 1fr))", gap: "0.9rem" }}>
          {viewers.map((viewer) => (
            <article key={viewer.userId || viewer.id} className="panel listing-stage" style={{ padding: "0.75rem" }}>
              <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
                <SafeProfileImage
                  src={viewer.photo}
                  alt={viewer.firstName ? `${viewer.firstName} profile` : "Profile photo"}
                  width={64}
                  height={64}
                  sizes="64px"
                  style={{ width: 64, height: 64, borderRadius: 14, objectFit: "cover", flexShrink: 0 }}
                />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: "flex", gap: "0.4rem", alignItems: "center", flexWrap: "wrap" }}>
                    <h3 style={{ margin: 0, fontSize: "1rem" }}>{viewer.firstName || "Member"}{viewer.age ? `, ${viewer.age}` : ""}</h3>
                    {viewer.isVerified && <span className="chip chip-support">Verified</span>}
                  </div>
                  <p style={{ margin: "0.25rem 0 0", color: "var(--ink-muted)", fontSize: "0.82rem" }}>
                    {[viewer.profession, viewer.city].filter(Boolean).join(" · ")}
                  </p>
                  {viewer.receivedAt && <p style={{ margin: "0.2rem 0 0", color: "var(--ink-muted)", fontSize: "0.74rem" }}>Viewed {viewer.receivedAt}</p>}
                </div>
                <Link href={`/profile/${viewer.userId}`} className="button button-secondary">View</Link>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
