package com.project.ieum.exception;

public class NotFoundException extends BusinessException {

    private static final String DEFAULT_CODE = "NOT_FOUND";

    public NotFoundException(String message) {
        super(DEFAULT_CODE, message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(DEFAULT_CODE, message, cause);
    }
}
