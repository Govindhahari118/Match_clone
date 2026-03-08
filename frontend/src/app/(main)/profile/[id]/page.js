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
import PageEmptyState from "../../../../components/states/PageEmptyState";
import PageLoadingState from "../../../../components/states/PageLoadingState";

const DEMO_PROFILE = {
  userId: "demo-user",
  firstName: "Priya",
  lastName: "Sharma",
  dateOfBirth: "1997-03-22",
  city: "Mumbai",
  state: "Maharashtra",
  country: "India",
  heightCm: 163,
  profession: "UX Designer",
  educationLevel: "B.Des",
  company: "Razorpay",
  incomeBand: "10-15L",
  religion: "Hindu",
  caste: "Agarwal",
  motherTongue: "Hindi",
  maritalStatus: "Never Married",
  foodHabit: "Non Vegetarian",
  bio: "Creative, family-oriented, and growth-focused. I enjoy thoughtful conversations, travel, and building a meaningful life with shared values.",
  hobbies: ["Design", "Travel", "Fitness", "Reading"],
  photos: [{ id: "demo-photo", photoUrl: "https://randomuser.me/api/portraits/women/44.jpg", isPrimary: true }],
  user: { isVerified: true },
  compatibilityScore: 31,
};

function calculateAge(dateOfBirth) {
  if (!dateOfBirth) return null;
  const date = new Date(dateOfBirth);
  if (Number.isNaN(date.getTime())) return null;
  return new Date().getFullYear() - date.getFullYear();
}

