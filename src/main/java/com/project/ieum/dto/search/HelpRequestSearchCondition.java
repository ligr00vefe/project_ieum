package com.project.ieum.dto.search;

import com.project.ieum.entity.request.HelpRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HelpRequestSearchCondition {
    private Long regionId;
    private Long serviceCategoryId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private HelpRequestStatus status;

    // 게시판 동적 검색용 추가 조건 — 제목/본문 키워드, 위치 스냅샷 기반 지역 필터(시/도·시군구).
    private String keyword;
    private String sido;
    private String sigungu;
}
