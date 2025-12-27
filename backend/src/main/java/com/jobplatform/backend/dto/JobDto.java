package com.jobplatform.backend.dto;

import com.jobplatform.backend.model.JobStatus;
import java.time.LocalDateTime;

public class JobDto {
    private Long id;
    private String name;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int retryCount;

    public JobDto(Long id, String name, JobStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, int retryCount) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.retryCount = retryCount;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public JobStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getRetryCount() { return retryCount; }
}
