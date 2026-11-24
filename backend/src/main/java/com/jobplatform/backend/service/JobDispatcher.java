package com.jobplatform.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobplatform.backend.config.KafkaConfig;
import com.jobplatform.backend.model.*;
import com.jobplatform.backend.repository.JobRepository;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class JobDispatcher {
    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);
    private final JobRepository repository;
    private final TransactionTemplate transactions;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final JobService jobs;

    public JobDispatcher(JobRepository repository, TransactionTemplate transactions, KafkaTemplate<String, String> kafka,
            ObjectMapper json, JobService jobs) {
        this.repository = repository;
        this.transactions = transactions;
        this.kafka = kafka;
        this.json = json;
        this.jobs = jobs;
    }

    @Scheduled(fixedDelay = 300)
    public void dispatch() {
        Instant now = Instant.now();
        for (Long id : repository.findDispatchable(now, now.minusSeconds(30), PageRequest.of(0, 25))) {
            try {
                transactions.executeWithoutResult(tx -> repository.lockById(id).ifPresent(this::publish));
            } catch (RuntimeException error) {
                log.warn("Job {} remains queued for retry: {}", id, error.getMessage());
                return;
            }
        }
    }

    private void publish(Job job) {
        Instant now = Instant.now();
        boolean deadLetter = job.getStatus() == JobStatus.FAILED && job.getDeadLetteredAt() == null;
        boolean ready = (job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.PROCESSING) && job.isDue(now) &&
                (job.getDispatchedAt() == null || !job.getDispatchedAt().isAfter(now.minusSeconds(30)));
        if (!deadLetter && !ready) return;
        String topic = deadLetter ? KafkaConfig.JOB_DLQ_TOPIC : KafkaConfig.JOB_TOPIC;
        int attempt = job.getAttemptNumber() + (deadLetter ? 0 : 1);
        try {
            String value = json.writeValueAsString(new JobMessage(job.getId(), job.getRunNumber(), attempt));
            kafka.send(topic, job.getId().toString(), value).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publishing interrupted", error);
        } catch (Exception error) {
            throw new IllegalStateException("Kafka acknowledgement unavailable", error);
        }
        if (deadLetter) {
            job.markDeadLettered(Instant.now());
            jobs.announce(job);
        } else {
            job.markDispatched(Instant.now());
        }
    }
}
