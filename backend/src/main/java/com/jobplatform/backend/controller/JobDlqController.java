package com.jobplatform.backend.controller;

import com.jobplatform.backend.service.JobDlqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class JobDlqController {

    private final JobDlqService jobDlqService;

    public JobDlqController(JobDlqService jobDlqService) {
        this.jobDlqService = jobDlqService;
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<?> retryJob(@PathVariable Long jobId) {

        boolean success = jobDlqService.reprocessJob(jobId);

        if (success) {
            return ResponseEntity.ok("Job requeued successfully");
        }

        return ResponseEntity
                .status(404)
                .body("Job not found in DLQ");
    }
}
