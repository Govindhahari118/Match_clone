"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";

const STORIES = [
  {
    id: "s1",
    coupleNames: "Rahul and Priya",
    marriageYear: 2024,
    photo1: "https://randomuser.me/api/portraits/men/32.jpg",
    photo2: "https://randomuser.me/api/portraits/women/44.jpg",
    city: "Mumbai",
    story:
      "We matched in early 2024 and built trust through daily conversations. Within six months, both families met and we got married in December.",
    tag: "Love Story",
    days: 45,
  },
  {
    id: "s2",
    coupleNames: "Arjun and Sneha",
    marriageYear: 2024,
    photo1: "https://randomuser.me/api/portraits/men/33.jpg",
    photo2: "https://randomuser.me/api/portraits/women/45.jpg",
    city: "Bangalore",
    story:
      "We started with shared values and career goals. The connection was natural from day one and we completed our engagement in three months.",
    tag: "Family First",
    days: 120,
  },
  {
    id: "s3",
    coupleNames: "Vikram and Kavya",
    marriageYear: 2023,
    photo1: "https://randomuser.me/api/portraits/men/34.jpg",
    photo2: "https://randomuser.me/api/portraits/women/46.jpg",
    city: "Hyderabad",
    story:
      "Compatibility was strong and communication was easy. Our first video call set the tone and we moved confidently towards marriage.",
    tag: "Quick Match",
    days: 90,
  },
  {
    id: "s4",
    coupleNames: "Aditya and Meera",
    marriageYear: 2024,
    photo1: "https://randomuser.me/api/portraits/men/35.jpg",
    photo2: "https://randomuser.me/api/portraits/women/47.jpg",
    city: "Delhi",
    story:
      "We were in different cities but discovered common roots and similar priorities. The platform helped us stay connected and move quickly.",
    tag: "Location Match",
    days: 200,
  },
  {
    id: "s5",
    coupleNames: "Suresh and Anita",
    marriageYear: 2023,
    photo1: "https://randomuser.me/api/portraits/men/36.jpg",
    photo2: "https://randomuser.me/api/portraits/women/48.jpg",
    city: "Chennai",
    story:
      "Both of us were entering a second chapter in life. Honest conversations and emotional maturity helped us form a strong partnership.",
    tag: "Second Chance",
    days: 300,
  },
  {
    id: "s6",
    coupleNames: "Nikhil and Shruti",
    marriageYear: 2024,
    photo1: "https://randomuser.me/api/portraits/men/37.jpg",
    photo2: "https://randomuser.me/api/portraits/women/49.jpg",
    city: "Pune",
    story:
      "We navigated long-distance with structured conversations and regular calls. That consistency turned into confidence and commitment.",
    tag: "NRI Journey",
    days: 160,
  },
];

const STATS = [
  { label: "Happy Couples", value: "50,000+" },
  { label: "Marriages in 2024", value: "12,000+" },
  { label: "Average Days to Match", value: "47" },
  { label: "States Covered", value: "28" },
];

