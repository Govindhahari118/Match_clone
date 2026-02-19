"use client";

import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";

export default function Step4() {
    const router = useRouter();
    const { register, handleSubmit, formState: { errors } } = useForm();

    const onSubmit = async (data) => {
        try {
            await api.post("/profile/update", { step: 4, ...data });
            // Final step -> redirect to home
            router.push("/onboarding/step-5");
        } catch (error) {
            alert("Failed to save details: " + (error.response?.data?.error || error.message));
        }
    };

    return (
        <div>
            <h2 className="text-2xl font-bold mb-6">Lifestyle & Bio</h2>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                <div>
                    <label className="block text-sm font-medium text-gray-700">Dietary Preference</label>
                    <select
                        {...register("food_habit", { required: "Dietary preference is required" })}
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="">Select Diet</option>
                        <option value="vegetarian">Vegetarian</option>
                        <option value="non_vegetarian">Non-Vegetarian</option>
                        <option value="vegan">Vegan</option>
                        <option value="eggetarian">Eggetarian</option>
                    </select>
                    {errors.food_habit && <p className="text-red-500 text-xs mt-1">{errors.food_habit.message}</p>}
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Drinking Habits</label>
                        <select
                            {...register("drinks")}
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        >
                            <option value="no">No</option>
                            <option value="occasionally">Occasionally</option>
                            <option value="regularly">Regularly</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Smoking Habits</label>
                        <select
                            {...register("smokes")}
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        >
                            <option value="no">No</option>
                            <option value="occasionally">Occasionally</option>
                            <option value="regularly">Regularly</option>
                        </select>
                    </div>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Hobbies</label>
                    <input
                        {...register("hobbies")}
                        placeholder="e.g. Reading, Traveling, Music (comma separated)"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                    <p className="text-xs text-gray-500 mt-1">Separate with commas</p>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">About Me</label>
                    <textarea
                        {...register("bio", { required: "Bio is required", minLength: { value: 20, message: "Bio must be at least 20 characters" } })}
                        rows="4"
                        placeholder="Tell us about yourself..."
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    ></textarea>
                    {errors.bio && <p className="text-red-500 text-xs mt-1">{errors.bio.message}</p>}
                </div>

                <button
                    type="submit"
                    className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none"
                >
                    Complete Profile
                </button>
            </form>
        </div>
    );
}
