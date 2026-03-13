"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { signInWithPopup } from "firebase/auth";
import Link from "next/link";
import api from "../../../services/api";
import { auth, googleProvider } from "../../../config/firebase";
import { useAuth } from "../../../context/AuthContext";
import { authRules, errorIdFor, getInputA11y } from "../../../validation/rules";

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const router = useRouter();
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
      const response = await api.post("/auth/request-otp", { phone: data.phone });
      if (response.data.success) {
        router.push(`/otp?phone=${encodeURIComponent(data.phone)}&otp_id=${response.data.otp_id}`);
      }
    } catch (error) {
      setErrorMessage(error.response?.data?.error || "Failed to send OTP. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const result = await signInWithPopup(auth, googleProvider);
      const idToken = await result.user.getIdToken();
      const backendResponse = await api.post("/auth/login-firebase", { idToken });

      if (backendResponse.data.success) {
        const accessToken = backendResponse.data.access_token || backendResponse.data.accessToken;
        const refreshToken = backendResponse.data.refresh_token || backendResponse.data.refreshToken;
        await login(backendResponse.data.user, accessToken, refreshToken);
        router.push("/");
      }
    } catch (error) {
      setErrorMessage(error.response?.data?.error || "Google sign in failed. Try phone login.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <p className="section-label">Welcome back</p>
        <h1 className="auth-title">Continue your match journey</h1>
        <p className="auth-subtitle">Secure sign-in keeps your matches, chats, and preferences private.</p>

        <form onSubmit={handleSubmit(onSubmit)} noValidate style={{ marginTop: "1.2rem", display: "grid", gap: "0.8rem" }}>
          <div>
            <label className="form-label" htmlFor="phone">Phone Number</label>
            <input
              {...register("phone", authRules.phone)}
              {...getInputA11y("phone", errors)}
              id="phone"
              type="tel"
              className="form-input"
              placeholder="+91 98765 43210"
              autoComplete="tel"
            />
            <p className="form-helper">We will send a 6-digit OTP to verify your number.</p>
            {errors.phone && <p id={errorIdFor("phone")} className="form-error" role="alert">{errors.phone.message}</p>}
          </div>

          <button type="submit" className="button button-primary cta-full" disabled={loading} style={{ width: "100%" }}>
            {loading ? "Sending OTP..." : "Continue with Phone"}
          </button>

          <div className="auth-divider"><span>or</span></div>

          <button
            type="button"
            onClick={handleGoogleLogin}
            disabled={loading}
            className="button button-secondary cta-full"
            style={{ width: "100%" }}
          >
            <span aria-hidden="true" style={{ display: "inline-flex", alignItems: "center" }}>
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
                <path d="M17.64 9.2045c0-.638-.057-1.251-.163-1.836H9v3.471h4.844c-.209 1.125-.84 2.079-1.79 2.717v2.257h2.898c1.695-1.56 2.688-3.858 2.688-6.608Z" fill="#4285F4"/>
                <path d="M9 18c2.43 0 4.467-.806 5.956-2.187l-2.898-2.257c-.806.54-1.837.859-3.058.859-2.356 0-4.351-1.59-5.065-3.73H.939v2.323C2.42 15.983 5.44 18 9 18Z" fill="#34A853"/>
                <path d="M3.935 10.685a5.41 5.41 0 0 1-.282-1.685c0-.585.102-1.153.282-1.685V4.992H.939A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.939 4.008l2.996-2.323Z" fill="#FBBC05"/>
                <path d="M9 3.579c1.322 0 2.509.455 3.44 1.349l2.581-2.581C13.463.9 11.43 0 9 0 5.44 0 2.42 2.017.939 4.992l2.996 2.323C4.649 5.169 6.644 3.579 9 3.579Z" fill="#EA4335"/>
              </svg>
            </span>
            Continue with Google
          </button>

          {errorMessage && (
            <div role="alert" aria-live="assertive" className="auth-alert">
              {errorMessage}
            </div>
          )}

          <p className="form-note">
            By continuing, you agree to our <Link href="/terms">Terms</Link> and <Link href="/privacy">Privacy Policy</Link>.
          </p>
          <p className="form-note">
            New here? <Link href="/step-1" style={{ color: "var(--brand)" }}>Create a free profile</Link>.
          </p>
        </form>
      </div>
    </div>
  );
}
