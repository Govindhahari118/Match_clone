"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import api from "@/services/api";
import { toast } from "react-toastify";
import PageLoadingState from "@/components/states/PageLoadingState";

function StatTile({ label, value, tone = "brand" }) {
  const toneStyle =
    tone === "danger"
      ? { color: "#b91c1c", background: "#fef2f2", borderColor: "#fecaca" }
      : tone === "success"
        ? { color: "#166534", background: "#f0fdf4", borderColor: "#bbf7d0" }
        : tone === "warning"
          ? { color: "#92400e", background: "#fffbeb", borderColor: "#fde68a" }
          : { color: "#1d4ed8", background: "#eff6ff", borderColor: "#bfdbfe" };

  return (
    <div className="panel" style={{ padding: "0.9rem", border: `1px solid ${toneStyle.borderColor}` }}>
      <p style={{ margin: 0, fontSize: "0.75rem", color: "var(--ink-muted)", textTransform: "uppercase", letterSpacing: "0.08em", fontWeight: 700 }}>
        {label}
      </p>
      <p style={{ margin: "0.35rem 0 0", fontSize: "1.5rem", fontWeight: 800, color: toneStyle.color }}>
        {value}
      </p>
    </div>
  );
}

export default function AdminPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [queue, setQueue] = useState([]);
  const [opsMetrics, setOpsMetrics] = useState(null);
  const [runbooks, setRunbooks] = useState([]);
  const [qaSample, setQaSample] = useState([]);
  const [workingReportId, setWorkingReportId] = useState(null);
  const [workingUserId, setWorkingUserId] = useState(null);

  const handleUnauthorized = useCallback(() => {
    toast.error("Admin access required.");
    router.push("/matches");
  }, [router]);

  const loadPage = useCallback(async () => {
    setLoading(true);
    try {
      const [statsRes, usersRes, queueRes, opsRes, runbookRes, qaRes] = await Promise.all([
        api.get("/admin/dashboard"),
        api.get("/admin/users"),
        api.get("/admin/moderation/queue", { params: { limit: 50 } }),
        api.get("/admin/ops/metrics"),
        api.get("/admin/ops/runbooks"),
        api.get("/admin/moderation/qa-sample", { params: { limit: 10 } }),
      ]);

      setStats(statsRes.data || null);
      setUsers(Array.isArray(usersRes.data) ? usersRes.data : []);
      setQueue(Array.isArray(queueRes?.data?.items) ? queueRes.data.items : []);
      setOpsMetrics(opsRes.data || null);
      setRunbooks(Array.isArray(runbookRes?.data?.runbooks) ? runbookRes.data.runbooks : []);
      setQaSample(Array.isArray(qaRes?.data?.items) ? qaRes.data.items : []);
    } catch (error) {
      if (error?.response?.status === 401 || error?.response?.status === 403) {
        handleUnauthorized();
        return;
      }
      toast.error("Failed to load admin workspace.");
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized]);

  useEffect(() => {
    loadPage();
  }, [loadPage]);

  const queueStats = useMemo(() => {
    const total = queue.length;
    const overdue = queue.filter((item) => item?.sla?.overdue).length;
    const critical = queue.filter((item) => item?.severity === "critical").length;
    return { total, overdue, critical };
  }, [queue]);

  const handleBan = async (userId) => {
    setWorkingUserId(userId);
    try {
      await api.post("/admin/users/ban", { userId, reason: "Manual moderation action" });
      toast.success("User banned.");
      setUsers((previous) => previous.map((item) => (item.id === userId ? { ...item, isBanned: true } : item)));
    } catch {
      toast.error("Failed to ban user.");
    } finally {
      setWorkingUserId(null);
    }
  };

  const updateCase = async (reportId, status, safetyState = null) => {
    setWorkingReportId(reportId);
    try {
      await api.patch(`/admin/moderation/${reportId}`, {
        status,
        safetyState,
        adminNote: `Updated from admin dashboard (${status})`,
      });
      toast.success("Case updated.");
      await loadPage();
    } catch {
      toast.error("Failed to update case.");
    } finally {
      setWorkingReportId(null);
    }
  };

  if (loading) {
    return (
      <PageLoadingState
        title="Loading admin workspace..."
        description="Fetching moderation, operations, and QA datasets."
      />
    );
  }

  return (
    <div style={{ display: "grid", gap: "1rem" }}>
      <section className="panel" style={{ padding: "1rem" }}>
        <p className="section-label" style={{ marginBottom: "0.2rem" }}>Operations</p>
        <h1 style={{ margin: 0, fontSize: "1.65rem", fontFamily: "var(--font-display)" }}>Admin Workspace</h1>
        <p style={{ margin: "0.45rem 0 0", color: "var(--ink-muted)", fontSize: "0.9rem" }}>
          Moderation, risk, reliability, and support operations in one place.
        </p>
      </section>

      <section style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: "0.65rem" }}>
        <StatTile label="Total Users" value={stats?.totalUsers ?? "-"} />
        <StatTile label="Active Subs" value={stats?.activeSubscriptions ?? "-"} tone="success" />
        <StatTile label="Pending Reports" value={stats?.pendingReports ?? "-"} tone="warning" />
        <StatTile label="Queue Open" value={queueStats.total} />
        <StatTile label="Queue Overdue" value={queueStats.overdue} tone="danger" />
        <StatTile label="Critical Cases" value={queueStats.critical} tone="danger" />
      </section>

      <section className="panel" style={{ padding: "0.85rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.5rem", gap: "0.5rem", flexWrap: "wrap" }}>
          <h2 style={{ margin: 0, fontSize: "1rem" }}>Moderation Queue</h2>
          <button type="button" className="button button-secondary" onClick={loadPage}>
            Refresh
          </button>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.82rem" }}>
            <thead>
              <tr style={{ borderBottom: "1px solid var(--line)" }}>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>Report</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>Type</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>Severity</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>SLA</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>Status</th>
                <th style={{ textAlign: "right", padding: "0.5rem" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {queue.slice(0, 12).map((item) => (
                <tr key={item.id} style={{ borderBottom: "1px solid var(--line)" }}>
                  <td style={{ padding: "0.5rem" }}>{item.id.slice(0, 8)}...</td>
                  <td style={{ padding: "0.5rem" }}>{item.reportType}</td>
                  <td style={{ padding: "0.5rem" }}>{item.severity}</td>
                  <td style={{ padding: "0.5rem", color: item?.sla?.overdue ? "#b91c1c" : "var(--ink-muted)" }}>
                    {item?.sla?.overdue ? "Overdue" : `${item?.sla?.remainingMinutes ?? "-"}m left`}
                  </td>
                  <td style={{ padding: "0.5rem" }}>{item.status}</td>
                  <td style={{ padding: "0.5rem", textAlign: "right" }}>
                    <div style={{ display: "inline-flex", gap: "0.35rem", flexWrap: "wrap", justifyContent: "flex-end" }}>
                      <button type="button" className="button button-secondary" style={{ padding: "0.3rem 0.5rem", minHeight: 34 }} disabled={workingReportId === item.id} onClick={() => updateCase(item.id, "reviewing")}>
                        Review
                      </button>
                      <button type="button" className="button button-secondary" style={{ padding: "0.3rem 0.5rem", minHeight: 34 }} disabled={workingReportId === item.id} onClick={() => updateCase(item.id, "resolved", "restricted")}>
                        Resolve
                      </button>
                      <button type="button" className="button button-secondary" style={{ padding: "0.3rem 0.5rem", minHeight: 34 }} disabled={workingReportId === item.id} onClick={() => updateCase(item.id, "dismissed")}>
                        Dismiss
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {queue.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ padding: "0.8rem", color: "var(--ink-muted)", textAlign: "center" }}>
                    No moderation cases in queue.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section style={{ display: "grid", gridTemplateColumns: "minmax(0, 1.2fr) minmax(0, 1fr)", gap: "0.8rem" }} className="main-shell-grid">
        <article className="panel" style={{ padding: "0.85rem" }}>
          <h2 style={{ margin: "0 0 0.5rem", fontSize: "1rem" }}>Ops Metrics (24h)</h2>
          <div style={{ display: "grid", gap: "0.45rem" }}>
            <p style={{ margin: 0, fontSize: "0.86rem", color: "var(--ink-muted)" }}>
              Queue Open: <strong>{opsMetrics?.queue?.openCases ?? "-"}</strong>
            </p>
            <p style={{ margin: 0, fontSize: "0.86rem", color: "var(--ink-muted)" }}>
              Queue Overdue: <strong>{opsMetrics?.queue?.overdueCases ?? "-"}</strong>
            </p>
            <p style={{ margin: 0, fontSize: "0.86rem", color: "var(--ink-muted)" }}>
              Delivery Success: <strong>{Math.round((Number(opsMetrics?.reliability?.deliverySuccessRate || 0) * 100))}%</strong>
            </p>
            <p style={{ margin: 0, fontSize: "0.86rem", color: "var(--ink-muted)" }}>
              Critical Ops Events: <strong>{opsMetrics?.reliability?.criticalOpsEvents ?? "-"}</strong>
            </p>
          </div>
        </article>

        <article className="panel" style={{ padding: "0.85rem" }}>
          <h2 style={{ margin: "0 0 0.5rem", fontSize: "1rem" }}>Runbooks</h2>
          <div style={{ display: "grid", gap: "0.5rem" }}>
            {runbooks.map((item) => (
              <details key={item.key}>
                <summary style={{ cursor: "pointer", fontWeight: 700 }}>{item.title}</summary>
                <ol style={{ margin: "0.45rem 0 0", paddingLeft: "1.15rem" }}>
                  {(item.steps || []).map((step, index) => (
                    <li key={`${item.key}-${index}`} style={{ marginBottom: "0.28rem", fontSize: "0.84rem", color: "var(--ink-muted)" }}>
                      {step}
                    </li>
                  ))}
                </ol>
              </details>
            ))}
          </div>
        </article>
      </section>

      <section className="panel" style={{ padding: "0.85rem" }}>
        <h2 style={{ margin: "0 0 0.5rem", fontSize: "1rem" }}>User Management</h2>
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.82rem" }}>
            <thead>
              <tr style={{ borderBottom: "1px solid var(--line)" }}>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>Name</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>Contact</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>Role</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>Status</th>
                <th style={{ textAlign: "right", padding: "0.5rem" }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {users.slice(0, 20).map((item) => (
                <tr key={item.id} style={{ borderBottom: "1px solid var(--line)" }}>
                  <td style={{ padding: "0.5rem" }}>{`${item?.profile?.firstName || ""} ${item?.profile?.lastName || ""}`.trim() || "Unknown"}</td>
                  <td style={{ padding: "0.5rem", color: "var(--ink-muted)" }}>{item.email || item.phone || "-"}</td>
                  <td style={{ padding: "0.5rem" }}>{item.role}</td>
                  <td style={{ padding: "0.5rem", color: item.isBanned ? "#b91c1c" : "var(--ink-muted)" }}>{item.isBanned ? "Banned" : "Active"}</td>
                  <td style={{ padding: "0.5rem", textAlign: "right" }}>
                    {item.role !== "admin" && !item.isBanned && (
                      <button
                        type="button"
                        className="button button-secondary"
                        style={{ minHeight: 34, padding: "0.3rem 0.6rem" }}
                        onClick={() => handleBan(item.id)}
                        disabled={workingUserId === item.id}
                      >
                        {workingUserId === item.id ? "..." : "Ban"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel" style={{ padding: "0.85rem" }}>
        <h2 style={{ margin: "0 0 0.5rem", fontSize: "1rem" }}>Moderation QA Sample</h2>
        <div style={{ display: "grid", gap: "0.45rem" }}>
          {qaSample.length === 0 && <p style={{ margin: 0, color: "var(--ink-muted)" }}>No sample records available.</p>}
          {qaSample.map((item) => (
            <div key={item.id} className="panel" style={{ padding: "0.65rem" }}>
              <p style={{ margin: 0, fontSize: "0.82rem", fontWeight: 700 }}>
                {item.id.slice(0, 8)}... | {item.reportType} | {item.status}
              </p>
              <p style={{ margin: "0.25rem 0 0", fontSize: "0.8rem", color: "var(--ink-muted)" }}>
                {item.description || "No description"}
              </p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
