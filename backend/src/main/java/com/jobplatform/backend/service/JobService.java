package com.jobplatform.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobplatform.backend.model.Job;
import com.jobplatform.backend.model.JobStatus;
import com.jobplatform.backend.repository.JobRepository;
import com.jobplatform.backend.exception.JobNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JobService {

    private static final String JOB_TOPIC = "job-events";

    private final JobRepository jobRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public JobService(JobRepository jobRepository,
                      KafkaTemplate<String, String> kafkaTemplate,
                      ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Job createJob(String name) {
        Job job = new Job(name);
        Job savedJob = jobRepository.save(job);

        publishEvent(savedJob, "CREATED");
        return savedJob;
    }

    public Job getJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + id));
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job updateJobStatus(Long id, JobStatus status) {
        Job job = getJob(id);
        job.setStatus(status);
        Job updatedJob = jobRepository.save(job);

        publishEvent(updatedJob, "STATUS_UPDATED");
        return updatedJob;
    }

    private void publishEvent(Job job, String eventType) {
        try {
            Map<String, Object> payload = Map.of(
                    "jobId", job.getId(),
                    "name", job.getName(),
                    "status", job.getStatus().name(),
                    "eventType", eventType,
                    "timestamp", System.currentTimeMillis()
            );

            String message = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(JOB_TOPIC, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to publish Kafka event", e);
        }
    }
}
