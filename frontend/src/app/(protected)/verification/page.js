"use client";

import PublicTopNav from "@/components/PublicTopNav";
import IdentityVerification from "@/components/IdentityVerification";

export default function VerificationPage() {
  return (
    <div className="page-shell page-pad-lg">
      <div className="container-shell">
        <PublicTopNav compact />

        <section className="panel verification-shell">
          <div className="verification-header">
            <div>
              <p className="section-label">Trust & Safety</p>
              <h1 className="section-title section-title-sm">
                Profile trust checklist
              </h1>
              <p className="section-copy">
                Complete the self-serve checklist to improve trust and visibility. No documents, no manual review.
              </p>
            </div>
          </div>

          <IdentityVerification />

          <div className="verification-benefits">
            <h3>Benefits after completion</h3>
            <ul>
              <li>Profile completeness badge on listings</li>
              <li>Higher trust for incoming interests and conversations</li>
              <li>Improved visibility based on completeness</li>
              <li>Safer interactions with privacy-first controls</li>
            </ul>
          </div>
        </section>
      </div>
    </div>
  );
}
