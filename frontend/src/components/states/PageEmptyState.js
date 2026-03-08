"use client";

import Link from "next/link";

export default function PageEmptyState({
  title = "Nothing here yet",
  description = "Content will appear here when available.",
  primaryActionLabel = "",
  primaryActionHref = "",
  onPrimaryAction,
  secondaryActionLabel = "",
  secondaryActionHref = "",
  onSecondaryAction,
}) {
  return (
    <section className="panel listing-stage" style={{ textAlign: "center", padding: "2.2rem" }}>
      <p className="section-label" style={{ marginBottom: "0.24rem" }}>
        Empty State
      </p>
      <h2 style={{ marginTop: 0, marginBottom: "0.38rem" }}>{title}</h2>
      <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.9rem" }}>{description}</p>

      {(primaryActionLabel || secondaryActionLabel) && (
        <div style={{ marginTop: "0.9rem", display: "inline-flex", gap: "0.5rem", flexWrap: "wrap", justifyContent: "center" }}>
          {primaryActionLabel &&
            (primaryActionHref ? (
              <Link href={primaryActionHref} className="button button-primary">
                {primaryActionLabel}
              </Link>
            ) : (
              <button type="button" className="button button-primary" onClick={onPrimaryAction}>
                {primaryActionLabel}
              </button>
            ))}

          {secondaryActionLabel &&
            (secondaryActionHref ? (
              <Link href={secondaryActionHref} className="button button-secondary">
                {secondaryActionLabel}
              </Link>
            ) : (
              <button type="button" className="button button-secondary" onClick={onSecondaryAction}>
                {secondaryActionLabel}
              </button>
            ))}
        </div>
      )}
    </section>
  );
}
