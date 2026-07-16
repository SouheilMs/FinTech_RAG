package com.finassistmini.web;

import com.finassistmini.dto.*;
import com.finassistmini.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String id,
            @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(id, req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable String id,
            @RequestBody UpdateStatusRequest req) {
        userService.updateStatus(id, req.enabled());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<List<String>> getRoles(@PathVariable String id) {
        return ResponseEntity.ok(userService.getRoles(id));
    }

    @PostMapping("/{id}/roles/{roleName}")
    public ResponseEntity<Void> assignRole(
            @PathVariable String id,
            @PathVariable String roleName) {
        userService.assignRole(id, roleName);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<Void> removeRole(
            @PathVariable String id,
            @PathVariable String roleName) {
        userService.removeRole(id, roleName);
        return ResponseEntity.noContent().build();
    }
}