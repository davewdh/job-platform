package com.jobplatform.backend.service;

import com.jobplatform.backend.dto.JobResponseDto;
import com.jobplatform.backend.exception.JobNotFoundException;
import com.jobplatform.backend.model.*;
import com.jobplatform.backend.repository.JobRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JobService {
    private final JobRepository repository;
    private final ApplicationEventPublisher events;
    private final SimpMessagingTemplate messaging;

    public JobService(JobRepository repository, ApplicationEventPublisher events, SimpMessagingTemplate messaging) {
        this.repository = repository;
        this.events = events;
        this.messaging = messaging;
    }

    @Transactional
    public JobResponseDto createJob(String name, JobScenario scenario) {
        if (name == null || name.isBlank() || name.strip().length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a job name of 1 to 120 characters.");
        }
        return announce(repository.save(new Job(name.strip(), scenario, Instant.now())));
    }

    @Transactional(readOnly = true)
    public JobResponseDto getJob(Long id) {
        return JobResponseDto.from(repository.findById(id).orElseThrow(() -> missing(id)));
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getAllJobs() {
        return repository.findAllByOrderByIdDesc(PageRequest.of(0, 200))
                .stream().map(JobResponseDto::from).toList();
    }

    @Transactional
    public JobResponseDto requeue(Long id, JobScenario scenario) {
        Job job = repository.lockById(id).orElseThrow(() -> missing(id));
        if (job.getStatus() != JobStatus.FAILED || job.getDeadLetteredAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only jobs recorded in the dead-letter queue can be requeued.");
        }
        job.requeue(scenario, Instant.now());
        return announce(job);
    }

    @Transactional
    public boolean start(JobMessage message) {
        Job job = repository.lockById(message.jobId()).orElse(null);
        Instant now = Instant.now();
        if (job == null || !job.accepts(message.run(), message.attempt(), now)) return false;
        job.startAttempt(now);
        announce(job);
        return true;
    }

    @Transactional
    public void finish(JobMessage message) {
        repository.lockById(message.jobId()).ifPresent(job -> {
            if (job.getStatus() == JobStatus.PROCESSING && job.matches(message.run(), message.attempt())) {
                job.recordAttempt(Instant.now());
                announce(job);
            }
        });
    }

    JobResponseDto announce(Job job) {
        JobResponseDto snapshot = JobResponseDto.from(job);
        events.publishEvent(snapshot);
        return snapshot;
    }

    @TransactionalEventListener
    public void broadcast(JobResponseDto job) {
        messaging.convertAndSend("/topic/jobs", job);
    }

    private JobNotFoundException missing(Long id) {
        return new JobNotFoundException("Job not found with id: " + id);
    }
}
