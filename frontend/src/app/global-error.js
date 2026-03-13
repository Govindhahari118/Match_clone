"use client";

export default function GlobalError({ reset }) {
  return (
    <html className="error-html">
      <body className="error-shell">
        <section className="panel error-card">
          <p className="error-kicker">
            System Error
          </p>
          <h2 className="error-title">
            Something went wrong
          </h2>
          <p className="error-copy">
            We hit an unexpected rendering issue. You can retry immediately or return to the home page.
          </p>
          <div className="error-actions">
            <button
              onClick={() => reset()}
              className="button button-primary"
            >
              Try Again
            </button>
            <button
              onClick={() => {
                window.location.href = "/";
              }}
              className="button button-secondary"
            >
              Go Home
            </button>
          </div>
        </section>
      </body>
    </html>
  );
}

