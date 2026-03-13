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
    <section className="panel listing-stage state-panel">
      <p className="section-label state-label">
        Empty State
      </p>
      <h2 className="state-title">{title}</h2>
      <p className="state-copy">{description}</p>

      {(primaryActionLabel || secondaryActionLabel) && (
        <div className="state-actions">
          {primaryActionLabel &&
            (primaryActionHref ? (
              <Link href={primaryActionHref} className="button button-primary cta-full">
                {primaryActionLabel}
              </Link>
            ) : (
              <button type="button" className="button button-primary cta-full" onClick={onPrimaryAction}>
                {primaryActionLabel}
              </button>
            ))}

          {secondaryActionLabel &&
            (secondaryActionHref ? (
              <Link href={secondaryActionHref} className="button button-secondary cta-full">
                {secondaryActionLabel}
              </Link>
            ) : (
              <button type="button" className="button button-secondary cta-full" onClick={onSecondaryAction}>
                {secondaryActionLabel}
              </button>
            ))}
        </div>
      )}
    </section>
  );
}
