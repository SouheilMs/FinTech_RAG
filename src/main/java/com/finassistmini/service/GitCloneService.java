package com.finassistmini.service;

import com.finassistmini.config.RepositoryProperties;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

@Service
public class GitCloneService {

    private static final Logger log = LoggerFactory.getLogger(GitCloneService.class);

    private final RepositoryProperties props;

    public GitCloneService(RepositoryProperties props) {
        this.props = props;
    }

    public CloneResult cloneOrPull(String url, String localName) throws GitAPIException, IOException {
        Path storageRoot = Paths.get(props.getStoragePath());
        Files.createDirectories(storageRoot);

        // Sanitise folder name to prevent path traversal
        String safeName = localName.replaceAll("[^\\w.-]", "_");
        Path repoPath = storageRoot.resolve(safeName).normalize();

        // Safety check: ensure path stays within storage root
        if (!repoPath.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Resolved path escapes storage root: " + repoPath);
        }

        if (Files.exists(repoPath.resolve(".git"))) {
            log.info("Repository already exists at {} — performing git pull", repoPath);
            return pull(repoPath);
        } else {
            log.info("Cloning {} into {}", url, repoPath);
            return clone(url, repoPath);
        }
    }

    private CloneResult clone(String url, Path repoPath) throws GitAPIException, IOException {
        CloneResult result;
        try (Git git = Git.cloneRepository()
                .setURI(url)
                .setDirectory(repoPath.toFile())
                .setDepth(1)
                .setNoTags()
                .call()){
            result = buildResult(git, repoPath);
        }
        return result;
    }

    private CloneResult pull(Path repoPath) throws IOException, GitAPIException {
        CloneResult result;
        try (Git git = Git.open(repoPath.toFile())) {
            git.pull().call();
            result = buildResult(git, repoPath);
        }
        return result;
    }

    private CloneResult buildResult(Git git, Path repoPath) throws IOException {
        Repository repo = git.getRepository();
        String branch = repo.getBranch();

        Ref head = repo.findRef("HEAD");
        String commitHash = "";
        if (head != null) {
            try (RevWalk walk = new RevWalk(repo)) {
                RevCommit commit = walk.parseCommit(head.getObjectId());
                commitHash = commit.getName();
            }
        }

        return new CloneResult(repoPath.toAbsolutePath().toString(), branch, commitHash);
    }

    public record CloneResult(String localPath, String branch, String commitHash) {}
}