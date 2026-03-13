"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import PhotoUpload from "../../../components/shared/PhotoUpload";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";

const DEMO_PROFILE = {
  firstName: "Laksh",
  lastName: "Singh",
  dateOfBirth: "1998-04-15",
  gender: "male",
  maritalStatus: "never_married",
  role: "self",
  city: "Mumbai",
  state: "Maharashtra",
  country: "India",
  district: "Mumbai City",
  religion: "Hindu",
  caste: "Rajput",
  subCaste: "Kshatriya",
  motherTongue: "Hindi",
  profession: "Software Engineer",
  educationLevel: "bachelors",
  educationField: "Computer Science",
  company: "TechNova Labs",
  incomeBand: "10-25L",
  heightCm: 178,
  foodHabit: "vegetarian",
  drinks: "no",
  smokes: "no",
  religiousness: "moderate",
  birthPlace: "Mumbai",
  birthTime: "08:15",
  gothra: "Kashyap",
  zodiacSign: "Aries (Mesha)",
  nakshatra: "Ashwini",
  fatherOccupation: "Business",
  motherOccupation: "Homemaker",
  siblingsCount: 1,
  familyType: "nuclear",
  hasChildren: "no",
  residentialStatus: "citizen",
  personalityQuiz: { answers: { values: "Balanced" } },
  videoUrl: "",
  bio: "Engineer by profession, family-oriented by values. I enjoy travel, fitness, and meaningful conversations.",
  hobbies: ["Travel", "Fitness", "Music", "Reading"],
  photos: [{ id: "ph-1", photoUrl: "https://randomuser.me/api/portraits/men/32.jpg", isPrimary: true }],
};

const EDITABLE_FIELDS = [
  { key: "firstName", label: "First name", type: "text" },
  { key: "lastName", label: "Last name", type: "text" },
  { key: "dateOfBirth", label: "Date of birth", type: "date" },
  { key: "gender", label: "Gender", type: "text" },
  {
    key: "role",
    label: "Profile created for",
    type: "select",
    options: [
      { value: "self", label: "Self" },
      { value: "parent", label: "Parent" },
      { value: "relative", label: "Relative" },
    ],
  },
  { key: "maritalStatus", label: "Marital status", type: "text" },
  { key: "city", label: "City", type: "text" },
  { key: "district", label: "District", type: "text" },
  { key: "state", label: "State", type: "text" },
  { key: "country", label: "Country", type: "text" },
  { key: "religion", label: "Religion", type: "text" },
  { key: "caste", label: "Caste", type: "text" },
  { key: "subCaste", label: "Sub caste", type: "text" },
  { key: "motherTongue", label: "Mother tongue", type: "text" },
  { key: "profession", label: "Profession", type: "text" },
  { key: "educationLevel", label: "Education level", type: "text" },
  { key: "educationField", label: "Education field", type: "text" },
  { key: "company", label: "Company", type: "text" },
  { key: "incomeBand", label: "Income", type: "text" },
  { key: "heightCm", label: "Height (cm)", type: "number" },
  {
    key: "foodHabit",
    label: "Diet",
    type: "select",
    options: [
      { value: "vegetarian", label: "Vegetarian" },
      { value: "non_vegetarian", label: "Non-vegetarian" },
      { value: "vegan", label: "Vegan" },
      { value: "eggetarian", label: "Eggetarian" },
    ],
  },
  {
    key: "drinks",
    label: "Drinking",
    type: "select",
    options: [
      { value: "no", label: "No" },
      { value: "occasionally", label: "Occasionally" },
      { value: "regularly", label: "Regularly" },
    ],
  },
  {
    key: "smokes",
    label: "Smoking",
    type: "select",
    options: [
      { value: "no", label: "No" },
      { value: "occasionally", label: "Occasionally" },
      { value: "regularly", label: "Regularly" },
    ],
  },
  {
    key: "religiousness",
    label: "Family values",
    type: "select",
    options: [
      { value: "traditional", label: "Traditional" },
      { value: "moderate", label: "Moderate" },
      { value: "liberal", label: "Liberal" },
    ],
  },
  { key: "birthPlace", label: "Birth place", type: "text" },
  { key: "birthTime", label: "Birth time", type: "text" },
  { key: "gothra", label: "Gothra", type: "text" },
  { key: "zodiacSign", label: "Zodiac sign", type: "text" },
  { key: "nakshatra", label: "Nakshatra", type: "text" },
  { key: "fatherOccupation", label: "Father's occupation", type: "text" },
  { key: "motherOccupation", label: "Mother's occupation", type: "text" },
  { key: "siblingsCount", label: "Siblings", type: "number" },
  {
    key: "familyType",
    label: "Family type",
    type: "select",
    options: [
      { value: "nuclear", label: "Nuclear" },
      { value: "joint", label: "Joint" },
      { value: "other", label: "Other" },
    ],
  },
  {
    key: "hasChildren",
    label: "Have children",
    type: "select",
    options: [
      { value: "no", label: "No" },
      { value: "yes_living_with_me", label: "Yes, living with me" },
      { value: "yes_not_living_with_me", label: "Yes, not living with me" },
      { value: "unknown", label: "Prefer not to say" },
    ],
  },
  {
    key: "residentialStatus",
    label: "Residential status",
    type: "select",
    options: [
      { value: "citizen", label: "Citizen" },
      { value: "permanent_resident", label: "Permanent resident" },
      { value: "work_permit", label: "Work permit" },
      { value: "student_visa", label: "Student visa" },
      { value: "not_specified", label: "Not specified" },
      { value: "other", label: "Other" },
    ],
  },
  { key: "videoUrl", label: "Video introduction URL", type: "url" },
];

