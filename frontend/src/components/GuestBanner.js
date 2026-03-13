"use client";

import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { usePathname, useRouter } from "next/navigation";

export default function GuestBanner() {
  const { user, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [hidden, setHidden] = useState(false);
  const [dismissed, setDismissed] = useState(() => {
    if (typeof window === "undefined") return false;
    try {
      return sessionStorage.getItem("guest_banner_dismissed") === "true";
    } catch {
      return false;
    }
  });

  const isAuthPage = ["/login", "/step-1", "/forgot-password"].includes(pathname);

  if (loading || user || hidden || dismissed || isAuthPage) {
    return null;
  }

  const handleDismiss = () => {
    setHidden(true);
    setDismissed(true);
    try {
      sessionStorage.setItem("guest_banner_dismissed", "true");
    } catch {}
  };

  return (
    <div className="guest-banner">
      <span className="guest-banner-message">
        <strong>Join 5M+ members.</strong> Sign up to view full profiles and connect directly.
      </span>
      <div className="guest-banner-actions">
        <button
          onClick={() => router.push("/step-1")}
          className="button button-primary cta-full"
        >
          Sign Up Free
        </button>
        <button
          onClick={() => router.push("/login")}
          className="button button-ghost-light cta-full"
        >
          Login
        </button>
      </div>
      <button
        onClick={handleDismiss}
        className="guest-banner-dismiss"
        aria-label="Dismiss banner"
      >
        x
      </button>
    </div>
  );
}
