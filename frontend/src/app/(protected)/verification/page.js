"use client";

import PublicTopNav from "@/components/PublicTopNav";
import IdentityVerification from "@/components/IdentityVerification";

export default function VerificationPage() {
  return (
    <div className="page-shell" style={{ padding: "1rem 0 2rem" }}>
      <div className="container-shell">
        <PublicTopNav compact />

        <section className="panel verification-shell">
          <div className="verification-header">
            <div>
              <p className="section-label">Trust & Safety</p>
              <h1 className="section-title" style={{ fontSize: "clamp(1.8rem,4vw,2.6rem)" }}>
                Identity Verification
              </h1>
              <p className="section-copy" style={{ marginTop: "0.7rem" }}>
                Verify your identity to improve trust and unlock better visibility across the platform.
              </p>
            </div>
          </div>

          <IdentityVerification />

          <div className="verification-benefits">
            <h3>Benefits after verification</h3>
            <ul>
              <li>Verified badge on profile and search listings</li>
              <li>Higher trust for incoming interests and conversations</li>
              <li>Improved ranking in recommendations</li>
              <li>Safer interactions for both members and families</li>
            </ul>
          </div>
        </section>
      </div>
    </div>
  );
}
