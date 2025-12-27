package com.jobplatform.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobplatform.backend.model.Job;
import com.jobplatform.backend.model.JobStatus;
import com.jobplatform.backend.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JobConsumer {

    private static final Logger logger = LoggerFactory.getLogger(JobConsumer.class);
    private static final int MAX_RETRIES = 3;

    private final JobRepository jobRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DLQ_TOPIC = "job-events-dlq";
    private static final String JOB_TOPIC = "job-events";

    public JobConsumer(JobRepository jobRepository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       SimpMessagingTemplate messagingTemplate) {
        this.jobRepository = jobRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = JOB_TOPIC, groupId = "job-group")
    public void consume(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            Long jobId = node.get("jobId").asLong();
            String eventType = node.get("eventType").asText();
            int retryCount = node.has("retryCount") ? node.get("retryCount").asInt() : 0;

            Optional<Job> optionalJob = jobRepository.findById(jobId);
            if (optionalJob.isEmpty()) {
                logger.warn("Job not found for ID: {}", jobId);
                return;
            }

            if ("CREATED".equals(eventType)) {
                processJob(optionalJob.get(), retryCount);
            }

        } catch (Exception e) {
            logger.error("Failed to process Kafka message: {}", message, e);
            kafkaTemplate.send(DLQ_TOPIC, message);
        }
    }

    private void processJob(Job job, int retryCount) {
        try {
            job.setStatus(JobStatus.PROCESSING);
            job.setUpdatedAt(java.time.LocalDateTime.now());
            jobRepository.save(job);
            messagingTemplate.convertAndSend("/topic/jobs", job);

            Thread.sleep(2000); // simulate processing

            // Simulated failure for demo
            if (Math.random() < 0.7) {
                throw new RuntimeException("Simulated failure");
            }

            job.setStatus(JobStatus.COMPLETED);
            job.setUpdatedAt(java.time.LocalDateTime.now());
            jobRepository.save(job);
            messagingTemplate.convertAndSend("/topic/jobs", job);
            logger.info("Job processed successfully: {}", job.getId());

        } catch (Exception e) {
            logger.error("Job processing failed: {}, retryCount={}", job.getId(), retryCount, e);

            job.setRetryCount(retryCount);
            jobRepository.save(job);

            if (retryCount < MAX_RETRIES) {
                logger.info("Retrying job {} after failure", job.getId());
                sendToKafka(job, retryCount + 1);
            } else {
                job.setStatus(JobStatus.FAILED);
                job.setUpdatedAt(java.time.LocalDateTime.now());
                jobRepository.save(job);
                messagingTemplate.convertAndSend("/topic/jobs", job);
                kafkaTemplate.send(DLQ_TOPIC, serializeJob(job, retryCount));
            }
        }
    }

    private void sendToKafka(Job job, int retryCount) {
        kafkaTemplate.send(JOB_TOPIC, serializeJob(job, retryCount));
    }

    private String serializeJob(Job job, int retryCount) {
        try {
            return objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("jobId", job.getId())
                            .put("eventType", "CREATED")
                            .put("retryCount", retryCount)
            );
        } catch (Exception e) {
            logger.error("Failed to serialize job for Kafka: {}", job.getId(), e);
            return "{}";
        }
    }
}
