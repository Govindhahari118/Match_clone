"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import PublicTopNav from "@/components/PublicTopNav";
import { APP_NAV_SECTIONS } from "@/config/navigation";
import { useAuth } from "@/context/AuthContext";

const whyChooseCards = [
  { icon: "PR", title: "Private by Default", desc: "You control profile visibility and who can contact you." },
  { icon: "VR", title: "Verified People", desc: "ID and profile checks reduce fake accounts and spam." },
  { icon: "AI", title: "Smart Matching", desc: "Compatibility-first recommendations based on intent and lifestyle." },
  { icon: "24", title: "Real Support", desc: "Human assistance for profile, safety, and subscription questions." },
];

const journeySteps = [
  { step: "01", title: "Create your profile", desc: "Add details once and get better-quality recommendations quickly." },
  { step: "02", title: "Discover and connect", desc: "Explore matches, send interests, and start meaningful chats." },
  { step: "03", title: "Move forward safely", desc: "Use verification, filters, and privacy controls with confidence." },
];

const storyCards = [
  { names: "Aarav & Nisha", city: "Bengaluru", copy: "Matched in 12 days, families met in 5 weeks." },
  { names: "Rohan & Kavya", city: "London", copy: "Started with values-first filters and built trust through chat." },
  { names: "Dev & Meera", city: "Hyderabad", copy: "Verified profiles helped both families decide faster." },
];

const dashboardStats = [
  { value: 18, label: "New Matches", tone: "#f06b4e" },
  { value: 7, label: "Mutual Interests", tone: "#1f8a9e" },
  { value: 42, label: "Profile Views", tone: "#334f86" },
  { value: 5, label: "Unread Chats", tone: "#c38a22" },
];

const dashboardActions = [
  { href: "/matches", title: "Explore Matches", desc: "Review today's compatibility recommendations." },
  { href: "/search", title: "Advanced Search", desc: "Filter by city, education, profession, and intent." },
  { href: "/who-viewed", title: "Who Viewed You", desc: "See recent visitors and priority viewers." },
  { href: "/interests", title: "Manage Interests", desc: "Track accepted, pending, and declined interests." },
  { href: "/shortlists", title: "Shortlist Board", desc: "Compare favorites side-by-side before deciding." },
  { href: "/chat", title: "Open Conversations", desc: "Continue active conversations with top matches." },
];

