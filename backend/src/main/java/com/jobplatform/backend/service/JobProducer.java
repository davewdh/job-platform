package com.jobplatform.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobplatform.backend.model.Job;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishJob(Job job) {
        try {
            String payload = objectMapper.createObjectNode()
                    .put("jobId", job.getId())
                    .put("eventType", "CREATED")
                    .toString();

            kafkaTemplate.send("job-events", payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish job to Kafka", e);
        }
    }
}
