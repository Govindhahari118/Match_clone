"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";
import { fetchOnboardingContext, saveOnboardingProgress, toDateInput } from "../onboardingHelpers";
import { errorIdFor, getInputA11y, onboardingRules } from "../../../validation/rules";

const LANDING_DRAFT_KEY = "landingDraft";

function readLandingDraft() {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.sessionStorage.getItem(LANDING_DRAFT_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object") return null;
    return parsed;
  } catch {
    return null;
  }
}

export default function Step1() {
  const router = useRouter();
  const [resumeDraft, setResumeDraft] = useState({});
  const [initializing, setInitializing] = useState(true);
  const {
    register,
    handleSubmit,
    reset,
    watch,
    getValues,
    formState: { errors },
  } = useForm({
    defaultValues: {
      profile_created_for: "self",
      consent_confirmed: false,
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
      const landingDraft = readLandingDraft();
      setResumeDraft(resumeState?.draft || {});

      reset({
        profile_created_for: stepDraft.profile_created_for ?? landingDraft?.createdFor ?? "self",
        consent_confirmed: stepDraft.consent_confirmed ?? false,
        first_name: stepDraft.first_name ?? profile.firstName ?? landingDraft?.firstName ?? "",
        last_name: stepDraft.last_name ?? profile.lastName ?? "",
        date_of_birth: stepDraft.date_of_birth ?? toDateInput(profile.dateOfBirth),
        gender: stepDraft.gender ?? profile.gender ?? landingDraft?.gender ?? "",
        marital_status: stepDraft.marital_status ?? profile.maritalStatus ?? "",
      });

      if (landingDraft && typeof window !== "undefined") {
        window.sessionStorage.removeItem(LANDING_DRAFT_KEY);
      }

      setInitializing(false);
    };

    load();
    return () => {
      mounted = false;
    };
  }, [reset]);

  const createdForValue = watch("profile_created_for");

  const mapCreatedForToRole = (value) => {
    if (value === "self") return "self";
    if (value === "son" || value === "daughter") return "parent";
    return "relative";
  };

  const onSubmit = async (data) => {
    try {
      await api.put("/users/profile", {
        role: mapCreatedForToRole(data.profile_created_for),
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
      <div className="onboarding-step-head">
        <p className="section-label">Step 1 - About you</p>
        <h2 className="section-title">Basic details</h2>
        <p className="section-copy">
          Add the essentials so we can personalize your matches and profile summary.
        </p>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="form-grid">
        <div>
          <label htmlFor="profile_created_for" className="form-label">Profile created for</label>
          <select
            {...register("profile_created_for", onboardingRules.step1.profile_created_for)}
            {...getInputA11y("profile_created_for", errors)}
            id="profile_created_for"
            className="form-input"
          >
            <option value="self">Myself</option>
            <option value="daughter">Daughter</option>
            <option value="son">Son</option>
            <option value="sibling">Sibling</option>
            <option value="relative">Relative</option>
          </select>
          {errors.profile_created_for && (
            <p id={errorIdFor("profile_created_for")} className="form-error" role="alert">
              {errors.profile_created_for.message}
            </p>
          )}
        </div>

        {createdForValue !== "self" && (
          <div className="form-consent">
            <label className="checkbox-row">
              <input
                type="checkbox"
                {...register("consent_confirmed", {
                  validate: (value) => {
                    const selection = getValues("profile_created_for");
                    if (selection !== "self" && !value) {
                      return "Consent confirmation is required.";
                    }
                    return true;
                  },
                })}
              />
              <span>I confirm I have consent to create and manage this profile.</span>
            </label>
            {errors.consent_confirmed && (
              <p id={errorIdFor("consent_confirmed")} className="form-error" role="alert">
                {errors.consent_confirmed.message}
              </p>
            )}
          </div>
        )}

        <div className="form-grid-2">
          <div>
            <label htmlFor="first_name" className="form-label">First Name</label>
            <input
              {...register("first_name", onboardingRules.step1.first_name)}
              {...getInputA11y("first_name", errors)}
              id="first_name"
              className="form-input"
            />
            {errors.first_name && <p id={errorIdFor("first_name")} className="form-error" role="alert">{errors.first_name.message}</p>}
          </div>
          <div>
            <label htmlFor="last_name" className="form-label">Last Name</label>
            <input
              {...register("last_name", onboardingRules.step1.last_name)}
              {...getInputA11y("last_name", errors)}
              id="last_name"
              className="form-input"
            />
            {errors.last_name && <p id={errorIdFor("last_name")} className="form-error" role="alert">{errors.last_name.message}</p>}
          </div>
        </div>

        <div>
          <label htmlFor="date_of_birth" className="form-label">Date of Birth</label>
          <input
            {...register("date_of_birth", onboardingRules.step1.date_of_birth)}
            {...getInputA11y("date_of_birth", errors)}
            id="date_of_birth"
            type="date"
            className="form-input"
          />
          {errors.date_of_birth && <p id={errorIdFor("date_of_birth")} className="form-error" role="alert">{errors.date_of_birth.message}</p>}
        </div>

        <div>
          <label htmlFor="gender" className="form-label">Gender</label>
          <select
            {...register("gender", onboardingRules.step1.gender)}
            {...getInputA11y("gender", errors)}
            id="gender"
            className="form-input"
          >
            <option value="">Select Gender</option>
            <option value="male">Male</option>
            <option value="female">Female</option>
            <option value="other">Other</option>
          </select>
          {errors.gender && <p id={errorIdFor("gender")} className="form-error" role="alert">{errors.gender.message}</p>}
        </div>

        <div>
          <label htmlFor="marital_status" className="form-label">Marital Status</label>
          <select
            {...register("marital_status", onboardingRules.step1.marital_status)}
            {...getInputA11y("marital_status", errors)}
            id="marital_status"
            className="form-input"
          >
            <option value="">Select Status</option>
            <option value="never_married">Never Married</option>
            <option value="divorced">Divorced</option>
            <option value="widowed">Widowed</option>
            <option value="annulled">Annulled</option>
          </select>
          {errors.marital_status && <p id={errorIdFor("marital_status")} className="form-error" role="alert">{errors.marital_status.message}</p>}
        </div>

        <button
          type="submit"
          disabled={initializing}
          className="button button-primary cta-full"
        >
          Next: Location & Community
        </button>
        <p className="form-note">You can update these details anytime from your profile.</p>
      </form>
    </div>
  );
}

