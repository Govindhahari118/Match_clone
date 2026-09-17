"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { toast } from "react-toastify";
import api from "../../../../services/api";
import { useAuth } from "../../../../context/AuthContext";
import LoginPromptModal from "../../../../components/LoginPromptModal";
import ReviewsSection from "../../../../components/ReviewsSection";
import TrustStatusPanel from "../../../../components/TrustStatusPanel";

const PREVIEW_PROFILE = {
  userId: "preview-user",
  firstName: "Preview",
  lastName: "Profile",
  dateOfBirth: "1997-03-22",
  city: "Hyderabad",
  state: "Telangana",
  country: "India",
  heightCm: 163,
  profession: "Professional",
  educationLevel: "Post Graduate",
  religion: "Not specified",
  maritalStatus: "Never Married",
  bio: "Guest preview only. Login to view live matrimony profiles and trust information.",
  photos: [{ id: "preview-photo", photoUrl: "https://randomuser.me/api/portraits/women/44.jpg", isPrimary: true }],
  whyRecommended: ["Guest preview only"],
  trust: {
    verification: { phone: "not_started", photo: "not_started", identity: "unverified" },
    activity: { label: "Preview only" },
    profileCompleteness: 0,
    managedBy: "self",
    searchStatus: "preview",
    statusReconfirmationLabel: "Preview only",
  },
};

function calculateAge(dateOfBirth) {
  if (!dateOfBirth) return null;
  const date = new Date(dateOfBirth);
  if (Number.isNaN(date.getTime())) return null;
  const now = new Date();
  let age = now.getFullYear() - date.getFullYear();
  const monthDelta = now.getMonth() - date.getMonth();
  if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < date.getDate())) age -= 1;
  return age;
}

function matchLabel(strength) {
  if (strength === "strong") return "Strong preference alignment";
  if (strength === "good") return "Good preference alignment";
  if (strength === "partial") return "Partial preference alignment";
  return "Match details available after preferences are completed";
}

