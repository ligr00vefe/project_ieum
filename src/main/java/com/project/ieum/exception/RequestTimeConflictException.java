package com.project.ieum.exception;

// post(작성) 시 같은 요청자의 "활성" 요청과 희망 시간대가 겹칠 때(existsOverlapping=true) 던진다. → 400.
// HelpRequestNotFoundException 과 동일하게 메시지를 생성자에 고정하는 도메인 예외.
public class RequestTimeConflictException extends BadRequestException {
    public RequestTimeConflictException() {
        super("이미 등록한 도움 요청과 시간대가 겹칩니다.");
    }
}
