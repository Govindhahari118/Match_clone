"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import SafeProfileImage from "../../../components/SafeProfileImage";

function ageFromDob(value) {
  if (!value) return null;
  const dob = new Date(value);
  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const delta = now.getMonth() - dob.getMonth();
  if (delta < 0 || (delta === 0 && now.getDate() < dob.getDate())) age -= 1;
  return age;
}

function formatSavedAt(value) {
  if (!value) return "Saved";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Saved" : `Saved ${date.toLocaleDateString()}`;
}

function ShortlistCard({ profile, onRemove, onInterest, busy }) {
  const age = profile.age ?? ageFromDob(profile.dateOfBirth);
  return (
    <article className="panel panel-hover listing-stage" style={{ overflow: "hidden" }}>
      <div style={{ height: 230 }}>
        <SafeProfileImage
          src={profile.photo || profile.photos?.find?.((p) => p.isPrimary)?.thumbnailUrl || profile.photos?.find?.((p) => p.isPrimary)?.photoUrl}
          alt={profile.firstName ? `${profile.firstName} profile` : "Profile photo"}
          width={640}
          height={760}
          sizes="(max-width: 760px) 100vw, 33vw"
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
      </div>
      <div style={{ padding: "0.95rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: "0.8rem" }}>
          <div>
            <h3 style={{ margin: 0, fontSize: "1.08rem" }}>
              {profile.firstName || "Member"}{age ? `, ${age}` : ""}
            </h3>
            <p style={{ margin: "0.28rem 0 0", color: "var(--ink-muted)", fontSize: "0.84rem" }}>
              {[profile.profession, profile.city].filter(Boolean).join(" · ") || "Profile details available"}
            </p>
          </div>
          {profile.isVerified && <span className="chip chip-support">Verified</span>}
        </div>
        <p style={{ margin: "0.55rem 0 0", color: "var(--ink-muted)", fontSize: "0.76rem" }}>
          {formatSavedAt(profile.shortlistedAt)}
        </p>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.5rem", marginTop: "0.85rem" }}>
          <button
            type="button"
            className="button button-primary"
            disabled={busy === profile.userId}
            onClick={() => onInterest(profile.userId)}
          >
            Send Interest
          </button>
          <Link href={`/profile/${profile.userId}`} className="button button-secondary">
            View Profile
          </Link>
          <button
            type="button"
            className="button button-secondary"
            disabled={busy === profile.userId}
            onClick={() => onRemove(profile.userId)}
            style={{ gridColumn: "1 / -1" }}
          >
            Remove from Shortlist
          </button>
        </div>
      </div>
    </article>
  );
}

export default function ShortlistsPage() {
  const [list, setList] = useState([]);
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(null);

  const load = useCallback(async () => {
    setState("loading");
    setError("");
    try {
      const response = await api.get("/shortlist");
      setList(Array.isArray(response.data) ? response.data : []);
      setState("content");
    } catch (err) {
      setList([]);
      setError(err.response?.data?.error || "Couldn’t load your shortlist.");
      setState("error");
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const verifiedCount = useMemo(() => list.filter((item) => item.isVerified).length, [list]);

  const handleRemove = async (userId) => {
    setBusy(userId);
    try {
      await api.post("/shortlist/remove", { shortlistedUserId: userId });
      setList((current) => current.filter((item) => item.userId !== userId));
      toast.success("Removed from shortlist.");
    } catch (err) {
      toast.error(err.response?.data?.error || "Couldn’t remove this profile. Try again.");
    } finally {
      setBusy(null);
    }
  };

  const handleInterest = async (userId) => {
    setBusy(userId);
    try {
      const response = await api.post("/interactions/like", { receiverId: userId });
      toast.success(response.data?.status === "accepted" ? "You are now connected." : "Interest sent.");
    } catch (err) {
      toast.error(err.response?.data?.error || "Couldn’t send interest. Try again.");
    } finally {
      setBusy(null);
    }
  };

  return (
    <div style={{ display: "grid", gap: "0.95rem" }}>
      <section className="listing-hero">
        <div>
          <p className="section-label">Saved Profiles</p>
          <h1 className="section-title" style={{ margin: 0 }}>Shortlists</h1>
          <p className="section-copy">Profiles you explicitly saved appear here. No demo profiles are substituted if the service fails.</p>
          {state === "content" && (
            <div className="result-metrics">
              <span className="metric-chip metric-chip-highlight">{list.length} saved</span>
              <span className="metric-chip">{verifiedCount} verified</span>
            </div>
          )}
        </div>
        <Link href="/matches" className="button button-primary">Browse Matches</Link>
      </section>

      {state === "loading" && (
        <section className="panel listing-stage" style={{ padding: "1.4rem" }}>
          Loading your shortlist…
        </section>
      )}

      {state === "error" && (
        <section className="panel listing-stage" role="alert" style={{ padding: "1.4rem" }}>
          <h2 style={{ marginTop: 0 }}>Couldn’t load shortlists</h2>
          <p style={{ color: "var(--ink-muted)" }}>{error}</p>
          <button type="button" className="button button-primary" onClick={load}>Retry</button>
        </section>
      )}

      {state === "content" && list.length === 0 && (
        <section className="panel listing-stage" style={{ textAlign: "center", padding: "2rem" }}>
          <h2 style={{ marginTop: 0 }}>No shortlisted profiles yet</h2>
          <p style={{ color: "var(--ink-muted)" }}>Save a real profile from discovery and it will appear here.</p>
          <Link href="/matches" className="button button-primary">Browse Matches</Link>
        </section>
      )}

      {state === "content" && list.length > 0 && (
        <div className="results-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "0.9rem" }}>
          {list.map((profile) => (
            <ShortlistCard
              key={profile.shortlistId || profile.userId}
              profile={profile}
              busy={busy}
              onRemove={handleRemove}
              onInterest={handleInterest}
            />
          ))}
        </div>
      )}
    </div>
  );
}
