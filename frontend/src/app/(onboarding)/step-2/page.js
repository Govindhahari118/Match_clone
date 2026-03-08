"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";
import { fetchOnboardingContext, saveOnboardingProgress } from "../onboardingHelpers";
import { errorIdFor, getInputA11y, onboardingRules } from "../../../validation/rules";

export default function Step2() {
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
      country: "India",
      state: "",
      city: "",
      religion: "",
      caste: "",
      mother_tongue: "",
    },
  });

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      const { profile, resumeState } = await fetchOnboardingContext();
      if (!mounted) return;

      const stepDraft = resumeState?.draft?.step2 || {};
      setResumeDraft(resumeState?.draft || {});

      reset({
        country: stepDraft.country ?? profile.country ?? "India",
        state: stepDraft.state ?? profile.state ?? "",
        city: stepDraft.city ?? profile.city ?? "",
        religion: stepDraft.religion ?? profile.religion ?? "",
        caste: stepDraft.caste ?? profile.caste ?? "",
        mother_tongue: stepDraft.mother_tongue ?? profile.motherTongue ?? "",
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
        country: data.country,
        state: data.state,
        city: data.city,
        religion: data.religion,
        caste: data.caste,
        motherTongue: data.mother_tongue,
      });

      const nextRoute = "/step-3";
      const nextDraft = await saveOnboardingProgress({
        nextStep: 3,
        route: nextRoute,
        resumeDraft,
        stepKey: "step2",
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
      <h2 className="text-2xl font-bold mb-6">Location & Community</h2>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="country" className="block text-sm font-medium text-gray-700">Country</label>
            <input
              {...register("country", onboardingRules.step2.country)}
              {...getInputA11y("country", errors)}
              id="country"
              defaultValue="India"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
            {errors.country && <p id={errorIdFor("country")} className="text-red-500 text-xs mt-1" role="alert">{errors.country.message}</p>}
          </div>
          <div>
            <label htmlFor="state" className="block text-sm font-medium text-gray-700">State</label>
            <input
              {...register("state", onboardingRules.step2.state)}
              {...getInputA11y("state", errors)}
              id="state"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
            {errors.state && <p id={errorIdFor("state")} className="text-red-500 text-xs mt-1" role="alert">{errors.state.message}</p>}
          </div>
        </div>

        <div>
          <label htmlFor="city" className="block text-sm font-medium text-gray-700">City</label>
          <input
            {...register("city", onboardingRules.step2.city)}
            {...getInputA11y("city", errors)}
            id="city"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
          {errors.city && <p id={errorIdFor("city")} className="text-red-500 text-xs mt-1" role="alert">{errors.city.message}</p>}
        </div>

        <div>
          <label htmlFor="religion" className="block text-sm font-medium text-gray-700">Religion</label>
          <select
            {...register("religion", onboardingRules.step2.religion)}
            {...getInputA11y("religion", errors)}
            id="religion"
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
          {errors.religion && <p id={errorIdFor("religion")} className="text-red-500 text-xs mt-1" role="alert">{errors.religion.message}</p>}
        </div>

        <div>
          <label htmlFor="caste" className="block text-sm font-medium text-gray-700">Caste</label>
          <input
            {...register("caste")}
            {...getInputA11y("caste", errors)}
            id="caste"
            placeholder="Optional"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        <div>
          <label htmlFor="mother_tongue" className="block text-sm font-medium text-gray-700">Mother Tongue</label>
          <input
            {...register("mother_tongue", onboardingRules.step2.mother_tongue)}
            {...getInputA11y("mother_tongue", errors)}
            id="mother_tongue"
            placeholder="e.g. Hindi, English, Tamil"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
          {errors.mother_tongue && <p id={errorIdFor("mother_tongue")} className="text-red-500 text-xs mt-1" role="alert">{errors.mother_tongue.message}</p>}
        </div>

        <button
          type="submit"
          disabled={initializing}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none disabled:opacity-60"
        >
          Next: Education & Career
        </button>
      </form>
    </div>
  );
}
