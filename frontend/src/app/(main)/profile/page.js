"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import SafeProfileImage from "../../../components/SafeProfileImage";

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
  { key: "caste", label: "Community / caste", type: "text" },
  { key: "motherTongue", label: "Mother tongue", type: "text" },
  { key: "profession", label: "Profession", type: "text" },
  { key: "educationLevel", label: "Education", type: "text" },
  { key: "incomeBand", label: "Income", type: "text" },
  { key: "heightCm", label: "Height (cm)", type: "number" },
];

function ageFromDob(value) {
  if (!value) return null;
  const dob = new Date(value);
  if (Number.isNaN(dob.getTime())) return null;
  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const delta = now.getMonth() - dob.getMonth();
  if (delta < 0 || (delta === 0 && now.getDate() < dob.getDate())) age -= 1;
  return age;
}

function toDateInput(value) {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toISOString().slice(0, 10);
}

export default function ProfilePage() {
  const { user } = useAuth();
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState(null);
  const [formData, setFormData] = useState({});

  const loadProfile = useCallback(async () => {
    if (!user) {
      setProfile(null);
      setState("signed_out");
      return;
    }
    setState("loading");
    setError("");
    try {
      const response = await api.get("/users/profile");
      setProfile(response.data || null);
      setFormData(response.data || {});
      setState("content");
    } catch (err) {
      setProfile(null);
      setError(err.response?.data?.error || "Couldn’t load your profile.");
      setState("error");
    }
  }, [user]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const age = useMemo(() => ageFromDob(profile?.dateOfBirth), [profile]);
  const primaryPhoto = profile?.photos?.find((photo) => photo.isPrimary)?.photoUrl || profile?.photos?.[0]?.photoUrl || null;
  const completeness = Number.isFinite(Number(profile?.completionPercentage)) ? Number(profile.completionPercentage) : null;

  const onFieldChange = (key, value) => setFormData((previous) => ({ ...previous, [key]: value }));

  const onSave = async () => {
    if (!user || saving) return;
    setSaving(true);
    try {
      const payload = { ...formData };
      if (payload.dateOfBirth) payload.dateOfBirth = toDateInput(payload.dateOfBirth);
      const response = await api.put("/users/profile", payload);
      setProfile((current) => ({ ...current, ...response.data }));
      setFormData((current) => ({ ...current, ...response.data }));
      setEditing(false);
      toast.success("Profile updated.");
    } catch (err) {
      toast.error(err.response?.data?.error || "Profile update failed. Your previous data is unchanged.");
    } finally {
      setSaving(false);
    }
  };

  if (state === "signed_out") {
    return (
      <section className="panel" style={{ padding: "2rem", textAlign: "center" }}>
        <h1 style={{ marginTop: 0 }}>My Profile</h1>
        <p style={{ color: "var(--ink-muted)" }}>Sign in to view and edit your real profile.</p>
        <Link href="/login" className="button button-primary">Sign In</Link>
      </section>
    );
  }

  if (state === "loading") return <section className="panel" style={{ padding: "2rem" }}>Loading profile…</section>;

  if (state === "error") {
    return (
      <section className="panel" role="alert" style={{ padding: "2rem" }}>
        <h1 style={{ marginTop: 0 }}>Couldn’t load profile</h1>
        <p style={{ color: "var(--ink-muted)" }}>{error}</p>
        <button type="button" className="button button-primary" onClick={loadProfile}>Retry</button>
      </section>
    );
  }

  if (!profile?.firstName) {
    return (
      <section className="panel" style={{ padding: "2rem", textAlign: "center" }}>
        <h1 style={{ marginTop: 0 }}>Complete your matrimony profile</h1>
        <p style={{ color: "var(--ink-muted)" }}>Your account exists, but required profile details have not been completed yet.</p>
        <Link href="/step-1" className="button button-primary">Continue Onboarding</Link>
      </section>
    );
  }

  return (
    <div style={{ display: "grid", gap: "0.9rem" }}>
      <section className="panel" style={{ padding: "1rem" }}>
        <div style={{ display: "grid", gridTemplateColumns: "96px minmax(0,1fr) auto", gap: "0.9rem", alignItems: "center" }}>
          <SafeProfileImage
            src={primaryPhoto}
            alt="Your profile photo"
            width={192}
            height={192}
            sizes="96px"
            style={{ width: 96, height: 96, borderRadius: "50%", objectFit: "cover" }}
          />
          <div>
            <h1 style={{ margin: 0, fontSize: "1.7rem" }}>{profile.firstName} {profile.lastName || ""}</h1>
            <p style={{ margin: "0.3rem 0 0", color: "var(--ink-muted)" }}>
              {[age ? `${age} yrs` : null, profile.profession, profile.city].filter(Boolean).join(" · ")}
            </p>
            <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginTop: "0.55rem" }}>
              {user?.isVerified && <span className="chip chip-support">Account verified</span>}
              {completeness !== null && <span className="chip chip-brand">{completeness}% profile complete</span>}
            </div>
          </div>
          <div style={{ display: "flex", gap: "0.45rem", flexWrap: "wrap", justifyContent: "flex-end" }}>
            {editing ? (
              <>
                <button type="button" className="button button-secondary" disabled={saving} onClick={() => { setEditing(false); setFormData(profile); }}>Cancel</button>
                <button type="button" className="button button-primary" disabled={saving} onClick={onSave}>{saving ? "Saving…" : "Save"}</button>
              </>
            ) : (
              <button type="button" className="button button-primary" onClick={() => setEditing(true)}>Edit Profile</button>
            )}
          </div>
        </div>
      </section>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.9rem" }}>
        <section className="panel" style={{ padding: "1rem" }}>
          <p className="section-label">About</p>
          <h2 style={{ marginTop: "0.35rem" }}>Personal Bio</h2>
          {editing ? (
            <textarea className="form-input" rows={5} value={formData.bio || ""} onChange={(event) => onFieldChange("bio", event.target.value)} />
          ) : (
            <p style={{ color: "var(--ink-muted)", lineHeight: 1.65 }}>{profile.bio || "No bio added yet."}</p>
          )}
          <p className="section-label" style={{ marginTop: "1rem" }}>Interests</p>
          <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap" }}>
            {(profile.hobbies || []).length ? profile.hobbies.map((item) => <span key={item} className="chip chip-brand">{item}</span>) : <span style={{ color: "var(--ink-muted)" }}>No interests added yet.</span>}
          </div>
        </section>

        <section className="panel" style={{ padding: "1rem" }}>
          <p className="section-label">Profile Details</p>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.6rem" }}>
            {EDITABLE_FIELDS.map((field) => (
              <div key={field.key}>
                <label className="form-label">{field.label}</label>
                {editing ? (
                  <input
                    className="form-input"
                    type={field.type}
                    value={field.key === "dateOfBirth" ? toDateInput(formData[field.key]) : (formData[field.key] ?? "")}
                    onChange={(event) => onFieldChange(field.key, field.type === "number" ? Number(event.target.value) : event.target.value)}
                    disabled={field.key === "religion"}
                  />
                ) : (
                  <div className="field-tile">{String(profile[field.key] ?? "Not provided").replaceAll("_", " ")}</div>
                )}
              </div>
            ))}
          </div>
          <div style={{ marginTop: "0.85rem", display: "flex", gap: "0.45rem", flexWrap: "wrap" }}>
            <Link href="/settings" className="button button-secondary">Privacy Settings</Link>
            <Link href="/verification" className="button button-secondary">Verification</Link>
          </div>
        </section>
      </div>
    </div>
  );
}
