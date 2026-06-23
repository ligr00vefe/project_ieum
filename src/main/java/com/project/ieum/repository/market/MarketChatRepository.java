package com.project.ieum.repository.market;

import com.project.ieum.entity.User;
import com.project.ieum.entity.market.MarketChat;
import com.project.ieum.entity.market.MarketPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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
    // seller.id, buyer.id: User 엔티티의 PK — 삭제된 상품(REMOVED) 제외
    @Query(value = """
        select c from MarketChat c
        join fetch c.post p
        join fetch c.seller s
        join fetch c.buyer b
        where (s.id = :userId or b.id = :userId)
          and p.status <> com.project.ieum.entity.market.MarketPostStatus.REMOVED
        order by c.lastMessageAt desc, c.id desc
    """,
            countQuery = """
        select count(c) from MarketChat c
        where (c.seller.id = :userId or c.buyer.id = :userId)
          and c.post.status <> com.project.ieum.entity.market.MarketPostStatus.REMOVED
    """)
    Page<MarketChat> findMyChats(@Param("userId") Long userId, Pageable pageable);

    // 내가 구매자인 채팅방 목록 (마이페이지 구매내역용) — 삭제된 상품 제외
    @Query("""
        select c from MarketChat c
        join fetch c.post p
        join fetch c.seller s
        join fetch c.buyer b
        where b.id = :userId
          and p.status <> com.project.ieum.entity.market.MarketPostStatus.REMOVED
        order by c.lastMessageAt desc, c.id desc
    """)
    List<MarketChat> findByBuyerId(@Param("userId") Long userId);

    // 특정 게시글의 채팅방 수 — 관심 구매자 수 표시용 (선택 기능)
    int countByPost_Id(Long postId);

    // 특정 게시글의 채팅방 목록 — 상품 삭제 시 일괄 종료용
    List<MarketChat> findByPost_Id(Long postId);

    // 관리자 상품 상세 — 해당 게시글의 채팅 목록 (buyer 정보 포함)
    @Query("""
        select c from MarketChat c
        join fetch c.buyer b
        where c.post.id = :postId
        order by c.createdAt desc
    """)
    List<MarketChat> findByPostIdForAdmin(@Param("postId") Long postId);

    // 관리자 채팅 목록 — 상품 상태 필터 + 키워드 + 날짜 범위
    @Query(value = """
        select c from MarketChat c
        join fetch c.post p
        join fetch c.seller s
        join fetch c.buyer b
        where (:postStatus is null or p.status = :postStatus)
          and (:keyword is null
               or lower(p.title) like lower(concat('%', :keyword, '%'))
               or lower(s.email) like lower(concat('%', :keyword, '%'))
               or lower(b.email) like lower(concat('%', :keyword, '%')))
          and (:fromDate is null or c.createdAt >= :fromDate)
          and (:toDate is null or c.createdAt <= :toDate)
        order by c.createdAt desc
    """,
    countQuery = """
        select count(c) from MarketChat c
        join c.post p
        join c.seller s
        join c.buyer b
        where (:postStatus is null or p.status = :postStatus)
          and (:keyword is null
               or lower(p.title) like lower(concat('%', :keyword, '%'))
               or lower(s.email) like lower(concat('%', :keyword, '%'))
               or lower(b.email) like lower(concat('%', :keyword, '%')))
          and (:fromDate is null or c.createdAt >= :fromDate)
          and (:toDate is null or c.createdAt <= :toDate)
    """)
    Page<MarketChat> findAdminChats(
            @Param("postStatus") MarketPostStatus postStatus,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}