package com.jobplatform.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private int retryCount = 0;

    public Job() {
        this.status = JobStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Job(String name) {
        this();
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { 
        return id; 
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public JobStatus getStatus() { 
        return status; 
    }

    public void setStatus(JobStatus status) { 
        this.status = status; 
    }

    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }

    public LocalDateTime getUpdatedAt() { 
        return updatedAt; 
    }

    public void setUpdatedAt(LocalDateTime updatedAt) { 
        this.updatedAt = updatedAt; 
    }

    public int getRetryCount() { 
        return retryCount; 
    }

    public void setRetryCount(int retryCount) { 
        this.retryCount = retryCount; 
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
