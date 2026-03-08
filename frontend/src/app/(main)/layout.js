"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import {
  APP_NAV_ITEMS,
  APP_NAV_SECTIONS,
  MOBILE_PRIMARY_NAV,
  isRouteActive,
} from "@/config/navigation";

const THEME_OPTIONS = [
  { value: "glacier", label: "Glacier" },
  { value: "light", label: "Light" },
  { value: "dark", label: "Dark" },
  { value: "lavender", label: "Lavender" },
  { value: "solar", label: "Solar" },
  { value: "rose", label: "Rose" },
];
const VIEWPORT_OPTIONS = [
  { value: "desktop", label: "Desktop" },
  { value: "tablet", label: "Tablet" },
  { value: "mobile", label: "Mobile" },
];
const VIEWPORT_STORAGE_KEY = "workspaceViewport";
const RAIL_STORAGE_KEY = "workspaceRailOpen";
const RAIL_PRIMARY_ROUTES = ["/", "/matches", "/interests", "/shortlists", "/chat", "/notifications", "/profile"];

function UtilityIcon({ name }) {
  if (name === "sun") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="4.5" />
        <path d="M12 2.5V5.2M12 18.8v2.7M4.9 4.9 6.8 6.8M17.2 17.2l1.9 1.9M2.5 12h2.7M18.8 12h2.7M4.9 19.1l1.9-1.9M17.2 6.8l1.9-1.9" />
      </svg>
    );
  }

  if (name === "moon") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M20.5 14.4A8.5 8.5 0 1 1 9.6 3.5a7 7 0 0 0 10.9 10.9Z" />
      </svg>
    );
  }

  if (name === "bell") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M6.8 9.5a5.2 5.2 0 0 1 10.4 0V13l1.6 3.1a1 1 0 0 1-.9 1.4H6.1a1 1 0 0 1-.9-1.4L6.8 13V9.5Z" />
        <path d="M10 18.3a2.2 2.2 0 0 0 4 0" />
      </svg>
    );
  }

  if (name === "user") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="8" r="3.2" />
        <path d="M5.4 19.2a6.6 6.6 0 0 1 13.2 0" />
      </svg>
    );
  }

  if (name === "plus") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M12 5.2v13.6M5.2 12h13.6" />
      </svg>
    );
  }

  if (name === "star") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="m12 3.8 2.6 5.3 5.9.9-4.3 4.2 1 5.9-5.2-2.8-5.2 2.8 1-5.9-4.3-4.2 5.9-.9L12 3.8Z" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M4 7h16M4 12h16M4 17h16" />
    </svg>
  );
}

