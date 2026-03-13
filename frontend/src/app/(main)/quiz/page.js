"use client";

import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";

const QUIZ_QUESTIONS = [
  {
    id: "values",
    label: "Family values",
    description: "Which family environment feels closest to you?",
    options: ["Traditional", "Balanced", "Modern"],
  },
  {
    id: "lifestyle",
    label: "Lifestyle tempo",
    description: "How do you prefer to spend your week?",
    options: ["Home-centered", "Social & active", "Travel-focused"],
  },
  {
    id: "career",
    label: "Career priorities",
    description: "What best describes your career outlook?",
    options: ["Balanced growth", "Ambitious growth", "Flexible & supportive"],
  },
  {
    id: "relocation",
    label: "Relocation openness",
    description: "Are you open to relocating after marriage?",
    options: ["Same city", "Same country", "Open to relocate"],
  },
  {
    id: "communication",
    label: "Communication style",
    description: "How do you like to resolve differences?",
    options: ["Direct & clear", "Gentle & patient", "Thoughtful & reflective"],
  },
];

function normalizeAnswers(value) {
  if (!value || typeof value !== "object") return {};
  const source = value.answers && typeof value.answers === "object" ? value.answers : value;
  return Object.fromEntries(
    Object.entries(source).map(([key, val]) => [key, String(val || "").trim()])
  );
}

export default function QuizPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState(null);
  const [saving, setSaving] = useState(false);
  const [answers, setAnswers] = useState({});
  const [previewMode, setPreviewMode] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      try {
        if (!user) {
          if (!cancelled) {
            setPreviewMode(true);
            setProfile({ personalityQuiz: null });
          }
          return;
        }
        const response = await api.get("/users/profile");
        if (!cancelled) {
          setProfile(response?.data || {});
        }
      } catch {
        if (!cancelled) {
          setPreviewMode(true);
          setProfile({ personalityQuiz: null });
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

  useEffect(() => {
    if (!profile) return;
    const existing = normalizeAnswers(profile.personalityQuiz);
    if (Object.keys(existing).length > 0) {
      setAnswers(existing);
    }
  }, [profile]);

  const completedCount = useMemo(
    () => QUIZ_QUESTIONS.filter((question) => answers[question.id]).length,
    [answers]
  );

  const isComplete = completedCount === QUIZ_QUESTIONS.length;

  const handleSave = async () => {
    if (previewMode) {
      toast.info("Login to save your quiz.");
      return;
    }
    if (!isComplete) {
      toast.error("Please answer all questions before saving.");
      return;
    }

    setSaving(true);
    try {
      await api.put("/users/profile", {
        personalityQuiz: { answers },
      });
      toast.success("Compatibility quiz saved.");
    } catch {
      toast.error("Unable to save quiz right now.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <PageLoadingState
        title="Loading compatibility quiz..."
        description="Preparing your personalized questionnaire."
      />
    );
  }

  if (!profile && !previewMode) {
    return (
      <PageEmptyState
        title="Quiz unavailable"
        description="Please try again or return to your profile."
        primaryActionLabel="Back to Profile"
        primaryActionHref="/profile"
      />
    );
  }

  return (
    <div className="page-shell quiz-page">
      <div className="container-shell">
        <section className="panel quiz-shell">
          <div className="quiz-header">
            <div className="quiz-head-copy">
              <p className="section-label">Compatibility Quiz</p>
              <h1 className="section-title quiz-title">Tell us about your preferences</h1>
              <p className="section-copy quiz-copy">
                Answer five quick questions to personalize your matches. This stays private to you.
              </p>
            </div>
            <div className="quiz-progress" style={{ "--progress": `${(completedCount / QUIZ_QUESTIONS.length) * 100}%` }}>
              <span>{completedCount}/{QUIZ_QUESTIONS.length} answered</span>
              <div className="quiz-progress-track">
                <div className="quiz-progress-fill" />
              </div>
            </div>
          </div>

          <div className="quiz-grid">
            {QUIZ_QUESTIONS.map((question) => (
              <div key={question.id} className="quiz-card">
                <h3>{question.label}</h3>
                <p>{question.description}</p>
                <div className="quiz-options">
                  {question.options.map((option) => (
                    <label key={option} className={`quiz-option ${answers[question.id] === option ? "active" : ""}`}>
                      <input
                        type="radio"
                        name={question.id}
                        value={option}
                        checked={answers[question.id] === option}
                        onChange={() => setAnswers((prev) => ({ ...prev, [question.id]: option }))}
                      />
                      <span>{option}</span>
                    </label>
                  ))}
                </div>
              </div>
            ))}
          </div>

          <div className="quiz-footer">
            <button type="button" className="button button-secondary" onClick={() => setAnswers({})}>
              Reset
            </button>
            <button type="button" className="button button-primary" onClick={handleSave} disabled={saving || !isComplete}>
              {saving ? "Saving..." : "Save Quiz"}
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}
