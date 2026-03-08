"use client";

import RouteErrorState from "@/components/RouteErrorState";

export default function MainRouteError({ error, reset }) {
  return (
    <RouteErrorState
      title="Main workspace unavailable"
      description="A dashboard route failed to load. Retry or return to Matches."
      error={error}
      reset={reset}
      homeHref="/matches"
      homeLabel="Back to Matches"
    />
  );
}
