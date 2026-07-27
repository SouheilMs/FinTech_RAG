package com.finassistmini.service;

import com.finassistmini.model.GitRepository;
import com.finassistmini.model.RepositoryFile;
import com.finassistmini.model.RepositoryKnowledge;
import com.finassistmini.repository.GitRepositoryRepository;
import com.finassistmini.repository.RepositoryFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class RepositorySummaryService {

    private static final Logger log = LoggerFactory.getLogger(RepositorySummaryService.class);
    private static final int MAX_CONTEXT_CHARS = 12_000;
    private static final int MAX_TREE_CONTEXT_CHARS =  3_000;
    private static final String OVERVIEW_HEADING = "## Overview";
    private static final String STACK_HEADING = "## Stack";

    private final ChatModel chatModel;
    private final GitRepositoryRepository repositoryRepo;
    private final RepositoryFileRepository repositoryFileRepo;
    private final RepositoryKnowledgeExtractor knowledgeExtractor;

    public RepositorySummaryService(ChatModel chatModel, GitRepositoryRepository repositoryRepo,
                                    RepositoryFileRepository repositoryFileRepo, RepositoryKnowledgeExtractor knowledgeExtractor) {
        this.chatModel = chatModel;
        this.repositoryRepo = repositoryRepo;
        this.repositoryFileRepo = repositoryFileRepo;
        this.knowledgeExtractor = knowledgeExtractor;
    }

    public String getSummary(GitRepository repository) {
        if (repository.getSummary() != null && !repository.getSummary().isBlank()) {
            log.debug("Returning cached summary for repository {}", repository.getId());
            return repository.getSummary();
        }
        return generateAndCache(repository);
    }

    public boolean hasUsableCachedSummary(GitRepository repository) {
        if (repository == null) {
            return false;
        }
        String summary = repository.getSummary();
        return summary != null && !summary.isBlank() && isCurrentFormat(summary);
    }

    public String getRepositoryFileTree(Long repositoryId) {
        List<RepositoryFile> files = repositoryFileRepo.findByRepositoryId(repositoryId);
        if (files.isEmpty()) return "(no indexed file metadata available)";

        TreeNode root = new TreeNode("");
        files.stream()
                .filter(f -> f.getPath() != null && !f.getPath().isBlank())
                .map(f -> f.getPath().replace('\\', '/'))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(root::insert);

        StringBuilder tree = new StringBuilder();
        root.render(tree, "", true);

        String result = tree.toString().trim();
        if (result.length() > MAX_TREE_CONTEXT_CHARS) {
            return result.substring(0, MAX_TREE_CONTEXT_CHARS) + "\n... (truncated)";
        }
        return result;
    }

    private String generateAndCache(GitRepository repository) {
        log.info("Generating AI summary for repository: {}", repository.getName());

        RepositoryKnowledge knowledge = knowledgeExtractor.extract(repository);
        String context = buildSummaryContext(knowledge);
        String prompt = buildSummaryPrompt(repository.getName(), repository.getUrl(), context);

        try {
            String summary = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

            String normalizedSummary = summary == null ? "" : summary.strip();
            repository.setSummary(normalizedSummary);
            repositoryRepo.save(repository);
            log.info("Summary generated and cached for repository {}", repository.getId());
            return normalizedSummary;
        } catch (Exception e) {
            log.error("Failed to generate summary for repository {}: {}", repository.getId(), e.getMessage());
            return "Summary generation failed: " + e.getMessage();
        }
    }

    private String buildSummaryContext(RepositoryKnowledge knowledge) {
        StringBuilder context = new StringBuilder();

        appendTextSection(context, "README", defaultIfBlank(knowledge.getReadme(), "Not available"));
        appendListSection(context, "Detected technologies", knowledge.getDetectedTechnologies());
        appendListSection(context, "Application entry points", knowledge.getEntryPoints());

        List<String> mainServices = new ArrayList<>();
        List<String> domainModel = new ArrayList<>();
        splitImportantClasses(knowledge.getImportantClasses(), mainServices, domainModel);

        appendListSection(context, "Main services", mainServices);
        appendListSection(context, "Domain model", domainModel);
        appendListSection(context, "Configuration", knowledge.getConfiguration());
        appendListSection(context, "Dependencies", knowledge.getDependencies());

        return truncateContext(context.toString());
    }

    private void splitImportantClasses(List<String> classes, List<String> mainServices, List<String> domainModel) {
        for (String value : classes) {
            String lower = value.toLowerCase();
            if (lower.contains("entity") || lower.contains("model") || lower.contains("dto")) {
                domainModel.add(value);
            } else {
                mainServices.add(value);
            }
        }
    }

    private void appendTextSection(StringBuilder target, String title, String content) {
        target.append(title)
                .append("\n------------------\n")
                .append(content)
                .append("\n\n");
    }

    private void appendListSection(StringBuilder target, String title, List<String> values) {
        target.append(title).append("\n------------------\n");
        if (values == null || values.isEmpty()) {
            target.append("- Not detected\n\n");
            return;
        }

        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(15)
                .forEach(value -> target.append("- ").append(value).append('\n'));

        target.append('\n');
    }

    private String truncateContext(String context) {
        if (context.length() <= MAX_CONTEXT_CHARS) {
            return context;
        }
        return context.substring(0, MAX_CONTEXT_CHARS) + "\n[...truncated]";
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String buildSummaryPrompt(String name, String url, String context) {
        return """
        You are a senior software architect reviewing a GitHub project.

        Your goal is to provide a HIGH-LEVEL overview of the application, helping a developer quickly understand what the project is about without reading the code.

        Project Name:
        %s

        Repository URL:
        %s

        Project Context:
        %s

        Produce the response using exactly these sections:

        ## Overview
        Write one short paragraph (3–5 sentences) describing:
        - What the application is.
        - Its primary purpose.
        - Who it is intended for.
        - The problem it solves.
        - The overall value it provides.

        ## Key Capabilities
        List 4–8 of the application's main capabilities or features that are explicitly supported by the provided context.

        ## Technology Stack
        Summarize only the major technologies:
        - Languages
        - Frameworks / Platforms
        - Database (if any)

        Rules:
        - Focus on the application, not the source code.
        - Explain WHAT the project does rather than HOW it is implemented.
        - Keep the explanation understandable for someone who has never seen the repository.
        - Mention technologies only if they are clearly supported by the context.
        - If some information is unavailable, simply omit it instead of guessing.

        Do NOT include:
        - package names
        - class names
        - folder structure
        - repository statistics
        - code organization
        - implementation details
        - design patterns
        - architecture diagrams
        - APIs
        - configuration files
        - dependencies list
        - indexing, embeddings, vector stores, chunking or metadata

        Keep the entire response under 200 words.
        """.formatted(name, url, context);
    }

    private boolean isCurrentFormat(String summary) {
        String value = summary == null ? "" : summary.toLowerCase();
        return value.contains(OVERVIEW_HEADING.toLowerCase()) && value.contains(STACK_HEADING.toLowerCase());
    }

    private static final class TreeNode {

        private final String                name;
        private final Map<String, TreeNode> children =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        TreeNode(String name) { this.name = name; }

        void insert(String path) {
            TreeNode current = this;
            for (String part : path.split("/")) {
                if (!part.isBlank()) {
                    current = current.children.computeIfAbsent(part, TreeNode::new);
                }
            }
        }

        void render(StringBuilder sb, String prefix, boolean isRoot) {
            if (!isRoot) {
                sb.append(name);
                if (!children.isEmpty()) sb.append('/');
                sb.append('\n');
            }

            List<Map.Entry<String, TreeNode>> entries = new ArrayList<>(children.entrySet());
            for (int i = 0; i < entries.size(); i++) {
                boolean  isLast  = (i == entries.size() - 1);
                TreeNode child   = entries.get(i).getValue();
                String   conn    = isLast ? "└── " : "├── ";
                String   cont    = isLast ? "    " : "│   ";
                sb.append(prefix).append(conn);
                child.render(sb, prefix + cont, false);
            }
        }
    }
}