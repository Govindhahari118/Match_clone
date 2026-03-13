"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import PublicTopNav from "@/components/PublicTopNav";
import { APP_NAV_SECTIONS } from "@/config/navigation";
import { useAuth } from "@/context/AuthContext";
import { STORY_SNIPPETS } from "@/components/successStoriesData";

const TRUST_FEATURES = [
  {
    title: "Self-attested profiles",
    copy: "Clear checklists and community guidelines keep intent high without manual ops.",
    metric: "Self-serve trust",
  },
  {
    title: "Rule-based matching",
    copy: "Filters and preference-based ranking surface stronger conversations without automation.",
    metric: "Transparent logic",
  },
  {
    title: "Privacy-first controls",
    copy: "Visibility, contact gating, and consent-based sharing keep members safe.",
    metric: "Privacy by default",
  },
];

const ASSISTED_BENEFITS = [
  {
    title: "Guided shortlist flow",
    copy: "Shortlist, compare, and revisit profiles in one place.",
  },
  {
    title: "Saved searches",
    copy: "Reuse preference sets to keep discovery fast and consistent.",
  },
  {
    title: "Consent-first outreach",
    copy: "Contact details stay hidden until mutual interest.",
  },
];

const LANDING_SIGNALS = [
  { value: "Zero-INR", label: "New features" },
  { value: "Zero-Infra", label: "Ops model" },
  { value: "No-AI", label: "Matching" },
];

const LANDING_BENEFITS = [
  "Profile completeness checklist",
  "Privacy-first visibility controls",
  "Rule-based matching and filters",
];

const PREMIUM_PACKAGES = [
  {
    title: "Starter",
    duration: "Free",
    copy: "Self-serve profiles, shortlists, and interest requests.",
    highlight: "Always free",
  },
  {
    title: "Plus",
    duration: "Monthly",
    copy: "Extra visibility windows and advanced filters.",
    highlight: "For active search",
  },
  {
    title: "Focus",
    duration: "Quarterly",
    copy: "Priority listing slots and unlimited shortlists.",
    highlight: "Best for speed",
  },
];

const APP_METRICS = [
  { value: "Zero-ops", label: "Operating model" },
  { value: "No-AI", label: "Matching logic" },
  { value: "Privacy-first", label: "Default controls" },
];

const APP_HIGHLIGHTS = [
  "Fast shortlist + interest flows",
  "Profile completeness nudges",
  "Family sharing and approvals",
];

const SUPPORT_CHANNELS = [
  {
    title: "Help center",
    copy: "Self-serve guides, FAQs, and onboarding tips.",
    action: { label: "Open help center", href: "/help" },
  },
  {
    title: "Community guidelines",
    copy: "Clear rules and reporting to keep intent high.",
    action: { label: "Read guidelines", href: "/community-guidelines" },
  },
  {
    title: "Feedback loop",
    copy: "Share feedback and feature requests directly.",
    action: { label: "Send feedback", href: "/help" },
  },
];

const SEGMENT_GROUPS = [
  {
    title: "By community",
    items: [
      { label: "Hindu", href: "/matches?religion=Hindu" },
      { label: "Muslim", href: "/matches?religion=Muslim" },
      { label: "Christian", href: "/matches?religion=Christian" },
      { label: "Sikh", href: "/matches?religion=Sikh" },
      { label: "Jain", href: "/matches?religion=Jain" },
    ],
  },
  {
    title: "By city",
    items: [
      { label: "Mumbai", href: "/matches?city=Mumbai" },
      { label: "Delhi", href: "/matches?city=Delhi" },
      { label: "Bengaluru", href: "/matches?city=Bengaluru" },
      { label: "Hyderabad", href: "/matches?city=Hyderabad" },
      { label: "Chennai", href: "/matches?city=Chennai" },
    ],
  },
  {
    title: "By language",
    items: [
      { label: "Hindi", href: "/matches?motherTongue=Hindi" },
      { label: "Tamil", href: "/matches?motherTongue=Tamil" },
      { label: "Telugu", href: "/matches?motherTongue=Telugu" },
      { label: "Marathi", href: "/matches?motherTongue=Marathi" },
      { label: "Gujarati", href: "/matches?motherTongue=Gujarati" },
    ],
  },
];

