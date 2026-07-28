package com.finassistmini.repository;

import com.finassistmini.model.ConversationSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConversationSummaryRepository extends JpaRepository<ConversationSummary, String> {

    Optional<ConversationSummary> findByConversationId(String conversationId);

    void deleteByConversationId(String conversationId);
}