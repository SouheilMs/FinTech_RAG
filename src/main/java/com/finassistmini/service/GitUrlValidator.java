package com.finassistmini.service;

import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class GitUrlValidator {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "github.com",
            "gitlab.com",
            "bitbucket.org"
    );

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\.");
    private static final Pattern VALID_REPO_PATH = Pattern.compile("^/[\\w.-]+/[\\w.-]+(\\.git)?/?$");

    public void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Repository URL must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed repository URL: " + url);
        }

        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(
                    "Only HTTPS URLs are accepted. Received scheme: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || !ALLOWED_HOSTS.contains(host.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported Git provider. Allowed: GitHub, GitLab, Bitbucket. Received host: " + host);
        }

        String path = uri.getPath();
        if (PATH_TRAVERSAL.matcher(path).find()) {
            throw new IllegalArgumentException("URL contains illegal path traversal sequence");
        }

        if (!VALID_REPO_PATH.matcher(path).matches()) {
            throw new IllegalArgumentException(
                    "URL path does not match expected owner/repository format: " + path);
        }
    }

    public String extractOwner(String url) {
        String path = URI.create(url).getPath();
        String[] segments = path.split("/");
        return segments.length > 1 ? segments[1] : "unknown";
    }

    public String extractName(String url) {
        String path = URI.create(url).getPath();
        String[] segments = path.split("/");
        String name = segments.length > 2 ? segments[2] : "unknown";
        return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }
}