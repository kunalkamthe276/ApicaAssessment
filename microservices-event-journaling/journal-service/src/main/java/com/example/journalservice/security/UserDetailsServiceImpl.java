package com.example.journalservice.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("journalUserDetailsService") // Optional: Naming to distinguish if it were in a shared context
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * This method is part of the UserDetailsService contract.
     * In JournalService, we don't load users from a database by username for JWT authentication.
     * The JWT itself contains the necessary information (username, roles).
     * The JwtRequestFilter will extract these and build UserDetails directly.
     * This method would only be called if Spring Security tries to authenticate
     * a user by username/password through an AuthenticationManager, which is not
     * the primary authentication mechanism for this service.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // This implementation should ideally not be hit in the normal JWT flow for Journal Service.
        // If it is, it implies a misconfiguration or an attempt to authenticate
        // via a mechanism not intended for this service.
        throw new UsernameNotFoundException("User '" + username + "' not found. JournalService authenticates via JWT claims, not direct username lookup.");
    }

    /**
     * A helper method that can be used by JwtRequestFilter to construct UserDetails
     * once username and roles are extracted from a validated JWT.
     *
     * @param username The username from the JWT.
     * @param roles    The list of role strings from the JWT.
     * @return UserDetails object.
     */
    public UserDetails buildUserDetailsFromJwt(String username, List<String> roles) {
        if (username == null || roles == null) {
            throw new IllegalArgumentException("Username or roles cannot be null when building UserDetails from JWT");
        }
        return new User(username,
                "", // Password is not relevant here as authentication is via JWT signature
                roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
    }
}