package com.project.ieum.exception;

// 400 Bad Request 계열 도메인 예외의 부모.
// 기존 NotFoundException(404)/ForbiddenException(403)과 "완전히 동일한 패턴": BusinessException 상속 + 전용 400 핸들러.
// ⚠ org.apache.coyote.BadRequestException(Tomcat 내부 · checked)와 혼동 금지.
//   서비스에서는 이 클래스(com.project.ieum.exception.BadRequestException · unchecked)를 import할 것.
//   unchecked라 메서드 시그니처에 throws를 달지 않는다(서비스의 throws 절 제거 대상).
public class BadRequestException extends BusinessException {

    // 도메인 코드(HTTP 숫자 "400"이 아님). NotFoundException("NOT_FOUND")/ForbiddenException("FORBIDDEN")과 같은 결.
    private static final String DEFAULT_CODE = "BAD_REQUEST";

    public BadRequestException(String message) {
        super(DEFAULT_CODE, message);
    }

    // TODO(선택): 원인(cause) 포함이 필요하면 (String message, Throwable cause) 오버로드 추가.
}
