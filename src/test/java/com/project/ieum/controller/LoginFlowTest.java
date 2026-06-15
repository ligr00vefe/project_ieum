package com.project.ieum.controller;

import com.project.ieum.entity.Gender;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.repository.UserProfileRepository;
import com.project.ieum.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserProfileRepository userProfileRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void shouldRedirectUserToMypageAfterLogin() throws Exception {
        User user = userRepository.save(User.builder()
                .email("login-user@ieum.test")
                .phone("010-1111-2222")
                .passwordHash(passwordEncoder.encode("password123!"))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(user)
                .fullName("로그인 이용자")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.F)
                .guardianName("보호자")
                .guardianPhone("010-0000-0000")
                .build());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "login-user@ieum.test")
                        .param("password", "password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/disabled/board"));
    }

    @Test
    void shouldShowLoginPageWithCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRenderStep1AgainWhenBasicInfoValidationFails() throws Exception {
        mockMvc.perform(post("/register/caregiver/step1")
                        .with(csrf())
                        .param("email", "invalid-email")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register/step1"));
    }

    @Test
    void shouldCompleteCaregiverRegistrationAndLogin() throws Exception {
        var session = mockMvc.perform(post("/register/type")
                        .with(csrf())
                        .param("type", "caregiver"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(post("/register/caregiver/step1")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(csrf())
                        .param("email", "new-caregiver@ieum.test")
                        .param("password", "password123!")
                        .param("name", "신규 지원사")
                        .param("birthDate", "1991-02-03")
                        .param("gender", "F")
                        .param("phone", "010-2222-3333"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/register/caregiver/step2")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(csrf())
                        .param("hasCertification", "true")
                        .param("certificationType", "활동지원사"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/register/caregiver/step3")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(csrf())
                        .param("experience", "병원 동행 경험")
                        .param("regionIds", "1"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/register/caregiver/step4")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(csrf())
                        .param("personalityTagIds", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "new-caregiver@ieum.test")
                        .param("password", "password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/caregiver/board"));
    }

    @Test
    void shouldRedirectCompletePageToHomeWhenRegistrationSessionIsMissing() throws Exception {
        mockMvc.perform(get("/register/caregiver/complete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
