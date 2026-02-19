"use client";
import { useRouter } from "next/navigation";

export default function LoginPromptModal({ isOpen, onClose, triggerText = "Sign in to continue" }) {
    const router = useRouter();
    if (!isOpen) return null;

    return (
        <div style={{
            position: "fixed", inset: 0, zIndex: 9999,
            background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)",
            display: "flex", alignItems: "center", justifyContent: "center",
            padding: "1rem"
        }} onClick={onClose}>
            <div style={{
                background: "white", borderRadius: 24, padding: "2rem",
                width: "100%", maxWidth: 400, textAlign: "center",
                boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
                animation: "slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1)"
            }} onClick={e => e.stopPropagation()}>
                <div style={{ fontSize: 48, marginBottom: "1rem" }}>🔒</div>
                <h2 style={{ fontSize: 20, fontWeight: 800, color: "#111827", marginBottom: "0.5rem" }}>
                    {triggerText}
                </h2>
                <p style={{ color: "#64748b", fontSize: 14, marginBottom: "1.5rem", lineHeight: 1.6 }}>
                    Join <strong>5M+ members</strong> to unlock full profiles, contact numbers, and chat with your matches.
                </p>

                <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                    <button onClick={() => router.push("/login")} style={{
                        padding: "12px", background: "linear-gradient(135deg, #e11d48, #be123c)",
                        color: "white", borderRadius: 12, fontWeight: 700, border: "none", cursor: "pointer", fontSize: 15
                    }}>
                        Log In
                    </button>
                    <button onClick={() => router.push("/signup")} style={{
                        padding: "12px", background: "white", border: "1.5px solid #e2e8f0",
                        color: "#374151", borderRadius: 12, fontWeight: 700, cursor: "pointer", fontSize: 15
                    }}>
                        Create Free Account
                    </button>
                </div>

                <p style={{ marginTop: "1.5rem", fontSize: 12, color: "#94a3b8", cursor: "pointer" }} onClick={onClose}>
                    Maybe later
                </p>
                <style>{`@keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }`}</style>
            </div>
        </div>
    );
}
