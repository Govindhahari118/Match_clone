"use client";

import api from "../../services/api";

export function toDateInput(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toISOString().slice(0, 10);
}

export function csvToArray(value) {
  if (!value) return [];
  return String(value)
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

export function arrayToCsv(value) {
  if (!Array.isArray(value)) return "";
  return value.join(", ");
}

export async function fetchOnboardingContext() {
  try {
    const response = await api.get("/users/profile");
    const profile = response?.data || {};
    const resumeState = profile?.onboarding?.resumeState || {
      step: 1,
      route: null,
      draft: {},
    };
    return { profile, resumeState };
  } catch {
    return {
      profile: {},
      resumeState: { step: 1, route: null, draft: {} },
    };
  }
}

export async function saveOnboardingProgress({ nextStep, route, resumeDraft, stepKey, stepValues }) {
  const mergedDraft = {
    ...(resumeDraft && typeof resumeDraft === "object" ? resumeDraft : {}),
    [stepKey]: stepValues,
  };

  await api.post("/users/onboarding/progress", {
    step: nextStep,
    route,
    draft: mergedDraft,
  });

  return mergedDraft;
}
