"use client";

import PublicTopNav from "@/components/PublicTopNav";

export default function AuthLayout({ children }) {
  return (
    <div className="public-flow-shell">
      <div className="container-shell public-flow-top">
        <PublicTopNav compact />
      </div>
      <div className="public-flow-content">{children}</div>
    </div>
  );
}
