package com.example.userservice.service;
// ... imports ...
import com.example.userservice.dto.UserDto;
import com.example.userservice.dto.UserEvent;
import com.example.userservice.dto.UserRegistrationDto;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.exception.UserNotFoundException; // Create this
import com.example.userservice.repository.RoleRepository;
import com.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public UserDto registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User(
                registrationDto.getUsername(),
                registrationDto.getEmail(),
                passwordEncoder.encode(registrationDto.getPassword())
        );
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER"))); // Create if not exists
        user.setRoles(Collections.singleton(userRole));
        User savedUser = userRepository.save(user);

        // Publish event
        UserEvent event = new UserEvent("USER_CREATED", savedUser.getId(), savedUser.getUsername(), LocalDateTime.now(),
                Map.of("email", savedUser.getEmail(), "roles", savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
        );
        kafkaEventPublisher.publishUserEvent(event);

        return mapToUserDto(savedUser);
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return mapToUserDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) { // Assume UserDto has updatable fields
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        // Update fields (e.g., email) - DO NOT update password here directly
        boolean changed = false;
        if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
            user.setEmail(userDto.getEmail());
            changed = true;
        }
        // ... other updatable fields

        User updatedUser = userRepository.save(user);

        if (changed) { // Only publish if something actually changed
            UserEvent event = new UserEvent("USER_UPDATED", updatedUser.getId(), updatedUser.getUsername(), LocalDateTime.now(),
                    Map.of("updatedFields", Map.of("email", updatedUser.getEmail())) // Example details
            );
            kafkaEventPublisher.publishUserEvent(event);
        }
        return mapToUserDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        userRepository.delete(user);

        UserEvent event = new UserEvent("USER_DELETED", user.getId(), user.getUsername(), LocalDateTime.now(), null);
        kafkaEventPublisher.publishUserEvent(event);
    }

    @Transactional
    public UserDto assignRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        user.getRoles().add(role);
        User updatedUser = userRepository.save(user);

        UserEvent event = new UserEvent("ROLE_ASSIGNED", updatedUser.getId(), updatedUser.getUsername(), LocalDateTime.now(),
                Map.of("assignedRole", roleName)
        );
        kafkaEventPublisher.publishUserEvent(event);
        return mapToUserDto(updatedUser);
    }

    @Transactional
    public UserDto removeRoleFromUser(Long userId, String roleName) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        boolean removed = user.getRoles().remove(role);
        if (!removed) {
            throw new IllegalArgumentException("User does not have role: " + roleName);
        }
        User updatedUser = userRepository.save(user);

        UserEvent event = new UserEvent("ROLE_REMOVED", updatedUser.getId(), updatedUser.getUsername(), LocalDateTime.now(),
                Map.of("removedRole", roleName)
        );
        kafkaEventPublisher.publishUserEvent(event);
        return mapToUserDto(updatedUser);
    }


    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto(); // Define UserDto with id, username, email, roles
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return dto;
    }
}