package com.project.ieum.repository;

import com.project.ieum.entity.conversation.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
        select c
        from Conversation c
        join fetch c.requester r
        join fetch r.user ru
        join fetch c.caregiver cg
        join fetch cg.user cu
        where c.id = :id
    """)
    Optional<Conversation> findWithParticipantsById(@Param("id") Long id);

    @Query(value = """
        select c
        from Conversation c
        join fetch c.requester r
        join fetch r.user ru
        join fetch c.caregiver cg
        join fetch cg.user cu
        where r.userId = :userId or cg.userId = :userId
        order by c.lastMessageAt desc, c.id desc
    """,
    countQuery = """
        select count(c)
        from Conversation c
        where c.requester.userId = :userId or c.caregiver.userId = :userId
    """)
    Page<Conversation> findMyConversations(@Param("userId") Long userId, Pageable pageable);

    Optional<Conversation> findByApplication_Id(Long applicationId);

    List<Conversation> findByApplication_IdIn(List<Long> applicationIds);
}