export default function SuccessStoriesPage() {
  const [expanded, setExpanded] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name1: "", name2: "", city: "", story: "", email: "" });
  const [submitted, setSubmitted] = useState(false);

  return (
    <div style={{ display: "grid", gap: "1rem" }}>
      <section
        className="panel"
        style={{
          padding: "1.4rem",
          background: "linear-gradient(135deg, #172554, #1d4ed8, #0f766e)",
          color: "white",
          borderColor: "rgba(255,255,255,0.18)",
        }}
      >
        <p className="section-label" style={{ color: "rgba(255,255,255,0.78)" }}>
          Community Stories
        </p>
        <h1 className="section-title" style={{ fontSize: "clamp(1.9rem,4vw,2.7rem)", marginTop: "0.5rem" }}>
          Real Journeys, Real Commitments
        </h1>
        <p style={{ margin: "0.75rem 0 0", maxWidth: 640, color: "rgba(255,255,255,0.9)" }}>
          Thousands of couples found their partner through MatrimonyConnect. Explore their experiences
          and share your own journey.
        </p>
        <button
          type="button"
          onClick={() => setShowForm(true)}
          className="button button-ghost-light"
          style={{ marginTop: "1rem" }}
        >
          Share Your Story
        </button>
      </section>

      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))",
          gap: "0.7rem",
        }}
      >
        {STATS.map((item) => (
          <article key={item.label} className="panel" style={{ padding: "0.9rem", textAlign: "center" }}>
            <p style={{ margin: 0, fontSize: "1.4rem", fontWeight: 800, color: "#b91c5c" }}>{item.value}</p>
            <p style={{ margin: "0.28rem 0 0", fontSize: "0.78rem", color: "var(--ink-muted)" }}>{item.label}</p>
          </article>
        ))}
      </section>

      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))",
          gap: "0.9rem",
        }}
      >
        {STORIES.map((story) => (
          <article key={story.id} className="panel panel-hover" style={{ overflow: "hidden" }}>
            <div style={{ position: "relative", height: 160, background: "linear-gradient(135deg, #1e3a8a, #0f766e)" }}>
              <div
                style={{
                  position: "absolute",
                  bottom: -26,
                  left: "50%",
                  transform: "translateX(-50%)",
                  display: "flex",
                  alignItems: "center",
                }}
              >
                <Image
                  src={story.photo1}
                  alt={`${story.coupleNames} profile 1`}
                  width={64}
                  height={64}
                  sizes="64px"
                  style={{
                    width: 64,
                    height: 64,
                    borderRadius: "50%",
                    objectFit: "cover",
                    border: "3px solid white",
                    marginRight: -10,
                    zIndex: 1,
                  }}
                />
                <div
                  style={{
                    width: 28,
                    height: 28,
                    borderRadius: "50%",
                    border: "3px solid white",
                    display: "grid",
                    placeItems: "center",
                    fontSize: 10,
                    fontWeight: 800,
                    color: "white",
                    background: "linear-gradient(135deg, #e11d48, #be185d)",
                    zIndex: 2,
                  }}
                >
                  OK
                </div>
                <Image
                  src={story.photo2}
                  alt={`${story.coupleNames} profile 2`}
                  width={64}
                  height={64}
                  sizes="64px"
                  style={{
                    width: 64,
                    height: 64,
                    borderRadius: "50%",
                    objectFit: "cover",
                    border: "3px solid white",
                    marginLeft: -10,
                    zIndex: 1,
                  }}
                />
              </div>

              <span
                style={{
                  position: "absolute",
                  top: 12,
                  left: 12,
                  fontSize: 11,
                  fontWeight: 700,
                  padding: "3px 10px",
                  borderRadius: 999,
                  border: "1px solid rgba(255,255,255,0.22)",
                  background: "rgba(255,255,255,0.16)",
                  color: "white",
                }}
              >
                {story.tag}
              </span>
              <span
                style={{
                  position: "absolute",
                  top: 12,
                  right: 12,
                  fontSize: 11,
                  fontWeight: 700,
                  padding: "3px 10px",
                  borderRadius: 999,
                  border: "1px solid rgba(255,255,255,0.22)",
                  background: "rgba(255,255,255,0.16)",
                  color: "white",
                }}
              >
                {story.days} days
              </span>
            </div>

            <div style={{ padding: "2.1rem 1rem 1rem" }}>
              <h3 style={{ margin: 0, fontSize: "1rem", textAlign: "center" }}>{story.coupleNames}</h3>
              <p style={{ margin: "0.3rem 0 0", textAlign: "center", color: "var(--ink-muted)", fontSize: "0.78rem" }}>
                {story.city} | Married {story.marriageYear}
              </p>
              <p
                style={{
                  margin: "0.78rem 0 0",
                  fontSize: "0.88rem",
                  color: "var(--ink-muted)",
                  lineHeight: 1.58,
                  display: expanded === story.id ? "block" : "-webkit-box",
                  WebkitLineClamp: 3,
                  WebkitBoxOrient: "vertical",
                  overflow: "hidden",
                }}
              >
                "{story.story}"
              </p>
              <button
                type="button"
                onClick={() => setExpanded(expanded === story.id ? null : story.id)}
                style={{
                  marginTop: 8,
                  background: "none",
                  border: "none",
                  color: "#be185d",
                  fontWeight: 700,
                  fontSize: "0.77rem",
                  cursor: "pointer",
                  padding: 0,
                }}
              >
                {expanded === story.id ? "Read less" : "Read more"}
              </button>
            </div>
          </article>
        ))}
      </section>

      <section
        className="panel"
        style={{
          padding: "1.2rem",
          textAlign: "center",
          borderColor: "rgba(227,68,117,0.22)",
          background: "linear-gradient(135deg, #fff1f2, #fce7f3)",
        }}
      >
        <h3 style={{ margin: 0, fontSize: "1.3rem", color: "#9f1239" }}>Build your own success story</h3>
        <p style={{ margin: "0.45rem 0 0", color: "#be185d", fontSize: "0.9rem" }}>
          Join thousands of members finding high-intent, meaningful matches.
        </p>
        <Link href="/matches" className="button button-primary" style={{ marginTop: "0.85rem" }}>
          Browse Matches
        </Link>
      </section>

      {showForm && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.5)",
            backdropFilter: "blur(4px)",
            zIndex: 9999,
            display: "grid",
            placeItems: "center",
            padding: "1rem",
          }}
        >
          <div className="panel" style={{ width: "100%", maxWidth: 520, padding: "1.1rem", borderRadius: 18 }}>
            {submitted ? (
              <div style={{ textAlign: "center", padding: "0.5rem" }}>
                <h3 style={{ margin: 0, fontSize: "1.1rem" }}>Story submitted</h3>
                <p style={{ margin: "0.5rem 0 0", color: "var(--ink-muted)", fontSize: "0.9rem" }}>
                  Thank you for sharing. Our team will review it before publishing.
                </p>
                <button
                  type="button"
                  onClick={() => {
                    setShowForm(false);
                    setSubmitted(false);
                  }}
                  className="button button-primary"
                  style={{ marginTop: "0.8rem" }}
                >
                  Close
                </button>
              </div>
            ) : (
              <>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.8rem" }}>
                  <h3 style={{ margin: 0, fontSize: "1rem" }}>Share Your Story</h3>
                  <button type="button" onClick={() => setShowForm(false)} className="button button-secondary">
                    Cancel
                  </button>
                </div>

                {[
                  { label: "Your Name", key: "name1", placeholder: "Rahul" },
                  { label: "Partner Name", key: "name2", placeholder: "Priya" },
                  { label: "City", key: "city", placeholder: "Mumbai" },
                  { label: "Email", key: "email", placeholder: "name@email.com" },
                ].map((field) => (
                  <div key={field.key} style={{ marginBottom: "0.7rem" }}>
                    <label className="form-label">{field.label}</label>
                    <input
                      value={form[field.key]}
                      onChange={(event) =>
                        setForm((previous) => ({ ...previous, [field.key]: event.target.value }))
                      }
                      placeholder={field.placeholder}
                      className="form-input"
                    />
                  </div>
                ))}

                <div style={{ marginBottom: "0.9rem" }}>
                  <label className="form-label">Story</label>
                  <textarea
                    value={form.story}
                    onChange={(event) => setForm((previous) => ({ ...previous, story: event.target.value }))}
                    placeholder="Tell us your journey..."
                    rows={4}
                    className="form-input"
                    style={{ resize: "vertical" }}
                  />
                </div>

                <button type="button" onClick={() => setSubmitted(true)} className="button button-primary" style={{ width: "100%" }}>
                  Submit Story
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
