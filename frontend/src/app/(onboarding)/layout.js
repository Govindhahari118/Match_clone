"use client";

import { usePathname } from "next/navigation";
import PublicTopNav from "@/components/PublicTopNav";

export default function OnboardingLayout({ children }) {
  const pathname = usePathname();
  const currentStep = parseInt(pathname.split("/").pop().replace("step-", ""), 10) || 1;
  const totalSteps = 5;

  return (
    <div className="page-shell" style={{ padding: "1rem 0 2rem" }}>
      <div className="container-shell">
        <PublicTopNav compact />

        <section className="panel onboarding-shell">
          <div className="onboarding-progress-head">
            <p className="section-label">Onboarding</p>
            <p className="onboarding-progress-text">
              Step {currentStep} of {totalSteps}
            </p>
          </div>

          <div className="onboarding-progress-track">
            <div
              className="onboarding-progress-fill"
              style={{ width: `${(currentStep / totalSteps) * 100}%` }}
            />
          </div>

          <div style={{ marginTop: "1rem" }}>{children}</div>
        </section>
      </div>
    </div>
  );
}
