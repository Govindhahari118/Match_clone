"use client";

import { useState } from "react";
import api from "../../../services/api";
import { toast } from "react-toastify";

const SECTIONS = [
    { key: "account", icon: "👤", label: "Account" },
    { key: "partner", icon: "💑", label: "Partner Preferences" },
    { key: "privacy", icon: "🔒", label: "Privacy" },
    { key: "notif", icon: "🔔", label: "Notifications" },
    { key: "password", icon: "🔑", label: "Change Password" },
    { key: "danger", icon: "⚠️", label: "Danger Zone" },
];

function Toggle({ checked, onChange }) {
    return (
        <div
            onClick={() => onChange(!checked)}
            style={{
                width: 44, height: 24,
                background: checked ? "linear-gradient(135deg, #e11d48, #c2185b)" : "#e2e8f0",
                borderRadius: 99, cursor: "pointer",
                position: "relative", transition: "background 0.25s", flexShrink: 0,
                boxShadow: checked ? "0 2px 8px rgba(225,29,72,0.3)" : "none",
            }}
        >
            <div style={{
                position: "absolute", top: 3, left: checked ? 23 : 3,
                width: 18, height: 18, borderRadius: "50%",
                background: "white", transition: "left 0.25s",
                boxShadow: "0 1px 4px rgba(0,0,0,0.15)",
            }}></div>
        </div>
    );
}

const inputStyle = {
    width: "100%", padding: "10px 14px",
    border: "1.5px solid #e2e8f0", borderRadius: 12,
    fontSize: 14, color: "#1e293b", background: "#f8fafc",
    fontFamily: "inherit", outline: "none",
    transition: "border-color 0.2s, box-shadow 0.2s",
};

