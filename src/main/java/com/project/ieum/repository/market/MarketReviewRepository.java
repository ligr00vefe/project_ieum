package com.project.ieum.repository.market;

import com.project.ieum.entity.market.MarketReview;
import com.project.ieum.entity.request.ReviewVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketReviewRepository extends JpaRepository<MarketReview, Long> {

    // 중복 후기 방지 — 채팅방당 후기 1건만 허용 (DB unique 제약의 애플리케이션 레벨 보완)
    boolean existsByChat_Id(Long chatId);

    // 채팅방 ID로 후기 조회 — 후기 수정 시 사용
    Optional<MarketReview> findByChat_Id(Long chatId);

    // 특정 판매자가 받은 공개 후기 목록 — 판매자 프로필 페이지에 표시
    // target.id: User PK
    @Query("""
        select r from MarketReview r
        join fetch r.author
        join fetch r.chat c
        join fetch c.post
        where r.target.id = :targetUserId
          and r.visibility = :visibility
        order by r.createdAt desc
    """)
    List<MarketReview> findByTargetWithFetch(
            @Param("targetUserId") Long targetUserId,
            @Param("visibility") ReviewVisibility visibility);

    // 판매자의 마켓 평균 별점 집계
    // coalesce: 후기가 0건일 때 null 대신 0.0 반환
    @Query("select coalesce(avg(r.rating), 0) from MarketReview r where r.target.id = :targetUserId")
    Double averageRatingByTargetUserId(@Param("targetUserId") Long targetUserId);

    // 판매자가 받은 마켓 후기 총 건수
    int countByTarget_Id(Long targetUserId);
}