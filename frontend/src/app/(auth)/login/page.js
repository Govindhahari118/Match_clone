"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import api from "../../../services/api";
import { auth, googleProvider } from "../../../config/firebase";
import { signInWithPopup } from "firebase/auth";
import { useAuth } from "../../../context/AuthContext";

export default function LoginPage() {
    const [loading, setLoading] = useState(false);
    const router = useRouter();
    const { login } = useAuth();
    const { register, handleSubmit, formState: { errors } } = useForm();

    const onSubmit = async (data) => {
        setLoading(true);
        try {
            // Backend expects type: 'login' or 'signup'. Using flexible approach.
            const response = await api.post("/auth/request-otp", { phone: data.phone });

            if (response.data.success) {
                router.push(`/otp?phone=${encodeURIComponent(data.phone)}&otp_id=${response.data.otp_id}`);
            }
        } catch (error) {
            alert(error.response?.data?.error || "Failed to send OTP");
        } finally {
            setLoading(false);
        }
    };

    const handleGoogleLogin = async () => {
        setLoading(true);
        try {
            const result = await signInWithPopup(auth, googleProvider);
            const user = result.user;
            const idToken = await user.getIdToken();

            // Send ID Token to Backend
            const backendResponse = await api.post("/auth/login-firebase", { idToken });

            if (backendResponse.data.success) {
                const accessToken = backendResponse.data.access_token || backendResponse.data.accessToken;
                const refreshToken = backendResponse.data.refresh_token || backendResponse.data.refreshToken;
                await login(backendResponse.data.user, accessToken, refreshToken);
                router.push("/");
            }
        } catch (error) {
            console.error("Google Login Error:", error);
            alert("Google Login Failed: " + error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex min-h-screen flex-col items-center justify-center bg-gray-50 p-4">
            <div className="w-full max-w-md bg-white p-8 shadow-md rounded-lg">
                <h1 className="text-2xl font-bold text-center mb-6">Welcome to Matrimony Connect</h1>

                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Phone Number</label>
                        <input
                            {...register("phone", { required: "Phone number is required" })}
                            type="tel"
                            placeholder="+919876543210"
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                        {errors.phone && <p className="text-red-500 text-xs mt-1">{errors.phone.message}</p>}
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none disabled:bg-blue-300"
                    >
                        {loading ? "Processing..." : "Continue with Phone"}
                    </button>

                    <div className="relative my-4">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-300"></div>
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="px-2 bg-white text-gray-500">Or continue with</span>
                        </div>
                    </div>

                    <button
                        type="button"
                        onClick={handleGoogleLogin}
                        disabled={loading}
                        className="w-full flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none disabled:bg-gray-100"
                    >
                        <img className="h-5 w-5 mr-2" src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" />
                        Google
                    </button>
                </form>
            </div>
        </div>
    );
}
