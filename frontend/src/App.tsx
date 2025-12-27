import React, { useState } from "react";
import { useJobWebSocket } from "./hooks/useJobWebSocket";
import { Job } from "./types";
import { JobTable } from "./components/JobTable";

const WS_URL = "http://localhost:8080/ws-jobs";
const API_URL = "http://localhost:8080/jobs";

function App() {
  const jobs = useJobWebSocket(WS_URL);
  const [retryingJobs, setRetryingJobs] = useState<number[]>([]);
  const [reprocessingJobs, setReprocessingJobs] = useState<number[]>([]);

  const retryJob = async (id: number) => {
    setRetryingJobs((prev) => [...prev, id]);
    try {
      const res = await fetch(`${API_URL}/${id}/retry`, { method: "POST" });
      if (!res.ok) alert(await res.text());
    } catch (err) {
      alert(`Error retrying job: ${err}`);
    } finally {
      setRetryingJobs((prev) => prev.filter((jobId) => jobId !== id));
    }
  };

  const reprocessDLQ = async (id: number) => {
    setReprocessingJobs((prev) => [...prev, id]);
    try {
      const res = await fetch(`${API_URL}/${id}/reprocess-dlq`, { method: "POST" });
      if (!res.ok) alert(await res.text());
    } catch (err) {
      alert(`Error reprocessing DLQ job: ${err}`);
    } finally {
      setReprocessingJobs((prev) => prev.filter((jobId) => jobId !== id));
    }
  };

  const generateTestJobs = async () => {
    try {
      const res = await fetch(`${API_URL}/generate-test`, { method: "POST" });
      if (!res.ok) alert(await res.text());
    } catch (err) {
      alert(`Error: ${err}`);
    }
  };

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-4">Job Dashboard</h1>

      <button
        className="px-4 py-2 bg-purple-500 text-white rounded mb-4"
        onClick={generateTestJobs}
      >
        Generate Test Jobs
      </button>

      <JobTable
        jobs={jobs}
        retryingJobs={retryingJobs}
        reprocessingJobs={reprocessingJobs}
        onRetry={retryJob}
        onReprocessDLQ={reprocessDLQ}
      />
    </div>
  );
}

export default App;
