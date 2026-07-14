package com.finassistmini.service;

import com.finassistmini.config.RepositoryProperties;
import com.finassistmini.dto.IndexRepositoryRequest;
import com.finassistmini.dto.RepositoryDetailsResponse;
import com.finassistmini.dto.RepositoryResponse;
import com.finassistmini.dto.RepositorySummaryResponse;
import com.finassistmini.model.*;
import com.finassistmini.repository.GitRepositoryRepository;
import com.finassistmini.repository.RepositoryFileRepository;
import com.finassistmini.repository.RepositoryIndexJobRepository;
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
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RepositoryIngestionService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryIngestionService.class);

    // Prefix stored as `documentId` in pgvector metadata.
    // Allows removeByDocumentId() to delete all chunks for a repository.
    private static final String DOC_ID_PREFIX = "repo-";

    private static final int EMBEDDING_BATCH_SIZE = 20;

    private final GitUrlValidator          urlValidator;
    private final GitCloneService          cloneService;
    private final FileFilterService        fileFilter;
    private final LanguageDetector         languageDetector;
    private final CodeChunkingService      chunkingService;
    private final VectorStoreService       vectorStoreService;
    private final RepositorySummaryService summaryService;
    private final GitRepositoryRepository  repoRepository;
    private final RepositoryFileRepository fileRepository;
    private final RepositoryIndexJobRepository jobRepository;
    private final RepositoryProperties     props;

    public RepositoryIngestionService(
            GitUrlValidator urlValidator,
            GitCloneService cloneService,
            FileFilterService fileFilter,
            LanguageDetector languageDetector,
            CodeChunkingService chunkingService,
            VectorStoreService vectorStoreService,
            RepositorySummaryService summaryService,
            GitRepositoryRepository repoRepository,
            RepositoryFileRepository fileRepository,
            RepositoryIndexJobRepository jobRepository,
            RepositoryProperties props) {
        this.urlValidator      = urlValidator;
        this.cloneService      = cloneService;
        this.fileFilter        = fileFilter;
        this.languageDetector  = languageDetector;
        this.chunkingService   = chunkingService;
        this.vectorStoreService = vectorStoreService;
        this.summaryService    = summaryService;
        this.repoRepository    = repoRepository;
        this.fileRepository    = fileRepository;
        this.jobRepository     = jobRepository;
        this.props             = props;
    }

    @Transactional
    public RepositoryResponse submit(IndexRepositoryRequest request) {
        String url = request.url().trim();
        urlValidator.validate(url);

        String owner = urlValidator.extractOwner(url);
        String name  = urlValidator.extractName(url);

        // If already submitted, return the existing record
        Optional<GitRepository> existing = repoRepository.findByUrl(url);
        if (existing.isPresent()) {
            GitRepository repo = existing.get();
            return new RepositoryResponse(repo.getId(), repo.getStatus().name());
        }

        GitRepository repo = GitRepository.builder()
                .url(url)
                .name(name)
                .owner(owner)
                .status(RepositoryStatus.PENDING)
                .build();

        repo = repoRepository.save(repo);
        log.info("Repository submitted for indexing: {} (id={})", url, repo.getId());

        indexAsync(repo.getId());

        return new RepositoryResponse(repo.getId(), repo.getStatus().name());
    }

    public List<RepositoryDetailsResponse> listAll() {
        return repoRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDetail)
                .toList();
    }

    public RepositoryDetailsResponse getById(Long id) {
        return toDetail(findOrThrow(id));
    }

    @Transactional
    public void delete(Long id) {
        GitRepository repo = findOrThrow(id);

        // Remove all embeddings from pgvector
        try {
            vectorStoreService.removeByDocumentId(DOC_ID_PREFIX + id);
            log.info("Removed pgvector chunks for repository {}", id);
        } catch (Exception e) {
            log.error("Failed to remove pgvector chunks for repository {}: {}", id, e.getMessage());
        }

        // Delete local clone
        if (repo.getLocalPath() != null) {
            deleteDirectory(Paths.get(repo.getLocalPath()));
        }

        repoRepository.delete(repo);
        log.info("Repository {} deleted", id);
    }

    @Transactional
    public RepositoryResponse reindex(Long id) {
        GitRepository repo = findOrThrow(id);

        // Remove old embeddings
        vectorStoreService.removeByDocumentId(DOC_ID_PREFIX + id);

        // Delete old file records
        fileRepository.deleteByRepositoryId(id);

        repo.setStatus(RepositoryStatus.PENDING);
        repo.setSummary(null);
        repo.setIndexedFiles(0);
        repo.setTotalChunks(0);
        repoRepository.save(repo);

        indexAsync(id);
        return new RepositoryResponse(id, RepositoryStatus.PENDING.name());
    }

    public RepositorySummaryResponse getSummary(Long id) {
        GitRepository repo = findOrThrow(id);

        if (repo.getStatus() != RepositoryStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Repository is not fully indexed yet. Current status: " + repo.getStatus());
        }

        boolean cached = repo.getSummary() != null && !repo.getSummary().isBlank();
        String summary = summaryService.getSummary(repo);

        return new RepositorySummaryResponse(id, repo.getName(), summary, cached);
    }

    @Async("repositoryIndexExecutor")
    public void indexAsync(Long repositoryId) {
        GitRepository repo = findOrThrow(repositoryId);
        RepositoryIndexJob job = RepositoryIndexJob.builder()
                .repository(repo)
                .status(JobStatus.RUNNING)
                .build();
        jobRepository.save(job);

        try {
            runIndexingPipeline(repo, job);
        } catch (Exception e) {
            log.error("Indexing failed for repository {}: {}", repositoryId, e.getMessage(), e);
            markFailed(repo, job, e.getMessage());
        }
    }

    private void runIndexingPipeline(GitRepository repo, RepositoryIndexJob job) throws Exception {
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

        // 6 ── Generate summary asynchronously (non-blocking, best effort)
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
        String chunkId = "repo-%d-%s-%d".formatted(repo.getId(),
                file.relativePath().replaceAll("[^\\w]", "_"), chunkIndex);

        Map<String, Object> metadata = new HashMap<>();
        // documentId = "repo-{id}" — allows removeByDocumentId() to delete all chunks
        metadata.put("documentId",      DOC_ID_PREFIX + repo.getId());
        metadata.put("documentName",    repo.getName());
        metadata.put("pageNumber",      chunkIndex + 1);     // reused by SourceReference
        metadata.put("chunkId",         chunkId);
        // Repository-specific metadata
        metadata.put("repositoryId",    String.valueOf(repo.getId()));
        metadata.put("repositoryName",  repo.getName());
        metadata.put("owner",           repo.getOwner());
        metadata.put("filePath",        file.relativePath());
        metadata.put("language",        language);
        metadata.put("extension",       file.extension());
        metadata.put("branch",          branch);
        metadata.put("chunkIndex",      String.valueOf(chunkIndex));
        metadata.put("commitHash",      commitHash);
        metadata.put("sourceType",      "repository");       // distinguishes from PDFs

        return Document.builder()
                .id(UUID.randomUUID().toString())
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

    private GitRepository findOrThrow(Long id) {
        return repoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Repository not found with id: " + id));
    }

    private RepositoryDetailsResponse toDetail(GitRepository repo) {
        return new RepositoryDetailsResponse(
                repo.getId(),
                repo.getUrl(),
                repo.getName(),
                repo.getOwner(),
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
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); }
                        catch (IOException e) { log.warn("Could not delete {}", p); }
                    });
        } catch (IOException e) {
            log.error("Failed to delete directory {}: {}", path, e.getMessage());
        }
    }
}