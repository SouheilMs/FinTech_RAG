package com.finassistmini.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserManagementException extends ResponseStatusException {

    public UserManagementException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public static UserManagementException usernameExists(String username) {
        return new UserManagementException(HttpStatus.CONFLICT,
                "Username already exists: " + username);
    }

    public static UserManagementException emailExists(String email) {
        return new UserManagementException(HttpStatus.CONFLICT,
                "Email already registered: " + email);
    }

    public static UserManagementException notFound(String id) {
        return new UserManagementException(HttpStatus.NOT_FOUND,
                "User not found: " + id);
    }

    public static UserManagementException roleNotFound(String role) {
        return new UserManagementException(HttpStatus.NOT_FOUND,
                "Role not found: " + role);
    }

    public static UserManagementException keycloakUnavailable() {
        return new UserManagementException(HttpStatus.SERVICE_UNAVAILABLE,
                "Identity service is temporarily unavailable");
    }
}