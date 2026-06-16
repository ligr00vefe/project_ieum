package com.project.ieum.service.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * TMap 통합검색(POI, {@code /tmap/pois}) 기반 장소 검색. 지도 모달의 "위치/주소 검색"에서
 * 검색어를 좌표로 바꿔 지도를 이동시키는 데 쓴다(서버 프록시 — 브라우저 CORS 회피).
 *
 * <p>appKey는 {@link TmapGeocodingService}와 동일하게 {@code appKey} 헤더로 전달한다(인코딩 모호성 제거).
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
        // 외부 호출이 응답하지 않을 때 요청 스레드가 무한 대기하지 않도록 연결·읽기 타임아웃을 둔다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public List<PlaceResult> search(String keyword) {
        if (keyword == null || keyword.isBlank() || appKey == null || appKey.isBlank()) {
            return List.of();
        }
        try {
            // 검색어는 UriComponentsBuilder가 인코딩하고, appKey는 헤더로 전달(쿼리 인코딩 모호성 제거).
            URI uri = UriComponentsBuilder.fromUriString(poiUrl)
                    .queryParam("version", 1)
                    .queryParam("format", "json")
                    .queryParam("count", 5)
                    .queryParam("searchKeyword", keyword)
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            PoiResponse response = restClient.get()
                    .uri(uri)
                    .header("appKey", appKey)
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
