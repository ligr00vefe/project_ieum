package com.project.ieum.exception;

import com.project.ieum.dto.ErrorResponse;
import com.project.ieum.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalExceptionHandler}의 메시지 노출 규칙 회귀가드.
 *
 * <p>핵심은 하나다 — {@code IllegalStateException}·{@code IllegalArgumentException}이
 * 도메인 규칙 위반과 프레임워크 내부 오류 <b>양쪽</b>으로 올라오는데, 앞은 사용자에게 보여야 하고
 * 뒤는 가려야 한다. 그래서 "일반화했는가"가 아니라 "출처에 따라 갈리는가"를 본다.
 *
 * <p>프레임워크 예외는 흉내 내지 않고 실제로 던지게 한다({@code PageRequest.of(-1, ...)},
 * {@code Enum.valueOf}). 스택 최상단으로 판정하므로, 손으로 만든 예외로는 검증이 성립하지 않는다.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** 실제로 프레임워크가 던지게 해서 잡는다 — 스택 최상단으로 판정하므로 흉내로는 검증이 안 된다. */
    private static IllegalArgumentException thrownBy(Runnable call) {
        try {
            call.run();
        } catch (IllegalArgumentException e) {
            return e;
        }
        throw new AssertionError("IllegalArgumentException이 발생하지 않았습니다");
    }

    private static String messageOf(ResponseEntity<ErrorResponse> response) {
        assertThat(response.getBody()).isNotNull();
        return response.getBody().getMessage();
    }

    @Nested
    @DisplayName("프레임워크가 던진 예외 — 내부 사정을 응답에 싣지 않는다")
    class FromFramework {

        @Test
        @DisplayName("페이지 번호가 음수일 때 스프링의 영문 메시지가 새지 않는다")
        void pageRequestMessageIsHidden() {
            IllegalArgumentException e = thrownBy(() -> PageRequest.of(-1, 10));
            assertThat(e.getMessage()).contains("Page index");   // 원본에는 들어 있다

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(e);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(messageOf(response))
                    .isEqualTo(ClientSafeMessage.FALLBACK)
                    .doesNotContain("Page index");
        }

        @Test
        @DisplayName("잘못된 enum 값일 때 패키지 구조가 새지 않는다")
        void enumValueOfMessageIsHidden() {
            IllegalArgumentException e = thrownBy(() -> UserRole.valueOf("NOT_A_ROLE"));
            assertThat(e.getMessage()).contains("com.project.ieum");  // 원본에는 FQCN이 들어 있다

            assertThat(messageOf(handler.handleIllegalArgumentException(e)))
                    .doesNotContain("com.project.ieum")
                    .doesNotContain("NOT_A_ROLE");
        }

        @Test
        @DisplayName("스택이 비어 있으면 판정 근거가 없으므로 가린다")
        void noStackTraceIsTreatedAsUnsafe() {
            IllegalStateException e = new IllegalStateException("내부 사정") {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this;
                }
            };

            assertThat(messageOf(handler.handleIllegalStateException(e)))
                    .isEqualTo(ClientSafeMessage.FALLBACK);
        }
    }

    @Nested
    @DisplayName("우리 코드가 던진 예외 — 사용자 안내가 그대로 살아남는다")
    class FromDomain {

        @Test
        @DisplayName("도메인 규칙 위반 메시지는 뭉개지지 않는다")
        void domainMessageSurvives() {
            IllegalStateException e = new IllegalStateException("이미 지원한 요청입니다.");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalStateException(e);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(messageOf(response)).isEqualTo("이미 지원한 요청입니다.");
        }

        @Test
        @DisplayName("메시지가 비어 있으면 대체 문구로 채운다")
        void blankDomainMessageFallsBack() {
            assertThat(messageOf(handler.handleIllegalArgumentException(new IllegalArgumentException())))
                    .isEqualTo(ClientSafeMessage.FALLBACK);
        }
    }

    @Nested
    @DisplayName("도메인 예외 계열은 상태 코드와 메시지를 그대로 유지한다")
    class DomainExceptions {

        @Test
        @DisplayName("NotFound 404 / Forbidden 403 / BadRequest 400")
        void keepsStatusAndMessage() {
            ResponseEntity<ErrorResponse> notFound =
                    handler.handleNotFoundException(new NotFoundException("문의를 찾을 수 없습니다."));
            assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(messageOf(notFound)).isEqualTo("문의를 찾을 수 없습니다.");

            ResponseEntity<ErrorResponse> forbidden =
                    handler.handleForbiddenException(new ForbiddenException("본인의 문의만 수정할 수 있습니다."));
            assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(messageOf(forbidden)).isEqualTo("본인의 문의만 수정할 수 있습니다.");

            ResponseEntity<ErrorResponse> badRequest =
                    handler.handleBadRequestException(new BadRequestException("이미지는 최대 5장까지 올릴 수 있습니다."));
            assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(messageOf(badRequest)).isEqualTo("이미지는 최대 5장까지 올릴 수 있습니다.");
        }

        @Test
        @DisplayName("분류되지 않은 BusinessException은 500이 아니라 400이다")
        void businessExceptionIsClientError() {
            ResponseEntity<ErrorResponse> response =
                    handler.handleBusinessException(new BusinessException("RULE_VIOLATION", "규칙에 어긋납니다."));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(messageOf(response)).isEqualTo("규칙에 어긋납니다.");
        }
    }

    @Test
    @DisplayName("정체불명 예외는 언제나 고정 문구 500")
    void unexpectedExceptionIsAlwaysGeneric() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpectedException(new RuntimeException("jdbc:mysql://internal-host:3306/db7"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(messageOf(response))
                .isEqualTo("서버 내부 오류가 발생했습니다.")
                .doesNotContain("jdbc");
    }
}
