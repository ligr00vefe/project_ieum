package com.project.ieum.repository.market;

import com.project.ieum.entity.market.MarketPostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketPostImageRepository extends JpaRepository<MarketPostImage, Long> {

    // 게시글의 이미지 전체 조회 — display_order 오름차순 (0번 = 대표 이미지가 첫 번째)
    List<MarketPostImage> findByPost_IdOrderByDisplayOrderAsc(Long postId);

    // 게시글 삭제 시 해당 게시글의 이미지 전체 삭제 (Service에서 호출)
    void deleteByPost_Id(Long postId);

    // 게시글에 등록된 이미지 수 확인 — 5장 초과 여부 체크용
    int countByPost_Id(Long postId);
}