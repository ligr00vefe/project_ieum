package com.project.ieum.config;

import com.project.ieum.entity.UserRole;
import com.project.ieum.repository.RegionRepository;
import com.project.ieum.repository.ServiceCategoryRepository;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.repository.market.MarketCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 시드 스위치가 양쪽 방향으로 제대로 동작하는지 확인한다.
 *
 * <p>{@code DataInitializer}는 기동 시 한 번 도는 {@code CommandLineRunner}라 나중에 호출해
 * 확인할 수 없다. 그래서 기동 결과로 남은 상태를 본다 — 데모 계정은 없고, 기준 데이터는 있다.
 * 둘 중 하나만 보면 회귀가 무증상으로 지나간다. 계정만 보면 기준 데이터까지 통째로 꺼진
 * 회귀를 놓치고, 기준 데이터만 보면 계정이 다시 심어지는 회귀를 놓친다.
 *
 * <p>어노테이션 조합을 다른 통합 테스트와 같게 맞춰 스프링 컨텍스트를 재사용한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeedGateTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired RegionRepository regionRepository;
    @Autowired ServiceCategoryRepository serviceCategoryRepository;
    @Autowired MarketCategoryRepository marketCategoryRepository;

    @Test
    @DisplayName("시드가 꺼져 있으면 데모 계정이 하나도 심어지지 않는다")
    void demoAccountsAreNotSeeded() {
        assertThat(userRepository.existsByEmail("admin@test.com")).isFalse();
        assertThat(userRepository.existsByEmail("d01@test.com")).isFalse();
        assertThat(userRepository.existsByEmail("c01@test.com")).isFalse();
        assertThat(userRepository.countByRole(UserRole.ADMIN)).isZero();
    }

    @Test
    @DisplayName("기준 데이터는 시드 스위치와 무관하게 채워진다")
    void referenceDataIsAlwaysSeeded() {
        // 회원가입·요청등록·마켓등록 폼의 선택지가 여기서 나온다. 비면 가입 자체가 막힌다.
        assertThat(regionRepository.count()).isPositive();
        assertThat(serviceCategoryRepository.count()).isPositive();
        assertThat(marketCategoryRepository.count()).isPositive();
    }

    @Test
    @DisplayName("로그인 페이지가 데모 계정 주소와 공통 비밀번호를 싣지 않는다")
    void loginPageDoesNotAdvertiseDemoCredentials() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andReturn().getResponse().getContentAsString();

        // 페이지가 실제로 렌더됐는지 먼저 확인 — 빈 응답이면 아래 단언이 아무것도 증명하지 않는다.
        assertThat(html).contains("이음 계정으로 로그인하세요");

        assertThat(html)
                .doesNotContain("test123$")
                .doesNotContain("admin@test.com")
                .doesNotContain("fillTestAccount");
    }
}
