package com.jobplatform.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobplatform.backend.service.JobService;

@RestController
@RequestMapping("/jobs")
public class JobTestController {

    private final JobService jobService;

    public JobTestController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/generate-test")
    public ResponseEntity<String> generateTestJobs() {
        for (int i = 1; i <= 5; i++) {
            jobService.createJob("TestJob_" + i);
        }
        return ResponseEntity.ok("Test jobs created");
    }
}
