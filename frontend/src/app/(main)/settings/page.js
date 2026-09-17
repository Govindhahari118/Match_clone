"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import TrustStatusPanel from "../../../components/TrustStatusPanel";

const SECTIONS = [
  ["status", "Search status"],
  ["partner", "Partner preferences"],
  ["privacy", "Privacy"],
  ["account", "Account & deletion"],
];

const HARD_FIELDS = [
  ["age", "Age range"],
  ["location", "Location"],
  ["religion", "Religion"],
  ["caste", "Community / caste"],
  ["food_habit", "Food habit"],
];

const STATUS_OPTIONS = [
  ["active", "Actively searching", "Your profile is eligible for discovery when other requirements are met."],
  ["low_activity", "Searching less actively", "Remain discoverable with lower freshness priority."],
  ["paused", "Temporarily pause", "Hide from new discovery while keeping your account and history."],
  ["found_match", "Found a match", "Stop new discovery and interests."],
  ["married", "Married", "Remove the profile from matrimony discovery."],
];

function csv(value) {
  if (Array.isArray(value)) return value.join(", ");
  return value || "";
}
function arrayValue(value) {
  return String(value || "").split(",").map((item) => item.trim()).filter(Boolean);
}

export default function SettingsPage() {
  const [section, setSection] = useState("status");
  const [profile, setProfile] = useState(null);
  const [trust, setTrust] = useState(null);
  const [preferences, setPreferences] = useState({
    minAge: 18,
    maxAge: 60,
    preferredLocations: "",
    preferredReligions: "",
    preferredCastes: "",
    foodHabitPreferences: "",
    minEducation: "",
    minIncomeBand: "",
    hardFields: [],
  });
  const [privacy, setPrivacy] = useState({
    showPhone: false,
    showPhoto: true,
    showProfile: true,
    allowSearch: true,
    showLastSeen: false,
    allowMessages: true,
    photoVisibility: "public",
    profileVisibility: "public",
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletePassword, setDeletePassword] = useState("");
  const [deleteConfirmation, setDeleteConfirmation] = useState("");

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      try {
        const [profileResponse, privacyResponse, preferenceResponse, trustResponse] = await Promise.all([
          api.get("/users/profile"),
          api.get("/users/privacy"),
          api.get("/users/profile/preferences"),
          api.get("/trust/me"),
        ]);
        if (cancelled) return;
        setProfile(profileResponse.data);
        setTrust(trustResponse.data);
        if (privacyResponse.data?.settings) setPrivacy((prev) => ({ ...prev, ...privacyResponse.data.settings }));
        const pref = preferenceResponse.data;
        if (pref) {
          setPreferences({
            minAge: pref.minAge ?? 18,
            maxAge: pref.maxAge ?? 60,
            preferredLocations: csv(pref.preferredLocations),
            preferredReligions: csv(pref.preferredReligions),
            preferredCastes: csv(pref.preferredCastes),
            foodHabitPreferences: csv(pref.foodHabitPreferences),
            minEducation: pref.minEducation || "",
            minIncomeBand: pref.minIncomeBand || "",
            hardFields: Array.isArray(pref.hardFields) ? pref.hardFields : [],
          });
        }
      } catch (error) {
        toast.error(error?.response?.data?.error || "Settings could not be loaded.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, []);

  const hardSet = useMemo(() => new Set(preferences.hardFields), [preferences.hardFields]);

  const savePreferences = async () => {
    setSaving(true);
    try {
      const payload = {
        minAge: Number(preferences.minAge),
        maxAge: Number(preferences.maxAge),
        preferredLocations: arrayValue(preferences.preferredLocations),
        preferredReligions: arrayValue(preferences.preferredReligions),
        preferredCastes: arrayValue(preferences.preferredCastes),
        foodHabitPreferences: arrayValue(preferences.foodHabitPreferences),
        minEducation: preferences.minEducation || null,
        minIncomeBand: preferences.minIncomeBand || null,
        hardFields: preferences.hardFields,
      };
      await api.put("/users/profile/preferences", payload);
      toast.success("Partner preferences saved. Must-match choices will not be silently relaxed.");
    } catch (error) {
      toast.error(error?.response?.data?.error || "Preferences could not be saved.");
    } finally {
      setSaving(false);
    }
  };

  const savePrivacy = async () => {
    setSaving(true);
    try {
      const response = await api.post("/users/privacy", privacy);
      setPrivacy(response.data.settings || privacy);
      toast.success("Privacy controls saved.");
    } catch (error) {
      toast.error(error?.response?.data?.error || "Privacy settings could not be saved.");
    } finally {
      setSaving(false);
    }
  };

  const reconfirm = async (status) => {
    setSaving(true);
    try {
      const response = await api.post("/trust/reconfirm", { status });
      const refreshed = await api.get("/trust/me");
      setTrust(refreshed.data);
      toast.success(`Search status updated to ${response.data.searchStatus}.`);
    } catch (error) {
      toast.error(error?.response?.data?.error || "Search status could not be updated.");
    } finally {
      setSaving(false);
    }
  };

  const deleteProfile = async () => {
    if (deleteConfirmation !== "DELETE") {
      toast.error("Type DELETE exactly to confirm.");
      return;
    }
    setSaving(true);
    try {
      await api.delete("/users/profile", { data: { confirmation: "DELETE", password: deletePassword || undefined } });
      toast.success("Your matrimony profile has been removed from public discovery.");
      window.location.href = "/";
    } catch (error) {
      toast.error(error?.response?.data?.error || "Profile deletion failed.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>Loading settings…</div>;

  return (
    <div style={{ maxWidth: 980, margin: "0 auto", display: "grid", gap: "0.9rem" }}>
      <header>
        <p className="section-label" style={{ marginBottom: "0.2rem" }}>Control center</p>
        <h1 style={{ margin: 0, fontFamily: "var(--font-display)", fontSize: "1.9rem" }}>Matrimony Settings</h1>
        <p style={{ margin: "0.35rem 0 0", color: "var(--ink-muted)" }}>Control discovery, must-match preferences, privacy and account lifecycle from one place.</p>
      </header>

      <div style={{ display: "flex", gap: "0.45rem", flexWrap: "wrap" }}>
        {SECTIONS.map(([key, label]) => (
          <button key={key} type="button" className={section === key ? "button button-primary" : "button button-secondary"} onClick={() => setSection(key)}>{label}</button>
        ))}
      </div>

      {section === "status" && (
        <div style={{ display: "grid", gap: "0.8rem" }}>
          <TrustStatusPanel trust={trust} />
          <section className="panel" style={{ padding: "1rem" }}>
            <h2 style={{ marginTop: 0 }}>Are you still looking for a match?</h2>
            <p style={{ color: "var(--ink-muted)" }}>Reconfirming prevents married, paused and stale profiles from remaining in active discovery.</p>
            <div style={{ display: "grid", gap: "0.45rem" }}>
              {STATUS_OPTIONS.map(([value, label, description]) => (
                <button key={value} type="button" disabled={saving} onClick={() => reconfirm(value)} className="panel panel-hover" style={{ padding: "0.75rem", textAlign: "left", cursor: "pointer", borderColor: trust?.searchStatus === value ? "var(--brand)" : undefined }}>
                  <strong>{label}</strong>
                  <p style={{ margin: "0.25rem 0 0", color: "var(--ink-muted)", fontSize: "0.8rem" }}>{description}</p>
                </button>
              ))}
            </div>
          </section>
        </div>
      )}

      {section === "partner" && (
        <section className="panel" style={{ padding: "1rem" }}>
          <h2 style={{ marginTop: 0 }}>Partner preferences</h2>
          <p style={{ color: "var(--ink-muted)" }}>A “Must match” rule is enforced before ranking. A normal preference only influences ordering.</p>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(220px,1fr))", gap: "0.7rem" }}>
            <label>Min age<input className="form-input" type="number" min="18" max="80" value={preferences.minAge} onChange={(e) => setPreferences((p) => ({ ...p, minAge: e.target.value }))} /></label>
            <label>Max age<input className="form-input" type="number" min="18" max="80" value={preferences.maxAge} onChange={(e) => setPreferences((p) => ({ ...p, maxAge: e.target.value }))} /></label>
            <label>Locations<input className="form-input" value={preferences.preferredLocations} onChange={(e) => setPreferences((p) => ({ ...p, preferredLocations: e.target.value }))} placeholder="Hyderabad, Vijayawada" /></label>
            <label>Religions<input className="form-input" value={preferences.preferredReligions} onChange={(e) => setPreferences((p) => ({ ...p, preferredReligions: e.target.value }))} placeholder="Comma separated" /></label>
            <label>Communities / castes<input className="form-input" value={preferences.preferredCastes} onChange={(e) => setPreferences((p) => ({ ...p, preferredCastes: e.target.value }))} placeholder="Comma separated" /></label>
            <label>Food habits<input className="form-input" value={preferences.foodHabitPreferences} onChange={(e) => setPreferences((p) => ({ ...p, foodHabitPreferences: e.target.value }))} placeholder="vegetarian, non_vegetarian" /></label>
            <label>Minimum education<input className="form-input" value={preferences.minEducation} onChange={(e) => setPreferences((p) => ({ ...p, minEducation: e.target.value }))} /></label>
            <label>Minimum income band<input className="form-input" value={preferences.minIncomeBand} onChange={(e) => setPreferences((p) => ({ ...p, minIncomeBand: e.target.value }))} /></label>
          </div>
          <div style={{ marginTop: "0.9rem" }}>
            <strong>Must match</strong>
            <div style={{ display: "flex", gap: "0.42rem", flexWrap: "wrap", marginTop: "0.45rem" }}>
              {HARD_FIELDS.map(([key, label]) => (
                <label key={key} className="chip chip-support" style={{ cursor: "pointer" }}>
                  <input type="checkbox" checked={hardSet.has(key)} onChange={(e) => setPreferences((p) => ({ ...p, hardFields: e.target.checked ? Array.from(new Set([...p.hardFields, key])) : p.hardFields.filter((item) => item !== key) }))} /> {label}
                </label>
              ))}
            </div>
          </div>
          <button type="button" className="button button-primary" style={{ marginTop: "1rem" }} disabled={saving} onClick={savePreferences}>{saving ? "Saving…" : "Save preferences"}</button>
        </section>
      )}

      {section === "privacy" && (
        <section className="panel" style={{ padding: "1rem" }}>
          <h2 style={{ marginTop: 0 }}>Privacy</h2>
          <p style={{ color: "var(--ink-muted)" }}>Membership never overrides these member-level permissions.</p>
          <div style={{ display: "grid", gap: "0.65rem" }}>
            <label>Profile visibility<select className="form-input" value={privacy.profileVisibility} onChange={(e) => setPrivacy((p) => ({ ...p, profileVisibility: e.target.value }))}><option value="public">All eligible members</option><option value="verified_only">Verified members only</option><option value="premium_only">Premium members only</option><option value="hidden">Hidden</option></select></label>
            <label>Photo visibility<select className="form-input" value={privacy.photoVisibility} onChange={(e) => setPrivacy((p) => ({ ...p, photoVisibility: e.target.value }))}><option value="public">All eligible members</option><option value="protected">Mutual connections only</option><option value="request_access">Explicit approval required</option></select></label>
            {[
              ["showPhone", "Allow approved contact sharing"],
              ["showPhoto", "Allow photo visibility according to rule above"],
              ["showProfile", "Show matrimony profile"],
              ["allowSearch", "Allow discovery/search"],
              ["showLastSeen", "Show exact last-active time (activity bucket remains available)"],
              ["allowMessages", "Allow messages from mutual connections"],
            ].map(([field, label]) => <label key={field} className="panel" style={{ padding: "0.65rem" }}><input type="checkbox" checked={Boolean(privacy[field])} onChange={(e) => setPrivacy((p) => ({ ...p, [field]: e.target.checked }))} /> {label}</label>)}
          </div>
          <button type="button" className="button button-primary" style={{ marginTop: "1rem" }} disabled={saving} onClick={savePrivacy}>{saving ? "Saving…" : "Save privacy"}</button>
        </section>
      )}

      {section === "account" && (
        <div style={{ display: "grid", gap: "0.8rem" }}>
          <section className="panel" style={{ padding: "1rem" }}>
            <h2 style={{ marginTop: 0 }}>Account</h2>
            <p style={{ color: "var(--ink-muted)" }}>Profile version: {profile?.profileVersion ?? "-"}. Sensitive edits are versioned so another device cannot silently overwrite your changes.</p>
            <div style={{ display: "flex", gap: "0.45rem", flexWrap: "wrap" }}><Link className="button button-secondary" href="/profile">Edit profile</Link><Link className="button button-secondary" href="/verification">Verification center</Link><Link className="button button-secondary" href="/help">Support tickets</Link></div>
          </section>
          <section className="panel" style={{ padding: "1rem", borderColor: "#efb4a9" }}>
            <h2 style={{ marginTop: 0 }}>Delete matrimony profile</h2>
            <p style={{ color: "var(--ink-muted)" }}>Deletion immediately removes your profile from discovery and disables active matches. Internal retention, where legally required, remains separate from public visibility.</p>
            <input className="form-input" value={deleteConfirmation} onChange={(e) => setDeleteConfirmation(e.target.value)} placeholder="Type DELETE" />
            <input className="form-input" type="password" style={{ marginTop: "0.5rem" }} value={deletePassword} onChange={(e) => setDeletePassword(e.target.value)} placeholder="Password if your account uses one" />
            <button type="button" className="button button-secondary" style={{ marginTop: "0.7rem" }} disabled={saving || deleteConfirmation !== "DELETE"} onClick={deleteProfile}>Delete matrimony profile</button>
          </section>
        </div>
      )}
    </div>
  );
}
