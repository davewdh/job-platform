package com.jobplatform.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
public class JobDlqService {

    private static final String DLQ_TOPIC = "job-events-dlq";
    private static final String JOB_TOPIC = "job-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobDlqService(KafkaTemplate<String, String> kafkaTemplate,
                         ConsumerFactory<String, String> consumerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
    }

    /**
     * Reprocess a job from DLQ by jobId
     */
    public boolean reprocessJob(Long jobId) {
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        TopicPartition partition = new TopicPartition(DLQ_TOPIC, 0);

        consumer.assign(Collections.singleton(partition));
        consumer.seekToBeginning(Collections.singleton(partition));

        ConsumerRecords<String, String> records =
                consumer.poll(Duration.ofSeconds(5));

        for (var record : records) {
            try {
                JsonNode node = objectMapper.readTree(record.value());
                if (node.get("jobId").asLong() == jobId) {

                    // re-publish to main topic
                    kafkaTemplate.send(JOB_TOPIC, record.value());

                    // commit offset so we don't retry same DLQ message again
                    consumer.commitSync();

                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }
}
