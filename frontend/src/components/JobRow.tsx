import React from "react";
import { Job } from "../types";

interface JobRowProps {
  job: Job;
  retrying: boolean;
  reprocessing: boolean;
  onRetry: (id: number) => void;
  onReprocessDLQ: (id: number) => void;
}

export const JobRow: React.FC<JobRowProps> = ({
  job,
  retrying,
  reprocessing,
  onRetry,
  onReprocessDLQ,
}) => {
  const statusColors: Record<string, string> = {
    PENDING: "bg-gray-300 text-gray-800",
    PROCESSING: "bg-yellow-300 text-yellow-800",
    COMPLETED: "bg-green-300 text-green-800",
    FAILED: "bg-red-300 text-red-800",
  };

  return (
    <tr>
      <td className="border px-4 py-2">{job.id}</td>
      <td className="border px-4 py-2">{job.name}</td>
      <td className="border px-4 py-2">
        <span className={`px-2 py-1 rounded ${statusColors[job.status]}`}>
          {job.status}
        </span>
      </td>
      <td className="border px-4 py-2">{job.retryCount}</td>
      <td className="border px-4 py-2">{new Date(job.createdAt).toLocaleString()}</td>
      <td className="border px-4 py-2">{new Date(job.updatedAt).toLocaleString()}</td>
      <td className="border px-4 py-2 flex gap-2">
        {job.status === "FAILED" && (
          <>
            <button
              className="px-2 py-1 bg-blue-500 text-white rounded disabled:opacity-50"
              onClick={() => onRetry(job.id)}
              disabled={retrying}
            >
              Retry
            </button>
            <button
              className="px-2 py-1 bg-red-500 text-white rounded disabled:opacity-50"
              onClick={() => onReprocessDLQ(job.id)}
              disabled={reprocessing}
            >
              DLQ Reprocess
            </button>
          </>
        )}
      </td>
    </tr>
  );
};
