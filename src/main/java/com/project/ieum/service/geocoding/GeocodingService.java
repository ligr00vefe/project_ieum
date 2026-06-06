package com.project.ieum.service.geocoding;

import java.util.Optional;

/**
 * 도로명주소 → <b>좌표</b> 변환 계약. HelpRequest 위치 스냅샷 중 {@code latitude/longitude}만
 * 채우는 경계(port)다.
 *
 * <p>역할 경계: 이 서비스는 좌표만 담당한다. 구조화된 주소 구성요소
 * ({@code road_address·sido·sigungu·bname·zonecode·bcode})는 주소검색 위젯(우편번호 서비스)이
 * 제공하며, 그 출력(도로명주소)이 이 서비스의 입력이 된다.
 */
public interface GeocodingService {

    /**
     * 도로명주소를 좌표로 변환한다.
     *
     * @param roadAddress 도로명주소(예: "서울특별시 강남구 테헤란로 152")
     * @return 변환된 좌표. 주소를 해석하지 못하면 {@link Optional#empty()}.
     */
    Optional<GeoPoint> geocode(String roadAddress);
}
