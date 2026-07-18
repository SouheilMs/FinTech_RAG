package com.finassistmini.repository;

import com.finassistmini.model.DocumentMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentMeta, String> {

    List<DocumentMeta> findAllByOwnerId(String ownerId);

    Optional<DocumentMeta> findByDocumentIdAndOwnerId(String documentId, String ownerId);

    boolean existsByDocumentId(String documentId);
}