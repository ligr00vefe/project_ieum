package com.project.ieum.service.admin;

import com.project.ieum.entity.popup.Popup;
import com.project.ieum.repository.PopupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 활성 팝업 목록 캐시.
 *
 * <p>{@code GlobalModelAdvice}가 모든 뷰 렌더링마다 팝업을 조회하기 때문에, 이 쿼리는 에러 페이지(/error)
 * 렌더링에도 따라붙는다. 커넥션 풀이 마르면 에러 페이지조차 못 그리고 500이 되는 원인이 된다.
 *
 * <p>만료 시각은 캐시하지 않는다 — 캐시에는 "enabled=true인 팝업 전체"만 담고, 만료 여부는 호출부가
 * 매 요청 현재 시각으로 거른다. 그래서 만료가 실시간으로 반영되고, 캐시는 관리자가 팝업을 바꿀 때만
 * 비워주면 된다({@link AdminPopupService}의 {@code @CacheEvict}).
 *
 * <p>자기호출(self-invocation)로는 캐시 프록시를 타지 못하므로 별도 빈으로 분리한다.
 */
@Component
@RequiredArgsConstructor
public class ActivePopupCache {

    public static final String ENABLED_POPUPS = "enabledPopups";

    private final PopupRepository popupRepository;

    // 트랜잭션 필수 — 트랜잭션 없이 파생 쿼리를 실행하면 커넥션이 세션 종료까지 반납되지 않아,
    // SSE 구독 요청(OSIV로 세션이 30분 유지)에서 캐시 미스가 나면 커넥션이 30분 물린다.
    // 캐시 히트 시에는 지연 획득(delayed acquisition) 덕에 빈 트랜잭션이 커넥션을 빌리지 않는다.
    @Cacheable(ENABLED_POPUPS)
    @Transactional(readOnly = true)
    public List<Popup> findEnabled() {
        return popupRepository.findByEnabledTrue();
    }
}
