package com.ninelives.safepaws.exception;

import com.ninelives.safepaws.dto.ErrorResponse;

import java.util.List;

/**
 * The query parameters of GET /announcements break one or more rules.
 */
public class InvalidFilterException extends RuntimeException {

    private final List<ErrorResponse.FieldError> fieldErrors;

    public InvalidFilterException(List<ErrorResponse.FieldError> fieldErrors) {
        super("Some fields are invalid");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<ErrorResponse.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
