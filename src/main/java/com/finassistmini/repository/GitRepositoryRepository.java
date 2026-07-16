package com.finassistmini.repository;

import com.finassistmini.model.GitRepository;
import com.finassistmini.model.RepositoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GitRepositoryRepository extends JpaRepository<GitRepository, Long> {

    Optional<GitRepository> findByUrl(String url);

    List<GitRepository> findAllByOrderByCreatedAtDesc();

    List<GitRepository> findByStatus(RepositoryStatus status);

    boolean existsByUrl(String url);
}