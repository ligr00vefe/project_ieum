package com.project.ieum.controller;

import com.project.ieum.exception.ClientSafeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 프레임워크 예외 메시지가 응답으로 새지 않는지 실제 요청으로 확인한다.
 *
 * <p>{@code GlobalExceptionHandlerTest}는 핸들러를 직접 불러 규칙을 보고, 여기서는 그 규칙이
 * <b>실제 요청 경로에서도</b> 적용되는지를 본다. 둘이 갈라질 수 있어서 나눴다 — 컨트롤러가
 * 예외를 자체 처리해 버리면 핸들러 단위 테스트가 통과해도 화면에는 다른 값이 나간다.
 *
 * <p>단언은 "없는 것"만 보지 않는다. 없는 것만 보면 요청이 예외 경로에 닿지 못하고 정상
 * 200을 받아도 통과해 버린다. 그래서 <b>400 + 우리 대체 문구</b>가 실제로 나왔는지를 먼저
 * 확인하고, 그 다음에 원문이 없는지를 본다.
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
    @DisplayName("음수 페이지 요청 — 예외 경로를 실제로 타고, 응답에는 대체 문구만 남는다")
    void negativePageIsHandledWithSafeMessage() throws Exception {
        MvcResult result = mockMvc.perform(get("/caregiver/board").param("page", "-1")).andReturn();
        String body = result.getResponse().getContentAsString();

        // 예외 경로에 도달했다는 증거 — 이게 없으면 아래 doesNotContain은 아무것도 증명하지 않는다.
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body).contains(ClientSafeMessage.FALLBACK);
        assertThat(body).contains("INVALID_ARGUMENT");   // 우리 핸들러가 만든 바디가 맞다

        // 수정 전에는 여기 스프링의 영문 메시지가 그대로 실려 있었다.
        assertThat(body)
                .doesNotContain("Page index")
                .doesNotContain("must not be less than zero");
    }

    @Test
    @DisplayName("음수 페이지 요청 — 패키지 구조·예외 타입·스택이 응답에 실리지 않는다")
    void negativePageDoesNotLeakInternals() throws Exception {
        MvcResult result = mockMvc.perform(get("/caregiver/board").param("page", "-1")).andReturn();
        String body = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body)
                .doesNotContain("org.springframework")
                .doesNotContain("com.project.ieum")
                .doesNotContain("IllegalArgumentException")
                .doesNotContain("\tat ");
    }
}
