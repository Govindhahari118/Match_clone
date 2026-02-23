"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import {
  APP_NAV_ITEMS,
  APP_NAV_SECTIONS,
  MOBILE_PRIMARY_NAV,
  isRouteActive,
} from "@/config/navigation";

const PRIMARY_NAV_ROUTES = ["/matches", "/search", "/interests", "/chat", "/shortlists", "/profile"];

export default function MainLayout({ children }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout, loading } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [query, setQuery] = useState("");
  const [drawerOpen, setDrawerOpen] = useState(false);

  const initial = useMemo(() => {
    const source = user?.profile?.firstName || user?.firstName || user?.email || "U";
    return source.charAt(0).toUpperCase();
  }, [user]);

  const isActive = (href) => isRouteActive(pathname, href);

  const onSearchSubmit = (event) => {
    event.preventDefault();
    if (!query.trim()) return;
    router.push(`/search?q=${encodeURIComponent(query.trim())}`);
  };

  return (
    <div className="page-shell" style={{ background: "var(--bg-soft)" }}>
      <div className="container-shell" style={{ paddingTop: "1.08rem" }}>
        <header
          className="panel app-topbar premium-topbar"
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: "0.9rem",
            position: "sticky",
            top: "0.85rem",
            zIndex: 30,
            marginBottom: "1rem",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "0.65rem", minWidth: 0 }}>
            <button
              type="button"
              onClick={() => setDrawerOpen(true)}
              className="menu-trigger button button-secondary"
              style={{ padding: "0.62rem 0.86rem" }}
            >
              Menu
            </button>

            <Link href="/" style={{ display: "flex", alignItems: "center", gap: "0.55rem", textDecoration: "none" }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 10,
                  background: "linear-gradient(135deg, var(--brand), var(--brand-deep))",
                  display: "grid",
                  placeItems: "center",
                  color: "white",
                  fontWeight: 800,
                  fontSize: 13,
                }}
              >
                M
              </div>
              <strong style={{ fontSize: "0.99rem", letterSpacing: "0.005em", fontWeight: 780 }}>
                MatrimonyConnect
              </strong>
            </Link>
          </div>

          <form onSubmit={onSearchSubmit} className="topbar-search" style={{ flex: 1, maxWidth: 620 }}>
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="form-input"
              placeholder="Search by ID, name, or profile keyword"
              aria-label="Global search"
            />
          </form>

          <div className="topbar-actions" style={{ display: "flex", alignItems: "center", gap: "0.48rem" }}>
            {user && (
              <Link className="button button-primary hide-mobile-sm" href="/pricing">
                Upgrade
              </Link>
            )}

            <Link className="button button-secondary hide-mobile-xs" href="/notifications">
              Alerts
            </Link>

            <button
              type="button"
              className="button button-secondary"
              onClick={toggleTheme}
              aria-label="Toggle theme"
              style={{ padding: "0.62rem 0.78rem", minWidth: 64, fontWeight: 700 }}
              title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
            >
              {theme === "dark" ? "Dark" : "Light"}
            </button>

            {loading ? (
              <span className="button button-secondary" style={{ opacity: 0.7, cursor: "default" }}>
                Account
              </span>
            ) : user ? (
              <>
                <Link
                  href="/profile"
                  className="button button-secondary"
                  style={{ width: 38, height: 38, padding: 0, borderRadius: "50%", fontWeight: 800 }}
                >
                  {initial}
                </Link>
                <button type="button" onClick={logout} className="button button-primary">
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link className="button button-secondary" href="/login">
                  Login
                </Link>
                <Link className="button button-primary" href="/step-1">
                  Sign Up
                </Link>
              </>
            )}
          </div>
        </header>

        <nav className="panel context-quick-nav premium-quick-nav quick-strip-shell" aria-label="Quick navigation">
          <div className="quick-strip-head">
            <p className="section-label">Quick Navigation</p>
            <button type="button" className="button button-secondary" onClick={() => setDrawerOpen(true)}>
              All Pages
            </button>
          </div>

          <div className="quick-strip-row">
            {APP_NAV_ITEMS.filter((item) => PRIMARY_NAV_ROUTES.includes(item.href)).map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={`button button-secondary quick-strip-link ${
                  isActive(item.href) ? "quick-strip-link-active" : ""
                }`}
              >
                {item.label}
              </Link>
            ))}
          </div>
        </nav>

        <div className="main-shell-grid" style={{ paddingBottom: "5.2rem" }}>
          <main className="main-content-shell" style={{ minWidth: 0 }}>
            {children}
          </main>
        </div>
      </div>

      <nav
        className="mobile-bottom-nav"
        style={{
          position: "fixed",
          left: "50%",
          transform: "translateX(-50%)",
          bottom: "0.7rem",
          width: "min(700px, calc(100% - 1rem))",
          zIndex: 60,
          background: "var(--bg-elevated)",
          backdropFilter: "blur(10px)",
          border: "1px solid var(--line)",
          borderRadius: 14,
          boxShadow: "var(--shadow-md)",
          padding: "0.38rem",
          display: "grid",
          gridTemplateColumns: "repeat(6, minmax(0, 1fr))",
          gap: "0.32rem",
        }}
      >
        {APP_NAV_ITEMS.filter((item) => MOBILE_PRIMARY_NAV.includes(item.href)).map((item) => (
          <Link
            key={item.href}
            href={item.href}
            style={{
              borderRadius: 10,
              textAlign: "center",
              textDecoration: "none",
              fontSize: "0.74rem",
              fontWeight: 700,
              padding: "0.5rem 0.3rem",
              color: isActive(item.href) ? "var(--ink)" : "var(--ink-muted)",
              background: isActive(item.href) ? "rgba(29, 78, 216, 0.13)" : "transparent",
            }}
          >
            {item.label}
          </Link>
        ))}

        <button
          type="button"
          onClick={() => setDrawerOpen(true)}
          style={{
            borderRadius: 10,
            textAlign: "center",
            fontSize: "0.74rem",
            fontWeight: 700,
            padding: "0.5rem 0.3rem",
            color: drawerOpen ? "var(--ink)" : "var(--ink-muted)",
            background: drawerOpen ? "rgba(29, 78, 216, 0.13)" : "transparent",
            border: "none",
            cursor: "pointer",
          }}
        >
          Menu
        </button>
      </nav>

      {drawerOpen && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 80,
            background: "rgba(5, 12, 22, 0.48)",
            backdropFilter: "blur(2px)",
            display: "flex",
          }}
          onClick={() => setDrawerOpen(false)}
        >
          <aside
            className="panel app-sidebar"
            style={{
              width: "min(420px, calc(100% - 2.2rem))",
              margin: "0.8rem",
              borderRadius: 18,
              padding: "0.95rem",
              overflowY: "auto",
            }}
            onClick={(event) => event.stopPropagation()}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.75rem" }}>
              <strong style={{ fontSize: "1rem" }}>All Pages</strong>
              <button type="button" className="button button-secondary" onClick={() => setDrawerOpen(false)}>
                Close
              </button>
            </div>

            <div className="drawer-menu-groups">
              {APP_NAV_SECTIONS.map((section) => (
                <section className="drawer-menu-section" key={section.title}>
                  <p className="drawer-menu-title">{section.title}</p>
                  <nav className="drawer-menu-grid">
                    {section.items.map((item) => (
                      <Link
                        key={item.href}
                        href={item.href}
                        className={`drawer-menu-link ${isActive(item.href) ? "drawer-menu-link-active" : ""}`}
                        onClick={() => setDrawerOpen(false)}
                      >
                        <span className="drawer-menu-code">{item.short}</span>
                        <strong>{item.label}</strong>
                      </Link>
                    ))}
                  </nav>
                </section>
              ))}
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
