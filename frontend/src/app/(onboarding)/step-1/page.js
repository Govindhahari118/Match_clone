"use client";

import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import { useAuth } from "../../../context/AuthContext";
import api from "../../../services/api";

export default function Step1() {
    const router = useRouter();
    const { user } = useAuth();
    const { register, handleSubmit, formState: { errors } } = useForm({
        defaultValues: {
            first_name: user?.first_name || "",
            last_name: user?.last_name || "",
            date_of_birth: "",
            gender: "",
            marital_status: "",
        },
    });

    const onSubmit = async (data) => {
        try {
            await api.post("/profile/update", { step: 1, ...data });
            router.push("/onboarding/step-2");
        } catch (error) {
            alert("Failed to save details: " + (error.response?.data?.error || error.message));
        }
    };

    return (
        <div>
            <h2 className="text-2xl font-bold mb-6">Basic Details</h2>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">First Name</label>
                        <input
                            {...register("first_name", { required: "First name is required" })}
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                        {errors.first_name && <p className="text-red-500 text-xs mt-1">{errors.first_name.message}</p>}
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Last Name</label>
                        <input
                            {...register("last_name", { required: "Last name is required" })}
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                        {errors.last_name && <p className="text-red-500 text-xs mt-1">{errors.last_name.message}</p>}
                    </div>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Date of Birth</label>
                    <input
                        {...register("date_of_birth", { required: "Date of birth is required" })}
                        type="date"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                    {errors.date_of_birth && <p className="text-red-500 text-xs mt-1">{errors.date_of_birth.message}</p>}
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Gender</label>
                    <select
                        {...register("gender", { required: "Gender is required" })}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="">Select Gender</option>
                        <option value="male">Male</option>
                        <option value="female">Female</option>
                        <option value="other">Other</option>
                    </select>
                    {errors.gender && <p className="text-red-500 text-xs mt-1">{errors.gender.message}</p>}
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Marital Status</label>
                    <select
                        {...register("marital_status", { required: "Marital status is required" })}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="">Select Status</option>
                        <option value="never_married">Never Married</option>
                        <option value="divorced">Divorced</option>
                        <option value="widowed">Widowed</option>
                        <option value="annulled">Annulled</option>
                    </select>
                    {errors.marital_status && <p className="text-red-500 text-xs mt-1">{errors.marital_status.message}</p>}
                </div>

                <button
                    type="submit"
                    className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none"
                >
                    Next: Location & Community
                </button>
            </form>
        </div>
    );
}
