"use client";

import RouteErrorState from "@/components/RouteErrorState";

export default function GlobalError({ error, reset }) {
  return (
    <div className="page-shell page-pad">
      <div className="container-shell">
        <RouteErrorState
          title="App failed to render"
          description="The app hit an unexpected issue. Retry or return to the home page."
          error={error}
          reset={reset}
          homeHref="/"
          homeLabel="Back to Home"
        />
      </div>
    </div>
  );
}
