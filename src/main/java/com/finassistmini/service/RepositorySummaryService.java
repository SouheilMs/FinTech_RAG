package com.finassistmini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finassistmini.model.GitRepository;
import com.finassistmini.repository.GitRepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class RepositorySummaryService {

    private static final Logger log = LoggerFactory.getLogger(RepositorySummaryService.class);
    private static final int MAX_CONTEXT_CHARS = 8_000;
    private static final List<String> PRIORITY_FILES = List.of(
            "README.md", "README.rst", "README.txt", "readme.md",
            "pom.xml", "build.gradle", "build.gradle.kts", "package.json",
            "Cargo.toml", "go.mod", "requirements.txt", "setup.py",
            "docker-compose.yml", "docker-compose.yaml", "Dockerfile"
    );

    private final ChatModel                chatModel;
    private final GitRepositoryRepository  repositoryRepo;

    public RepositorySummaryService(ChatModel chatModel,
                                    GitRepositoryRepository repositoryRepo ) {
        this.chatModel      = chatModel;
        this.repositoryRepo = repositoryRepo;
    }

    public String getSummary(GitRepository repository) {
        if (repository.getSummary() != null && !repository.getSummary().isBlank()) {
            log.debug("Returning cached summary for repository {}", repository.getId());
            return repository.getSummary();
        }
        return generateAndCache(repository);
    }

    private String generateAndCache(GitRepository repository)  {
        log.info("Generating AI summary for repository: {}", repository.getName());

        String context = buildSummaryContext(repository);
        String prompt  = buildSummaryPrompt(repository.getName(), repository.getUrl(), context);

        try {
            String summary = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

            repository.setSummary(summary);
            repositoryRepo.save(repository);
            log.info("Summary generated and cached for repository {}", repository.getId());
            return summary;
        } catch (Exception e) {
            log.error("Failed to generate summary for repository {}: {}",
                    repository.getId(), e.getMessage());
            return "Summary generation failed: " + e.getMessage();
        }
    }

    private String buildSummaryContext(GitRepository repository) {
        Path repoRoot = Paths.get(repository.getLocalPath());
        StringBuilder context = new StringBuilder();

        for (String fileName : PRIORITY_FILES) {
            Path candidate = repoRoot.resolve(fileName);
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                try {
                    String content = Files.readString(candidate, StandardCharsets.UTF_8);
                    int limit = Math.min(content.length(), 2000);
                    context.append("=== ").append(fileName).append(" ===\n");
                    context.append(content, 0, limit);
                    if (content.length() > limit) context.append("\n[...truncated]");
                    context.append("\n\n");

                    if (context.length() >= MAX_CONTEXT_CHARS) break;
                } catch (IOException e) {
                    log.debug("Could not read priority file {}: {}", candidate, e.getMessage());
                }
            }
        }

        // If we have very little context, sample source files
        if (context.length() < 1000) {
            context.append(sampleSourceFiles(repoRoot));
        }

        return context.toString();
    }

    private String sampleSourceFiles(Path repoRoot) {
        StringBuilder sb = new StringBuilder();
        List<Path> sourceFiles = new ArrayList<>();

        try {
            Files.walk(repoRoot, 4)
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".java") || name.endsWith(".py")
                                || name.endsWith(".ts") || name.endsWith(".go");
                    })
                    .limit(5)
                    .forEach(sourceFiles::add);

            for (Path file : sourceFiles) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                int limit = Math.min(content.length(), 800);
                sb.append("=== ").append(repoRoot.relativize(file)).append(" ===\n");
                sb.append(content, 0, limit).append("\n\n");
            }
        } catch (IOException e) {
            log.debug("Error sampling source files: {}", e.getMessage());
        }

        return sb.toString();
    }

    private String buildSummaryPrompt(String name, String url, String context) {
        return """
                You are a senior software architect. Analyze this Git repository and produce a comprehensive technical summary.

                Repository: %s
                URL: %s

                Available files and content:
                %s

                Generate a structured summary covering:
                1. Project purpose and problem it solves
                2. Architecture and design patterns used
                3. Main technologies and frameworks
                4. Module and package structure
                5. Build tool and configuration
                6. Authentication and security approach (if any)
                7. Database and persistence strategy (if any)
                8. Public API and entry points
                9. Key classes and their responsibilities
                10. Notable design decisions and observations
                11. External dependencies
                12. Folder structure overview

                Be precise, technical, and concise. Use bullet points where appropriate.
                """.formatted(name, url, context);
    }
}