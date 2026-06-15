package com.project.ieum.repository.search;

import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.entity.request.HelpRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HelpRequestSearchRepository {
    Page<HelpRequest> searchHelpRequests(HelpRequestSearchCondition condition, Pageable pageable);

    // 게시판 동적 검색 — 기준 좌표(lat,lng)가 있으면 #66 거리순 정렬, 없으면 시작시각 순.
    Page<HelpRequest> searchHelpRequests(HelpRequestSearchCondition condition, Pageable pageable,
                                         Double lat, Double lng);
}
