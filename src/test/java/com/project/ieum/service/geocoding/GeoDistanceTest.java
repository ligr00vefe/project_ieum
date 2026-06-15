package com.project.ieum.service.geocoding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GeoDistance} 단위 검증 — 하버사인 거리와 프라이버시 근사 라벨.
 */
class GeoDistanceTest {

    @Test
    @DisplayName("서울시청↔강남역 직선 거리는 약 8~10km")
    void haversineKm_seoulCityHallToGangnam() {
        // 서울시청(37.5665, 126.9780) ↔ 강남역(37.4979, 127.0276) — 실제 직선거리 약 8.8km
        double km = GeoDistance.haversineKm(37.5665, 126.9780, 37.4979, 127.0276);

        assertThat(km).isBetween(8.0, 10.0);
    }

    @Test
    @DisplayName("같은 좌표면 거리 0")
    void haversineKm_samePoint_isZero() {
        double km = GeoDistance.haversineKm(37.5665, 126.9780, 37.5665, 126.9780);

        assertThat(km).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("근사 라벨 — 0.5km 버킷으로 반올림")
    void approxLabel_roundsToHalfKm() {
        assertThat(GeoDistance.approxLabel(2.3)).isEqualTo("약 2.5km"); // 2.3 → 2.5
        assertThat(GeoDistance.approxLabel(2.1)).isEqualTo("약 2km");   // 2.1 → 2.0(정수는 소수점 없이)
        assertThat(GeoDistance.approxLabel(3.0)).isEqualTo("약 3km");   // 정수는 소수점 없이
        assertThat(GeoDistance.approxLabel(0.75)).isEqualTo("약 1km");  // 0.75 → 1.0
    }

    @Test
    @DisplayName("근사 라벨 — 250m 미만은 '500m 이내'")
    void approxLabel_veryClose_showsWithin500m() {
        assertThat(GeoDistance.approxLabel(0.1)).isEqualTo("500m 이내");
        assertThat(GeoDistance.approxLabel(0.24)).isEqualTo("500m 이내");
        // 0.25 이상은 버킷팅 시작
        assertThat(GeoDistance.approxLabel(0.25)).isEqualTo("약 0.5km");
    }
}
