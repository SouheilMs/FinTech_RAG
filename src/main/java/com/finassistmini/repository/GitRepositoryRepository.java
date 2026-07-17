package com.finassistmini.repository;

import com.finassistmini.model.GitRepository;
import com.finassistmini.model.RepositoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GitRepositoryRepository extends JpaRepository<GitRepository, Long> {

    List<GitRepository> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    Optional<GitRepository> findByIdAndOwnerId(Long id, String ownerId);

    Optional<GitRepository> findByUrlAndOwnerId(String url, String ownerId);

    boolean existsByIdAndOwnerId(Long id, String ownerId);
}