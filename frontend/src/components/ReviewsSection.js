"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "react-toastify";
import api from "../services/api";
import { useAuth } from "../context/AuthContext";
import SafeProfileImage from "./SafeProfileImage";

export default function ReviewsSection({ userId, userName }) {
  const { user } = useAuth();
  const [reviews, setReviews] = useState([]);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const isSelf = user?.id === userId;

  const fetchReviews = useCallback(async () => {
    if (!userId) {
      setReviews([]);
      setState("content");
      return;
    }
    setState("loading");
    setError("");
    try {
      const response = await api.get(`/reviews/user/${userId}`);
      setReviews(Array.isArray(response.data) ? response.data : []);
      setState("content");
    } catch (err) {
      setReviews([]);
      setError(err.response?.data?.error || "Couldn’t load reviews.");
      setState("error");
    }
  }, [userId]);

  useEffect(() => {
    fetchReviews();
  }, [fetchReviews]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!comment.trim() || submitting) return;
    setSubmitting(true);
    try {
      await api.post("/reviews", { reviewedUserId: userId, rating, comment: comment.trim() });
      toast.success("Review submitted.");
      setComment("");
      await fetchReviews();
    } catch (err) {
      toast.error(err.response?.data?.error || "Failed to submit review.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="panel" style={{ padding: 24, marginTop: 24 }}>
      <h3 style={{ fontSize: 20, fontWeight: 800, marginTop: 0 }}>Reviews & Ratings</h3>

      {state === "loading" && <p style={{ color: "var(--ink-muted)" }}>Loading reviews…</p>}

      {state === "error" && (
        <div role="alert">
          <p style={{ color: "var(--ink-muted)" }}>{error}</p>
          <button type="button" className="button button-secondary" onClick={fetchReviews}>Retry</button>
        </div>
      )}

      {state === "content" && reviews.length === 0 && (
        <p style={{ color: "var(--ink-muted)" }}>No reviews have been recorded for this profile.</p>
      )}

      {state === "content" && reviews.length > 0 && (
        <div style={{ display: "grid", gap: 16, marginBottom: 24 }}>
          {reviews.map((review) => (
            <article key={review.id} style={{ borderBottom: "1px solid #eef2f7", paddingBottom: 16 }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <SafeProfileImage
                    src={review.reviewerPhoto}
                    alt={review.reviewerName ? `${review.reviewerName} profile` : "Reviewer photo"}
                    width={28}
                    height={28}
                    sizes="28px"
                    style={{ width: 28, height: 28, borderRadius: "50%", objectFit: "cover" }}
                  />
                  <strong style={{ fontSize: 14 }}>{review.reviewerName || "Member"}</strong>
                  <span aria-label={`${review.rating} out of 5 stars`} style={{ fontSize: 12 }}>
                    {"★".repeat(Math.max(0, Math.min(5, Number(review.rating) || 0)))}
                  </span>
                </div>
                {review.createdAt && (
                  <time style={{ fontSize: 12, color: "var(--ink-muted)" }}>
                    {new Date(review.createdAt).toLocaleDateString()}
                  </time>
                )}
              </div>
              {review.comment && <p style={{ marginBottom: 0, color: "var(--ink-muted)", lineHeight: 1.6 }}>{review.comment}</p>}
            </article>
          ))}
        </div>
      )}

      {user && !isSelf && (
        <form onSubmit={handleSubmit} style={{ background: "#f8fafc", padding: 20, borderRadius: 16 }}>
          <h4 style={{ marginTop: 0 }}>Write a Review for {userName || "this member"}</h4>
          <label className="form-label" htmlFor={`review-rating-${userId}`}>Rating</label>
          <select
            id={`review-rating-${userId}`}
            className="form-input"
            value={rating}
            onChange={(event) => setRating(Number(event.target.value))}
          >
            {[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} / 5</option>)}
          </select>
          <label className="form-label" htmlFor={`review-comment-${userId}`} style={{ marginTop: 12 }}>Comment</label>
          <textarea
            id={`review-comment-${userId}`}
            className="form-input"
            value={comment}
            onChange={(event) => setComment(event.target.value)}
            rows={4}
            maxLength={1000}
            required
          />
          <button type="submit" className="button button-primary" disabled={submitting} style={{ marginTop: 12 }}>
            {submitting ? "Submitting…" : "Submit Review"}
          </button>
        </form>
      )}
    </section>
  );
}
