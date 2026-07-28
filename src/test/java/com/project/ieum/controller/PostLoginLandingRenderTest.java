package com.project.ieum.controller;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.repository.CaregiverProfileRepository;
import com.project.ieum.repository.UserProfileRepository;
import com.project.ieum.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인 직후 착지 페이지가 실제로 렌더링되는지 검증한다.
 *
 * <p>기존 렌더링 테스트({@code PublicBrowseAccessTest})는 모두 익명 방문자 기준이라,
 * 로그인 사용자에게만 그려지는 헤더(알림 종·프로필)와 목록의 본인 분기가 한 번도 렌더링되지 않았다.
 * 로그인 성공 핸들러는 역할에 따라 이 두 경로로 보내므로, 여기서 깨지면 사용자는 로그인 직후 500을 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostLoginLandingRenderTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserProfileRepository userProfileRepository;
    @Autowired CaregiverProfileRepository caregiverProfileRepository;

    private String createUser(UserRole role) {
        String email = "landing-" + System.nanoTime() + "@ieum.test";
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("hash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
        if (role == UserRole.CAREGIVER) {
            caregiverProfileRepository.save(CaregiverProfile.builder()
                    .user(user)
                    .fullName("착지 테스트")
                    .build());
        } else {
            userProfileRepository.save(UserProfile.builder()
                    .user(user)
                    .fullName("착지 테스트")
                    .build());
        }
        return email;
    }

    @Test
    @DisplayName("USER 로그인 후 착지하는 /disabled/board가 렌더링된다")
    void userLandingRenders() throws Exception {
        String email = createUser(UserRole.USER);
        mockMvc.perform(get("/disabled/board").with(user(email).roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CAREGIVER 로그인 후 착지하는 /caregiver/board가 렌더링된다")
    void caregiverLandingRenders() throws Exception {
        String email = createUser(UserRole.CAREGIVER);
        mockMvc.perform(get("/caregiver/board").with(user(email).roles("CAREGIVER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 사용자가 이음마켓 목록을 열람할 수 있다")
    void marketListRendersForLoggedInUser() throws Exception {
        String email = createUser(UserRole.CAREGIVER);
        mockMvc.perform(get("/market").with(user(email).roles("CAREGIVER")))
                .andExpect(status().isOk());
    }

    /**
     * {@code /api/**}는 OSIV 대상에서 제외되어 있다(WebMvcConfig). 헤더가 매 페이지에서 호출하는
     * 알림 API가 세션 없이도 동작하는지 확인한다 — 여기서 깨지면 알림 뱃지가 조용히 죽는다.
     */
    @Test
    @DisplayName("OSIV가 빠진 /api/** 알림 엔드포인트가 정상 응답한다")
    void notificationApiWorksWithoutOsiv() throws Exception {
        String email = createUser(UserRole.USER);
        mockMvc.perform(get("/api/notifications/unread-count").with(user(email).roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications").param("page", "0").param("size", "20")
                        .with(user(email).roles("USER")))
                .andExpect(status().isOk());
    }
}