export default function MainLayout({ children }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout, loading } = useAuth();
  const { theme, setTheme } = useTheme();
  const [query, setQuery] = useState("");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [viewportMode, setViewportMode] = useState("desktop");
  const [railOpen, setRailOpen] = useState(true);
  const [railPage, setRailPage] = useState(() =>
    RAIL_PRIMARY_ROUTES.some((href) => isRouteActive(pathname, href)) ? 0 : 1
  );

  const initial = useMemo(() => {
    const source = user?.profile?.firstName || user?.firstName || user?.email || "U";
    return source.charAt(0).toUpperCase();
  }, [user]);

  const activeNavItem = useMemo(
    () => APP_NAV_ITEMS.find((item) => isRouteActive(pathname, item.href)) || APP_NAV_ITEMS[0],
    [pathname]
  );
  const primaryRailItems = useMemo(
    () => APP_NAV_ITEMS.filter((item) => RAIL_PRIMARY_ROUTES.includes(item.href)),
    []
  );
  const secondaryRailItems = useMemo(
    () => APP_NAV_ITEMS.filter((item) => !RAIL_PRIMARY_ROUTES.includes(item.href)),
    []
  );

  const isActive = (href) => isRouteActive(pathname, href);

  useEffect(() => {
    if (typeof window === "undefined") return;

    const frame = window.requestAnimationFrame(() => {
      try {
        const storedViewport = window.localStorage.getItem(VIEWPORT_STORAGE_KEY);
        if (VIEWPORT_OPTIONS.some((option) => option.value === storedViewport)) {
          setViewportMode(storedViewport);
        }
      } catch {}

      try {
        setRailOpen(window.localStorage.getItem(RAIL_STORAGE_KEY) !== "0");
      } catch {}
    });

    return () => window.cancelAnimationFrame(frame);
  }, []);

  const onSearchSubmit = (event) => {
    event.preventDefault();
    const normalized = query.trim();
    if (!normalized) return;
    router.push(`/matches?q=${encodeURIComponent(normalized)}`);
  };

  const onViewportChange = (nextMode) => {
    if (!VIEWPORT_OPTIONS.some((option) => option.value === nextMode)) return;
    setViewportMode(nextMode);
    setDrawerOpen(false);
    try {
      window.localStorage.setItem(VIEWPORT_STORAGE_KEY, nextMode);
    } catch {}
  };

  const toggleRail = () => {
    setDrawerOpen(false);
    setRailOpen((previous) => {
      const next = !previous;
      try {
        window.localStorage.setItem(RAIL_STORAGE_KEY, next ? "1" : "0");
      } catch {}
      return next;
    });
  };

  const handleMainMenuToggle = () => {
    if (viewportMode === "desktop") {
      toggleRail();
      return;
    }
    setDrawerOpen((previous) => !previous);
  };

  const menuExpanded = viewportMode === "desktop" ? railOpen : drawerOpen;
  const menuControlTarget = viewportMode === "desktop" ? "app-left-rail" : "app-quick-nav-sheet";
  const menuActionLabel =
    viewportMode === "desktop"
      ? railOpen
        ? "Collapse sidebar"
        : "Expand sidebar"
      : drawerOpen
        ? "Close quick navigation"
        : "Open quick navigation";
  const visibleRailItems = railPage === 0 ? primaryRailItems : secondaryRailItems;
  const railPageTitle = railPage === 0 ? "Primary" : "More";
  const canGoBack = true;

  const handleBack = () => {
    if (typeof window !== "undefined" && window.history.length > 1) {
      router.back();
      return;
    }
    router.push("/");
  };

  return (
    <div className={`app-shell-v2 viewport-${viewportMode} ${railOpen ? "rail-expanded" : "rail-collapsed"}`}>
      <aside id="app-left-rail" className="panel app-rail" aria-label="Application navigation">
        <Link href="/" className="app-brand app-brand-sidebar" aria-label="MatrimonyConnect home">
          <span className="app-brand-mark">MC</span>
          <span className="app-brand-copy">
            <strong>MatrimonyConnect</strong>
            <small>Relationship Workspace</small>
          </span>
        </Link>

        <div className="app-rail-scroll">
          <section className="app-rail-section">
            <p className="app-rail-title">{railPageTitle}</p>
            <div className="app-rail-links">
              {visibleRailItems.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`app-rail-link ${isActive(item.href) ? "app-rail-link-active" : ""}`}
                >
                  <span className="app-rail-link-code">{item.short}</span>
                  <span className="app-rail-link-body">
                    <strong>{item.label}</strong>
                    <small>{item.blurb}</small>
                  </span>
                </Link>
              ))}
            </div>
          </section>
        </div>

        {secondaryRailItems.length > 0 && (
          <div className="app-rail-pager">
            <button
              type="button"
              className="button button-secondary app-rail-page-btn"
              onClick={() => setRailPage((previous) => (previous === 0 ? 1 : 0))}
              aria-label={railPage === 0 ? "Open more pages" : "Back to primary pages"}
            >
              <span className="app-rail-page-btn-arrow" aria-hidden="true">
                {railPage === 0 ? ">" : "<"}
              </span>
              <span>{railPage === 0 ? "More pages" : "Back to primary"}</span>
            </button>
          </div>
        )}

        <div className="app-rail-footer">
          {user ? (
            <button type="button" className="button button-primary" onClick={logout}>
              Logout
            </button>
          ) : (
            <Link href="/step-1" className="button button-primary">
              Create Account
            </Link>
          )}
        </div>
      </aside>

      <div className="app-workspace">
        <header className="panel app-utility-bar">
          <div className="app-utility-left">
            {canGoBack && (
              <button
                type="button"
                className="button button-secondary app-icon-btn app-back-btn"
                onClick={handleBack}
                aria-label="Go back"
                title="Go back"
              >
                <span aria-hidden="true">&lt;</span>
              </button>
            )}

            <Link href="/" className="app-brand app-brand-mobile" aria-label="MatrimonyConnect home">
              <span className="app-brand-mark">MC</span>
              <span className="app-brand-copy">
                <strong>MatrimonyConnect</strong>
                <small>{activeNavItem?.section || "Workspace"}</small>
              </span>
            </Link>

            <div className="app-context hide-mobile-sm">
              <p className="app-context-eyebrow">{activeNavItem?.section || "Workspace"}</p>
              <p className="app-context-title">{activeNavItem?.label || "Overview"}</p>
            </div>
          </div>

          <form onSubmit={onSearchSubmit} className="app-spotlight-form" role="search" aria-label="Search profiles">
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="form-input"
              placeholder="Search profile ID, name, or city"
              aria-label="Global profile search"
            />
          </form>

          <div className="app-utility-actions">
            <label className="app-hidden-label" htmlFor="app-theme-select-top">
              Choose interface theme
            </label>
            <select
              id="app-theme-select-top"
              className="form-input app-compact-select app-theme-select-top"
              value={theme}
              onChange={(event) => setTheme(event.target.value)}
              aria-label="Choose interface theme"
              title="Theme"
            >
              {THEME_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            <label className="app-hidden-label" htmlFor="app-viewport-select">
              Preview viewport
            </label>
            <select
              id="app-viewport-select"
              className="form-input app-compact-select app-viewport-select"
              value={viewportMode}
              onChange={(event) => onViewportChange(event.target.value)}
              aria-label="Preview viewport"
              title="Preview viewport"
            >
              {VIEWPORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            {user && (
              <Link className="button button-secondary app-icon-btn hide-mobile-xs" href="/notifications" aria-label="Notifications" title="Notifications">
                <UtilityIcon name="bell" />
              </Link>
            )}

            {user && (
              <Link className="button button-primary app-icon-btn hide-mobile-sm" href="/pricing" aria-label="Upgrade plan" title="Upgrade">
                <UtilityIcon name="star" />
              </Link>
            )}

            {loading ? (
              <span className="button button-secondary app-icon-btn" style={{ opacity: 0.7, cursor: "default" }} aria-hidden="true">
                <UtilityIcon name="user" />
              </span>
            ) : user ? (
              <Link href="/profile" className="button button-secondary app-avatar-button" aria-label="Open profile">
                {initial}
              </Link>
            ) : (
              <Link className="button button-secondary app-icon-btn" href="/login" aria-label="Login" title="Login">
                <UtilityIcon name="user" />
              </Link>
            )}

            <button
              type="button"
              className={`button button-secondary app-hamburger-btn ${menuExpanded ? "is-open" : ""}`}
              onClick={handleMainMenuToggle}
              aria-expanded={menuExpanded}
              aria-controls={menuControlTarget}
              title={menuActionLabel}
              aria-label={menuActionLabel}
            >
              <span className="app-hamburger-icon" aria-hidden="true">
                <span />
                <span />
                <span />
              </span>
              <span>{menuActionLabel}</span>
            </button>
          </div>
        </header>

        <main className="app-content-stage">{children}</main>
      </div>

      <nav className="panel mobile-dock" aria-label="Mobile navigation">
        {APP_NAV_ITEMS.filter((item) => MOBILE_PRIMARY_NAV.includes(item.href)).map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={`mobile-dock-link ${isActive(item.href) ? "mobile-dock-link-active" : ""}`}
          >
            <span>{item.short}</span>
            <strong>{item.label}</strong>
          </Link>
        ))}

        <button
          type="button"
          onClick={() => setDrawerOpen(true)}
          className={`mobile-dock-link ${drawerOpen ? "mobile-dock-link-active" : ""}`}
          aria-expanded={drawerOpen}
          aria-controls="app-quick-nav-sheet"
        >
          <span>{drawerOpen ? "CL" : "MN"}</span>
          <strong>{drawerOpen ? "Close" : "Menu"}</strong>
        </button>
      </nav>

      {drawerOpen && (
        <div className="app-sheet-backdrop" onClick={() => setDrawerOpen(false)}>
          <aside
            id="app-quick-nav-sheet"
            className="panel app-sheet"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="app-sheet-head">
              <p className="section-label">All Routes</p>
              <button type="button" className="button button-secondary" onClick={() => setDrawerOpen(false)}>
                Close
              </button>
            </div>

            <div className="app-sheet-controls">
              <div className="app-viewport-switcher app-viewport-switcher-sheet" role="group" aria-label="Preview viewport layout">
                {VIEWPORT_OPTIONS.map((option) => (
                  <button
                    key={`sheet-${option.value}`}
                    type="button"
                    className={`button button-secondary app-viewport-btn ${
                      viewportMode === option.value ? "app-viewport-btn-active" : ""
                    }`}
                    onClick={() => onViewportChange(option.value)}
                    aria-pressed={viewportMode === option.value}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="app-sheet-groups">
              {APP_NAV_SECTIONS.map((section) => (
                <section key={section.title} className="app-sheet-group">
                  <p className="app-sheet-title">{section.title}</p>
                  <div className="app-sheet-grid">
                    {section.items.map((item) => (
                      <Link
                        key={item.href}
                        href={item.href}
                        className={`app-sheet-link ${isActive(item.href) ? "app-sheet-link-active" : ""}`}
                        onClick={() => setDrawerOpen(false)}
                      >
                        <span className="app-sheet-code">{item.short}</span>
                        <span className="app-sheet-copy">
                          <strong>{item.label}</strong>
                          <small>{item.blurb}</small>
                        </span>
                      </Link>
                    ))}
                  </div>
                </section>
              ))}
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
