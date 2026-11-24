import React, { FormEvent, useState } from "react";
import { useJobWebSocket } from "./hooks/useJobWebSocket";
import { jobRequest } from "./jobsApi";
import { Job, JobScenario, JobStatus } from "./types";

const scenarios: Record<JobScenario, string> = {
  SUCCESS: "Succeed immediately",
  FAIL_ONCE: "Fail once, then succeed",
  EXHAUST_RETRIES: "Exhaust retries",
};

function statusLabel(job: Job) {
  if (job.status === "PENDING") return job.attempts.some(attempt => attempt.runNumber === job.runNumber)
    ? "Retry scheduled" : "Queued";
  if (job.status === "FAILED") return job.deadLetteredAt ? "Dead-lettered" : "Failed";
  return job.status === "COMPLETED" ? "Completed" : "Processing";
}

function formatTime(value: string) {
  return new Date(value).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

export default function App() {
  const { jobs, connected, loading, error, update } = useJobWebSocket();
  const [name, setName] = useState("");
  const [scenario, setScenario] = useState<JobScenario>("SUCCESS");
  const [filter, setFilter] = useState<"ALL" | JobStatus>("ALL");
  const [selectedId, setSelectedId] = useState<number>();
  const [recovery, setRecovery] = useState<JobScenario>("SUCCESS");
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState<{ text: string; error?: boolean }>();

  const selected = jobs.find(job => job.id === selectedId);
  const filtered = jobs.filter(job => filter === "ALL" || job.status === filter);
  const metrics = [
    ["Queued", jobs.filter(job => job.status === "PENDING").length],
    ["Completed", jobs.filter(job => job.status === "COMPLETED").length],
    ["Dead-lettered", jobs.filter(job => job.status === "FAILED" && job.deadLetteredAt).length],
  ] as const;

  async function runAction(action: () => Promise<void>) {
    setBusy(true);
    setFeedback(undefined);
    try {
      await action();
    } catch (error) {
      setFeedback({ text: error instanceof Error ? error.message : "The request failed.", error: true });
    } finally {
      setBusy(false);
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void runAction(async () => {
      const query = new URLSearchParams({ name: name.trim(), scenario });
      const job = await jobRequest<Job>("?" + query, { method: "POST" });
      update(job);
      setSelectedId(job.id);
      setFilter("ALL");
      setName("");
      setFeedback({ text: `Job #${job.id} was queued.` });
    });
  }

  return (
    <main className="dashboard">
      <header className="dashboard-header">
        <div>
          <p className="dashboard-label">Job Platform</p>
          <h1>Job operations</h1>
        </div>
        <span className={`connection ${connected ? "connected" : ""}`} role="status">
          {connected ? "Live updates" : "Polling for updates"}
        </span>
      </header>

      <dl className="metrics" aria-label="Job totals for the latest 200 jobs">
        {metrics.map(([label, count]) => <div key={label}><dt>{label}</dt><dd>{count}</dd></div>)}
      </dl>

      <section className="panel submit-panel" aria-labelledby="submit-title">
        <h2 id="submit-title">Submit a job</h2>
        <form className="job-form" onSubmit={submit}>
          <label>Job name<input value={name} onChange={event => setName(event.target.value)}
            placeholder="e.g. Generate monthly report" maxLength={120} required /></label>
          <label>Scenario<select value={scenario} onChange={event => setScenario(event.target.value as JobScenario)}>
            {Object.entries(scenarios).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select></label>
          <button className="primary" disabled={busy || !name.trim()}>Submit job</button>
          <button type="button" className="secondary" disabled={busy} onClick={() => void runAction(async () => {
            const demo = await jobRequest<Job[]>("/generate-test", { method: "POST" });
            update(...demo);
            setFilter("ALL");
            setSelectedId(demo[demo.length - 1].id);
            setFeedback({ text: "Three demo jobs were queued." });
          })}>Run all examples</button>
        </form>
        {feedback && <p className={`feedback ${feedback.error ? "error" : ""}`}
          role={feedback.error ? "alert" : "status"}>{feedback.text}</p>}
      </section>

      <div className="workspace">
        <section className="panel" aria-labelledby="jobs-title">
          <div className="panel-heading">
            <h2 id="jobs-title">Recent jobs</h2>
            <label>Status <select value={filter} onChange={event => setFilter(event.target.value as "ALL" | JobStatus)}>
              <option value="ALL">All jobs</option><option value="PENDING">Queued and retrying</option>
              <option value="PROCESSING">Processing</option><option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed and dead-lettered</option>
            </select></label>
          </div>
          {error && <p className="feedback error" role="alert">{error}</p>}
          {loading ? <p className="muted" role="status">Loading jobs…</p> :
            !filtered.length ? <p className="muted">{jobs.length ? "No jobs match this filter." : "Submit a job to see it here."}</p> :
            <div className="table-scroll"><table>
              <thead><tr><th scope="col">Job</th><th scope="col">Status</th><th scope="col">Run</th></tr></thead>
              <tbody>{filtered.map(job => (
                <tr key={job.id} className={job.id === selectedId ? "selected" : undefined}>
                  <td><button className="job-link" aria-pressed={job.id === selectedId}
                    onClick={() => { setSelectedId(job.id); setRecovery("SUCCESS"); }}>
                    <span>#{job.id}</span> {job.name}
                  </button></td>
                  <td><span className={`status status-${job.status.toLowerCase()}`}>{statusLabel(job)}</span></td>
                  <td>{job.runNumber}</td>
                </tr>
              ))}</tbody>
            </table></div>}
        </section>

        <section className="panel" aria-labelledby="timeline-title">
          <h2 id="timeline-title">Attempt timeline</h2>
          {!selected ? <p className="muted">Select a job to see its attempts.</p> : (
            <>
              <div className="timeline-heading">
                <h3>#{selected.id} · {selected.name}</h3>
                <p className="muted">{scenarios[selected.scenario]} · Run {selected.runNumber}</p>
              </div>
              {selected.status === "PENDING" && selected.nextAttemptAt && (
                <p className="muted">Eligible to run at {formatTime(selected.nextAttemptAt)}.</p>
              )}
              {!selected.attempts.length ? <p className="muted">No attempts recorded yet.</p> : (
                <ol className="attempts">
                  {selected.attempts.map(attempt => (
                    <li key={`${attempt.runNumber}-${attempt.attemptNumber}`} className={attempt.succeeded ? "success" : "failed"}>
                      <div><strong>Run {attempt.runNumber} · Attempt {attempt.attemptNumber}</strong>
                        <span>{attempt.succeeded ? "Succeeded" : "Failed"}</span></div>
                      <time dateTime={attempt.finishedAt}>{formatTime(attempt.finishedAt)}</time>
                    </li>
                  ))}
                </ol>
              )}
              {selected.deadLetteredAt && (
                <div className="requeue">
                  <p className="muted">Dead-lettered at {formatTime(selected.deadLetteredAt)}.</p>
                  <label>Next run scenario<select value={recovery} onChange={event => setRecovery(event.target.value as JobScenario)}>
                    {Object.entries(scenarios).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select></label>
                  <button className="primary" type="button" disabled={busy} onClick={() => void runAction(async () => {
                    const job = await jobRequest<Job>(`/${selected.id}/reprocess-dlq?scenario=${recovery}`, { method: "POST" });
                    update(job);
                    setFilter("ALL");
                    setFeedback({ text: `Job #${job.id} was requeued for run ${job.runNumber}.` });
                  })}>Requeue job</button>
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </main>
  );
}
