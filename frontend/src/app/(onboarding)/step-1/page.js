"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";
import { fetchOnboardingContext, saveOnboardingProgress, toDateInput } from "../onboardingHelpers";
import { errorIdFor, getInputA11y, onboardingRules } from "../../../validation/rules";

export default function Step1() {
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
      first_name: "",
      last_name: "",
      date_of_birth: "",
      gender: "",
      marital_status: "",
    },
  });

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      const { profile, resumeState } = await fetchOnboardingContext();
      if (!mounted) return;

      const stepDraft = resumeState?.draft?.step1 || {};
      setResumeDraft(resumeState?.draft || {});

      reset({
        first_name: stepDraft.first_name ?? profile.firstName ?? "",
        last_name: stepDraft.last_name ?? profile.lastName ?? "",
        date_of_birth: stepDraft.date_of_birth ?? toDateInput(profile.dateOfBirth),
        gender: stepDraft.gender ?? profile.gender ?? "",
        marital_status: stepDraft.marital_status ?? profile.maritalStatus ?? "",
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
        firstName: data.first_name,
        lastName: data.last_name,
        dateOfBirth: data.date_of_birth,
        gender: data.gender,
        maritalStatus: data.marital_status,
      });

      const nextRoute = "/step-2";
      const nextDraft = await saveOnboardingProgress({
        nextStep: 2,
        route: nextRoute,
        resumeDraft,
        stepKey: "step1",
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
      <h2 className="text-2xl font-bold mb-6">Basic Details</h2>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="first_name" className="block text-sm font-medium text-gray-700">First Name</label>
            <input
              {...register("first_name", onboardingRules.step1.first_name)}
              {...getInputA11y("first_name", errors)}
              id="first_name"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
            {errors.first_name && <p id={errorIdFor("first_name")} className="text-red-500 text-xs mt-1" role="alert">{errors.first_name.message}</p>}
          </div>
          <div>
            <label htmlFor="last_name" className="block text-sm font-medium text-gray-700">Last Name</label>
            <input
              {...register("last_name", onboardingRules.step1.last_name)}
              {...getInputA11y("last_name", errors)}
              id="last_name"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
            {errors.last_name && <p id={errorIdFor("last_name")} className="text-red-500 text-xs mt-1" role="alert">{errors.last_name.message}</p>}
          </div>
        </div>

        <div>
          <label htmlFor="date_of_birth" className="block text-sm font-medium text-gray-700">Date of Birth</label>
          <input
            {...register("date_of_birth", onboardingRules.step1.date_of_birth)}
            {...getInputA11y("date_of_birth", errors)}
            id="date_of_birth"
            type="date"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
          {errors.date_of_birth && <p id={errorIdFor("date_of_birth")} className="text-red-500 text-xs mt-1" role="alert">{errors.date_of_birth.message}</p>}
        </div>

        <div>
          <label htmlFor="gender" className="block text-sm font-medium text-gray-700">Gender</label>
          <select
            {...register("gender", onboardingRules.step1.gender)}
            {...getInputA11y("gender", errors)}
            id="gender"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">Select Gender</option>
            <option value="male">Male</option>
            <option value="female">Female</option>
            <option value="other">Other</option>
          </select>
          {errors.gender && <p id={errorIdFor("gender")} className="text-red-500 text-xs mt-1" role="alert">{errors.gender.message}</p>}
        </div>

        <div>
          <label htmlFor="marital_status" className="block text-sm font-medium text-gray-700">Marital Status</label>
          <select
            {...register("marital_status", onboardingRules.step1.marital_status)}
            {...getInputA11y("marital_status", errors)}
            id="marital_status"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">Select Status</option>
            <option value="never_married">Never Married</option>
            <option value="divorced">Divorced</option>
            <option value="widowed">Widowed</option>
            <option value="annulled">Annulled</option>
          </select>
          {errors.marital_status && <p id={errorIdFor("marital_status")} className="text-red-500 text-xs mt-1" role="alert">{errors.marital_status.message}</p>}
        </div>

        <button
          type="submit"
          disabled={initializing}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none disabled:opacity-60"
        >
          Next: Location & Community
        </button>
      </form>
    </div>
  );
}
