"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useAuth } from "../../context/AuthContext";

const NAV_ITEMS = [
  { href: "/", label: "Home", short: "HM" },
  { href: "/matches", label: "Matches", short: "MT" },
  { href: "/search", label: "Search", short: "SR" },
  { href: "/interests", label: "Interests", short: "IN" },
  { href: "/shortlists", label: "Shortlists", short: "SL" },
  { href: "/chat", label: "Messages", short: "MS" },
  { href: "/profile", label: "Profile", short: "PF" },
  { href: "/settings", label: "Settings", short: "ST" },
];

const MOBILE_PRIMARY = ["/", "/matches", "/search", "/chat", "/profile"];

export default function MainLayout({ children }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();
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
      <div className="container-shell" style={{ paddingTop: "1rem" }}>
        <header
          className="panel"
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: "0.9rem",
            padding: "0.75rem 0.9rem",
            position: "sticky",
            top: "0.75rem",
            zIndex: 30,
            marginBottom: "0.95rem",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "0.65rem", minWidth: 0 }}>
            <button
              type="button"
              onClick={() => setDrawerOpen(true)}
              className="button button-secondary"
              style={{ padding: "0.5rem 0.78rem" }}
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
              <strong style={{ fontSize: "0.98rem", letterSpacing: "0.01em" }}>MatrimonyConnect</strong>
            </Link>
          </div>

          <form onSubmit={onSearchSubmit} style={{ flex: 1, maxWidth: 540 }}>
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="form-input"
              placeholder="Search by name, city, or profile keyword"
              aria-label="Global search"
            />
          </form>

          <div style={{ display: "flex", alignItems: "center", gap: "0.48rem" }}>
            <Link className="button button-secondary" href="/notifications">
              Alerts
            </Link>
            {user ? (
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

        <div
          className="main-shell-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "220px minmax(0, 1fr)",
            gap: "0.95rem",
            paddingBottom: "5.2rem",
          }}
        >
          <aside className="panel desktop-nav-shell" style={{ height: "fit-content", padding: "0.8rem", position: "sticky", top: "5.35rem" }}>
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
                    background: isActive(item.href) ? "rgba(240, 107, 78, 0.14)" : "transparent",
                    border: isActive(item.href) ? "1px solid rgba(240, 107, 78, 0.35)" : "1px solid transparent",
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
          width: "min(680px, calc(100% - 1rem))",
          zIndex: 60,
          background: "rgba(255,255,255,0.94)",
          backdropFilter: "blur(10px)",
          border: "1px solid rgba(23, 33, 59, 0.12)",
          borderRadius: 16,
          boxShadow: "var(--shadow-md)",
          padding: "0.4rem",
          display: "grid",
          gridTemplateColumns: "repeat(5, minmax(0, 1fr))",
          gap: "0.34rem",
        }}
      >
        {NAV_ITEMS.filter((item) => MOBILE_PRIMARY.includes(item.href)).map((item) => (
          <Link
            key={item.href}
            href={item.href}
            style={{
              borderRadius: 12,
              textAlign: "center",
              textDecoration: "none",
              fontSize: "0.74rem",
              fontWeight: 700,
              padding: "0.5rem 0.3rem",
              color: isActive(item.href) ? "var(--ink)" : "var(--ink-muted)",
              background: isActive(item.href) ? "rgba(240, 107, 78, 0.16)" : "transparent",
            }}
          >
            {item.label}
          </Link>
        ))}
      </nav>

      {drawerOpen && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 80,
            background: "rgba(8, 16, 32, 0.45)",
            backdropFilter: "blur(2px)",
            display: "flex",
          }}
          onClick={() => setDrawerOpen(false)}
        >
          <aside
            className="panel"
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
                    border: "1px solid rgba(23, 33, 59, 0.1)",
                    padding: "0.62rem 0.72rem",
                    background: isActive(item.href) ? "rgba(240, 107, 78, 0.14)" : "#fff",
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
        @media (max-width: 980px) {
          .main-shell-grid {
            grid-template-columns: 1fr !important;
          }

          .desktop-nav-shell {
            display: none !important;
          }
        }

        @media (min-width: 981px) {
          .mobile-bottom-nav {
            display: none !important;
          }
        }
      `}</style>
    </div>
  );
}
