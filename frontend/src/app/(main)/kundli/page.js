"use client";

import { useState } from "react";
import Link from "next/link";

export default function KundliPage() {
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [formData, setFormData] = useState({
        mName: "", mDob: "", mTime: "", mPlace: "",
        fName: "", fDob: "", fTime: "", fPlace: ""
    });

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleMatch = (e) => {
        e.preventDefault();
        setLoading(true);

        // Simulate calculation
        setTimeout(() => {
            const score = Math.floor(Math.random() * 15) + 21; // 21-36
            setResult({
                score,
                total: 36,
                varna: { score: 1, total: 1, status: "Good" },
                vashya: { score: 1.5, total: 2, status: "Average" },
                tara: { score: 3, total: 3, status: "Excelente" },
                yoni: { score: 2, total: 4, status: "Average" },
                maitri: { score: 4, total: 5, status: "Good" },
                gana: { score: 6, total: 6, status: "Perfect" },
                bhakoot: { score: 7, total: 7, status: "Perfect" },
                nadi: { score: 0, total: 8, status: "Dosha Present" },
                manglik: Math.random() > 0.7,
                conclusion: score > 25 ? "Excellent Match! Proceed with confidence." : "Average Match. Consult an astrologer."
            });
            setLoading(false);
        }, 1500);
    };

    return (
        <div style={{ maxWidth: 800, margin: "0 auto" }}>
            <div style={{ textAlign: "center", marginBottom: "2rem" }}>
                <h1 style={{ fontSize: 28, fontWeight: 900, color: "#111827" }}>🔮 Free Kundli Matching</h1>
                <p style={{ color: "#64748b", marginTop: 8 }}>check compatibility (Gun Milan) using Vedic Astrology</p>
            </div>

            {!result ? (
                <form onSubmit={handleMatch} style={{ background: "white", padding: "2rem", borderRadius: 24, boxShadow: "0 4px 20px rgba(0,0,0,0.05)", border: "1px solid #f1f5f9" }}>
                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "2rem" }}>
                        {/* Boy's Details */}
                        <div>
                            <h3 style={{ fontSize: 16, fontWeight: 800, color: "#1e40af", marginBottom: "1rem", display: "flex", alignItems: "center", gap: 8 }}>
                                <span style={{ background: "#dbeafe", width: 24, height: 24, borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12 }}>♂</span>
                                Boy's Details
                            </h3>
                            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                                <input name="mName" placeholder="Name" required className="k-input" onChange={handleChange} />
                                <input name="mDob" type="date" required className="k-input" onChange={handleChange} />
                                <input name="mTime" type="time" required className="k-input" onChange={handleChange} />
                                <input name="mPlace" placeholder="Birth Place" required className="k-input" onChange={handleChange} />
                            </div>
                        </div>

                        {/* Girl's Details */}
                        <div>
                            <h3 style={{ fontSize: 16, fontWeight: 800, color: "#be123c", marginBottom: "1rem", display: "flex", alignItems: "center", gap: 8 }}>
                                <span style={{ background: "#fce7f3", width: 24, height: 24, borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12 }}>♀</span>
                                Girl's Details
                            </h3>
                            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                                <input name="fName" placeholder="Name" required className="k-input" onChange={handleChange} />
                                <input name="fDob" type="date" required className="k-input" onChange={handleChange} />
                                <input name="fTime" type="time" required className="k-input" onChange={handleChange} />
                                <input name="fPlace" placeholder="Birth Place" required className="k-input" onChange={handleChange} />
                            </div>
                        </div>
                    </div>

                    <div style={{ marginTop: "2rem", textAlign: "center" }}>
                        <button
                            type="submit"
                            disabled={loading}
                            style={{
                                padding: "14px 40px",
                                background: loading ? "#cbd5e1" : "linear-gradient(135deg, #e11d48, #c2185b)",
                                color: "white", border: "none", borderRadius: 99,
                                fontWeight: 800, fontSize: 16, cursor: loading ? "default" : "pointer",
                                boxShadow: "0 10px 30px rgba(225, 29, 72, 0.3)",
                                transform: loading ? "none" : "translateY(0)",
                                transition: "all 0.2s"
                            }}
                        >
                            {loading ? "Calculating..." : "Check Compatibility"}
                        </button>
                    </div>
                </form>
            ) : (
                <div style={{ background: "white", borderRadius: 24, overflow: "hidden", border: "1px solid #f1f5f9", boxShadow: "0 10px 40px rgba(0,0,0,0.08)" }}>
                    {/* Result Header */}
                    <div style={{ background: "linear-gradient(135deg, #4f46e5, #7c3aed)", padding: "2rem", color: "white", textAlign: "center" }}>
                        <p style={{ fontWeight: 700, opacity: 0.9, letterSpacing: 1 }}>MATCH SCORE</p>
                        <div style={{ fontSize: 64, fontWeight: 900, marginBottom: 8 }}>{result.score}<span style={{ fontSize: 32, opacity: 0.6 }}>/36</span></div>
                        <div style={{ display: "inline-block", background: "rgba(255,255,255,0.2)", padding: "6px 16px", borderRadius: 99, fontWeight: 700 }}>
                            {result.conclusion}
                        </div>
                    </div>

                    {/* Report Body */}
                    <div style={{ padding: "2rem" }}>
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "2rem", marginBottom: "2rem" }}>
                            <div style={{ background: result.manglik ? "#fff1f2" : "#f0fdf4", padding: "1rem", borderRadius: 16, border: `1px solid ${result.manglik ? "#fecdd3" : "#86efac"}` }}>
                                <h4 style={{ fontWeight: 800, color: result.manglik ? "#9f1239" : "#166534", marginBottom: 4 }}>Mangal Dosha</h4>
                                <p style={{ fontSize: 13, color: result.manglik ? "#be123c" : "#15803d" }}>
                                    {result.manglik ? "Mangal Dosha present in chart. Remedial measures recommended." : "No significant Mangal Dosha found. Compatible."}
                                </p>
                            </div>
                            <div style={{ background: "#f8fafc", padding: "1rem", borderRadius: 16, border: "1px solid #e2e8f0" }}>
                                <h4 style={{ fontWeight: 800, color: "#334155", marginBottom: 4 }}>Bhakoot & Nadi</h4>
                                <p style={{ fontSize: 13, color: "#64748b" }}>
                                    Nadi Score: {result.nadi.score}/8 • Bhakoot Score: {result.bhakoot.score}/7
                                </p>
                            </div>
                        </div>

                        <h3 style={{ fontWeight: 800, fontSize: 16, marginBottom: "1rem" }}>Guna Milan Details</h3>
                        <div style={{ border: "1px solid #e2e8f0", borderRadius: 16, overflow: "hidden" }}>
                            {[
                                { name: "Varna (Work)", ...result.varna },
                                { name: "Vashya (Dominance)", ...result.vashya },
                                { name: "Tara (Destiny)", ...result.tara },
                                { name: "Yoni (Mentality)", ...result.yoni },
                                { name: "Maitri (Friendship)", ...result.maitri },
                                { name: "Gana (Temperament)", ...result.gana },
                                { name: "Bhakoot (Love)", ...result.bhakoot },
                                { name: "Nadi (Health)", ...result.nadi },
                            ].map((row, i) => (
                                <div key={row.name} style={{ display: "flex", justifyContent: "space-between", padding: "12px 16px", background: i % 2 === 0 ? "white" : "#f8fafc", borderBottom: i === 7 ? "none" : "1px solid #f1f5f9" }}>
                                    <span style={{ fontWeight: 600, color: "#374151" }}>{row.name}</span>
                                    <div style={{ display: "flex", gap: 12 }}>
                                        <span style={{ fontSize: 13, color: "#64748b" }}>{row.status}</span>
                                        <span style={{ fontWeight: 800, color: "#111827", width: 40, textAlign: "right" }}>{row.score}/{row.total}</span>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <button onClick={() => { setResult(null); setLoading(false); }} style={{ marginTop: "2rem", width: "100%", padding: "14px", background: "#f1f5f9", color: "#334155", fontWeight: 700, border: "none", borderRadius: 12, cursor: "pointer" }}>
                            Check Another Match
                        </button>
                    </div>
                </div>
            )}

            <style>{`
                .k-input { width: 100%; padding: 12px; border-radius: 10px; border: 1.5px solid #e2e8f0; outline: none; font-size: 14px; transition: all 0.2s; }
                .k-input:focus { border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1); }
                @media (max-width: 640px) {
                    form > div { grid-template-columns: 1fr !important; }
                }
            `}</style>
        </div>
    );
}
