package com.finassistmini.repository;

import com.finassistmini.model.RepositoryFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepositoryFileRepository extends JpaRepository<RepositoryFile, Long> {

    List<RepositoryFile> findByRepositoryId(Long repositoryId);

    int countByRepositoryIdAndIndexedTrue(Long repositoryId);

    void deleteByRepositoryId(Long repositoryId);
}