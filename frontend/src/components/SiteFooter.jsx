import Link from "next/link";

const FOOTER_SECTIONS = [
  {
    title: "Product",
    items: [
      { label: "Matches", href: "/matches" },
      { label: "Shortlists", href: "/shortlists" },
      { label: "Pricing", href: "/pricing" },
      { label: "Success Stories", href: "/success-stories" },
      { label: "Kundli", href: "/kundli" },
    ],
  },
  {
    title: "Company",
    items: [
      { label: "Home", href: "/" },
      { label: "Community Guidelines", href: "/community-guidelines" },
      { label: "Security Practices", href: "/security" },
      { label: "Trust Checklist", href: "/verification" },
      { label: "Help Center", href: "/help" },
    ],
  },
  {
    title: "Legal",
    items: [
      { label: "Terms", href: "/terms" },
      { label: "Privacy", href: "/privacy" },
      { label: "Refunds", href: "/refunds" },
      { label: "Security", href: "/security" },
    ],
  },
  {
    title: "Support",
    items: [
      { label: "Help Center", href: "/help" },
      { label: "Feedback", href: "/help" },
      { label: "Report Issue", href: "/help" },
      { label: "Pricing FAQ", href: "/pricing" },
    ],
  },
  {
    title: "Social",
    items: [
      { label: "LinkedIn", href: "https://www.linkedin.com", external: true },
      { label: "Instagram", href: "https://www.instagram.com", external: true },
      { label: "YouTube", href: "https://www.youtube.com", external: true },
      { label: "X (Twitter)", href: "https://x.com", external: true },
    ],
  },
];

const FOOTER_BOTTOM_LINKS = [
  { label: "Privacy", href: "/privacy" },
  { label: "Terms", href: "/terms" },
  { label: "Refunds", href: "/refunds" },
];

function FooterLink({ item }) {
  if (item.external) {
    return (
      <a className="site-footer-link" href={item.href} target="_blank" rel="noreferrer">
        {item.label}
      </a>
    );
  }
  return (
    <Link className="site-footer-link" href={item.href}>
      {item.label}
    </Link>
  );
}

export default function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="site-footer">
      <div className="container-shell site-footer-shell">
        <div className="site-footer-top">
          <div className="site-footer-brand">
            <span className="site-footer-mark">MC</span>
            <div>
              <strong>MatrimonyConnect</strong>
              <p>Privacy-first, self-serve matchmaking built for serious intent.</p>
            </div>
          </div>
          <div className="site-footer-cta">
            <Link href="/step-1" className="button button-primary">
              Start Free
            </Link>
            <Link href="/matches" className="button button-secondary">
              Browse Matches
            </Link>
          </div>
        </div>

        <div className="site-footer-grid">
          {FOOTER_SECTIONS.map((section) => (
            <div key={section.title} className="site-footer-section">
              <p className="site-footer-title">{section.title}</p>
              <div className="site-footer-links">
                {section.items.map((item) => (
                  <FooterLink key={`${section.title}-${item.label}`} item={item} />
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="site-footer-bottom">
          <span>(c) {year} MatrimonyConnect. All rights reserved.</span>
          <div className="site-footer-bottom-links">
            {FOOTER_BOTTOM_LINKS.map((item) => (
              <Link key={item.href} href={item.href} className="site-footer-bottom-link">
                {item.label}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
