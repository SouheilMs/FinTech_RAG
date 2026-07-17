package com.finassistmini.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "git_repositories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1024)
    private String url;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String owner;

    @Column(name = "owner_id", updatable = false)
    private String ownerId;

    @Column(name = "owner_username")
    private String ownerUsername;

    @Column
    private String branch;

    @Column(name = "commit_hash", length = 40)
    private String commitHash;

    @Column(name = "local_path", length = 1024)
    private String localPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepositoryStatus status;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "indexed_files")
    @Builder.Default
    private Integer indexedFiles = 0;

    @Column(name = "total_chunks")
    @Builder.Default
    private Integer totalChunks = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RepositoryFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RepositoryIndexJob> jobs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}