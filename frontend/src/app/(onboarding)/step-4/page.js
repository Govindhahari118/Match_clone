"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import api from "../../../services/api";
import { arrayToCsv, csvToArray, fetchOnboardingContext, saveOnboardingProgress } from "../onboardingHelpers";
import { errorIdFor, getInputA11y, onboardingRules } from "../../../validation/rules";
import PhotoUpload from "../../../components/shared/PhotoUpload";

export default function Step4() {
  const router = useRouter();
  const [resumeDraft, setResumeDraft] = useState({});
  const [initializing, setInitializing] = useState(true);
  const [photoCount, setPhotoCount] = useState(0);
  const [photoError, setPhotoError] = useState("");
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
      father_occupation: "",
      mother_occupation: "",
      siblings_count: "",
      family_type: "",
      has_children: "",
      residential_status: "",
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
        father_occupation: stepDraft.father_occupation ?? profile.fatherOccupation ?? "",
        mother_occupation: stepDraft.mother_occupation ?? profile.motherOccupation ?? "",
        siblings_count: stepDraft.siblings_count ?? profile.siblingsCount ?? "",
        family_type: stepDraft.family_type ?? profile.familyType ?? "",
        has_children: stepDraft.has_children ?? profile.hasChildren ?? "",
        residential_status: stepDraft.residential_status ?? profile.residentialStatus ?? "",
      });
      setPhotoCount(Array.isArray(profile.photos) ? profile.photos.length : 0);

      setInitializing(false);
    };

    load();
    return () => {
      mounted = false;
    };
  }, [reset]);

  const onSubmit = async (data) => {
    try {
      if (photoCount === 0) {
        setPhotoError("Upload at least one photo to continue.");
        return;
      }
      await api.put("/users/profile", {
        foodHabit: data.food_habit,
        drinks: data.drinks,
        smokes: data.smokes,
        hobbies: csvToArray(data.hobbies),
        bio: data.bio,
        fatherOccupation: data.father_occupation,
        motherOccupation: data.mother_occupation,
        siblingsCount: data.siblings_count === "" ? null : Number(data.siblings_count),
        familyType: data.family_type,
        hasChildren: data.has_children || null,
        residentialStatus: data.residential_status || null,
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

  const handlePhotoUploaded = () => {
    setPhotoCount((count) => count + 1);
    setPhotoError("");
  };

  return (
    <div>
      <div className="onboarding-step-head">
        <p className="section-label">Step 4 - Lifestyle</p>
        <h2 className="section-title">Lifestyle & bio</h2>
        <p className="section-copy">
          Share the habits and interests that shape your daily life.
        </p>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="form-grid">
        <div className="form-grid-2">
          <div>
            <label htmlFor="father_occupation" className="form-label">Father&apos;s occupation</label>
            <input
              {...register("father_occupation")}
              {...getInputA11y("father_occupation", errors)}
              id="father_occupation"
              placeholder="e.g. Business, Govt. service"
              className="form-input"
            />
          </div>
          <div>
            <label htmlFor="mother_occupation" className="form-label">Mother&apos;s occupation</label>
            <input
              {...register("mother_occupation")}
              {...getInputA11y("mother_occupation", errors)}
              id="mother_occupation"
              placeholder="e.g. Homemaker, Teacher"
              className="form-input"
            />
          </div>
        </div>

        <div className="form-grid-2">
          <div>
            <label htmlFor="siblings_count" className="form-label">Number of siblings</label>
            <input
              {...register("siblings_count")}
              {...getInputA11y("siblings_count", errors)}
              id="siblings_count"
              type="number"
              min="0"
              max="20"
              className="form-input"
            />
          </div>
          <div>
            <label htmlFor="family_type" className="form-label">Family type</label>
            <select
              {...register("family_type")}
              {...getInputA11y("family_type", errors)}
              id="family_type"
              className="form-input"
            >
              <option value="">Select family type</option>
              <option value="nuclear">Nuclear</option>
              <option value="joint">Joint</option>
              <option value="other">Other</option>
            </select>
          </div>
        </div>

        <div className="form-grid-2">
          <div>
            <label htmlFor="has_children" className="form-label">Have children</label>
            <select
              {...register("has_children")}
              {...getInputA11y("has_children", errors)}
              id="has_children"
              className="form-input"
            >
              <option value="">Select option</option>
              <option value="no">No</option>
              <option value="yes_living_with_me">Yes, living with me</option>
              <option value="yes_not_living_with_me">Yes, not living with me</option>
              <option value="unknown">Prefer not to say</option>
            </select>
          </div>
          <div>
            <label htmlFor="residential_status" className="form-label">Residential status</label>
            <select
              {...register("residential_status")}
              {...getInputA11y("residential_status", errors)}
              id="residential_status"
              className="form-input"
            >
              <option value="">Select status</option>
              <option value="citizen">Citizen</option>
              <option value="permanent_resident">Permanent resident</option>
              <option value="work_permit">Work permit</option>
              <option value="student_visa">Student visa</option>
              <option value="not_specified">Not specified</option>
              <option value="other">Other</option>
            </select>
          </div>
        </div>

        <div>
          <label htmlFor="food_habit" className="form-label">Dietary Preference</label>
          <select
            {...register("food_habit", onboardingRules.step4.food_habit)}
            {...getInputA11y("food_habit", errors)}
            id="food_habit"
            className="form-input"
          >
            <option value="">Select Diet</option>
            <option value="vegetarian">Vegetarian</option>
            <option value="non_vegetarian">Non-Vegetarian</option>
            <option value="vegan">Vegan</option>
            <option value="eggetarian">Eggetarian</option>
          </select>
          {errors.food_habit && <p id={errorIdFor("food_habit")} className="form-error" role="alert">{errors.food_habit.message}</p>}
        </div>

        <div className="form-grid-2">
          <div>
            <label htmlFor="drinks" className="form-label">Drinking Habits</label>
            <select
              {...register("drinks")}
              {...getInputA11y("drinks", errors)}
              id="drinks"
              className="form-input"
            >
              <option value="no">No</option>
              <option value="occasionally">Occasionally</option>
              <option value="regularly">Regularly</option>
            </select>
          </div>
          <div>
            <label htmlFor="smokes" className="form-label">Smoking Habits</label>
            <select
              {...register("smokes")}
              {...getInputA11y("smokes", errors)}
              id="smokes"
              className="form-input"
            >
              <option value="no">No</option>
              <option value="occasionally">Occasionally</option>
              <option value="regularly">Regularly</option>
            </select>
          </div>
        </div>

        <div>
          <label htmlFor="hobbies" className="form-label">Hobbies</label>
          <input
            {...register("hobbies")}
            {...getInputA11y("hobbies", errors)}
            id="hobbies"
            placeholder="e.g. Reading, Traveling, Music (comma separated)"
            className="form-input"
          />
          <p className="form-note">Separate with commas</p>
        </div>

        <div>
          <label htmlFor="bio" className="form-label">About Me</label>
          <textarea
            {...register("bio", onboardingRules.step4.bio)}
            {...getInputA11y("bio", errors)}
            id="bio"
            rows="4"
            placeholder="Tell us about yourself..."
            className="form-input"
          ></textarea>
          {errors.bio && <p id={errorIdFor("bio")} className="form-error" role="alert">{errors.bio.message}</p>}
        </div>

        <div>
          <p className="form-label form-label-tight">Profile photos</p>
          <p className="form-note">Add at least one clear photo to complete onboarding.</p>
          <PhotoUpload onUploadComplete={handlePhotoUploaded} />
          <p className="form-note">Uploaded photos: {photoCount}</p>
          {photoError && <p className="form-error" role="alert">{photoError}</p>}
        </div>

        <button
          type="submit"
          disabled={initializing || photoCount === 0}
          className="button button-primary cta-full"
        >
          Continue to Preferences
        </button>
        <p className="form-note">You control what is shown publicly in your profile.</p>
      </form>
    </div>
  );
}

