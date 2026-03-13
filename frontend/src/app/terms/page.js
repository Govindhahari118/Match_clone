import PageHero, { PageHeroLink } from "@/components/PageHero";

export const metadata = {
  title: "Terms of Service | MatrimonyConnect",
  description: "Terms and conditions for using MatrimonyConnect.",
};

export default function TermsPage() {
  return (
    <div className="container-shell legal-page">
      <PageHero
        eyebrow="Legal"
        title="Terms of Service"
        copy="MatrimonyConnect is a matchmaking platform. By using the service, you agree to the terms below."
        note="Effective March 10, 2026"
        actions={(
          <>
            <PageHeroLink href="/privacy" label="Privacy" />
            <PageHeroLink href="/community-guidelines" label="Community" />
            <PageHeroLink href="/refunds" label="Refunds" />
          </>
        )}
      />

      <section className="legal-section">
        <h2>Eligibility</h2>
        <ul>
          <li>You must be 18+ to create an account.</li>
          <li>You are responsible for the accuracy of your profile details.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>Acceptable Use</h2>
        <ul>
          <li>No harassment, impersonation, or fraudulent activity.</li>
          <li>No solicitation or commercial spam.</li>
          <li>Respect privacy controls and consent boundaries.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>Service Disclaimer</h2>
        <p>
          MatrimonyConnect is a facilitation tool and does not guarantee marriage outcomes. Users are responsible
          for their interactions and decisions.
        </p>
      </section>

      <section className="legal-section">
        <h2>Account Termination</h2>
        <p>
          We may suspend or terminate accounts that violate these terms or pose safety risks to the community.
        </p>
      </section>

      <section className="legal-section">
        <h2>Contact</h2>
        <p>Questions about these terms can be sent to support@matrimonyconnect.com.</p>
      </section>
    </div>
  );
}
