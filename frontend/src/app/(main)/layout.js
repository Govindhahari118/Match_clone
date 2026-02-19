"use client";

import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "../../context/AuthContext";

const NAV = [
    { href: "/", icon: "🏠", label: "Home" },
    { href: "/matches", icon: "💖", label: "Matches" },
    { href: "/interests", icon: "💌", label: "Interests" },
    { href: "/shortlists", icon: "⭐", label: "Shortlisted" },
    { href: "/search", icon: "🔍", label: "Search" },
    { href: "/chat", icon: "💬", label: "Messages" },
    { href: "/who-viewed", icon: "👁️", label: "Viewed" },
    { href: "/kundli", icon: "🔮", label: "Kundli" },
    { href: "/profile", icon: "👤", label: "Profile" },
];

export default function MainLayout({ children }) {
    const { user, logout } = useAuth();
    const navItems = NAV; // Show all items for everyone
    const pathname = usePathname();
    const router = useRouter();
    const [mobileOpen, setMobileOpen] = useState(false);
    const [profileOpen, setProfileOpen] = useState(false);
    const [darkMode, setDarkMode] = useState(false);
    const [searchQuery, setSearchQuery] = useState("");
    const profileRef = useRef(null);

    // Close profile dropdown on outside click
    useEffect(() => {
        const handler = (e) => {
            if (profileRef.current && !profileRef.current.contains(e.target)) {
                setProfileOpen(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    // Close mobile menu on route change
    useEffect(() => { setMobileOpen(false); }, [pathname]);

    const isActive = (href) => href === "/" ? pathname === "/" : pathname.startsWith(href);

    const getInitial = () =>
        user?.profile?.firstName?.[0]?.toUpperCase() ||
        user?.email?.[0]?.toUpperCase() || "U";

    const handleGlobalSearch = (e) => {
        e.preventDefault();
        if (searchQuery.trim()) {
            router.push(`/search?q=${encodeURIComponent(searchQuery)}`);
        }
    };

    return (
        <div className={`layout-container ${darkMode ? "dark-theme" : ""}`} style={{ display: "flex", minHeight: "100vh", background: darkMode ? "#0f172a" : "#f8fafc", color: darkMode ? "#f8fafc" : "inherit" }}>


            {/* ══════════ MAIN CONTENT WRAPPER ══════════ */}
            <div id="main-content-wrapper" style={{ flex: 1, display: "flex", flexDirection: "column", marginLeft: 0 }}>

                {/* ══════════ HEADER ══════════ */}
                <header style={{
                    height: 70, background: "white", borderBottom: "1px solid #e2e8f0",
                    display: "flex", alignItems: "center", justifyContent: "space-between",
                    padding: "0 24px", position: "sticky", top: 0, zIndex: 40
                }}>

                    {/* Logo (Always Visible) */}
                    <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
                        <Link href="/" style={{ display: "flex", alignItems: "center", gap: 8, textDecoration: "none" }}>
                            <div style={{ width: 28, height: 28, background: "#e11d48", borderRadius: 8, color: "white", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 800 }}>M</div>
                            <span style={{ fontWeight: 800, fontSize: 16, color: "#111827" }}>Matrimony</span>
                        </Link>
                    </div>

                    {/* Desktop Global Search */}
                    <div className="hidden lg:block" style={{ flex: 1, maxWidth: 480 }}>
                        <form onSubmit={handleGlobalSearch} style={{ position: "relative" }}>
                            <span style={{ position: "absolute", left: 14, top: "50%", transform: "translateY(-50%)", fontSize: 16, opacity: 0.5 }}>🔍</span>
                            <input
                                type="text"
                                placeholder="Search by ID, Name or Keywords..."
                                value={searchQuery}
                                onChange={e => setSearchQuery(e.target.value)}
                                style={{
                                    width: "100%", padding: "10px 14px 10px 42px", borderRadius: 99,
                                    border: "1px solid #e2e8f0", background: "#f8fafc", fontSize: 14, outline: "none",
                                    transition: "all 0.2s"
                                }}
                                onFocus={e => { e.target.style.background = "white"; e.target.style.borderColor = "#cbd5e1"; e.target.style.boxShadow = "0 2px 10px rgba(0,0,0,0.05)"; }}
                                onBlur={e => { e.target.style.background = "#f8fafc"; e.target.style.borderColor = "#e2e8f0"; e.target.style.boxShadow = "none"; }}
                            />
                        </form>
                    </div>

                    {/* Right Actions */}
                    <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
                        {/* Dark Mode Toggle (Visible to All) */}
                        <button onClick={() => setDarkMode(!darkMode)} className="hidden sm:flex" style={{ width: 40, height: 40, background: darkMode ? "#334155" : "#f1f5f9", borderRadius: "50%", border: "none", cursor: "pointer", alignItems: "center", justifyContent: "center", fontSize: 18 }}>
                            {darkMode ? "🌙" : "☀️"}
                        </button>

                        {!user ? (
                            <>
                                <Link href="/login" style={{ fontSize: 13, fontWeight: 700, color: "#475569", textDecoration: "none" }}>Login</Link>
                                <Link href="/signup" style={{ fontSize: 13, fontWeight: 700, color: "white", background: "#e11d48", padding: "8px 20px", borderRadius: 99, textDecoration: "none" }}>Sign Up</Link>
                            </>
                        ) : (
                            <>
                                <Link href="/upgrade" className="hidden sm:block" style={{ background: "linear-gradient(135deg, #f59e0b, #d97706)", color: "white", padding: "8px 20px", borderRadius: 99, fontWeight: 700, fontSize: 13, textDecoration: "none", boxShadow: "0 4px 12px rgba(245, 158, 11, 0.3)" }}>
                                    ✨ Upgrade
                                </Link>

                                {/* Notifications */}
                                <Link href="/notifications" style={{ position: "relative", width: 40, height: 40, background: "#f1f5f9", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", textDecoration: "none", color: "#64748b", fontSize: 20 }}>
                                    🔔
                                    <span style={{ position: "absolute", top: 10, right: 10, width: 8, height: 8, background: "#e11d48", borderRadius: "50%", border: "2px solid white" }}></span>
                                </Link>

                                {/* Profile Dropdown */}
                                <div style={{ position: "relative", cursor: "pointer" }} ref={profileRef} onClick={() => setProfileOpen(!profileOpen)}>
                                    <div style={{ display: "flex", alignItems: "center", gap: 6, padding: "4px 8px 4px 4px", borderRadius: 99, border: "1px solid #e2e8f0", background: "white" }}>
                                        <div style={{ width: 32, height: 32, background: "#a855f7", color: "white", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 700, fontSize: 14 }}>
                                            {getInitial()}
                                        </div>
                                        <span className="hidden sm:block" style={{ fontSize: 13, fontWeight: 600, color: "#334155" }}>Me ▼</span>
                                    </div>

                                    {profileOpen && (
                                        <div style={{ position: "absolute", right: 0, top: "120%", width: 220, background: "white", borderRadius: 16, border: "1px solid #f1f5f9", boxShadow: "0 10px 40px rgba(0,0,0,0.12)", overflow: "hidden", zIndex: 100 }}>
                                            <div style={{ padding: "16px", borderBottom: "1px solid #f8fafc", background: "#f8fafc" }}>
                                                <p style={{ fontWeight: 800, fontSize: 15, color: "#1e293b" }}>{user.profile?.firstName || "Guest"}</p>
                                                <p style={{ fontSize: 12, color: "#64748b" }}>{user.email}</p>
                                            </div>
                                            <Link href="/profile" style={{ display: "flex", alignItems: "center", gap: 10, padding: "12px 16px", fontSize: 14, color: "#334155", textDecoration: "none", fontWeight: 500 }}><span>👤</span> My Profile</Link>
                                            <button onClick={(e) => { e.stopPropagation(); setDarkMode(!darkMode); }} style={{ width: "100%", textAlign: "left", display: "flex", alignItems: "center", gap: 10, padding: "12px 16px", fontSize: 14, color: "#334155", background: "none", border: "none", cursor: "pointer", fontWeight: 500 }}><span>{darkMode ? "🌙" : "☀️"}</span> {darkMode ? "Light Mode" : "Dark Mode"}</button>
                                            <Link href="/settings" style={{ display: "flex", alignItems: "center", gap: 10, padding: "12px 16px", fontSize: 14, color: "#334155", textDecoration: "none", fontWeight: 500 }}><span>⚙️</span> Settings</Link>
                                            <button onClick={logout} style={{ width: "100%", textAlign: "left", display: "flex", alignItems: "center", gap: 10, padding: "12px 16px", fontSize: 14, color: "#e11d48", background: "#fff1f2", border: "none", cursor: "pointer", fontWeight: 700, marginTop: 4 }}><span>🚪</span> Sign Out</button>
                                        </div>
                                    )}
                                </div>
                            </>
                        )}
                    </div>
                </header>

                {/* ══════════ CONTENT ══════════ */}
                <main className="layout-main-wrapper" style={{ flex: 1, padding: "24px", maxWidth: 1200, width: "100%", margin: "0 auto", paddingBottom: 80 }}>
                    {children}
                </main>
            </div>

            {/* ══════════ MOBILE DRAWER ══════════ */}
            {mobileOpen && (
                <div style={{ position: "fixed", inset: 0, zIndex: 100, display: "flex" }}>
                    <div style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,0.5)" }} onClick={() => setMobileOpen(false)}></div>
                    <div style={{ width: 300, background: "white", height: "100%", position: "relative", display: "flex", flexDirection: "column", animation: "slideRight 0.3s cubic-bezier(0.16, 1, 0.3, 1)" }}>

                        {/* Drawer Header: Profile & Dark Mode */}
                        <div style={{ padding: 24, paddingBottom: 16, borderBottom: "1px solid #f1f5f9", background: "#f8fafc" }}>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 16 }}>
                                <button onClick={() => setMobileOpen(false)} style={{ background: "none", border: "none", fontSize: 24, color: "#64748b" }}>✕</button>
                                <div style={{ display: "flex", gap: 12 }}>
                                    <button onClick={() => setDarkMode(!darkMode)} style={{ width: 36, height: 36, borderRadius: "50%", background: darkMode ? "#334155" : "white", border: "1px solid #e2e8f0", display: "flex", alignItems: "center", justifyContent: "center" }}>
                                        {darkMode ? "🌙" : "☀️"}
                                    </button>
                                </div>
                            </div>

                            {user ? (
                                <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
                                    <div style={{ width: 56, height: 56, background: "linear-gradient(135deg, #e11d48, #be123c)", color: "white", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 24, fontWeight: 700, boxShadow: "0 4px 12px rgba(225, 29, 72, 0.2)" }}>
                                        {getInitial()}
                                    </div>
                                    <div>
                                        <p style={{ fontWeight: 800, fontSize: 18, color: "#1e293b" }}>{user.profile?.firstName || "User"}</p>
                                        <Link href="/profile" onClick={() => setMobileOpen(false)} style={{ fontSize: 13, color: "#e11d48", fontWeight: 600, textDecoration: "none" }}>View Profile →</Link>
                                    </div>
                                </div>
                            ) : (
                                <div>
                                    <p style={{ fontWeight: 800, fontSize: 18, color: "#1e293b", marginBottom: 8 }}>Welcome!</p>
                                    <div style={{ display: "flex", gap: 10 }}>
                                        <Link href="/login" onClick={() => setMobileOpen(false)} style={{ padding: "8px 16px", background: "white", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 13, fontWeight: 600, textDecoration: "none", color: "#334155" }}>Login</Link>
                                        <Link href="/signup" onClick={() => setMobileOpen(false)} style={{ padding: "8px 16px", background: "#e11d48", color: "white", borderRadius: 8, fontSize: 13, fontWeight: 600, textDecoration: "none" }}>Sign Up</Link>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Navigation Items */}
                        <div style={{ padding: "16px 12px", overflowY: "auto", flex: 1 }}>
                            <div style={{ marginBottom: 24 }}>
                                <h4 style={{ fontSize: 11, fontWeight: 800, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "1px", marginLeft: 12, marginBottom: 8 }}>Menu</h4>
                                {navItems.map(item => (
                                    <Link key={item.href} href={item.href} onClick={() => setMobileOpen(false)} style={{ display: "flex", alignItems: "center", gap: 14, padding: "12px 14px", borderRadius: 12, textDecoration: "none", color: isActive(item.href) ? "#e11d48" : "#334155", background: isActive(item.href) ? "#fff1f2" : "transparent", fontSize: 15, fontWeight: 600 }}>
                                        <span style={{ fontSize: 20 }}>{item.icon}</span> {item.label}
                                    </Link>
                                ))}
                            </div>

                            <div>
                                <h4 style={{ fontSize: 11, fontWeight: 800, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "1px", marginLeft: 12, marginBottom: 8 }}>Account</h4>
                                <Link href="/notifications" onClick={() => setMobileOpen(false)} style={{ display: "flex", alignItems: "center", gap: 14, padding: "12px 14px", borderRadius: 12, textDecoration: "none", color: "#334155", fontSize: 15, fontWeight: 600 }}>
                                    <span style={{ fontSize: 20 }}>🔔</span> Notifications
                                </Link>
                                <Link href="/settings" onClick={() => setMobileOpen(false)} style={{ display: "flex", alignItems: "center", gap: 14, padding: "12px 14px", borderRadius: 12, textDecoration: "none", color: "#334155", fontSize: 15, fontWeight: 600 }}>
                                    <span style={{ fontSize: 20 }}>⚙️</span> Settings
                                </Link>
                                {user && (
                                    <button onClick={() => { logout(); setMobileOpen(false); }} style={{ width: "100%", textAlign: "left", display: "flex", alignItems: "center", gap: 14, padding: "12px 14px", borderRadius: 12, border: "none", background: "none", color: "#ef4444", fontSize: 15, fontWeight: 600, cursor: "pointer" }}>
                                        <span style={{ fontSize: 20 }}>🚪</span> Sign Out
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}
            <style>{`@keyframes slideRight { from { transform: translateX(-100%); } to { transform: translateX(0); } }`}</style>

            {/* ══════════ MOBILE BOTTOM NAV ══════════ */}
            {/* ══════════ MOBILE BOTTOM NAV ══════════ */}
            <nav className="flex" style={{
                position: "fixed", bottom: 0, left: 0, right: 0, background: darkMode ? "#1e293b" : "white", borderTop: `1px solid ${darkMode ? "#334155" : "#f1f5f9"}`,
                padding: "8px 0", zIndex: 40, boxShadow: "0 -4px 10px rgba(0,0,0,0.03)",
                justifyContent: "space-around"
            }}>
                {navItems.filter(i => ["/", "/matches", "/search", "/chat"].includes(i.href)).map(item => (
                    <Link key={item.href} href={item.href} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 4, textDecoration: "none", color: isActive(item.href) ? "#e11d48" : (darkMode ? "#94a3b8" : "#94a3b8") }}>
                        <span style={{ fontSize: 20 }}>{item.icon}</span>
                        <span style={{ fontSize: 10, fontWeight: 600, color: darkMode ? (isActive(item.href) ? "#e11d48" : "#cbd5e1") : "inherit" }}>{item.label}</span>
                    </Link>
                ))}

                {/* Menu Item (Always Visible) */}
                <button onClick={() => setMobileOpen(true)} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 4, background: "none", border: "none", color: mobileOpen ? "#e11d48" : (darkMode ? "#94a3b8" : "#94a3b8") }}>
                    <span style={{ fontSize: 20 }}>☰</span>
                    <span style={{ fontSize: 10, fontWeight: 600, color: darkMode ? (mobileOpen ? "#e11d48" : "#cbd5e1") : "inherit" }}>Menu</span>
                </button>
            </nav>
            <style jsx global>{`
                .no-scrollbar::-webkit-scrollbar { display: none; }
                
                /* Dark Mode Overrides */
                .dark-theme header { background-color: #1e293b !important; border-color: #334155 !important; }
                .dark-theme h1, .dark-theme h2, .dark-theme h3, .dark-theme h4, .dark-theme p, .dark-theme span, .dark-theme label { color: #f8fafc !important; }
                .dark-theme input, .dark-theme select, .dark-theme textarea { background-color: #334155 !important; color: white !important; border-color: #475569 !important; }
                
                /* Override white backgrounds on cards */
                .dark-theme div[style*="background: white"], .dark-theme div[style*="background:white"] { background-color: #1e293b !important; color: #f8fafc !important; border-color: #334155 !important; }
                
                /* Specific Text adjustments */
                .dark-theme .text-slate-500, .dark-theme [style*="color: #64748b"] { color: #cbd5e1 !important; }
                .dark-theme [style*="color: #111827"] { color: white !important; }
            `}</style>
        </div>
    );
}
