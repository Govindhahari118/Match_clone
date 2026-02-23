"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { PUBLIC_SHORTCUTS, isRouteActive } from "@/config/navigation";

export default function PublicTopNav({ compact = false }) {
  const pathname = usePathname();

  return (
    <header className={`panel public-top-nav ${compact ? "public-top-nav-compact" : ""}`}>
      <Link href="/" className="public-brand" aria-label="Go to home">
        <span className="public-brand-mark">M</span>
        <strong>MatrimonyConnect</strong>
      </Link>

      <nav className="public-nav-links" aria-label="Primary">
        {PUBLIC_SHORTCUTS.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={`button button-secondary public-nav-link ${
              isRouteActive(pathname, item.href) ? "public-nav-link-active" : ""
            }`}
          >
            {item.label}
          </Link>
        ))}
      </nav>

      <div className="public-nav-actions">
        <Link href="/login" className="button button-secondary">
          Login
        </Link>
        <Link href="/step-1" className="button button-primary">
          Register
        </Link>
      </div>
    </header>
  );
}
