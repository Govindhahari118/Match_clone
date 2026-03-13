"use client";

import { useEffect, useMemo, useState } from "react";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";

const DEMO_PROFILE = {
  firstName: "Laksh",
  lastName: "Singh",
  dateOfBirth: "1998-04-15",
  gender: "male",
  maritalStatus: "never_married",
  city: "Mumbai",
  state: "Maharashtra",
  country: "India",
  religion: "Hindu",
  caste: "Rajput",
  subCaste: "Kshatriya",
  motherTongue: "Hindi",
  educationLevel: "bachelors",
  educationField: "Computer Science",
  profession: "Software Engineer",
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
  hobbies: ["Travel", "Fitness", "Music", "Reading"],
};

function toDateInput(value) {
  if (!value) return "";
  if (typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
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
  if (!monthPassed && !sameMonthAndDayPassed) years -= 1;
  return years > 0 ? years : null;
}

function formatValue(value) {
  if (value === null || value === undefined) return "-";
  if (Array.isArray(value)) return value.length ? value.join(", ") : "-";
  if (typeof value === "string" && value.trim().length === 0) return "-";
  return String(value).replaceAll("_", " ");
}

export default function BiodataPage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [previewMode, setPreviewMode] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      try {
        if (!user) {
          if (!cancelled) {
            setProfile(DEMO_PROFILE);
            setPreviewMode(true);
          }
          return;
        }
        const response = await api.get("/users/profile");
        if (!cancelled) {
          setProfile(response?.data || DEMO_PROFILE);
        }
      } catch {
        if (!cancelled) {
          setProfile(DEMO_PROFILE);
          setPreviewMode(true);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    load();
    return () => {
      cancelled = true;
    };
  }, [user]);

  const fullName = useMemo(() => {
    const composed = [profile?.firstName, profile?.lastName]
      .filter((part) => typeof part === "string" && part.trim().length > 0)
      .join(" ")
      .trim();
    return composed || "Member";
  }, [profile?.firstName, profile?.lastName]);

  const age = useMemo(() => calculateAge(profile?.dateOfBirth), [profile?.dateOfBirth]);

  if (loading) {
    return (
      <PageLoadingState
        title="Preparing biodata..."
        description="Collecting profile details and formatting your printable biodata."
      />
    );
  }

  if (!profile) {
    return (
      <PageEmptyState
        title="Biodata unavailable"
        description="We could not load your biodata right now. Please try again."
        primaryActionLabel="Back to Profile"
        primaryActionHref="/profile"
      />
    );
  }

  return (
    <div className="page-shell biodata-page">
      <div className="container-shell">
        <section className="panel biodata-shell">
          <div className="biodata-header no-print">
            <div className="biodata-head">
              <p className="section-label">Biodata</p>
              <h1 className="section-title biodata-title">
                {fullName}
              </h1>
              <p className="section-copy biodata-copy">
                Printable profile snapshot for families and offline sharing.
              </p>
              {previewMode && <span className="chip chip-brand biodata-preview-chip">Preview</span>}
            </div>
            <button type="button" className="button button-primary" onClick={() => window.print()}>
              Print / Save PDF
            </button>
          </div>

          <div className="biodata-card">
            <div className="biodata-section">
              <h2>Personal Details</h2>
              <div className="biodata-grid">
                <div><span>Name</span><strong>{fullName}</strong></div>
                <div><span>Age</span><strong>{age ? `${age} years` : "-"}</strong></div>
                <div><span>Gender</span><strong>{formatValue(profile.gender)}</strong></div>
                <div><span>Marital Status</span><strong>{formatValue(profile.maritalStatus)}</strong></div>
                <div><span>Height</span><strong>{profile.heightCm ? `${profile.heightCm} cm` : "-"}</strong></div>
                <div><span>Mother Tongue</span><strong>{formatValue(profile.motherTongue)}</strong></div>
                <div><span>Religion</span><strong>{formatValue(profile.religion)}</strong></div>
                <div><span>Caste / Sub-caste</span><strong>{formatValue([profile.caste, profile.subCaste].filter(Boolean))}</strong></div>
              </div>
            </div>

            <div className="biodata-section">
              <h2>Location</h2>
              <div className="biodata-grid">
                <div><span>City</span><strong>{formatValue(profile.city)}</strong></div>
                <div><span>State</span><strong>{formatValue(profile.state)}</strong></div>
                <div><span>Country</span><strong>{formatValue(profile.country)}</strong></div>
              </div>
            </div>

            <div className="biodata-section">
              <h2>Education & Career</h2>
              <div className="biodata-grid">
                <div><span>Education Level</span><strong>{formatValue(profile.educationLevel)}</strong></div>
                <div><span>Education Field</span><strong>{formatValue(profile.educationField)}</strong></div>
                <div><span>Profession</span><strong>{formatValue(profile.profession)}</strong></div>
                <div><span>Company</span><strong>{formatValue(profile.company)}</strong></div>
                <div><span>Income</span><strong>{formatValue(profile.incomeBand)}</strong></div>
              </div>
            </div>

            <div className="biodata-section">
              <h2>Family Details</h2>
              <div className="biodata-grid">
                <div><span>Father&apos;s Occupation</span><strong>{formatValue(profile.fatherOccupation)}</strong></div>
                <div><span>Mother&apos;s Occupation</span><strong>{formatValue(profile.motherOccupation)}</strong></div>
                <div><span>Siblings</span><strong>{formatValue(profile.siblingsCount)}</strong></div>
                <div><span>Family Type</span><strong>{formatValue(profile.familyType)}</strong></div>
                <div><span>Family Values</span><strong>{formatValue(profile.religiousness)}</strong></div>
              </div>
            </div>

            <div className="biodata-section">
              <h2>Lifestyle</h2>
              <div className="biodata-grid">
                <div><span>Diet</span><strong>{formatValue(profile.foodHabit)}</strong></div>
                <div><span>Drinking</span><strong>{formatValue(profile.drinks)}</strong></div>
                <div><span>Smoking</span><strong>{formatValue(profile.smokes)}</strong></div>
                <div><span>Family Values</span><strong>{formatValue(profile.religiousness)}</strong></div>
                <div><span>Hobbies</span><strong>{formatValue(profile.hobbies)}</strong></div>
              </div>
            </div>

            <div className="biodata-section">
              <h2>Horoscope</h2>
              <div className="biodata-grid">
                <div><span>Birth Place</span><strong>{formatValue(profile.birthPlace)}</strong></div>
                <div><span>Birth Time</span><strong>{formatValue(profile.birthTime)}</strong></div>
                <div><span>Gothra</span><strong>{formatValue(profile.gothra)}</strong></div>
                <div><span>Zodiac</span><strong>{formatValue(profile.zodiacSign)}</strong></div>
                <div><span>Nakshatra</span><strong>{formatValue(profile.nakshatra)}</strong></div>
              </div>
            </div>

            <div className="biodata-footer">
              <p>Contact details are shared only with verified mutual matches.</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
