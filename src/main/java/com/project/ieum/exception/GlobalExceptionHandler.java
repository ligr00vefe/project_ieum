package com.project.ieum.exception;

import com.project.ieum.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 전역 예외 → 응답 변환.
 *
 * <p>규칙은 하나다. <b>응답에 나가는 메시지는 우리가 사용자에게 보여주려고 쓴 문구여야 한다.</b>
 * 도메인 예외(BusinessException 계열)의 메시지는 작성자가 쓴 한국어 안내라 그대로 내보내고,
 * 그 밖의 예외는 출처를 따져({@link ClientSafeMessage}) 우리 코드가 만든 것만 통과시킨다.
 * 어느 쪽이든 상세 내용은 로그에 남기므로 진단 정보가 사라지지는 않는다.
 *
 * <p>응답 바디는 {@link #respond}에서만 만든다. 메시지를 거르는 자리가 여러 곳으로 흩어지면
 * 새 핸들러가 추가될 때 조용히 빠지기 때문이다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 응답 바디를 만드는 유일한 지점. */
    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }

    // ── 도메인 예외 ──────────────────────────────────────────────
    // 메시지가 전부 작성자가 쓴 사용자 대상 안내라 그대로 내보낸다.

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e) {
        log.warn("Resource not found: message={}", e.getMessage());
        return respond(HttpStatus.NOT_FOUND, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e) {
        log.warn("Access forbidden: message={}", e.getMessage());
        return respond(HttpStatus.FORBIDDEN, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException e) {
        log.warn("Bad request: message={}", e.getMessage());
        return respond(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage());
    }

    /**
     * 위 세 서브클래스에 걸리지 않은 도메인 예외.
     *
     * <p>500이 아니라 400으로 답한다. BusinessException은 "서버가 고장 났다"가 아니라
     * "요청이 규칙에 어긋난다"는 뜻이라, 500으로 답하면 상태 코드가 원인을 잘못 가리키는 데다
     * 그 응답에 도메인 문구가 실려 나간다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("Business exception occurred: code={}, message={}", e.getCode(), e.getMessage());
        return respond(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage());
    }

    // ── 검증 ────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // 제약 애노테이션에 message를 안 붙이면 getDefaultMessage()가 null일 수 있다.
        // 그대로 join하면 응답에 "null"이 실린다.
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        if (errorMessage.isBlank()) {
            errorMessage = "입력값이 올바르지 않습니다.";
        }

        log.warn("Validation failed: errors={}", errorMessage);
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", errorMessage);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: parameter={}, value={}", e.getName(), e.getValue());
        // 파라미터 '값'은 싣지 않는다 — 사용자가 보낸 값이라 새 정보는 없고, 그대로 되돌리면
        // 응답을 화면에 그리는 자리에서 반사형 주입의 통로가 된다.
        return respond(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "잘못된 파라미터 형식입니다: " + e.getName());
    }

    // ── 출처를 따져야 하는 예외 ───────────────────────────────────
    // 도메인 규칙 위반과 프레임워크 내부 오류가 같은 타입으로 올라온다.

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logGuarded("Illegal argument", e);
        return respond(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ClientSafeMessage.of(e));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        logGuarded("Illegal state", e);
        return respond(HttpStatus.BAD_REQUEST, "INVALID_STATE", ClientSafeMessage.of(e));
    }

    /**
     * 도메인 규칙 위반은 흔한 흐름이라 한 줄로, 출처가 밖인 예외는 원인을 찾아야 하므로
     * 스택까지 남긴다. 응답에서 가린 내용이 로그에서도 사라지지 않게 하는 것이 요점이다.
     */
    private void logGuarded(String label, RuntimeException e) {
        if (ClientSafeMessage.authoredHere(e)) {
            log.warn("{}: message={}", label, e.getMessage());
        } else {
            log.warn("{} from outside domain code", label, e);
        }
    }

    // ── 그 밖 ───────────────────────────────────────────────────

    // favicon.ico 등 정적 리소스 미존재 요청 — ERROR 로그 방지
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        log.error("Unexpected exception occurred", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다.");
    }
}
