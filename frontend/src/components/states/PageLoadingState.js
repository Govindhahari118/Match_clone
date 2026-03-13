"use client";

export default function PageLoadingState({
  title = "Loading...",
  description = "Please wait while content is being prepared.",
  compact = false,
}) {
  return (
    <section className={`panel listing-stage state-panel ${compact ? "state-panel-compact" : ""}`}>
      <p className="section-label state-label">
        Loading
      </p>
      <h2 className="state-title">{title}</h2>
      <p className="state-copy">{description}</p>
    </section>
  );
}
