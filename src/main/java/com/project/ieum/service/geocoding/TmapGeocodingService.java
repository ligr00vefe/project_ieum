package com.project.ieum.service.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * TMap full-text geocoding({@code /tmap/geo/fullAddrGeo}) 기반 {@link GeocodingService} 구현.
 *
 * <p>도로명주소를 입력받아 좌표(WGS84)로 변환한다. TMap 책임은 좌표 변환만이며, 구조화된 주소
 * 구성요소(우편번호·법정동코드 등)는 상위(생성 폼)의 주소검색 위젯이 채운다.
 *
 * <p>응답 {@code coordinateInfo.coordinate[0]}에서 도로명 중심 좌표({@code newLat/newLon})를 쓰고,
 * 비어 있으면 지번 중심({@code lat/lon})으로 폴백한다. 좌표는 발견/정렬용이라, 키 미설정·미해석·
 * 호출 실패 시 {@link Optional#empty()}를 반환해 호출부가 좌표 미확보로 안전하게 동작하도록 한다.
 */
@Slf4j
@Service
public class TmapGeocodingService implements GeocodingService {

    private final RestClient restClient;
    private final String appKey;
    private final String geocodeUrl;

    public TmapGeocodingService(
            @Value("${tmap.app-key:}") String appKey,
            @Value("${tmap.geocode-url:https://apis.openapi.sk.com/tmap/geo/fullAddrGeo}") String geocodeUrl) {
        this.appKey = appKey;
        this.geocodeUrl = geocodeUrl;
        // 외부 호출이 응답하지 않을 때 요청 스레드(및 생성 트랜잭션)가 무한 대기하지 않도록 타임아웃을 둔다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public Optional<GeoPoint> geocode(String roadAddress) {
        if (roadAddress == null || roadAddress.isBlank()) {
            return Optional.empty();
        }
        if (appKey == null || appKey.isBlank()) {
            log.warn("[geocoding] TMap appKey 미설정 — 좌표 미확보 처리");
            return Optional.empty();
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(geocodeUrl)
                    .queryParam("version", 1)
                    .queryParam("format", "json")
                    .queryParam("addressFlag", "F02")
                    .queryParam("count", 1)
                    .queryParam("fullAddr", roadAddress)
                    .encode(StandardCharsets.UTF_8)
                    .build(true)
                    .toUri();

            TmapGeoResponse response = restClient.get()
                    .uri(uri)
                    .header("appKey", appKey)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(TmapGeoResponse.class);

            return toGeoPoint(response);
        } catch (Exception e) {
            log.warn("[geocoding] TMap 지오코딩 실패 — 좌표 미확보: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GeoPoint> toGeoPoint(TmapGeoResponse response) {
        if (response == null || response.coordinateInfo() == null) {
            return Optional.empty();
        }
        List<Coordinate> coordinates = response.coordinateInfo().coordinate();
        if (coordinates == null || coordinates.isEmpty()) {
            return Optional.empty();
        }
        Coordinate c = coordinates.get(0);
        String latText = firstNonBlank(c.newLat(), c.lat());
        String lonText = firstNonBlank(c.newLon(), c.lon());
        if (latText == null || lonText == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new GeoPoint(new BigDecimal(latText), new BigDecimal(lonText)));
        } catch (NumberFormatException e) {
            log.warn("[geocoding] 좌표 파싱 실패 — lat={}, lon={}", latText, lonText);
            return Optional.empty();
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    // ── TMap fullAddrGeo 응답 매핑(필요 필드만) ──
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TmapGeoResponse(CoordinateInfo coordinateInfo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CoordinateInfo(List<Coordinate> coordinate) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Coordinate(String newLat, String newLon, String lat, String lon) {}
}