export default function UserProfilePage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();

  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState(null);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [shortlisted, setShortlisted] = useState(false);
  const [sendingInterest, setSendingInterest] = useState(false);

  const profileId = params?.id;

  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true);

      try {
        if (!profileId) {
          setProfile(DEMO_PROFILE);
          return;
        }

        if (!user) {
          setProfile({ ...DEMO_PROFILE, userId: profileId });
          return;
        }

        const response = await api.get(`/profiles/${profileId}`);
        setProfile(response.data || { ...DEMO_PROFILE, userId: profileId });
      } catch {
        setProfile({ ...DEMO_PROFILE, userId: profileId });
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
      if (response.data?.matchId) toast.success("It is a match. Start your conversation.");
      else toast.success("Interest sent successfully.");
    } catch {
      toast.info("Interest action is running in demo mode.");
    } finally {
      setSendingInterest(false);
    }
  };

  const toggleShortlist = async () => {
    if (!guardUserAction()) return;

    const nextValue = !shortlisted;
    setShortlisted(nextValue);

    try {
      if (nextValue) {
        await api.post("/shortlist/add", { shortlistedUserId: profileId });
        toast.success("Added to shortlist.");
      } else {
        await api.post("/shortlist/remove", { shortlistedUserId: profileId });
        toast.info("Removed from shortlist.");
      }
    } catch {
      toast.info("Shortlist updated locally.");
    }
  };

  const reportProfile = async () => {
    if (!guardUserAction()) return;

    try {
      await api.post("/interactions/report", {
        reportedUserId: profileId,
        reportType: "inappropriate",
        description: "Reported from profile view",
      });
      toast.success("Report submitted. Thank you.");
    } catch {
      toast.info("Report action is available in demo mode.");
    }
  };

  if (loading) {
    return (
      <PageLoadingState
        title="Loading profile..."
        description="Fetching profile details and compatibility context."
      />
    );
  }

  if (!profile) {
    return (
      <PageEmptyState
        title="Profile unavailable"
        description="This profile may be private, removed, or currently inaccessible."
        primaryActionLabel="Back to Matches"
        primaryActionHref="/matches"
      />
    );
  }

  return (
    <div style={{ display: "grid", gap: "0.9rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "0.55rem" }}>
        <button type="button" className="button button-secondary" onClick={() => router.back()}>
          Back
        </button>
        <div style={{ display: "flex", gap: "0.42rem", flexWrap: "wrap" }}>
          <button type="button" className="button button-secondary" onClick={toggleShortlist}>
            {shortlisted ? "Saved" : "Save Profile"}
          </button>
          <button type="button" className="button button-primary" onClick={sendInterest} disabled={sendingInterest}>
            {sendingInterest ? "Sending..." : "Send Interest"}
          </button>
        </div>
      </div>

      <section className="panel" style={{ overflow: "hidden" }}>
        <div style={{ height: 190, background: "linear-gradient(140deg, #17213b 0%, #1f3f63 45%, #0d8ea0 100%)", position: "relative" }}>
          <div
            style={{ position: "absolute", inset: 0, background: "radial-gradient(circle at 80% 20%, rgba(240,107,78,0.4), transparent 45%)" }}
          />
        </div>

        <div style={{ padding: "0.95rem", marginTop: -54 }}>
          <div style={{ display: "grid", gridTemplateColumns: "88px minmax(0,1fr)", gap: "0.75rem", alignItems: "end" }}>
            <div
              style={{
                width: 88,
                height: 88,
                borderRadius: "50%",
                border: "4px solid #fff",
                overflow: "hidden",
                background: "#f2efe9",
                boxShadow: "var(--shadow-md)",
              }}
            >
              {primaryPhoto ? (
                <Image
                  src={primaryPhoto}
                  alt="Profile photo"
                  width={240}
                  height={240}
                  sizes="88px"
                  style={{ width: "100%", height: "100%", objectFit: "cover" }}
                />
              ) : (
                <div style={{ width: "100%", height: "100%", display: "grid", placeItems: "center", fontWeight: 800 }}>
                  {(profile.firstName || "U").charAt(0)}
                </div>
              )}
            </div>

            <div>
              <h1 style={{ margin: 0, fontFamily: "var(--font-display)", fontSize: "1.72rem" }}>
                {profile.firstName} {profile.lastName}
              </h1>
              <p style={{ margin: "0.24rem 0 0", color: "var(--ink-muted)", fontSize: "0.9rem" }}>
                {age ? `${age} yrs | ` : ""}
                {profile.profession || "Professional"}
                {profile.city ? ` | ${profile.city}` : ""}
              </p>
              <div style={{ marginTop: "0.38rem", display: "flex", gap: "0.4rem", flexWrap: "wrap" }}>
                {profile.user?.isVerified && <span className="chip chip-support">Verified Profile</span>}
                {profile.compatibilityScore && <span className="chip chip-brand">Compatibility {profile.compatibilityScore}/36</span>}
              </div>
            </div>
          </div>
        </div>
      </section>

      <div style={{ display: "grid", gridTemplateColumns: "1.08fr 0.92fr", gap: "0.9rem" }} className="profile-view-grid">
        <section className="panel" style={{ padding: "0.95rem" }}>
          <p className="section-label" style={{ marginBottom: "0.3rem" }}>
            About
          </p>
          <h2 style={{ margin: "0 0 0.7rem", fontFamily: "var(--font-display)", fontSize: "1.45rem" }}>Introduction</h2>
          <p style={{ margin: 0, color: "var(--ink)", lineHeight: 1.68 }}>{profile.bio || "No bio added yet."}</p>

          <div style={{ marginTop: "0.9rem" }}>
            <p className="section-label" style={{ marginBottom: "0.35rem" }}>
              Interests
            </p>
            <div style={{ display: "flex", gap: "0.42rem", flexWrap: "wrap" }}>
              {(profile.hobbies || []).length > 0 ? (
                profile.hobbies.map((item) => (
                  <span key={item} className="chip chip-support">
                    {item}
                  </span>
                ))
              ) : (
                <span style={{ color: "var(--ink-muted)", fontSize: "0.85rem" }}>No interests listed.</span>
              )}
            </div>
          </div>

          <div style={{ marginTop: "1rem" }}>
            <button type="button" className="button button-secondary" onClick={reportProfile}>
              Report Profile
            </button>
          </div>
        </section>

        <section className="panel" style={{ padding: "0.95rem", display: "grid", gap: "0.55rem" }}>
          <p className="section-label" style={{ marginBottom: "0.3rem" }}>
            Details
          </p>
          <h2 style={{ margin: "0 0 0.5rem", fontFamily: "var(--font-display)", fontSize: "1.45rem" }}>Profile Snapshot</h2>

          {[
            ["Religion", profile.religion],
            ["Caste", profile.caste],
            ["Mother tongue", profile.motherTongue],
            ["Marital status", profile.maritalStatus],
            ["Education", profile.educationLevel],
            ["Profession", profile.profession],
            ["Company", profile.company],
            ["Income", profile.incomeBand],
            ["Height", profile.heightCm ? `${profile.heightCm} cm` : null],
            ["Location", [profile.city, profile.state, profile.country].filter(Boolean).join(", ")],
          ].map(([label, value]) => (
            <div key={label} className="panel" style={{ padding: "0.52rem 0.62rem", borderRadius: 10 }}>
              <p style={{ margin: 0, fontSize: "0.72rem", color: "var(--ink-muted)", textTransform: "uppercase", letterSpacing: "0.03em" }}>{label}</p>
              <p style={{ margin: "0.2rem 0 0", fontSize: "0.87rem", fontWeight: 700 }}>{value || "-"}</p>
            </div>
          ))}

          <div style={{ marginTop: "0.2rem", display: "flex", gap: "0.42rem", flexWrap: "wrap" }}>
            <Link href="/chat" className="button button-primary">
              Open Chat
            </Link>
            <Link href="/pricing" className="button button-secondary">
              Unlock Contact
            </Link>
          </div>
        </section>
      </div>

      <section className="panel" style={{ padding: "0.95rem" }}>
        <ReviewsSection userId={profileId} userName={profile.firstName} />
      </section>

      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />

    </div>
  );
}
