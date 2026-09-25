"use client";

import Link from "next/link";

export default function SuccessStoriesPage() {
  return (
    <div style={{ display: "grid", gap: "1rem" }}>
      <section
        className="panel"
        style={{
          padding: "1.4rem",
          background: "linear-gradient(135deg, #172554, #1d4ed8, #0f766e)",
          color: "white",
          borderColor: "rgba(255,255,255,0.18)",
        }}
      >
        <p className="section-label" style={{ color: "rgba(255,255,255,0.78)" }}>Community Stories</p>
        <h1 className="section-title" style={{ fontSize: "clamp(1.9rem,4vw,2.7rem)", marginTop: "0.5rem" }}>
          Success Stories
        </h1>
        <p style={{ margin: "0.75rem 0 0", maxWidth: 680, color: "rgba(255,255,255,0.9)" }}>
          Only moderator-approved stories tied to genuine submitted records will be published here.
        </p>
      </section>

      <section className="panel" style={{ padding: "2.2rem", textAlign: "center" }}>
        <h2 style={{ marginTop: 0 }}>No approved stories published yet</h2>
        <p style={{ color: "var(--ink-muted)", maxWidth: 620, margin: "0 auto" }}>
          We are not showing sample couples, stock people, fabricated marriage counts, or made-up success statistics.
        </p>
        <Link href="/matches" className="button button-primary" style={{ marginTop: "1rem" }}>
          Browse Real Profiles
        </Link>
      </section>
    </div>
  );
}
