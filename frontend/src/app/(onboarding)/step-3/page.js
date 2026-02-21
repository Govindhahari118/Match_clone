"use client";

import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";

export default function Step3() {
    const router = useRouter();
    const { register, handleSubmit, formState: { errors } } = useForm();

    const onSubmit = async (data) => {
        try {
            await api.post("/profile/update", { step: 3, ...data });
            router.push("/onboarding/step-4");
        } catch (error) {
            alert("Failed to save details: " + (error.response?.data?.error || error.message));
        }
    };

    return (
        <div>
            <h2 className="text-2xl font-bold mb-6">Education & Career</h2>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                <div>
                    <label className="block text-sm font-medium text-gray-700">Highest Qualification</label>
                    <select
                        {...register("education_level", { required: "Education level is required" })}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="">Select Qualification</option>
                        <option value="high_school">High School</option>
                        <option value="bachelors">Bachelor&apos;s Degree</option>
                        <option value="masters">Master&apos;s Degree</option>
                        <option value="phd">PhD / Doctorate</option>
                        <option value="other">Other</option>
                    </select>
                    {errors.education_level && <p className="text-red-500 text-xs mt-1">{errors.education_level.message}</p>}
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Education Field</label>
                    <input
                        {...register("education_field")}
                        placeholder="e.g. Computer Science, Arts"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Profession</label>
                    <input
                        {...register("profession", { required: "Profession is required" })}
                        placeholder="e.g. Software Engineer"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                    {errors.profession && <p className="text-red-500 text-xs mt-1">{errors.profession.message}</p>}
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Annual Income</label>
                    <select
                        {...register("income_band", { required: "Income is required" })}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="">Select Income Range</option>
                        <option value="below_5L">Below 5 LPA</option>
                        <option value="5-10L">5 - 10 LPA</option>
                        <option value="10-25L">10 - 25 LPA</option>
                        <option value="25-50L">25 - 50 LPA</option>
                        <option value="50L+">Above 50 LPA</option>
                    </select>
                    {errors.income_band && <p className="text-red-500 text-xs mt-1">{errors.income_band.message}</p>}
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Company Name</label>
                    <input
                        {...register("company")}
                        placeholder="Optional"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                </div>

                <button
                    type="submit"
                    className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none"
                >
                    Next: Lifestyle & Bio
                </button>
            </form>
        </div>
    );
}
