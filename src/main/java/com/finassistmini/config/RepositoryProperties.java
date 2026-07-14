package com.finassistmini.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "repository")
@Data
public class RepositoryProperties {

    private String storagePath = "storage/repositories";

    private long maxFileSizeBytes = 1_048_576;

    private int maxFilesPerRepo = 2000;

    private int maxDepth = 20;

    private int cloneTimeoutSeconds = 120;

    private boolean summaryEnabled = true;

    private boolean asyncEnabled = true;

    private List<String> allowedExtensions = List.of(
            "java","kt","py","js","ts","go","rs","c","cpp","cs",
            "yaml","yml","json","xml","md","properties","sql",
            "gradle","kts","dockerfile","txt"
    );

    private List<String> ignoredDirectories = List.of(
            ".git","node_modules","target","build","dist","coverage",
            ".idea",".vscode","bin","obj","vendor","logs","tmp",
            "generated","__pycache__",".gradle",".mvn"
    );
}