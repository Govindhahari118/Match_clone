"use client";

import { useEffect, useMemo, useState } from "react";
import api from "../services/api";

const TRUST_CHECKS = [
  {
    key: "photo",
    label: "Add a clear profile photo",
    hint: "Profiles with a face photo feel more trustworthy.",
  },
  {
    key: "bio",
    label: "Write a meaningful bio",
    hint: "Share values, goals, and what you are looking for.",
  },
  {
    key: "basics",
    label: "Complete basics",
    hint: "Age, city, profession, and education help matches decide.",
  },
  {
    key: "family",
    label: "Add family details",
    hint: "Family context improves serious intent.",
  },
  {
    key: "interests",
    label: "Add interests",
    hint: "A few interests make conversations easier.",
  },
  {
    key: "quiz",
    label: "Finish compatibility quiz",
    hint: "Short quiz answers improve match relevance.",
  },
  {
    key: "attest",
    label: "Self-attestation",
    hint: "Confirm that your details are accurate.",
  },
];

function hasValue(value) {
  if (Array.isArray(value)) return value.length > 0;
  if (typeof value === "string") return value.trim().length > 0;
  if (value === null || value === undefined) return false;
  return Boolean(value);
}

export default function IdentityVerification() {
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState(null);
  const [attested, setAttested] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const loadProfile = async () => {
      try {
        const response = await api.get("/users/profile");
        if (!cancelled) {
          setProfile(response?.data || null);
        }
      } catch {
        if (!cancelled) {
          setProfile(null);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    if (typeof window !== "undefined") {
      setAttested(window.localStorage.getItem("trust_attested") === "1");
    }

    loadProfile();
    return () => {
      cancelled = true;
    };
  }, []);

  const primaryPhoto =
    profile?.photos?.find((photo) => photo.isPrimary)?.photoUrl ||
    profile?.photos?.[0]?.photoUrl ||
    "";

  const checklist = useMemo(() => {
    const quizDone = Boolean(
      profile?.personalityQuiz?.answers && Object.keys(profile.personalityQuiz.answers).length > 0
    );

    const mapping = {
      photo: Boolean(primaryPhoto),
      bio: hasValue(profile?.bio),
      basics: hasValue(profile?.dateOfBirth) && hasValue(profile?.city) && hasValue(profile?.profession),
      family:
        hasValue(profile?.familyType) ||
        hasValue(profile?.fatherOccupation) ||
        hasValue(profile?.motherOccupation),
      interests: Array.isArray(profile?.hobbies) && profile.hobbies.length >= 2,
      quiz: quizDone,
      attest: attested,
    };

    return TRUST_CHECKS.map((item) => ({ ...item, done: Boolean(mapping[item.key]) }));
  }, [attested, primaryPhoto, profile]);

  const completion = useMemo(() => {
    if (checklist.length === 0) return 0;
    const doneCount = checklist.filter((item) => item.done).length;
    return Math.round((doneCount / checklist.length) * 100);
  }, [checklist]);

  const onAttestChange = (event) => {
    const next = event.target.checked;
    setAttested(next);
    if (typeof window !== "undefined") {
      window.localStorage.setItem("trust_attested", next ? "1" : "0");
    }
  };

  if (loading) {
    return <div className="verification-loading">Loading trust checklist...</div>;
  }

  return (
    <div className="panel verification-card">
      <div className="verification-progress" style={{ "--progress": `${completion}%` }}>
        <div className="verification-progress-row">
          <span className="section-label">Checklist</span>
          <span className="form-note">{completion}% complete</span>
        </div>
        <div className="verification-progress-track">
          <div className="verification-progress-fill" />
        </div>
      </div>

      <div className="verify-grid">
        {checklist.map((item) => (
          <div key={item.key} className={`verify-item ${item.done ? "is-done" : ""}`}>
            <div>
              <strong>{item.label}</strong>
              <p>{item.hint}</p>
            </div>
            <span className="chip chip-support">{item.done ? "Done" : "Pending"}</span>
          </div>
        ))}
      </div>

      <label className="checkbox-row checkbox-row-spaced">
        <input type="checkbox" checked={attested} onChange={onAttestChange} />
        <span>I confirm the details on my profile are accurate.</span>
      </label>
    </div>
  );
}
