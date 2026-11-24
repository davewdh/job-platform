package com.jobplatform.backend.controller;

import com.jobplatform.backend.dto.JobResponseDto;
import com.jobplatform.backend.model.JobScenario;
import com.jobplatform.backend.service.JobService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/jobs", "/api/jobs"})
public class JobController {
    private final JobService jobs;
    public JobController(JobService jobs) { this.jobs = jobs; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponseDto create(@RequestParam String name,
            @RequestParam(defaultValue = "SUCCESS") JobScenario scenario) {
        return jobs.createJob(name, scenario);
    }

    @GetMapping("/{id}")
    public JobResponseDto get(@PathVariable Long id) { return jobs.getJob(id); }

    @GetMapping
    public List<JobResponseDto> list() { return jobs.getAllJobs(); }

    @PostMapping({"/{id}/retry", "/{id}/reprocess-dlq"})
    public JobResponseDto reprocess(@PathVariable Long id, @RequestParam(required = false) JobScenario scenario) {
        return jobs.requeue(id, scenario);
    }

    @PostMapping("/generate-test")
    @ResponseStatus(HttpStatus.CREATED)
    public List<JobResponseDto> demo() {
        return List.of(
                jobs.createJob("Successful job", JobScenario.SUCCESS),
                jobs.createJob("Retries once", JobScenario.FAIL_ONCE),
                jobs.createJob("Dead-letter example", JobScenario.EXHAUST_RETRIES));
    }
}
