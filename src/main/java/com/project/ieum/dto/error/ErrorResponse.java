package com.project.ieum.dto.error;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp,
        List<FieldErrorDetail> fieldErrors
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, LocalDateTime.now(), List.of());
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(code, message, LocalDateTime.now(), fieldErrors);
    }

    public record FieldErrorDetail(
            String field,
            String message
    ) {
    }
}
