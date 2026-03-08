"use client";

import Link from "next/link";

export default function RouteErrorState({
  title = "Something went wrong",
  description = "An unexpected error occurred while loading this page.",
  error,
  reset,
  homeHref = "/",
  homeLabel = "Go Home",
}) {
  return (
    <div className="panel" style={{ padding: "1.2rem", display: "grid", gap: "0.7rem" }}>
      <p className="section-label">Error</p>
      <h1 style={{ margin: 0, fontSize: "1.5rem", fontWeight: 800 }}>{title}</h1>
      <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.9rem" }}>{description}</p>

      {error?.message && (
        <p
          style={{
            margin: 0,
            padding: "0.65rem 0.75rem",
            borderRadius: 10,
            background: "rgba(220, 38, 38, 0.08)",
            color: "#991b1b",
            fontSize: "0.82rem",
          }}
        >
          {error.message}
        </p>
      )}

      <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
        <button type="button" onClick={() => reset?.()} className="button button-primary">
          Try Again
        </button>
        <Link href={homeHref} className="button button-secondary">
          {homeLabel}
        </Link>
      </div>
    </div>
  );
}
