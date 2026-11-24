export const apiBase = (process.env.REACT_APP_API_URL || "").replace(/\/$/, "");

export async function jobRequest<T>(path = "", options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBase}/jobs${path}`, options);
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.detail || data?.message || `Request failed (${response.status}).`);
  if (!data) throw new Error("The job API did not return a response.");
  return data as T;
}
