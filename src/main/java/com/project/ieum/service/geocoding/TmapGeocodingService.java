package com.project.ieum.service.geocoding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * TMap 기반 {@link GeocodingService} 구현 — <b>스캐폴드(미구현)</b>.
 *
 * <p>이번 라운드 범위는 매칭 엔티티(#8/#10)와 겹침 조회(#9)까지다. 실제 TMap 연동(appKey, HTTP,
 * 응답 파싱)은 후속 PR. 현재는 항상 {@link Optional#empty()}를 반환하므로 호출부는 좌표 미확보
 * (스냅샷 null)로 안전하게 동작한다.
 *
 * <p>TMap 책임 = 좌표 변환만. 우편번호·법정동코드 등 구조화된 주소 구성요소는 TMap이 제공하지 않으며,
 * 상위(생성 폼)의 주소검색 위젯이 채운 값을 스냅샷한다.
 *
 * <p>TODO(후속): TMap full-text geocoding 연동.
 *  - appKey는 {@code application-secret.properties}에 보관 후 주입. WGS84(경도,위도) 순서 주의.
 *  - 입력 도로명주소는 주소검색 위젯 출력을 그대로 사용(좌표만 보강).
 *  - 실패/미해석 시 empty 반환 — 거리는 정렬용이라 좌표 부재가 매칭을 막지 않음.
 *  - ⚠️ 무료 Quota/요금 미확인 — 연동 전 SK Open API 콘솔에서 호출 한도·초과 정책 확인 필수.
 */
@Slf4j
@Service
public class TmapGeocodingService implements GeocodingService {

    @Override
    public Optional<GeoPoint> geocode(String roadAddress) {
        // TODO: TMap 지오코딩 연동 (현재는 스캐폴드 — 좌표 미확보)
        log.debug("[geocoding-stub] TMap 연동 미구현 — 좌표 미확보 처리: {}", roadAddress);
        return Optional.empty();
    }
}
