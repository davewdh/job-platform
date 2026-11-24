package com.jobplatform.backend;

import com.jobplatform.backend.model.Job;
import com.jobplatform.backend.model.JobScenario;
import com.jobplatform.backend.model.JobStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JobTest {
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void succeedsImmediatelyAndRejectsDuplicateDelivery() {
        Job job = new Job("successful", JobScenario.SUCCESS, start);
        assertTrue(job.accepts(1, 1, start));
        job.startAttempt(start);
        assertEquals(JobStatus.PROCESSING, job.getStatus());
        assertFalse(job.accepts(1, 1, start));
        assertTrue(job.accepts(1, 1, start.plusSeconds(30)));
        job.recordAttempt(start);
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertFalse(job.accepts(1, 1, start));
        assertTrue(job.getAttempts().get(0).isSucceeded());
    }

    @Test
    void failsOnceAndThenSucceedsAfterBackoff() {
        Job job = new Job("retry", JobScenario.FAIL_ONCE, start);
        job.startAttempt(start);
        job.recordAttempt(start);
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertFalse(job.accepts(1, 2, start));
        assertTrue(job.accepts(1, 2, start.plusSeconds(1)));
        job.startAttempt(start.plusSeconds(1));
        job.recordAttempt(start.plusSeconds(1));
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals(1, job.getRetryCount());
        assertEquals(2, job.getAttempts().size());
    }

    @Test
    void exhaustsRetriesAndPreservesHistoryWhenRequeued() {
        Job job = new Job("dead letter", JobScenario.EXHAUST_RETRIES, start);
        int elapsed = 0;
        for (int attempt = 1; attempt <= 4; attempt++) {
            Instant now = start.plusSeconds(elapsed);
            assertTrue(job.accepts(1, attempt, now));
            job.startAttempt(now);
            job.recordAttempt(now);
            if (attempt < 4) {
                elapsed += 1 << (attempt - 1);
                assertEquals(start.plusSeconds(elapsed), job.getNextAttemptAt());
            }
        }
        assertEquals(JobStatus.FAILED, job.getStatus());
        job.markDeadLettered(start.plusSeconds(elapsed));
        assertNotNull(job.getDeadLetteredAt());
        job.requeue(JobScenario.SUCCESS, start.plusSeconds(elapsed));
        assertNull(job.getDeadLetteredAt());
        assertFalse(job.accepts(1, 1, start.plusSeconds(elapsed)));
        assertTrue(job.accepts(2, 1, start.plusSeconds(elapsed)));
        job.startAttempt(start.plusSeconds(elapsed));
        job.recordAttempt(start.plusSeconds(elapsed));
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals(5, job.getAttempts().size());
        assertEquals(2, job.getAttempts().get(4).getRunNumber());
    }
}
