package com.project.ieum.controller;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.market.MarketCategory;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.ServiceCategory;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.repository.HelpRequestRepository;
import com.project.ieum.repository.ServiceCategoryRepository;
import com.project.ieum.repository.UserProfileRepository;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.repository.market.MarketCategoryRepository;
import com.project.ieum.repository.market.MarketPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 비로그인(익명) 방문자의 공개 열람 범위 가드.
 *
 * <p>이음매칭(활동지원사 경로)·이음마켓의 <b>목록/상세는 공개</b>, 지원하기·채팅하기 같은 행위와
 * 작성/내 목록은 로그인으로 유도되어야 한다. 접근 규칙은 SecurityConfig의 matcher 순서에 의존하고
 * 상세 경로는 숫자 id 정규식으로만 열어 두므로, /market/new 같은 형제 경로가 함께 열리는 회귀를 막는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicBrowseAccessTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserProfileRepository userProfileRepository;
    @Autowired ServiceCategoryRepository serviceCategoryRepository;
    @Autowired HelpRequestRepository helpRequestRepository;
    @Autowired MarketCategoryRepository marketCategoryRepository;
    @Autowired MarketPostRepository marketPostRepository;

    private Long helpRequestId;
    private Long marketPostId;

    @BeforeEach
    void setUp() {
        User seller = userRepository.save(User.builder()
                .email("public-browse-" + System.nanoTime() + "@ieum.test")
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        UserProfile requester = userProfileRepository.save(UserProfile.builder()
                .user(seller)
                .fullName("공개열람 요청자")
                .build());

        ServiceCategory serviceCategory = serviceCategoryRepository.save(
                ServiceCategory.builder().code("MOVE-" + System.nanoTime()).name("이동 보조").build());
        helpRequestId = helpRequestRepository.save(HelpRequest.builder()
                .requester(requester)
                .serviceCategory(serviceCategory)
                .title("병원 동행 도움이 필요해요")
                .desiredStartDatetime(LocalDateTime.of(2026, 8, 1, 10, 0))
                .desiredEndDatetime(LocalDateTime.of(2026, 8, 1, 12, 0))
                .roadAddress("서울특별시 강남구 테헤란로 152")
                .sido("서울특별시")
                .sigungu("강남구")
                .latitude(new BigDecimal("37.500123"))
                .longitude(new BigDecimal("127.036456"))
                .status(HelpRequestStatus.OPEN)
                .build()).getId();

        MarketCategory marketCategory = marketCategoryRepository.save(
                MarketCategory.builder().name("보조기기-" + System.nanoTime()).build());
        marketPostId = marketPostRepository.save(MarketPost.builder()
                .seller(seller)
                .category(marketCategory)
                .title("전동 휠체어 나눔합니다")
                .price(new BigDecimal("0"))
                .roadAddress("서울특별시 강남구 테헤란로 152")
                .sido("서울특별시")
                .sigungu("강남구")
                .build()).getId();
    }

    @Test
    @DisplayName("비회원의 이음매칭 진입(/board)은 활동지원사 경로 게시판으로 간다")
    void anonymousBoardEntryGoesToCaregiverPath() throws Exception {
        // RequestUriInterceptor가 넣는 requestURI 모델 속성이 쿼리스트링으로 덧붙는다(기존 동작) → prefix로 검증.
        mockMvc.perform(get("/board"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/caregiver/board*"));
    }

    @Test
    @DisplayName("비회원도 매칭 목록·상세를 열람할 수 있다")
    void anonymousCanBrowseMatching() throws Exception {
        mockMvc.perform(get("/caregiver/board")).andExpect(status().isOk());
        mockMvc.perform(get("/caregiver/board/" + helpRequestId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("병원 동행 도움이 필요해요")));
    }

    @Test
    @DisplayName("비회원도 마켓 목록·상세를 열람할 수 있다")
    void anonymousCanBrowseMarket() throws Exception {
        mockMvc.perform(get("/market")).andExpect(status().isOk());
        mockMvc.perform(get("/market/" + marketPostId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("전동 휠체어 나눔합니다")));
    }

    @Test
    @DisplayName("비회원 상세 화면의 지원하기·채팅하기는 로그인 페이지로 연결된다")
    void anonymousDetailCtaPointsToLogin() throws Exception {
        mockMvc.perform(get("/caregiver/board/" + helpRequestId))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<a href=\"/login\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("지원하려면 활동지원사 로그인이 필요합니다.")));
        mockMvc.perform(get("/market/" + marketPostId))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("채팅을 하려면 로그인이 필요합니다.")));
    }

    @Test
    @DisplayName("비회원이 지원하기·채팅하기를 실제로 실행하면 로그인으로 막힌다")
    void anonymousActionsAreBlocked() throws Exception {
        mockMvc.perform(post("/caregiver/board/" + helpRequestId + "/apply").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(post("/market/" + marketPostId + "/chat").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("공개 범위는 목록·상세뿐 — 작성·내 목록·이용자 게시판은 여전히 로그인 필요")
    void nonPublicPathsStillRequireLogin() throws Exception {
        mockMvc.perform(get("/market/new"))
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/market/my"))
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/market/" + marketPostId + "/edit"))
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/disabled/board"))
                .andExpect(redirectedUrl("/login"));
    }
}
