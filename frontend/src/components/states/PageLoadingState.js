"use client";

export default function PageLoadingState({
  title = "Loading...",
  description = "Please wait while content is being prepared.",
  compact = false,
}) {
  const padding = compact ? "1rem" : "2rem";

  return (
    <section className="panel listing-stage" style={{ textAlign: "center", padding }}>
      <p className="section-label" style={{ marginBottom: "0.28rem" }}>
        Loading
      </p>
      <h2 style={{ marginTop: 0, marginBottom: "0.4rem" }}>{title}</h2>
      <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.9rem" }}>{description}</p>
    </section>
  );
}
