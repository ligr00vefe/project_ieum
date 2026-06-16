package com.project.ieum.service.geocoding;

/**
 * 장소(POI) 검색 결과 1건. 지도 모달의 위치 검색에서 검색어 → 좌표 이동에 쓴다.
 *
 * @param name 장소명
 * @param lat  위도(WGS84)
 * @param lng  경도(WGS84)
 */
public record PlaceResult(String name, double lat, double lng) {
}
