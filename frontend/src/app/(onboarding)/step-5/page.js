"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";
import { arrayToCsv, fetchOnboardingContext, saveOnboardingProgress } from "../onboardingHelpers";
import { errorIdFor, getInputA11y, onboardingRules } from "../../../validation/rules";

function normalizeReligion(value) {
  if (!value) return "Any";
  if (value.toLowerCase() === "any") return "Any";
  return value;
}

export default function Step5() {
  const router = useRouter();
  const [resumeDraft, setResumeDraft] = useState({});
  const [initializing, setInitializing] = useState(true);
  const {
    register,
    handleSubmit,
    reset,
    getValues,
    formState: { errors },
  } = useForm({
    defaultValues: {
      min_age: 21,
      max_age: 35,
      marital_status: "any",
      religion: "Any",
      preferred_locations: "",
      min_income_band: "",
    },
  });

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      const { profile, resumeState } = await fetchOnboardingContext();
      if (!mounted) return;

      const prefs = profile.partnerPreference || {};
      const stepDraft = resumeState?.draft?.step5 || {};
      setResumeDraft(resumeState?.draft || {});

      reset({
        min_age: stepDraft.min_age ?? prefs.minAge ?? 21,
        max_age: stepDraft.max_age ?? prefs.maxAge ?? 35,
        marital_status: stepDraft.marital_status ?? "any",
        religion: stepDraft.religion ?? normalizeReligion(prefs.preferredReligions?.[0] || "Any"),
        preferred_locations: stepDraft.preferred_locations ?? arrayToCsv(prefs.preferredLocations),
        min_income_band: stepDraft.min_income_band ?? prefs.minIncomeBand ?? "",
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
      await api.put("/users/profile/preferences", {
        min_age: Number(data.min_age),
        max_age: Number(data.max_age),
        marital_status: data.marital_status,
        religion: normalizeReligion(data.religion),
        preferred_locations: data.preferred_locations,
        min_income_band: data.min_income_band || null,
      });

      const destination = "/matches";
      const nextDraft = await saveOnboardingProgress({
        nextStep: 5,
        route: destination,
        resumeDraft,
        stepKey: "step5",
        stepValues: data,
      });
      setResumeDraft(nextDraft);
      router.push(destination);
    } catch (error) {
      alert("Failed to save preferences: " + (error.response?.data?.error || error.message));
    }
  };

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">Partner Preferences</h2>
      <p className="text-gray-600 mb-6">Tell us who you&apos;re looking for to get better matches.</p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="min_age" className="block text-sm font-medium text-gray-700">Min Age</label>
            <input
              {...register("min_age", onboardingRules.step5.min_age)}
              {...getInputA11y("min_age", errors)}
              id="min_age"
              type="number"
              placeholder="18"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
            {errors.min_age && <p id={errorIdFor("min_age")} className="text-red-500 text-xs mt-1" role="alert">{errors.min_age.message}</p>}
          </div>
          <div>
            <label htmlFor="max_age" className="block text-sm font-medium text-gray-700">Max Age</label>
            <input
              {...register("max_age", {
                ...onboardingRules.step5.max_age,
                validate: (value) =>
                  Number(value) >= Number(getValues("min_age")) || "Max age should be greater than or equal to min age",
              })}
              {...getInputA11y("max_age", errors)}
              id="max_age"
              type="number"
              placeholder="35"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
            {errors.max_age && <p id={errorIdFor("max_age")} className="text-red-500 text-xs mt-1" role="alert">{errors.max_age.message}</p>}
          </div>
        </div>

        <div>
          <label htmlFor="marital_status" className="block text-sm font-medium text-gray-700">Marital Status Preference</label>
          <select
            {...register("marital_status")}
            {...getInputA11y("marital_status", errors)}
            id="marital_status"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="any">Doesn&apos;t Matter</option>
            <option value="never_married">Never Married</option>
            <option value="divorced">Divorced</option>
            <option value="widowed">Widowed</option>
          </select>
        </div>

        <div>
          <label htmlFor="religion" className="block text-sm font-medium text-gray-700">Religion Preference</label>
          <select
            {...register("religion")}
            {...getInputA11y("religion", errors)}
            id="religion"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="Any">Open to all</option>
            <option value="Hindu">Hindu</option>
            <option value="Muslim">Muslim</option>
            <option value="Christian">Christian</option>
            <option value="Sikh">Sikh</option>
            <option value="Jain">Jain</option>
          </select>
        </div>

        <div>
          <label htmlFor="preferred_locations" className="block text-sm font-medium text-gray-700">Preferred Locations</label>
          <input
            {...register("preferred_locations")}
            {...getInputA11y("preferred_locations", errors)}
            id="preferred_locations"
            placeholder="e.g. Mumbai, Delhi, USA (comma separated)"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        <div>
          <label htmlFor="min_income_band" className="block text-sm font-medium text-gray-700">Min Income</label>
          <select
            {...register("min_income_band")}
            {...getInputA11y("min_income_band", errors)}
            id="min_income_band"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">Doesn&apos;t Matter</option>
            <option value="5-10L">5 LPA+</option>
            <option value="10-25L">10 LPA+</option>
            <option value="25-50L">25 LPA+</option>
            <option value="50L+">50 LPA+</option>
          </select>
        </div>

        <button
          type="submit"
          disabled={initializing}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-pink-600 hover:bg-pink-700 focus:outline-none disabled:opacity-60"
        >
          Finish & See Matches
        </button>
      </form>
    </div>
  );
}
