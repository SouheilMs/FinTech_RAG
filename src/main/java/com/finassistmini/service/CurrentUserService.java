package com.finassistmini.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public String getUserId() {
        return jwt().getSubject();
    }

    public String getUsername() {
        String username = jwt().getClaimAsString("preferred_username");
        return username != null ? username : jwt().getSubject();
    }

    public String getEmail() {
        String email = jwt().getClaimAsString("email");
        return email != null ? email : "";
    }

    private Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new IllegalStateException(
                "No authenticated JWT found in SecurityContext. " +
                        "CurrentUserService must only be called from authenticated request threads.");
    }
}