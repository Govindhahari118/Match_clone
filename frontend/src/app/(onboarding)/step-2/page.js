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
      district: "",
      religion: "",
      caste: "",
      sub_caste: "",
      gothra: "",
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
        district: stepDraft.district ?? profile.district ?? "",
        religion: stepDraft.religion ?? profile.religion ?? "",
        caste: stepDraft.caste ?? profile.caste ?? "",
        sub_caste: stepDraft.sub_caste ?? profile.subCaste ?? "",
        gothra: stepDraft.gothra ?? profile.gothra ?? "",
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
        district: data.district,
        religion: data.religion,
        caste: data.caste,
        subCaste: data.sub_caste,
        gothra: data.gothra,
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
      <div className="onboarding-step-head">
        <p className="section-label">Step 2 - Location</p>
        <h2 className="section-title">Location & community</h2>
        <p className="section-copy">
          Tell us where you live and how you identify so matches feel more relevant.
        </p>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="form-grid">
        <div className="form-grid-2">
          <div>
            <label htmlFor="country" className="form-label">Country</label>
            <input
              {...register("country", onboardingRules.step2.country)}
              {...getInputA11y("country", errors)}
              id="country"
              defaultValue="India"
              className="form-input"
            />
            {errors.country && <p id={errorIdFor("country")} className="form-error" role="alert">{errors.country.message}</p>}
          </div>
          <div>
            <label htmlFor="state" className="form-label">State</label>
            <input
              {...register("state", onboardingRules.step2.state)}
              {...getInputA11y("state", errors)}
              id="state"
              className="form-input"
            />
            {errors.state && <p id={errorIdFor("state")} className="form-error" role="alert">{errors.state.message}</p>}
          </div>
        </div>

        <div className="form-grid-2">
          <div>
            <label htmlFor="city" className="form-label">City</label>
            <input
              {...register("city", onboardingRules.step2.city)}
              {...getInputA11y("city", errors)}
              id="city"
              className="form-input"
            />
            {errors.city && <p id={errorIdFor("city")} className="form-error" role="alert">{errors.city.message}</p>}
          </div>
          <div>
            <label htmlFor="district" className="form-label">District</label>
            <input
              {...register("district")}
              {...getInputA11y("district", errors)}
              id="district"
              placeholder="Optional"
              className="form-input"
            />
          </div>
        </div>

        <div>
          <label htmlFor="religion" className="form-label">Religion</label>
          <select
            {...register("religion", onboardingRules.step2.religion)}
            {...getInputA11y("religion", errors)}
            id="religion"
            className="form-input"
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
          {errors.religion && <p id={errorIdFor("religion")} className="form-error" role="alert">{errors.religion.message}</p>}
        </div>

        <div>
          <label htmlFor="caste" className="form-label">Caste</label>
          <input
            {...register("caste")}
            {...getInputA11y("caste", errors)}
            id="caste"
            placeholder="Optional"
            className="form-input"
          />
        </div>

        <div>
          <label htmlFor="sub_caste" className="form-label">Sub Caste</label>
          <input
            {...register("sub_caste")}
            {...getInputA11y("sub_caste", errors)}
            id="sub_caste"
            placeholder="Optional"
            className="form-input"
          />
        </div>

        <div>
          <label htmlFor="gothra" className="form-label">Gothra</label>
          <input
            {...register("gothra")}
            {...getInputA11y("gothra", errors)}
            id="gothra"
            placeholder="Optional"
            className="form-input"
          />
        </div>

        <div>
          <label htmlFor="mother_tongue" className="form-label">Mother Tongue</label>
          <input
            {...register("mother_tongue", onboardingRules.step2.mother_tongue)}
            {...getInputA11y("mother_tongue", errors)}
            id="mother_tongue"
            placeholder="e.g. Hindi, English, Tamil"
            className="form-input"
          />
          {errors.mother_tongue && <p id={errorIdFor("mother_tongue")} className="form-error" role="alert">{errors.mother_tongue.message}</p>}
        </div>

        <button
          type="submit"
          disabled={initializing}
          className="button button-primary cta-full"
        >
          Next: Education & Career
        </button>
        <p className="form-note">Your community details are only shared with compatible matches.</p>
      </form>
    </div>
  );
}

