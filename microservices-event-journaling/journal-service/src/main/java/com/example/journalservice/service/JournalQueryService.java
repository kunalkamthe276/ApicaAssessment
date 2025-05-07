package com.example.journalservice.service;

import com.example.journalservice.dto.JournalEntryDto;
import com.example.journalservice.entity.JournalEntry;
import com.example.journalservice.repository.JournalEntryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class JournalQueryService {
    @Autowired
    private JournalEntryRepository journalEntryRepository;
    @Autowired
    private ObjectMapper objectMapper;

    public Page<JournalEntryDto> getAllJournalEntries(Pageable pageable) {
        return journalEntryRepository.findAll(pageable).map(this::mapToDto);
    }

    public JournalEntryDto getJournalEntryById(Long id) {
        return journalEntryRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Journal entry not found: " + id));
    }

    public Page<JournalEntryDto> getJournalEntriesByUserId(Long userId, Pageable pageable) {
        return journalEntryRepository.findByUserId(userId, pageable).map(this::mapToDto);
    }

    private JournalEntryDto mapToDto(JournalEntry entry) {
        return new JournalEntryDto(
                entry.getId(),
                entry.getEventType(),
                entry.getUserId(),
                entry.getUsername(),
                entry.getEventTimestamp(),
                entry.getDetailsJson(),
                entry.getReceivedTimestamp()
        );
    }
}