const dashboardShortcuts = [
  { href: "/matches", label: "Matches" },
  { href: "/search", label: "Search" },
  { href: "/interests", label: "Interests" },
  { href: "/shortlists", label: "Shortlists" },
  { href: "/notifications", label: "Alerts" },
  { href: "/profile", label: "Profile" },
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

const heroSignals = [
  { value: "5M+", label: "Members" },
  { value: "220K+", label: "Monthly Matches" },
  { value: "1.8M", label: "Verified Profiles" },
];

function LandingPage() {
  const router = useRouter();
  const [gender, setGender] = useState("female");
  const [ageMin, setAgeMin] = useState(20);
  const [ageMax, setAgeMax] = useState(30);
  const [religion, setReligion] = useState("Hindu");

  const onSearch = (event) => {
    event.preventDefault();
    router.push(
      `/matches?gender=${gender}&minAge=${ageMin}&maxAge=${ageMax}&religion=${encodeURIComponent(
        religion
      )}`
    );
  };

  return (
    <div className="page-shell premium-landing">
      <section className="hero-backdrop home-hero">
        <div className="container-shell home-shell">
          <PublicTopNav compact />

          <div className="hero-grid">
            <aside className="panel hero-form-card anim-rise delay-1">
              <p className="section-label" style={{ color: "#9f1239" }}>
                Quick Match Search
              </p>
              <h2>Find your special someone</h2>
              <form onSubmit={onSearch} className="hero-form-grid">
                <div className="hero-form-row">
                  <div>
                    <label className="form-label">I&apos;m looking for</label>
                    <select
                      className="form-input"
                      value={gender}
                      onChange={(event) => setGender(event.target.value)}
                    >
                      <option value="female">Woman</option>
                      <option value="male">Man</option>
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Religion</label>
                    <select
                      className="form-input"
                      value={religion}
                      onChange={(event) => setReligion(event.target.value)}
                    >
                      <option>Hindu</option>
                      <option>Muslim</option>
                      <option>Christian</option>
                      <option>Sikh</option>
                      <option>Jain</option>
                      <option>Buddhist</option>
                      <option>Any</option>
                    </select>
                  </div>
                </div>

                <div className="hero-form-row age-row">
                  <div>
                    <label className="form-label">Age</label>
                    <input
                      className="form-input"
                      type="number"
                      min="18"
                      max="70"
                      value={ageMin}
                      onChange={(event) => setAgeMin(event.target.value)}
                    />
                  </div>
                  <span className="age-separator">to</span>
                  <div>
                    <label className="form-label">&nbsp;</label>
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

                <button className="button home-primary-cta" type="submit">
                  Find Matches
                </button>
              </form>
            </aside>

            <div className="hero-copy anim-rise delay-2">
              <h1 className="hero-title">
                <span className="hero-line hero-line-1">Meaningful Matches.</span>
                <span className="hero-line hero-line-2">Clear Intent.</span>
                <span className="hero-line hero-line-3">Better Journeys.</span>
              </h1>
              <p className="hero-sub">
                MatrimonyConnect combines verified profiles, structured search, and focused
                communication so your partner search is faster and more reliable.
              </p>

              <div className="hero-proof-row">
                <div className="proof-avatars">
                  {[0, 1, 2, 3].map((dot) => (
                    <span key={dot} className="proof-avatar" />
                  ))}
                </div>
                <div>
                  <p className="proof-stars">*****</p>
                  <p className="proof-copy">Trusted by families worldwide</p>
                </div>
              </div>

              <div className="hero-signal-row">
                {heroSignals.map((signal) => (
                  <div key={signal.label} className="hero-signal-tile">
                    <p>{signal.value}</p>
                    <span>{signal.label}</span>
                  </div>
                ))}
              </div>

              <div className="hero-cta-row">
                <Link className="button button-primary" href="/step-1">
                  Start Free
                </Link>
                <Link className="button button-ghost-light" href="#all-pages">
                  Browse All Pages
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="section-block why-section">
        <div className="container-shell">
          <p className="section-label why-label">Why MatrimonyConnect</p>
          <div className="why-grid">
            {whyChooseCards.map((item, index) => (
              <article
                key={item.title}
                className={`panel why-card anim-rise delay-${Math.min(index + 1, 5)}`}
              >
                <span className="why-icon">{item.icon}</span>
                <h3>{item.title}</h3>
                <p>{item.desc}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section-block journey-section">
        <div className="container-shell">
          <p className="section-label">How It Works</p>
          <h2 className="section-title">Three steps to a stronger match journey</h2>
          <div className="journey-grid">
            {journeySteps.map((item, index) => (
              <article
                key={item.step}
                className={`panel journey-card anim-rise delay-${Math.min(index + 1, 5)}`}
              >
                <span className="journey-step">{item.step}</span>
                <h3>{item.title}</h3>
                <p>{item.desc}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section-block home-nav-directory" id="all-pages">
        <div className="container-shell">
          <p className="section-label">Page Navigation</p>
          <h2 className="section-title">All key pages in one place</h2>
          <p className="section-copy">
            Navigation buttons now include all important pages with a consistent visual style.
          </p>

          <div className="home-nav-sections">
            {APP_NAV_SECTIONS.map((section) => (
              <section key={section.title} className="panel home-nav-section">
                <p className="home-nav-title">{section.title}</p>
                <div className="home-nav-grid">
                  {section.items.map((item) => (
                    <Link key={item.href} href={item.href} className="button button-secondary home-nav-link">
                      <span className="home-nav-code">{item.short}</span>
                      <span>{item.label}</span>
                    </Link>
                  ))}
                </div>
              </section>
            ))}
          </div>
        </div>
      </section>

      <section className="section-block story-section">
        <div className="container-shell">
          <p className="section-label">Real Outcomes</p>
          <h2 className="section-title">Success stories from recent members</h2>

          <div className="story-grid">
            {storyCards.map((item, index) => (
              <article key={item.names} className={`panel story-card anim-rise delay-${Math.min(index + 1, 5)}`}>
                <p className="story-names">{item.names}</p>
                <p className="story-city">{item.city}</p>
                <p className="story-copy">{item.copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

function Dashboard({ user, logout }) {
  const firstName =
    user?.profile?.firstName || user?.firstName || user?.email?.split("@")[0] || "Member";
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
        <section className="panel" style={{ padding: "0.9rem" }}>
          <p className="section-label">Quick access</p>
          <div
            style={{
              marginTop: "0.65rem",
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))",
              gap: "0.5rem",
            }}
          >
            {dashboardShortcuts.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="button button-secondary"
                style={{ justifyContent: "center", minHeight: 40 }}
              >
                {item.label}
              </Link>
            ))}
          </div>
        </section>

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
                <p
                  style={{
                    margin: 0,
                    color: item.tone,
                    fontFamily: "var(--font-display)",
                    fontSize: "2rem",
                    lineHeight: 1,
                  }}
                >
                  {item.value}
                </p>
                <p style={{ margin: "0.32rem 0 0", color: "var(--ink-muted)", fontSize: "0.83rem" }}>
                  {item.label}
                </p>
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
                  sizes="(max-width: 760px) 100vw, (max-width: 1200px) 50vw, 25vw"
                  style={{
                    width: "100%",
                    height: 190,
                    borderTopLeftRadius: "var(--radius-md)",
                    borderTopRightRadius: "var(--radius-md)",
                    objectFit: "cover",
                  }}
                />
                <div style={{ padding: "0.8rem" }}>
                  <h3 style={{ margin: 0, fontSize: "1rem" }}>
                    {person.firstName}, {person.age}
                  </h3>
                  <p style={{ margin: "0.3rem 0 0", fontSize: "0.84rem", color: "var(--ink-muted)" }}>
                    {person.role} | {person.city}
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
  const { user, logout } = useAuth();

  if (user) {
    return <Dashboard user={user} logout={logout} />;
  }

  return <LandingPage />;
}
