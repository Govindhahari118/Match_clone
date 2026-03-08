"use client";

export default function GlobalError({ reset }) {
  return (
    <html>
      <body
        style={{
          margin: 0,
          minHeight: "100vh",
          display: "grid",
          placeItems: "center",
          padding: "1rem",
          fontFamily: "Manrope, Segoe UI, sans-serif",
          background:
            "radial-gradient(56rem 34rem at -8% -12%, rgba(225,29,72,0.22), transparent 62%), radial-gradient(48rem 28rem at 108% 104%, rgba(37,99,235,0.16), transparent 62%), linear-gradient(170deg, #fdf8fc, #f2e8f1)",
        }}
      >
        <section
          style={{
            width: "min(520px, 100%)",
            borderRadius: 20,
            border: "1px solid rgba(225,29,72,0.2)",
            background: "rgba(255,255,255,0.92)",
            boxShadow: "0 20px 48px rgba(34,14,24,0.16)",
            padding: "1.35rem",
            textAlign: "center",
          }}
        >
          <p style={{ margin: 0, textTransform: "uppercase", letterSpacing: "0.14em", fontSize: 11, fontWeight: 800, color: "#9f1239" }}>
            System Error
          </p>
          <h2 style={{ margin: "0.6rem 0 0", fontSize: 30, lineHeight: 1.02, fontWeight: 800, color: "#0f1a16" }}>
            Something went wrong
          </h2>
          <p style={{ margin: "0.72rem 0 0", color: "#4d605a", lineHeight: 1.55 }}>
            We hit an unexpected rendering issue. You can retry immediately or return to the home page.
          </p>
          <div style={{ marginTop: "1rem", display: "flex", gap: "0.55rem", justifyContent: "center", flexWrap: "wrap" }}>
            <button
              onClick={() => reset()}
              style={{
                minHeight: 44,
                padding: "0.68rem 1.1rem",
                borderRadius: 12,
                border: "none",
                background: "linear-gradient(135deg, #fb3c78, #9f1239)",
                color: "#ffffff",
                fontWeight: 700,
                cursor: "pointer",
              }}
            >
              Try Again
            </button>
            <button
              onClick={() => {
                window.location.href = "/";
              }}
              style={{
                minHeight: 44,
                padding: "0.68rem 1.1rem",
                borderRadius: 12,
                border: "1px solid rgba(225,29,72,0.22)",
                background: "rgba(255,255,255,0.86)",
                color: "#0f1a16",
                fontWeight: 700,
                cursor: "pointer",
              }}
            >
              Go Home
            </button>
          </div>
        </section>
      </body>
    </html>
  );
}

