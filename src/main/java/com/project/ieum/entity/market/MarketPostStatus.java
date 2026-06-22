package com.project.ieum.entity.market;

public enum MarketPostStatus {
    ACTIVE,    // 판매중 — 게시글 등록 직후 기본값
    RESERVED,  // 예약중 — 판매자가 특정 구매자와 채팅 중 예약 처리
    SOLD,      // 판매완료 — 양쪽 거래 확정 후 자동 전환
    REMOVED    // 삭제됨 — 판매자 삭제 요청 (soft delete, DB에서 실제 삭제 안 함)
}