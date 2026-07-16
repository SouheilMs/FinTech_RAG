package com.finassistmini.repository;

import com.finassistmini.model.RepositoryIndexJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RepositoryIndexJobRepository extends JpaRepository<RepositoryIndexJob, Long> {

    List<RepositoryIndexJob> findByRepositoryIdOrderByStartedAtDesc(Long repositoryId);

    Optional<RepositoryIndexJob> findFirstByRepositoryIdOrderByStartedAtDesc(Long repositoryId);
}