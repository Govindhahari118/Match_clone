"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "react-toastify";
import api from "../../../../services/api";
import { useAuth } from "../../../../context/AuthContext";
import ReviewsSection from "../../../../components/ReviewsSection";
import SafeProfileImage from "../../../../components/SafeProfileImage";

function calculateAge(value) {
  if (!value) return null;
  const dob = new Date(value);
  if (Number.isNaN(dob.getTime())) return null;
  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const delta = now.getMonth() - dob.getMonth();
  if (delta < 0 || (delta === 0 && now.getDate() < dob.getDate())) age -= 1;
  return age;
}

export default function UserProfilePage() {
  const { id: profileId } = useParams();
  const router = useRouter();
  const { user } = useAuth();

  const [profile, setProfile] = useState(null);
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");
  const [shortlisted, setShortlisted] = useState(false);
  const [busy, setBusy] = useState("");

  const loadProfile = useCallback(async () => {
    if (!user) {
      setProfile(null);
      setState("signed_out");
      return;
    }
    if (!profileId) {
      setProfile(null);
      setState("error");
      setError("Profile ID is missing.");
      return;
    }

    setState("loading");
    setError("");
    try {
      const [profileResponse, shortlistResponse] = await Promise.all([
        api.get(`/profiles/${profileId}`),
        api.get("/shortlist").catch(() => ({ data: [] })),
      ]);
      setProfile(profileResponse.data || null);
      setShortlisted(Array.isArray(shortlistResponse.data) && shortlistResponse.data.some((item) => item.userId === profileId));
      setState("content");
    } catch (err) {
      setProfile(null);
      if (err.response?.status === 404 || err.response?.status === 403) {
        setError("This profile is unavailable or you no longer have permission to view it.");
      } else {
        setError(err.response?.data?.error || "Couldn’t load this profile.");
      }
      setState("error");
    }
  }, [profileId, user]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const age = useMemo(() => calculateAge(profile?.dateOfBirth), [profile]);
  const primaryPhoto = profile?.photos?.find?.((photo) => photo.isPrimary)?.photoUrl || profile?.photos?.[0]?.photoUrl || null;

  const sendInterest = async () => {
    setBusy("interest");
    try {
      const response = await api.post("/interactions/like", { receiverId: profileId });
      toast.success(response.data?.status === "accepted" ? "You are now connected." : "Interest sent.");
    } catch (err) {
      toast.error(err.response?.data?.error || "Couldn’t send interest.");
    } finally {
      setBusy("");
    }
  };

  const toggleShortlist = async () => {
    setBusy("shortlist");
    try {
      if (shortlisted) {
        await api.post("/shortlist/remove", { shortlistedUserId: profileId });
        setShortlisted(false);
        toast.info("Removed from shortlist.");
      } else {
        await api.post("/shortlist/add", { shortlistedUserId: profileId });
        setShortlisted(true);
        toast.success("Added to shortlist.");
      }
    } catch (err) {
      toast.error(err.response?.data?.error || "Couldn’t update shortlist.");
    } finally {
      setBusy("");
    }
  };

  const reportProfile = async () => {
    setBusy("report");
    try {
      await api.post("/interactions/report", {
        reportedUserId: profileId,
        reportType: "inappropriate",
        description: "Reported from profile detail",
      });
      toast.success("Report submitted for review.");
    } catch (err) {
      toast.error(err.response?.data?.error || "Couldn’t submit report.");
    } finally {
      setBusy("");
    }
  };

  const blockProfile = async () => {
    const confirmed = window.confirm("Block this member? They will no longer be able to interact with you.");
    if (!confirmed) return;
    setBusy("block");
    try {
      await api.post("/interactions/block", { userId: profileId });
      toast.success("Member blocked.");
      router.replace("/matches");
    } catch (err) {
      toast.error(err.response?.data?.error || "Couldn’t block this member.");
    } finally {
      setBusy("");
    }
  };

  if (state === "signed_out") {
    return (
      <section className="panel" style={{ padding: "2rem", textAlign: "center" }}>
        <h1 style={{ marginTop: 0 }}>Profile</h1>
        <p style={{ color: "var(--ink-muted)" }}>Sign in to view eligible member profiles. Sample members are not substituted.</p>
        <Link href="/login" className="button button-primary">Sign In</Link>
      </section>
    );
  }

  if (state === "loading") return <section className="panel" style={{ padding: "2rem" }}>Loading profile…</section>;

  if (state === "error" || !profile) {
    return (
      <section className="panel" role="alert" style={{ padding: "2rem", textAlign: "center" }}>
        <h1 style={{ marginTop: 0 }}>Profile unavailable</h1>
        <p style={{ color: "var(--ink-muted)" }}>{error || "This profile cannot be shown."}</p>
        <div style={{ display: "flex", justifyContent: "center", gap: "0.5rem" }}>
          <button type="button" className="button button-secondary" onClick={() => router.back()}>Back</button>
          <button type="button" className="button button-primary" onClick={loadProfile}>Retry</button>
        </div>
      </section>
    );
  }

  const compatibility = profile.compatibility;

  return (
    <div style={{ display: "grid", gap: "0.9rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: "0.55rem", flexWrap: "wrap" }}>
        <button type="button" className="button button-secondary" onClick={() => router.back()}>Back</button>
        <div style={{ display: "flex", gap: "0.42rem", flexWrap: "wrap" }}>
          <button type="button" className="button button-secondary" disabled={Boolean(busy)} onClick={toggleShortlist}>
            {shortlisted ? "Remove Saved" : "Shortlist"}
          </button>
          <button type="button" className="button button-primary" disabled={Boolean(busy)} onClick={sendInterest}>
            {busy === "interest" ? "Sending…" : "Send Interest"}
          </button>
        </div>
      </div>

      <section className="panel" style={{ padding: "1rem" }}>
        <div style={{ display: "grid", gridTemplateColumns: "110px minmax(0,1fr)", gap: "0.9rem", alignItems: "center" }}>
          <SafeProfileImage
            src={primaryPhoto}
            alt={profile.firstName ? `${profile.firstName} profile` : "Profile photo"}
            width={220}
            height={260}
            sizes="110px"
            style={{ width: 110, height: 130, borderRadius: 18, objectFit: "cover" }}
          />
          <div>
            <h1 style={{ margin: 0, fontSize: "1.75rem" }}>{profile.firstName || "Member"} {profile.lastName || ""}</h1>
            <p style={{ margin: "0.3rem 0 0", color: "var(--ink-muted)" }}>
              {[age ? `${age} yrs` : null, profile.profession, profile.city].filter(Boolean).join(" · ")}
            </p>
            <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginTop: "0.55rem" }}>
              {profile.user?.isVerified && <span className="chip chip-support">Account verified</span>}
              {profile.activity && <span className="chip chip-brand">{String(profile.activity).replaceAll("_", " ")}</span>}
            </div>
          </div>
        </div>
      </section>

      {compatibility && (
        <section className="panel" style={{ padding: "1rem" }}>
          <p className="section-label">Why this profile may align</p>
          <h2 style={{ marginTop: "0.35rem" }}>Explainable match signals</h2>
          {compatibility.reasons?.length ? (
            <ul style={{ marginBottom: 0 }}>
              {compatibility.reasons.map((reason) => <li key={reason}>{reason}</li>)}
            </ul>
          ) : (
            <p style={{ color: "var(--ink-muted)" }}>There isn’t enough preference data to explain compatibility yet.</p>
          )}
          <p style={{ color: "var(--ink-muted)", fontSize: "0.82rem" }}>
            No fabricated compatibility percentage is displayed.
          </p>
        </section>
      )}

      <div className="profile-view-grid" style={{ display: "grid", gridTemplateColumns: "1.08fr 0.92fr", gap: "0.9rem" }}>
        <section className="panel" style={{ padding: "1rem" }}>
          <p className="section-label">About</p>
          <h2 style={{ marginTop: "0.35rem" }}>Introduction</h2>
          <p style={{ color: "var(--ink-muted)", lineHeight: 1.65 }}>{profile.bio || "No introduction provided."}</p>
          <div style={{ display: "flex", gap: "0.45rem", flexWrap: "wrap", marginTop: "1rem" }}>
            <button type="button" className="button button-secondary" disabled={Boolean(busy)} onClick={reportProfile}>Report</button>
            <button type="button" className="button button-secondary" disabled={Boolean(busy)} onClick={blockProfile}>Block</button>
          </div>
        </section>

        <section className="panel" style={{ padding: "1rem" }}>
          <p className="section-label">Profile Snapshot</p>
          <div style={{ display: "grid", gap: "0.5rem" }}>
            {[
              ["Religion", profile.religion],
              ["Community", profile.caste],
              ["Mother tongue", profile.motherTongue],
              ["Marital status", profile.maritalStatus],
              ["Education", profile.educationLevel],
              ["Profession", profile.profession],
              ["Income", profile.incomeBand],
              ["Height", profile.heightCm ? `${profile.heightCm} cm` : null],
              ["Location", [profile.city, profile.state, profile.country].filter(Boolean).join(", ")],
            ].map(([label, value]) => (
              <div key={label} className="field-tile">
                <small style={{ color: "var(--ink-muted)" }}>{label}</small>
                <div style={{ fontWeight: 700, marginTop: 3 }}>{value ? String(value).replaceAll("_", " ") : "Not provided"}</div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <ReviewsSection userId={profileId} userName={profile.firstName} />
    </div>
  );
}
