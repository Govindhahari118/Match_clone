"use client";

function titleCase(value) {
  return String(value || "").replace(/_/g, " ").replace(/\b\w/g, (char) => char.toUpperCase());
}

function StatusItem({ label, value, verified = false }) {
  return (
    <div className="panel" style={{ padding: "0.55rem 0.62rem", borderRadius: 10 }}>
      <p style={{ margin: 0, fontSize: "0.7rem", color: "var(--ink-muted)", textTransform: "uppercase", letterSpacing: "0.03em" }}>{label}</p>
      <p style={{ margin: "0.2rem 0 0", fontSize: "0.86rem", fontWeight: 750 }}>
        {verified ? "✓ " : ""}{value || "Not available"}
      </p>
    </div>
  );
}

export default function TrustStatusPanel({ trust, compact = false }) {
  if (!trust) return null;
  const verification = trust.verification || {};
  const rows = [
    ["Phone", titleCase(verification.phone || "not_started"), verification.phone === "verified"],
    ["Photo", titleCase(verification.photo || "not_started"), verification.photo === "verified"],
    ["Identity", titleCase(verification.identity || "unverified"), verification.identity === "verified"],
    ["Activity", trust.activity?.label || "Activity unavailable", false],
    ["Managed by", titleCase(trust.managedBy || "self"), false],
    ["Profile completeness", `${trust.profileCompleteness ?? 0}%`, false],
    ["Marriage search", trust.statusReconfirmationLabel || "Not reconfirmed yet", false],
  ];

  return (
    <section className="panel" style={{ padding: compact ? "0.72rem" : "0.95rem" }}>
      <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between", gap: "0.6rem", marginBottom: "0.6rem" }}>
        <div>
          <p className="section-label" style={{ marginBottom: "0.18rem" }}>Trust & activity</p>
          {!compact && <h2 style={{ margin: 0, fontFamily: "var(--font-display)", fontSize: "1.28rem" }}>What has actually been verified</h2>}
        </div>
        <span className="chip chip-support">{titleCase(trust.searchStatus || "active")}</span>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: compact ? "repeat(2,minmax(0,1fr))" : "repeat(auto-fit,minmax(150px,1fr))", gap: "0.45rem" }}>
        {rows.map(([label, value, verified]) => <StatusItem key={label} label={label} value={value} verified={verified} />)}
      </div>
      {!compact && (
        <p style={{ margin: "0.65rem 0 0", color: "var(--ink-muted)", fontSize: "0.76rem", lineHeight: 1.45 }}>
          Verification confirms only the listed checks. It is not a guarantee of compatibility, conduct, or marriage outcome.
        </p>
      )}
    </section>
  );
}
