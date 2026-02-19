"use client";

import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";

export default function Step5() {
    const router = useRouter();
    const { register, handleSubmit, formState: { errors } } = useForm();

    const onSubmit = async (data) => {
        try {
            // Step 5: Partner Preferences + Completion
            // We might want to save preferences to a different table or as part of profile
            // For now, assuming profile update or separate endpoint if schema dictates
            // Based on previous schema, PartnerPreference is a separate model linked to User

            await api.post("/profile/preferences", { ...data });

            // Update profile completion status final
            await api.post("/profile/update", { step: 5, completion_percentage: 100 });

            router.push("/");
        } catch (error) {
            alert("Failed to save preferences: " + (error.response?.data?.error || error.message));
        }
    };

    return (
        <div>
            <h2 className="text-2xl font-bold mb-6">Partner Preferences</h2>
            <p className="text-gray-600 mb-6">Tell us who you're looking for to get better matches.</p>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Min Age</label>
                        <input
                            {...register("min_age", { required: "Min age is required", min: 18, max: 70 })}
                            type="number"
                            placeholder="18"
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                        {errors.min_age && <p className="text-red-500 text-xs mt-1">{errors.min_age.message}</p>}
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Max Age</label>
                        <input
                            {...register("max_age", { required: "Max age is required", min: 18, max: 70 })}
                            type="number"
                            placeholder="35"
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                        {errors.max_age && <p className="text-red-500 text-xs mt-1">{errors.max_age.message}</p>}
                    </div>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Marital Status Preference</label>
                    <select
                        {...register("marital_status")}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="any">Doesn't Matter</option>
                        <option value="never_married">Never Married</option>
                        <option value="divorced">Divorced</option>
                        <option value="widowed">Widowed</option>
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Religion Preference</label>
                    <select
                        {...register("religion")}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="any">Open to all</option>
                        <option value="Hindu">Hindu</option>
                        <option value="Muslim">Muslim</option>
                        <option value="Christian">Christian</option>
                        <option value="Sikh">Sikh</option>
                        <option value="Jain">Jain</option>
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Preferred Locations</label>
                    <input
                        {...register("preferred_locations")}
                        placeholder="e.g. Mumbai, Delhi, USA (comma separated)"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Min Income</label>
                    <select
                        {...register("min_income_band")}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="">Doesn't Matter</option>
                        <option value="5-10L">5 LPA+</option>
                        <option value="10-25L">10 LPA+</option>
                        <option value="25-50L">25 LPA+</option>
                        <option value="50L+">50 LPA+</option>
                    </select>
                </div>


                <button
                    type="submit"
                    className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-pink-600 hover:bg-pink-700 focus:outline-none"
                >
                    Finish & See Matches
                </button>
            </form>
        </div>
    );
}
