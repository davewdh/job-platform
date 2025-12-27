package com.jobplatform.backend.controller;

import com.jobplatform.backend.dto.JobResponseDto;
import com.jobplatform.backend.model.Job;
import com.jobplatform.backend.model.JobStatus;
import com.jobplatform.backend.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // Create a new job
    @PostMapping
    public ResponseEntity<JobResponseDto> createJob(@RequestParam String name) {
        Job job = jobService.createJob(name);
        return ResponseEntity.ok(convertToDto(job));
    }

    // Get a job by ID
    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJob(@PathVariable Long id) {
        Job job = jobService.getJob(id);
        return ResponseEntity.ok(convertToDto(job));
    }

    // Get all jobs
    @GetMapping
    public ResponseEntity<List<JobResponseDto>> getAllJobs() {
        List<JobResponseDto> jobs = jobService.getAllJobs()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobs);
    }

    // Update job status
    @PutMapping("/{id}/status")
    public ResponseEntity<JobResponseDto> updateJobStatus(
            @PathVariable Long id,
            @RequestParam JobStatus status
    ) {
        Job job = jobService.updateJobStatus(id, status);
        return ResponseEntity.ok(convertToDto(job));
    }

    // Convert entity to DTO
    private JobResponseDto convertToDto(Job job) {
        JobResponseDto dto = new JobResponseDto();
        dto.setId(job.getId());
        dto.setName(job.getName());
        dto.setStatus(job.getStatus().name());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        dto.setRetryCount(job.getRetryCount());
        return dto;
    }
}
