export interface Job {
  id: number;
  name: string;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  createdAt: string;
  updatedAt: string;
  retryCount: number; 
}