"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";
import { fetchOnboardingContext, saveOnboardingProgress } from "../onboardingHelpers";
import { errorIdFor, getInputA11y, onboardingRules } from "../../../validation/rules";

export default function Step3() {
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
      education_level: "",
      education_field: "",
      profession: "",
      income_band: "",
      company: "",
    },
  });

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      const { profile, resumeState } = await fetchOnboardingContext();
      if (!mounted) return;

      const stepDraft = resumeState?.draft?.step3 || {};
      setResumeDraft(resumeState?.draft || {});

      reset({
        education_level: stepDraft.education_level ?? profile.educationLevel ?? "",
        education_field: stepDraft.education_field ?? profile.educationField ?? "",
        profession: stepDraft.profession ?? profile.profession ?? "",
        income_band: stepDraft.income_band ?? profile.incomeBand ?? "",
        company: stepDraft.company ?? profile.company ?? "",
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
        educationLevel: data.education_level,
        educationField: data.education_field,
        profession: data.profession,
        incomeBand: data.income_band,
        company: data.company,
      });

      const nextRoute = "/step-4";
      const nextDraft = await saveOnboardingProgress({
        nextStep: 4,
        route: nextRoute,
        resumeDraft,
        stepKey: "step3",
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
      <h2 className="text-2xl font-bold mb-6">Education & Career</h2>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-6">
        <div>
          <label htmlFor="education_level" className="block text-sm font-medium text-gray-700">Highest Qualification</label>
          <select
            {...register("education_level", onboardingRules.step3.education_level)}
            {...getInputA11y("education_level", errors)}
            id="education_level"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">Select Qualification</option>
            <option value="high_school">High School</option>
            <option value="bachelors">Bachelor&apos;s Degree</option>
            <option value="masters">Master&apos;s Degree</option>
            <option value="phd">PhD / Doctorate</option>
            <option value="other">Other</option>
          </select>
          {errors.education_level && <p id={errorIdFor("education_level")} className="text-red-500 text-xs mt-1" role="alert">{errors.education_level.message}</p>}
        </div>

        <div>
          <label htmlFor="education_field" className="block text-sm font-medium text-gray-700">Education Field</label>
          <input
            {...register("education_field")}
            {...getInputA11y("education_field", errors)}
            id="education_field"
            placeholder="e.g. Computer Science, Arts"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        <div>
          <label htmlFor="profession" className="block text-sm font-medium text-gray-700">Profession</label>
          <input
            {...register("profession", onboardingRules.step3.profession)}
            {...getInputA11y("profession", errors)}
            id="profession"
            placeholder="e.g. Software Engineer"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
          {errors.profession && <p id={errorIdFor("profession")} className="text-red-500 text-xs mt-1" role="alert">{errors.profession.message}</p>}
        </div>

        <div>
          <label htmlFor="income_band" className="block text-sm font-medium text-gray-700">Annual Income</label>
          <select
            {...register("income_band", onboardingRules.step3.income_band)}
            {...getInputA11y("income_band", errors)}
            id="income_band"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">Select Income Range</option>
            <option value="below_5L">Below 5 LPA</option>
            <option value="5-10L">5 - 10 LPA</option>
            <option value="10-25L">10 - 25 LPA</option>
            <option value="25-50L">25 - 50 LPA</option>
            <option value="50L+">Above 50 LPA</option>
          </select>
          {errors.income_band && <p id={errorIdFor("income_band")} className="text-red-500 text-xs mt-1" role="alert">{errors.income_band.message}</p>}
        </div>

        <div>
          <label htmlFor="company" className="block text-sm font-medium text-gray-700">Company Name</label>
          <input
            {...register("company")}
            {...getInputA11y("company", errors)}
            id="company"
            placeholder="Optional"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        <button
          type="submit"
          disabled={initializing}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none disabled:opacity-60"
        >
          Next: Lifestyle & Bio
        </button>
      </form>
    </div>
  );
}
