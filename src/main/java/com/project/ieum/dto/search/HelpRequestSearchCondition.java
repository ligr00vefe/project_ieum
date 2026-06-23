package com.project.ieum.dto.search;

import com.project.ieum.entity.request.HelpRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class HelpRequestSearchCondition {
    private Long regionId;
    private Long serviceCategoryId; // 단일 — 기존 /api/search 호환용
    private LocalDate fromDate;
    private LocalDate toDate;
    private HelpRequestStatus status;

    // 게시판 동적 검색용 추가 조건 — 제목/본문 키워드, 위치 스냅샷 기반 지역 필터(시/도·시군구).
    private String keyword;
    private String sido;
    private String sigungu;

    // 게시판 다중 선택 — 서비스 카테고리 여러 개(IN). 비어 있으면 단일 serviceCategoryId로 폴백.
    private List<Long> serviceCategoryIds;

    // 이용자 둘러보기 보드 — 설정되면 "OPEN(남의 글) 또는 본인 글" 범위로 조회한다(status 단일 필터 대체).
    // null이면 무영향 → /api/search·caregiver 보드 등 기존 경로는 회귀 0.
    private Long ownerScopeUserId;
}
