"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import Link from "next/link";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import { authRules, errorIdFor, getInputA11y } from "../../../validation/rules";

function OtpPageContent() {
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const router = useRouter();
  const searchParams = useSearchParams();
  const phone = searchParams.get("phone");
  const otpId = searchParams.get("otp_id");
  const { login } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm();

  const onSubmit = async (data) => {
    setLoading(true);
    setErrorMessage("");

    try {
      const response = await api.post("/auth/verify-otp", {
        phone,
        otp: data.otp,
        otp_id: otpId,
      });

      if (response.data.success) {
        const accessToken = response.data.access_token || response.data.accessToken;
        const refreshToken = response.data.refresh_token || response.data.refreshToken;
        await login(response.data.user, accessToken, refreshToken);

        if (response.data.user.isNewUser) {
          router.push("/step-1");
        } else {
          router.push("/");
        }
      }
    } catch (error) {
      setErrorMessage(error.response?.data?.error || "OTP verification failed. Please retry.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <p className="section-label">Verification</p>
        <h1 className="auth-title">Enter the verification code</h1>
        <p className="auth-subtitle">We sent a 6-digit code to {phone || "your phone"}.</p>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="form-stack">
          <div>
            <label className="form-label" htmlFor="otp">6-digit OTP</label>
            <input
              {...register("otp", authRules.otp)}
              {...getInputA11y("otp", errors)}
              id="otp"
              type="text"
              className="form-input otp-input"
              placeholder="123456"
              inputMode="numeric"
              maxLength={6}
              autoComplete="one-time-code"
            />
            <p className="form-helper">Code expires in a few minutes. Request a new OTP if needed.</p>
            {errors.otp && <p id={errorIdFor("otp")} className="form-error" role="alert">{errors.otp.message}</p>}
          </div>

          <button type="submit" className="button button-primary cta-full" disabled={loading}>
            {loading ? "Verifying..." : "Verify and Continue"}
          </button>

          {errorMessage && (
            <div role="alert" aria-live="assertive" className="auth-alert">
              {errorMessage}
            </div>
          )}

          <p className="form-note">
            Demo OTP for local testing is usually <strong>123456</strong>.
          </p>
          <p className="form-note">
            Entered the wrong number? <Link href="/login" className="link-accent">Go back</Link>.
          </p>
        </form>
      </div>
    </div>
  );
}

export default function OtpPage() {
  return (
    <Suspense fallback={<div className="auth-shell"><div className="auth-card">Loading verification...</div></div>}>
      <OtpPageContent />
    </Suspense>
  );
}