export default function UserProfilePage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState(null);
  const [loadError, setLoadError] = useState("");
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [shortlisted, setShortlisted] = useState(false);
  const [sendingInterest, setSendingInterest] = useState(false);
  const profileId = params?.id;

  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true);
      setLoadError("");
      try {
        if (!profileId) throw new Error("Profile id missing");
        if (!user) {
          setProfile({ ...PREVIEW_PROFILE, userId: profileId });
          return;
        }
        const response = await api.get(`/profiles/${profileId}`);
        setProfile(response.data || null);
      } catch (error) {
        setProfile(null);
        setLoadError(error?.response?.data?.error || "This live profile could not be loaded.");
      } finally {
        setLoading(false);
      }
    };
    loadProfile();
  }, [profileId, user]);

  const age = useMemo(() => calculateAge(profile?.dateOfBirth), [profile]);
  const primaryPhoto = profile?.photos?.[0]?.photoUrl || profile?.photos?.[0]?.thumbnailUrl;

  const guardUserAction = () => {
    if (!user) {
      setShowLoginModal(true);
      return false;
    }
    return true;
  };

  const sendInterest = async () => {
    if (!guardUserAction()) return;
    setSendingInterest(true);
    try {
      const response = await api.post("/interactions/like", { receiverId: profileId });
      toast.success(response.data?.matchId ? "It is a mutual connection. You can now chat." : "Interest sent successfully.");
    } catch (error) {
      toast.error(error?.response?.data?.error || "Interest could not be sent.");
    } finally {
      setSendingInterest(false);
    }
  };

  const toggleShortlist = async () => {
    if (!guardUserAction()) return;
    try {
      if (!shortlisted) {
        await api.post("/shortlist/add", { shortlistedUserId: profileId });
        setShortlisted(true);
        toast.success("Added to shortlist.");
      } else {
        await api.post("/shortlist/remove", { shortlistedUserId: profileId });
        setShortlisted(false);
        toast.info("Removed from shortlist.");
      }
    } catch (error) {
      toast.error(error?.response?.data?.error || "Shortlist could not be updated.");
    }
  };

  const requestContact = async () => {
    if (!guardUserAction()) return;
    try {
      await api.post(`/interactions/contact/${profileId}`);
      toast.success("Contact request sent. Details are shared only after approval.");
    } catch (error) {
      toast.error(error?.response?.data?.error || "Contact request is unavailable.");
    }
  };

  const requestPhotoAccess = async () => {
    if (!guardUserAction()) return;
    try {
      await api.post(`/interactions/photo-access/${profileId}`);
      toast.success("Photo access request sent.");
    } catch (error) {
      toast.error(error?.response?.data?.error || "Photo access request could not be sent.");
    }
  };

  const hideProfile = async () => {
    if (!guardUserAction()) return;
    try {
      await api.post(`/trust/users/${profileId}/hide`);
      toast.info("Profile hidden. It will not return to your recommendations unless restored.");
      router.back();
    } catch (error) {
      toast.error(error?.response?.data?.error || "Profile could not be hidden.");
    }
  };

  const blockProfile = async () => {
    if (!guardUserAction()) return;
    try {
      await api.post(`/interactions/block/${profileId}`, { reason: "Blocked from profile view" });
      toast.success("Profile blocked across discovery, messaging and contact sharing.");
      router.back();
    } catch (error) {
      toast.error(error?.response?.data?.error || "Profile could not be blocked.");
    }
  };

  const reportProfile = async () => {
    if (!guardUserAction()) return;
    try {
      await api.post("/interactions/report", {
        reportedUserId: profileId,
        reportType: "wrong_information",
        description: "Reported from profile view; user can provide further details to support.",
      });
      toast.success("Report submitted for review.");
    } catch (error) {
      toast.error(error?.response?.data?.error || "Report could not be submitted.");
    }
  };

  if (loading) return <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>Loading profile…</div>;
  if (!profile) {
    return (
      <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>
        <h2 style={{ marginTop: 0 }}>Profile unavailable</h2>
        <p style={{ color: "var(--ink-muted)" }}>{loadError || "The profile may be paused, stale, blocked, deleted, or temporarily unavailable."}</p>
        <button type="button" className="button button-secondary" onClick={() => router.back()}>Go back</button>
      </div>
    );
  }

  return (
    <div style={{ display: "grid", gap: "0.9rem" }}>
      {!user && <div className="chip chip-brand">Guest preview — this is not live member data</div>}

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "0.55rem" }}>
        <button type="button" className="button button-secondary" onClick={() => router.back()}>Back</button>
        <div style={{ display: "flex", gap: "0.42rem", flexWrap: "wrap" }}>
          <button type="button" className="button button-secondary" onClick={toggleShortlist}>{shortlisted ? "Saved" : "Save Profile"}</button>
          <button type="button" className="button button-primary" onClick={sendInterest} disabled={sendingInterest}>{sendingInterest ? "Sending…" : "Send Interest"}</button>
        </div>
      </div>

      <section className="panel" style={{ overflow: "hidden" }}>
        <div style={{ height: 170, background: "linear-gradient(140deg, #17213b 0%, #1f3f63 45%, #0d8ea0 100%)", position: "relative" }} />
        <div style={{ padding: "0.95rem", marginTop: -54 }}>
          <div style={{ display: "grid", gridTemplateColumns: "88px minmax(0,1fr)", gap: "0.75rem", alignItems: "end" }}>
            <div style={{ width: 88, height: 88, borderRadius: "50%", border: "4px solid #fff", overflow: "hidden", background: "#f2efe9", boxShadow: "var(--shadow-md)" }}>
              {primaryPhoto ? <Image src={primaryPhoto} alt="Profile photo" width={240} height={240} sizes="88px" style={{ width: "100%", height: "100%", objectFit: "cover" }} /> : <div style={{ width: "100%", height: "100%", display: "grid", placeItems: "center", fontWeight: 800 }}>{(profile.firstName || "U").charAt(0)}</div>}
            </div>
            <div>
              <h1 style={{ margin: 0, fontFamily: "var(--font-display)", fontSize: "1.72rem" }}>{profile.firstName} {profile.lastName}</h1>
              <p style={{ margin: "0.24rem 0 0", color: "var(--ink-muted)", fontSize: "0.9rem" }}>{age ? `${age} yrs · ` : ""}{profile.profession || "Professional"}{profile.city ? ` · ${profile.city}` : ""}</p>
              <div style={{ marginTop: "0.38rem", display: "flex", gap: "0.4rem", flexWrap: "wrap" }}>
                <span className="chip chip-brand">{matchLabel(profile.compatibilityStrength)}</span>
                {profile.trust?.activity?.label && <span className="chip chip-support">{profile.trust.activity.label}</span>}
                {profile.trust?.managedBy && <span className="chip chip-support">Managed by {profile.trust.managedBy}</span>}
              </div>
            </div>
          </div>
        </div>
      </section>

      <TrustStatusPanel trust={profile.trust} />

      {profile.whyRecommended?.length > 0 && (
        <section className="panel" style={{ padding: "0.95rem" }}>
          <p className="section-label" style={{ marginBottom: "0.3rem" }}>Why this profile appears</p>
          <div style={{ display: "flex", gap: "0.42rem", flexWrap: "wrap" }}>{profile.whyRecommended.map((reason) => <span key={reason} className="chip chip-support">{reason}</span>)}</div>
        </section>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "1.08fr 0.92fr", gap: "0.9rem" }} className="profile-view-grid">
        <section className="panel" style={{ padding: "0.95rem" }}>
          <p className="section-label" style={{ marginBottom: "0.3rem" }}>About</p>
          <h2 style={{ margin: "0 0 0.7rem", fontFamily: "var(--font-display)", fontSize: "1.45rem" }}>Introduction</h2>
          <p style={{ margin: 0, color: "var(--ink)", lineHeight: 1.68 }}>{profile.bio || "No bio added yet."}</p>
          <div style={{ marginTop: "1rem", display: "flex", gap: "0.42rem", flexWrap: "wrap" }}>
            <button type="button" className="button button-secondary" onClick={hideProfile}>Don’t show again</button>
            <button type="button" className="button button-secondary" onClick={reportProfile}>Report</button>
            <button type="button" className="button button-secondary" onClick={blockProfile}>Block</button>
          </div>
        </section>

        <section className="panel" style={{ padding: "0.95rem", display: "grid", gap: "0.55rem" }}>
          <p className="section-label" style={{ marginBottom: "0.3rem" }}>Details</p>
          <h2 style={{ margin: "0 0 0.5rem", fontFamily: "var(--font-display)", fontSize: "1.45rem" }}>Profile Snapshot</h2>
          {[
            ["Religion", profile.religion], ["Caste", profile.caste], ["Mother tongue", profile.motherTongue],
            ["Marital status", profile.maritalStatus], ["Education", profile.educationLevel], ["Profession", profile.profession],
            ["Company", profile.company], ["Income", profile.incomeBand], ["Height", profile.heightCm ? `${profile.heightCm} cm` : null],
            ["Location", [profile.city, profile.state, profile.country].filter(Boolean).join(", ")],
          ].map(([label, value]) => (
            <div key={label} className="panel" style={{ padding: "0.52rem 0.62rem", borderRadius: 10 }}>
              <p style={{ margin: 0, fontSize: "0.72rem", color: "var(--ink-muted)", textTransform: "uppercase", letterSpacing: "0.03em" }}>{label}</p>
              <p style={{ margin: "0.2rem 0 0", fontSize: "0.87rem", fontWeight: 700 }}>{value || "-"}</p>
            </div>
          ))}
          <div style={{ marginTop: "0.2rem", display: "flex", gap: "0.42rem", flexWrap: "wrap" }}>
            <Link href="/chat" className="button button-primary">Open Chat</Link>
            {profile.contact?.phone ? <a className="button button-secondary" href={`tel:${profile.contact.phone}`}>Call</a> : <button type="button" className="button button-secondary" onClick={requestContact}>Request Contact</button>}
            {profile.privacy?.photoVisibility === "request_access" && !profile.privacy?.canViewPhotos && <button type="button" className="button button-secondary" onClick={requestPhotoAccess}>Request Photo Access</button>}
          </div>
          <p style={{ margin: "0.2rem 0 0", color: "var(--ink-muted)", fontSize: "0.76rem" }}>Paid membership never overrides another member’s contact or photo privacy choices.</p>
        </section>
      </div>

      {user && <section className="panel" style={{ padding: "0.95rem" }}><ReviewsSection userId={profileId} userName={profile.firstName} /></section>}
      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />
    </div>
  );
}