const FAQ_ITEMS = [
  {
    question: "How is trust built without manual verification?",
    answer: "A self-serve checklist, privacy controls, and reporting keep intent high.",
  },
  {
    question: "Can families manage profiles together?",
    answer: "Yes. Profiles can be created and managed by parents or family members.",
  },
  {
    question: "What if I want privacy?",
    answer: "Visibility, contact gating, and masked details are available in settings.",
  },
  {
    question: "Do you use AI for matching?",
    answer: "No. Matching and discovery are rule-based and transparent.",
  },
];

const DASHBOARD_STATS = [
  { value: "18", label: "New Matches" },
  { value: "7", label: "Mutual Interests" },
  { value: "42", label: "Profile Views" },
  { value: "5", label: "Unread Chats" },
];

const DASHBOARD_ACTIONS = [
  { href: "/matches", title: "Review curated matches", copy: "Review compatibility-ranked recommendations for today." },
  { href: "/matches?filters=1", title: "Use advanced filters", copy: "Filter by city, profession, education, and lifestyle preferences." },
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

const PUBLIC_MAP_SECTIONS = APP_NAV_SECTIONS.map((section) => ({
  ...section,
  items: section.items.filter((item) => item.href !== "/admin"),
})).filter((section) => section.items.length > 0);

const LANDING_DRAFT_KEY = "landingDraft";

function LandingPage() {
  const router = useRouter();
  const [createdFor, setCreatedFor] = useState("self");
  const [gender, setGender] = useState("");
  const [firstName, setFirstName] = useState("");
  const [phone, setPhone] = useState("");
  const [religion, setReligion] = useState("Hindu");
  const [formError, setFormError] = useState("");

  const onCreateProfile = (event) => {
    event.preventDefault();
    const trimmedName = firstName.trim();
    const trimmedPhone = phone.trim();
    if (!gender) {
      setFormError("Select a gender to continue.");
      return;
    }
    if (!trimmedName) {
      setFormError("Add your first name to continue.");
      return;
    }
    setFormError("");
    if (typeof window !== "undefined") {
      const draftPayload = {
        createdFor,
        firstName: trimmedName,
        gender,
        religion,
      };
      if (trimmedPhone) draftPayload.phone = trimmedPhone;
      window.sessionStorage.setItem(
        LANDING_DRAFT_KEY,
        JSON.stringify(draftPayload)
      );
    }
    router.push("/step-1");
  };

  return (
    <div className="page-shell home-shell-heirloom">
      <section className="home-hero-heirloom">
        <div className="container-shell home-hero-container">
          <PublicTopNav compact showBack={false} />

          <div className="home-hero-grid">
            <div className="home-hero-copy anim-rise delay-1">
              <p className="home-kicker">Trusted Global Matrimony Platform</p>
              <h1 className="home-hero-title">
                <span>Find the</span>
                <span>right match</span>
                <span>with clarity.</span>
              </h1>
              <p className="home-hero-subtitle">
                Privacy-first profiles, rule-based matching, and family-friendly controls. Built for serious
                matchmaking across mobile and desktop.
              </p>

              <ul className="home-hero-list">
                {LANDING_BENEFITS.map((benefit) => (
                  <li key={benefit}>{benefit}</li>
                ))}
              </ul>

              <div className="home-signal-row">
                {LANDING_SIGNALS.map((item) => (
                  <article key={item.label} className="home-signal-card">
                    <strong>{item.value}</strong>
                    <span>{item.label}</span>
                  </article>
                ))}
              </div>

              <div className="home-cta-row">
                <Link className="button button-primary" href="/step-1">
                  Start Free
                </Link>
                <Link className="button button-secondary" href="/matches">
                  Browse Matches
                </Link>
              </div>
              <p className="home-hero-note">Self-serve onboarding. Pause or switch anytime.</p>
            </div>

            <aside className="panel home-quickstart-card anim-rise delay-2">
              <div className="home-quickstart-head">
                <p className="section-label">Create your profile</p>
                <h2 className="home-quickstart-title">Start with the essentials</h2>
                <p className="home-quickstart-copy">
                  Start with the essentials. Add contact details later and control visibility in settings.
                </p>
              </div>

              <form onSubmit={onCreateProfile} className="home-quickstart-form">
                <label>
                  <span className="form-label">Profile created for</span>
                  <select
                    className="form-input"
                    value={createdFor}
                    onChange={(event) => setCreatedFor(event.target.value)}
                  >
                    <option value="self">Myself</option>
                    <option value="daughter">Daughter</option>
                    <option value="son">Son</option>
                    <option value="sibling">Sibling</option>
                    <option value="friend">Friend</option>
                  </select>
                </label>

                <label>
                  <span className="form-label">Gender</span>
                  <select
                    className="form-input"
                    value={gender}
                    onChange={(event) => setGender(event.target.value)}
                  >
                    <option value="">Select gender</option>
                    <option value="female">Female</option>
                    <option value="male">Male</option>
                    <option value="other">Other</option>
                  </select>
                </label>

                <label>
                  <span className="form-label">First name</span>
                  <input
                    className="form-input"
                    value={firstName}
                    onChange={(event) => setFirstName(event.target.value)}
                    placeholder="e.g. Aanya"
                  />
                </label>

                <label>
                  <span className="form-label">Contact number (optional)</span>
                  <input
                    className="form-input"
                    type="tel"
                    value={phone}
                    onChange={(event) => setPhone(event.target.value)}
                    placeholder="+91 90000 00000"
                  />
                </label>

                <label>
                  <span className="form-label">Religion preference</span>
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

                <button type="submit" className="button button-primary cta-full">
                  Create Profile
                </button>
              </form>
              {formError && <p className="form-error" role="alert">{formError}</p>}

              <div className="home-quickstart-foot">
                {LANDING_BENEFITS.map((item) => (
                  <span key={item} className="home-mini-chip">
                    {item}
                  </span>
                ))}
              </div>
              <p className="home-quickstart-note">You can add or hide contact details anytime.</p>
            </aside>
          </div>
        </div>
      </section>

      <section className="home-section">
        <div className="container-shell">
          <div className="home-section-head">
            <p className="section-label">Trust and transparency</p>
            <h2 className="section-title">Safety-first signals at every step</h2>
            <p className="section-copy">
              Structured trust signals keep profiles authentic without manual ops or hidden scoring.
            </p>
          </div>

          <div className="home-trust-grid">
            {TRUST_FEATURES.map((item) => (
              <article key={item.title} className="panel home-card home-trust-card">
                <span className="home-metric-pill">{item.metric}</span>
                <h3>{item.title}</h3>
                <p>{item.copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="home-section home-section-muted">
        <div className="container-shell">
          <div className="home-section-head">
            <p className="section-label">Self-serve matchmaking</p>
            <h2 className="section-title">Clear steps without manual ops</h2>
            <p className="section-copy">
              Move from discovery to conversation with a crisp, guided path.
            </p>
          </div>

          <div className="home-steps-grid">
            {ASSISTED_BENEFITS.map((item, index) => (
              <article key={item.title} className="panel home-card home-step-card">
                <span className="home-step-index">{String(index + 1).padStart(2, "0")}</span>
                <h3>{item.title}</h3>
                <p>{item.copy}</p>
              </article>
            ))}
          </div>
          <div className="home-cta-row home-cta-row-centered">
            <Link className="button button-primary" href="/pricing">
              Explore Plans
            </Link>
            <Link className="button button-secondary" href="/help">
              See how it works
            </Link>
          </div>
        </div>
      </section>

      <section className="home-section">
        <div className="container-shell">
          <div className="home-section-head">
            <p className="section-label">Self-serve plans</p>
            <h2 className="section-title">Choose a plan that fits your pace</h2>
          </div>

          <div className="home-plan-grid">
            {PREMIUM_PACKAGES.map((plan, index) => (
              <article
                key={plan.title}
                className={`panel home-card home-plan-card ${index === 1 ? "is-featured" : ""}`}
              >
                <div className="home-plan-head">
                  <h3>{plan.title}</h3>
                  <span className="home-plan-duration">{plan.duration}</span>
                </div>
                <p>{plan.copy}</p>
                <span className="chip chip-brand">{plan.highlight}</span>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="home-section home-section-muted">
        <div className="container-shell">
          <div className="home-split">
            <div className="home-split-copy">
              <p className="section-label">Mobile first</p>
              <h2 className="section-title">Take Match wherever you go</h2>
              <p className="section-copy">
                Shortlists, interest requests, and family updates from your phone. Stay in control on every device.
              </p>

              <div className="home-signal-row home-signal-row-compact">
                {APP_METRICS.map((metric) => (
                  <article key={metric.label} className="home-signal-card">
                    <strong>{metric.value}</strong>
                    <span>{metric.label}</span>
                  </article>
                ))}
              </div>

              <div className="home-cta-row">
                <Link className="button button-primary" href="/help">
                  Get app link
                </Link>
                <Link className="button button-secondary" href="/help">
                  View mobile features
                </Link>
              </div>
            </div>

            <aside className="panel home-card home-split-card">
              <p className="section-label">Why members love the app</p>
              <ul className="home-highlight-list">
                {APP_HIGHLIGHTS.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </aside>
          </div>
        </div>
      </section>

      <section className="home-section">
        <div className="container-shell">
          <div className="home-section-head">
            <p className="section-label">Support</p>
            <h2 className="section-title">Guidance for every stage</h2>
          </div>
          <div className="home-support-grid">
            {SUPPORT_CHANNELS.map((channel) => (
              <article key={channel.title} className="panel home-card home-support-card">
                <h3>{channel.title}</h3>
                <p>{channel.copy}</p>
                <Link className="button button-secondary" href={channel.action.href}>
                  {channel.action.label}
                </Link>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="home-section home-section-muted">
        <div className="container-shell">
          <div className="home-section-head">
            <p className="section-label">Browse by community</p>
            <h2 className="section-title">Start with what matters most</h2>
          </div>
          <div className="home-segment-grid">
            {SEGMENT_GROUPS.map((group) => (
              <article key={group.title} className="panel home-card home-segment-card">
                <h3>{group.title}</h3>
                <div className="home-segment-chips">
                  {group.items.map((item) => (
                    <Link key={item.href} href={item.href} className="home-segment-chip">
                      {item.label}
                    </Link>
                  ))}
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="home-section">
        <div className="container-shell">
          <div className="home-section-head">
            <p className="section-label">Platform map</p>
            <h2 className="section-title">Every major page, grouped by goal</h2>
          </div>
          <div className="home-map-grid">
            {PUBLIC_MAP_SECTIONS.map((section) => (
              <section key={section.title} className="panel home-card home-map-group">
                <p className="home-map-title">{section.title}</p>
                <div className="home-map-links">
                  {section.items.map((item) => (
                    <Link key={item.href} href={item.href} className="home-map-link">
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

      <section className="home-section home-section-muted">
        <div className="container-shell">
          <div className="home-section-head">
            <p className="section-label">Recent outcomes</p>
            <h2 className="section-title">Members who moved quickly with focused navigation</h2>
          </div>

          <div className="home-story-grid">
            {STORY_SNIPPETS.map((story) => (
              <article key={story.names} className="panel home-card home-story-card">
                <p className="home-story-name">{story.names}</p>
                <p className="home-story-city">{story.city}</p>
                <p className="home-story-copy">{story.copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="home-section">
        <div className="container-shell">
          <div className="home-section-head">
            <p className="section-label">FAQ</p>
            <h2 className="section-title">Answers to common questions</h2>
          </div>
          <div className="home-faq-grid">
            {FAQ_ITEMS.map((item) => (
              <article key={item.question} className="panel home-card home-faq-card">
                <h3>{item.question}</h3>
                <p>{item.answer}</p>
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
                Browse Matches
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
                        View Profile
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
