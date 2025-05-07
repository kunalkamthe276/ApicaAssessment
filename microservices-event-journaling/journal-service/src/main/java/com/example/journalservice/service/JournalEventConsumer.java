// com/example/journalservice/service/JournalEventConsumer.java
package com.example.journalservice.service;

import com.example.journalservice.dto.UserEvent;
import com.example.journalservice.entity.JournalEntry;
import com.example.journalservice.repository.JournalEntryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class JournalEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(JournalEventConsumer.class);
    private static final String TOPIC_NAME = "user-events";
    private static final String GROUP_ID = "journal-group";

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private ObjectMapper objectMapper; // For converting Map to JSON string

    @KafkaListener(topics = TOPIC_NAME, groupId = GROUP_ID,
            containerFactory = "userEventKafkaListenerContainerFactory") // Reference the factory
    public void consumeUserEvent(UserEvent event) {
        logger.info("Received event from Kafka: {}", event);
        try {
            String detailsJson = null;
            if (event.getDetails() != null) {
                detailsJson = objectMapper.writeValueAsString(event.getDetails());
            }

            JournalEntry entry = new JournalEntry(
                    event.getEventType(),
                    event.getUserId(),
                    event.getUsername(),
                    event.getTimestamp(),
                    detailsJson
            );
            journalEntryRepository.save(entry);
            logger.info("Persisted journal entry: {}", entry.getId());
        } catch (JsonProcessingException e) {
            logger.error("Error serializing event details to JSON: {}", event.getDetails(), e);
            // Decide how to handle: DLQ, log and skip, etc.
        } catch (Exception e) {
            logger.error("Error processing consumed event: {}", event, e);
            // Potentially rethrow to trigger Kafka error handling / retry
        }
    }
}