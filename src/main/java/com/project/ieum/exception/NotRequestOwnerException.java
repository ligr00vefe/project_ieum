package com.project.ieum.exception;

public class NotRequestOwnerException extends ForbiddenException {
    public NotRequestOwnerException() {
        super("해당 도움 요청의 게시자만 수행할 수 있습니다.");
    }
}
