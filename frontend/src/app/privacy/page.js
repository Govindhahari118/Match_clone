import PageHero, { PageHeroLink } from "@/components/PageHero";

export const metadata = {
  title: "Privacy Policy | MatrimonyConnect",
  description: "How MatrimonyConnect collects, uses, and protects member data.",
};

export default function PrivacyPage() {
  return (
    <div className="container-shell legal-page">
      <PageHero
        eyebrow="Privacy"
        title="Privacy Policy"
        copy="MatrimonyConnect is built around trust. This policy explains what data we collect, why we collect it, and how you control your privacy."
        note="Effective March 10, 2026"
        actions={(
          <>
            <PageHeroLink href="/terms" label="Terms" />
            <PageHeroLink href="/security" label="Security" />
          </>
        )}
      />

      <section className="legal-section">
        <h2>What We Collect</h2>
        <ul>
          <li>Account details: name, contact information, and login identifiers.</li>
          <li>Profile details: demographics, preferences, and profile photos you provide.</li>
          <li>Usage data: actions taken inside the product to improve stability and safety.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>How We Use Data</h2>
        <ul>
          <li>To provide matchmaking, search, and communication features.</li>
          <li>To protect user safety and reduce misuse or fake profiles.</li>
          <li>To improve performance, reliability, and user experience.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>Privacy Controls</h2>
        <ul>
          <li>Phone and contact visibility are hidden by default.</li>
          <li>You can control profile visibility, last seen, and search indexing.</li>
          <li>Privacy settings are available in Settings &gt; Privacy.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>Data Retention</h2>
        <p>
          We keep your data only as long as necessary to provide the service. You may request deletion at
          any time from Settings.
        </p>
      </section>

      <section className="legal-section">
        <h2>Contact</h2>
        <p>For privacy questions, email support@matrimonyconnect.com.</p>
      </section>
    </div>
  );
}
