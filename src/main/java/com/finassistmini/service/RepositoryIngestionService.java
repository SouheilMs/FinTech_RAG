package com.finassistmini.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.finassistmini.config.RepositoryProperties;
import com.finassistmini.dto.IndexRepositoryRequest;
import com.finassistmini.dto.RepositoryDetailsResponse;
import com.finassistmini.dto.RepositoryResponse;
import com.finassistmini.dto.RepositorySummaryResponse;
import com.finassistmini.model.*;
import com.finassistmini.repository.GitRepositoryRepository;
import com.finassistmini.repository.RepositoryFileRepository;
import com.finassistmini.repository.RepositoryIndexJobRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributeView;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RepositoryIngestionService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryIngestionService.class);
    private static final String DOC_ID_PREFIX = "repo-";
    private static final int EMBEDDING_BATCH_SIZE = 20;

    private final GitUrlValidator urlValidator;
    private final GitCloneService cloneService;
    private final FileFilterService fileFilter;
    private final LanguageDetector languageDetector;
    private final CodeChunkingService chunkingService;
    private final VectorStoreService vectorStoreService;
    private final RepositorySummaryService summaryService;
    private final GitRepositoryRepository repoRepository;
    private final RepositoryFileRepository fileRepository;
    private final RepositoryIndexJobRepository jobRepository;
    private final RepositoryProperties props;

    public RepositoryIngestionService( GitUrlValidator urlValidator, GitCloneService cloneService,
            FileFilterService fileFilter, LanguageDetector languageDetector, CodeChunkingService chunkingService,
            VectorStoreService vectorStoreService, RepositorySummaryService summaryService, GitRepositoryRepository repoRepository,
            RepositoryFileRepository fileRepository, RepositoryIndexJobRepository jobRepository, RepositoryProperties props) {
        this.urlValidator = urlValidator;
        this.cloneService = cloneService;
        this.fileFilter = fileFilter;
        this.languageDetector = languageDetector;
        this.chunkingService = chunkingService;
        this.vectorStoreService = vectorStoreService;
        this.summaryService = summaryService;
        this.repoRepository = repoRepository;
        this.fileRepository = fileRepository;
        this.jobRepository = jobRepository;
        this.props = props;
    }

    @Transactional
    public RepositoryResponse submit(IndexRepositoryRequest request, String ownerId, String ownerUsername) {
        String url = request.url().trim();
        urlValidator.validate(url);

        String owner = urlValidator.extractOwner(url);
        String name  = urlValidator.extractName(url);

        // Per-owner deduplication — two users may index the same URL independently
        Optional<GitRepository> existing = repoRepository.findByUrlAndOwnerId(url, ownerId);
        if (existing.isPresent()) {
            GitRepository repo = existing.get();
            return new RepositoryResponse(repo.getId(), repo.getStatus().name());
        }

        GitRepository repo = GitRepository.builder()
                .url(url)
                .name(name)
                .owner(owner)
                .ownerId(ownerId)
                .ownerUsername(ownerUsername)
                .status(RepositoryStatus.PENDING)
                .build();

        repo = repoRepository.save(repo);
        log.info("Repository submitted for indexing: {} (id={})", url, repo.getId());
        runIndexingPipeline(repo.getId(), ownerId);
        return new RepositoryResponse(repo.getId(), repo.getStatus().name());
    }

    public List<RepositoryDetailsResponse> listAll(String ownerId) {
        return repoRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(this::toDetail)
                .toList();
    }

    public RepositoryDetailsResponse getById(Long id, String ownerId) {
        return toDetail(findOrThrow(id, ownerId));
    }

    @Transactional
    public void delete(Long id, String ownerId) {
        GitRepository repo = findOrThrow(id, ownerId);

        try {
            vectorStoreService.removeByDocumentId(DOC_ID_PREFIX + id, repo.getOwnerId());
        } catch (Exception e) {
            log.error("Failed to remove pgvector chunks for repository {}: {}", id, e.getMessage());
        }

        if (repo.getLocalPath() != null) {
            deleteDirectory(Paths.get(repo.getLocalPath()));
        }

        repoRepository.delete(repo);
        log.info("Repository {} deleted by owner '{}'", id, ownerId);
    }

    @Transactional
    public RepositoryResponse reindex(Long id, String ownerId) {
        GitRepository repo = findOrThrow(id, ownerId);

        vectorStoreService.removeByDocumentId(DOC_ID_PREFIX + id, repo.getOwnerId());

        repo.setStatus(RepositoryStatus.PENDING);
        repo.setSummary(null);
        repo.setIndexedFiles(0);
        repo.setTotalChunks(0);
        repoRepository.save(repo);

        runIndexingPipeline(id, ownerId);
        return new RepositoryResponse(id, RepositoryStatus.PENDING.name());
    }

    public RepositorySummaryResponse getSummary(Long id, String ownerId) {
        GitRepository repo = findOrThrow(id, ownerId);

        if (repo.getStatus() != RepositoryStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Repository is not fully indexed yet. Current status: " + repo.getStatus());
        }

        boolean cached = repo.getSummary() != null && !repo.getSummary().isBlank();
        String summary = summaryService.getSummary(repo);

        return new RepositorySummaryResponse(id, repo.getName(), summary, cached);
    }

    @Async("repositoryIndexExecutor")
    public void runIndexingPipeline(Long repositoryId, String ownerId) {
        GitRepository repo = findOrThrow(repositoryId, ownerId);
        RepositoryIndexJob job = RepositoryIndexJob.builder()
                .repository(repo)
                .status(JobStatus.RUNNING)
                .build();
        jobRepository.save(job);

        try {
            runIndexingPipelineInternal(repo, job);
        } catch (Exception e) {
            log.error("Indexing failed for repository {}: {}", repositoryId, e.getMessage(), e);
            markFailed(repo, job, e.getMessage());
        }
    }

    private void runIndexingPipelineInternal(GitRepository repo, RepositoryIndexJob job) throws Exception {
        // 1 ── Clone or pull
        updateStatus(repo, RepositoryStatus.CLONING);
        GitCloneService.CloneResult cloneResult = cloneService.cloneOrPull(
                repo.getUrl(), repo.getOwner() + "_" + repo.getName());

        repo.setLocalPath(cloneResult.localPath());
        repo.setBranch(cloneResult.branch());
        repo.setCommitHash(cloneResult.commitHash());
        repoRepository.save(repo);

        // 2 ── Collect eligible files
        updateStatus(repo, RepositoryStatus.INDEXING);
        Path repoRoot = Paths.get(cloneResult.localPath());
        List<FileFilterService.EligibleFile> candidates = fileFilter.collectFiles(repoRoot);

        List<FileFilterService.EligibleFile> eligible = candidates.stream()
                .filter(FileFilterService.EligibleFile::eligible)
                .toList();

        log.info("Repository {}: {} eligible files out of {} total",
                repo.getId(), eligible.size(), candidates.size());

        job.setTotalFiles(eligible.size());
        jobRepository.save(job);

        // 3 ── Persist skipped file records
        saveSkippedFiles(repo, candidates.stream()
                .filter(f -> !f.eligible()).toList());

        // 4 ── Chunk, embed, store — in batches to control memory
        int totalChunks = 0;
        int processedFiles = 0;
        List<Document> batch = new ArrayList<>();

        for (FileFilterService.EligibleFile file : eligible) {
            try {
                Path filePath = repoRoot.resolve(file.relativePath());
                String content = Files.readString(filePath, StandardCharsets.UTF_8);

                List<String> chunks = chunkingService.chunk(content, file.relativePath());
                if (chunks.isEmpty()) continue;

                String language = languageDetector.detect(file.extension());

                // Persist file record
                RepositoryFile repoFile = RepositoryFile.builder()
                        .repository(repo)
                        .path(file.relativePath())
                        .language(language)
                        .extension(file.extension())
                        .sizeBytes(file.sizeBytes())
                        .chunkCount(chunks.size())
                        .indexed(true)
                        .build();
                fileRepository.save(repoFile);

                // Build Spring AI Documents
                for (int i = 0; i < chunks.size(); i++) {
                    batch.add(buildDocument(repo, file, language, chunks.get(i), i,
                            cloneResult.commitHash(), cloneResult.branch()));
                }

                totalChunks += chunks.size();
                processedFiles++;

                // Flush batch when it reaches the threshold
                if (batch.size() >= EMBEDDING_BATCH_SIZE) {
                    vectorStoreService.addAll(batch);
                    batch.clear();
                    log.debug("Flushed embedding batch, processed {}/{} files",
                            processedFiles, eligible.size());
                }

                job.setProcessedFiles(processedFiles);
                job.setTotalChunks(totalChunks);
                jobRepository.save(job);

            } catch (IOException e) {
                log.warn("Failed to read file {}: {}", file.relativePath(), e.getMessage());
            }
        }

        // Flush remaining documents
        if (!batch.isEmpty()) {
            vectorStoreService.addAll(batch);
        }

        // 5 ── Mark repository as completed
        repo.setIndexedFiles(processedFiles);
        repo.setTotalChunks(totalChunks);
        repo.setStatus(RepositoryStatus.COMPLETED);
        repo.setIndexedAt(LocalDateTime.now());
        repoRepository.save(repo);

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);

        log.info("Repository {} indexed: {} files, {} chunks", repo.getId(), processedFiles, totalChunks);

        if (props.isSummaryEnabled()) {
            try {
                summaryService.getSummary(repo);
            } catch (Exception e) {
                log.warn("Summary generation failed for repository {}: {}", repo.getId(), e.getMessage());
            }
        }
    }

    private Document buildDocument(GitRepository repo, FileFilterService.EligibleFile file, String language,
                                   String chunkText, int chunkIndex, String commitHash, String branch) {
        String chunkId = UUID.randomUUID().toString();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ownerId",        repo.getOwnerId());
        metadata.put("documentId",     DOC_ID_PREFIX + repo.getId());
        metadata.put("documentName",   repo.getName());
        metadata.put("pageNumber",     chunkIndex + 1);
        metadata.put("chunkId",        chunkId);
        metadata.put("repositoryId",   String.valueOf(repo.getId()));
        metadata.put("repositoryName", repo.getName());
        metadata.put("owner",          repo.getOwner());
        metadata.put("filePath",       file.relativePath());
        metadata.put("language",       language);
        metadata.put("extension",      file.extension());
        metadata.put("branch",         branch);
        metadata.put("chunkIndex",     String.valueOf(chunkIndex));
        metadata.put("commitHash",     commitHash);
        metadata.put("chunkId", "repo-%d-%s-%d".formatted(repo.getId(), file.relativePath().replaceAll("[^\\w]", "_"), chunkIndex));
        metadata.put("sourceType",     "repository");

        return Document.builder()
                .id(chunkId)
                .text(chunkText)
                .metadata(metadata)
                .build();
    }

    private void saveSkippedFiles(GitRepository repo,
                                  List<FileFilterService.EligibleFile> skipped) {
        List<RepositoryFile> records = skipped.stream()
                .map(f -> RepositoryFile.builder()
                        .repository(repo)
                        .path(f.relativePath())
                        .extension(f.extension())
                        .sizeBytes(f.sizeBytes())
                        .skipped(true)
                        .skipReason(f.skipReason())
                        .build())
                .toList();
        fileRepository.saveAll(records);
    }

    private void updateStatus(GitRepository repo, RepositoryStatus status) {
        repo.setStatus(status);
        repoRepository.save(repo);
    }

    private void markFailed(GitRepository repo, RepositoryIndexJob job, String errorMessage) {
        repo.setStatus(RepositoryStatus.FAILED);
        repo.setErrorMessage(errorMessage != null && errorMessage.length() > 2048
                ? errorMessage.substring(0, 2048) : errorMessage);
        repoRepository.save(repo);

        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(repo.getErrorMessage());
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    private GitRepository findOrThrow(Long id, String ownerId) {
        return repoRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Repository not found: " + id));
    }

    private RepositoryDetailsResponse toDetail(GitRepository repo) {
        return new RepositoryDetailsResponse(
                repo.getId(),
                repo.getUrl(),
                repo.getName(),
                repo.getOwner(),
                repo.getOwnerId(),
                repo.getOwnerUsername(),
                repo.getBranch(),
                repo.getCommitHash(),
                repo.getStatus().name(),
                repo.getErrorMessage(),
                repo.getIndexedFiles(),
                repo.getTotalChunks(),
                repo.getSummary() != null && !repo.getSummary().isBlank(),
                repo.getCreatedAt(),
                repo.getIndexedAt()
        );
    }

    private void deleteDirectory(Path path) {
        if (!Files.exists(path)) return;
        releaseJGitHandles(path);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> deleteWithRetry(p, 5));
            if (Files.exists(path)) {
                log.error("Repository directory was not fully deleted: {}", path);
            } else {
                log.info("Repository directory deleted: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to walk directory {}: {}", path, e.getMessage());
        }
    }

    private void releaseJGitHandles(Path repoPath) {
        Path gitDir = repoPath.resolve(".git");
        if (!Files.exists(gitDir)) return;
        try (Git git = Git.open(repoPath.toFile())) {
            git.getRepository().getObjectDatabase().close();
            git.getRepository().close();
            log.debug("JGit repository closed for {}", repoPath);
        } catch (Exception e) {
            log.debug("Could not open/close JGit repository at {}: {}", repoPath, e.getMessage());
        }
        try {
            WindowCacheConfig cfg = new WindowCacheConfig();
            cfg.setPackedGitLimit(0);
            cfg.setPackedGitWindowSize(4096);
            cfg.setPackedGitMMAP(false);
            cfg.setDeltaBaseCacheLimit(0);
            WindowCache.reconfigure(cfg);
            log.debug("JGit WindowCache flushed");
        } catch (Exception e) {
            log.debug("Could not reconfigure JGit WindowCache: {}", e.getMessage());
        }
    }

    private void deleteWithRetry(Path path, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                DosFileAttributeView dos = Files.getFileAttributeView(
                        path, DosFileAttributeView.class);
                if (dos != null) {
                    dos.setReadOnly(false);
                }
                path.toFile().setWritable(true);
                path.toFile().setReadable(true);
                Files.delete(path);
                return;
            } catch (IOException e) {
                if (attempt == maxRetries) {
                    log.warn("Could not delete {} after {} attempts: {}",
                            path, maxRetries, e.getMessage());
                } else {
                    try {
                        Thread.sleep(200L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}