"use client";

import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";

export default function Step2() {
    const router = useRouter();
    const { register, handleSubmit, formState: { errors } } = useForm();

    const onSubmit = async (data) => {
        try {
            await api.post("/profile/update", { step: 2, ...data });
            router.push("/onboarding/step-3");
        } catch (error) {
            alert("Failed to save details: " + (error.response?.data?.error || error.message));
        }
    };

    return (
        <div>
            <h2 className="text-2xl font-bold mb-6">Location & Community</h2>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Country</label>
                        <input
                            {...register("country", { required: "Country is required" })}
                            defaultValue="India"
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                        {errors.country && <p className="text-red-500 text-xs mt-1">{errors.country.message}</p>}
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">State</label>
                        <input
                            {...register("state", { required: "State is required" })}
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                        {errors.state && <p className="text-red-500 text-xs mt-1">{errors.state.message}</p>}
                    </div>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">City</label>
                    <input
                        {...register("city", { required: "City is required" })}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                    {errors.city && <p className="text-red-500 text-xs mt-1">{errors.city.message}</p>}
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Religion</label>
                    <select
                        {...register("religion", { required: "Religion is required" })}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="">Select Religion</option>
                        <option value="Hindu">Hindu</option>
                        <option value="Muslim">Muslim</option>
                        <option value="Sikh">Sikh</option>
                        <option value="Christian">Christian</option>
                        <option value="Jain">Jain</option>
                        <option value="Buddhist">Buddhist</option>
                        <option value="Other">Other</option>
                    </select>
                    {errors.religion && <p className="text-red-500 text-xs mt-1">{errors.religion.message}</p>}
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Caste</label>
                    <input
                        {...register("caste")}
                        placeholder="Optional"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Mother Tongue</label>
                    <input
                        {...register("mother_tongue", { required: "Mother tongue is required" })}
                        placeholder="e.g. Hindi, English, Tamil"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                    {errors.mother_tongue && <p className="text-red-500 text-xs mt-1">{errors.mother_tongue.message}</p>}
                </div>

                <button
                    type="submit"
                    className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none"
                >
                    Next: Education & Career
                </button>
            </form>
        </div>
    );
}
