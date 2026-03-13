import PageHero, { PageHeroLink } from "@/components/PageHero";

export const metadata = {
  title: "Security | MatrimonyConnect",
  description: "Security practices and responsible disclosure information.",
};

export default function SecurityPage() {
  return (
    <div className="container-shell legal-page">
      <PageHero
        eyebrow="Security"
        title="Security Practices"
        copy="We take security seriously. This page outlines basic practices and how to report security issues responsibly."
        note="Effective March 10, 2026"
        actions={(
          <>
            <PageHeroLink href="/community-guidelines" label="Community" />
            <PageHeroLink href="/privacy" label="Privacy" />
            <PageHeroLink href="/help" label="Report Issue" variant="primary" />
          </>
        )}
      />

      <section className="legal-section">
        <h2>Security Basics</h2>
        <ul>
          <li>HTTPS is enforced across the platform.</li>
          <li>Strong input validation and safe defaults protect user data.</li>
          <li>Security headers reduce common browser attacks.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>Responsible Disclosure</h2>
        <p>
          If you find a vulnerability, email security@matrimonyconnect.com with a detailed report. Do not publicly
          disclose issues before we investigate and fix them.
        </p>
      </section>
    </div>
  );
}
