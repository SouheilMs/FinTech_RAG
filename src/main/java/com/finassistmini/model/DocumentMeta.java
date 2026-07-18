package com.finassistmini.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "document_meta")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentMeta {

    @Id
    @Column(name = "document_id")
    private String documentId;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "owner_id", updatable = false)
    private String ownerId;
    @Column(name = "owner_username")
    private String ownerUsername;
    @Column(name = "file_path", nullable = false)
    private String filePath;
    @Column(name = "file_count", nullable = false)
    private int pageCount;
    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;
    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
    @Column
    private String status;
    @Column
    private String errorMessage;
}