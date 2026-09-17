"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";

function OtpPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const phone = searchParams.get("phone") || "";
  const initialExpiry = Number.parseInt(searchParams.get("expires_in"), 10) || 600;
  const initialResend = Number.parseInt(searchParams.get("resend_after"), 10) || 60;
  const devOtp = process.env.NODE_ENV !== "production" ? searchParams.get("dev_otp") : null;
  const { login } = useAuth();

  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [secondsLeft, setSecondsLeft] = useState(initialExpiry);
  const [resendLeft, setResendLeft] = useState(initialResend);

  const { register, handleSubmit, setValue, formState: { errors } } = useForm();

  useEffect(() => {
    const timer = window.setInterval(() => {
      setSecondsLeft((value) => Math.max(0, value - 1));
      setResendLeft((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!phone) router.replace("/login");
  }, [phone, router]);

  const expiryLabel = useMemo(() => {
    const minutes = Math.floor(secondsLeft / 60);
    const seconds = secondsLeft % 60;
    return `${minutes}:${String(seconds).padStart(2, "0")}`;
  }, [secondsLeft]);

  const onSubmit = async (data) => {
    if (secondsLeft <= 0) {
      setErrorMessage("This OTP has expired. Request a new code.");
      return;
    }
    setLoading(true);
    setErrorMessage("");
    try {
      const response = await api.post("/auth/verify-otp", { phone, otp: data.otp });
      if (response.data.success) {
        await login(response.data.user, response.data.access_token, response.data.refresh_token);
        router.replace(response.data.user?.isNewUser ? "/onboarding/step-1" : "/");
      }
    } catch (error) {
      setErrorMessage(error.response?.data?.error || "OTP verification failed. Please retry.");
    } finally {
      setLoading(false);
    }
  };

  const resend = async () => {
    if (!phone || resendLeft > 0 || resending) return;
    setResending(true);
    setErrorMessage("");
    try {
      const response = await api.post("/auth/request-otp", { phone, type: "signup" });
      setSecondsLeft(response.data.expires_in || 600);
      setResendLeft(response.data.resend_after || 60);
      setValue("otp", "");
      if (response.data.dev_otp && process.env.NODE_ENV !== "production") {
        setErrorMessage(`Development environment OTP: ${response.data.dev_otp}`);
      }
    } catch (error) {
      setErrorMessage(error.response?.data?.error || "OTP could not be resent.");
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <p className="section-label">Verification</p>
        <h1 className="auth-title">Enter One-Time Password</h1>
        <p className="auth-subtitle">A temporary 6-digit code was sent to {phone || "your phone"}.</p>

        <form onSubmit={handleSubmit(onSubmit)} style={{ marginTop: "1.2rem", display: "grid", gap: "0.8rem" }}>
          <div>
            <label className="form-label">6-digit OTP</label>
            <input
              {...register("otp", {
                required: "OTP is required",
                pattern: { value: /^\d{6}$/, message: "Enter exactly 6 digits" },
              })}
              type="text"
              className="form-input"
              placeholder="••••••"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              disabled={secondsLeft <= 0}
              style={{ textAlign: "center", letterSpacing: "0.35em", fontWeight: 700 }}
            />
            {errors.otp && <p className="form-error">{errors.otp.message}</p>}
          </div>

          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "0.7rem", flexWrap: "wrap" }}>
            <span style={{ fontSize: "0.78rem", color: secondsLeft > 0 ? "var(--ink-muted)" : "#9e2f2f" }}>
              {secondsLeft > 0 ? `Code expires in ${expiryLabel}` : "Code expired"}
            </span>
            <button type="button" className="button button-secondary" disabled={resendLeft > 0 || resending} onClick={resend}>
              {resending ? "Sending…" : resendLeft > 0 ? `Resend in ${resendLeft}s` : "Resend OTP"}
            </button>
          </div>

          <button type="submit" className="button button-primary" disabled={loading || secondsLeft <= 0} style={{ width: "100%" }}>
            {loading ? "Verifying…" : "Verify and Continue"}
          </button>

          {devOtp && (
            <div style={{ borderRadius: 12, background: "#fff6d9", color: "#6f5510", border: "1px solid #f0d887", fontSize: "0.8rem", padding: "0.55rem 0.7rem" }}>
              Development environment OTP: <strong>{devOtp}</strong>. This is never exposed in production.
            </div>
          )}

          {errorMessage && <div style={{ borderRadius: 12, background: "#ffe8e8", color: "#9e2f2f", border: "1px solid #ffc7c7", fontSize: "0.82rem", padding: "0.55rem 0.7rem" }}>{errorMessage}</div>}

          <p style={{ margin: "0.3rem 0 0", fontSize: "0.78rem", color: "var(--ink-muted)", lineHeight: 1.45 }}>
            Codes expire automatically and repeated incorrect attempts are limited. Never share an OTP with another person, including support staff.
          </p>
        </form>
      </div>
    </div>
  );
}

export default function OtpPage() {
  return (
    <Suspense fallback={<div className="auth-shell"><div className="auth-card">Loading verification…</div></div>}>
      <OtpPageContent />
    </Suspense>
  );
}
