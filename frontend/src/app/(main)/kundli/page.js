"use client";

import { useState } from "react";
import Link from "next/link";

export default function KundliPage() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [formData, setFormData] = useState({
    aName: "",
    aDob: "",
    aTime: "",
    aPlace: "",
    bName: "",
    bDob: "",
    bTime: "",
    bPlace: "",
  });

  const handleChange = (event) => {
    setFormData({ ...formData, [event.target.name]: event.target.value });
  };

  const handleMatch = (event) => {
    event.preventDefault();
    setLoading(true);

    setTimeout(() => {
      const score = Math.floor(Math.random() * 15) + 21;
      setResult({
        score,
        total: 36,
        varna: { score: 1, total: 1, status: "Good" },
        vashya: { score: 1.5, total: 2, status: "Average" },
        tara: { score: 3, total: 3, status: "Excellent" },
        yoni: { score: 2, total: 4, status: "Average" },
        maitri: { score: 4, total: 5, status: "Good" },
        gana: { score: 6, total: 6, status: "Perfect" },
        bhakoot: { score: 7, total: 7, status: "Perfect" },
        nadi: { score: 0, total: 8, status: "Dosha Present" },
        manglik: Math.random() > 0.7,
        conclusion:
          score > 25
            ? "Excellent match. Proceed with confidence."
            : "Average match. Consider expert guidance.",
      });
      setLoading(false);
    }, 1200);
  };

  return (
    <div className="kundli-shell">
      <section className="page-hero">
        <div>
          <p className="section-label">Astro Match</p>
          <h1 className="page-hero-title">Kundli matching</h1>
          <p className="page-hero-copy">
            Run a quick Gun Milan check to understand compatibility. This tool is a demo and does not replace
            expert advice.
          </p>
        </div>
        <div className="page-hero-actions">
          <Link href="/matches" className="button button-primary">Browse Matches</Link>
          <Link href="/help" className="button button-secondary">How it works</Link>
        </div>
      </section>

      {!result ? (
        <section className="panel kundli-panel">
          <form onSubmit={handleMatch} className="form-grid">
            <div className="form-grid-2">
              <div>
                <p className="section-label kundli-subtitle">Partner A</p>
                <div className="form-grid">
                  <input name="aName" placeholder="Name" required className="form-input" onChange={handleChange} />
                  <input name="aDob" type="date" required className="form-input" onChange={handleChange} />
                  <input name="aTime" type="time" required className="form-input" onChange={handleChange} />
                  <input name="aPlace" placeholder="Birth place" required className="form-input" onChange={handleChange} />
                </div>
              </div>
              <div>
                <p className="section-label kundli-subtitle">Partner B</p>
                <div className="form-grid">
                  <input name="bName" placeholder="Name" required className="form-input" onChange={handleChange} />
                  <input name="bDob" type="date" required className="form-input" onChange={handleChange} />
                  <input name="bTime" type="time" required className="form-input" onChange={handleChange} />
                  <input name="bPlace" placeholder="Birth place" required className="form-input" onChange={handleChange} />
                </div>
              </div>
            </div>

            <button type="submit" disabled={loading} className="button button-primary cta-full">
              {loading ? "Calculating..." : "Check compatibility"}
            </button>
            <p className="form-note">Scores are indicative for demo purposes.</p>
          </form>
        </section>
      ) : (
        <section className="panel kundli-result">
          <div className="kundli-score-card">
            <p className="kundli-score-label">Match Score</p>
            <div className="kundli-score">
              {result.score}
              <span> / {result.total}</span>
            </div>
            <p className="kundli-score-note">{result.conclusion}</p>
          </div>

          <div className="kundli-result-body">
            <div
              className="panel kundli-manglik"
              style={{ "--tone": result.manglik ? "#fecdd3" : "#bbf7d0" }}
            >
              <strong>Mangal Dosha</strong>
              <p className="kundli-manglik-copy">
                {result.manglik
                  ? "Mangal Dosha present in chart. Remedial measures recommended."
                  : "No significant Mangal Dosha found. Compatible."}
              </p>
            </div>

            <div className="panel kundli-breakdown">
              <strong>Guna Milan details</strong>
              <div className="kundli-breakdown-grid">
                {[
                  { name: "Varna (Work)", ...result.varna },
                  { name: "Vashya (Dominance)", ...result.vashya },
                  { name: "Tara (Destiny)", ...result.tara },
                  { name: "Yoni (Mentality)", ...result.yoni },
                  { name: "Maitri (Friendship)", ...result.maitri },
                  { name: "Gana (Temperament)", ...result.gana },
                  { name: "Bhakoot (Love)", ...result.bhakoot },
                  { name: "Nadi (Health)", ...result.nadi },
                ].map((row) => (
                  <div key={row.name} className="kundli-row">
                    <span>{row.name}</span>
                    <span className="kundli-row-status">{row.status}</span>
                    <strong>{row.score}/{row.total}</strong>
                  </div>
                ))}
              </div>
            </div>

            <button
              type="button"
              onClick={() => { setResult(null); setLoading(false); }}
              className="button button-secondary"
            >
              Check another match
            </button>
          </div>
        </section>
      )}
    </div>
  );
}
