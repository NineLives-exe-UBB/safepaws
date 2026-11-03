package com.ninelives.safepaws.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors,
        LocalDateTime timestamp
) {

    public record FieldError(
            String field,
            String message
    ) {
    }
}

