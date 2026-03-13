"use client";

import { useState } from "react";

const FAQS = [
  {
    category: "Getting Started",
    icon: "GS",
    items: [
      { q: "How do I create a profile?", a: "Click Create Free Profile, add your basics, upload a photo, and complete preferences. It usually takes under five minutes." },
      { q: "Is registration free?", a: "Yes. Creating a profile and browsing are free. Premium features like contact visibility require a subscription." },
      { q: "How do I verify my profile?", a: "Go to Profile > Verification, upload a government ID. Verification typically completes within 24 hours." },
    ],
  },
  {
    category: "Matching & Search",
    icon: "MS",
    items: [
      { q: "How does matching work?", a: "Compatibility is computed from values, lifestyle, intent, and preferences to surface relevant profiles." },
      { q: "How do I send an interest?", a: "Click Send Interest on any profile card. If they accept, you can chat and share details." },
      { q: "What is a mutual match?", a: "A mutual match happens when both users accept each other. Mutual matches can chat freely." },
    ],
  },
  {
    category: "Premium Plans",
    icon: "PR",
    items: [
      { q: "What do premium plans include?", a: "Premium includes contact access, priority visibility, and advanced filters. Platinum adds priority placement." },
      { q: "Can I get a refund?", a: "Yes. We offer a 7-day money-back guarantee on all plans. Contact support within 7 days." },
      { q: "How do I upgrade?", a: "Open Pricing from navigation, choose a plan, and complete payment via UPI, card, or netbanking." },
    ],
  },
  {
    category: "Privacy & Safety",
    icon: "PS",
    items: [
      { q: "Who can see my phone number?", a: "Your number is hidden by default. It is visible only after mutual match on premium plans." },
      { q: "How do I report someone?", a: "Open the profile menu and select Report Profile. Reports are queued for review and action." },
      { q: "Is my data safe?", a: "We use SSL encryption and never sell personal data. You control visibility settings in Privacy." },
    ],
  },
];

const CATEGORIES = [
  { title: "Getting Started", icon: "GS", caption: "Profile setup and verification" },
  { title: "Matching & Search", icon: "MS", caption: "Compatibility and filters" },
  { title: "Premium Plans", icon: "PR", caption: "Upgrades and refunds" },
  { title: "Privacy & Safety", icon: "PS", caption: "Reporting and controls" },
  { title: "Technical Issues", icon: "TI", caption: "Login, OTP, performance" },
  { title: "Billing", icon: "BL", caption: "Payments and invoices" },
];

const SUPPORT_CARDS = [
  { title: "Live Chat", desc: "Chat with support in real time", action: "Start chat", href: "/help", tone: "support" },
  { title: "Email Us", desc: "support@matrimonyconnect.com", action: "Send email", href: "mailto:support@matrimonyconnect.com", tone: "brand" },
  { title: "Call Us", desc: "Mon-Sat, 9:00-20:00 IST", action: "1800-XXX-XXXX", href: "tel:18000000000", tone: "warning" },
  { title: "Raise Ticket", desc: "We respond within 24 hours", action: "Open ticket", href: "/help", tone: "success" },
];

export default function HelpPage() {
  const [search, setSearch] = useState("");
  const [openFaq, setOpenFaq] = useState(null);
  const [activeCategory, setActiveCategory] = useState(null);

  const filteredFaqs = FAQS.map((section) => ({
    ...section,
    items: section.items.filter(
      (item) =>
        item.q.toLowerCase().includes(search.toLowerCase()) ||
        item.a.toLowerCase().includes(search.toLowerCase())
    ),
  })).filter((section) => section.items.length > 0 && (!activeCategory || section.category === activeCategory));

  return (
    <div className="help-shell">
      <section className="help-hero">
        <p className="section-label help-hero-kicker">Support Center</p>
        <h1 className="help-hero-title">How can we help?</h1>
        <p className="help-hero-copy">
          Search the knowledge base or browse categories to get answers fast.
        </p>
        <div className="help-search">
          <input
            type="text"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search for help. Example: verify profile"
          />
          <span>?</span>
        </div>
      </section>

      <hr className="section-divider" />

      <section>
        <p className="section-label">Browse Topics</p>
        <div className="help-category-grid">
          {CATEGORIES.map((cat) => (
            <button
              key={cat.title}
              type="button"
              onClick={() => setActiveCategory(activeCategory === cat.title ? null : cat.title)}
              className={`help-category-card ${activeCategory === cat.title ? "active" : ""}`}
            >
              <span className="chip chip-support help-category-chip">{cat.icon}</span>
              <strong>{cat.title}</strong>
              <small className="help-category-caption">{cat.caption}</small>
            </button>
          ))}
        </div>
      </section>

      <section>
        <div className="help-section-head">
          <div>
            <p className="section-label">Knowledge Base</p>
            <h2 className="section-title section-title-xs">Frequently asked questions</h2>
          </div>
          {(activeCategory || search) && (
            <button
              type="button"
              className="button button-secondary"
              onClick={() => {
                setActiveCategory(null);
                setSearch("");
              }}
            >
              Clear filters
            </button>
          )}
        </div>

        {filteredFaqs.length === 0 ? (
          <div className="panel help-empty-panel">
            <h3>No results found</h3>
            <p className="section-copy">
              Try different keywords or contact support below.
            </p>
          </div>
        ) : (
          <div className="help-faq-grid">
            {filteredFaqs.map((section) => (
              <div key={section.category} className="help-faq-card">
                <div className="help-faq-head">
                  <span className="chip chip-support">{section.icon}</span>
                  <strong>{section.category}</strong>
                  <span className="chip help-faq-count">{section.items.length} articles</span>
                </div>

                {section.items.map((item, idx) => {
                  const key = `${section.category}-${idx}`;
                  const isOpen = openFaq === key;
                  return (
                    <div key={key} className={`help-faq-item ${idx === 0 ? "first" : ""}`}>
                      <button
                        type="button"
                        onClick={() => setOpenFaq(isOpen ? null : key)}
                        className={`help-faq-row ${isOpen ? "active" : ""}`}
                      >
                        <span>{item.q}</span>
                        <span className="chip help-faq-toggle">
                          {isOpen ? "-" : "+"}
                        </span>
                      </button>
                      {isOpen && (
                        <div className="help-faq-answer">
                          <p>{item.a}</p>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        )}
      </section>

      <section>
        <p className="section-label">Contact Support</p>
        <h2 className="section-title section-title-xs">Still need help?</h2>
        <div className="help-support-grid help-support-grid-spaced">
          {SUPPORT_CARDS.map((card) => (
            <div key={card.title} className="help-support-card">
              <strong>{card.title}</strong>
              <p className="help-support-desc">{card.desc}</p>
              <a
                href={card.href}
                className="button button-secondary help-support-cta"
              >
                {card.action}
              </a>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
