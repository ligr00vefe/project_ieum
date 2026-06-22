package com.project.ieum.repository.market;

import com.project.ieum.entity.User;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.market.MarketPostStatus;
import com.project.ieum.repository.search.MarketPostSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketPostRepository extends JpaRepository<MarketPost, Long>,
        MarketPostSearchRepository {  // QueryDSL 커스텀 검색 인터페이스 함께 상속

    // 게시글 상세 조회 — seller와 category를 한 번에 로딩 (N+1 방지)
    // @EntityGraph: LAZY 설정된 연관관계를 이 쿼리에서만 EAGER로 오버라이드
    @EntityGraph(attributePaths = {"seller", "category"})
    Optional<MarketPost> findWithDetailById(Long id);

    // 내가 등록한 게시글 목록 — REMOVED 제외, 최신순
    // findBySeller: seller 필드로 조회
    // AndStatusNot: status가 특정 값이 아닌 것
    @EntityGraph(attributePaths = {"category"})
    List<MarketPost> findBySellerAndStatusNotOrderByCreatedAtDesc(
            User seller, MarketPostStatus status);  // status에 REMOVED 전달

    // 내가 구매 채팅한 게시글 목록은 MarketChatRepository에서 조회 (buyer_id 기준)

    // 특정 상태의 게시글 수 — 관리자 통계용
    long countByStatus(MarketPostStatus status);

    // 관리자 대시보드 최근 등록 상품 5개
    @EntityGraph(attributePaths = {"seller", "category"})
    List<MarketPost> findTop5ByOrderByCreatedAtDesc();

    // 관리자 전체 목록 — seller 정보 포함, 최신순
    @Query(value = "SELECT p FROM MarketPost p LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.category ORDER BY p.createdAt DESC",
            countQuery = "SELECT count(p) FROM MarketPost p")
    Page<MarketPost> findAllPagedWithFetch(Pageable pageable);

    // 관리자 상품 필터 검색 — 상태 + 키워드 + 날짜 범위
    @Query(value = """
        select p from MarketPost p
        join fetch p.seller s
        join fetch p.category c
        where (:status is null or p.status = :status)
          and (:keyword is null
               or lower(p.title) like lower(concat('%', :keyword, '%'))
               or lower(s.email) like lower(concat('%', :keyword, '%')))
          and (:fromDate is null or p.createdAt >= :fromDate)
          and (:toDate is null or p.createdAt <= :toDate)
        order by p.createdAt desc
    """,
    countQuery = """
        select count(p) from MarketPost p
        join p.seller s
        where (:status is null or p.status = :status)
          and (:keyword is null
               or lower(p.title) like lower(concat('%', :keyword, '%'))
               or lower(s.email) like lower(concat('%', :keyword, '%')))
          and (:fromDate is null or p.createdAt >= :fromDate)
          and (:toDate is null or p.createdAt <= :toDate)
    """)
    Page<MarketPost> findAdminPosts(
            @Param("status") MarketPostStatus status,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    // 관리자 매출 조회 — SOLD 상태 게시글 날짜 범위
    @Query(value = """
        select p from MarketPost p
        join fetch p.seller s
        join fetch p.category c
        where p.status = com.project.ieum.entity.market.MarketPostStatus.SOLD
          and (:fromDate is null or p.updatedAt >= :fromDate)
          and (:toDate is null or p.updatedAt <= :toDate)
        order by p.updatedAt desc
    """,
    countQuery = """
        select count(p) from MarketPost p
        where p.status = com.project.ieum.entity.market.MarketPostStatus.SOLD
          and (:fromDate is null or p.updatedAt >= :fromDate)
          and (:toDate is null or p.updatedAt <= :toDate)
    """)
    Page<MarketPost> findSoldPostsAdmin(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}