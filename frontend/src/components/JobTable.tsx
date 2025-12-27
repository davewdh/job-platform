import React from "react";
import { Job } from "../types";
import { JobRow } from "./JobRow";

interface JobTableProps {
  jobs: Job[];
  retryingJobs: number[];
  reprocessingJobs: number[];
  onRetry: (id: number) => void;
  onReprocessDLQ: (id: number) => void;
}

export const JobTable: React.FC<JobTableProps> = ({
  jobs,
  retryingJobs,
  reprocessingJobs,
  onRetry,
  onReprocessDLQ,
}) => {
  return (
    <table className="min-w-full border border-gray-200">
      <thead>
        <tr>
          <th className="border px-4 py-2">ID</th>
          <th className="border px-4 py-2">Name</th>
          <th className="border px-4 py-2">Status</th>
          <th className="border px-4 py-2">Retry Count</th>
          <th className="border px-4 py-2">Created</th>
          <th className="border px-4 py-2">Updated</th>
          <th className="border px-4 py-2">Actions</th>
        </tr>
      </thead>
      <tbody>
        {jobs.map((job) => (
          <JobRow
            key={job.id}
            job={job}
            retrying={retryingJobs.includes(job.id)}
            reprocessing={reprocessingJobs.includes(job.id)}
            onRetry={onRetry}
            onReprocessDLQ={onReprocessDLQ}
          />
        ))}
      </tbody>
    </table>
  );
};
