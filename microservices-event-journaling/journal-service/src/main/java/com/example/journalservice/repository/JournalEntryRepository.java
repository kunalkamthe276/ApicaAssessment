package com.example.journalservice.repository;

import com.example.journalservice.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Page<JournalEntry> findByUserId(Long userId, Pageable pageable);
    Page<JournalEntry> findByEventType(String eventType, Pageable pageable);
}