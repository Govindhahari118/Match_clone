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
    if (!window.confirm("Report this profile for inappropriate content?")) return;

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
    <div className="profile-detail-shell">
      <div className="profile-detail-actions">
        <button type="button" className="button button-secondary" onClick={() => router.back()}>
          Back
        </button>
        <div className="profile-detail-action-group">
          <button type="button" className="button button-secondary" onClick={toggleShortlist} aria-pressed={shortlisted}>
            {shortlisted ? "Saved" : "Save Profile"}
          </button>
          <button type="button" className="button button-primary" onClick={sendInterest} disabled={sendingInterest}>
            {sendingInterest ? "Sending..." : "Send Interest"}
          </button>
        </div>
      </div>

      <section className="panel profile-hero profile-hero-detail">
        <div className="profile-hero-banner profile-hero-banner-lg" />

        <div className="profile-hero-body">
          <div className="profile-hero-head profile-hero-head-simple">
            <div className="profile-avatar profile-avatar-lg">
              {primaryPhoto ? (
                <Image
                  src={primaryPhoto}
                  alt="Profile photo"
                  width={240}
                  height={240}
                  sizes="88px"
                  className="profile-avatar-img"
                />
              ) : (
                <div className="profile-avatar-letter">
                  {(profile.firstName || "U").charAt(0)}
                </div>
              )}
            </div>

            <div className="profile-hero-meta">
              <h1 className="profile-hero-name">
                {profile.firstName} {profile.lastName}
              </h1>
              <p className="profile-hero-summary">
                {age ? `${age} yrs | ` : ""}
                {profile.profession || "Professional"}
                {profile.city ? ` | ${profile.city}` : ""}
              </p>
              <div className="profile-hero-chips">
                {profile.user?.isVerified && <span className="chip chip-support">Verified Profile</span>}
                {profile.compatibilityScore && (
                  <span className="chip chip-brand">Compatibility {profile.compatibilityScore}/36</span>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className="profile-view-grid">
        <section className="panel profile-block">
          <p className="section-label">About</p>
          <h2 className="profile-section-title">Introduction</h2>
          <p className="profile-bio-text">{profile.bio || "No bio added yet."}</p>

          <div className="stack-sm profile-section-stack">
            <p className="section-label">Interests</p>
            <div className="profile-interests">
              {(profile.hobbies || []).length > 0 ? (
                profile.hobbies.map((item) => (
                  <span key={item} className="chip chip-support">
                    {item}
                  </span>
                ))
              ) : (
                <span className="profile-interests-empty">No interests listed.</span>
              )}
            </div>
          </div>

          <div className="profile-actions-row">
            <button type="button" className="button button-secondary" onClick={reportProfile}>
              Report Profile
            </button>
          </div>
        </section>

        <section className="panel profile-block profile-detail-panel">
          <p className="section-label">Details</p>
          <h2 className="profile-section-title">Profile Snapshot</h2>

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
            ["District", profile.district],
            ["Residential status", profile.residentialStatus?.replaceAll("_", " ")],
            ["Children", profile.hasChildren?.replaceAll("_", " ")],
            ["Location", [profile.city, profile.state, profile.country].filter(Boolean).join(", ")],
          ].map(([label, value]) => (
            <div key={label} className="profile-detail-tile">
              <p className="profile-detail-label">{label}</p>
              <p className="profile-detail-value">{value || "-"}</p>
            </div>
          ))}

          <div className="profile-actions-row">
            <Link href="/chat" className="button button-primary">
              Open Chat
            </Link>
            <Link href="/pricing" className="button button-secondary">
              Unlock Contact
            </Link>
          </div>
        </section>
      </div>

      <section className="panel profile-block">
        <ReviewsSection userId={profileId} userName={profile.firstName} />
      </section>

      <div className="profile-sticky-cta" role="region" aria-label="Profile actions">
        <div className="profile-sticky-actions">
          <button type="button" className="button button-secondary" onClick={toggleShortlist} aria-pressed={shortlisted}>
            {shortlisted ? "Saved" : "Save"}
          </button>
          <button type="button" className="button button-primary" onClick={sendInterest} disabled={sendingInterest}>
            {sendingInterest ? "Sending..." : "Send Interest"}
          </button>
        </div>
      </div>

      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />

    </div>
  );
}
