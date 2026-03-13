"use client";

import { usePathname } from "next/navigation";
import PublicTopNav from "@/components/PublicTopNav";

export default function OnboardingLayout({ children }) {
  const pathname = usePathname();
  const currentStep = Number(pathname.split("/").pop()?.replace("step-", "")) || 1;
  const totalSteps = 5;

  return (
    <div className="public-flow-shell onboarding-flow-shell">
      <div className="container-shell public-flow-top">
        <PublicTopNav compact />
      </div>

      <div className="container-shell">
        <section className="panel onboarding-shell-v2">
          <div className="onboarding-head-v2">
            <p className="section-label">Onboarding Journey</p>
            <p className="onboarding-progress-text-v2">
              Step {currentStep} of {totalSteps}
            </p>
          </div>

          <div
            className="onboarding-progress-track-v2"
            style={{ "--progress": `${(currentStep / totalSteps) * 100}%` }}
            aria-label={`Progress ${currentStep} of ${totalSteps}`}
          >
            <div className="onboarding-progress-fill-v2" />
          </div>

          <div className="onboarding-content-v2">{children}</div>
        </section>
      </div>
    </div>
  );
}
