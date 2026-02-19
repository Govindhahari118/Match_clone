"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";

export default function OtpPage() {
    const [loading, setLoading] = useState(false);
    const router = useRouter();
    const searchParams = useSearchParams();
    const phone = searchParams.get("phone");
    const otpId = searchParams.get("otp_id");
    const { login } = useAuth();
    const { register, handleSubmit, formState: { errors } } = useForm();

    const onSubmit = async (data) => {
        setLoading(true);
        try {
            const response = await api.post("/auth/verify-otp", {
                phone,
                otp: data.otp,
                otp_id: otpId
            });

            if (response.data.success) {
                // Log in user
                login(
                    response.data.user,
                    response.data.access_token,
                    response.data.refresh_token
                );

                if (response.data.user.isNewUser) {
                    router.push("/onboarding/step-1");
                } else {
                    router.push("/");
                }
            }
        } catch (error) {
            alert(error.response?.data?.error || "OTP verification failed");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex min-h-screen flex-col items-center justify-center bg-gray-50 p-4">
            <div className="w-full max-w-md bg-white p-8 shadow-md rounded-lg">
                <h1 className="text-2xl font-bold text-center mb-6">Verify OTP</h1>
                <p className="text-center text-gray-500 mb-6">Enter code sent to {phone}</p>

                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Enter OTP</label>
                        <input
                            {...register("otp", { required: "OTP is required", minLength: 6, maxLength: 6 })}
                            type="text"
                            placeholder="123456"
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-center text-2xl tracking-widest"
                        />
                        {errors.otp && <p className="text-red-500 text-xs mt-1">{errors.otp.message}</p>}
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none disabled:bg-green-300"
                    >
                        {loading ? "Verifying..." : "Verify & Login"}
                    </button>
                </form>
            </div>
        </div>
    );
}
