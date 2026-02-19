"use client";

import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { useRouter } from "next/navigation";
import Link from "next/link";

// ═══════════════════════════════════════════════
// DASHBOARD (Logged-In View)
// ═══════════════════════════════════════════════
function Dashboard({ user, logout }) {
  const [recentMatches] = useState([
    { id: 1, firstName: "Priya", age: 26, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/women/44.jpg", verified: true, match: 94 },
    { id: 2, firstName: "Ananya", age: 24, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/women/45.jpg", verified: true, match: 89 },
    { id: 3, firstName: "Kavya", age: 27, city: "Chennai", profession: "CA", photo: "https://randomuser.me/api/portraits/women/46.jpg", verified: false, match: 85 },
    { id: 4, firstName: "Riya", age: 25, city: "Pune", profession: "Architect", photo: "https://randomuser.me/api/portraits/women/47.jpg", verified: true, match: 82 },
    { id: 5, firstName: "Simran", age: 28, city: "Delhi", profession: "Lawyer", photo: "https://randomuser.me/api/portraits/women/48.jpg", verified: true, match: 79 },
    { id: 6, firstName: "Naina", age: 23, city: "Hyderabad", profession: "Designer", photo: "https://randomuser.me/api/portraits/women/49.jpg", verified: false, match: 75 },
  ]);

  const firstName = user?.profile?.firstName || user?.email?.split("@")[0] || "there";
  const initial = firstName[0]?.toUpperCase();

  const STATS = [
    { label: "New Matches", value: 12, icon: "💖", bg: "linear-gradient(135deg,#e11d48,#be123c)", href: "/matches", shadow: "rgba(225,29,72,.3)" },
    { label: "Interests", value: 5, icon: "💌", bg: "linear-gradient(135deg,#8b5cf6,#7c3aed)", href: "/interests", shadow: "rgba(139,92,246,.3)" },
    { label: "Profile Views", value: 38, icon: "👁", bg: "linear-gradient(135deg,#0ea5e9,#0284c7)", href: "/profile", shadow: "rgba(14,165,233,.3)" },
    { label: "Shortlisted", value: 7, icon: "⭐", bg: "linear-gradient(135deg,#f59e0b,#d97706)", href: "/shortlists", shadow: "rgba(245,158,11,.3)" },
  ];

  const ACTIONS = [
    { href: "/matches", icon: "💖", label: "Browse Matches", desc: "See today's curated picks", color: "#e11d48", bg: "#fff1f2" },
    { href: "/search", icon: "🔍", label: "Advanced Search", desc: "Filter by 9+ criteria", color: "#8b5cf6", bg: "#faf5ff" },
    { href: "/interests", icon: "💌", label: "Manage Interests", desc: "Received, Sent & Mutual", color: "#0ea5e9", bg: "#f0f9ff" },
    { href: "/shortlists", icon: "⭐", label: "Shortlisted", desc: "Your saved favourites", color: "#f59e0b", bg: "#fffbeb" },
    { href: "/chat", icon: "💬", label: "Messages", desc: "Chat with mutual matches", color: "#10b981", bg: "#f0fdf4" },
    { href: "/profile", icon: "👤", label: "Edit Profile", desc: "Keep info up to date", color: "#6366f1", bg: "#eef2ff" },
    { href: "/pricing", icon: "👑", label: "Go Premium", desc: "Unlock contact numbers", color: "#d97706", bg: "#fffbeb" },
    { href: "/settings", icon: "⚙️", label: "Settings", desc: "Privacy & preferences", color: "#64748b", bg: "#f8fafc" },
  ];

  return (
    <div style={{ minHeight: "100vh", background: "#f8fafc" }}>

      {/* ── Welcome Banner ── */}
      <div style={{
        background: "linear-gradient(135deg, #1a0533 0%, #4a0070 40%, #8b0037 100%)",
        position: "relative", overflow: "hidden",
      }}>
        {/* Decorative blobs */}
        <div style={{ position: "absolute", top: -60, right: -60, width: 300, height: 300, background: "rgba(225,29,72,0.15)", borderRadius: "50%", filter: "blur(60px)", pointerEvents: "none" }}></div>
        <div style={{ position: "absolute", bottom: -40, left: -40, width: 250, height: 250, background: "rgba(139,92,246,0.15)", borderRadius: "50%", filter: "blur(50px)", pointerEvents: "none" }}></div>

        <div style={{ maxWidth: 1280, margin: "0 auto", padding: "2rem 1.5rem", position: "relative" }}>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "1.5rem", justifyContent: "space-between", alignItems: "center" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
              <div style={{
                width: 58, height: 58,
                background: "linear-gradient(135deg, #e11d48, #8b5cf6)",
                borderRadius: 18,
                display: "flex", alignItems: "center", justifyContent: "center",
                fontSize: 22, fontWeight: 900, color: "white",
                boxShadow: "0 8px 24px rgba(225,29,72,0.4)",
                flexShrink: 0,
              }}>{initial}</div>
              <div>
                <h1 style={{ fontSize: 22, fontWeight: 800, color: "white", marginBottom: 4 }}>
                  Welcome back, {firstName}! <span className="anim-heartbeat" style={{ display: "inline-block" }}>💕</span>
                </h1>
                <p style={{ fontSize: 13, color: "rgba(255,255,255,0.65)" }}>
                  You have <strong style={{ color: "#fda4af" }}>12 new matches</strong> waiting for you today
                </p>
              </div>
            </div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <Link href="/profile" style={{
                padding: "8px 18px", background: "rgba(255,255,255,0.12)",
                border: "1px solid rgba(255,255,255,0.2)", borderRadius: 999,
                color: "white", fontSize: 13, fontWeight: 600, textDecoration: "none",
                transition: "all 0.2s",
              }}>Edit Profile</Link>
              <Link href="/pricing" style={{
                padding: "8px 18px", background: "linear-gradient(135deg, #f59e0b, #d97706)",
                borderRadius: 999, color: "white", fontSize: 13, fontWeight: 700,
                textDecoration: "none", boxShadow: "0 4px 12px rgba(245,158,11,0.4)",
              }}>👑 Upgrade Plan</Link>
            </div>
          </div>
        </div>
      </div>

      <div style={{ maxWidth: 1280, margin: "0 auto", padding: "1.5rem" }}>

        {/* ── Profile incomplete nudge ── */}
        {!user?.isVerified && (
          <div className="anim-fadeInUp" style={{
            display: "flex", flexWrap: "wrap", gap: "1rem",
            alignItems: "center", justifyContent: "space-between",
            background: "linear-gradient(135deg, #fff7ed, #fef3c7)",
            border: "1px solid #fde68a",
            borderRadius: 16, padding: "14px 20px", marginBottom: "1.5rem",
          }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ fontSize: 28 }}>⚡</div>
              <div>
                <p style={{ fontWeight: 700, color: "#92400e", fontSize: 14 }}>Complete your profile for 3× more matches!</p>
                <p style={{ fontSize: 12, color: "#b45309" }}>Add a photo, verify your ID, and set partner preferences.</p>
              </div>
            </div>
            <Link href="/profile" style={{
              padding: "8px 20px", background: "#f59e0b",
              color: "white", borderRadius: 999, fontSize: 13, fontWeight: 700,
              textDecoration: "none", boxShadow: "0 4px 12px rgba(245,158,11,0.3)",
            }}>
              Complete Now →
            </Link>
          </div>
        )}

        {/* ── Stats Row ── */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: "1rem", marginBottom: "1.5rem" }}>
          {STATS.map((s, i) => (
            <Link key={s.label} href={s.href} style={{ textDecoration: "none" }}>
              <div
                className={`stat-card anim-fadeInUp delay-${i + 1}`}
                style={{ background: s.bg, boxShadow: `0 8px 24px ${s.shadow}` }}
              >
                <div style={{ fontSize: 28, marginBottom: 8 }}>{s.icon}</div>
                <div style={{ fontSize: 36, fontWeight: 900, lineHeight: 1 }}>{s.value}</div>
                <div style={{ fontSize: 12, color: "rgba(255,255,255,0.8)", fontWeight: 600, marginTop: 4 }}>{s.label}</div>
              </div>
            </Link>
          ))}
        </div>

        {/* ── Today's Matches ── */}
        <div className="card anim-fadeInUp" style={{ marginBottom: "1.5rem" }}>
          <div style={{ padding: "16px 20px 12px", borderBottom: "1px solid #f8fafc", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div>
              <h2 style={{ fontWeight: 800, fontSize: 17, color: "#111827" }}>Today's Matches ✨</h2>
              <p style={{ fontSize: 12, color: "#94a3b8", marginTop: 2 }}>Fresh picks based on your preferences</p>
            </div>
            <Link href="/matches" style={{ fontSize: 13, fontWeight: 700, color: "#e11d48", textDecoration: "none" }}>
              View All →
            </Link>
          </div>
          <div style={{ padding: "1rem", display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(155px, 1fr))", gap: "1rem" }}>
            {recentMatches.map((m, i) => (
              <Link key={m.id} href={`/profile/${m.id}`} style={{ textDecoration: "none" }}>
                <div
                  className={`profile-card anim-fadeInUp delay-${Math.min(i + 1, 5)}`}
                  style={{ cursor: "pointer" }}
                >
                  <div className="photo-wrapper" style={{ height: 170, position: "relative" }}>
                    <img src={m.photo} alt={m.firstName} />
                    {/* Match score badge */}
                    <div style={{
                      position: "absolute", top: 8, right: 8,
                      background: "linear-gradient(135deg, #e11d48, #be123c)",
                      color: "white", fontSize: 10, fontWeight: 800,
                      padding: "3px 7px", borderRadius: 99,
                      boxShadow: "0 2px 8px rgba(225,29,72,0.4)",
                    }}>{m.match}%</div>
                    {/* Verified badge */}
                    {m.verified && (
                      <div style={{
                        position: "absolute", bottom: 8, left: 8,
                        background: "rgba(255,255,255,0.9)", backdropFilter: "blur(8px)",
                        color: "#2563eb", fontSize: 10, fontWeight: 700,
                        padding: "2px 7px", borderRadius: 99,
                      }}>✅ Verified</div>
                    )}
                    {/* Gradient overlay */}
                    <div style={{
                      position: "absolute", bottom: 0, left: 0, right: 0, height: "55%",
                      background: "linear-gradient(to top, rgba(0,0,0,0.7), transparent)",
                    }}></div>
                    <div style={{ position: "absolute", bottom: 8, left: 10, color: "white" }}>
                      <p style={{ fontWeight: 700, fontSize: 13 }}>{m.firstName}, {m.age}</p>
                      <p style={{ fontSize: 10, color: "rgba(255,255,255,0.8)" }}>{m.city}</p>
                    </div>
                  </div>
                  <div style={{ padding: "10px 12px" }}>
                    <p style={{ fontSize: 11, color: "#64748b", fontWeight: 600 }}>{m.profession}</p>
                    <div style={{ display: "flex", gap: 4, marginTop: 8 }}>
                      <button
                        style={{
                          flex: 1, padding: "6px", borderRadius: 8,
                          background: "linear-gradient(135deg, #e11d48, #be123c)",
                          color: "white", border: "none", cursor: "pointer",
                          fontSize: 11, fontWeight: 700,
                        }}
                        onClick={e => e.preventDefault()}
                      >
                        💌 Interest
                      </button>
                    </div>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>

        {/* ── Quick Actions ── */}
        <div>
          <h2 style={{ fontWeight: 800, fontSize: 17, color: "#111827", marginBottom: "1rem" }}>Quick Access</h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(190px, 1fr))", gap: "0.875rem" }}>
            {ACTIONS.map((a, i) => (
              <Link key={a.href} href={a.href} style={{ textDecoration: "none" }}>
                <div
                  className={`card card-lift anim-fadeInUp delay-${Math.min(i + 1, 5)}`}
                  style={{
                    padding: "16px", display: "flex", flexDirection: "column", gap: 8,
                    height: "100%", cursor: "pointer", background: a.bg,
                    border: `1.5px solid ${a.color}18`,
                  }}
                >
                  <div style={{
                    width: 40, height: 40, borderRadius: 12,
                    background: `${a.color}18`,
                    display: "flex", alignItems: "center", justifyContent: "center",
                    fontSize: 20,
                  }}>{a.icon}</div>
                  <div>
                    <p style={{ fontWeight: 700, fontSize: 13, color: "#111827" }}>{a.label}</p>
                    <p style={{ fontSize: 11, color: "#64748b", marginTop: 2 }}>{a.desc}</p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>

        {/* Admin panel */}
        {user?.role === "admin" && (
          <Link href="/admin" style={{ textDecoration: "none", display: "block", marginTop: "1.25rem" }}>
            <div style={{
              background: "linear-gradient(135deg, #0f0c29, #302b63)",
              borderRadius: 20, padding: "1.25rem 1.5rem",
              display: "flex", alignItems: "center", justifyContent: "space-between",
              color: "white",
            }}>
              <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
                <div style={{ width: 48, height: 48, background: "rgba(255,255,255,0.1)", borderRadius: 14, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 24 }}>🛠️</div>
                <div>
                  <h3 style={{ fontWeight: 800, fontSize: 16 }}>Admin Panel</h3>
                  <p style={{ fontSize: 12, color: "rgba(255,255,255,0.6)" }}>Manage users, profiles & platform</p>
                </div>
              </div>
              <span style={{ fontSize: 20, color: "rgba(255,255,255,0.4)" }}>→</span>
            </div>
          </Link>
        )}
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════
// LANDING PAGE (Guest View)
// ═══════════════════════════════════════════════
function LandingPage() {
  const router = useRouter();
  const [gender, setGender] = useState("female");
  const [ageMin, setAgeMin] = useState(22);
  const [ageMax, setAgeMax] = useState(30);
  const [religion, setReligion] = useState("Hindu");
  const [activeStory, setActiveStory] = useState(0);

  const onSearch = (e) => {
    e.preventDefault();
    router.push(`/matches?gender=${gender}&minAge=${ageMin}&maxAge=${ageMax}&religion=${religion}`);
  };

  const COMMUNITY = ["Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Parsi", "Jewish"];
  const STATS = [
    { value: "5M+", label: "Registered Members" },
    { value: "4.2L+", label: "Successful Marriages" },
    { value: "99%", label: "Verified Profiles" },
    { value: "84%", label: "Match Satisfaction" },
  ];
  const FEATURES = [
    { icon: "🤖", title: "AI Matchmaking", desc: "Our algorithm analyses 50+ dimensions for perfect compatibility scores." },
    { icon: "🛡️", title: "Verified Profiles", desc: "Every profile is government ID verified. Zero fake accounts." },
    { icon: "🔒", title: "Privacy First", desc: "Phone numbers hidden until mutual consent. Your data stays yours." },
    { icon: "💬", title: "Real-time Chat", desc: "Instant encrypted messaging with your matched profiles." },
    { icon: "📊", title: "Compatibility Score", desc: "Detailed analysis reports showing match strength across 10 pillars." },
    { icon: "👑", title: "Premium Matching", desc: "Priority placement + dedicated relationship manager for VIP members." },
  ];
  const STORIES = [
    { names: "Rahul & Priya", where: "Mumbai", year: "2025", img: "https://images.unsplash.com/photo-1606216794074-735e91aaad6e?w=800&q=80", quote: "The 94% compatibility score predicted every little thing about us. We're forever grateful!" },
    { names: "Arjun & Deepika", where: "Bangalore", year: "2024", img: "https://images.unsplash.com/photo-1583939003579-730e3918a45a?w=800&q=80", quote: "I was skeptical, but the matching was uncanny. 3 months from first message to saying yes!" },
    { names: "Karan & Sneha", where: "Delhi", year: "2025", img: "https://images.unsplash.com/photo-1537907510278-3a0a82b8e5bb?w=800&q=80", quote: "The verification gave us trust to be open and honest. Best decision of our lives." },
  ];
  const STEPS = [
    { n: "01", icon: "📝", title: "Create Profile", desc: "Fill in your details and upload photos in under 5 minutes." },
    { n: "02", icon: "⚙️", title: "Set Preferences", desc: "Tell us what matters most to you in a life partner." },
    { n: "03", icon: "💖", title: "Get AI Matches", desc: "Receive daily curated profiles with compatibility scores." },
    { n: "04", icon: "💍", title: "Start Your Story", desc: "Connect, chat, meet, and build your forever." },
  ];

  // Auto-rotate stories
  useEffect(() => {
    const t = setInterval(() => setActiveStory(p => (p + 1) % STORIES.length), 5000);
    return () => clearInterval(t);
  }, []);

  return (
    <div style={{ fontFamily: "'Inter', sans-serif", color: "#111827" }}>

      {/* ─── NAVBAR ─── */}
      <nav style={{
        position: "fixed", top: 0, left: 0, right: 0, zIndex: 100,
        padding: "0 1.5rem",
        background: "rgba(15,5,30,0.75)",
        backdropFilter: "blur(20px)",
        WebkitBackdropFilter: "blur(20px)",
        borderBottom: "1px solid rgba(255,255,255,0.06)",
      }}>
        <div style={{ maxWidth: 1280, margin: "0 auto", height: 64, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div style={{ width: 36, height: 36, background: "linear-gradient(135deg,#e11d48,#be123c)", borderRadius: 11, display: "flex", alignItems: "center", justifyContent: "center", color: "white", fontWeight: 900, fontSize: 17 }}>M</div>
            <span style={{ fontWeight: 800, fontSize: 17, color: "white" }}>Matrimony<span style={{ color: "#fda4af" }}>Connect</span></span>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <a href="/help" style={{ color: "rgba(255,255,255,0.6)", textDecoration: "none", fontSize: 13, fontWeight: 600, padding: "0 10px" }} className="hidden md:block">Help</a>
            <Link href="/login" style={{ color: "rgba(255,255,255,0.85)", textDecoration: "none", fontSize: 13, fontWeight: 600, padding: "8px 16px" }}>Login</Link>
            <Link href="/signup" style={{
              background: "linear-gradient(135deg,#e11d48,#c2185b)",
              color: "white", padding: "8px 20px", borderRadius: 999,
              textDecoration: "none", fontSize: 13, fontWeight: 700,
              boxShadow: "0 4px 16px rgba(225,29,72,0.4)",
            }}>Register Free</Link>
          </div>
        </div>
      </nav>

      {/* ─── HERO ─── */}
      <section style={{
        minHeight: "100vh",
        background: "linear-gradient(135deg, #0f0317 0%, #2d003e 35%, #5b0020 70%, #0f0317 100%)",
        position: "relative", overflow: "hidden",
        display: "flex", alignItems: "center",
      }}>
        {/* Background art */}
        <div style={{ position: "absolute", inset: 0, pointerEvents: "none" }}>
          <div style={{ position: "absolute", top: "10%", left: "5%", width: 500, height: 500, background: "radial-gradient(circle, rgba(139,92,246,0.2), transparent 70%)", borderRadius: "50%" }}></div>
          <div style={{ position: "absolute", bottom: "5%", right: "8%", width: 400, height: 400, background: "radial-gradient(circle, rgba(225,29,72,0.25), transparent 70%)", borderRadius: "50%" }}></div>
          <div style={{ position: "absolute", top: "50%", left: "50%", transform: "translate(-50%, -50%)", width: 800, height: 800, background: "radial-gradient(circle, rgba(79,20,100,0.3), transparent 60%)", borderRadius: "50%" }}></div>
          {/* Dots grid */}
          <div style={{ position: "absolute", inset: 0, backgroundImage: "radial-gradient(rgba(255,255,255,0.04) 1px, transparent 1px)", backgroundSize: "40px 40px" }}></div>
        </div>

        <div style={{ maxWidth: 1280, margin: "0 auto", padding: "6rem 1.5rem 4rem", width: "100%", display: "grid", gridTemplateColumns: "1fr 1fr", gap: "4rem", alignItems: "center", position: "relative", zIndex: 1 }} className="grid-cols-hero">
          {/* Left: Copy */}
          <div className="anim-fadeInUp">
            <div style={{
              display: "inline-flex", alignItems: "center", gap: 8,
              background: "rgba(225,29,72,0.15)", border: "1px solid rgba(225,29,72,0.3)",
              borderRadius: 999, padding: "6px 16px", marginBottom: "1.5rem",
            }}>
              <span style={{ width: 6, height: 6, background: "#e11d48", borderRadius: "50%", display: "inline-block" }}></span>
              <span style={{ fontSize: 12, fontWeight: 700, color: "#fda4af" }}>India's #1 AI Matrimony Platform</span>
            </div>

            <h1 style={{ fontSize: "clamp(2.2rem, 5vw, 3.8rem)", fontWeight: 900, color: "white", lineHeight: 1.1, marginBottom: "1.25rem" }}>
              Find Your<br />
              <span style={{
                background: "linear-gradient(135deg, #fda4af 0%, #f9a8d4 40%, #c084fc 100%)",
                WebkitBackgroundClip: "text", backgroundClip: "text",
                WebkitTextFillColor: "transparent",
              }}>Perfect Partner</span><br />
              <span style={{ fontSize: "70%", fontWeight: 700, color: "rgba(255,255,255,0.6)" }}>Made for Indians, by Indians</span>
            </h1>

            <p style={{ fontSize: 16, color: "rgba(255,255,255,0.6)", lineHeight: 1.75, marginBottom: "2rem", maxWidth: 480 }}>
              Join 5 million members. AI-powered compatibility analysis. 100% verified profiles. 4.2 lakh successful marriages.
            </p>

            {/* Social proof */}
            <div style={{ display: "flex", alignItems: "center", gap: "1.5rem", marginBottom: "2rem", flexWrap: "wrap" }}>
              <div style={{ display: "flex", alignItems: "center" }}>
                {[44, 45, 46, 47, 48].map(i => (
                  <img key={i} src={`https://randomuser.me/api/portraits/women/${i}.jpg`} alt="" style={{ width: 36, height: 36, borderRadius: "50%", border: "2px solid rgba(225,29,72,0.5)", marginLeft: i === 44 ? 0 : -10, objectFit: "cover" }} />
                ))}
              </div>
              <div>
                <div style={{ color: "#fbbf24", fontSize: 14, letterSpacing: 2 }}>★★★★★</div>
                <p style={{ fontSize: 12, color: "rgba(255,255,255,0.5)", marginTop: 2 }}>4.9/5 from 1.2M+ reviews</p>
              </div>
            </div>

            <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
              <Link href="/signup" style={{
                padding: "14px 32px",
                background: "linear-gradient(135deg, #e11d48, #c2185b)",
                color: "white", fontWeight: 800, fontSize: 15,
                borderRadius: 999, textDecoration: "none",
                boxShadow: "0 8px 30px rgba(225,29,72,0.45)",
                transition: "all 0.2s",
              }}>Create Free Profile</Link>
              <Link href="/login" style={{
                padding: "14px 28px",
                background: "rgba(255,255,255,0.08)",
                border: "1.5px solid rgba(255,255,255,0.2)",
                color: "white", fontWeight: 700, fontSize: 15,
                borderRadius: 999, textDecoration: "none",
                backdropFilter: "blur(8px)",
              }}>Sign In →</Link>
            </div>
          </div>

          {/* Right: Search Widget */}
          <div className="anim-fadeInUp delay-2">
            <div style={{
              background: "rgba(255,255,255,0.06)",
              backdropFilter: "blur(24px)",
              WebkitBackdropFilter: "blur(24px)",
              border: "1px solid rgba(255,255,255,0.12)",
              borderRadius: 28, padding: "2rem",
              boxShadow: "0 30px 80px rgba(0,0,0,0.4)",
            }}>
              <h2 style={{ fontWeight: 800, fontSize: 20, color: "white", marginBottom: 4 }}>
                🔍 Find Your Match
              </h2>
              <p style={{ fontSize: 13, color: "rgba(255,255,255,0.5)", marginBottom: "1.5rem" }}>
                Free registration · 5 lakh+ verified profiles
              </p>

              <form onSubmit={onSearch} style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem" }}>
                  <div>
                    <label style={{ display: "block", fontSize: 11, fontWeight: 700, color: "rgba(255,255,255,0.5)", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 6 }}>Looking for</label>
                    <select value={gender} onChange={e => setGender(e.target.value)} style={{ width: "100%", background: "rgba(255,255,255,0.08)", border: "1px solid rgba(255,255,255,0.15)", borderRadius: 12, padding: "10px 12px", color: "white", fontSize: 13, fontWeight: 600, outline: "none" }}>
                      <option value="female" style={{ background: "#1a0533" }}>Bride (Woman)</option>
                      <option value="male" style={{ background: "#1a0533" }}>Groom (Man)</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: 11, fontWeight: 700, color: "rgba(255,255,255,0.5)", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 6 }}>Religion</label>
                    <select value={religion} onChange={e => setReligion(e.target.value)} style={{ width: "100%", background: "rgba(255,255,255,0.08)", border: "1px solid rgba(255,255,255,0.15)", borderRadius: 12, padding: "10px 12px", color: "white", fontSize: 13, fontWeight: 600, outline: "none" }}>
                      {["Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Any"].map(r => <option key={r} style={{ background: "#1a0533" }}>{r}</option>)}
                    </select>
                  </div>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: 11, fontWeight: 700, color: "rgba(255,255,255,0.5)", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 6 }}>Age Range</label>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input type="number" min="18" max="70" value={ageMin} onChange={e => setAgeMin(e.target.value)} style={{ flex: 1, background: "rgba(255,255,255,0.08)", border: "1px solid rgba(255,255,255,0.15)", borderRadius: 12, padding: "10px 12px", color: "white", fontSize: 13, fontWeight: 600, outline: "none", textAlign: "center" }} />
                    <span style={{ color: "rgba(255,255,255,0.3)", fontSize: 16 }}>―</span>
                    <input type="number" min="18" max="70" value={ageMax} onChange={e => setAgeMax(e.target.value)} style={{ flex: 1, background: "rgba(255,255,255,0.08)", border: "1px solid rgba(255,255,255,0.15)", borderRadius: 12, padding: "10px 12px", color: "white", fontSize: 13, fontWeight: 600, outline: "none", textAlign: "center" }} />
                  </div>
                </div>

                <button type="submit" style={{
                  width: "100%", padding: "14px",
                  background: "linear-gradient(135deg, #e11d48, #c2185b)",
                  color: "white", fontWeight: 800, fontSize: 15,
                  border: "none", borderRadius: 14, cursor: "pointer",
                  boxShadow: "0 8px 24px rgba(225,29,72,0.4)",
                  transition: "transform 0.2s, box-shadow 0.2s",
                }}>
                  Search Free Matches →
                </button>

                <p style={{ textAlign: "center", fontSize: 12, color: "rgba(255,255,255,0.4)" }}>
                  New here?{" "}
                  <Link href="/signup" style={{ color: "#fda4af", fontWeight: 700, textDecoration: "none" }}>Register in 2 minutes</Link>
                </p>
              </form>
            </div>
          </div>
        </div>

        {/* Scroll indicator */}
        <div style={{ position: "absolute", bottom: 30, left: "50%", transform: "translateX(-50%)", textAlign: "center" }}>
          <div style={{ color: "rgba(255,255,255,0.3)", fontSize: 11, fontWeight: 600, letterSpacing: 2, marginBottom: 8 }}>SCROLL</div>
          <div className="anim-float" style={{ width: 1, height: 40, background: "linear-gradient(to bottom, rgba(255,255,255,0.3), transparent)", margin: "0 auto" }}></div>
        </div>
      </section>

      {/* ─── STATS BAND ─── */}
      <section style={{ background: "white", borderBottom: "1px solid #f1f5f9", padding: "3rem 1.5rem" }}>
        <div style={{ maxWidth: 1280, margin: "0 auto", display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "2rem", textAlign: "center" }} className="grid-stats">
          {STATS.map((s, i) => (
            <div key={s.label} className={`anim-fadeInUp delay-${i + 1}`}>
              <div style={{ fontSize: "clamp(2rem, 4vw, 3rem)", fontWeight: 900, background: "linear-gradient(135deg, #e11d48, #be123c)", WebkitBackgroundClip: "text", backgroundClip: "text", WebkitTextFillColor: "transparent" }}>{s.value}</div>
              <div style={{ fontSize: 13, color: "#64748b", fontWeight: 600, marginTop: 4 }}>{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ─── COMMUNITY TABS ─── */}
      <section style={{ background: "#faf5ff", padding: "5rem 1.5rem" }}>
        <div style={{ maxWidth: 1280, margin: "0 auto", textAlign: "center" }}>
          <p style={{ fontSize: 12, fontWeight: 800, color: "#8b5cf6", letterSpacing: 3, textTransform: "uppercase", marginBottom: "0.75rem" }}>For Every Community</p>
          <h2 style={{ fontSize: "clamp(1.8rem, 3.5vw, 2.8rem)", fontWeight: 900, color: "#111827", marginBottom: "0.75rem" }}>Trusted by All Communities</h2>
          <p style={{ color: "#64748b", fontSize: 15, marginBottom: "2.5rem" }}>India's most diverse matrimony platform, respecting every faith and tradition.</p>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "0.75rem", justifyContent: "center", marginBottom: "2.5rem" }}>
            {COMMUNITY.map(c => (
              <button key={c}
                onMouseEnter={e => { e.currentTarget.style.background = "#8b5cf6"; e.currentTarget.style.color = "white"; }}
                onMouseLeave={e => { e.currentTarget.style.background = "white"; e.currentTarget.style.color = "#374151"; }}
                style={{
                  padding: "10px 22px", borderRadius: 999,
                  background: "white", border: "1.5px solid #e9d5ff",
                  fontSize: 13, fontWeight: 700, color: "#374151",
                  cursor: "pointer", transition: "all 0.2s",
                  boxShadow: "0 2px 8px rgba(139,92,246,0.08)",
                }}
              >{c} Matrimony</button>
            ))}
          </div>
          <Link href="/signup" style={{
            display: "inline-block",
            padding: "13px 36px",
            background: "linear-gradient(135deg, #8b5cf6, #7c3aed)",
            color: "white", fontWeight: 800, fontSize: 15,
            borderRadius: 999, textDecoration: "none",
            boxShadow: "0 8px 24px rgba(139,92,246,0.35)",
          }}>Start Your Free Search →</Link>
        </div>
      </section>

      {/* ─── FEATURES ─── */}
      <section style={{ background: "white", padding: "5rem 1.5rem" }}>
        <div style={{ maxWidth: 1280, margin: "0 auto" }}>
          <div style={{ textAlign: "center", marginBottom: "3rem" }}>
            <p style={{ fontSize: 12, fontWeight: 800, color: "#e11d48", letterSpacing: 3, textTransform: "uppercase", marginBottom: "0.75rem" }}>Why Us</p>
            <h2 style={{ fontSize: "clamp(1.8rem, 3.5vw, 2.8rem)", fontWeight: 900, color: "#111827", marginBottom: "0.75rem" }}>Why MatrimonyConnect?</h2>
            <p style={{ color: "#64748b", fontSize: 15, maxWidth: 560, margin: "0 auto" }}>Built by engineers, designed for hearts. The most intelligent matrimony platform in India.</p>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "1.25rem" }}>
            {FEATURES.map((f, i) => (
              <div key={f.title}
                className={`anim-fadeInUp delay-${Math.min(i + 1, 5)}`}
                onMouseEnter={e => { e.currentTarget.style.borderColor = "#fda4af"; e.currentTarget.style.boxShadow = "0 12px 40px rgba(225,29,72,0.1)"; e.currentTarget.style.transform = "translateY(-4px)"; }}
                onMouseLeave={e => { e.currentTarget.style.borderColor = "#f1f5f9"; e.currentTarget.style.boxShadow = "none"; e.currentTarget.style.transform = "translateY(0)"; }}
                style={{ padding: "1.75rem", border: "1.5px solid #f1f5f9", borderRadius: 20, background: "white", transition: "all 0.25s" }}
              >
                <div style={{ fontSize: 36, marginBottom: "0.875rem" }}>{f.icon}</div>
                <h3 style={{ fontWeight: 800, fontSize: 16, color: "#111827", marginBottom: 8 }}>{f.title}</h3>
                <p style={{ color: "#64748b", fontSize: 13, lineHeight: 1.7 }}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ─── HOW IT WORKS ─── */}
      <section style={{ background: "linear-gradient(135deg, #fff1f2, #fdf2f8, #ede9fe)", padding: "5rem 1.5rem" }}>
        <div style={{ maxWidth: 1280, margin: "0 auto" }}>
          <div style={{ textAlign: "center", marginBottom: "3rem" }}>
            <p style={{ fontSize: 12, fontWeight: 800, color: "#8b5cf6", letterSpacing: 3, textTransform: "uppercase", marginBottom: "0.75rem" }}>Simple Process</p>
            <h2 style={{ fontSize: "clamp(1.8rem, 3.5vw, 2.8rem)", fontWeight: 900, color: "#111827" }}>Find Love in 4 Steps</h2>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "2rem" }}>
            {STEPS.map((s, i) => (
              <div key={s.n} className={`anim-fadeInUp delay-${i + 1}`} style={{ textAlign: "center" }}>
                <div style={{ position: "relative", display: "inline-block", marginBottom: "1.25rem" }}>
                  <div style={{
                    width: 72, height: 72, borderRadius: 22,
                    background: "white",
                    boxShadow: "0 8px 24px rgba(0,0,0,0.1)",
                    display: "flex", alignItems: "center", justifyContent: "center",
                    fontSize: 30, margin: "0 auto",
                  }}>{s.icon}</div>
                  <div style={{
                    position: "absolute", top: -6, right: -6,
                    width: 24, height: 24, borderRadius: "50%",
                    background: "linear-gradient(135deg, #e11d48, #be123c)",
                    color: "white", fontSize: 10, fontWeight: 900,
                    display: "flex", alignItems: "center", justifyContent: "center",
                    boxShadow: "0 3px 10px rgba(225,29,72,0.4)",
                  }}>{s.n}</div>
                </div>
                <h3 style={{ fontWeight: 800, fontSize: 16, color: "#111827", marginBottom: 8 }}>{s.title}</h3>
                <p style={{ color: "#64748b", fontSize: 13, lineHeight: 1.7 }}>{s.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ─── SUCCESS STORIES ─── */}
      <section style={{ background: "white", padding: "5rem 1.5rem" }}>
        <div style={{ maxWidth: 1280, margin: "0 auto" }}>
          <div style={{ textAlign: "center", marginBottom: "3rem" }}>
            <p style={{ fontSize: 12, fontWeight: 800, color: "#e11d48", letterSpacing: 3, textTransform: "uppercase", marginBottom: "0.75rem" }}>Real Couples</p>
            <h2 style={{ fontSize: "clamp(1.8rem, 3.5vw, 2.8rem)", fontWeight: 900, color: "#111827" }}>4.2 Lakh+ Success Stories</h2>
          </div>

          {/* Story carousel */}
          <div style={{ position: "relative", borderRadius: 28, overflow: "hidden", height: 400, marginBottom: "2rem", boxShadow: "0 20px 60px rgba(0,0,0,0.15)" }}>
            {STORIES.map((s, i) => (
              <div key={s.names} style={{
                position: "absolute", inset: 0,
                opacity: i === activeStory ? 1 : 0,
                transition: "opacity 0.7s ease",
              }}>
                <img src={s.img} alt={s.names} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0.2) 60%, transparent 100%)" }}></div>
                <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, padding: "2rem 2.5rem" }}>
                  <div style={{ display: "flex", gap: "0.5rem", marginBottom: "0.875rem" }}>
                    <span style={{ background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", fontSize: 11, fontWeight: 700, padding: "3px 10px", borderRadius: 99 }}>💍 Married {s.year}</span>
                    <span style={{ background: "rgba(255,255,255,0.15)", color: "white", fontSize: 11, fontWeight: 600, padding: "3px 10px", borderRadius: 99, backdropFilter: "blur(8px)" }}>📍 {s.where}</span>
                  </div>
                  <h3 style={{ fontSize: 28, fontWeight: 900, color: "white", marginBottom: 10 }}>{s.names}</h3>
                  <p style={{ fontSize: 14, color: "rgba(255,255,255,0.8)", fontStyle: "italic", maxWidth: 560, lineHeight: 1.6 }}>"{s.quote}"</p>
                </div>
              </div>
            ))}
            {/* Dots */}
            <div style={{ position: "absolute", bottom: 16, right: 20, display: "flex", gap: 6 }}>
              {STORIES.map((_, i) => (
                <button key={i} onClick={() => setActiveStory(i)} style={{ width: i === activeStory ? 20 : 6, height: 6, borderRadius: 99, background: i === activeStory ? "white" : "rgba(255,255,255,0.4)", border: "none", cursor: "pointer", transition: "all 0.3s" }}></button>
              ))}
            </div>
          </div>
          <div style={{ textAlign: "center" }}>
            <Link href="/success-stories" style={{ color: "#e11d48", fontWeight: 700, fontSize: 14, textDecoration: "none" }}>Read more success stories →</Link>
          </div>
        </div>
      </section>

      {/* ─── FINAL CTA ─── */}
      <section style={{
        background: "linear-gradient(135deg, #1a0533 0%, #4a0070 40%, #8b0037 100%)",
        padding: "5rem 1.5rem", textAlign: "center", position: "relative", overflow: "hidden",
      }}>
        <div style={{ position: "absolute", top: -100, left: "50%", transform: "translateX(-50%)", width: 700, height: 700, background: "radial-gradient(circle, rgba(225,29,72,0.15), transparent 70%)", borderRadius: "50%", pointerEvents: "none" }}></div>
        <div style={{ position: "relative", maxWidth: 700, margin: "0 auto" }}>
          <div className="anim-heartbeat" style={{ fontSize: 60, marginBottom: "1rem", display: "inline-block" }}>💕</div>
          <h2 style={{ fontSize: "clamp(1.8rem, 4vw, 3rem)", fontWeight: 900, color: "white", marginBottom: "1rem" }}>
            Your Soulmate is Waiting
          </h2>
          <p style={{ fontSize: 16, color: "rgba(255,255,255,0.65)", marginBottom: "2.5rem", lineHeight: 1.7 }}>
            Join 5 million Indians already on their journey to happiness. Create your free profile in under 2 minutes.
          </p>
          <div style={{ display: "flex", gap: "1rem", justifyContent: "center", flexWrap: "wrap" }}>
            <Link href="/signup" style={{
              padding: "15px 40px",
              background: "linear-gradient(135deg, #e11d48, #c2185b)",
              color: "white", fontWeight: 800, fontSize: 16,
              borderRadius: 999, textDecoration: "none",
              boxShadow: "0 10px 40px rgba(225,29,72,0.5)",
            }}>Create Free Profile</Link>
            <Link href="/login" style={{
              padding: "15px 36px",
              background: "rgba(255,255,255,0.08)",
              border: "1.5px solid rgba(255,255,255,0.2)",
              color: "white", fontWeight: 700, fontSize: 16,
              borderRadius: 999, textDecoration: "none",
            }}>Sign In</Link>
          </div>
          <p style={{ marginTop: "1.5rem", fontSize: 12, color: "rgba(255,255,255,0.35)" }}>
            No credit card required · Free forever plan available
          </p>
        </div>
      </section>

      {/* ─── FOOTER ─── */}
      <footer style={{ background: "#0a061a", color: "#4b5563", padding: "3rem 1.5rem 2rem" }}>
        <div style={{ maxWidth: 1280, margin: "0 auto" }}>
          <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr 1fr 1fr", gap: "2rem", marginBottom: "2rem" }} className="footer-grid">
            <div>
              <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 12 }}>
                <div style={{ width: 30, height: 30, background: "linear-gradient(135deg,#e11d48,#be123c)", borderRadius: 9, display: "flex", alignItems: "center", justifyContent: "center", color: "white", fontWeight: 800, fontSize: 14 }}>M</div>
                <span style={{ fontWeight: 800, fontSize: 15, color: "white" }}>MatrimonyConnect</span>
              </div>
              <p style={{ fontSize: 12, lineHeight: 1.8, color: "#374151", maxWidth: 240 }}>India's most trusted AI-powered matrimony platform. Real people. Real marriages.</p>
            </div>
            {[
              { h: "Services", l: ["Hindu Matrimony", "Muslim Matrimony", "Christian Matrimony", "Sikh Matrimony", "NRI Matrimony"] },
              { h: "Company", l: ["About Us", "Success Stories", "Careers", "Press", "Blog"] },
              { h: "Support", l: ["Help Center", "Safety Tips", "Contact Us", "Privacy Policy", "Terms"] },
            ].map(col => (
              <div key={col.h}>
                <h4 style={{ fontSize: 11, fontWeight: 800, color: "white", letterSpacing: 2, textTransform: "uppercase", marginBottom: 12 }}>{col.h}</h4>
                <ul style={{ listStyle: "none" }}>
                  {col.l.map(l => <li key={l} style={{ marginBottom: 8 }}><a href="#" style={{ fontSize: 12, color: "#374151", textDecoration: "none" }}>{l}</a></li>)}
                </ul>
              </div>
            ))}
          </div>
          <div style={{ borderTop: "1px solid #111827", paddingTop: "1.5rem", display: "flex", justifyContent: "space-between", fontSize: 11, flexWrap: "wrap", gap: "0.5rem" }}>
            <p>© 2026 MatrimonyConnect Pvt. Ltd. All rights reserved.</p>
            <p>Made with ❤️ in India</p>
          </div>
        </div>
      </footer>

      <style>{`
        @media (max-width: 768px) {
          .grid-cols-hero { grid-template-columns: 1fr !important; }
          .grid-stats { grid-template-columns: repeat(2, 1fr) !important; }
          .footer-grid { grid-template-columns: 1fr 1fr !important; }
        }
      `}</style>
    </div>
  );
}

// ─── ROOT EXPORT ───
export default function Home() {
  const { user, logout, loading } = useAuth();

  if (loading) return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "100vh", flexDirection: "column", gap: 16 }}>
      <div style={{
        width: 48, height: 48, borderRadius: "50%",
        border: "4px solid #fce7f3", borderTopColor: "#e11d48",
        animation: "spin-slow 0.9s linear infinite",
      }}></div>
      <p style={{ color: "#94a3b8", fontWeight: 600, fontSize: 14 }}>Loading MatrimonyConnect...</p>
      <style>{`@keyframes spin-slow { to { transform: rotate(360deg); } }`}</style>
    </div>
  );

  return user ? <Dashboard user={user} logout={logout} /> : <LandingPage />;
}
