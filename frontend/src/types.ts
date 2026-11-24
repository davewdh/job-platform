export type JobStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
export type JobScenario = "SUCCESS" | "FAIL_ONCE" | "EXHAUST_RETRIES";

export interface JobAttempt {
  runNumber: number;
  attemptNumber: number;
  succeeded: boolean;
  finishedAt: string;
}

export interface Job {
  id: number;
  name: string;
  status: JobStatus;
  scenario: JobScenario;
  createdAt: string;
  updatedAt: string;
  retryCount: number;
  runNumber: number;
  nextAttemptAt: string | null;
  deadLetteredAt: string | null;
  attempts: JobAttempt[];
}
