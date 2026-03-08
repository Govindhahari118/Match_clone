"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import PublicTopNav from "@/components/PublicTopNav";
import { APP_NAV_SECTIONS } from "@/config/navigation";
import { useAuth } from "@/context/AuthContext";

const TRUST_FEATURES = [
  {
    title: "Identity-first profiles",
    copy: "Verification checkpoints and moderation reduce fake accounts and low-intent messages.",
    metric: "1.8M verified",
  },
  {
    title: "Compatibility graph",
    copy: "Intent, lifestyle, and cultural preferences are weighted to surface stronger conversations.",
    metric: "220K monthly matches",
  },
  {
    title: "Family-friendly controls",
    copy: "Privacy, contact gating, and profile visibility can be tuned for personal or family-led journeys.",
    metric: "4.9/5 trust score",
  },
];

const PROCESS_STEPS = [
  {
    title: "Create a complete profile",
    copy: "Add personal details, values, and partner preferences once for better long-term recommendations.",
  },
  {
    title: "Prioritize quality matches",
    copy: "Use filters and compatibility scores to focus on relevant profiles instead of scrolling endlessly.",
  },
  {
    title: "Move safely to conversations",
    copy: "Start with interests, verify intent, then continue through protected chat and family introductions.",
  },
];

const STORY_SNIPPETS = [
  {
    names: "Aarav + Nisha",
    city: "Bengaluru",
    copy: "Started with values and profession filters, then families connected in under six weeks.",
  },
  {
    names: "Rohan + Kavya",
    city: "London",
    copy: "Verification badges helped both sides move from chat to clarity without uncertainty.",
  },
  {
    names: "Dev + Meera",
    city: "Hyderabad",
    copy: "Shortlists and shared preferences narrowed options quickly and led to one right match.",
  },
];

const LANDING_SIGNALS = [
  { value: "5M+", label: "Members" },
  { value: "220K+", label: "Monthly Matches" },
  { value: "1.8M", label: "Verified Profiles" },
];

const DASHBOARD_STATS = [
  { value: "18", label: "New Matches" },
  { value: "7", label: "Mutual Interests" },
  { value: "42", label: "Profile Views" },
  { value: "5", label: "Unread Chats" },
];

const DASHBOARD_ACTIONS = [
  { href: "/matches", title: "Review curated matches", copy: "Open compatibility-ranked recommendations for today." },
  { href: "/matches?filters=1", title: "Open advanced filters", copy: "Filter by city, profession, education, and lifestyle preferences." },
  { href: "/interests", title: "Manage interests", copy: "Respond to pending requests and revisit accepted connections." },
  { href: "/chat", title: "Continue conversations", copy: "Pick up active threads and message high-intent profiles." },
  { href: "/shortlists", title: "Refine shortlist", copy: "Compare favorites and narrow your top conversation candidates." },
  { href: "/who-viewed", title: "Check profile visitors", copy: "See who viewed your profile recently and who is most aligned." },
];

