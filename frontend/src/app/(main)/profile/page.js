"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";

const DEMO_PROFILE = {
  firstName: "Laksh",
  lastName: "Singh",
  dateOfBirth: "1998-04-15",
  gender: "male",
  maritalStatus: "never_married",
  city: "Mumbai",
  state: "Maharashtra",
  country: "India",
  religion: "Hindu",
  caste: "Rajput",
  motherTongue: "Hindi",
  profession: "Software Engineer",
  educationLevel: "bachelors",
  incomeBand: "10-25L",
  heightCm: 178,
  bio: "Engineer by profession, family-oriented by values. I enjoy travel, fitness, and meaningful conversations.",
  hobbies: ["Travel", "Fitness", "Music", "Reading"],
  photos: [{ id: "ph-1", photoUrl: "https://randomuser.me/api/portraits/men/32.jpg", isPrimary: true }],
};

const EDITABLE_FIELDS = [
  { key: "firstName", label: "First name", type: "text" },
  { key: "lastName", label: "Last name", type: "text" },
  { key: "dateOfBirth", label: "Date of birth", type: "date" },
  { key: "gender", label: "Gender", type: "text" },
  { key: "maritalStatus", label: "Marital status", type: "text" },
  { key: "city", label: "City", type: "text" },
  { key: "state", label: "State", type: "text" },
  { key: "country", label: "Country", type: "text" },
  { key: "religion", label: "Religion", type: "text" },
  { key: "caste", label: "Caste", type: "text" },
  { key: "motherTongue", label: "Mother tongue", type: "text" },
  { key: "profession", label: "Profession", type: "text" },
  { key: "educationLevel", label: "Education", type: "text" },
  { key: "incomeBand", label: "Income", type: "text" },
  { key: "heightCm", label: "Height (cm)", type: "number" },
];

function normalizeProfile(payload) {
  if (!payload) return null;

  return {
    ...payload,
    photos: payload.photos || [],
    hobbies: payload.hobbies || [],
  };
}

