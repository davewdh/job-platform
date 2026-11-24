package com.jobplatform.backend.model;

import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class JobAttempt {
    private int runNumber;
    private int attemptNumber;
    private boolean succeeded;
    private Instant finishedAt;

    protected JobAttempt() {}

    public JobAttempt(int runNumber, int attemptNumber, boolean succeeded, Instant finishedAt) {
        this.runNumber = runNumber;
        this.attemptNumber = attemptNumber;
        this.succeeded = succeeded;
        this.finishedAt = finishedAt;
    }

    public int getRunNumber() { return runNumber; }
    public int getAttemptNumber() { return attemptNumber; }
    public boolean isSucceeded() { return succeeded; }
    public Instant getFinishedAt() { return finishedAt; }
}
