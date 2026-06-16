package com.project.ieum.dto.search;

// 게시판 지역 필터 드롭다운 옵션 — 실제 요청에 존재하는 (시/도, 시군구) 쌍.
// HelpRequest 위치 스냅샷(sido/sigungu 문자열)에서 distinct로 뽑아 빈 결과 필터를 방지한다.
public record RegionOption(String sido, String sigungu) {}
