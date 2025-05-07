package com.example.userservice.controller;
// ... imports ...
import com.example.userservice.dto.UserDto;
import com.example.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired private UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Or just @PreAuthorize("isAuthenticated()") if any logged-in user can see
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isSelf(#id, principal.username)") // Example of custom access
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
        // @securityService is a custom bean you'd create for complex checks
        // For simplicity here, let's assume only ADMIN can update others.
        // If user can update self: ensure userDto doesn't try to change roles etc.
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> assignRole(@PathVariable Long userId, @PathVariable String roleName) {
        return ResponseEntity.ok(userService.assignRoleToUser(userId, roleName));
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> removeRole(@PathVariable Long userId, @PathVariable String roleName) {
        return ResponseEntity.ok(userService.removeRoleFromUser(userId, roleName));
    }
}