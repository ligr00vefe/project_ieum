package com.project.ieum.dto.market;

import com.project.ieum.entity.market.MarketPostStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MarketPostSearchCondition {

    // 카테고리 ID (선택) — null이면 전체 카테고리 조회
    private Long categoryId;

    // 제목/본문 키워드 검색 (선택) — null 또는 빈 문자열이면 전체
    private String keyword;

    // 지역 필터 — 카카오 주소 API가 반환하는 시/도 (예: "서울특별시")
    private String sido;

    // 지역 필터 — 시/군/구 (예: "강남구")
    private String sigungu;

    // 가격 범위 필터 (선택) — null이면 해당 조건 무시
    private BigDecimal minPrice;   // 최소 가격 (이상)
    private BigDecimal maxPrice;   // 최대 가격 (이하)

    // 상태 필터 — 기본적으로 목록은 ACTIVE만 표시, null이면 전체
    // Controller에서 기본값 ACTIVE로 세팅해서 넘겨줌
    private MarketPostStatus status;
}