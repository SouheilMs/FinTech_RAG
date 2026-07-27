package com.finassistmini.service;

import com.finassistmini.model.GitRepository;
import com.finassistmini.model.RepositoryKnowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class RepositoryKnowledgeExtractor {

    private static final Logger log = LoggerFactory.getLogger(RepositoryKnowledgeExtractor.class);

    private static final int MAX_README_CHARS = 6_000;
    private static final int MAX_IMPORTANT_FILES = 20;
    private static final int MAX_IMPORTANT_FILE_LINES = 150;
    private static final int MAX_METHOD_SIGNATURES = 4;
    private static final int MAX_ENTRY_POINTS = 25;
    private static final int MAX_DEPENDENCIES = 40;
    private static final int MAX_CONFIGURATION_ITEMS = 25;

    private static final List<String> README_PRIORITY = List.of("README.md", "README.txt", "README.rst");

    private static final List<String> CONFIG_FILES = List.of(
            "pom.xml",
            "build.gradle",
            "build.gradle.kts",
            "package.json",
            "Cargo.toml",
            "go.mod",
            "requirements.txt",
            "setup.py",
            "composer.json",
            "Dockerfile",
            "docker-compose.yml",
            "AndroidManifest.xml"
    );

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            "target", "build", "node_modules", ".git", ".idea", ".vscode"
    );

    private static final Set<String> IMPORTANT_NAME_KEYWORDS = Set.of(
            "controller", "service", "manager", "repository", "activity", "fragment",
            "viewmodel", "handler", "usecase", "entity", "model", "dto"
    );

    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            ".java", ".kt", ".py", ".js", ".ts", ".tsx", ".go"
    );

    private static final Pattern POM_DEPENDENCY_PATTERN = Pattern.compile(
            "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>(?:\\s*<version>([^<]+)</version>)?.*?</dependency>",
            Pattern.DOTALL
    );

    private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile(
            "^(public|protected|private|static|\\s)+[\\w<>,\\[\\]\\.?]+\\s+\\w+\\s*\\([^;]*\\)\\s*(\\{|throws).*"
    );

    public RepositoryKnowledge extract(GitRepository repository) {
        if (repository == null || repository.getLocalPath() == null || repository.getLocalPath().isBlank()) {
            return RepositoryKnowledge.builder().build();
        }

        Path repoRoot = Paths.get(repository.getLocalPath());
        if (!Files.exists(repoRoot) || !Files.isDirectory(repoRoot)) {
            return RepositoryKnowledge.builder().build();
        }

        String readme = extractReadme(repoRoot);
        List<Path> configFiles = findConfigurationFiles(repoRoot);
        List<String> dependencies = extractDependencies(configFiles);
        List<String> entryPoints = detectEntryPoints(repoRoot);
        List<String> importantClasses = extractImportantClasses(repoRoot);
        List<String> configuration = summarizeConfiguration(configFiles, repoRoot);
        List<String> technologies = detectTechnologies(readme, configFiles, dependencies, entryPoints, importantClasses);

        return RepositoryKnowledge.builder()
                .readme(readme)
                .dependencies(dependencies)
                .entryPoints(entryPoints)
                .importantClasses(importantClasses)
                .configuration(configuration)
                .detectedTechnologies(technologies)
                .build();
    }

    private String extractReadme(Path repoRoot) {
        for (String fileName : README_PRIORITY) {
            Path readmePath = repoRoot.resolve(fileName);
            if (!Files.exists(readmePath) || !Files.isRegularFile(readmePath)) {
                continue;
            }
            try {
                String content = Files.readString(readmePath, StandardCharsets.UTF_8);
                return truncate(content, MAX_README_CHARS);
            } catch (IOException e) {
                log.debug("Failed to read README {}: {}", readmePath, e.getMessage());
            }
        }
        return "";
    }

    private List<Path> findConfigurationFiles(Path repoRoot) {
        List<Path> files = new ArrayList<>();
        for (String name : CONFIG_FILES) {
            Path directPath = repoRoot.resolve(name);
            if (Files.exists(directPath) && Files.isRegularFile(directPath)) {
                files.add(directPath);
            }
        }

        Path androidManifestPath = repoRoot.resolve("app/src/main/AndroidManifest.xml");
        if (Files.exists(androidManifestPath) && Files.isRegularFile(androidManifestPath)) {
            files.add(androidManifestPath);
        }

        return files.stream()
                .distinct()
                .sorted(Comparator.comparing(path -> repoRoot.relativize(path).toString().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<String> extractDependencies(List<Path> configFiles) {
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        for (Path configFile : configFiles) {
            String fileName = configFile.getFileName().toString();
            try {
                String content = Files.readString(configFile, StandardCharsets.UTF_8);
                dependencies.addAll(extractDependenciesFromConfig(fileName, content));
            } catch (IOException e) {
                log.debug("Failed to read configuration file {}: {}", configFile, e.getMessage());
            }

            if (dependencies.size() >= MAX_DEPENDENCIES) {
                break;
            }
        }
        return dependencies.stream().limit(MAX_DEPENDENCIES).toList();
    }

    private List<String> extractDependenciesFromConfig(String fileName, String content) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if ("pom.xml".equals(lowerName)) {
            return extractPomDependencies(content);
        }
        if ("build.gradle".equals(lowerName) || "build.gradle.kts".equals(lowerName)) {
            return extractGradleDependencies(content);
        }
        if ("package.json".equals(lowerName) || "composer.json".equals(lowerName)) {
            return extractQuotedDependencies(content);
        }
        if ("requirements.txt".equals(lowerName)) {
            return extractLineDependencies(content);
        }
        if ("cargo.toml".equals(lowerName)) {
            return extractCargoDependencies(content);
        }
        if ("go.mod".equals(lowerName)) {
            return extractGoDependencies(content);
        }
        if ("setup.py".equals(lowerName)) {
            return extractSetupPyDependencies(content);
        }
        return List.of();
    }

    private List<String> extractPomDependencies(String content) {
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        Matcher matcher = POM_DEPENDENCY_PATTERN.matcher(content);
        while (matcher.find() && dependencies.size() < MAX_DEPENDENCIES) {
            String group = matcher.group(1).trim();
            String artifact = matcher.group(2).trim();
            String version = matcher.group(3) != null ? matcher.group(3).trim() : "";
            String value = version.isBlank() ? group + ":" + artifact : group + ":" + artifact + ":" + version;
            dependencies.add(value);
        }
        return dependencies.stream().toList();
    }

    private List<String> extractGradleDependencies(String content) {
        Pattern pattern = Pattern.compile("(?:implementation|api|compileOnly|runtimeOnly|testImplementation|kapt)\\s*[('\\\"]([^'\\\")]+)['\\\"]");
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find() && dependencies.size() < MAX_DEPENDENCIES) {
            dependencies.add(matcher.group(1).trim());
        }
        return dependencies.stream().toList();
    }

    private List<String> extractQuotedDependencies(String content) {
        Pattern pattern = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find() && dependencies.size() < MAX_DEPENDENCIES) {
            String name = matcher.group(1).trim();
            if (List.of("name", "version", "description", "author", "license", "scripts").contains(name)) {
                continue;
            }
            dependencies.add(name + "@" + matcher.group(2).trim());
        }
        return dependencies.stream().toList();
    }

    private List<String> extractLineDependencies(String content) {
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .limit(MAX_DEPENDENCIES)
                .toList();
    }

    private List<String> extractCargoDependencies(String content) {
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        boolean inDependencies = false;

        for (String line : content.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[")) {
                inDependencies = "[dependencies]".equalsIgnoreCase(trimmed);
                continue;
            }
            if (inDependencies && trimmed.contains("=")) {
                dependencies.add(trimmed);
            }
            if (dependencies.size() >= MAX_DEPENDENCIES) {
                break;
            }
        }
        return dependencies.stream().toList();
    }

    private List<String> extractGoDependencies(String content) {
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("^\\s*([\\w./-]+)\\s+v[\\w.+-]+", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find() && dependencies.size() < MAX_DEPENDENCIES) {
            dependencies.add(matcher.group(0).trim());
        }
        return dependencies.stream().toList();
    }

    private List<String> extractSetupPyDependencies(String content) {
        Pattern pattern = Pattern.compile("install_requires\\s*=\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return List.of();
        }
        return matcher.group(1).lines()
                .map(String::trim)
                .map(line -> line.replace(",", "").replace("\"", "").replace("'", "").trim())
                .filter(line -> !line.isBlank())
                .limit(MAX_DEPENDENCIES)
                .toList();
    }

    private List<String> detectEntryPoints(Path repoRoot) {
        try (Stream<Path> files = Files.walk(repoRoot)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> !isIgnoredPath(repoRoot.relativize(path)))
                    .filter(path -> isEntryPoint(path.getFileName().toString()))
                    .map(path -> repoRoot.relativize(path).toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .limit(MAX_ENTRY_POINTS)
                    .toList();
        } catch (IOException e) {
            log.debug("Failed to detect entry points in {}: {}", repoRoot, e.getMessage());
            return List.of();
        }
    }

    private boolean isEntryPoint(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith("application.java")
                || lower.equals("main.java")
                || lower.equals("mainactivity.java")
                || lower.equals("androidmanifest.xml")
                || lower.equals("main.py")
                || lower.equals("app.py")
                || lower.equals("index.js")
                || lower.equals("server.js")
                || lower.equals("app.js")
                || lower.equals("main.go");
    }

    private List<String> extractImportantClasses(Path repoRoot) {
        try (Stream<Path> files = Files.walk(repoRoot)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> !isIgnoredPath(repoRoot.relativize(path)))
                    .filter(this::isSourceFile)
                    .filter(this::isImportantFileName)
                    .sorted(Comparator.comparing(path -> repoRoot.relativize(path).toString().toLowerCase(Locale.ROOT)))
                    .limit(MAX_IMPORTANT_FILES)
                    .map(path -> summarizeImportantFile(repoRoot, path))
                    .filter(summary -> !summary.isBlank())
                    .toList();
        } catch (IOException e) {
            log.debug("Failed to extract important classes in {}: {}", repoRoot, e.getMessage());
            return List.of();
        }
    }

    private boolean isSourceFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return SOURCE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private boolean isImportantFileName(Path path) {
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return IMPORTANT_NAME_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private String summarizeImportantFile(Path repoRoot, Path file) {
        List<String> lines;
        try (Stream<String> stream = Files.lines(file, StandardCharsets.UTF_8)) {
            lines = stream.limit(MAX_IMPORTANT_FILE_LINES).toList();
        } catch (IOException e) {
            log.debug("Failed to read source file {}: {}", file, e.getMessage());
            return "";
        }

        List<String> summaryLines = new ArrayList<>();
        summaryLines.add("File: " + repoRoot.relativize(file));
        appendClassJavadoc(summaryLines, lines);
        appendPackageAndImports(summaryLines, lines);
        appendAnnotations(summaryLines, lines);
        appendClassDeclaration(summaryLines, lines);
        appendMethodSignatures(summaryLines, lines);

        return String.join("\n", summaryLines);
    }

    private void appendClassJavadoc(List<String> target, List<String> lines) {
        int classLine = findClassDeclarationLine(lines);
        if (classLine < 0) {
            return;
        }

        int start = -1;
        for (int i = classLine - 1; i >= 0; i--) {
            String line = lines.get(i).trim();
            if (line.startsWith("/**")) {
                start = i;
                break;
            }
            if (!line.startsWith("*") && !line.startsWith("*/") && !line.isBlank()) {
                break;
            }
        }

        if (start < 0) {
            return;
        }

        target.add("Javadoc:");
        for (int i = start; i < classLine; i++) {
            String line = lines.get(i).trim();
            if (!line.isBlank()) {
                target.add(line);
            }
        }
    }

    private void appendPackageAndImports(List<String> target, List<String> lines) {
        lines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("package ") || line.startsWith("import "))
                .forEach(target::add);
    }

    private void appendAnnotations(List<String> target, List<String> lines) {
        lines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("@"))
                .limit(8)
                .forEach(target::add);
    }

    private void appendClassDeclaration(List<String> target, List<String> lines) {
        int classLine = findClassDeclarationLine(lines);
        if (classLine < 0) {
            return;
        }

        target.add("Class declaration:");
        for (int i = classLine; i < lines.size() && i < classLine + 6; i++) {
            String line = lines.get(i).trim();
            if (line.isBlank()) {
                continue;
            }
            target.add(line);
            if (line.contains("{")) {
                break;
            }
        }
    }

    private int findClassDeclarationLine(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains(" class ") || line.contains(" interface ") || line.contains(" enum ") || line.contains(" record ")) {
                return i;
            }
        }
        return -1;
    }

    private void appendMethodSignatures(List<String> target, List<String> lines) {
        int methods = 0;
        target.add("Methods:");
        for (String line : lines) {
            String trimmed = line.trim();
            if (METHOD_SIGNATURE_PATTERN.matcher(trimmed).matches()) {
                target.add(trimmed);
                methods++;
                if (methods >= MAX_METHOD_SIGNATURES) {
                    break;
                }
            }
        }
    }

    private List<String> summarizeConfiguration(List<Path> configFiles, Path repoRoot) {
        return configFiles.stream()
                .map(path -> summarizeConfigurationFile(repoRoot, path))
                .filter(value -> !value.isBlank())
                .limit(MAX_CONFIGURATION_ITEMS)
                .toList();
    }

    private String summarizeConfigurationFile(Path repoRoot, Path file) {
        List<String> lines;
        try (Stream<String> stream = Files.lines(file, StandardCharsets.UTF_8)) {
            lines = stream
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .limit(15)
                    .toList();
        } catch (IOException e) {
            log.debug("Failed to summarize configuration file {}: {}", file, e.getMessage());
            return "";
        }

        return repoRoot.relativize(file) + " -> " + String.join(" | ", lines);
    }

    private List<String> detectTechnologies(String readme,
                                            List<Path> configFiles,
                                            List<String> dependencies,
                                            List<String> entryPoints,
                                            List<String> importantClasses) {
        String combined = (
                readme + "\n"
                        + String.join("\n", dependencies) + "\n"
                        + String.join("\n", entryPoints) + "\n"
                        + String.join("\n", importantClasses)
        ).toLowerCase(Locale.ROOT);

        LinkedHashSet<String> technologies = new LinkedHashSet<>();

        if (containsAnyConfig(configFiles, "pom.xml", "build.gradle", "build.gradle.kts") || combined.contains("spring") || combined.contains("@springbootapplication")) {
            technologies.add("Spring Boot");
        }
        if (combined.contains("android") || containsAnyConfig(configFiles, "androidmanifest.xml")) {
            technologies.add("Android SDK");
        }
        if (containsJavaFiles(entryPoints, importantClasses)) {
            technologies.add("Java");
        }
        if (combined.contains("postgres") || combined.contains("jdbc") || combined.contains("jpa")) {
            technologies.add("PostgreSQL");
        }
        if (combined.contains("hibernate")) {
            technologies.add("Hibernate");
        }
        if (containsAnyConfig(configFiles, "package.json") || containsAny(entryPoints, ".js")) {
            technologies.add("Node.js");
        }
        if (containsAny(entryPoints, ".py") || combined.contains("django") || combined.contains("flask") || combined.contains("fastapi")) {
            technologies.add("Python");
        }
        if (containsAny(entryPoints, ".go") || containsAnyConfig(configFiles, "go.mod")) {
            technologies.add("Go");
        }
        if (containsAnyConfig(configFiles, "dockerfile", "docker-compose.yml")) {
            technologies.add("Docker");
        }

        return technologies.stream().toList();
    }

    private boolean containsJavaFiles(List<String> entryPoints, List<String> importantClasses) {
        return containsAny(entryPoints, ".java")
                || importantClasses.stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(".java"));
    }

    private boolean containsAnyConfig(List<Path> configFiles, String... fileNames) {
        Set<String> targets = Stream.of(fileNames)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        return configFiles.stream()
                .map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
                .anyMatch(targets::contains);
    }

    private boolean containsAny(List<String> values, String marker) {
        return values.stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(marker.toLowerCase(Locale.ROOT)));
    }

    private boolean isIgnoredPath(Path relativePath) {
        for (Path part : relativePath) {
            if (IGNORED_DIRECTORIES.contains(part.toString().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "\n[...truncated]";
    }
}