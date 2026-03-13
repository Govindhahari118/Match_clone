"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { SUCCESS_STATS, SUCCESS_STORIES } from "@/components/successStoriesData";

export default function SuccessStoriesPage() {
  const [expanded, setExpanded] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name1: "", name2: "", city: "", story: "", email: "" });
  const [submitted, setSubmitted] = useState(false);

  return (
    <div className="success-shell">
      <section className="panel page-hero success-hero">
        <div>
          <p className="section-label">Member Stories</p>
          <h1 className="page-hero-title">Real Journeys, Real Commitments</h1>
          <p className="page-hero-copy">
            Thousands of members found their partner through MatrimonyConnect. Explore their journeys and share your own.
          </p>
          <p className="page-hero-note">Updated weekly with verified submissions.</p>
        </div>
        <div className="page-hero-actions">
          <button type="button" onClick={() => setShowForm(true)} className="button button-ghost-light">
            Share Your Story
          </button>
          <Link href="/matches" className="button button-primary">
            Browse Matches
          </Link>
        </div>
      </section>

      <section className="stats-grid success-stats-grid">
        {SUCCESS_STATS.map((item) => (
          <article key={item.label} className="stat-tile success-stat-tile">
            <p className="stat-value">{item.value}</p>
            <p className="stat-label">{item.label}</p>
          </article>
        ))}
      </section>

      <section className="story-grid success-story-grid">
        {SUCCESS_STORIES.map((story) => (
          <article key={story.id} className="panel panel-hover story-card">
            <div className="story-media">
              <div className="story-avatars">
                <Image
                  src={story.photo1}
                  alt={`${story.coupleNames} profile 1`}
                  width={64}
                  height={64}
                  sizes="64px"
                  className="story-avatar story-avatar-left"
                />
                <div className="story-avatar-badge">
                  OK
                </div>
                <Image
                  src={story.photo2}
                  alt={`${story.coupleNames} profile 2`}
                  width={64}
                  height={64}
                  sizes="64px"
                  className="story-avatar story-avatar-right"
                />
              </div>

              <span className="story-tag">{story.tag}</span>
              <span className="story-days">{story.days} days</span>
            </div>

            <div className="story-body">
              <h3 className="story-title">{story.coupleNames}</h3>
              <p className="story-meta">
                {story.city} | Married {story.marriageYear}
              </p>
              <p className={`story-copy ${expanded === story.id ? "expanded" : ""}`}>
                &ldquo;{story.story}&rdquo;
              </p>
              <button
                type="button"
                onClick={() => setExpanded(expanded === story.id ? null : story.id)}
                className="story-toggle"
              >
                {expanded === story.id ? "Read less" : "Read more"}
              </button>
            </div>
          </article>
        ))}
      </section>

      <section className="panel success-cta">
        <h3>Start your journey</h3>
        <p>
          Use guided filters and verified profiles to find a partner who shares your goals.
        </p>
        <Link href="/matches" className="button button-primary">
          Explore Matches
        </Link>
      </section>

      {showForm && (
        <div className="modal-overlay success-modal-overlay">
          <div className="panel modal-dialog success-modal">
            {submitted ? (
              <div className="success-modal-state">
                <h3>Story submitted</h3>
                <p>
                  Thank you for sharing. Our team will review it before publishing.
                </p>
                <button
                  type="button"
                  onClick={() => {
                    setShowForm(false);
                    setSubmitted(false);
                  }}
                  className="button button-primary"
                >
                  Close
                </button>
              </div>
            ) : (
              <>
                <div className="success-modal-head">
                  <h3>Share Your Story</h3>
                  <button type="button" onClick={() => setShowForm(false)} className="button button-quiet">
                    Cancel
                  </button>
                </div>

                <div className="success-form">
                  {[
                    { label: "Your Name", key: "name1", placeholder: "Rahul" },
                    { label: "Partner Name", key: "name2", placeholder: "Priya" },
                    { label: "City", key: "city", placeholder: "Mumbai" },
                    { label: "Email", key: "email", placeholder: "name@email.com" },
                  ].map((field) => (
                    <div key={field.key} className="success-form-field">
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

                  <div className="success-form-field">
                    <label className="form-label">Story</label>
                    <textarea
                      value={form.story}
                      onChange={(event) => setForm((previous) => ({ ...previous, story: event.target.value }))}
                      placeholder="Tell us your journey..."
                      rows={4}
                      className="form-input success-form-textarea"
                    />
                  </div>

                  <button type="button" onClick={() => setSubmitted(true)} className="button button-primary cta-full">
                    Submit Story
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

