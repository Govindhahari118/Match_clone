"use client";

import { useRouter } from "next/navigation";

export default function LoginPromptModal({
  isOpen,
  onClose,
  triggerText = "Sign in to continue",
}) {
  const router = useRouter();

  if (!isOpen) return null;

  return (
    <div
      role="presentation"
      onClick={onClose}
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 9999,
        background: "rgba(0,0,0,0.56)",
        backdropFilter: "blur(4px)",
        display: "grid",
        placeItems: "center",
        padding: "1rem",
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Login required"
        onClick={(event) => event.stopPropagation()}
        className="panel"
        style={{
          width: "100%",
          maxWidth: 420,
          borderRadius: 18,
          padding: "1.2rem",
          textAlign: "center",
          animation: "riseIn 0.24s ease both",
        }}
      >
        <div
          aria-hidden="true"
          style={{
            width: 52,
            height: 52,
            borderRadius: 14,
            margin: "0 auto 0.7rem",
            display: "grid",
            placeItems: "center",
            color: "#9f1239",
            fontWeight: 800,
            border: "1px solid rgba(227,68,117,0.28)",
            background: "rgba(255,235,244,0.84)",
            fontSize: "1.1rem",
          }}
        >
          LK
        </div>

        <h2 style={{ margin: 0, fontSize: "1.2rem", lineHeight: 1.2 }}>{triggerText}</h2>
        <p style={{ margin: "0.5rem 0 0", color: "var(--ink-muted)", fontSize: "0.9rem", lineHeight: 1.5 }}>
          Join 5M+ members to unlock full profile details and start direct conversations.
        </p>

        <div style={{ display: "grid", gap: "0.55rem", marginTop: "1rem" }}>
          <button type="button" onClick={() => router.push("/login")} className="button button-primary" style={{ width: "100%" }}>
            Log In
          </button>
          <button type="button" onClick={() => router.push("/step-1")} className="button button-secondary" style={{ width: "100%" }}>
            Create Free Account
          </button>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="button button-secondary"
          style={{ marginTop: "0.8rem", width: "100%" }}
        >
          Maybe Later
        </button>
      </div>
    </div>
  );
}
