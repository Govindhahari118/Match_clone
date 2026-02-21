"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";

const NAV_ITEMS = [
  { href: "/", label: "Home", short: "HM" },
  { href: "/matches", label: "Matches", short: "MT" },
  { href: "/search", label: "Search", short: "SR" },
  { href: "/interests", label: "Interests", short: "IN" },
  { href: "/shortlists", label: "Shortlists", short: "SL" },
  { href: "/chat", label: "Messages", short: "MS" },
  { href: "/who-viewed", label: "Viewed", short: "VW" },
  { href: "/kundli", label: "Kundli", short: "KD" },
  { href: "/profile", label: "Profile", short: "PF" },
  { href: "/settings", label: "Settings", short: "ST" },
  { href: "/notifications", label: "Alerts", short: "AL" },
  { href: "/pricing", label: "Pricing", short: "PR" },
  { href: "/help", label: "Help", short: "HP" },
  { href: "/success-stories", label: "Stories", short: "SS" },
];

const MOBILE_PRIMARY = ["/", "/matches", "/search", "/chat", "/profile"];
const QUICK_NAV_ITEMS = ["/", "/matches", "/search", "/interests", "/shortlists", "/chat", "/profile", "/notifications", "/pricing", "/help"];

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

  const isActive = (href) => {
    if (href === "/") return pathname === "/";
    return pathname.startsWith(href);
  };

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
              <strong style={{ fontSize: "0.99rem", letterSpacing: "0.005em", fontWeight: 780 }}>MatrimonyConnect</strong>
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
                <Link className="button button-primary" href="/signup">
                  Sign Up
                </Link>
              </>
            )}
          </div>
        </header>

        <nav
          className="panel context-quick-nav premium-quick-nav"
          style={{
            marginBottom: "0.82rem",
            padding: "0.5rem",
            display: "flex",
            gap: "0.45rem",
            overflowX: "auto",
            scrollbarWidth: "none",
          }}
        >
          {NAV_ITEMS.filter((item) => QUICK_NAV_ITEMS.includes(item.href)).map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="button button-secondary quick-nav-link"
              style={{
                whiteSpace: "nowrap",
                background: isActive(item.href) ? "rgba(227, 68, 117, 0.15)" : undefined,
                borderColor: isActive(item.href) ? "rgba(227, 68, 117, 0.36)" : undefined,
                color: isActive(item.href) ? "var(--ink)" : undefined,
              }}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div
          className="main-shell-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "220px minmax(0, 1fr)",
            gap: "1rem",
            paddingBottom: "5.2rem",
          }}
        >
          <aside className="panel desktop-nav-shell app-sidebar" style={{ height: "fit-content", padding: "0.9rem", position: "sticky", top: "5.45rem" }}>
            <p className="section-label" style={{ marginBottom: "0.65rem" }}>
              Navigation
            </p>
            <nav style={{ display: "grid", gap: "0.38rem" }}>
              {NAV_ITEMS.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className="nav-link"
                  style={{
                    justifyContent: "space-between",
                    padding: "0.6rem 0.72rem",
                    borderRadius: 12,
                    color: isActive(item.href) ? "var(--ink)" : "var(--ink-muted)",
                    background: isActive(item.href) ? "rgba(29, 78, 216, 0.12)" : "transparent",
                    border: isActive(item.href) ? "1px solid rgba(29, 78, 216, 0.3)" : "1px solid transparent",
                    textDecoration: "none",
                  }}
                >
                  <span>{item.label}</span>
                  <span style={{ fontSize: "0.72rem", opacity: 0.75 }}>{item.short}</span>
                </Link>
              ))}
            </nav>
          </aside>

          <main style={{ minWidth: 0 }}>{children}</main>
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
        {NAV_ITEMS.filter((item) => MOBILE_PRIMARY.includes(item.href)).map((item) => (
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
              width: "min(320px, calc(100% - 2.2rem))",
              margin: "0.8rem",
              borderRadius: 18,
              padding: "0.95rem",
              overflowY: "auto",
            }}
            onClick={(event) => event.stopPropagation()}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.75rem" }}>
              <strong style={{ fontSize: "1rem" }}>Menu</strong>
              <button type="button" className="button button-secondary" onClick={() => setDrawerOpen(false)}>
                Close
              </button>
            </div>

            <nav style={{ display: "grid", gap: "0.42rem" }}>
              {NAV_ITEMS.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setDrawerOpen(false)}
                  style={{
                    textDecoration: "none",
                    borderRadius: 12,
                    border: "1px solid rgba(15, 23, 42, 0.12)",
                    padding: "0.62rem 0.72rem",
                    background: isActive(item.href) ? "rgba(29, 78, 216, 0.12)" : "var(--bg-elevated)",
                    color: "var(--ink)",
                    fontWeight: 700,
                  }}
                >
                  {item.label}
                </Link>
              ))}
            </nav>
          </aside>
        </div>
      )}

      <style jsx>{`
        .premium-topbar {
          border: 1px solid rgba(29, 78, 216, 0.22);
          background: linear-gradient(160deg, rgba(255, 255, 255, 0.97), rgba(245, 249, 255, 0.95));
          box-shadow: 0 16px 34px rgba(15, 23, 42, 0.1);
        }

        .premium-quick-nav {
          border: 1px solid rgba(29, 78, 216, 0.18);
          background: linear-gradient(160deg, rgba(255, 255, 255, 0.96), rgba(247, 250, 255, 0.93));
          box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
        }

        .quick-nav-link {
          padding: 0.62rem 0.92rem;
          font-weight: 700;
          line-height: 1.2;
          min-height: 40px;
        }

        .topbar-search :global(.form-input) {
          background: rgba(255, 255, 255, 0.86);
        }

        .topbar-actions a,
        .topbar-actions button {
          white-space: nowrap;
        }

        @media (max-width: 680px) {
          .hide-mobile-sm {
            display: none !important;
          }
        }

        @media (max-width: 560px) {
          .topbar-search {
            display: none !important;
          }

          .hide-mobile-xs {
            display: none !important;
          }
        }

        @media (max-width: 980px) {
          .main-shell-grid {
            grid-template-columns: 1fr !important;
          }

          .desktop-nav-shell {
            display: none !important;
          }

          .context-quick-nav {
            display: none !important;
          }
        }

        @media (min-width: 981px) and (max-width: 1200px) {
          .main-shell-grid {
            grid-template-columns: 1fr !important;
          }

          .desktop-nav-shell {
            display: none !important;
          }
        }

        @media (min-width: 1201px) {
          .mobile-bottom-nav {
            display: none !important;
          }

          .menu-trigger {
            display: none !important;
          }
        }
      `}</style>
    </div>
  );
}
