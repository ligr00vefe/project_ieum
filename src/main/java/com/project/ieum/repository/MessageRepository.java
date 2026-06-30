package com.project.ieum.repository;

import com.project.ieum.entity.conversation.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderBySentAtDesc(Long conversationId, Pageable pageable);

    Optional<Message> findTopByConversationIdOrderBySentAtDesc(Long conversationId);

    @Query("""
        select count(m)
        from Message m
        where m.conversation.id = :conversationId
          and m.sender.id <> :userId
          and m.hasRead = false
    """)
    long countUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
        update Message m
        set m.hasRead = true
        where m.conversation.id = :conversationId
          and m.sender.id <> :userId
          and m.hasRead = false
    """)
    int markRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Message m where m.conversation.id = :conversationId")
    void deleteByConversationId(@Param("conversationId") Long conversationId);
}
