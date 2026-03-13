"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { PUBLIC_SHORTCUTS, isRouteActive } from "@/config/navigation";

export default function PublicTopNav({ compact = false, showBack }) {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const shouldShowBack = typeof showBack === "boolean" ? showBack : true;

  const PRIMARY_COUNT = 5;
  const primaryLinks = useMemo(() => PUBLIC_SHORTCUTS.slice(0, PRIMARY_COUNT), []);
  const moreLinks = useMemo(() => PUBLIC_SHORTCUTS.slice(PRIMARY_COUNT), []);
  const isMoreActive = useMemo(
    () => moreLinks.some((item) => isRouteActive(pathname, item.href)),
    [moreLinks, pathname]
  );

  useEffect(() => {
    if (!menuOpen) {
      setMoreOpen(false);
    }
  }, [menuOpen]);

  const onBack = () => {
    if (typeof window !== "undefined" && window.history.length > 1) {
      router.back();
      return;
    }
    router.push("/");
  };

  const closeMenus = () => {
    setMenuOpen(false);
    setMoreOpen(false);
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
            <small>Privacy-first matchmaking platform</small>
          </span>
        </Link>

        <button
          type="button"
          className={`button button-secondary public-nav-toggle ${menuOpen ? "is-open" : ""}`}
          onClick={() => setMenuOpen((previous) => !previous)}
          aria-expanded={menuOpen}
          aria-controls="public-nav-links public-nav-actions"
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
          {primaryLinks.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`public-nav-link-v2 ${isRouteActive(pathname, item.href) ? "public-nav-link-v2-active" : ""}`}
              onClick={closeMenus}
            >
              {item.label}
            </Link>
          ))}
          {moreLinks.length > 0 && (
            <div className={`public-nav-more ${moreOpen ? "open" : ""}`}>
              <button
                type="button"
                className={`public-nav-link-v2 public-nav-more-btn ${isMoreActive ? "public-nav-link-v2-active" : ""}`}
                aria-haspopup="menu"
                aria-expanded={moreOpen}
                aria-controls="public-nav-more-menu"
                onClick={() => setMoreOpen((previous) => !previous)}
              >
                More
              </button>
              <div id="public-nav-more-menu" className="public-nav-more-menu" role="menu">
                {moreLinks.map((item) => (
                  <Link
                    key={item.href}
                    href={item.href}
                    role="menuitem"
                    className={`public-nav-more-link ${isRouteActive(pathname, item.href) ? "active" : ""}`}
                    onClick={closeMenus}
                  >
                    {item.label}
                  </Link>
                ))}
              </div>
            </div>
          )}
        </nav>

        <div id="public-nav-actions" className={`public-nav-actions-v2 ${menuOpen ? "open" : ""}`}>
          <Link href="/login" className="button button-secondary" onClick={closeMenus}>
            Login
          </Link>
          <Link href="/step-1" className="button button-primary" onClick={closeMenus}>
            Start Free
          </Link>
        </div>
      </div>
    </header>
  );
}
