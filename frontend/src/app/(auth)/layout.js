"use client";

import PublicTopNav from "@/components/PublicTopNav";

export default function AuthLayout({ children }) {
  return (
    <div className="auth-flow-shell">
      <div className="container-shell" style={{ paddingTop: "1rem" }}>
        <PublicTopNav compact />
      </div>
      <div className="auth-flow-content">{children}</div>
    </div>
  );
}