const VERIFY_CHECKLIST = [
  { key: "photo", label: "Profile photo", hint: "Add a clear primary photo." },
  { key: "bio", label: "Bio added", hint: "Share values and what you are looking for." },
  { key: "basics", label: "Basics complete", hint: "Age, city, profession, and education." },
  { key: "interests", label: "Interests added", hint: "Add at least two interests." },
  { key: "family", label: "Family details", hint: "Add family type or occupations." },
  { key: "quiz", label: "Compatibility quiz", hint: "Answer a few quick questions." },
];

function ensureStringArray(value) {
  if (Array.isArray(value)) {
    return value
      .filter((item) => typeof item === "string")
      .map((item) => item.trim())
      .filter(Boolean);
  }

  if (typeof value === "string") {
    return value
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);
  }

  return [];
}

function toDateInput(value) {
  if (!value) return "";

  if (typeof value === "string") {
    const trimmed = value.trim();
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed;
    const match = trimmed.match(/^(\d{4}-\d{2}-\d{2})T/);
    if (match) return match[1];
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function calculateAge(value) {
  const normalized = toDateInput(value);
  if (!normalized) return null;

  const [year, month, day] = normalized.split("-").map(Number);
  if (!year || !month || !day) return null;

  const now = new Date();
  let years = now.getFullYear() - year;
  const monthPassed = now.getMonth() + 1 > month;
  const sameMonthAndDayPassed = now.getMonth() + 1 === month && now.getDate() >= day;
  if (!monthPassed && !sameMonthAndDayPassed) {
    years -= 1;
  }

  return years > 0 ? years : null;
}

function hasValue(value) {
  if (Array.isArray(value)) return value.length > 0;
  if (typeof value === "number") return Number.isFinite(value);
  if (typeof value === "string") return value.trim().length > 0;
  return Boolean(value);
}

function formatDisplayValue(key, value) {
  if (!hasValue(value)) return "-";

  if (key === "dateOfBirth") {
    const normalized = toDateInput(value);
    if (!normalized) return "-";
    const [year, month, day] = normalized.split("-");
    return `${day}/${month}/${year}`;
  }

  if (key === "heightCm") {
    return `${value} cm`;
  }

  if (key === "siblingsCount") {
    return `${value}`;
  }

  if (key === "role") {
    const label = String(value).replaceAll("_", " ");
    return label.charAt(0).toUpperCase() + label.slice(1);
  }

  if (key === "hasChildren" || key === "residentialStatus") {
    const label = String(value).replaceAll("_", " ");
    return label.replace(/\b\w/g, (char) => char.toUpperCase());
  }

  return String(value).replaceAll("_", " ");
}

function normalizeProfile(payload) {
  if (!payload) return null;

  const photos = Array.isArray(payload.photos)
    ? payload.photos
        .filter(Boolean)
        .map((photo) => ({
          ...photo,
          photoUrl: photo?.photoUrl || photo?.url || photo?.thumbnailUrl || "",
        }))
    : [];

  return {
    ...payload,
    dateOfBirth: toDateInput(payload.dateOfBirth),
    photos,
    hobbies: ensureStringArray(payload.hobbies),
  };
}

function buildFormState(profile) {
  const next = {};

  EDITABLE_FIELDS.forEach((field) => {
    if (field.key === "dateOfBirth") {
      next[field.key] = toDateInput(profile?.[field.key]);
      return;
    }

    next[field.key] = profile?.[field.key] ?? "";
  });

  next.bio = profile?.bio ?? "";
  next.hobbies = ensureStringArray(profile?.hobbies);
  return next;
}

function buildSavePayload(formData) {
  const payload = {};

  EDITABLE_FIELDS.forEach((field) => {
    const value = formData?.[field.key];

    if (field.key === "dateOfBirth") {
      payload[field.key] = toDateInput(value) || null;
      return;
    }

    if (field.type === "number") {
      if (value === "" || value === null || value === undefined) {
        payload[field.key] = null;
        return;
      }
      const parsed = Number(value);
      payload[field.key] = Number.isFinite(parsed) ? parsed : null;
      return;
    }

    payload[field.key] = typeof value === "string" ? value.trim() : value ?? "";
  });

  payload.bio = typeof formData?.bio === "string" ? formData.bio.trim() : "";
  payload.hobbies = ensureStringArray(formData?.hobbies);

  return payload;
}

export default function ProfilePage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState(null);
  const [formData, setFormData] = useState({});
  const [previewMode, setPreviewMode] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const loadProfile = async () => {
      setLoading(true);
      setPreviewMode(false);

      try {
        if (!user) {
          const demo = normalizeProfile(DEMO_PROFILE);
          if (!cancelled) {
            setProfile(demo);
            setFormData(buildFormState(demo));
            setPreviewMode(true);
          }
          return;
        }

        const response = await api.get("/users/profile");
        const normalized = normalizeProfile(response?.data) || normalizeProfile(DEMO_PROFILE);
        if (!cancelled) {
          setProfile(normalized);
          setFormData(buildFormState(normalized));
        }
      } catch {
        const fallback = normalizeProfile(DEMO_PROFILE);
        if (!cancelled) {
          setProfile(fallback);
          setFormData(buildFormState(fallback));
          setPreviewMode(true);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadProfile();

    return () => {
      cancelled = true;
    };
  }, [user]);

  const age = useMemo(() => calculateAge(profile?.dateOfBirth), [profile?.dateOfBirth]);

  const completion = useMemo(() => {
    if (!profile) return 0;
    const backendScore =
      profile?.onboarding?.completeness?.score ??
      profile?.completionPercentage;
    if (Number.isFinite(backendScore)) {
      return Math.round(backendScore);
    }

    const checks = [
      profile.firstName,
      profile.dateOfBirth,
      profile.gender,
      profile.maritalStatus,
      profile.city,
      profile.profession,
      profile.educationLevel,
      profile.bio,
      profile.heightCm,
      profile.photos,
      profile.familyType,
      profile.fatherOccupation,
    ];

    const filled = checks.filter(hasValue).length;
    return Math.round((filled / checks.length) * 100);
  }, [profile]);

  const primaryPhoto =
    profile?.photos?.find((photo) => photo.isPrimary)?.photoUrl ||
    profile?.photos?.[0]?.photoUrl ||
    "";

  const verificationChecklist = useMemo(() => {
    return VERIFY_CHECKLIST.map((item) => {
      if (item.key === "photo") return { ...item, done: Boolean(primaryPhoto) };
      if (item.key === "bio") return { ...item, done: Boolean(profile?.bio?.trim()) };
      if (item.key === "basics") {
        const basicsDone = Boolean(
          profile?.dateOfBirth && profile?.city && profile?.profession && profile?.educationLevel
        );
        return { ...item, done: basicsDone };
      }
      if (item.key === "interests") {
        const interestsDone = Array.isArray(profile?.hobbies) && profile.hobbies.length >= 2;
        return { ...item, done: interestsDone };
      }
      if (item.key === "family") {
        const familyDone = Boolean(profile?.familyType || profile?.fatherOccupation || profile?.motherOccupation);
        return { ...item, done: familyDone };
      }
      if (item.key === "quiz") {
        const quizDone = Boolean(profile?.personalityQuiz?.answers && Object.keys(profile.personalityQuiz.answers).length > 0);
        return { ...item, done: quizDone };
      }
      return { ...item, done: false };
    });
  }, [profile?.bio, profile?.city, profile?.dateOfBirth, profile?.educationLevel, profile?.fatherOccupation, profile?.familyType, profile?.hobbies, profile?.motherOccupation, profile?.personalityQuiz, profile?.profession, primaryPhoto]);

  const quizCompleted = useMemo(() => {
    const answers = profile?.personalityQuiz?.answers;
    return Boolean(answers && Object.keys(answers).length > 0);
  }, [profile?.personalityQuiz]);

  const profileSignals = useMemo(
    () => [
      { key: "completion", label: "Profile Score", value: `${completion}%` },
      { key: "age", label: "Age", value: age ? `${age} yrs` : "Not set" },
      { key: "photos", label: "Photos", value: `${profile?.photos?.length || 0}` },
      { key: "hobbies", label: "Interests", value: `${profile?.hobbies?.length || 0}` },
      { key: "quiz", label: "Quiz", value: quizCompleted ? "Completed" : "Pending" },
    ],
    [completion, age, profile, quizCompleted]
  );

  const fullName = useMemo(() => {
    const composed = [profile?.firstName, profile?.lastName]
      .filter((part) => typeof part === "string" && part.trim().length > 0)
      .join(" ")
      .trim();

    return composed || "Member Profile";
  }, [profile?.firstName, profile?.lastName]);

  const profileSummary = useMemo(() => {
    const parts = [
      age ? `${age} yrs` : null,
      profile?.profession || "Professional",
      profile?.city || null,
    ].filter(Boolean);

    return parts.join(" | ");
  }, [age, profile?.profession, profile?.city]);

  const onFieldChange = (key, value) => {
    setFormData((previous) => ({ ...previous, [key]: value }));
  };

  const onStartEdit = () => {
    setFormData(buildFormState(profile));
    setEditing(true);
  };

  const onCancelEdit = () => {
    setFormData(buildFormState(profile));
    setEditing(false);
  };

  const onSave = async () => {
    if (previewMode) {
      toast.info("Login to save profile changes.");
      return;
    }

    setSaving(true);

    try {
      const payload = buildSavePayload(formData);
      await api.put("/users/profile", payload);

      const merged = normalizeProfile({ ...profile, ...payload });
      setProfile(merged);
      setFormData(buildFormState(merged));
      setEditing(false);
      toast.success("Profile updated successfully.");
    } catch {
      toast.error("Unable to update profile right now. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  const handlePhotoUploaded = (photo) => {
    if (previewMode) {
      toast.info("Login to upload photos.");
      return;
    }
    if (!photo) return;
    setProfile((previous) => {
      if (!previous) return previous;
      const nextPhotos = Array.isArray(previous.photos) ? [...previous.photos, photo] : [photo];
      return { ...previous, photos: nextPhotos };
    });
    toast.success("Photo uploaded.");
  };

  if (loading) {
    return (
      <PageLoadingState
        title="Loading profile..."
        description="Preparing your profile dashboard and completion insights."
      />
    );
  }

  if (!profile) {
    return (
      <PageEmptyState
        title="Unable to load profile"
        description="Try reloading this page or return to matches while we reconnect."
        primaryActionLabel="Go to Matches"
        primaryActionHref="/matches"
      />
    );
  }

  return (
    <div className="profile-shell">
      <section className="panel profile-hero">
        <div className="profile-hero-banner" />

        <div className="profile-hero-body">
          <div className="profile-hero-head">
            <div className="profile-avatar">
              {primaryPhoto ? (
                <Image
                  src={primaryPhoto}
                  alt="Profile photo"
                  width={200}
                  height={200}
                  sizes="84px"
                  className="profile-avatar-img"
                />
              ) : (
                <div className="profile-avatar-letter">
                  {(profile.firstName || "U").charAt(0).toUpperCase()}
                </div>
              )}
            </div>

            <div className="profile-hero-meta">
              <h1 className="profile-hero-name">{fullName}</h1>
              <p className="profile-hero-summary">{profileSummary}</p>
              {previewMode && <span className="chip chip-brand profile-preview-chip">Preview Mode</span>}
            </div>

            <div className="profile-action-row">
              {editing ? (
                <>
                  <button type="button" className="button button-secondary" onClick={onCancelEdit} disabled={saving}>
                    Cancel
                  </button>
                  <button type="button" className="button button-primary" onClick={onSave} disabled={saving}>
                    {saving ? "Saving..." : "Save"}
                  </button>
                </>
              ) : (
                <button type="button" className="button button-primary" onClick={onStartEdit}>
                  Edit Profile
                </button>
              )}
            </div>
          </div>

          <div className="profile-completion">
            <div className="profile-progress-track" style={{ "--progress": `${completion}%` }}>
              <div className="profile-progress-fill" />
            </div>
            <span className="profile-progress-text">{completion}% complete</span>
          </div>
          <p className="form-note profile-progress-note">
            Complete every section to improve match quality and visibility.
          </p>

          <div className="profile-signals">
            {profileSignals.map((signal) => (
              <div key={signal.key} className="profile-signal-tile">
                <p className="signal-value">{signal.value}</p>
                <p className="signal-label">{signal.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <div className="profile-main-grid">
        <section className="panel profile-block">
          <p className="section-label">About</p>
          <h2 className="profile-section-title">Personal Bio</h2>

          {editing ? (
            <textarea
              value={formData.bio || ""}
              onChange={(event) => onFieldChange("bio", event.target.value)}
              rows={5}
              className="form-input profile-bio-input"
            />
          ) : (
            <p className="profile-bio-text">{profile.bio || "No bio added yet."}</p>
          )}

          <div className="stack-sm profile-section-stack">
            <p className="section-label">Interests</p>
            <div className="profile-interests">
              {(profile.hobbies || []).length > 0 ? (
                profile.hobbies.map((item) => (
                  <span key={item} className="chip chip-support">
                    {item}
                  </span>
                ))
              ) : (
                <span className="profile-interests-empty">No interests added yet.</span>
              )}
            </div>
          </div>
        </section>

        <section className="panel profile-block">
          <p className="section-label">Details</p>
          <h2 className="profile-section-title">Profile Information</h2>

          <div className="profile-details-grid">
            {EDITABLE_FIELDS.map((field) => (
              <div key={field.key}>
                <label className="form-label">{field.label}</label>
                {editing ? (
                  field.type === "select" ? (
                    <select
                      className="form-input"
                      value={formData[field.key] ?? ""}
                      onChange={(event) => onFieldChange(field.key, event.target.value)}
                    >
                      <option value="">Select</option>
                      {field.options?.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      className="form-input"
                      type={field.type}
                      value={formData[field.key] ?? ""}
                      onChange={(event) => onFieldChange(field.key, event.target.value)}
                    />
                  )
                ) : (
                  <div className="field-tile">
                    {formatDisplayValue(field.key, profile[field.key])}
                  </div>
                )}
              </div>
            ))}
          </div>

          <div className="profile-actions-row">
            <Link href="/settings" className="button button-secondary">
              Privacy Settings
            </Link>
            <Link href="/pricing" className="button button-primary">
              Upgrade Profile
            </Link>
          </div>
        </section>

        <section className="panel profile-block profile-block-full">
          <div className="profile-media-head">
            <div>
              <p className="section-label">Media</p>
              <h2 className="profile-section-title-sm">Photos & video introduction</h2>
            </div>
            <Link href="/biodata" className="button button-secondary">
              Download biodata
            </Link>
          </div>

          <div className="profile-media-grid">
            <div className="profile-media-panel">
              <h3>Photo gallery</h3>
              <p className="form-note">Add at least one clear photo to complete your profile.</p>
              <PhotoUpload onUploadComplete={handlePhotoUploaded} disabled={previewMode} />
              <div className="photo-grid">
                {(profile?.photos || []).length > 0 ? (
                  profile.photos.map((photo) => (
                    <div key={photo.id} className="photo-thumb">
                      <Image
                        src={photo.photoUrl}
                        alt="Profile photo"
                        width={120}
                        height={160}
                        sizes="120px"
                        className="photo-thumb-img"
                      />
                    </div>
                  ))
                ) : (
                  <div className="photo-empty">No photos uploaded yet.</div>
                )}
              </div>
            </div>

            <div className="profile-media-panel">
              <h3>Video introduction</h3>
              <p className="form-note">Share a short video link that highlights your values and expectations.</p>
              {editing ? (
                <input
                  className="form-input"
                  type="url"
                  value={formData.videoUrl ?? ""}
                  onChange={(event) => onFieldChange("videoUrl", event.target.value)}
                  placeholder="https://"
                />
              ) : profile?.videoUrl ? (
                <a className="button button-secondary" href={profile.videoUrl} target="_blank" rel="noreferrer">
                  View video introduction
                </a>
              ) : (
                <div className="field-tile">No video added yet.</div>
              )}
            </div>
          </div>
        </section>

        <section className="panel profile-block profile-block-full">
          <div className="profile-verify-head">
            <div>
              <p className="section-label">Trust checklist</p>
              <h2 className="profile-section-title-sm">Increase profile trust</h2>
            </div>
            <div className="profile-verify-actions">
              <Link href="/verification" className="button button-secondary">
                Review trust checklist
              </Link>
              <Link href="/quiz" className="button button-primary">
                Take quiz
              </Link>
            </div>
          </div>
          <div className="verify-grid">
            {verificationChecklist.map((item) => (
              <div key={item.key} className={`verify-item ${item.done ? "is-done" : ""}`}>
                <div>
                  <strong>{item.label}</strong>
                  <p>{item.hint}</p>
                </div>
                <span className="chip chip-support">{item.done ? "Done" : "Pending"}</span>
              </div>
            ))}
          </div>
        </section>
      </div>

    </div>
  );
}
