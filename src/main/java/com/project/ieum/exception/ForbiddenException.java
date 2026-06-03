package com.project.ieum.exception;

public class ForbiddenException extends BusinessException {

    private static final String DEFAULT_CODE = "FORBIDDEN";

    public ForbiddenException(String message) {
        super(DEFAULT_CODE, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(DEFAULT_CODE, message, cause);
    }
}
