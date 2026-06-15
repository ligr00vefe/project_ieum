package com.project.ieum.repository;

import com.project.ieum.entity.conversation.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
        join fetch c.application a
        join fetch a.helpRequest hr
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

    // 스케줄러 — 완료(COMPLETED)된 매칭의 아직 열린(ACTIVE) 대화방 중 희망종료+1h가 지난 것.
    // 닫으면 status가 CLOSED로 바뀌어 다음 주기에 재선택되지 않으므로 멱등하다.
    @Query("""
        select c from Conversation c
        join c.application a
        join a.helpRequest hr
        where c.status = com.project.ieum.entity.conversation.ConversationStatus.ACTIVE
          and hr.status = com.project.ieum.entity.request.HelpRequestStatus.COMPLETED
          and hr.desiredEndDatetime is not null
          and hr.desiredEndDatetime < :threshold
    """)
    List<Conversation> findActiveConversationsForCompletedRequestsEndedBefore(@Param("threshold") LocalDateTime threshold);
}
