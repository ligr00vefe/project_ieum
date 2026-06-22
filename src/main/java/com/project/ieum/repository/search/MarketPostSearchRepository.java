package com.project.ieum.repository.search;

import com.project.ieum.dto.market.MarketPostSearchCondition;
import com.project.ieum.entity.market.MarketPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MarketPostSearchRepository {

    // 기본 검색 (좌표 없음 → 최신순 정렬)
    Page<MarketPost> searchMarketPosts(MarketPostSearchCondition condition, Pageable pageable);

    // 거리 기반 검색 (좌표 있음 → 내 근처 가까운 순 정렬)
    // lat, lng: 현재 사용자 위치 또는 검색 기준 좌표
    Page<MarketPost> searchMarketPosts(MarketPostSearchCondition condition, Pageable pageable,
                                       Double lat, Double lng);
}