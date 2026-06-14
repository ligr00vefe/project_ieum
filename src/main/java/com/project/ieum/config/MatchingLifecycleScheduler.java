package com.project.ieum.config;

import com.project.ieum.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * 매칭 종축 시간 기반 자동전이 스케줄러.
 *
 * <p>기본 비활성(off). 데모/운영에서 {@code matching.scheduler.enabled=true}로 켠다.
 * 꺼져 있으면 이 빈 자체가 생성되지 않아 {@code @EnableScheduling}/{@code @Scheduled}도 동작하지 않는다(완전 무해).
 *
 * <p>단일 인스턴스 가정. 전이 규칙·멱등성(상태 가드)은 {@link MatchingService#runLifecycleTransitions}에 있다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "matching.scheduler.enabled", havingValue = "true")
@RequiredArgsConstructor
public class MatchingLifecycleScheduler {

    private final MatchingService matchingService;

    @Scheduled(fixedDelayString = "${matching.scheduler.fixed-delay-ms:60000}")
    public void run() {
        matchingService.runLifecycleTransitions(LocalDateTime.now());
    }
}
