import { useCallback, useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { apiBase, jobRequest } from "../jobsApi";
import { Job } from "../types";

function mergeJobs(current: Job[], incoming: Job[], allowEqual: boolean) {
  const rows = new Map(current.map(job => [job.id, job]));
  for (const job of incoming) {
    const previous = rows.get(job.id);
    const newer = previous && Date.parse(job.updatedAt) - Date.parse(previous.updatedAt);
    if (!previous || (newer !== undefined && (newer > 0 || (allowEqual && newer === 0)))) {
      rows.set(job.id, job);
    }
  }
  return [...rows.values()].sort((a, b) => b.id - a.id).slice(0, 200);
}

export function useJobWebSocket() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const update = useCallback((...incoming: Job[]) => {
    setJobs(current => mergeJobs(current, incoming, true));
  }, []);

  useEffect(() => {
    let active = true;
    let live = false;
    async function refresh() {
      try {
        const snapshot = await jobRequest<Job[]>();
        if (!active) return;
        setJobs(current => mergeJobs(current, snapshot, false));
        setError("");
      } catch (error) {
        if (active) setError(error instanceof Error ? error.message : "Unable to load jobs.");
      } finally {
        if (active) setLoading(false);
      }
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(`${apiBase || window.location.origin}/ws-jobs`),
      reconnectDelay: 3000,
    });
    client.onConnect = () => {
      if (!active) return;
      live = true;
      setConnected(true);
      client.subscribe("/topic/jobs", message => update(JSON.parse(message.body) as Job));
      void refresh();
    };
    client.onWebSocketClose = () => {
      live = false;
      if (active) setConnected(false);
    };

    client.activate();
    void refresh();
    const poll = window.setInterval(() => { if (!live) void refresh(); }, 5000);
    return () => {
      active = false;
      window.clearInterval(poll);
      void client.deactivate();
    };
  }, [update]);

  return { jobs, connected, loading, error, update };
}
