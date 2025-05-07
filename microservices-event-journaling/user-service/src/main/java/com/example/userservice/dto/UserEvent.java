package com.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor
public class UserEvent {
    private String eventType;
    private Long userId;
    private String username;
    private LocalDateTime timestamp;
    private Map<String, Object> details;
}