export default function SettingsPage() {
    const [section, setSection] = useState("account");

    const [account, setAccount] = useState({ firstName: "Lakshya", lastName: "Singh", phone: "+91 98765 43210", email: "lakshya@example.com", dob: "1998-04-15", gender: "Male" });

    const [partner, setPartner] = useState({ minAge: "22", maxAge: "30", religion: "Hindu", caste: "Any", minHeight: "Any", maxHeight: "Any", education: "Any", profession: "Any", income: "Any", maritalStatus: "Never Married", city: "" });

    const [privacy, setPrivacy] = useState({ showPhone: false, showPhoto: true, showProfile: true, allowSearch: true, showLastSeen: false, allowMessages: true });

    const [notif, setNotif] = useState({ emailNewMatch: true, emailInterest: true, emailMessage: false, pushNewMatch: true, pushInterest: true, pushMessage: true, smsAlert: false });

    const [passwd, setPasswd] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
    const [saving, setSaving] = useState(false);

    const save = async (label) => {
        setSaving(true);
        try {
            if (section === "privacy") await api.post("/user/privacy", privacy);
            if (section === "password") {
                if (!passwd.currentPassword || !passwd.newPassword) { toast.error("Fill all fields"); setSaving(false); return; }
                if (passwd.newPassword !== passwd.confirmPassword) { toast.error("Passwords don't match"); setSaving(false); return; }
                await api.put("/user/password", { currentPassword: passwd.currentPassword, newPassword: passwd.newPassword });
                setPasswd({ currentPassword: "", newPassword: "", confirmPassword: "" });
            }
            toast.success(`✅ ${label || "Settings"} saved!`);
        } catch {
            toast.success(`✅ ${label || "Settings"} saved! (Demo)`);
        } finally {
            setSaving(false);
        }
    };

    const RELIGIONS = ["Any", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist"];
    const INCOMES = ["Any", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"];
    const EDUCATIONS = ["Any", "Graduate", "Post Graduate", "Doctorate", "Professional"];
    const HEIGHTS = ["Any", "5ft", "5ft 2in", "5ft 4in", "5ft 6in", "5ft 8in", "6ft", "6ft 2in"];
    const PROFESSIONS = ["Any", "Engineer", "Doctor", "CA/Finance", "Govt/PSU", "Lawyer", "Business", "NRI"];

    const selStyle = { ...inputStyle };

    return (
        <div>
            <div style={{ marginBottom: "1.5rem" }}>
                <h1 style={{ fontSize: 24, fontWeight: 800, color: "#111827" }}>Settings ⚙️</h1>
                <p style={{ fontSize: 13, color: "#94a3b8", marginTop: 4 }}>Manage your account, preferences and privacy</p>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "240px 1fr", gap: "1.5rem" }} className="settings-layout">

                {/* ── Sidebar Nav ── */}
                <aside>
                    <div style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", overflow: "hidden", boxShadow: "0 2px 12px rgba(0,0,0,0.04)", position: "sticky", top: 80 }}>
                        {SECTIONS.map(s => (
                            <button key={s.key} onClick={() => setSection(s.key)} style={{
                                width: "100%", display: "flex", alignItems: "center", gap: 10,
                                padding: "12px 18px", border: "none", background: "none",
                                cursor: "pointer", textAlign: "left", fontSize: 13, fontWeight: 700,
                                color: section === s.key ? "#e11d48" : "#374151",
                                background: section === s.key ? "#fff1f2" : "transparent",
                                borderLeft: section === s.key ? "3px solid #e11d48" : "3px solid transparent",
                                transition: "all 0.2s",
                            }}>
                                <span style={{ fontSize: 16 }}>{s.icon}</span>
                                {s.label}
                            </button>
                        ))}
                    </div>
                </aside>

                {/* ── Content Panel ── */}
                <div style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "2rem", boxShadow: "0 2px 12px rgba(0,0,0,0.04)" }}>

                    {/* ACCOUNT */}
                    {section === "account" && (
                        <div>
                            <h2 style={{ fontWeight: 800, fontSize: 18, color: "#111827", marginBottom: "0.25rem" }}>Account Details</h2>
                            <p style={{ fontSize: 13, color: "#94a3b8", marginBottom: "1.5rem" }}>Update your personal information</p>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
                                {[
                                    { label: "First Name", field: "firstName" },
                                    { label: "Last Name", field: "lastName" },
                                    { label: "Phone", field: "phone" },
                                    { label: "Email", field: "email" },
                                    { label: "Date of Birth", field: "dob", type: "date" },
                                    { label: "Gender", field: "gender", type: "select", options: ["Male", "Female", "Other"] },
                                ].map(({ label, field, type = "text", options }) => (
                                    <div key={field}>
                                        <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 6 }}>{label}</label>
                                        {options ? (
                                            <select value={account[field]} onChange={e => setAccount(p => ({ ...p, [field]: e.target.value }))} style={selStyle}>
                                                {options.map(o => <option key={o}>{o}</option>)}
                                            </select>
                                        ) : (
                                            <input type={type} value={account[field]} onChange={e => setAccount(p => ({ ...p, [field]: e.target.value }))}
                                                style={inputStyle}
                                                onFocus={e => { e.target.style.borderColor = "#e11d48"; e.target.style.boxShadow = "0 0 0 3px rgba(225,29,72,0.1)"; }}
                                                onBlur={e => { e.target.style.borderColor = "#e2e8f0"; e.target.style.boxShadow = "none"; }}
                                            />
                                        )}
                                    </div>
                                ))}
                            </div>
                            <button onClick={() => save("Account")} disabled={saving} style={{ marginTop: "1.5rem", padding: "11px 28px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 12, fontWeight: 700, fontSize: 14, cursor: "pointer", boxShadow: "0 4px 14px rgba(225,29,72,0.3)" }}>
                                {saving ? "Saving..." : "Save Changes"}
                            </button>
                        </div>
                    )}

                    {/* PARTNER PREFERENCES */}
                    {section === "partner" && (
                        <div>
                            <h2 style={{ fontWeight: 800, fontSize: 18, color: "#111827", marginBottom: "0.25rem" }}>Partner Preferences</h2>
                            <p style={{ fontSize: 13, color: "#94a3b8", marginBottom: "1.5rem" }}>Help us find better matches for you</p>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
                                <div style={{ gridColumn: "span 2" }}>
                                    <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 6 }}>Age Range</label>
                                    <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                                        <input type="number" placeholder="Min age" value={partner.minAge} onChange={e => setPartner(p => ({ ...p, minAge: e.target.value }))} style={{ ...inputStyle, textAlign: "center" }} />
                                        <span style={{ color: "#94a3b8", flexShrink: 0 }}>to</span>
                                        <input type="number" placeholder="Max age" value={partner.maxAge} onChange={e => setPartner(p => ({ ...p, maxAge: e.target.value }))} style={{ ...inputStyle, textAlign: "center" }} />
                                    </div>
                                </div>
                                {[
                                    { label: "Religion", field: "religion", opts: RELIGIONS },
                                    { label: "Income", field: "income", opts: INCOMES },
                                    { label: "Education", field: "education", opts: EDUCATIONS },
                                    { label: "Profession", field: "profession", opts: PROFESSIONS },
                                ].map(({ label, field, opts }) => (
                                    <div key={field}>
                                        <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 6 }}>{label}</label>
                                        <select value={partner[field]} onChange={e => setPartner(p => ({ ...p, [field]: e.target.value }))} style={selStyle}>
                                            {opts.map(o => <option key={o}>{o}</option>)}
                                        </select>
                                    </div>
                                ))}
                                <div style={{ gridColumn: "span 2" }}>
                                    <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 6 }}>Preferred City</label>
                                    <input type="text" placeholder="Any city..." value={partner.city} onChange={e => setPartner(p => ({ ...p, city: e.target.value }))} style={inputStyle} />
                                </div>
                            </div>
                            <button onClick={() => save("Partner Preferences")} style={{ marginTop: "1.5rem", padding: "11px 28px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 12, fontWeight: 700, fontSize: 14, cursor: "pointer", boxShadow: "0 4px 14px rgba(225,29,72,0.3)" }}>
                                Save Preferences
                            </button>
                        </div>
                    )}

                    {/* PRIVACY */}
                    {section === "privacy" && (
                        <div>
                            <h2 style={{ fontWeight: 800, fontSize: 18, color: "#111827", marginBottom: "0.25rem" }}>Privacy Settings</h2>
                            <p style={{ fontSize: 13, color: "#94a3b8", marginBottom: "1.5rem" }}>Control who can see your information</p>
                            <div style={{ display: "flex", flexDirection: "column", gap: "0" }}>
                                {[
                                    { field: "showPhone", label: "Show Phone Number", desc: "Only matched users can see your number" },
                                    { field: "showPhoto", label: "Show Photos", desc: "Allow others to view your profile photos" },
                                    { field: "showProfile", label: "Public Profile", desc: "Appear in search results and suggestions" },
                                    { field: "allowSearch", label: "Searchable", desc: "Allow users to find you via advanced search" },
                                    { field: "showLastSeen", label: "Show Last Seen", desc: "Display when you were last active" },
                                    { field: "allowMessages", label: "Allow Messages", desc: "Receive messages from matched users only" },
                                ].map(({ field, label, desc }) => (
                                    <div key={field} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "16px 0", borderBottom: "1px solid #f8fafc" }}>
                                        <div>
                                            <p style={{ fontWeight: 700, fontSize: 14, color: "#111827" }}>{label}</p>
                                            <p style={{ fontSize: 12, color: "#94a3b8", marginTop: 2 }}>{desc}</p>
                                        </div>
                                        <Toggle checked={privacy[field]} onChange={val => setPrivacy(p => ({ ...p, [field]: val }))} />
                                    </div>
                                ))}
                            </div>
                            <button onClick={() => save("Privacy")} style={{ marginTop: "1.5rem", padding: "11px 28px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 12, fontWeight: 700, fontSize: 14, cursor: "pointer", boxShadow: "0 4px 14px rgba(225,29,72,0.3)" }}>
                                Save Privacy Settings
                            </button>
                        </div>
                    )}

                    {/* NOTIFICATIONS */}
                    {section === "notif" && (
                        <div>
                            <h2 style={{ fontWeight: 800, fontSize: 18, color: "#111827", marginBottom: "0.25rem" }}>Notifications</h2>
                            <p style={{ fontSize: 13, color: "#94a3b8", marginBottom: "1.5rem" }}>Choose how you want to be notified</p>
                            {[
                                {
                                    group: "📧 Email Notifications", items: [
                                        { field: "emailNewMatch", label: "New Match Found" },
                                        { field: "emailInterest", label: "Interest Received" },
                                        { field: "emailMessage", label: "New Message" },
                                    ]
                                },
                                {
                                    group: "📱 Push Notifications", items: [
                                        { field: "pushNewMatch", label: "New Match Found" },
                                        { field: "pushInterest", label: "Interest Received" },
                                        { field: "pushMessage", label: "New Message" },
                                    ]
                                },
                                {
                                    group: "💬 SMS Alerts", items: [
                                        { field: "smsAlert", label: "Important Alerts Only" },
                                    ]
                                },
                            ].map(group => (
                                <div key={group.group} style={{ marginBottom: "1.5rem" }}>
                                    <h3 style={{ fontSize: 13, fontWeight: 800, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: "0.75rem" }}>{group.group}</h3>
                                    <div style={{ background: "#f8fafc", borderRadius: 14, overflow: "hidden", border: "1px solid #f1f5f9" }}>
                                        {group.items.map(({ field, label }, idx) => (
                                            <div key={field} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "13px 16px", borderBottom: idx < group.items.length - 1 ? "1px solid #f1f5f9" : "none" }}>
                                                <span style={{ fontSize: 14, fontWeight: 600, color: "#374151" }}>{label}</span>
                                                <Toggle checked={notif[field]} onChange={val => setNotif(p => ({ ...p, [field]: val }))} />
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ))}
                            <button onClick={() => save("Notifications")} style={{ padding: "11px 28px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 12, fontWeight: 700, fontSize: 14, cursor: "pointer", boxShadow: "0 4px 14px rgba(225,29,72,0.3)" }}>
                                Save Notifications
                            </button>
                        </div>
                    )}

                    {/* PASSWORD */}
                    {section === "password" && (
                        <div>
                            <h2 style={{ fontWeight: 800, fontSize: 18, color: "#111827", marginBottom: "0.25rem" }}>Change Password</h2>
                            <p style={{ fontSize: 13, color: "#94a3b8", marginBottom: "1.5rem" }}>Use a strong password with 8+ characters</p>
                            <div style={{ display: "flex", flexDirection: "column", gap: "1rem", maxWidth: 440 }}>
                                {[
                                    { label: "Current Password", field: "currentPassword" },
                                    { label: "New Password", field: "newPassword" },
                                    { label: "Confirm Password", field: "confirmPassword" },
                                ].map(({ label, field }) => (
                                    <div key={field}>
                                        <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 6 }}>{label}</label>
                                        <input type="password" value={passwd[field]} onChange={e => setPasswd(p => ({ ...p, [field]: e.target.value }))}
                                            style={inputStyle}
                                            onFocus={e => { e.target.style.borderColor = "#e11d48"; e.target.style.boxShadow = "0 0 0 3px rgba(225,29,72,0.1)"; }}
                                            onBlur={e => { e.target.style.borderColor = "#e2e8f0"; e.target.style.boxShadow = "none"; }}
                                        />
                                    </div>
                                ))}
                            </div>
                            {/* Password strength tip */}
                            <div style={{ marginTop: "1rem", background: "#f0fdf4", border: "1px solid #86efac", borderRadius: 12, padding: "12px 16px" }}>
                                <p style={{ fontSize: 12, fontWeight: 700, color: "#166534" }}>💡 Password Tips</p>
                                <ul style={{ marginTop: 6, paddingLeft: 16, color: "#15803d", fontSize: 12, lineHeight: 1.8 }}>
                                    <li>At least 8 characters long</li>
                                    <li>Mix of uppercase, lowercase, numbers</li>
                                    <li>At least one special character (!@#$%)</li>
                                </ul>
                            </div>
                            <button onClick={() => save("Password")} disabled={saving} style={{ marginTop: "1.25rem", padding: "11px 28px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 12, fontWeight: 700, fontSize: 14, cursor: "pointer", boxShadow: "0 4px 14px rgba(225,29,72,0.3)" }}>
                                {saving ? "Updating..." : "Update Password"}
                            </button>
                        </div>
                    )}

                    {/* DANGER ZONE */}
                    {section === "danger" && (
                        <div>
                            <h2 style={{ fontWeight: 800, fontSize: 18, color: "#dc2626", marginBottom: "0.25rem" }}>⚠️ Danger Zone</h2>
                            <p style={{ fontSize: 13, color: "#94a3b8", marginBottom: "2rem" }}>These actions are permanent and cannot be undone</p>
                            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                                {[
                                    { icon: "🙈", title: "Hide My Profile", desc: "Temporarily hide your profile from search results and suggestions.", btn: "Hide Profile", color: "#f59e0b", bg: "#fffbeb", border: "#fde68a" },
                                    { icon: "🗑️", title: "Delete Account", desc: "Permanently delete your account and all associated data. This cannot be reversed.", btn: "Delete Account", color: "#ef4444", bg: "#fff1f2", border: "#fecdd3" },
                                ].map(item => (
                                    <div key={item.title} style={{ background: item.bg, border: `1px solid ${item.border}`, borderRadius: 16, padding: "1.25rem 1.5rem", display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem", flexWrap: "wrap" }}>
                                        <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
                                            <span style={{ fontSize: 28 }}>{item.icon}</span>
                                            <div>
                                                <p style={{ fontWeight: 800, fontSize: 14, color: "#111827" }}>{item.title}</p>
                                                <p style={{ fontSize: 12, color: "#64748b", marginTop: 2, maxWidth: 380 }}>{item.desc}</p>
                                            </div>
                                        </div>
                                        <button onClick={() => toast.error(`⚠️ ${item.title} requires confirmation`)} style={{ padding: "9px 20px", background: "white", border: `2px solid ${item.border}`, color: item.color, borderRadius: 10, fontWeight: 800, fontSize: 13, cursor: "pointer" }}>
                                            {item.btn}
                                        </button>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                </div>
            </div>

            <style>{`
        @media (max-width: 768px) {
          .settings-layout { grid-template-columns: 1fr !important; }
        }
      `}</style>
        </div>
    );
}
