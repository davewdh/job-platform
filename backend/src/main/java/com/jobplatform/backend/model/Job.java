package com.jobplatform.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Job {
    public static final int MAX_ATTEMPTS = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    @Enumerated(EnumType.STRING)
    private JobScenario scenario;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private int retryCount;
    private Integer runNumber;
    private Integer attemptNumber;
    private Instant nextAttemptAt;
    private Instant dispatchedAt;
    private Instant deadLetteredAt;

    @ElementCollection
    @OrderColumn(name = "attempt_index")
    private List<JobAttempt> attempts = new ArrayList<>();

    protected Job() {}

    public Job(String name, JobScenario scenario, Instant now) {
        this.name = name;
        this.scenario = scenario;
        this.status = JobStatus.PENDING;
        this.createdAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        this.updatedAt = createdAt;
        this.runNumber = 1;
        this.attemptNumber = 0;
        this.nextAttemptAt = now;
    }

    public boolean accepts(int run, int attempt, Instant now) {
        return matches(run, attempt) && isDue(now) && (status == JobStatus.PENDING ||
                (status == JobStatus.PROCESSING && !updatedAt.toInstant(ZoneOffset.UTC).isAfter(now.minusSeconds(30))));
    }

    public boolean matches(int run, int attempt) {
        return getRunNumber() == run && getAttemptNumber() + 1 == attempt;
    }

    public void startAttempt(Instant now) {
        status = JobStatus.PROCESSING;
        markDispatched(now);
        touch(now);
    }

    public boolean isDue(Instant now) {
        return nextAttemptAt == null || !nextAttemptAt.isAfter(now);
    }

    public void recordAttempt(Instant now) {
        attemptNumber = getAttemptNumber() + 1;
        retryCount = attemptNumber - 1;
        boolean success = getScenario().succeeds(attemptNumber);
        attempts.add(new JobAttempt(getRunNumber(), attemptNumber, success, now));
        dispatchedAt = null;
        nextAttemptAt = null;
        if (success) {
            status = JobStatus.COMPLETED;
        } else if (attemptNumber < MAX_ATTEMPTS) {
            status = JobStatus.PENDING;
            nextAttemptAt = now.plusSeconds(1L << (attemptNumber - 1));
        } else {
            status = JobStatus.FAILED;
        }
        touch(now);
    }

    public void requeue(JobScenario replacement, Instant now) {
        if (replacement != null) scenario = replacement;
        runNumber = getRunNumber() + 1;
        attemptNumber = 0;
        retryCount = 0;
        status = JobStatus.PENDING;
        nextAttemptAt = now;
        dispatchedAt = null;
        deadLetteredAt = null;
        touch(now);
    }

    public void touch(Instant now) { updatedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC); }
    public void markDispatched(Instant now) { dispatchedAt = now; }
    public void markDeadLettered(Instant now) { deadLetteredAt = now; touch(now); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public JobStatus getStatus() { return status; }
    public JobScenario getScenario() { return scenario == null ? JobScenario.SUCCESS : scenario; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getRetryCount() { return retryCount; }
    public int getRunNumber() { return runNumber == null ? 1 : runNumber; }
    public int getAttemptNumber() { return attemptNumber == null ? 0 : attemptNumber; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public Instant getDeadLetteredAt() { return deadLetteredAt; }
    public List<JobAttempt> getAttempts() { return attempts; }
}
