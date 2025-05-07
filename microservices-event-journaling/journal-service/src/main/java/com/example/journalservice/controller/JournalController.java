package com.example.journalservice.controller;

import com.example.journalservice.dto.JournalEntryDto;
import com.example.journalservice.service.JournalQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journal")
public class JournalController {
    @Autowired
    private JournalQueryService journalQueryService;

    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN')") // Or a specific ROLE_AUDITOR
    public ResponseEntity<Page<JournalEntryDto>> getAllEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(journalQueryService.getAllJournalEntries(pageable));
    }

    @GetMapping("/events/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JournalEntryDto> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(journalQueryService.getJournalEntryById(id));
    }

    @GetMapping("/events/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<JournalEntryDto>> getEventsByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(journalQueryService.getJournalEntriesByUserId(userId, pageable));
    }
}