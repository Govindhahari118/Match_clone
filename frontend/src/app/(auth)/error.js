"use client";

import RouteErrorState from "@/components/RouteErrorState";

export default function AuthError({ error, reset }) {
  return (
    <RouteErrorState
      title="Authentication screen failed"
      description="The login or verification screen could not be loaded."
      error={error}
      reset={reset}
      homeHref="/login"
      homeLabel="Back to Login"
    />
  );
}
