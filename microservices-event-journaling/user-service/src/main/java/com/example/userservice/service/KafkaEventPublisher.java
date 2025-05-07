package com.example.userservice.service;

import com.example.userservice.dto.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC_NAME = "user-events";

    @Autowired
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void publishUserEvent(UserEvent event) {
        try {
            // Use userId as key for partitioning if desired, or null for round-robin
            String key = event.getUserId() != null ? event.getUserId().toString() : null;
            kafkaTemplate.send(TOPIC_NAME, key, event);
            logger.info("Published event to {}: {}", TOPIC_NAME, event);
        } catch (Exception e) {
            logger.error("Error publishing event to Kafka: {}", event, e);
            // Handle exception (e.g., retry, dead-letter queue)
        }
    }
}