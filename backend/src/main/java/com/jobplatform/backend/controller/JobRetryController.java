package com.jobplatform.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobplatform.backend.model.Job;
import com.jobplatform.backend.model.JobStatus;
import com.jobplatform.backend.repository.JobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs")
public class JobRetryController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JobRepository jobRepository;

    public JobRetryController(KafkaTemplate<String, String> kafkaTemplate,
                              JobRepository jobRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.jobRepository = jobRepository;
    }

    // Manual retry for FAILED jobs
    @PostMapping("/{id}/retry")
    public ResponseEntity<String> retryJob(@PathVariable Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.getStatus() != JobStatus.FAILED) {
            return ResponseEntity.badRequest().body("Only FAILED jobs can be retried");
        }

        job.setRetryCount(job.getRetryCount() + 1); // increment retry count
        jobRepository.save(job);

        String message = new ObjectMapper().createObjectNode()
                .put("jobId", job.getId())
                .put("eventType", "CREATED")
                .put("retryCount", job.getRetryCount())
                .toString();

        kafkaTemplate.send("job-events", message);
        return ResponseEntity.ok("Job retried manually");
    }

    // DLQ reprocess endpoint
    @PostMapping("/{id}/reprocess-dlq")
    public ResponseEntity<String> reprocessDlqJob(@PathVariable Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.getStatus() != JobStatus.FAILED) {
            return ResponseEntity.badRequest().body("Only FAILED jobs can be reprocessed");
        }

        // Send job back to Kafka with current retry count
        String message = new ObjectMapper().createObjectNode()
                .put("jobId", job.getId())
                .put("eventType", "CREATED")
                .put("retryCount", job.getRetryCount())
                .toString();

        kafkaTemplate.send("job-events", message);
        return ResponseEntity.ok("DLQ job reprocessed");
    }
}
