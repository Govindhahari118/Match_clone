"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { useAuth } from "../context/AuthContext";
import { useEffect, useMemo, useState } from "react";

const landingStats = [
  { value: "5M+", label: "Global Members" },
  { value: "420K+", label: "Success Stories" },
  { value: "98%", label: "Verified Profiles" },
  { value: "24/7", label: "Human Support" },
];

const featureBlocks = [
  {
    icon: "AI",
    title: "Compatibility Intelligence",
    desc: "Deep preference scoring built across lifestyle, family values, intent, and communication style.",
  },
  {
    icon: "ID",
    title: "Identity Verification",
    desc: "Multi-layer profile checks and fraud filters help you spend time only on genuine connections.",
  },
  {
    icon: "PR",
    title: "Private by Design",
    desc: "Control who sees your details. Contact sharing remains hidden until mutual consent.",
  },
  {
    icon: "RT",
    title: "Real-time Conversations",
    desc: "Start secure chats instantly when both sides show interest and keep your journey moving.",
  },
  {
    icon: "GL",
    title: "International Reach",
    desc: "Discover matches across India, UK, US, UAE, Canada, Singapore, and more diaspora communities.",
  },
  {
    icon: "CX",
    title: "Premium Concierge",
    desc: "Optional managed matching support for busy professionals and families.",
  },
];

const processSteps = [
  {
    number: "01",
    title: "Build Your Profile",
    desc: "Add your story, preferences, and intent so your matches are relevant from day one.",
  },
  {
    number: "02",
    title: "Get Curated Matches",
    desc: "Receive a daily shortlist ranked by compatibility and profile trust score.",
  },
  {
    number: "03",
    title: "Connect Securely",
    desc: "Express interest, unlock conversation, and evaluate each profile at your pace.",
  },
  {
    number: "04",
    title: "Move Forward Confidently",
    desc: "Use verified insights and family-friendly workflows to transition from chat to commitment.",
  },
];

const stories = [
  {
    couple: "Arjun & Neha",
    location: "London",
    quote:
      "The profile quality felt very different. We found clarity quickly and spoke with complete confidence.",
  },
  {
    couple: "Rohan & Aisha",
    location: "Dubai",
    quote:
      "The compatibility insights matched our real conversations surprisingly well. It saved us months.",
  },
  {
    couple: "Vikram & Kavya",
    location: "Bengaluru",
    quote:
      "Professional UX and verified members made this the first platform our families trusted together.",
  },
];

const dashboardStats = [
  { value: 18, label: "New Matches", tone: "#f06b4e" },
  { value: 7, label: "Mutual Interests", tone: "#1f8a9e" },
  { value: 42, label: "Profile Views", tone: "#334f86" },
  { value: 5, label: "Unread Chats", tone: "#c38a22" },
];

const dashboardActions = [
  {
    href: "/matches",
    title: "Explore Matches",
    desc: "Review your latest compatibility shortlist.",
  },
  {
    href: "/search",
    title: "Advanced Search",
    desc: "Filter by location, profession, language, and intent.",
  },
  {
    href: "/who-viewed",
    title: "Profile Views",
    desc: "See who recently visited your profile.",
  },
  {
    href: "/kundli",
    title: "Kundli Matching",
    desc: "Compare horoscope compatibility in minutes.",
  },
  {
    href: "/interests",
    title: "Manage Interests",
    desc: "Track sent, received, and accepted interests.",
  },
  {
    href: "/chat",
    title: "Open Conversations",
    desc: "Continue meaningful chats with your top matches.",
  },
  {
    href: "/shortlists",
    title: "Shortlisted Profiles",
    desc: "Revisit your favorites and compare details.",
  },
  {
    href: "/profile",
    title: "Refine Profile",
    desc: "Improve profile quality for better recommendations.",
  },
];

