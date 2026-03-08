"use client";

import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { settingsRules } from "../../../validation/rules";

const SECTIONS = [
  { key: "account", label: "Account" },
  { key: "partner", label: "Partner Preferences" },
  { key: "privacy", label: "Privacy" },
  { key: "notif", label: "Notifications" },
  { key: "password", label: "Change Password" },
  { key: "danger", label: "Danger Zone" },
];

const RELIGIONS = ["Any", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist"];
const INCOMES = ["Any", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"];
const EDUCATIONS = ["Any", "Graduate", "Post Graduate", "Doctorate", "Professional"];
const PROFESSIONS = ["Any", "Engineer", "Doctor", "CA/Finance", "Govt/PSU", "Lawyer", "Business", "NRI"];

function toDateInput(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toISOString().slice(0, 10);
}

function formatGender(value) {
  const normalized = String(value || "").trim().toLowerCase();
  if (normalized === "male") return "Male";
  if (normalized === "female") return "Female";
  if (normalized === "other") return "Other";
  return "Male";
}

function Toggle({ checked, onChange, labelledBy }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-labelledby={labelledBy}
      onClick={() => onChange(!checked)}
      className={`toggle-v2 ${checked ? "on" : ""}`}
    >
      <span />
    </button>
  );
}

function Field({ label, children }) {
  return (
    <div className="settings-field-v2">
      <label className="form-label" htmlFor={children?.props?.id}>
        {label}
      </label>
      {children}
    </div>
  );
}

export default function SettingsPage() {
  const [section, setSection] = useState("account");
  const [saving, setSaving] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const [passwordErrors, setPasswordErrors] = useState({});

  const [account, setAccount] = useState({
    firstName: "",
    lastName: "",
    phone: "",
    email: "",
    dob: "",
    gender: "Male",
  });

  const [partner, setPartner] = useState({
    minAge: "21",
    maxAge: "35",
    religion: "Any",
    education: "Any",
    profession: "Any",
    income: "Any",
    city: "",
  });

  const [privacy, setPrivacy] = useState({
    showPhone: false,
    showPhoto: true,
    showProfile: true,
    allowSearch: true,
    showLastSeen: false,
    allowMessages: true,
  });

  const [notif, setNotif] = useState({
    emailNewMatch: true,
    emailInterest: true,
    emailMessage: false,
    pushNewMatch: true,
    pushInterest: true,
    pushMessage: true,
    smsAlert: false,
  });

  const [passwd, setPasswd] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      try {
        const [privacyResponse, profileResponse] = await Promise.all([
          api.get("/users/privacy").catch(() => null),
          api.get("/users/profile").catch(() => null),
        ]);

        if (!mounted) return;

        if (privacyResponse?.data?.settings) {
          setPrivacy((prev) => ({ ...prev, ...privacyResponse.data.settings }));
        }

        const profile = profileResponse?.data;
        if (!profile) return;

        setAccount((prev) => ({
          ...prev,
          firstName: profile.firstName || "",
          lastName: profile.lastName || "",
          phone: profile.phone || "",
          email: profile.email || "",
          dob: toDateInput(profile.dateOfBirth),
          gender: formatGender(profile.gender),
        }));

        const pref = profile.partnerPreference || {};
        setPartner((prev) => ({
          ...prev,
          minAge: String(pref.minAge ?? prev.minAge),
          maxAge: String(pref.maxAge ?? prev.maxAge),
          religion: pref.preferredReligions?.[0] || "Any",
          income: pref.minIncomeBand || "Any",
          city: Array.isArray(pref.preferredLocations) ? pref.preferredLocations.join(", ") : "",
        }));
      } catch {
        // Keep defaults when offline.
      }
    };

    load();
    return () => {
      mounted = false;
    };
  }, []);

  const save = async (label) => {
    setSaving(true);
    setStatusMessage("");

    try {
      if (section === "account") {
        await api.put("/users/profile", {
          firstName: account.firstName || undefined,
          lastName: account.lastName || undefined,
          dateOfBirth: account.dob || undefined,
          gender: account.gender ? account.gender.toLowerCase() : undefined,
        });
      }

      if (section === "partner") {
        await api.put("/users/profile/preferences", {
          minAge: Number.parseInt(partner.minAge, 10) || 21,
          maxAge: Number.parseInt(partner.maxAge, 10) || 35,
          religion: partner.religion || "Any",
          income: partner.income || "Any",
          education: partner.education || "Any",
          profession: partner.profession || "Any",
          city: partner.city || "",
        });
      }

      if (section === "privacy") {
        await api.post("/users/privacy", privacy);
      }

      if (section === "password") {
        const errors = {};

        if (!passwd.currentPassword) {
          errors.currentPassword = settingsRules.password.currentPassword.required;
        }
        if (!passwd.newPassword) {
          errors.newPassword = settingsRules.password.newPassword.required;
        } else if (passwd.newPassword.length < settingsRules.password.newPassword.minLength.value) {
          errors.newPassword = settingsRules.password.newPassword.minLength.message;
        }
        if (!passwd.confirmPassword) {
          errors.confirmPassword = "Confirm password is required";
        } else if (passwd.newPassword !== passwd.confirmPassword) {
          errors.confirmPassword = "Passwords do not match";
        }

        if (Object.keys(errors).length > 0) {
          setPasswordErrors(errors);
          const message = Object.values(errors)[0];
          setStatusMessage(String(message));
          toast.error(String(message));
          setSaving(false);
          return;
        }

        setPasswordErrors({});
        await api.put("/users/password", {
          currentPassword: passwd.currentPassword,
          newPassword: passwd.newPassword,
        });
        setPasswd({ currentPassword: "", newPassword: "", confirmPassword: "" });
      }

      toast.success(`${label} saved.`);
      setStatusMessage(`${label} saved.`);
    } catch (error) {
      const message = error?.response?.data?.error || `Failed to save ${label.toLowerCase()}.`;
      toast.error(message);
      setStatusMessage(message);
    } finally {
      setSaving(false);
    }
  };

  const sectionTitle = useMemo(() => SECTIONS.find((item) => item.key === section)?.label || "Settings", [section]);

  return (
    <div className="settings-page-v2">
      <header className="panel settings-hero-v2">
        <p className="section-label">Control Center</p>
        <h1 className="section-title">Settings</h1>
        <p className="section-copy">Manage your account details, discovery preferences, privacy controls, and security.</p>
      </header>

      <p aria-live="polite" className="settings-status-v2">
        {statusMessage}
      </p>

      <div className="settings-layout-v2">
        <aside className="panel settings-nav-v2" aria-label="Settings sections">
          {SECTIONS.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`settings-tab-v2 ${section === item.key ? "active" : ""}`}
              onClick={() => setSection(item.key)}
            >
              {item.label}
            </button>
          ))}
        </aside>

        <section className="panel settings-content-v2">
          <header className="settings-content-head-v2">
            <h2>{sectionTitle}</h2>
            <p>
              {section === "partner"
                ? "Update recommendation preferences used by smart matching and search."
                : "Customize this section and save changes instantly."}
            </p>
          </header>

          {section === "account" && (
            <div className="settings-grid-v2">
              <Field label="First Name">
                <input
                  id="account_first_name"
                  className="form-input"
                  value={account.firstName}
                  onChange={(event) => setAccount((prev) => ({ ...prev, firstName: event.target.value }))}
                />
              </Field>
              <Field label="Last Name">
                <input
                  id="account_last_name"
                  className="form-input"
                  value={account.lastName}
                  onChange={(event) => setAccount((prev) => ({ ...prev, lastName: event.target.value }))}
                />
              </Field>
              <Field label="Phone">
                <input id="account_phone" className="form-input input-readonly-v2" value={account.phone} readOnly />
              </Field>
              <Field label="Email">
                <input id="account_email" className="form-input input-readonly-v2" value={account.email} readOnly />
              </Field>
              <Field label="Date of Birth">
                <input
                  id="account_dob"
                  type="date"
                  className="form-input"
                  value={account.dob}
                  onChange={(event) => setAccount((prev) => ({ ...prev, dob: event.target.value }))}
                />
              </Field>
              <Field label="Gender">
                <select
                  id="account_gender"
                  className="form-input"
                  value={account.gender}
                  onChange={(event) => setAccount((prev) => ({ ...prev, gender: event.target.value }))}
                >
                  <option>Male</option>
                  <option>Female</option>
                  <option>Other</option>
                </select>
              </Field>

              <div className="settings-action-row-v2">
                <button type="button" onClick={() => save("Account")} disabled={saving} className="button button-primary">
                  {saving ? "Saving..." : "Save Account"}
                </button>
              </div>
            </div>
          )}

          {section === "partner" && (
            <div className="settings-grid-v2">
              <Field label="Min Age">
                <input
                  id="partner_min_age"
                  type="number"
                  className="form-input"
                  value={partner.minAge}
                  onChange={(event) => setPartner((prev) => ({ ...prev, minAge: event.target.value }))}
                />
              </Field>
              <Field label="Max Age">
                <input
                  id="partner_max_age"
                  type="number"
                  className="form-input"
                  value={partner.maxAge}
                  onChange={(event) => setPartner((prev) => ({ ...prev, maxAge: event.target.value }))}
                />
              </Field>
              <Field label="Religion">
                <select
                  id="partner_religion"
                  className="form-input"
                  value={partner.religion}
                  onChange={(event) => setPartner((prev) => ({ ...prev, religion: event.target.value }))}
                >
                  {RELIGIONS.map((item) => (
                    <option key={item}>{item}</option>
                  ))}
                </select>
              </Field>
              <Field label="Income">
                <select
                  id="partner_income"
                  className="form-input"
                  value={partner.income}
                  onChange={(event) => setPartner((prev) => ({ ...prev, income: event.target.value }))}
                >
                  {INCOMES.map((item) => (
                    <option key={item}>{item}</option>
                  ))}
                </select>
              </Field>
              <Field label="Education">
                <select
                  id="partner_education"
                  className="form-input"
                  value={partner.education}
                  onChange={(event) => setPartner((prev) => ({ ...prev, education: event.target.value }))}
                >
                  {EDUCATIONS.map((item) => (
                    <option key={item}>{item}</option>
                  ))}
                </select>
              </Field>
              <Field label="Profession">
                <select
                  id="partner_profession"
                  className="form-input"
                  value={partner.profession}
                  onChange={(event) => setPartner((prev) => ({ ...prev, profession: event.target.value }))}
                >
                  {PROFESSIONS.map((item) => (
                    <option key={item}>{item}</option>
                  ))}
                </select>
              </Field>
              <Field label="Preferred City">
                <input
                  id="partner_city"
                  className="form-input"
                  value={partner.city}
                  onChange={(event) => setPartner((prev) => ({ ...prev, city: event.target.value }))}
                />
              </Field>

              <div className="settings-action-row-v2">
                <button
                  type="button"
                  onClick={() => save("Partner Preferences")}
                  disabled={saving}
                  className="button button-primary"
                >
                  {saving ? "Saving..." : "Save Preferences"}
                </button>
              </div>
            </div>
          )}

          {section === "privacy" && (
            <div className="settings-toggle-list-v2">
              {[
                ["showPhone", "Show phone to matches"],
                ["showPhoto", "Show photos"],
                ["showProfile", "Profile visible"],
                ["allowSearch", "Allow search listing"],
                ["showLastSeen", "Show last seen"],
                ["allowMessages", "Allow messages"],
              ].map(([field, label]) => (
                <div key={field} className="settings-toggle-row-v2">
                  <span id={`privacy_${field}_label`}>{label}</span>
                  <Toggle
                    labelledBy={`privacy_${field}_label`}
                    checked={privacy[field]}
                    onChange={(value) => setPrivacy((prev) => ({ ...prev, [field]: value }))}
                  />
                </div>
              ))}

              <div className="settings-action-row-v2">
                <button type="button" onClick={() => save("Privacy")} disabled={saving} className="button button-primary">
                  {saving ? "Saving..." : "Save Privacy"}
                </button>
              </div>
            </div>
          )}

          {section === "notif" && (
            <div className="settings-toggle-list-v2">
              {[
                ["emailNewMatch", "Email: new match"],
                ["emailInterest", "Email: interest received"],
                ["emailMessage", "Email: new message"],
                ["pushNewMatch", "Push: new match"],
                ["pushInterest", "Push: interest received"],
                ["pushMessage", "Push: new message"],
                ["smsAlert", "SMS alerts"],
              ].map(([field, label]) => (
                <div key={field} className="settings-toggle-row-v2">
                  <span id={`notif_${field}_label`}>{label}</span>
                  <Toggle
                    labelledBy={`notif_${field}_label`}
                    checked={notif[field]}
                    onChange={(value) => setNotif((prev) => ({ ...prev, [field]: value }))}
                  />
                </div>
              ))}

              <div className="settings-action-row-v2">
                <button
                  type="button"
                  className="button button-primary"
                  onClick={() => {
                    const message = "Notification preferences updated locally.";
                    toast.success(message);
                    setStatusMessage(message);
                  }}
                >
                  Save Notifications
                </button>
              </div>
            </div>
          )}

          {section === "password" && (
            <div className="settings-password-v2">
              <Field label="Current Password">
                <input
                  id="password_current"
                  type="password"
                  className="form-input"
                  value={passwd.currentPassword}
                  onChange={(event) => {
                    setPasswd((prev) => ({ ...prev, currentPassword: event.target.value }));
                    setPasswordErrors((prev) => ({ ...prev, currentPassword: undefined }));
                  }}
                  aria-invalid={passwordErrors.currentPassword ? "true" : "false"}
                  aria-describedby={passwordErrors.currentPassword ? "password_current_error" : undefined}
                />
              </Field>
              {passwordErrors.currentPassword && (
                <p id="password_current_error" role="alert" className="form-error">
                  {passwordErrors.currentPassword}
                </p>
              )}

              <Field label="New Password">
                <input
                  id="password_new"
                  type="password"
                  className="form-input"
                  value={passwd.newPassword}
                  onChange={(event) => {
                    setPasswd((prev) => ({ ...prev, newPassword: event.target.value }));
                    setPasswordErrors((prev) => ({ ...prev, newPassword: undefined }));
                  }}
                  aria-invalid={passwordErrors.newPassword ? "true" : "false"}
                  aria-describedby={passwordErrors.newPassword ? "password_new_error" : undefined}
                />
              </Field>
              {passwordErrors.newPassword && (
                <p id="password_new_error" role="alert" className="form-error">
                  {passwordErrors.newPassword}
                </p>
              )}

              <Field label="Confirm Password">
                <input
                  id="password_confirm"
                  type="password"
                  className="form-input"
                  value={passwd.confirmPassword}
                  onChange={(event) => {
                    setPasswd((prev) => ({ ...prev, confirmPassword: event.target.value }));
                    setPasswordErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                  }}
                  aria-invalid={passwordErrors.confirmPassword ? "true" : "false"}
                  aria-describedby={passwordErrors.confirmPassword ? "password_confirm_error" : undefined}
                />
              </Field>
              {passwordErrors.confirmPassword && (
                <p id="password_confirm_error" role="alert" className="form-error">
                  {passwordErrors.confirmPassword}
                </p>
              )}

              <div className="settings-action-row-v2">
                <button type="button" onClick={() => save("Password")} disabled={saving} className="button button-primary">
                  {saving ? "Saving..." : "Update Password"}
                </button>
              </div>
            </div>
          )}

          {section === "danger" && (
            <div className="settings-danger-zone-v2">
              <article className="danger-card-v2 soft">
                <p>Hide profile</p>
                <small>Temporarily remove your profile from discovery while keeping all your account data intact.</small>
                <button type="button" className="button button-secondary" onClick={() => toast.info("Feature requires confirmation flow.")}>
                  Hide Profile
                </button>
              </article>

              <article className="danger-card-v2 hard">
                <p>Delete account</p>
                <small>Permanently delete your account and all related data after verification confirmation.</small>
                <button type="button" className="button button-secondary" onClick={() => toast.warn("Account deletion requires verification flow.")}>
                  Delete Account
                </button>
              </article>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
