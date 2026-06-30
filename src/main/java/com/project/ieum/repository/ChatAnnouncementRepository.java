package com.project.ieum.repository;

import com.project.ieum.entity.conversation.ChatAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatAnnouncementRepository extends JpaRepository<ChatAnnouncement, Long> {
    Optional<ChatAnnouncement> findByConversationId(Long conversationId);
    void deleteByConversationId(Long conversationId);
}
