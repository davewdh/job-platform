import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { Job } from "../types";

export const useJobWebSocket = (url: string) => {
  const [jobs, setJobs] = useState<Job[]>([]);

  useEffect(() => {
    fetch("http://localhost:8080/jobs")
      .then((res) => res.json())
      .then((data: Job[]) => {
        const sorted = data.sort((a, b) => b.id - a.id);
        setJobs(sorted);
      })
      .catch(console.error);
  }, []);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(url),
      debug: (str) => console.log(str),
    });

    client.onConnect = () => {
      console.log("WebSocket connected");
      client.subscribe("/topic/jobs", (message) => {
        const updatedJob: Job = JSON.parse(message.body);

        setJobs((prevJobs) => {
          const exists = prevJobs.some((j) => j.id === updatedJob.id);
          const newJobs = exists
            ? prevJobs.map((j) => (j.id === updatedJob.id ? updatedJob : j))
            : [...prevJobs, updatedJob];

          return newJobs.sort((a, b) => b.id - a.id);
        });
      });
    };

    client.activate();

    return () =>  { client.deactivate(); }
  }, [url]);

  return jobs;
};
