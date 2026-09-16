"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { signInWithPopup } from "firebase/auth";
import Image from "next/image";
import api from "../../../services/api";
import { auth, googleProvider } from "../../../config/firebase";
import { useAuth } from "../../../context/AuthContext";

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
      // Phone entry intentionally supports both returning and first-time users.
      // The backend never marks the account verified until the OTP succeeds.
      const response = await api.post("/auth/request-otp", { phone: data.phone, type: "signup" });
      if (response.data.success) {
        const query = new URLSearchParams({
          phone: data.phone,
          otp_id: response.data.otp_id || "",
          expires_in: String(response.data.expires_in || 600),
          resend_after: String(response.data.resend_after || 60),
        });
        if (response.data.dev_otp && process.env.NODE_ENV !== "production") query.set("dev_otp", response.data.dev_otp);
        router.push(`/otp?${query.toString()}`);
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
        router.push(backendResponse.data.user?.isNewUser ? "/onboarding/step-1" : "/");
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
        <p className="section-label">Secure access</p>
        <h1 className="auth-title">Continue with your phone</h1>
        <p className="auth-subtitle">Returning members sign in. First-time members verify their number before creating a profile.</p>

        <form onSubmit={handleSubmit(onSubmit)} style={{ marginTop: "1.2rem", display: "grid", gap: "0.8rem" }}>
          <div>
            <label className="form-label">Phone Number</label>
            <input
              {...register("phone", {
                required: "Phone number is required",
                minLength: { value: 10, message: "Enter a valid phone number" },
              })}
              type="tel"
              className="form-input"
              placeholder="+91 98765 43210"
              autoComplete="tel"
            />
            {errors.phone && <p className="form-error">{errors.phone.message}</p>}
          </div>

          <button type="submit" className="button button-primary" disabled={loading} style={{ width: "100%" }}>
            {loading ? "Sending secure OTP…" : "Continue with Phone"}
          </button>

          <div style={{ position: "relative", textAlign: "center", margin: "0.25rem 0" }}>
            <span style={{ background: "#fff", padding: "0 0.55rem", fontSize: "0.78rem", color: "var(--ink-muted)", position: "relative", zIndex: 1 }}>or</span>
            <div style={{ position: "absolute", left: 0, right: 0, top: "50%", borderTop: "1px solid var(--line)" }} />
          </div>

          <button type="button" onClick={handleGoogleLogin} disabled={loading} className="button button-secondary" style={{ width: "100%" }}>
            <Image src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" width={18} height={18} sizes="18px" />
            Continue with Google
          </button>

          {errorMessage && <div style={{ marginTop: "0.2rem", borderRadius: 12, background: "#ffe8e8", color: "#9e2f2f", border: "1px solid #ffc7c7", fontSize: "0.82rem", padding: "0.55rem 0.7rem" }}>{errorMessage}</div>}

          <p style={{ margin: "0.3rem 0 0", fontSize: "0.78rem", color: "var(--ink-muted)", lineHeight: 1.45 }}>
            OTPs expire automatically, resend attempts are throttled, and verification attempts are limited. By continuing, you agree to our Terms and Privacy Policy.
          </p>
        </form>
      </div>
    </div>
  );
}
