package com.example.journalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class JournalEntryDto {
    private Long id;
    private String eventType;
    private Long userId;
    private String username;
    private LocalDateTime eventTimestamp;
    private String detailsJson; // Store details as JSON string
    private LocalDateTime receivedTimestamp;
}