package com.example.journalservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_entries")
@Getter @Setter @NoArgsConstructor
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;
    private Long userId;
    private String username;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime eventTimestamp;

    @Lob
    private String detailsJson;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime receivedTimestamp;

    public JournalEntry(String eventType, Long userId, String username, LocalDateTime eventTimestamp, String detailsJson) {
        this.eventType = eventType;
        this.userId = userId;
        this.username = username;
        this.eventTimestamp = eventTimestamp;
        this.detailsJson = detailsJson;
        this.receivedTimestamp = LocalDateTime.now();
    }
}