export default function ProfilePage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState(null);
  const [formData, setFormData] = useState({});
  const [previewMode, setPreviewMode] = useState(false);

  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true);
      setPreviewMode(false);

      try {
        if (!user) {
          setProfile(DEMO_PROFILE);
          setFormData(DEMO_PROFILE);
          setPreviewMode(true);
          return;
        }

        const response = await api.get("/users/profile");
        const data = normalizeProfile(response.data) || DEMO_PROFILE;
        setProfile(data);
        setFormData(data);
      } catch {
        setProfile(DEMO_PROFILE);
        setFormData(DEMO_PROFILE);
        setPreviewMode(true);
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [user]);

  const age = useMemo(() => {
    const dob = profile?.dateOfBirth ? new Date(profile.dateOfBirth) : null;
    if (!dob || Number.isNaN(dob.getTime())) return null;
    return new Date().getFullYear() - dob.getFullYear();
  }, [profile]);

  const completion = useMemo(() => {
    if (!profile) return 0;

    const checks = [
      profile.firstName,
      profile.dateOfBirth,
      profile.gender,
      profile.maritalStatus,
      profile.city,
      profile.profession,
      profile.educationLevel,
      profile.bio,
      profile.heightCm,
      profile.photos?.length,
    ];

    const filled = checks.filter(Boolean).length;
    return Math.round((filled / checks.length) * 100);
  }, [profile]);

  const primaryPhoto = profile?.photos?.find((photo) => photo.isPrimary)?.photoUrl || profile?.photos?.[0]?.photoUrl;

  const profileSignals = useMemo(
    () => [
      { key: "completion", label: "Profile Score", value: `${completion}%` },
      { key: "age", label: "Age", value: age ? `${age} yrs` : "Not set" },
      { key: "photos", label: "Photos", value: `${profile?.photos?.length || 0}` },
      { key: "hobbies", label: "Interests", value: `${profile?.hobbies?.length || 0}` },
    ],
    [completion, age, profile]
  );

  const onFieldChange = (key, value) => {
    setFormData((previous) => ({ ...previous, [key]: value }));
  };

  const onSave = async () => {
    if (previewMode) {
      toast.info("Login to save profile changes.");
      return;
    }

    setSaving(true);
    try {
      await api.put("/users/profile", formData);
      setProfile((previous) => ({ ...previous, ...formData }));
      setEditing(false);
      toast.success("Profile updated successfully.");
    } catch {
      setProfile((previous) => ({ ...previous, ...formData }));
      setEditing(false);
      toast.info("Profile updated in local preview mode.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>Loading profile...</div>;
  }

  if (!profile) {
    return <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>Unable to load profile.</div>;
  }

  return (
    <div style={{ display: "grid", gap: "0.9rem" }}>
      <section className="panel profile-hero" style={{ overflow: "hidden" }}>
        <div className="profile-hero-banner" style={{ height: 170, position: "relative" }}>
          <div
            style={{
              position: "absolute",
              inset: 0,
              background: "linear-gradient(140deg, #1e2b51 0%, #24406f 45%, #0f8ca0 100%)",
            }}
          />
          <div
            style={{
              position: "absolute",
              inset: 0,
              background: "radial-gradient(circle at 80% 20%, rgba(240,107,78,0.45), transparent 45%)",
            }}
          />
        </div>

        <div style={{ padding: "0.95rem", marginTop: -54, position: "relative" }}>
          <div className="profile-hero-head" style={{ display: "grid", gridTemplateColumns: "84px minmax(0,1fr) auto", gap: "0.8rem", alignItems: "end" }}>
            <div className="profile-avatar" style={{ width: 84, height: 84, borderRadius: "50%", overflow: "hidden", border: "4px solid #fff", boxShadow: "var(--shadow-md)", background: "#f1ede6" }}>
              {primaryPhoto ? (
                <Image src={primaryPhoto} alt="Profile photo" width={200} height={200} unoptimized style={{ width: "100%", height: "100%", objectFit: "cover" }} />
              ) : (
                <div style={{ width: "100%", height: "100%", display: "grid", placeItems: "center", fontWeight: 800 }}>
                  {(profile.firstName || "U").charAt(0).toUpperCase()}
                </div>
              )}
            </div>

            <div style={{ minWidth: 0 }}>
              <h1 style={{ margin: 0, fontSize: "1.7rem", fontFamily: "var(--font-display)", lineHeight: 1.05 }}>
                {profile.firstName} {profile.lastName}
              </h1>
              <p style={{ margin: "0.26rem 0 0", color: "var(--ink-muted)", fontSize: "0.9rem" }}>
                {age ? `${age} yrs | ` : ""}
                {profile.profession || "Professional"}
                {profile.city ? ` | ${profile.city}` : ""}
              </p>
              {previewMode && <span className="chip chip-brand" style={{ marginTop: "0.45rem" }}>Preview Mode</span>}
            </div>

            <div className="profile-action-row" style={{ display: "flex", gap: "0.45rem", flexWrap: "wrap", justifyContent: "flex-end" }}>
              {editing ? (
                <>
                  <button type="button" className="button button-secondary" onClick={() => setEditing(false)} disabled={saving}>
                    Cancel
                  </button>
                  <button type="button" className="button button-primary" onClick={onSave} disabled={saving}>
                    {saving ? "Saving..." : "Save"}
                  </button>
                </>
              ) : (
                <button type="button" className="button button-primary" onClick={() => setEditing(true)}>
                  Edit Profile
                </button>
              )}
            </div>
          </div>

          <div className="completion-shell" style={{ marginTop: "0.82rem", display: "grid", gridTemplateColumns: "1fr auto", gap: "0.6rem", alignItems: "center" }}>
            <div style={{ background: "#e8edf8", borderRadius: 999, height: 8, overflow: "hidden" }}>
              <div
                style={{
                  width: `${completion}%`,
                  height: "100%",
                  background: "linear-gradient(90deg, var(--brand), var(--support))",
                }}
              />
            </div>
            <span style={{ fontSize: "0.8rem", fontWeight: 700, color: "var(--ink-muted)" }}>{completion}% complete</span>
          </div>

          <div className="profile-signals" style={{ marginTop: "0.78rem", display: "grid", gridTemplateColumns: "repeat(4, minmax(0, 1fr))", gap: "0.52rem" }}>
            {profileSignals.map((signal) => (
              <div key={signal.key} className="profile-signal-tile">
                <p className="signal-value">{signal.value}</p>
                <p className="signal-label">{signal.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <div className="profile-main-grid" style={{ display: "grid", gridTemplateColumns: "1.08fr 0.92fr", gap: "0.9rem" }}>
        <section className="panel profile-block" style={{ padding: "0.95rem" }}>
          <p className="section-label" style={{ marginBottom: "0.35rem" }}>
            About
          </p>
          <h2 style={{ margin: "0 0 0.7rem", fontFamily: "var(--font-display)", fontSize: "1.45rem" }}>Personal Bio</h2>

          {editing ? (
            <textarea
              value={formData.bio || ""}
              onChange={(event) => onFieldChange("bio", event.target.value)}
              rows={5}
              className="form-input"
              style={{ resize: "vertical" }}
            />
          ) : (
            <p style={{ margin: 0, color: "var(--ink)", lineHeight: 1.65 }}>{profile.bio || "No bio added yet."}</p>
          )}

          <div style={{ marginTop: "0.9rem" }}>
            <p className="section-label" style={{ marginBottom: "0.35rem" }}>
              Interests
            </p>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "0.45rem" }}>
              {(profile.hobbies || []).length > 0 ? (
                profile.hobbies.map((item) => (
                  <span key={item} className="chip chip-support">
                    {item}
                  </span>
                ))
              ) : (
                <span style={{ color: "var(--ink-muted)", fontSize: "0.84rem" }}>No interests added yet.</span>
              )}
            </div>
          </div>
        </section>

        <section className="panel profile-block" style={{ padding: "0.95rem" }}>
          <p className="section-label" style={{ marginBottom: "0.35rem" }}>
            Details
          </p>
          <h2 style={{ margin: "0 0 0.7rem", fontFamily: "var(--font-display)", fontSize: "1.45rem" }}>Profile Information</h2>

          <div className="details-grid" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.58rem" }}>
            {EDITABLE_FIELDS.map((field) => (
              <div key={field.key}>
                <label className="form-label">{field.label}</label>
                {editing ? (
                  <input
                    className="form-input"
                    type={field.type}
                    value={formData[field.key] || ""}
                    onChange={(event) => onFieldChange(field.key, field.type === "number" ? Number(event.target.value) : event.target.value)}
                  />
                ) : (
                  <div className="field-tile">
                    {String(profile[field.key] || "-").replaceAll("_", " ")}
                  </div>
                )}
              </div>
            ))}
          </div>

          <div style={{ marginTop: "0.85rem", display: "flex", gap: "0.45rem", flexWrap: "wrap" }}>
            <Link href="/settings" className="button button-secondary">
              Privacy Settings
            </Link>
            <Link href="/pricing" className="button button-primary">
              Upgrade Profile
            </Link>
          </div>
        </section>
      </div>

    </div>
  );
}
