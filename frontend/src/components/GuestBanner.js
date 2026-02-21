"use client";

import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { usePathname, useRouter } from "next/navigation";

export default function GuestBanner() {
  const { user, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [hidden, setHidden] = useState(false);

  const isAuthPage = ["/login", "/signup", "/forgot-password"].includes(pathname);
  const isDismissed =
    typeof window !== "undefined" &&
    sessionStorage.getItem("guest_banner_dismissed") === "true";

  if (loading || user || hidden || isDismissed || isAuthPage) {
    return null;
  }

  const handleDismiss = () => {
    setHidden(true);
    if (typeof window !== "undefined") {
      sessionStorage.setItem("guest_banner_dismissed", "true");
    }
  };

  return (
    <div
      style={{
        position: "sticky",
        top: 0,
        zIndex: 120,
        background: "linear-gradient(90deg, #111827, #374151)",
        color: "white",
        padding: "10px 16px",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        gap: 12,
        flexWrap: "wrap",
        boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
        fontSize: 13,
        fontWeight: 500,
      }}
    >
      <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
        <strong>Join 5M+ members.</strong> Sign up to view full profiles and contact numbers.
      </span>
      <div style={{ display: "flex", gap: 8 }}>
        <button
          onClick={() => router.push("/signup")}
          style={{
            padding: "6px 14px",
            background: "#e11d48",
            color: "white",
            border: "none",
            borderRadius: 99,
            fontWeight: 700,
            cursor: "pointer",
            fontSize: 12,
          }}
        >
          Sign Up Free
        </button>
        <button
          onClick={() => router.push("/login")}
          style={{
            padding: "6px 14px",
            background: "rgba(255,255,255,0.1)",
            color: "white",
            border: "1px solid rgba(255,255,255,0.2)",
            borderRadius: 99,
            fontWeight: 600,
            cursor: "pointer",
            fontSize: 12,
          }}
        >
          Login
        </button>
      </div>
      <button
        onClick={handleDismiss}
        style={{
          marginLeft: 16,
          background: "none",
          border: "none",
          color: "rgba(255,255,255,0.6)",
          cursor: "pointer",
          fontSize: 16,
          padding: 4,
        }}
      >
        x
      </button>
    </div>
  );
}
