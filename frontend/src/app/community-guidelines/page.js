import PageHero, { PageHeroLink } from "@/components/PageHero";

export const metadata = {
  title: "Community Guidelines | MatrimonyConnect",
  description: "How we keep MatrimonyConnect safe and respectful.",
};

export default function CommunityGuidelinesPage() {
  return (
    <div className="container-shell legal-page">
      <PageHero
        eyebrow="Safety"
        title="Community Guidelines"
        copy="MatrimonyConnect is built for serious, respectful matchmaking. These guidelines protect everyone on the platform."
        note="Effective March 10, 2026"
        actions={(
          <>
            <PageHeroLink href="/privacy" label="Privacy" />
            <PageHeroLink href="/terms" label="Terms" />
            <PageHeroLink href="/help" label="Report Concern" variant="primary" />
          </>
        )}
      />

      <section className="legal-section">
        <h2>Be Honest</h2>
        <ul>
          <li>Use your real identity and accurate profile details.</li>
          <li>Do not misrepresent age, marital status, or profession.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>Be Respectful</h2>
        <ul>
          <li>No harassment, threats, or abusive language.</li>
          <li>Respect family involvement and communication preferences.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>Protect Privacy</h2>
        <ul>
          <li>Do not request or share private information too early.</li>
          <li>Follow visibility settings and consent before sharing details.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>Report Concerns</h2>
        <p>
          Use the Report option on any profile or message to flag abuse. Reports are queued for review.
        </p>
      </section>
    </div>
  );
}
