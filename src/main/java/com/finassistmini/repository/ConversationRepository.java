package com.finassistmini.repository;

import com.finassistmini.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    @Query("SELECT c FROM Conversation c WHERE c.ownerId = :ownerId " +
            "ORDER BY c.pinned DESC, COALESCE(c.lastMessageAt, c.createdAt) DESC")
    List<Conversation> findByOwnerOrdered(@Param("ownerId") String ownerId);

    @Query("SELECT c FROM Conversation c WHERE c.ownerId = :ownerId " +
            "AND LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "ORDER BY c.pinned DESC, COALESCE(c.lastMessageAt, c.createdAt) DESC")
    List<Conversation> searchByTitle(@Param("ownerId") String ownerId, @Param("q") String q);

    Optional<Conversation> findByIdAndOwnerId(String id, String ownerId);
    boolean existsByIdAndOwnerId(String id, String ownerId);
}