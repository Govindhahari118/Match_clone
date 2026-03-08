"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { PUBLIC_SHORTCUTS, isRouteActive } from "@/config/navigation";

export default function PublicTopNav({ compact = false, showBack }) {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const shouldShowBack = typeof showBack === "boolean" ? showBack : true;

  const onBack = () => {
    if (typeof window !== "undefined" && window.history.length > 1) {
      router.back();
      return;
    }
    router.push("/");
  };

  return (
    <header className={`panel public-nav-v2 ${compact ? "public-nav-v2-compact" : ""}`}>
      <div className="public-nav-main">
        {shouldShowBack && (
          <button type="button" className="button button-secondary public-nav-icon-btn" onClick={onBack} aria-label="Go back">
            <span aria-hidden="true">&lt;</span>
          </button>
        )}

        <Link href="/" className="public-brand-v2" aria-label="Go to home" onClick={() => setMenuOpen(false)}>
          <span className="public-brand-mark-v2">MC</span>
          <span className="public-brand-copy-v2">
            <strong>MatrimonyConnect</strong>
            <small>Verified matchmaking platform</small>
          </span>
        </Link>

        <button
          type="button"
          className={`button button-secondary public-nav-toggle ${menuOpen ? "is-open" : ""}`}
          onClick={() => setMenuOpen((previous) => !previous)}
          aria-expanded={menuOpen}
          aria-controls="public-nav-links"
        >
          <span className="public-nav-toggle-icon" aria-hidden="true">
            <span />
            <span />
            <span />
          </span>
          <span>{menuOpen ? "Close" : "Menu"}</span>
        </button>

        <nav
          id="public-nav-links"
          className={`public-nav-links-v2 ${menuOpen ? "open" : ""}`}
          aria-label="Public navigation"
        >
          {PUBLIC_SHORTCUTS.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`public-nav-link-v2 ${isRouteActive(pathname, item.href) ? "public-nav-link-v2-active" : ""}`}
              onClick={() => setMenuOpen(false)}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className={`public-nav-actions-v2 ${menuOpen ? "open" : ""}`}>
          <Link href="/login" className="button button-secondary" onClick={() => setMenuOpen(false)}>
            Login
          </Link>
          <Link href="/step-1" className="button button-primary" onClick={() => setMenuOpen(false)}>
            Start Free
          </Link>
        </div>
      </div>
    </header>
  );
}
