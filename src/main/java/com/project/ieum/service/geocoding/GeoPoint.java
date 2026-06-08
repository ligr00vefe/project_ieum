package com.project.ieum.service.geocoding;

import java.math.BigDecimal;

/**
 * 지오코딩 결과 좌표(WGS84). HelpRequest 위치 스냅샷(latitude/longitude)을 채우는 데 쓴다.
 * 거리는 매칭 게이트가 아니라 발견/근처 정렬용이다(#10, D1=i·D2=b).
 *
 * @param latitude  위도
 * @param longitude 경도
 */
public record GeoPoint(BigDecimal latitude, BigDecimal longitude) {
}
