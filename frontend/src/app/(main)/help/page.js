"use client";

import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";

const FAQS = [
  ["What does Identity Verified mean?", "It means the supported identity verification process was completed. It does not guarantee compatibility, conduct or marriage outcome."],
  ["Why do I see Active this week instead of an exact time?", "Activity is shown as a privacy-friendly freshness bucket unless the member has chosen to share exact last-seen information."],
  ["Can premium members see my phone number automatically?", "No. Paid membership does not override your contact privacy. Contact details require the configured consent flow."],
  ["Why did a profile stop appearing?", "The member may have paused, found a match, become stale, been removed from discovery, blocked you, or no longer satisfy your must-match preferences."],
  ["Will the app relax my mandatory filters?", "No. Preferences marked Must match are enforced before ranking. If no profiles match, you choose whether to relax a criterion."],
  ["What happens when I block someone?", "Blocking removes discovery, active matching, messaging and pending contact/photo-access paths between the two profiles."],
  ["What does Resolved mean on a support ticket?", "Support believes the issue has been addressed. The ticket becomes Closed only after you confirm the resolution."],
];

function humanize(value) {
  return String(value || "").replace(/_/g, " ").replace(/\b\w/g, (char) => char.toUpperCase());
}

export default function HelpPage() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState({ category: "technical", subject: "", description: "", priority: "normal" });
  const [search, setSearch] = useState("");
  const [openFaq, setOpenFaq] = useState(null);

  const loadTickets = async () => {
    setLoading(true);
    try {
      const response = await api.get("/support");
      setTickets(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      toast.error(error?.response?.data?.error || "Support tickets could not be loaded.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadTickets(); }, []);

  const filteredFaqs = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return FAQS;
    return FAQS.filter(([question, answer]) => `${question} ${answer}`.toLowerCase().includes(query));
  }, [search]);

  const createTicket = async (event) => {
    event.preventDefault();
    if (!form.subject.trim() || !form.description.trim()) {
      toast.error("Subject and description are required.");
      return;
    }
    setCreating(true);
    try {
      await api.post("/support", form);
      setForm({ category: "technical", subject: "", description: "", priority: "normal" });
      await loadTickets();
      toast.success("Support ticket created. Its status and history will remain visible here.");
    } catch (error) {
      toast.error(error?.response?.data?.error || "Ticket could not be created.");
    } finally {
      setCreating(false);
    }
  };

  const confirmResolution = async (ticketId, accepted) => {
    try {
      await api.post(`/support/${ticketId}/confirm-resolution`, { accepted });
      await loadTickets();
      toast.success(accepted ? "Ticket closed after your confirmation." : "Ticket reopened for further work.");
    } catch (error) {
      toast.error(error?.response?.data?.error || "Ticket status could not be updated.");
    }
  };

  return (
    <div style={{ maxWidth: 980, margin: "0 auto", display: "grid", gap: "1rem" }}>
      <header className="panel" style={{ padding: "1.1rem" }}>
        <p className="section-label" style={{ marginBottom: "0.2rem" }}>Help & accountability</p>
        <h1 style={{ margin: 0, fontFamily: "var(--font-display)", fontSize: "1.9rem" }}>Support you can track</h1>
        <p style={{ margin: "0.4rem 0 0", color: "var(--ink-muted)", lineHeight: 1.55 }}>No fake hotline, invented SLA, or demo “ticket created” state. Every ticket below is backed by a real server record and status timeline.</p>
      </header>

      <section className="panel" style={{ padding: "1rem" }}>
        <h2 style={{ marginTop: 0 }}>Knowledge base</h2>
        <input className="form-input" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search verification, privacy, matching, blocking…" />
        <div style={{ display: "grid", gap: "0.45rem", marginTop: "0.7rem" }}>
          {filteredFaqs.map(([question, answer], index) => (
            <button key={question} type="button" className="panel" onClick={() => setOpenFaq(openFaq === index ? null : index)} style={{ padding: "0.7rem", textAlign: "left", cursor: "pointer" }}>
              <strong>{question}</strong>
              {openFaq === index && <p style={{ margin: "0.4rem 0 0", color: "var(--ink-muted)", lineHeight: 1.5 }}>{answer}</p>}
            </button>
          ))}
          {filteredFaqs.length === 0 && <p style={{ color: "var(--ink-muted)" }}>No article matched. Raise a ticket below.</p>}
        </div>
      </section>

      <section className="panel" style={{ padding: "1rem" }}>
        <h2 style={{ marginTop: 0 }}>Raise a support ticket</h2>
        <form onSubmit={createTicket} style={{ display: "grid", gap: "0.65rem" }}>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(200px,1fr))", gap: "0.6rem" }}>
            <label>Category<select className="form-input" value={form.category} onChange={(e) => setForm((p) => ({ ...p, category: e.target.value }))}><option value="technical">Technical issue</option><option value="payment">Payment</option><option value="verification">Verification</option><option value="safety">Safety / report</option><option value="privacy">Privacy</option><option value="account">Account</option><option value="other">Other</option></select></label>
            <label>Priority<select className="form-input" value={form.priority} onChange={(e) => setForm((p) => ({ ...p, priority: e.target.value }))}><option value="normal">Normal</option><option value="high">High</option><option value="critical">Critical</option></select></label>
          </div>
          <label>Subject<input className="form-input" maxLength={180} value={form.subject} onChange={(e) => setForm((p) => ({ ...p, subject: e.target.value }))} /></label>
          <label>Describe the issue<textarea className="form-input" rows={5} value={form.description} onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))} /></label>
          <button className="button button-primary" type="submit" disabled={creating}>{creating ? "Creating…" : "Create ticket"}</button>
        </form>
      </section>

      <section className="panel" style={{ padding: "1rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: "0.6rem", alignItems: "center" }}>
          <h2 style={{ margin: 0 }}>My tickets</h2>
          <button type="button" className="button button-secondary" onClick={loadTickets}>Refresh</button>
        </div>
        <div style={{ display: "grid", gap: "0.55rem", marginTop: "0.75rem" }}>
          {loading ? <p>Loading tickets…</p> : tickets.length === 0 ? <p style={{ color: "var(--ink-muted)" }}>No tickets yet.</p> : tickets.map((ticket) => (
            <article key={ticket.id} className="panel" style={{ padding: "0.75rem" }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: "0.6rem", flexWrap: "wrap" }}>
                <div>
                  <strong>{ticket.subject}</strong>
                  <p style={{ margin: "0.2rem 0 0", color: "var(--ink-muted)", fontSize: "0.78rem" }}>{humanize(ticket.category)} · Priority {humanize(ticket.priority)} · Ticket {ticket.id.slice(0, 8)}</p>
                </div>
                <span className="chip chip-support">{humanize(ticket.status)}</span>
              </div>
              <p style={{ margin: "0.55rem 0", lineHeight: 1.5 }}>{ticket.description}</p>
              <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.75rem" }}>Assigned: {ticket.assignedTeam || "Waiting for assignment"} · Last update: {new Date(ticket.updatedAt).toLocaleString()}</p>
              {ticket.resolution && <div className="panel" style={{ marginTop: "0.6rem", padding: "0.6rem" }}><strong>Support resolution</strong><p style={{ margin: "0.25rem 0 0" }}>{ticket.resolution}</p></div>}
              {ticket.status === "resolved" && (
                <div style={{ display: "flex", gap: "0.45rem", marginTop: "0.65rem", flexWrap: "wrap" }}>
                  <button className="button button-primary" type="button" onClick={() => confirmResolution(ticket.id, true)}>Issue resolved — close</button>
                  <button className="button button-secondary" type="button" onClick={() => confirmResolution(ticket.id, false)}>Not resolved — reopen</button>
                </div>
              )}
              {ticket.events?.length > 0 && (
                <details style={{ marginTop: "0.6rem" }}>
                  <summary style={{ cursor: "pointer", fontWeight: 700 }}>Ticket history ({ticket.events.length})</summary>
                  <div style={{ display: "grid", gap: "0.35rem", marginTop: "0.45rem" }}>
                    {ticket.events.map((event) => <div key={event.id} style={{ fontSize: "0.76rem", color: "var(--ink-muted)" }}>{new Date(event.createdAt).toLocaleString()} · {humanize(event.eventType)}{event.message ? ` · ${event.message}` : ""}</div>)}
                  </div>
                </details>
              )}
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
