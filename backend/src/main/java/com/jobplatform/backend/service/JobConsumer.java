package com.jobplatform.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobplatform.backend.config.KafkaConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class JobConsumer {
    private final JobService jobs;
    private final ObjectMapper json;

    public JobConsumer(JobService jobs, ObjectMapper json) {
        this.jobs = jobs;
        this.json = json;
    }

    @KafkaListener(topics = KafkaConfig.JOB_TOPIC, groupId = "job-worker")
    public void consume(String message) throws JsonProcessingException, InterruptedException {
        JobMessage attempt = json.readValue(message, JobMessage.class);
        if (jobs.start(attempt)) {
            Thread.sleep(350); // Simulated work so the processing transition is visible in the demo.
            jobs.finish(attempt);
        }
    }
}
