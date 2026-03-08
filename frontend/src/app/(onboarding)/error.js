"use client";

import RouteErrorState from "@/components/RouteErrorState";

export default function OnboardingError({ error, reset }) {
  return (
    <RouteErrorState
      title="Onboarding step failed"
      description="The current onboarding step could not be rendered."
      error={error}
      reset={reset}
      homeHref="/step-1"
      homeLabel="Restart Onboarding"
    />
  );
}
