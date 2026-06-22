package com.project.ieum.repository.market;

import com.project.ieum.entity.User;
import com.project.ieum.entity.market.MarketChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketChatRepository extends JpaRepository<MarketChat, Long> {

    // 중복 채팅방 방지 — 이미 채팅방이 있으면 새로 만들지 않고 기존 채팅방으로 이동
    // Service의 openOrGet()에서 사용: 있으면 반환, 없으면 신규 생성
    Optional<MarketChat> findByPost_IdAndBuyer(Long postId, User buyer);

    // 채팅방 상세 — post, seller, buyer 정보를 한 번에 로딩 (채팅방 입장 시 사용)
    @Query("""
        select c from MarketChat c
        join fetch c.post p
        join fetch p.category
        join fetch c.seller
        join fetch c.buyer
        where c.id = :id
    """)
    Optional<MarketChat> findWithDetailById(@Param("id") Long id);

    // 내 채팅 목록 — 판매자 또는 구매자로 참여한 채팅방, 최근 메시지 순 정렬
    // seller.id, buyer.id: User 엔티티의 PK
    @Query(value = """
        select c from MarketChat c
        join fetch c.post p
        join fetch c.seller s
        join fetch c.buyer b
        where s.id = :userId or b.id = :userId
        order by c.lastMessageAt desc, c.id desc
    """,
            countQuery = """
        select count(c) from MarketChat c
        where c.seller.id = :userId or c.buyer.id = :userId
    """)
    Page<MarketChat> findMyChats(@Param("userId") Long userId, Pageable pageable);

    // 특정 게시글의 채팅방 수 — 관심 구매자 수 표시용 (선택 기능)
    int countByPost_Id(Long postId);
}