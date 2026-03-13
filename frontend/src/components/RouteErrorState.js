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
    <div className="panel state-panel state-panel-compact">
      <p className="section-label state-label">Error</p>
      <h1 className="state-title state-title-lg">{title}</h1>
      <p className="state-copy">{description}</p>

      {error?.message && (
        <p className="state-callout state-callout-error">
          {error.message}
        </p>
      )}

      <div className="state-actions">
        <button type="button" onClick={() => reset?.()} className="button button-primary cta-full">
          Try Again
        </button>
        <Link href={homeHref} className="button button-secondary cta-full">
          {homeLabel}
        </Link>
      </div>
    </div>
  );
}
