import PageHero, { PageHeroLink } from "@/components/PageHero";

export const metadata = {
  title: "Refund Policy | MatrimonyConnect",
  description: "Refund policy for MatrimonyConnect premium plans.",
};

export default function RefundsPage() {
  return (
    <div className="container-shell legal-page">
      <PageHero
        eyebrow="Billing"
        title="Refund Policy"
        copy="All paid plans include a 7-day money-back guarantee. If you are not satisfied, contact support within 7 days."
        note="Effective March 10, 2026"
        actions={(
          <>
            <PageHeroLink href="/pricing" label="Pricing" variant="primary" />
            <PageHeroLink href="/terms" label="Terms" />
            <PageHeroLink href="/help" label="Support" />
          </>
        )}
      />

      <section className="legal-section">
        <h2>Eligibility</h2>
        <ul>
          <li>Refunds are available within 7 days of plan activation.</li>
          <li>Only the original payment method can receive refunds.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>How to Request</h2>
        <p>
          Email support@matrimonyconnect.com with your order ID and the reason for cancellation.
        </p>
      </section>

      <section className="legal-section">
        <h2>Exceptions</h2>
        <p>
          Refunds may not be granted after 7 days, or if the plan was purchased through a partner marketplace.
        </p>
      </section>
    </div>
  );
}
