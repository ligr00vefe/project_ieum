package com.project.ieum.config;

import com.project.ieum.interceptor.RequestUriInterceptor;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final EntityManagerFactory entityManagerFactory;

    public WebMvcConfig(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestUriInterceptor());
        registry.addWebRequestInterceptor(openEntityManagerInViewInterceptor())
                .excludePathPatterns("/api/**");
    }

    /**
     * OSIV를 {@code /api/**}에서만 제외한다.
     *
     * <p>OSIV가 켜져 있으면 Hibernate 세션이 응답 완료까지 열려 있고, 그 요청에서 획득한 커넥션은
     * 트랜잭션 종료가 아니라 <b>세션 종료</b> 시점에 반납된다. 일반 페이지는 수백 ms 안에 끝나 문제가
     * 없지만 SSE 응답({@code /api/notifications/stream})은 30분짜리라 구독 하나당 커넥션 하나가 30분씩
     * 물리고, EventSource 자동 재연결로 누적되어 풀이 고갈된다(2026-07-28 운영 장애: active=20/idle=0).
     *
     * <p>전역 해제(open-in-view=false)가 근본 해법이지만, 뷰 템플릿 다수가 지연로딩에 의존한다
     * (review.helpRequest, application.caregiver, inquiry.category 등). {@code /api/**}는 JSON·SSE 응답이라
     * 뷰 렌더링이 없어 안전하게 제외할 수 있다. 전역 해제는 각 화면을 fetch join으로 정리한 뒤에 한다.
     *
     * <p>이 빈이 있으면 Spring Boot의 자동 등록({@code @ConditionalOnMissingBean})이 물러나므로,
     * 여기서 등록한 인터셉터가 유일한 OSIV 인터셉터가 된다.
     */
    @org.springframework.context.annotation.Bean
    public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
        OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
        interceptor.setEntityManagerFactory(entityManagerFactory);
        return interceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profiles/**")
                .addResourceLocations(toUrl("uploads/profiles"));
        registry.addResourceHandler("/uploads/profile_custom/**")
                .addResourceLocations(toUrl("uploads/profile_custom"));
        registry.addResourceHandler("/uploads/profile_thumb/**")
                .addResourceLocations(toUrl("uploads/profile_thumb"));
        registry.addResourceHandler("/uploads/notices/**")
                .addResourceLocations(toUrl("uploads/notices"));
        registry.addResourceHandler("/uploads/market/**")
                .addResourceLocations(toUrl("uploads/market"));
        registry.addResourceHandler("/uploads/popups/**")
                .addResourceLocations(toUrl("uploads/popups"));
    }

    private String toUrl(String relativePath) {
        return Paths.get(relativePath).toAbsolutePath().normalize().toUri().toString() + "/";
    }
}