const recommended = [
  {
    id: "u1",
    firstName: "Ananya",
    age: 27,
    city: "Mumbai",
    role: "Product Strategist",
    score: 94,
    photo: "https://randomuser.me/api/portraits/women/45.jpg",
  },
  {
    id: "u2",
    firstName: "Ishita",
    age: 26,
    city: "Toronto",
    role: "Data Scientist",
    score: 91,
    photo: "https://randomuser.me/api/portraits/women/68.jpg",
  },
  {
    id: "u3",
    firstName: "Rhea",
    age: 28,
    city: "Singapore",
    role: "Corporate Lawyer",
    score: 89,
    photo: "https://randomuser.me/api/portraits/women/75.jpg",
  },
  {
    id: "u4",
    firstName: "Meera",
    age: 29,
    city: "Hyderabad",
    role: "Architect",
    score: 87,
    photo: "https://randomuser.me/api/portraits/women/57.jpg",
  },
];

function LandingPage() {
  const router = useRouter();
  const [gender, setGender] = useState("female");
  const [ageMin, setAgeMin] = useState(25);
  const [ageMax, setAgeMax] = useState(32);
  const [region, setRegion] = useState("India");
  const [activeStory, setActiveStory] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setActiveStory((prev) => (prev + 1) % stories.length);
    }, 4800);

    return () => clearInterval(timer);
  }, []);

  const onSearch = (event) => {
    event.preventDefault();
    router.push(
      `/matches?gender=${gender}&minAge=${ageMin}&maxAge=${ageMax}&region=${encodeURIComponent(
        region
      )}`
    );
  };

  return (
    <div className="page-shell">
      <section className="hero-backdrop">
        <div className="container-shell" style={{ padding: "1.2rem 0 4.3rem" }}>
          <header
            className="panel"
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              padding: "0.7rem 0.95rem",
              background: "rgba(12,22,44,0.42)",
              borderColor: "rgba(255,255,255,0.22)",
              color: "#f3f7ff",
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 10,
                  background: "linear-gradient(135deg, #f06b4e, #d75136)",
                  display: "grid",
                  placeItems: "center",
                  fontWeight: 800,
                  fontSize: 14,
                }}
              >
                M
              </div>
              <strong style={{ letterSpacing: "0.02em" }}>MatrimonyConnect</strong>
            </div>

            <nav style={{ display: "flex", alignItems: "center", gap: "0.45rem" }}>
              <Link className="button button-ghost-light" href="/login">
                Sign In
              </Link>
              <Link className="button button-primary" href="/signup">
                Create Profile
              </Link>
            </nav>
          </header>

          <div
            className="anim-rise"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(290px, 1fr))",
              gap: "1.2rem",
              marginTop: "2rem",
            }}
          >
            <div>
              <p className="section-label" style={{ color: "#ffd3c8" }}>
                International. Trusted. Premium.
              </p>
              <h1
                className="section-title"
                style={{ color: "#f7fbff", maxWidth: 700, marginTop: "0.9rem" }}
              >
                A Professional Matrimony Experience For Modern Families
              </h1>
              <p className="section-copy" style={{ color: "rgba(243,248,255,0.84)" }}>
                Discover serious matches with strong profile verification, compatibility scoring,
                and an interface designed to feel elegant on every device.
              </p>

              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.6rem",
                  marginTop: "1.2rem",
                  flexWrap: "wrap",
                }}
              >
                <span className="chip" style={{ color: "#0f6d7f", background: "#dcf2f5" }}>
                  Verified Profiles
                </span>
                <span className="chip" style={{ color: "#9a3f2b", background: "#ffe3db" }}>
                  AI Matching
                </span>
                <span className="chip" style={{ color: "#314f88", background: "#e4ecff" }}>
                  Privacy Controls
                </span>
              </div>

              <div
                style={{
                  marginTop: "1.4rem",
                  display: "grid",
                  gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
                  gap: "0.75rem",
                }}
              >
                {landingStats.map((item, index) => (
                  <div key={item.label} className={`stat-tile anim-rise delay-${Math.min(index + 1, 5)}`}>
                    <p className="stat-value" style={{ color: "#10203c" }}>
                      {item.value}
                    </p>
                    <p className="stat-label">{item.label}</p>
                  </div>
                ))}
              </div>
            </div>

            <div className="panel anim-rise delay-2" style={{ padding: "1rem" }}>
              <p className="section-label" style={{ marginBottom: "0.7rem" }}>
                Start your search
              </p>
              <form onSubmit={onSearch} style={{ display: "grid", gap: "0.82rem" }}>
                <div>
                  <label className="form-label">Looking for</label>
                  <select
                    className="form-input"
                    value={gender}
                    onChange={(event) => setGender(event.target.value)}
                  >
                    <option value="female">Bride</option>
                    <option value="male">Groom</option>
                  </select>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.55rem" }}>
                  <div>
                    <label className="form-label">Min age</label>
                    <input
                      className="form-input"
                      type="number"
                      min="18"
                      max="70"
                      value={ageMin}
                      onChange={(event) => setAgeMin(event.target.value)}
                    />
                  </div>
                  <div>
                    <label className="form-label">Max age</label>
                    <input
                      className="form-input"
                      type="number"
                      min="18"
                      max="70"
                      value={ageMax}
                      onChange={(event) => setAgeMax(event.target.value)}
                    />
                  </div>
                </div>

                <div>
                  <label className="form-label">Region</label>
                  <select
                    className="form-input"
                    value={region}
                    onChange={(event) => setRegion(event.target.value)}
                  >
                    <option>India</option>
                    <option>United States</option>
                    <option>United Kingdom</option>
                    <option>Canada</option>
                    <option>United Arab Emirates</option>
                    <option>Australia</option>
                    <option>Singapore</option>
                  </select>
                </div>

                <button className="button button-primary" type="submit" style={{ width: "100%" }}>
                  View Compatible Matches
                </button>
              </form>

              <p style={{ marginTop: "0.78rem", fontSize: "0.78rem", color: "var(--ink-muted)" }}>
                Free registration. No card required.
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="section-block">
        <div className="container-shell">
          <p className="section-label">Built for trust and clarity</p>
          <h2 className="section-title">Product-Grade UX For One Of Life&apos;s Biggest Decisions</h2>
          <p className="section-copy">
            A modern interaction model built to reduce noise, increase confidence, and help
            families move from browsing to meaningful conversations faster.
          </p>

          <div
            style={{
              marginTop: "1.6rem",
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
              gap: "0.9rem",
            }}
          >
            {featureBlocks.map((feature, index) => (
              <article key={feature.title} className={`feature-card anim-rise delay-${Math.min(index + 1, 5)}`}>
                <div className="feature-icon">{feature.icon}</div>
                <h3 style={{ margin: "0.72rem 0 0", fontSize: "1.03rem" }}>{feature.title}</h3>
                <p style={{ margin: "0.45rem 0 0", color: "var(--ink-muted)", fontSize: "0.9rem" }}>
                  {feature.desc}
                </p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section-block" style={{ background: "rgba(255,255,255,0.65)" }}>
        <div className="container-shell">
          <p className="section-label">Simple workflow</p>
          <h2 className="section-title">Four Clear Steps To Find The Right Match</h2>

          <div
            style={{
              marginTop: "1.5rem",
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
              gap: "0.85rem",
            }}
          >
            {processSteps.map((step, index) => (
              <div key={step.number} className={`panel panel-hover anim-rise delay-${Math.min(index + 1, 5)}`} style={{ padding: "1rem" }}>
                <div className="chip chip-support">Step {step.number}</div>
                <h3 style={{ margin: "0.78rem 0 0", fontSize: "1.02rem" }}>{step.title}</h3>
                <p style={{ margin: "0.42rem 0 0", color: "var(--ink-muted)", fontSize: "0.9rem" }}>
                  {step.desc}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="section-block">
        <div className="container-shell">
          <p className="section-label">Success stories</p>
          <h2 className="section-title">Real Couples. Real Outcomes.</h2>

          <div className="panel" style={{ marginTop: "1.3rem", padding: "1.2rem" }}>
            <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.95rem" }}>
              {stories[activeStory].location}
            </p>
            <h3 style={{ margin: "0.4rem 0 0", fontSize: "1.4rem", fontFamily: "var(--font-display)" }}>
              {stories[activeStory].couple}
            </h3>
            <p
              style={{
                margin: "0.7rem 0 0",
                fontSize: "1rem",
                maxWidth: 780,
                color: "var(--ink)",
              }}
            >
              &ldquo;{stories[activeStory].quote}&rdquo;
            </p>

            <div style={{ marginTop: "0.95rem", display: "flex", gap: "0.45rem" }}>
              {stories.map((item, index) => (
                <button
                  key={item.couple}
                  type="button"
                  onClick={() => setActiveStory(index)}
                  aria-label={`View story ${index + 1}`}
                  style={{
                    width: activeStory === index ? 24 : 8,
                    height: 8,
                    borderRadius: 999,
                    border: "none",
                    background: activeStory === index ? "var(--brand)" : "#cfd8e8",
                    cursor: "pointer",
                    transition: "all .2s ease",
                  }}
                />
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="hero-backdrop" style={{ padding: "4rem 0" }}>
        <div className="container-shell" style={{ textAlign: "center" }}>
          <p className="section-label" style={{ color: "#ffd3c8" }}>
            Ready to begin
          </p>
          <h2 className="section-title" style={{ color: "#f7fbff", marginTop: "0.75rem" }}>
            Upgrade Your Matrimony Experience
          </h2>
          <p className="section-copy" style={{ color: "rgba(247,251,255,0.82)", marginInline: "auto" }}>
            Join a platform that feels international, professional, and intentional from first click.
          </p>
          <div style={{ marginTop: "1rem", display: "flex", justifyContent: "center", gap: "0.6rem", flexWrap: "wrap" }}>
            <Link className="button button-primary" href="/signup">
              Create Free Profile
            </Link>
            <Link className="button button-ghost-light" href="/login">
              Sign In
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

function Dashboard({ user, logout }) {
  const firstName =
    user?.profile?.firstName ||
    user?.firstName ||
    user?.email?.split("@")[0] ||
    "Member";

  const initial = useMemo(() => firstName.charAt(0).toUpperCase(), [firstName]);

  return (
    <div className="page-shell">
      <section className="hero-backdrop" style={{ padding: "1rem 0 2.5rem" }}>
        <div className="container-shell">
          <header
            className="panel"
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: "0.8rem",
              padding: "0.74rem 0.9rem",
              background: "rgba(12,22,44,0.44)",
              borderColor: "rgba(255,255,255,0.22)",
              color: "#f6fbff",
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: "0.65rem" }}>
              <div
                style={{
                  width: 34,
                  height: 34,
                  borderRadius: 10,
                  display: "grid",
                  placeItems: "center",
                  fontWeight: 800,
                  background: "linear-gradient(135deg, #f06b4e, #d75136)",
                }}
              >
                {initial}
              </div>
              <div>
                <p style={{ margin: 0, fontSize: "0.85rem", opacity: 0.78 }}>Welcome back</p>
                <p style={{ margin: 0, fontWeight: 700 }}>{firstName}</p>
              </div>
            </div>

            <div style={{ display: "flex", gap: "0.45rem", flexWrap: "wrap" }}>
              <Link href="/profile" className="button button-ghost-light">
                Profile
              </Link>
              <button type="button" onClick={logout} className="button button-primary">
                Logout
              </button>
            </div>
          </header>
        </div>
      </section>

      <main className="container-shell" style={{ marginTop: "-1.7rem", paddingBottom: "2rem" }}>
        <section className="panel" style={{ padding: "1rem" }}>
          <p className="section-label">Dashboard overview</p>
          <h1 className="section-title" style={{ fontSize: "clamp(1.8rem,4vw,2.6rem)", marginTop: "0.5rem" }}>
            Your Match Activity
          </h1>

          <div
            style={{
              marginTop: "1rem",
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
              gap: "0.7rem",
            }}
          >
            {dashboardStats.map((item, index) => (
              <div
                key={item.label}
                className={`panel anim-rise delay-${Math.min(index + 1, 5)}`}
                style={{ padding: "0.9rem", borderColor: `${item.tone}33` }}
              >
                <p style={{ margin: 0, color: item.tone, fontFamily: "var(--font-display)", fontSize: "2rem", lineHeight: 1 }}>
                  {item.value}
                </p>
                <p style={{ margin: "0.32rem 0 0", color: "var(--ink-muted)", fontSize: "0.83rem" }}>{item.label}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="section-block" style={{ paddingBottom: "1.2rem" }}>
          <p className="section-label">Quick actions</p>
          <div
            style={{
              marginTop: "0.8rem",
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
              gap: "0.72rem",
            }}
          >
            {dashboardActions.map((action, index) => (
              <Link
                key={action.href}
                href={action.href}
                className={`panel panel-hover anim-rise delay-${Math.min(index + 1, 5)}`}
                style={{ padding: "0.95rem", textDecoration: "none" }}
              >
                <h3 style={{ margin: 0, fontSize: "1rem" }}>{action.title}</h3>
                <p style={{ margin: "0.4rem 0 0", color: "var(--ink-muted)", fontSize: "0.88rem" }}>
                  {action.desc}
                </p>
              </Link>
            ))}
          </div>
        </section>

        <section className="section-block" style={{ paddingTop: "1.2rem" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "0.8rem" }}>
            <div>
              <p className="section-label">Today&apos;s highlights</p>
              <h2 className="section-title" style={{ fontSize: "clamp(1.7rem,3.8vw,2.3rem)", marginTop: "0.45rem" }}>
                Recommended Profiles
              </h2>
            </div>
            <Link href="/matches" className="button button-secondary">
              View All
            </Link>
          </div>

          <div
            style={{
              marginTop: "0.9rem",
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
              gap: "0.75rem",
            }}
          >
            {recommended.map((person, index) => (
              <article key={person.id} className={`panel panel-hover anim-rise delay-${Math.min(index + 1, 5)}`}>
                <Image
                  src={person.photo}
                  alt={`${person.firstName} profile`}
                  width={640}
                  height={760}
                  style={{
                    width: "100%",
                    height: 190,
                    borderTopLeftRadius: "var(--radius-md)",
                    borderTopRightRadius: "var(--radius-md)",
                    objectFit: "cover",
                  }}
                  unoptimized
                />
                <div style={{ padding: "0.8rem" }}>
                  <h3 style={{ margin: 0, fontSize: "1rem" }}>
                    {person.firstName}, {person.age}
                  </h3>
                  <p style={{ margin: "0.3rem 0 0", fontSize: "0.84rem", color: "var(--ink-muted)" }}>
                    {person.role} · {person.city}
                  </p>
                  <div style={{ marginTop: "0.55rem", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <span className="chip chip-brand">{person.score}% Match</span>
                    <Link href={`/profile/${person.id}`} className="button button-secondary" style={{ padding: "0.42rem 0.8rem", fontSize: "0.8rem" }}>
                      Open
                    </Link>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}

export default function HomePage() {
  const { user, logout, loading } = useAuth();

  if (loading) {
    return (
      <div className="auth-shell">
        <div className="auth-card" style={{ textAlign: "center" }}>
          <p className="section-label">Loading</p>
          <h1 className="auth-title">Preparing your experience</h1>
          <p className="auth-subtitle">Please wait a moment while we sync your profile.</p>
        </div>
      </div>
    );
  }

  return user ? <Dashboard user={user} logout={logout} /> : <LandingPage />;
}
