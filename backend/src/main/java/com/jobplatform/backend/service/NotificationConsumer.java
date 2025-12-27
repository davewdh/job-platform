package com.jobplatform.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    @KafkaListener(topics = "job-events", groupId = "notification-group")
    public void consume(String message) {
        logger.info("Notification service received: {}", message);
    }
}
