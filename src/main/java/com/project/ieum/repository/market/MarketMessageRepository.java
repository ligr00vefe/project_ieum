package com.project.ieum.repository.market;

import com.project.ieum.entity.market.MarketMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketMessageRepository extends JpaRepository<MarketMessage, Long> {

    // 메시지 목록 (최신순) — 기존 MessageRepository와 동일한 구조
    Page<MarketMessage> findByChat_IdOrderBySentAtDesc(Long chatId, Pageable pageable);

    // 마지막 메시지 조회 — 채팅 목록 미리보기용
    java.util.Optional<MarketMessage> findTopByChat_IdOrderBySentAtDesc(Long chatId);

    // 안 읽은 메시지 수 — 채팅 목록 뱃지용
    @Query("""
        select count(m) from MarketMessage m
        where m.chat.id = :chatId
          and m.sender.id <> :userId
          and m.hasRead = false
    """)
    long countUnread(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // 읽음 일괄 처리 — 채팅방 입장 시 호출
    @Modifying(clearAutomatically = true)
    @Query("""
        update MarketMessage m set m.hasRead = true
        where m.chat.id = :chatId
          and m.sender.id <> :userId
          and m.hasRead = false
    """)
    int markRead(@Param("chatId") Long chatId, @Param("userId") Long userId);
}