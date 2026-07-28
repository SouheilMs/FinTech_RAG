package com.finassistmini.repository;

import com.finassistmini.model.ConversationMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, String> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    @Query("SELECT m FROM ConversationMessage m WHERE m.conversationId = :id ORDER BY m.createdAt DESC")
    List<ConversationMessage> findRecentDesc(@Param("id") String conversationId, Pageable pageable);

    long countByConversationId(String conversationId);

    void deleteByConversationId(String conversationId);
}