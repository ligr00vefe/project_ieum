package com.project.ieum.config;

import com.project.ieum.service.admin.ActivePopupCache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 인메모리 캐시 설정(단일 인스턴스 전제).
 *
 * <p>{@code @EnableCaching}을 메인 애플리케이션 클래스가 아니라 이 설정 클래스에 두는 이유:
 * 슬라이스 테스트(@DataJpaTest 등)는 {@code @SpringBootConfiguration} 클래스의 애노테이션은 그대로
 * 적용하면서 캐시 자동설정은 로딩하지 않아, 메인 클래스에 붙이면 CacheManager 없이 컨텍스트가 깨진다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(ActivePopupCache.ENABLED_POPUPS);
    }
}
