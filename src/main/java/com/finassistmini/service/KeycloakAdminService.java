package com.finassistmini.service;

import com.finassistmini.dto.*;
import com.finassistmini.exception.UserManagementException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);
    private static final Set<String> MANAGED_ROLES = Set.of("ADMIN", "USER");
    private final Keycloak keycloak;
    private final String   realm;

    public KeycloakAdminService(
            Keycloak keycloak,
            @Value("${keycloak.admin.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm    = realm;
    }

    public List<UserResponse> findAll() {
        try {
            return realmResource().users().list()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch users from Keycloak: {}", e.getMessage());
            throw UserManagementException.keycloakUnavailable();
        }
    }

    public UserResponse findById(String id) {
        try {
            UserRepresentation rep = userResource(id).toRepresentation();
            return toResponse(rep);
        } catch (NotFoundException e) {
            throw UserManagementException.notFound(id);
        } catch (Exception e) {
            log.error("Failed to fetch user {}: {}", id, e.getMessage());
            throw UserManagementException.keycloakUnavailable();
        }
    }

    public UserResponse create(CreateUserRequest req) {
        // Check username uniqueness
        if (!realmResource().users().searchByUsername(req.username(), true).isEmpty()) {
            throw UserManagementException.usernameExists(req.username());
        }
        // Check email uniqueness
        if (!realmResource().users().searchByEmail(req.email(), true).isEmpty()) {
            throw UserManagementException.emailExists(req.email());
        }
        UserRepresentation user = new UserRepresentation();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEnabled(true);
        user.setEmailVerified(true);
        Response response;
        try {
            response = realmResource().users().create(user);
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak: {}", e.getMessage());
            throw UserManagementException.keycloakUnavailable();
        }
        if (response.getStatus() == 409) {
            throw UserManagementException.usernameExists(req.username());
        }
        if (response.getStatus() >= 400) {
            throw new UserManagementException(
                    HttpStatus.valueOf(response.getStatus()),
                    "Keycloak rejected user creation");
        }
        // Extract created user ID from Location header
        String userId = extractCreatedId(response);
        // Set password
        setPassword(userId, req.password(), false);
        // Assign default USER role
        assignRole(userId, "USER");
        log.info("User created in Keycloak: {} ({})", req.username(), userId);
        return findById(userId);
    }

    public UserResponse update(String id, UpdateUserRequest req) {
        try {
            UserRepresentation rep = userResource(id).toRepresentation();
            rep.setUsername(req.username());
            rep.setEmail(req.email());
            rep.setFirstName(req.firstName());
            rep.setLastName(req.lastName());
            rep.setEnabled(req.enabled());
            userResource(id).update(rep);
            log.info("User updated: {}", id);
            return findById(id);
        } catch (NotFoundException e) {
            throw UserManagementException.notFound(id);
        } catch (ClientErrorException e) {
            if (e.getResponse().getStatus() == 409) {
                throw UserManagementException.usernameExists(req.username());
            }
            throw UserManagementException.keycloakUnavailable();
        }
    }

    public void delete(String id) {
        try {
            Response response = realmResource().users().delete(id);
            if (response.getStatus() == 404) throw UserManagementException.notFound(id);
            log.info("User deleted from Keycloak: {}", id);
        } catch (UserManagementException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", id, e.getMessage());
            throw UserManagementException.keycloakUnavailable();
        }
    }

    public void resetPassword(String id, ResetPasswordRequest req) {
        setPassword(id, req.password(), req.temporary());
        log.info("Password reset for user: {}", id);
    }

    public void updateStatus(String id, boolean enabled) {
        try {
            UserRepresentation rep = userResource(id).toRepresentation();
            rep.setEnabled(enabled);
            userResource(id).update(rep);
            log.info("User {} {}", id, enabled ? "enabled" : "disabled");
        } catch (NotFoundException e) {
            throw UserManagementException.notFound(id);
        }
    }

    public List<String> getRoles(String id) {
        try {
            return userResource(id).roles().realmLevel().listEffective()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .filter(MANAGED_ROLES::contains)
                    .toList();
        } catch (NotFoundException e) {
            throw UserManagementException.notFound(id);
        }
    }

    public void assignRole(String userId, String roleName) {
        RoleRepresentation role = findRole(roleName);
        try {
            userResource(userId).roles().realmLevel().add(List.of(role));
            log.info("Role {} assigned to user {}", roleName, userId);
        } catch (NotFoundException e) {
            throw UserManagementException.notFound(userId);
        }
    }

    public void removeRole(String userId, String roleName) {
        RoleRepresentation role = findRole(roleName);
        try {
            userResource(userId).roles().realmLevel().remove(List.of(role));
            log.info("Role {} removed from user {}", roleName, userId);
        } catch (NotFoundException e) {
            throw UserManagementException.notFound(userId);
        }
    }

    private void setPassword(String userId, String password, boolean temporary) {
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(temporary);
        try {
            userResource(userId).resetPassword(cred);
        } catch (NotFoundException e) {
            throw UserManagementException.notFound(userId);
        } catch (Exception e) {
            log.error("Failed to set password for user {}: {}", userId, e.getMessage());
            throw new UserManagementException(HttpStatus.BAD_REQUEST,
                    "Password does not meet Keycloak policy requirements");
        }
    }

    private RoleRepresentation findRole(String roleName) {
        try {
            return realmResource().roles().get(roleName).toRepresentation();
        } catch (NotFoundException e) {
            throw UserManagementException.roleNotFound(roleName);
        }
    }

    private RealmResource realmResource() {
        return keycloak.realm(realm);
    }

    private UserResource userResource(String id) {
        return realmResource().users().get(id);
    }

    private String extractCreatedId(Response response) {
        String location = response.getHeaderString("Location");
        if (location == null) {
            throw new UserManagementException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Keycloak did not return user location");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private UserResponse toResponse(UserRepresentation rep) {
        List<String> roles;
        try {
            roles = getRoles(rep.getId());
        } catch (Exception e) {
            roles = List.of();
        }

        return new UserResponse(
                rep.getId(),
                rep.getUsername(),
                rep.getEmail(),
                rep.getFirstName(),
                rep.getLastName(),
                Boolean.TRUE.equals(rep.isEnabled()),
                roles,
                rep.getCreatedTimestamp()
        );
    }
}