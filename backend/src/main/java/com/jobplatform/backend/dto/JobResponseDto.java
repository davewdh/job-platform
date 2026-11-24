package com.jobplatform.backend.dto;

import com.jobplatform.backend.model.*;
import java.time.*;
import java.util.List;

public record JobResponseDto(Long id, String name, JobStatus status, JobScenario scenario, int retryCount,
        int runNumber, Instant createdAt, Instant updatedAt, Instant nextAttemptAt,
        Instant deadLetteredAt, List<JobAttempt> attempts) {
    public static JobResponseDto from(Job job) {
        return new JobResponseDto(job.getId(), job.getName(), job.getStatus(), job.getScenario(),
                job.getRetryCount(), job.getRunNumber(), job.getCreatedAt().toInstant(ZoneOffset.UTC),
                job.getUpdatedAt().toInstant(ZoneOffset.UTC), job.getNextAttemptAt(),
                job.getDeadLetteredAt(), List.copyOf(job.getAttempts()));
    }
}
