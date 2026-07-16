package com.finassistmini.service;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class LanguageDetector {

    private static final Map<String, String> EXTENSION_MAP = Map.ofEntries(
            Map.entry("java",       "Java"),
            Map.entry("kt",         "Kotlin"),
            Map.entry("kts",        "Kotlin"),
            Map.entry("py",         "Python"),
            Map.entry("js",         "JavaScript"),
            Map.entry("ts",         "TypeScript"),
            Map.entry("go",         "Go"),
            Map.entry("rs",         "Rust"),
            Map.entry("c",          "C"),
            Map.entry("cpp",        "C++"),
            Map.entry("cs",         "C#"),
            Map.entry("yaml",       "YAML"),
            Map.entry("yml",        "YAML"),
            Map.entry("json",       "JSON"),
            Map.entry("xml",        "XML"),
            Map.entry("md",         "Markdown"),
            Map.entry("properties", "Properties"),
            Map.entry("sql",        "SQL"),
            Map.entry("gradle",     "Gradle"),
            Map.entry("dockerfile", "Dockerfile"),
            Map.entry("txt",        "Text")
    );

    public String detect(String extension) {
        if (extension == null) return "Unknown";
        return EXTENSION_MAP.getOrDefault(extension.toLowerCase(), "Unknown");
    }
}