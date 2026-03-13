"use client";

import { useRouter } from "next/navigation";

export default function LoginPromptModal({
  isOpen,
  onClose,
  triggerText = "Sign in to continue",
}) {
  const router = useRouter();

  if (!isOpen) return null;

  return (
    <div role="presentation" onClick={onClose} className="modal-overlay">
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Login required"
        onClick={(event) => event.stopPropagation()}
        className="panel modal-dialog"
      >
        <div aria-hidden="true" className="modal-mark">
          LK
        </div>

        <h2 className="modal-title">{triggerText}</h2>
        <p className="modal-copy">
          Join 5M+ members to unlock full profile details and start direct conversations.
        </p>

        <div className="modal-actions">
          <button type="button" onClick={() => router.push("/login")} className="button button-primary cta-full">
            Log In
          </button>
          <button type="button" onClick={() => router.push("/step-1")} className="button button-secondary cta-full">
            Create Free Account
          </button>
        </div>

        <button type="button" onClick={onClose} className="button button-secondary cta-full">
          Maybe Later
        </button>
      </div>
    </div>
  );
}
