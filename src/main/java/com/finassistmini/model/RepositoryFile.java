package com.finassistmini.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "repository_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private GitRepository repository;

    @Column(nullable = false, length = 1024)
    private String path;

    @Column(length = 64)
    private String language;

    @Column(length = 32)
    private String extension;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "chunk_count")
    @Builder.Default
    private Integer chunkCount = 0;

    @Column(name = "indexed")
    @Builder.Default
    private boolean indexed = false;

    @Column(name = "skipped")
    @Builder.Default
    private boolean skipped = false;

    @Column(name = "skip_reason", length = 512)
    private String skipReason;
}