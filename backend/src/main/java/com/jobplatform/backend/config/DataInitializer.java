package com.jobplatform.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.jobplatform.backend.service.JobService;

@Component
public class DataInitializer implements CommandLineRunner {

    private final JobService jobService;

    public DataInitializer(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void run(String... args) throws Exception {
        for (int i = 1; i <= 5; i++) {
            jobService.createJob("TestJob_" + i);
        }
    }
}