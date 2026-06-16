package com.project.ieum.service.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * TMap 통합검색(POI, {@code /tmap/pois}) 기반 장소 검색. 지도 모달의 "위치/주소 검색"에서
 * 검색어를 좌표로 바꿔 지도를 이동시키는 데 쓴다(서버 프록시 — 브라우저 CORS 회피).
 *
 * <p>appKey는 지도 SDK와 동일하게 쿼리 파라미터로 전달한다(이미 검증된 인증 경로).
 * 키가 비었거나 호출이 실패하면 빈 목록을 반환해 검색 UI가 "결과 없음"으로 안전하게 처리한다.
 */
@Slf4j
@Service
public class TmapPlaceSearchService {

    private final RestClient restClient;
    private final String appKey;
    private final String poiUrl;

    public TmapPlaceSearchService(
            @Value("${tmap.app-key:}") String appKey,
            @Value("${tmap.poi-url:https://apis.openapi.sk.com/tmap/pois}") String poiUrl) {
        this.appKey = appKey;
        this.poiUrl = poiUrl;
        this.restClient = RestClient.builder().build();
    }

    public List<PlaceResult> search(String keyword) {
        if (keyword == null || keyword.isBlank() || appKey == null || appKey.isBlank()) {
            return List.of();
        }
        try {
            // appKey는 이미 인코딩된 값일 수 있어 그대로 두고(지도 SDK와 동일 경로), 검색어만 인코딩.
            String url = poiUrl
                    + "?version=1&format=json&count=5&searchKeyword="
                    + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&appKey=" + appKey;

            PoiResponse response = restClient.get()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(PoiResponse.class);

            return toResults(response);
        } catch (Exception e) {
            log.warn("[poi-search] TMap 장소 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<PlaceResult> toResults(PoiResponse response) {
        if (response == null || response.searchPoiInfo() == null
                || response.searchPoiInfo().pois() == null
                || response.searchPoiInfo().pois().poi() == null) {
            return List.of();
        }
        return response.searchPoiInfo().pois().poi().stream()
                .map(Poi::toResult)
                .filter(p -> p != null)
                .toList();
    }

    // ── TMap POI 응답 매핑(필요 필드만) ──
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PoiResponse(SearchPoiInfo searchPoiInfo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchPoiInfo(Pois pois) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Pois(List<Poi> poi) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Poi(String name, String frontLat, String frontLon, String noorLat, String noorLon) {
        PlaceResult toResult() {
            String latText = firstNonBlank(frontLat, noorLat);
            String lonText = firstNonBlank(frontLon, noorLon);
            if (latText == null || lonText == null) {
                return null;
            }
            try {
                return new PlaceResult(name, Double.parseDouble(latText), Double.parseDouble(lonText));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static String firstNonBlank(String a, String b) {
            if (a != null && !a.isBlank()) {
                return a;
            }
            return (b != null && !b.isBlank()) ? b : null;
        }
    }
}
