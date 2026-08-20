package com.project.ieum.controller;

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
 * 프레임워크 예외 메시지가 응답으로 새지 않는지 실제 요청으로 확인한다.
 *
 * <p>{@code GlobalExceptionHandlerTest}는 핸들러를 직접 불러 규칙을 보고, 여기서는 그 규칙이
 * <b>실제 요청 경로에서도</b> 적용되는지를 본다. 둘이 갈라질 수 있어서 나눴다 — 컨트롤러가
 * 예외를 자체 처리해 버리면 핸들러 단위 테스트가 통과해도 화면에는 다른 값이 나간다.
 *
 * <p>대상은 로그인 없이 열리는 목록 화면이다({@code PublicBrowseAccessTest} 참고).
 * 인증 없이 프레임워크 메시지를 뽑아낼 수 있는지가 요점이므로 익명으로 요청한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExceptionMessageExposureTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("음수 페이지 요청 — 스프링 내부 메시지가 응답에 실리지 않는다")
    void negativePageDoesNotLeakFrameworkMessage() throws Exception {
        String body = mockMvc.perform(get("/caregiver/board").param("page", "-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain("Page index")
                .doesNotContain("must not be less than zero");
    }

    @Test
    @DisplayName("음수 페이지 요청 — 패키지 구조·스택이 응답에 실리지 않는다")
    void negativePageDoesNotLeakInternals() throws Exception {
        String body = mockMvc.perform(get("/caregiver/board").param("page", "-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain("org.springframework")
                .doesNotContain("com.project.ieum.controller")
                .doesNotContain("java.lang.IllegalArgumentException");
    }
}
