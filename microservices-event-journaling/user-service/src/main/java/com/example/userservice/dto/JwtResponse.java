package com.example.userservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor; // It's good practice to have a no-args constructor
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class JwtResponse {

    private String token;
    private String type = "Bearer";
    private String username;
    private List<String> roles;

    public JwtResponse(String accessToken, String username, Collection<? extends GrantedAuthority> authorities) {
        this.token = accessToken;
        this.username = username;
        this.roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    public JwtResponse(String token, String type, String username, List<String> roles) {
        this.token = token;
        this.type = type;
        this.username = username;
        this.roles = roles;
    }
}