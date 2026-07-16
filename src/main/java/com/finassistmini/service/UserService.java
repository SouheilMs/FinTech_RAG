package com.finassistmini.service;

import com.finassistmini.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final KeycloakAdminService keycloakAdminService;

    public List<UserResponse> getAllUsers() {
        return keycloakAdminService.findAll();
    }

    public UserResponse getUserById(String id) {
        return keycloakAdminService.findById(id);
    }

    public UserResponse createUser(CreateUserRequest req) {
        return keycloakAdminService.create(req);
    }

    public UserResponse updateUser(String id, UpdateUserRequest req) {
        return keycloakAdminService.update(id, req);
    }

    public void deleteUser(String id) {
        keycloakAdminService.delete(id);
    }

    public void resetPassword(String id, ResetPasswordRequest req) {
        keycloakAdminService.resetPassword(id, req);
    }

    public void updateStatus(String id, boolean enabled) {
        keycloakAdminService.updateStatus(id, enabled);
    }

    public List<String> getRoles(String id) {
        return keycloakAdminService.getRoles(id);
    }

    public void assignRole(String id, String roleName) {
        keycloakAdminService.assignRole(id, roleName);
    }

    public void removeRole(String id, String roleName) {
        keycloakAdminService.removeRole(id, roleName);
    }
}