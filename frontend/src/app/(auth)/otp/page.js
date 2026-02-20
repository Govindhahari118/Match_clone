"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";

export default function OtpPage() {
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
        await login(response.data.user, response.data.access_token, response.data.refresh_token);

        if (response.data.user.isNewUser) {
          router.push("/onboarding/step-1");
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
        <h1 className="auth-title">Enter One-Time Password</h1>
        <p className="auth-subtitle">Code sent to {phone || "your phone"}.</p>

        <form onSubmit={handleSubmit(onSubmit)} style={{ marginTop: "1.2rem", display: "grid", gap: "0.8rem" }}>
          <div>
            <label className="form-label">6-digit OTP</label>
            <input
              {...register("otp", {
                required: "OTP is required",
                minLength: { value: 6, message: "OTP should be 6 digits" },
                maxLength: { value: 6, message: "OTP should be 6 digits" },
              })}
              type="text"
              className="form-input"
              placeholder="123456"
              inputMode="numeric"
              maxLength={6}
              style={{ textAlign: "center", letterSpacing: "0.35em", fontWeight: 700 }}
            />
            {errors.otp && <p className="form-error">{errors.otp.message}</p>}
          </div>

          <button type="submit" className="button button-primary" disabled={loading} style={{ width: "100%" }}>
            {loading ? "Verifying..." : "Verify and Continue"}
          </button>

          {errorMessage && (
            <div
              style={{
                borderRadius: 12,
                background: "#ffe8e8",
                color: "#9e2f2f",
                border: "1px solid #ffc7c7",
                fontSize: "0.82rem",
                padding: "0.55rem 0.7rem",
              }}
            >
              {errorMessage}
            </div>
          )}

          <p style={{ margin: "0.3rem 0 0", fontSize: "0.78rem", color: "var(--ink-muted)" }}>
            Demo OTP for local testing is usually <strong>123456</strong>.
          </p>
        </form>
      </div>
    </div>
  );
}