const DASHBOARD_RECOMMENDED = [
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
  const [ageMin, setAgeMin] = useState(24);
  const [ageMax, setAgeMax] = useState(31);
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
    <div className="page-shell landing-v2">
      <section className="landing-hero-v2">
        <div className="container-shell landing-shell-v2">
          <PublicTopNav compact />

          <div className="landing-grid-v2">
            <div className="landing-copy-v2 anim-rise delay-1">
              <p className="landing-kicker-v2">Trusted Global Matrimony Platform</p>
              <h1 className="landing-title-v2">
                Better Navigation.
                <br />
                Better Match Decisions.
              </h1>
              <p className="landing-subtitle-v2">
                Explore the full platform with a cleaner route system, stronger filters, and focused profile workflows.
                Every page is optimized for clarity across mobile and desktop.
              </p>

              <div className="landing-signals-v2">
                {LANDING_SIGNALS.map((item) => (
                  <article key={item.label} className="landing-signal-v2">
                    <strong>{item.value}</strong>
                    <span>{item.label}</span>
                  </article>
                ))}
              </div>

              <div className="landing-cta-row-v2">
                <Link className="button button-primary" href="/step-1">
                  Create Free Account
                </Link>
                <Link className="button button-secondary" href="/matches">
                  Explore Matches
                </Link>
              </div>
            </div>

            <aside className="panel landing-form-v2 anim-rise delay-2">
              <p className="section-label">Quick Start</p>
              <h2>Find profiles that fit your intent</h2>

              <form onSubmit={onSearch} className="landing-form-grid-v2">
                <label>
                  <span className="form-label">Looking for</span>
                  <select
                    className="form-input"
                    value={gender}
                    onChange={(event) => setGender(event.target.value)}
                  >
                    <option value="female">Woman</option>
                    <option value="male">Man</option>
                  </select>
                </label>

                <label>
                  <span className="form-label">Religion</span>
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
                </label>

                <div className="landing-age-row-v2">
                  <label>
                    <span className="form-label">Min age</span>
                    <input
                      className="form-input"
                      type="number"
                      min="18"
                      max="70"
                      value={ageMin}
                      onChange={(event) => setAgeMin(event.target.value)}
                    />
                  </label>
                  <label>
                    <span className="form-label">Max age</span>
                    <input
                      className="form-input"
                      type="number"
                      min="18"
                      max="70"
                      value={ageMax}
                      onChange={(event) => setAgeMax(event.target.value)}
                    />
                  </label>
                </div>

                <button type="submit" className="button button-primary landing-submit-v2">
                  Show Matches
                </button>
              </form>
            </aside>
          </div>
        </div>
      </section>

      <section className="section-block">
        <div className="container-shell">
          <p className="section-label">Why this redesign helps</p>
          <h2 className="section-title">A faster way to move from browsing to meaningful conversations</h2>

          <div className="trust-grid-v2">
            {TRUST_FEATURES.map((item) => (
              <article key={item.title} className="panel trust-card-v2 panel-hover anim-rise delay-1">
                <p className="trust-metric-v2">{item.metric}</p>
                <h3>{item.title}</h3>
                <p>{item.copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section-block section-muted-v2">
        <div className="container-shell">
          <p className="section-label">How it works</p>
          <h2 className="section-title">Three phases from profile setup to final shortlist</h2>

          <div className="process-grid-v2">
            {PROCESS_STEPS.map((item, index) => (
              <article key={item.title} className="panel process-card-v2 panel-hover anim-rise delay-2">
                <span className="process-index-v2">0{index + 1}</span>
                <h3>{item.title}</h3>
                <p>{item.copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section-block">
        <div className="container-shell">
          <p className="section-label">Platform map</p>
          <h2 className="section-title">Every major page, grouped by goal</h2>
          <div className="map-groups-v2">
            {APP_NAV_SECTIONS.map((section) => (
              <section key={section.title} className="panel map-group-v2">
                <p className="map-title-v2">{section.title}</p>
                <div className="map-links-v2">
                  {section.items.map((item) => (
                    <Link key={item.href} href={item.href} className="map-link-v2">
                      <span>{item.short}</span>
                      <strong>{item.label}</strong>
                    </Link>
                  ))}
                </div>
              </section>
            ))}
          </div>
        </div>
      </section>

      <section className="section-block section-muted-v2">
        <div className="container-shell">
          <p className="section-label">Recent outcomes</p>
          <h2 className="section-title">Members who moved quickly with focused navigation</h2>

          <div className="story-grid-v2">
            {STORY_SNIPPETS.map((story) => (
              <article key={story.names} className="panel story-card-v2 panel-hover">
                <p className="story-name-v2">{story.names}</p>
                <p className="story-city-v2">{story.city}</p>
                <p className="story-copy-v2">{story.copy}</p>
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
    <div className="page-shell dashboard-v2">
      <section className="dashboard-hero-v2">
        <div className="container-shell">
          <div className="panel dashboard-hero-card-v2">
            <div className="dashboard-identity-v2">
              <span className="dashboard-avatar-v2">{initial}</span>
              <div>
                <p className="dashboard-eyebrow-v2">Welcome back</p>
                <h1 className="dashboard-title-v2">{firstName}</h1>
                <p className="dashboard-subtitle-v2">Your workspace is refreshed with a faster route system and cleaner UI hierarchy.</p>
              </div>
            </div>

            <div className="dashboard-hero-actions-v2">
              <Link href="/matches" className="button button-primary">
                Open Matches
              </Link>
              <Link href="/profile" className="button button-secondary">
                Profile
              </Link>
              <button type="button" className="button button-secondary" onClick={logout}>
                Logout
              </button>
            </div>
          </div>

          <div className="dashboard-stats-v2">
            {DASHBOARD_STATS.map((item) => (
              <article key={item.label} className="panel dashboard-stat-card-v2">
                <strong>{item.value}</strong>
                <span>{item.label}</span>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="container-shell dashboard-body-v2">
        <div className="dashboard-grid-v2">
          <section className="panel dashboard-card-v2">
            <div className="dashboard-card-head-v2">
              <p className="section-label">Action Center</p>
              <Link href="/notifications" className="button button-secondary">
                Notifications
              </Link>
            </div>

            <div className="dashboard-actions-v2">
              {DASHBOARD_ACTIONS.map((action) => (
                <Link key={action.href} href={action.href} className="dashboard-action-link-v2">
                  <strong>{action.title}</strong>
                  <p>{action.copy}</p>
                </Link>
              ))}
            </div>
          </section>

          <section className="panel dashboard-card-v2">
            <div className="dashboard-card-head-v2">
              <p className="section-label">Recommended profiles</p>
              <Link href="/matches" className="button button-secondary">
                View all
              </Link>
            </div>

            <div className="profile-grid-v2">
              {DASHBOARD_RECOMMENDED.map((person) => (
                <article key={person.id} className="profile-card-v2">
                  <Image
                    src={person.photo}
                    alt={`${person.firstName} profile`}
                    width={640}
                    height={760}
                    sizes="(max-width: 760px) 100vw, (max-width: 1200px) 50vw, 25vw"
                    className="profile-photo-v2"
                  />
                  <div className="profile-content-v2">
                    <h3>
                      {person.firstName}, {person.age}
                    </h3>
                    <p>
                      {person.role} | {person.city}
                    </p>
                    <div className="profile-foot-v2">
                      <span className="chip chip-brand">{person.score}% Match</span>
                      <Link href={`/profile/${person.id}`} className="button button-secondary">
                        Open
                      </Link>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </section>
        </div>
      </section>
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
