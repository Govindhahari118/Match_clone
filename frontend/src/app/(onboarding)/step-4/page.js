"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";
import { arrayToCsv, csvToArray, fetchOnboardingContext, saveOnboardingProgress } from "../onboardingHelpers";
import { errorIdFor, getInputA11y, onboardingRules } from "../../../validation/rules";

export default function Step4() {
  const router = useRouter();
  const [resumeDraft, setResumeDraft] = useState({});
  const [initializing, setInitializing] = useState(true);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      food_habit: "",
      drinks: "no",
      smokes: "no",
      hobbies: "",
      bio: "",
    },
  });

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      const { profile, resumeState } = await fetchOnboardingContext();
      if (!mounted) return;

      const stepDraft = resumeState?.draft?.step4 || {};
      setResumeDraft(resumeState?.draft || {});

      reset({
        food_habit: stepDraft.food_habit ?? profile.foodHabit ?? "",
        drinks: stepDraft.drinks ?? profile.drinks ?? "no",
        smokes: stepDraft.smokes ?? profile.smokes ?? "no",
        hobbies: stepDraft.hobbies ?? arrayToCsv(profile.hobbies),
        bio: stepDraft.bio ?? profile.bio ?? "",
      });

      setInitializing(false);
    };

    load();
    return () => {
      mounted = false;
    };
  }, [reset]);

  const onSubmit = async (data) => {
    try {
      await api.put("/users/profile", {
        foodHabit: data.food_habit,
        drinks: data.drinks,
        smokes: data.smokes,
        hobbies: csvToArray(data.hobbies),
        bio: data.bio,
      });

      const nextRoute = "/step-5";
      const nextDraft = await saveOnboardingProgress({
        nextStep: 5,
        route: nextRoute,
        resumeDraft,
        stepKey: "step4",
        stepValues: data,
      });
      setResumeDraft(nextDraft);
      router.push(nextRoute);
    } catch (error) {
      alert("Failed to save details: " + (error.response?.data?.error || error.message));
    }
  };

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">Lifestyle & Bio</h2>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-6">
        <div>
          <label htmlFor="food_habit" className="block text-sm font-medium text-gray-700">Dietary Preference</label>
          <select
            {...register("food_habit", onboardingRules.step4.food_habit)}
            {...getInputA11y("food_habit", errors)}
            id="food_habit"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">Select Diet</option>
            <option value="vegetarian">Vegetarian</option>
            <option value="non_vegetarian">Non-Vegetarian</option>
            <option value="vegan">Vegan</option>
            <option value="eggetarian">Eggetarian</option>
          </select>
          {errors.food_habit && <p id={errorIdFor("food_habit")} className="text-red-500 text-xs mt-1" role="alert">{errors.food_habit.message}</p>}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="drinks" className="block text-sm font-medium text-gray-700">Drinking Habits</label>
            <select
              {...register("drinks")}
              {...getInputA11y("drinks", errors)}
              id="drinks"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="no">No</option>
              <option value="occasionally">Occasionally</option>
              <option value="regularly">Regularly</option>
            </select>
          </div>
          <div>
            <label htmlFor="smokes" className="block text-sm font-medium text-gray-700">Smoking Habits</label>
            <select
              {...register("smokes")}
              {...getInputA11y("smokes", errors)}
              id="smokes"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="no">No</option>
              <option value="occasionally">Occasionally</option>
              <option value="regularly">Regularly</option>
            </select>
          </div>
        </div>

        <div>
          <label htmlFor="hobbies" className="block text-sm font-medium text-gray-700">Hobbies</label>
          <input
            {...register("hobbies")}
            {...getInputA11y("hobbies", errors)}
            id="hobbies"
            placeholder="e.g. Reading, Traveling, Music (comma separated)"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
          <p className="text-xs text-gray-500 mt-1">Separate with commas</p>
        </div>

        <div>
          <label htmlFor="bio" className="block text-sm font-medium text-gray-700">About Me</label>
          <textarea
            {...register("bio", onboardingRules.step4.bio)}
            {...getInputA11y("bio", errors)}
            id="bio"
            rows="4"
            placeholder="Tell us about yourself..."
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          ></textarea>
          {errors.bio && <p id={errorIdFor("bio")} className="text-red-500 text-xs mt-1" role="alert">{errors.bio.message}</p>}
        </div>

        <button
          type="submit"
          disabled={initializing}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none disabled:opacity-60"
        >
          Continue to Preferences
        </button>
      </form>
    </div>
  );
}
