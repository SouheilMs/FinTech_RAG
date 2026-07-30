package com.finassistmini.service;

import com.finassistmini.dto.ConversationResponse;
import com.finassistmini.model.Conversation;
import com.finassistmini.repository.ConversationMessageRepository;
import com.finassistmini.repository.ConversationRepository;
import com.finassistmini.repository.ConversationSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository repo;
    private final ConversationMessageRepository messageRepo;
    private final ConversationSummaryRepository summaryRepo;
    private final ConversationTitleService titleService;

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(String ownerId, String search) {
        List<Conversation> results = (search != null && !search.isBlank())
                ? repo.searchByTitle(ownerId, search)
                : repo.findByOwnerOrdered(ownerId);
        return results.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ConversationResponse create(String ownerId, String firstMessage) {
        Conversation c = Conversation.builder()
                .ownerId(ownerId)
                .title(titleService.generateTitle(firstMessage))
                .pinned(false)
                .build();
        ConversationResponse response = toResponse(repo.save(c));
        log.info("Conversation created: {} for owner '{}'", response.id(), ownerId);
        return response;
    }

    @Transactional(readOnly = true)
    public ConversationResponse getById(String id, String ownerId) {
        return toResponse(findOrThrow(id, ownerId));
    }

    @Transactional
    public ConversationResponse rename(String id, String ownerId, String title) {
        Conversation c = findOrThrow(id, ownerId);
        c.setTitle(title.strip());
        return toResponse(repo.save(c));
    }

    @Transactional
    public ConversationResponse setPin(String id, String ownerId, boolean pinned) {
        Conversation c = findOrThrow(id, ownerId);
        c.setPinned(pinned);
        return toResponse(repo.save(c));
    }

    @Transactional
    public void delete(String id, String ownerId) {
        Conversation c = findOrThrow(id, ownerId);
        messageRepo.deleteByConversationId(id);
        summaryRepo.deleteByConversationId(id);
        repo.delete(c);
        log.info("Conversation {} deleted by owner '{}'", id, ownerId);
    }

    @Transactional
    public void updateLastMessage(String id) {
        repo.findById(id).ifPresent(c -> {
            c.setLastMessageAt(LocalDateTime.now());
            repo.save(c);
        });
    }

    /** Package-accessible — used by ConversationController to verify ownership. */
    public Conversation findOrThrow(String id, String ownerId) {
        return repo.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conversation not found: " + id));
    }

    private ConversationResponse toResponse(Conversation c) {
        return new ConversationResponse(
                c.getId(), c.getTitle(), c.isPinned(),
                c.getCreatedAt(), c.getLastMessageAt());
    }
}