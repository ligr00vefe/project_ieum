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

    // 관리자 전체 목록 — seller 정보 포함, 최신순
    @Query(value = "SELECT p FROM MarketPost p LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.category ORDER BY p.createdAt DESC",
            countQuery = "SELECT count(p) FROM MarketPost p")
    Page<MarketPost> findAllPagedWithFetch(Pageable pageable);
}